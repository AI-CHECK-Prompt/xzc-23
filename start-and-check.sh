#!/usr/bin/env bash
# 一键启动 + 自检
# 用法: bash start-and-check.sh
set -e

echo "==========================================="
echo "近海渔船渔获登记与配额管控平台 - 一键启动"
echo "==========================================="

cd "$(dirname "$0")"

# 检查 Docker
if ! command -v docker >/dev/null 2>&1; then
  echo "[错误] 未检测到 Docker，请先安装 Docker 与 docker-compose"
  exit 1
fi

echo "[1/4] 启动容器栈（首次将自动构建镜像）..."
docker compose down -v 2>/dev/null || true
docker compose up -d --build

echo "[2/4] 等待后端就绪..."
for i in {1..60}; do
  if curl -fsS http://localhost:8080/actuator/health >/dev/null 2>&1; then
    echo "后端已就绪（耗时 ${i}s）"
    break
  fi
  sleep 2
done

echo "[3/4] 等待前端就绪..."
for i in {1..30}; do
  if curl -fsS -I http://localhost:8081/ >/dev/null 2>&1; then
    echo "管理端已就绪"
    break
  fi
  sleep 2
done

echo "[4/4] 启动数据模拟器（100艘船 × 30天）..."
docker compose run --rm simulator python simulator.py --vessels 100 --days 30

echo "==========================================="
echo "启动完成！访问以下地址："
echo "  管理端  : http://localhost:8081"
echo "  移动端  : http://localhost:8082"
echo "  API 根  : http://localhost:8080/api"
echo "==========================================="

echo
echo "[自检] 关键指标："
curl -sS http://localhost:8080/api/selfcheck/overview | python3 -m json.tool 2>/dev/null || true
