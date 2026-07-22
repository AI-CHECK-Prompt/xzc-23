@echo off
REM Windows 一键启动脚本
echo ===========================================
echo  近海渔船渔获登记与配额管控平台 - 一键启动
echo ===========================================
cd /d %~dp0

docker compose down -v 2>nul
docker compose up -d --build
if errorlevel 1 goto :error

echo 等待后端就绪...
:wait_backend
curl -fsS http://localhost:8080/actuator/health >nul 2>&1
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto :wait_backend
)
echo 后端已就绪

echo 启动数据模拟器（100艘船 x 30天）...
docker compose run --rm simulator python simulator.py --vessels 100 --days 30

echo ===========================================
echo 启动完成！
echo   管理端: http://localhost:8081
echo   移动端: http://localhost:8082
echo   API根 : http://localhost:8080/api
echo ===========================================
exit /b 0

:error
echo Docker 启动失败，请确认 Docker Desktop 已运行
exit /b 1
