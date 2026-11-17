@echo off
taskkill /FI "WINDOWTITLE eq TrailMind ML service*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq TrailMind backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq TrailMind tunnel*" /T /F >nul 2>&1
echo TrailMind services stopped (MongoDB keeps running).
pause