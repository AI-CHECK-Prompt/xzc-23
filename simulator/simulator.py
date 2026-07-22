"""
近海渔船渔获数据模拟器
- 至少 100 艘船
- 连续 30 天的出海与渔获数据
- 包含船位轨迹、申报、渔获、告警、配额扣减等场景
- 兼容 JT/T 808 终端上报：调用后端 /api/position/ingest
"""
import os
import sys
import time
import json
import random
import argparse
import requests
from datetime import datetime, timedelta

API_BASE = os.environ.get("API_BASE", "http://localhost:8080")
PORTS = [
    {"name": "泉州祥芝渔港", "area": "闽南近海渔区", "lon": 118.78, "lat": 24.86},
    {"name": "福州连江黄岐渔港", "area": "闽东近海渔区", "lon": 119.89, "lat": 26.32},
    {"name": "漳州龙海港尾渔港", "area": "闽南近海渔区", "lon": 117.88, "lat": 24.45},
    {"name": "莆田湄洲岛渔港", "area": "闽中近海渔区", "lon": 119.00, "lat": 25.08},
    {"name": "宁德三沙渔港", "area": "闽东近海渔区", "lon": 120.13, "lat": 26.88},
]
SPECIES = ["带鱼", "黄花鱼", "鲳鱼", "墨鱼", "梭子蟹", "虾蛄"]
METHODS = ["刺网", "围网", "拖网", "钓具"]


def log(msg):
    print(f"[模拟器 {datetime.now().strftime('%H:%M:%S')}] {msg}", flush=True)


def http_post(path, json_body):
    url = API_BASE.rstrip("/") + path
    for i in range(5):
        try:
            r = requests.post(url, json=json_body, timeout=15)
            if r.status_code == 200:
                return r.json()
        except Exception as e:
            log(f"请求失败 {i+1}/5: {e}")
            time.sleep(2)
    return None


def http_get(path):
    url = API_BASE.rstrip("/") + path
    for i in range(3):
        try:
            r = requests.get(url, timeout=15)
            if r.status_code == 200:
                return r.json()
        except Exception as e:
            log(f"请求失败 {i+1}/3: {e}")
            time.sleep(2)
    return None


def ensure_vessel(idx, port, owner_idx):
    """创建一艘船，若已存在则复用"""
    vessel_no = f"{port['name'][:2]}-渔-{2000+idx:04d}"
    # 先查
    r = http_get(f"/api/vessel/byNo?vesselNo={vessel_no}")
    if r and r.get("code") == 0 and r.get("data"):
        return r["data"]
    body = {
        "vesselNo": vessel_no,
        "vesselName": f"{port['name']}号船{idx}",
        "ownerName": f"船东{owner_idx}-{idx % 5 + 1}",
        "captainName": f"船长{idx % 20 + 1}",
        "phone": f"138{random.randint(10000000, 99999999)}",
        "portName": port["name"],
        "seaAreaName": port["area"],
        "certValidFrom": "2024-01-01",
        "certValidTo": "2027-12-31",
        "suspended": False,
        "status": "在港"
    }
    r = http_post("/api/vessel/save", body)
    if r and r.get("code") == 0:
        return r["data"]
    return None


def submit_voyage(vessel, plan_day, plan_days, target_area):
    """提交一次出海申报"""
    now = plan_day.replace(hour=random.randint(4, 7), minute=0, second=0)
    body = {
        "vesselId": vessel["id"],
        "crewListJson": json.dumps([
            {"name": f"船员{i}", "idNo": f"3505821990{i:06d}"} for i in range(1, 4)
        ]),
        "planSeaArea": target_area,
        "planDays": plan_days,
        "planMethod": random.choice(METHODS),
        "netSpec": f"刺网 网目 50mm 长 {random.choice([600, 800, 1000])}m",
        "planDepartureTime": now.strftime("%Y-%m-%d %H:%M:%S")
    }
    r = http_post("/api/voyage/submit", body)
    if r and r.get("code") == 0:
        return r["data"]
    return None


def depart(voyage_id):
    http_post(f"/api/voyage/depart/{voyage_id}", {})


def return_port(voyage_id):
    http_post(f"/api/voyage/return/{voyage_id}", {})


def push_positions(vessel, start_time, hours, deviating=False):
    """回传连续船位，deviating=True 时偏离申报海域"""
    base_lon, base_lat = (vessel.get("_lon", 118.5), vessel.get("_lat", 24.5))
    pts = []
    for h in range(hours):
        t = start_time + timedelta(hours=h)
        # 简化轨迹：以 0.01 度/小时漂移
        lon = base_lon + (0.5 - h * 0.005) + random.uniform(-0.005, 0.005)
        lat = base_lat + (h * 0.005) + random.uniform(-0.005, 0.005)
        if deviating and h > hours * 0.6:
            lon += 1.2  # 故意偏离
            lat += 0.8
        pts.append({
            "vesselId": vessel["id"],
            "vesselNo": vessel["vesselNo"],
            "longitude": round(lon, 6),
            "latitude": round(lat, 6),
            "speed": round(random.uniform(4, 9), 2),
            "heading": round(random.uniform(0, 360), 1),
            "reportTime": t.strftime("%Y-%m-%dT%H:%M:%S")
        })
    if pts:
        http_post("/api/position/ingestBatch", pts)


def submit_catch(voyage_id, vessel):
    """归港后提交渔获预申报"""
    items = []
    total = 0
    species_count = random.randint(2, 4)
    chosen = random.sample(SPECIES, species_count)
    for s in chosen:
        w = round(random.uniform(20, 120), 2)
        items.append({
            "species": s,
            "estimatedWeight": w,
            "actualWeight": round(w * random.uniform(0.9, 1.1), 2),
            "isProtected": s == "黄花鱼" and random.random() < 0.3,
            "juvenileRatio": round(random.uniform(0, 5), 2)
        })
        total += w
    body = {
        "voyageId": voyage_id,
        "vesselId": vessel["id"],
        "itemsJson": json.dumps(items),
        "estimatedTotal": total
    }
    r = http_post("/api/catch/submitPre", body)
    if r and r.get("code") == 0:
        cd = r["data"]
        # 模拟过磅
        actual = round(total * random.uniform(0.95, 1.1), 2)
        http_post(f"/api/catch/confirmWeigh/{cd['id']}", {
            "actualTotal": actual,
            "operator": "电子秤台账",
            "reason": ""
        })


def issue_violation(vessel, voyage_id):
    """10% 概率触发违规告知书"""
    if random.random() < 0.1:
        body = {
            "vesselId": vessel["id"],
            "voyageId": voyage_id,
            "violationType": random.choice(["越界作业", "幼鱼比例超标", "未开启船位终端"]),
            "description": f"海警渔政现场检查发现 {vessel['vesselNo']} 存在违规情形",
            "quotaDeducted": round(random.uniform(50, 200), 2),
            "officerName": random.choice(["海警王警官", "渔政张队", "海警李警员"])
        }
        r = http_post("/api/enforcement/issue", body)
        if r and r.get("code") == 0:
            http_post(f"/api/enforcement/applyQuotaDeduct/{r['data']['id']}", {
                "deducted": body["quotaDeducted"]
            })


def inject_alert(vessel, voyage_id, alert_type, description, level="warn"):
    http_post("/api/alert/create", {
        "vesselId": vessel["id"],
        "vesselNo": vessel["vesselNo"],
        "voyageId": voyage_id,
        "alertType": alert_type,
        "level": level,
        "description": description
    })


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--vessels", type=int, default=100, help="模拟船舶数量")
    parser.add_argument("--days", type=int, default=30, help="连续天数")
    parser.add_argument("--voyages-per-vessel", type=int, default=4, help="每船航次数")
    parser.add_argument("--only-create-vessels", action="store_true", help="仅创建船舶")
    args = parser.parse_args()

    log(f"启动模拟：{args.vessels} 艘船 × {args.days} 天，每船 {args.voyages_per_vessel} 航次")

    # 1. 检查后端健康
    health = http_get("/actuator/health")
    log(f"后端健康检查：{health}")

    # 2. 创建船舶
    vessels = []
    for i in range(1, args.vessels + 1):
        port = random.choice(PORTS)
        owner_idx = (i - 1) // 20 + 1
        v = ensure_vessel(i, port, owner_idx)
        if v:
            vessels.append(v)
        if i % 20 == 0:
            log(f"已创建/复用船舶 {i}/{args.vessels}")
    log(f"共就绪船舶 {len(vessels)} 艘")

    if args.only_create_vessels:
        return

    # 3. 为每艘船生成航次
    today = datetime.now().replace(minute=0, second=0, microsecond=0)
    start_day = today - timedelta(days=args.days - 1)

    total_voyages = 0
    for vi, v in enumerate(vessels):
        for k in range(args.voyages_per_vessel):
            # 在 30 天内随机选一个出发日
            offset = random.randint(0, args.days - 3)
            plan_day = (start_day + timedelta(days=offset)).replace(hour=5, minute=0, second=0)
            plan_days = random.randint(1, 5)
            target_area = v["seaAreaName"] + "-" + random.choice(["近岸", "外海", "深水区"])

            voyage = submit_voyage(v, plan_day, plan_days, target_area)
            if not voyage:
                continue
            total_voyages += 1
            depart(voyage["id"])
            # 回传船位
            push_positions(v, plan_day, plan_days * 12, deviating=(k == args.voyages_per_vessel - 1))
            # 归港
            return_port(voyage["id"])
            # 渔获
            submit_catch(voyage["id"], v)
            # 违规
            issue_violation(v, voyage["id"])

            # 异常告警：最后一航次触发越界 / 终端关闭
            if k == args.voyages_per_vessel - 1:
                inject_alert(v, voyage["id"], "超出申报海域", f"{v['vesselNo']} 船位偏离申报海域 {target_area}", "danger")
            if k == 0 and random.random() < 0.5:
                inject_alert(v, voyage["id"], "关闭船位终端", f"{v['vesselNo']} 长时间无船位回传", "danger")

        if (vi + 1) % 10 == 0:
            log(f"进度 {vi+1}/{len(vessels)} 艘，累计航次 {total_voyages}")

    log(f"全部完成：{len(vessels)} 艘船，{total_voyages} 个航次")


if __name__ == "__main__":
    main()
