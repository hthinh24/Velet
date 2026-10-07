@echo off
REM Mo dashboard Knowledge Graph cho project Velet (khong can qua AI).
REM Yeu cau: Node.js + pnpm da cai; plugin understand-anything da checkout.
setlocal
set GRAPH_DIR=%~dp0..
set DASHBOARD_DIR=C:\Users\Hung Thinh\.understand-anything\repo\understand-anything-plugin\packages\dashboard
if not exist "%GRAPH_DIR%\.ua\knowledge-graph.json" (
  echo [LOI] Chua co knowledge-graph.json. Chay /understand trong AI truoc.
  pause
  exit /b 1
)
cd /d "%DASHBOARD_DIR%"
echo Dang khoi dong dashboard...
echo Xem URL co ?token= trong cua so nay, roi mo tren trinh duyet.
npx vite --host 127.0.0.1 --port 5173 --open
