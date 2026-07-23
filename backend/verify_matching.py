"""
跨渔港撮合 - 端到端验证脚本
====================================

业务流程：预挂牌 → 采购商报价（多个）→ 船东接受其中一份 → 自动拒绝其他报价 →
生成电子交易确认单 → 双方签署 → 状态 SIGNED → 查交易流水

保留此文件作为后续回归测试用例。
"""
import sys
import json
import urllib.request
import urllib.parse
from datetime import datetime, timedelta

API = "http://localhost:8080"
OUT = []


def log(msg):
    line = f"[撮合E2E {datetime.now().strftime('%H:%M:%S')}] {msg}"
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


def pick_vessels():
    r = http_get("/api/vessel/list")
    data = r.get("data") or []
    if len(data) < 2:
        raise RuntimeError("至少需要 2 艘船，跳过本测试")
    return data[0], data[1]


def must(cond, msg):
    if not cond:
        raise RuntimeError("ASSERTION FAILED: " + msg)


def main():
    log("========== 跨渔港撮合 端到端验证开始 ==========")
    v1, v2 = pick_vessels()
    log(f"v1={v1['vesselNo']} port={v1['portName']} sea={v1['seaAreaName']}")
    log(f"v2={v2['vesselNo']} port={v2['portName']} sea={v2['seaAreaName']}")

    # 1. 船东 v1 发布预挂牌
    log("【1】船东 v1 发布预挂牌（带鱼 100kg 期望 28元/kg）")
    available = (datetime.now() + timedelta(hours=4)).strftime("%Y-%m-%dT%H:%M:%S")
    r = http_post("/api/matching/listing/create", {
        "vesselId": v1["id"],
        "species": "带鱼",
        "expectedWeight": 100,
        "expectedPrice": 28.0,
        "availableTime": available,
        "remark": "刚归港，冷鲜"
    })
    must(r.get("code") == 0, "创建预挂牌失败: " + str(r))
    listing = r["data"]
    log(f"  listingId={listing['id']} status={listing['status']}")
    must(listing["status"] == "OPEN", "初始状态应为 OPEN")

    # 2. 跨渔港检索
    log("【2】跨渔港检索：查询闽南近海渔区 + 带鱼 + OPEN 状态")
    r = http_get("/api/matching/listing/search", {
        "seaArea": v1["seaAreaName"], "species": "带鱼", "status": "OPEN"
    })
    must(r.get("code") == 0, "检索失败")
    listings = r["data"]
    log(f"  检索到 {len(listings)} 条挂牌")
    must(any(x["id"] == listing["id"] for x in listings), "新创建的挂牌未出现在检索结果中")

    # 3. 采购商 A/B/C 报价
    log("【3】三个采购商分别报价 27/29/30 元/kg")
    bids = []
    for buyer, price in [("采购商A", 27.0), ("采购商B", 29.0), ("采购商C", 30.0)]:
        r = http_post("/api/matching/bid/create", {
            "listingId": listing["id"],
            "buyerName": buyer,
            "buyerPhone": f"1390000000{len(bids)}",
            "bidPrice": price,
            "bidWeight": 100,
            "destination": f"{buyer}加工厂",
            "message": f"现货现结"
        })
        must(r.get("code") == 0, f"{buyer} 报价失败: {r}")
        bids.append(r["data"])
        log(f"  {buyer} 报价 {price} 元/kg bidId={r['data']['id']}")

    # 4. 船东接受 采购商C 的报价（最高价）
    log("【4】船东接受 采购商C 的报价（30 元/kg）")
    r = http_post("/api/matching/accept", query={
        "listingId": listing["id"], "bidId": bids[2]["id"]
    })
    must(r.get("code") == 0, "成交失败: " + str(r))
    conf = r["data"]
    log(f"  确认单号={conf['confirmationNo']} 金额={conf['totalAmount']} 状态={conf['status']}")
    must(conf["status"] == "DRAFT", "初始应为 DRAFT")
    must(float(conf["totalAmount"]) == 3000.0, f"金额应为 3000.0，实际 {conf['totalAmount']}")

    # 5. 验证其他报价自动 REJECTED
    log("【5】验证其他两个报价自动 REJECTED")
    r = http_get(f"/api/matching/bid/byListing/{listing['id']}")
    bid_statuses = {b["buyerName"]: b["status"] for b in r["data"]}
    log(f"  报价状态：{bid_statuses}")
    must(bid_statuses["采购商A"] == "REJECTED", "采购商A 状态应 REJECTED")
    must(bid_statuses["采购商B"] == "REJECTED", "采购商B 状态应 REJECTED")
    must(bid_statuses["采购商C"] == "ACCEPTED", "采购商C 状态应 ACCEPTED")

    # 6. 验证挂牌 DEAL
    log("【6】验证挂牌状态 = DEAL")
    r = http_get(f"/api/matching/listing/byVessel/{v1['id']}")
    statuses = {x["id"]: x["status"] for x in r["data"]}
    must(statuses[listing["id"]] == "DEAL", "挂牌应 DEAL")

    # 7. 双方电子签署
    log("【7】双方电子签署")
    r = http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "买方", "signer": "采购商C-张经理", "buyerSide": "true"
    })
    must(r.get("code") == 0, "买方签署失败")
    must(r["data"]["status"] == "DRAFT", "单方签署后仍 DRAFT")
    log(f"  买方签署完成，状态={r['data']['status']}")

    r = http_post("/api/matching/confirmation/sign", query={
        "confirmationId": conf["id"], "party": "卖方", "signer": f"{v1['ownerName']}", "buyerSide": "false"
    })
    must(r.get("code") == 0, "卖方签署失败")
    must(r["data"]["status"] == "SIGNED", "双方签署后应 SIGNED")
    log(f"  卖方签署完成，状态={r['data']['status']}")

    # 8. 交易流水
    log("【8】查询交易流水")
    r = http_get("/api/matching/transactions", {"vesselId": v1["id"]})
    txs = r["data"]
    log(f"  该船交易流水数={len(txs)}")
    must(len(txs) >= 1, "交易流水不应为空")
    must(txs[0]["confirmationNo"] == conf["confirmationNo"], "流水号不匹配")

    # 9. 边界：已下架/已成交的挂牌不能再次接受报价
    log("【9】边界：已 DEAL 的挂牌不接受新报价")
    r = http_post("/api/matching/bid/create", {
        "listingId": listing["id"], "buyerName": "采购商D",
        "bidPrice": 35, "bidWeight": 50
    })
    must(r.get("code") != 0, "DEAL 状态的挂牌应拒绝新报价")
    log(f"  正确拒绝：{r.get('message')}")

    # 10. 边界：下架流程
    log("【10】边界：新建挂牌 → 主动下架")
    r = http_post("/api/matching/listing/create", {
        "vesselId": v1["id"], "species": "墨鱼",
        "expectedWeight": 50, "expectedPrice": 32.0,
        "availableTime": available
    })
    must(r.get("code") == 0, "二次挂牌失败")
    lid2 = r["data"]["id"]
    r = http_post(f"/api/matching/listing/cancel/{lid2}")
    must(r.get("code") == 0 and r["data"]["status"] == "CANCEL", "下架失败")
    log(f"  挂牌 {lid2} 已下架")

    log("========== 跨渔港撮合 端到端验证通过 ==========")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FAIL: {e}")
        sys.exit(1)
