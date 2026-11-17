@echo off
title TrailMind launcher
set "ROOT=C:\D_Drive_Backup\PROJECTS\trailmind"

echo.
echo [1/5] MongoDB
powershell -NoProfile -Command "if ((Test-NetConnection localhost -Port 27017 -WarningAction SilentlyContinue).TcpTestSucceeded) { exit 0 } else { exit 1 }"
if errorlevel 1 (echo   WARNING: MongoDB is not answering on port 27017. Start the MongoDB service in services.msc, then run this again.) else (echo   OK)

echo [2/5] ML service
start "TrailMind ML service" /D "%ROOT%\ml-service" cmd /k "venv\Scripts\python.exe -m uvicorn app:app --port 8000"
echo [3/5] Backend
start "TrailMind backend" /D "%ROOT%\backend" cmd /k "npm start"
echo [4/5] Tunnel
start "TrailMind tunnel" cmd /k ""C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel run trailmind"

echo [5/5] Waiting for everything to answer (up to 3 minutes each)...
call :wait "http://localhost:8000/health" "ML service"
call :wait "http://localhost:5000/api/health" "Backend"
call :wait "https://trailmind.pallavkafle.com.np/api/health" "Public address"
echo.
echo Done. Keep the three windows open. Public address: https://trailmind.pallavkafle.com.np
pause
exit /b

:wait
powershell -NoProfile -Command "$u='%~1'; for($i=0;$i -lt 60;$i++){ try { if((Invoke-WebRequest -UseBasicParsing $u -TimeoutSec 5).StatusCode -eq 200){ exit 0 } } catch {}; Start-Sleep 3 }; exit 1"
if errorlevel 1 (echo   FAILED: %~2 did not answer at %~1) else (echo   OK: %~2)
exit /b