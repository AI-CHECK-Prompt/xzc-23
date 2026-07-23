"""
冷链配送协同 - 端到端验证脚本
====================================

业务流程：
  1) 注册冷链车辆
  2) 跨渔港撮合 → 双方签署 → 取得 SIGNED 确认单
  3) 为确认单创建冷链运单（自动按品种设置温度阈值）
  4) 标记起运
  5) 上报 1 条正常温度 → 不产生告警
  6) 上报 1 条异常温度 → 触发告警（复用 AlertService）
  7) 在告警列表中按 "冷链温度异常" 关键字能查到对应告警
  8) 标记到达，车辆状态回归 IDLE

保留此文件作为后续回归测试用例（参见 CLAUDE.md 偏好）。
"""
import sys
import json
import urllib.request
import urllib.parse
from datetime import datetime, timedelta

API = "http://localhost:8080"
OUT = []


def log(msg):
    line = f"[冷链E2E {datetime.now().strftime('%H:%M:%S')}] {msg}"
    print(line, flush=True)
    OUT.append(line)


def http_get(path, params=None):
    url = API + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=15) as r:
        return json.loads(r.read().decode("utf-8"))


def http_post(path, body=None, query=None):
    url = API + path
    if query:
        url += "?" + urllib.parse.urlencode(query)
    req = urllib.request.Request(
        url,
        data=json.dumps(body or {}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read().decode("utf-8"))


def must(cond, msg):
    if not cond:
        raise RuntimeError("ASSERTION FAILED: " + msg)


def pick_vessels():
    r = http_get("/api/vessel/list")
    data = r.get("data") or []
    if len(data) < 1:
        raise RuntimeError("至少需要 1 艘船，跳过本测试")
    return data


def register_vehicle(vehicle_no, driver):
    log(f"【1】注册冷链车辆 车号={vehicle_no} 司机={driver}")
    r = http_post("/api/cold-chain/vehicle/register", {
        "vehicleNo": vehicle_no,
        "driverName": driver,
        "driverPhone": "13900099000",
        "vehicleType": "冷藏车-中型",
        "capacity": 5.0,
        "status": "IDLE"
    })
    must(r.get("code") == 0, "注册车辆失败: " + str(r))
    log(f"  vehicleId={r['data']['id']} status={r['data']['status']}")
    return r["data"]


def make_signed_confirmation(vessels):
    """走一遍撮合流程，生成一个 SIGNED 状态的交易确认单供冷链使用"""
    log("【2】跨渔港撮合 → 双方签署 → 取得 SIGNED 确认单")
    v1 = vessels[0]
    available = (datetime.now() + timedelta(hours=4)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/matching/listing/create", {
        "vesselId": v1["id"],
        "species": "带鱼",
        "expectedWeight": 80,
        "expectedPrice": 28.0,
        "availableTime": available,
        "remark": "冷链E2E专用"
    })
    must(r.get("code") == 0, "创建预挂牌失败: " + str(r))
    listing = r["data"]

    r = http_post("/api/matching/bid/create", {
        "listingId": listing["id"],
        "buyerName": "冷链E2E采购商",
        "buyerPhone": "13900000099",
        "bidPrice": 30.0,
        "bidWeight": 80,
        "destination": "冷链E2E加工厂",
        "message": "现结"
    })
    must(r.get("code") == 0, "报价失败: " + str(r))
    bid = r["data"]

    r = http_post("/api/matching/accept", query={
        "listingId": listing["id"], "bidId": bid["id"]
    })
    must(r.get("code") == 0, "成交失败: " + str(r))
    conf = r["data"]

    # 双方签署
    r = http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "买方",
        "signer": "冷链E2E-张经理", "buyerSide": "true"
    })
    must(r.get("code") == 0, "买方签署失败")
    r = http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "卖方",
        "signer": v1["ownerName"], "buyerSide": "false"
    })
    must(r.get("code") == 0, "卖方签署失败")
    must(r["data"]["status"] == "SIGNED", "双方签署后应 SIGNED，实际=" + r["data"]["status"])
    log(f"  确认单 {conf['confirmationNo']} 已 SIGNED")
    return conf


def create_shipment(conf, vehicle):
    log("【3】为确认单创建冷链运单")
    departure = (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%S")
    arrival = (datetime.now() + timedelta(hours=5)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/cold-chain/shipment/create", query={
        "confirmationId": conf["id"],
        "vehicleId": vehicle["id"],
        "plannedDeparture": departure,
        "plannedArrival": arrival
    })
    must(r.get("code") == 0, "创建运单失败: " + str(r))
    s = r["data"]
    log(f"  shipmentId={s['id']} status={s['status']} 阈值=[{s['minTemp']}, {s['maxTemp']}]°C")
    must(s["status"] == "CREATED", "初始状态应为 CREATED")
    must(s["minTemp"] is not None and s["maxTemp"] is not None, "温度阈值未设置")
    # 带鱼阈值应为 [-1, 4]
    must(float(s["minTemp"]) == -1 and float(s["maxTemp"]) == 4,
         f"带鱼阈值应为 [-1, 4]，实际=[{s['minTemp']}, {s['maxTemp']}]")
    return s


def mark_depart(shipment_id):
    log("【4】标记起运")
    r = http_post(f"/api/cold-chain/shipment/depart/{shipment_id}")
    must(r.get("code") == 0, "起运失败: " + str(r))
    must(r["data"]["status"] == "IN_TRANSIT", "起运后状态应 IN_TRANSIT")
    log(f"  actualDeparture={r['data']['actualDepartureTime']}")


def report_normal_temp(shipment_id):
    log("【5】上报正常温度 2°C（带鱼阈值 [-1, 4]）")
    r = http_post("/api/cold-chain/temperature/report", query={
        "shipmentId": shipment_id,
        "temperature": 2.0,
        "source": "GPS-温控探头"
    })
    must(r.get("code") == 0, "温度上报失败: " + str(r))
    must(r["data"]["anomaly"] is False, "2°C 不应标记为异常")
    log(f"  readingId={r['data']['id']} anomaly={r['data']['anomaly']}")


def report_anomaly_temp(shipment_id):
    log("【6】上报异常温度 12°C（超出带鱼阈值）")
    r = http_post("/api/cold-chain/temperature/report", query={
        "shipmentId": shipment_id,
        "temperature": 12.0,
        "source": "GPS-温控探头"
    })
    must(r.get("code") == 0, "温度上报失败: " + str(r))
    must(r["data"]["anomaly"] is True, "12°C 应标记为异常")
    log(f"  readingId={r['data']['id']} anomaly={r['data']['anomaly']}")


def verify_alert_generated(vehicle_no, conf_no):
    log("【7】验证告警已生成（按 vesselNo='冷链-车号' 检索）")
    pending = http_get("/api/alert/pending").get("data") or []
    log(f"  当前待处理告警数={len(pending)}")
    matched = [a for a in pending
               if a.get("alertType") == "冷链温度异常"
               and a.get("vesselNo") == "冷链-" + vehicle_no]
    must(len(matched) >= 1, "未找到冷链温度异常告警")
    a = matched[0]
    log(f"  告警ID={a['id']} 级别={a['level']} 描述={a['description']}")
    must("温度异常" in a["description"], "告警描述缺失温度异常关键字")
    must(conf_no in a["description"], "告警描述缺失确认单号")


def mark_arrive(shipment_id):
    log("【8】标记到达")
    r = http_post(f"/api/cold-chain/shipment/arrive/{shipment_id}")
    must(r.get("code") == 0, "到达失败: " + str(r))
    must(r["data"]["status"] == "ARRIVED", "到达后应 ARRIVED")
    log(f"  actualArrival={r['data']['actualArrivalTime']}")


def detail_check(shipment_id):
    log("【9】运单详情：应包含 2 条温度记录，anomalyCount=1")
    r = http_get(f"/api/cold-chain/shipment/{shipment_id}")
    must(r.get("code") == 0, "查询运单详情失败")
    detail = r["data"]
    s = detail["shipment"]
    readings = detail["readings"]
    log(f"  状态={s['status']} anomalyCount={s['anomalyCount']} 温度记录数={len(readings)}")
    must(len(readings) == 2, f"应有 2 条温度记录，实际 {len(readings)}")
    must(s["anomalyCount"] == 1, f"anomalyCount 应为 1，实际 {s['anomalyCount']}")


def main():
    log("========== 冷链配送协同 端到端验证开始 ==========")
    vessels = pick_vessels()
    vehicle = register_vehicle("CC-E2E-001", "老王")
    conf = make_signed_confirmation(vessels)
    shipment = create_shipment(conf, vehicle)
    mark_depart(shipment["id"])
    report_normal_temp(shipment["id"])
    report_anomaly_temp(shipment["id"])
    verify_alert_generated(vehicle["vehicleNo"], conf["confirmationNo"])
    mark_arrive(shipment["id"])
    detail_check(shipment["id"])
    log("========== 冷链配送协同 端到端验证通过 ==========")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FAIL: {e}")
        sys.exit(1)
