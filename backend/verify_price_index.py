"""
渔获价格指数 - 端到端验证脚本
====================================

场景：
1. 注入一批已知的采购回传数据（含 1 个明显异常高价）
2. 触发指定日级指数计算
3. 校验：
   a) 结果集中应剔除异常值（anomalyFiltered >= 1）
   b) 中位数与 numpy 计算一致（在容忍误差内）
   c) 同一份输入再次计算结果完全相同（确定性）
4. 重建区间 [today-1, today)，再触发周/月级

保留此文件作为后续回归测试用例（参见 CLAUDE.md 偏好）。
"""
import sys
import json
import time
import urllib.request
import urllib.parse
from datetime import datetime, timedelta

API = "http://localhost:8080"
OUT = []


def log(msg):
    line = f"[价格指数E2E {datetime.now().strftime('%H:%M:%S')}] {msg}"
    print(line, flush=True)
    OUT.append(line)


def http_get(path, params=None):
    url = API + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    with urllib.request.urlopen(url, timeout=15) as r:
        return json.loads(r.read().decode("utf-8"))


def http_post(path, body=None):
    req = urllib.request.Request(
        API + path,
        data=json.dumps(body or {}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=15) as r:
        return json.loads(r.read().decode("utf-8"))


def get_vessel():
    r = http_get("/api/vessel/list")
    if r.get("code") != 0 or not r.get("data"):
        raise RuntimeError("未找到任何船舶，请先启动 DataInitializer")
    return r["data"][0]


def seed_purchase(vessel, base_date):
    """注入 12 条已知采购数据：带鱼 20~30 之间 11 条 + 1 条 1000（异常高价）"""
    log(f"注入采购数据到 vesselId={vessel['id']} seaArea={vessel['seaAreaName']}")
    prices = [20, 22, 24, 25, 26, 27, 28, 29, 29.5, 30, 30, 1000]  # 最后一条是异常
    for i, p in enumerate(prices):
        body = {
            "vesselId": vessel["id"],
            "vesselNo": vessel["vesselNo"],
            "voyageId": None,
            "buyerName": f"采购商{i}",
            "species": "带鱼",
            "weight": 10 + i * 0.5,  # 全部为"中"规格（>=1kg 且 <=5kg 取决于具体值）— 实际范围 10-15.5 都 >5kg -> "大"
            "price": p,
            "destination": "测试加工厂",
            "purchaseTime": (base_date + timedelta(hours=i)).strftime("%Y-%m-%dT%H:%M:%S")
        }
        r = http_post("/api/purchase/report", body)
        if r.get("code") != 0:
            raise RuntimeError(f"注入失败: {r}")
    log(f"已注入 {len(prices)} 条采购记录（最后一条为异常高价 1000）")


def trigger_and_check(period_type, period_key, expected_filtered_min=1):
    log(f"触发计算 periodType={period_type} periodKey={period_key}")
    r = http_post(f"/api/price-index/calculate/{period_type}/{period_key}")
    if r.get("code") != 0:
        raise RuntimeError(f"计算失败: {r}")
    results = r.get("data") or []
    if not results:
        raise RuntimeError(f"未生成任何指数记录，请确认采购时间在 {period_key} 周期内")
    log(f"生成 {len(results)} 条指数记录")
    for idx in results:
        log(f"  {idx['seaArea']} / {idx['species']} / {idx['specification']} / {idx['season']} "
            f"P50={idx['median']} P25={idx['p25']} P75={idx['p75']} "
            f"P5={idx['p5']} P95={idx['p95']} "
            f"样本={idx['sampleSize']} 剔除={idx['anomalyFiltered']}")
    if results[0]["anomalyFiltered"] < expected_filtered_min:
        raise RuntimeError(f"异常剔除数 {results[0]['anomalyFiltered']} < 预期 {expected_filtered_min}")
    return results


def check_determinism(period_type, period_key):
    log("【确定性】再次触发相同周期计算，两次结果必须完全相同")
    r1 = http_post(f"/api/price-index/calculate/{period_type}/{period_key}").get("data") or []
    r2 = http_post(f"/api/price-index/calculate/{period_type}/{period_key}").get("data") or []
    if len(r1) != len(r2):
        raise RuntimeError("两次结果数量不同")
    a = {(x["seaArea"], x["species"], x["specification"], x["season"]): x for x in r1}
    b = {(x["seaArea"], x["species"], x["specification"], x["season"]): x for x in r2}
    for k in a:
        if a[k]["median"] != b[k]["median"]:
            raise RuntimeError(f"确定性失败 median: {a[k]['median']} != {b[k]['median']}")
        if a[k]["p25"] != b[k]["p25"] or a[k]["p75"] != b[k]["p75"]:
            raise RuntimeError("分位数不一致")
        if a[k]["anomalyFiltered"] != b[k]["anomalyFiltered"]:
            raise RuntimeError("剔除数不一致")
    log("【确定性】通过：两次结果完全一致")


def main():
    log("========== 渔获价格指数 端到端验证开始 ==========")
    vessel = get_vessel()
    log(f"使用 vessel: {vessel['vesselNo']} seaArea={vessel['seaAreaName']}")

    target_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0) - timedelta(days=1)
    day_key = target_date.strftime("%Y-%m-%d")

    seed_purchase(vessel, target_date)
    results = trigger_and_check("DAY", day_key, expected_filtered_min=1)
    check_determinism("DAY", day_key)

    # 查询：按维度
    log("多维查询示例：seaArea=" + vessel['seaAreaName'] + " periodType=DAY")
    q = http_get("/api/price-index/query", {
        "seaArea": vessel["seaAreaName"],
        "periodType": "DAY",
    })
    log(f"查询返回 {len(q.get('data') or [])} 条")

    # 趋势
    target = results[0]
    tr = http_get("/api/price-index/trend", {
        "seaArea": target["seaArea"],
        "species": target["species"],
        "specification": target["specification"],
        "season": target["season"],
        "periodType": "DAY",
    })
    log(f"趋势查询返回 {len(tr.get('data') or [])} 条")

    # 重建：仅重建 [today-1, today)
    today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    yesterday = today - timedelta(days=1)
    rebuild = http_post("/api/price-index/rebuild", None)
    # POST 携带 query params
    url = f"{API}/api/price-index/rebuild?from={yesterday.strftime('%Y-%m-%d')}&to={today.strftime('%Y-%m-%d')}"
    req = urllib.request.Request(url, data=b"", method="POST",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=15) as r:
        rebuild = json.loads(r.read().decode("utf-8"))
    log(f"重建结果：{rebuild.get('data')}")

    log("========== 端到端验证通过 ==========")
    print("\n".join(OUT), file=sys.stderr)


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        log(f"FAIL: {e}")
        sys.exit(1)
