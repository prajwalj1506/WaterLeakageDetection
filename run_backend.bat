@echo off
title Water Leakage Detection - Backend & USB Bridge
cd /d "%~dp0"

echo ========================================================
echo   Water Leakage Detection - Backend Startup
echo ========================================================
echo.

echo [1/2] Connecting phone via USB bridge (port 8000)...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
if %ERRORLEVEL% EQU 0 (
    echo   - USB Bridge active: Phone can reach backend at http://localhost:8000/
) else (
    echo   - Note: Phone not detected via ADB. If testing over Wi-Fi, use your PC's IP.
)
echo.

echo [2/2] Starting FastAPI ML Server...
set PYTHONPATH=.
.\.venv\Scripts\uvicorn.exe backend.app.main:app --host 0.0.0.0 --port 8000 --reload

pause

