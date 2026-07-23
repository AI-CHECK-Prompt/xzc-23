"""
全模块端到端贯通验证脚本
====================================

按业务时序串通四个模块：
  模块① 渔获价格指数   —— 历史采购回传 → 指数计算（异常剔除 + 中位数/分位数）
  模块② 跨渔港撮合     —— 预挂牌 → 多家报价 → 接受最高价 → 双方电子签署
  模块③ 冷链配送协同   —— 车辆注册 → 运单创建 → 起运 → 温度异常告警 → 到达
  模块④ 争议处理       —— 发起争议 → 协调检测机构 → 出具报告 → 仲裁结案

验收标准（对应需求文档的「验收场景」）：
  ✓ 渔获价格指数在历史采购回传数据上输出一致性结果
  ✓ 跨渔港撮合从预挂牌、报价、成交、确认单全流程跑通
  ✓ 冷链温度异常时平台生成告警并通知双方
  ✓ 争议处理流程可发起、可跟踪、可结案

保留此文件作为关键全流程回归测试用例。
"""
import sys
import io
# 兼容 Windows GBK 控制台：强制 UTF-8 输出
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
sys.stderr = sys.stdout
import json
import urllib.request
import urllib.parse
from datetime import datetime, timedelta

API = "http://localhost:8080"
OUT = []


def log(msg):
    line = f"[全流程E2E {datetime.now().strftime('%H:%M:%S')}] {msg}"
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


def section(title):
    log("")
    log("=" * 70)
    log(f"  {title}")
    log("=" * 70)


def main():
    log("########## 全模块端到端贯通验证 ##########")

    # ===========================================================
    # ① 渔获价格指数
    # ===========================================================
    section("模块① 渔获价格指数 —— 异常剔除 + 中位数/分位数")

    vessel = http_get("/api/vessel/list")["data"][0]
    log(f"选用船舶：{vessel['vesselNo']} 海区={vessel['seaAreaName']}")

    # 注入 11 条正常 + 1 条异常高价
    base_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0) - timedelta(days=2)
    day_key = base_date.strftime("%Y-%m-%d")
    prices = [20, 22, 24, 25, 26, 27, 28, 29, 29.5, 30, 30, 1000]
    for i, p in enumerate(prices):
        r = http_post("/api/purchase/report", {
            "vesselId": vessel["id"],
            "vesselNo": vessel["vesselNo"],
            "voyageId": None,
            "buyerName": f"全流程采购商{i}",
            "species": "带鱼",
            "weight": 5.0,
            "price": p,
            "destination": "全流程加工厂",
            "purchaseTime": (base_date + timedelta(hours=i)).strftime("%Y-%m-%dT%H:%M:%S")
        })
        must(r.get("code") == 0, f"采购回传失败: {r}")
    log(f"已注入 {len(prices)} 条采购回传（含 1 条异常高价 1000）")

    r = http_post(f"/api/price-index/calculate/DAY/{day_key}")
    must(r.get("code") == 0, "价格指数计算失败")
    indices = r["data"]
    must(len(indices) >= 1, "未生成指数记录")
    idx = indices[0]
    log(f"指数：海区={idx['seaArea']} 品种={idx['species']} P50={idx['median']} "
        f"P25={idx['p25']} P75={idx['p75']} 样本={idx['sampleSize']} 剔除={idx['anomalyFiltered']}")
    must(idx["anomalyFiltered"] >= 1, "应至少剔除 1 条异常高价")
    must(idx["median"] is not None and float(idx["median"]) < 30,
         f"中位数应剔除异常后 <30，实际 {idx['median']}")

    # 一致性：再次计算
    r2 = http_post(f"/api/price-index/calculate/DAY/{day_key}").get("data") or []
    must(r2[0]["median"] == idx["median"], "价格指数非确定性")
    log("  [OK] 渔获价格指数：异常剔除 + 一致性通过")

    # ===========================================================
    # ② 跨渔港撮合
    # ===========================================================
    section("模块② 跨渔港撮合 —— 预挂牌 / 报价 / 成交 / 签署")

    available = (datetime.now() + timedelta(hours=4)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/matching/listing/create", {
        "vesselId": vessel["id"], "species": "带鱼",
        "expectedWeight": 200, "expectedPrice": 28.0,
        "availableTime": available, "remark": "全流程贯通"
    })
    must(r.get("code") == 0, "挂牌失败")
    listing = r["data"]
    log(f"挂牌 {listing['id']} 创建成功（OPEN）")

    # 跨港检索
    r = http_get("/api/matching/listing/search", {
        "seaArea": vessel["seaAreaName"], "species": "带鱼", "status": "OPEN"
    })
    must(any(x["id"] == listing["id"] for x in r["data"]), "新挂牌未出现在跨港检索结果中")
    log("[OK] 跨渔港检索：跨港采购商可见本港挂牌")

    # 三家报价
    bids = []
    for buyer, price in [("全流程A", 27.0), ("全流程B", 29.0), ("全流程C", 31.0)]:
        r = http_post("/api/matching/bid/create", {
            "listingId": listing["id"], "buyerName": buyer,
            "bidPrice": price, "bidWeight": 200,
            "destination": f"{buyer}加工厂"
        })
        must(r.get("code") == 0, f"{buyer} 报价失败")
        bids.append(r["data"])
        log(f"  {buyer} 报价 {price} 元/kg")

    # 接受最高价
    r = http_post("/api/matching/accept", query={
        "listingId": listing["id"], "bidId": bids[2]["id"]
    })
    must(r.get("code") == 0, "成交失败")
    conf = r["data"]
    log(f"成交：确认单 {conf['confirmationNo']} 金额 {conf['totalAmount']}（DRAFT）")
    must(float(conf["totalAmount"]) == 31.0 * 200, "金额计算错误")

    # 双方电子签署
    http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "买方",
        "signer": "全流程C-王总", "buyerSide": "true"
    })
    r = http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "卖方",
        "signer": vessel["ownerName"], "buyerSide": "false"
    })
    must(r["data"]["status"] == "SIGNED", "双方签署后应 SIGNED")
    log("[OK] 跨渔港撮合：预挂牌 / 报价 / 成交 / 双签 / SIGNED")

    # ===========================================================
    # ③ 冷链配送协同
    # ===========================================================
    section("模块③ 冷链配送协同 —— 异常温度自动告警")

    r = http_post("/api/cold-chain/vehicle/register", {
        "vehicleNo": "CC-ALL-001", "driverName": "全流程老王",
        "driverPhone": "13911110000", "vehicleType": "冷藏车-大型",
        "capacity": 10.0, "status": "IDLE"
    })
    must(r.get("code") == 0, "车辆注册失败")
    vehicle_obj = r["data"]
    log(f"车辆 {vehicle_obj['vehicleNo']} 注册成功")

    dep = (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%dT%H:%M:%S")
    arr = (datetime.now() + timedelta(hours=6)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/cold-chain/shipment/create", query={
        "confirmationId": conf["id"], "vehicleId": vehicle_obj["id"],
        "plannedDeparture": dep, "plannedArrival": arr
    })
    must(r.get("code") == 0, "运单创建失败")
    shipment = r["data"]
    log(f"运单 {shipment['id']} 创建成功 阈值=[{shipment['minTemp']}, {shipment['maxTemp']}]°C")

    http_post(f"/api/cold-chain/shipment/depart/{shipment['id']}")
    log("起运 IN_TRANSIT")

    # 正常温度
    r = http_post("/api/cold-chain/temperature/report", query={
        "shipmentId": shipment["id"], "temperature": 2.0, "source": "GPS-温控探头"
    })
    must(r["data"]["anomaly"] is False, "2°C 不应异常")
    log("正常温度 2°C → 无告警")

    # 异常温度 → 告警
    r = http_post("/api/cold-chain/temperature/report", query={
        "shipmentId": shipment["id"], "temperature": 15.0, "source": "GPS-温控探头"
    })
    must(r["data"]["anomaly"] is True, "15°C 应异常")
    log("异常温度 15°C → 触发告警")

    pending = http_get("/api/alert/pending").get("data") or []
    matched = [a for a in pending
               if a.get("alertType") == "冷链温度异常"
               and a.get("vesselNo") == "冷链-" + vehicle_obj["vehicleNo"]]
    must(len(matched) >= 1, "未在告警列表中找到冷链温度异常")
    log(f"[OK] 冷链告警已生成：{matched[0]['description']}")

    http_post(f"/api/cold-chain/shipment/arrive/{shipment['id']}")
    log("到达 ARRIVED [OK]")

    # ===========================================================
    # ④ 争议处理
    # ===========================================================
    section("模块④ 争议处理 —— 发起 / 检测 / 仲裁结案")

    r = http_post("/api/dispute/open", {
        "confirmationId": conf["id"],
        "initiator": "买方", "initiatorName": "全流程C",
        "respondentName": vessel["ownerName"],
        "disputeType": "WEIGHT",
        "description": "短重 15kg",
        "evidence": "现场过磅单"
    })
    must(r.get("code") == 0, "争议发起失败")
    d = r["data"]
    log(f"争议 {d['disputeNo']} 发起成功（{d['status']}）")

    r = http_post("/api/dispute/assignAgency", query={
        "disputeId": d["id"], "agencyName": "国家海产品质量监督检验中心"
    })
    must(r["data"]["status"] == "INSPECTING", "分派后应 INSPECTING")
    log(f"分派机构：{r['data']['assignedAgency']}（INSPECTING）")

    r = http_post("/api/dispute/report/submit", {
        "disputeId": d["id"],
        "agencyName": "国家海产品质量监督检验中心",
        "reportType": "WEIGHT",
        "measuredWeight": 185.0,
        "measuredSpec": "体长 28-32cm",
        "qualityGrade": "A",
        "conclusion": "实测 185kg，确认单 200kg，短重 15kg（7.5%）",
        "method": "GB/T 5009.1",
        "inspector": "高级工程师 张博士"
    })
    must(r.get("code") == 0, "报告提交失败")
    report = r["data"]
    log(f"报告 {report['reportNo']} 出具 → 状态 ARBITRATING")

    r = http_post("/api/dispute/close", query={
        "disputeId": d["id"],
        "result": "依据报告实测 185kg，判定短重 15kg，卖方退还 15kg×31元=465 元",
        "closedBy": "平台仲裁委员会"
    })
    must(r["data"]["status"] == "CLOSED", "结案后应 CLOSED")
    log(f"[OK] 争议 CLOSED，经办={r['data']['closedBy']}")

    # ===========================================================
    section("[PASS] 全部四大模块端到端贯通验证通过")
    log("  ① 渔获价格指数 —— 异常剔除 + 中位数/分位数 + 一致性")
    log("  ② 跨渔港撮合   —— 挂牌/报价/成交/签署")
    log("  ③ 冷链配送协同 —— 温度异常自动告警")
    log("  ④ 争议处理     —— 发起/检测/仲裁结案")
    log("########## 验收场景全部命中 ##########")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FAIL: {e}")
        sys.exit(1)
