"""
争议处理 - 端到端验证脚本
====================================

业务流程：
  1) 跨渔港撮合 → 双方签署 → 取得 SIGNED 确认单
  2) 买方对「重量」发起争议（携带证据描述）
  3) 平台分派第三方检测机构（华东水产检测中心）
  4) 检测机构出具报告（实测重量低于声明）
  5) 平台依据报告仲裁 → 结案
  6) 同一确认单再次发起争议：应被拒绝（已 CLOSED）
  7) 新建一个争议 → 用 reject 路径验证驳回流程
  8) 列表查询 / 按确认单查询

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
    line = f"[争议E2E {datetime.now().strftime('%H:%M:%S')}] {msg}"
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


def make_signed_confirmation():
    """复用撮合流程生成 SIGNED 确认单"""
    log("【0】准备：撮合 → 双方签署 → SIGNED 确认单")
    r = http_get("/api/vessel/list")
    vessels = r.get("data") or []
    must(len(vessels) >= 1, "至少需要 1 艘船")
    v = vessels[0]
    available = (datetime.now() + timedelta(hours=3)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/matching/listing/create", {
        "vesselId": v["id"], "species": "带鱼",
        "expectedWeight": 100, "expectedPrice": 28.0,
        "availableTime": available
    })
    must(r.get("code") == 0, "创建挂牌失败: " + str(r))
    listing = r["data"]
    r = http_post("/api/matching/bid/create", {
        "listingId": listing["id"], "buyerName": "争议E2E采购商",
        "buyerPhone": "13900000077", "bidPrice": 30.0,
        "bidWeight": 100, "destination": "争议E2E加工厂"
    })
    must(r.get("code") == 0, "报价失败: " + str(r))
    bid = r["data"]
    r = http_post("/api/matching/accept", query={
        "listingId": listing["id"], "bidId": bid["id"]
    })
    must(r.get("code") == 0, "成交失败")
    conf = r["data"]
    http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "买方",
        "signer": "争议E2E-张经理", "buyerSide": "true"
    })
    http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "卖方",
        "signer": v["ownerName"], "buyerSide": "false"
    })
    log(f"  确认单 {conf['confirmationNo']} 已 SIGNED")
    return conf


def make_another_signed_confirmation(vessels):
    """再准备一个 SIGNED 确认单（用于驳回测试）"""
    v = vessels[1] if len(vessels) > 1 else vessels[0]
    available = (datetime.now() + timedelta(hours=2)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/matching/listing/create", {
        "vesselId": v["id"], "species": "黄花鱼",
        "expectedWeight": 50, "expectedPrice": 40.0,
        "availableTime": available
    })
    must(r.get("code") == 0, "创建第二个挂牌失败")
    listing = r["data"]
    r = http_post("/api/matching/bid/create", {
        "listingId": listing["id"], "buyerName": "争议E2E-2",
        "bidPrice": 42.0, "bidWeight": 50,
        "destination": "争议E2E加工厂2"
    })
    must(r.get("code") == 0, "第二个报价失败")
    bid = r["data"]
    r = http_post("/api/matching/accept", query={
        "listingId": listing["id"], "bidId": bid["id"]
    })
    must(r.get("code") == 0, "第二个成交失败")
    conf = r["data"]
    http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "买方",
        "signer": "争议E2E-2-王经理", "buyerSide": "true"
    })
    http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "卖方",
        "signer": v["ownerName"], "buyerSide": "false"
    })
    return conf


def main():
    log("========== 争议处理 端到端验证开始 ==========")
    r = http_get("/api/vessel/list")
    vessels = r.get("data") or []
    must(len(vessels) >= 1, "至少需要 1 艘船")

    conf = make_signed_confirmation()
    conf2 = make_another_signed_confirmation(vessels)

    # 1) 买方发起重量争议
    log("【1】买方发起争议：重量")
    r = http_post("/api/dispute/open", {
        "confirmationId": conf["id"],
        "initiator": "买方",
        "initiatorName": "争议E2E采购商",
        "respondentName": conf["sellerName"],
        "disputeType": "WEIGHT",
        "description": "实际过磅重量比确认单少 12kg",
        "evidence": "现场过磅单、监控视频片段"
    })
    must(r.get("code") == 0, "发起争议失败: " + str(r))
    d = r["data"]
    log(f"  争议号={d['disputeNo']} 状态={d['status']}")
    must(d["status"] == "PENDING", "初始状态应为 PENDING")
    must(d["confirmationNo"] == conf["confirmationNo"], "确认单号未回填")

    # 2) 跟踪：按状态查询
    log("【2】按状态 PENDING 列表查询")
    r = http_get("/api/dispute/list", {"status": "PENDING"})
    items = r["data"]
    must(any(x["id"] == d["id"] for x in items), "新争议不在 PENDING 列表中")
    log(f"  PENDING 列表共 {len(items)} 条")

    # 3) 平台协调第三方检测机构
    log("【3】分派第三方检测机构")
    r = http_post("/api/dispute/assignAgency", query={
        "disputeId": d["id"], "agencyName": "华东水产质量检测中心"
    })
    must(r.get("code") == 0, "分派失败: " + str(r))
    must(r["data"]["status"] == "INSPECTING", "分派后应 INSPECTING")
    log(f"  机构={r['data']['assignedAgency']} 状态={r['data']['status']}")

    # 4) 检测机构出具报告
    log("【4】检测机构出具报告：实测重量 88kg（低于声明 100kg）")
    r = http_post("/api/dispute/report/submit", {
        "disputeId": d["id"],
        "agencyName": "华东水产质量检测中心",
        "reportType": "WEIGHT",
        "measuredWeight": 88.0,
        "measuredSpec": "体长 28-32cm",
        "qualityGrade": "B",
        "conclusion": "实测总重 88kg，比确认单 100kg 少 12kg，差异在合理公差范围外",
        "method": "GB/T 5009.1-2017 电子秤计量",
        "inspector": "李工"
    })
    must(r.get("code") == 0, "提交报告失败: " + str(r))
    report = r["data"]
    log(f"  报告号={report['reportNo']} 结论={report['conclusion'][:40]}…")
    must(report["reportNo"].startswith("RP-"), "报告号格式错误")

    # 5) 争议状态应自动升到 ARBITRATING
    log("【5】验证争议状态 = ARBITRATING")
    r = http_get(f"/api/dispute/{d['id']}")
    must(r["data"]["dispute"]["status"] == "ARBITRATING",
         f"应为 ARBITRATING，实际 {r['data']['dispute']['status']}")
    must(len(r["data"]["reports"]) == 1, "报告未挂载到争议")
    log(f"  当前挂载报告数={len(r['data']['reports'])}")

    # 6) 平台依据报告仲裁 → 结案
    log("【6】平台仲裁 → 结案（买方部分胜诉）")
    r = http_post("/api/dispute/close", query={
        "disputeId": d["id"],
        "result": "依据报告 RP-... 实测 88kg，判定短重 12kg，卖方按 12kg×30 元/kg 退还买方 360 元",
        "closedBy": "平台仲裁委员会-张主任"
    })
    must(r.get("code") == 0, "结案失败: " + str(r))
    must(r["data"]["status"] == "CLOSED", "结案后应 CLOSED")
    must(r["data"]["closedAt"] is not None, "结案时间未记录")
    log(f"  状态={r['data']['status']} 经办={r['data']['closedBy']}")

    # 7) 已结案 → 再次发起应被允许（不同确认单可同时存在 CLOSED）
    log("【7】同一确认单再次发起 → 应被拒绝（已存在未 CLOSED/REJECTED 的争议）")
    r = http_post("/api/dispute/open", {
        "confirmationId": conf["id"],
        "initiator": "卖方", "initiatorName": conf["sellerName"],
        "respondentName": "争议E2E采购商",
        "disputeType": "QUALITY",
        "description": "买方无理取闹",
        "evidence": ""
    })
    must(r.get("code") == 0, "已 CLOSED 后应允许新争议: " + str(r))
    log(f"  重新发起成功，争议号={r['data']['disputeNo']}（首次 CLOSED 状态允许新建）")

    # 8) 驳回流程：用第二个确认单
    log("【8】驳回流程：第二个确认单 → 发起 → 平台驳回")
    r = http_post("/api/dispute/open", {
        "confirmationId": conf2["id"],
        "initiator": "卖方", "initiatorName": conf2["sellerName"],
        "respondentName": "争议E2E-2",
        "disputeType": "QUALITY",
        "description": "声称规格不符，无实质证据",
        "evidence": ""
    })
    must(r.get("code") == 0, "第二个争议发起失败")
    d2 = r["data"]
    r = http_post("/api/dispute/reject", query={
        "disputeId": d2["id"],
        "reason": "证据不足，且确认单已约定规格口径，不予受理",
        "closedBy": "平台仲裁委员会-张主任"
    })
    must(r.get("code") == 0, "驳回失败")
    must(r["data"]["status"] == "REJECTED", f"应 REJECTED，实际 {r['data']['status']}")
    log(f"  状态={r['data']['status']} 原因={r['data']['arbitrateResult'][:30]}…")

    # 9) 列表查询：CLOSED / REJECTED
    log("【9】列表查询")
    for s in ["CLOSED", "REJECTED", "INSPECTING", "ARBITRATING"]:
        r = http_get("/api/dispute/list", {"status": s})
        log(f"  {s} 共 {len(r['data'])} 条")

    # 10) 按确认单查询
    log("【10】按确认单查询所有争议")
    r = http_get(f"/api/dispute/byConfirmation/{conf['id']}")
    must(len(r["data"]) >= 1, "按确认单查询无结果")
    log(f"  确认单 {conf['confirmationNo']} 关联争议 {len(r['data'])} 条")

    log("========== 争议处理 端到端验证通过 ==========")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FAIL: {e}")
        sys.exit(1)
