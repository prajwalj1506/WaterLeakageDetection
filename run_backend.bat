@echo off
title Water Leakage Detection - Backend & Auto Connectivity
cd /d "%~dp0"

echo ======================================================================
echo   Water Leakage Detection - FastAPI ML Backend
echo ======================================================================
echo.

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set PYTHONPATH=.

REM 1. Set up USB Bridge immediately
echo [1/3] Initializing USB Bridge for connected devices...
%ADB% reverse tcp:8000 tcp:8000 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [OK] USB Bridge established: Phone can connect via http://127.0.0.1:8000/
) else (
    echo   [INFO] No USB device detected. Non-USB Wi-Fi / Hotspot / Emulator modes ready.
)

REM 2. Start minimized background daemon to keep USB reverse alive continuously
echo [2/3] Starting background USB bridge daemon...
start /min "WaterLeakage-ADB-Bridge" cmd /c "for /l %%i in () do ( %ADB% reverse tcp:8000 tcp:8000 >nul 2>&1 & timeout /t 3 /nobreak >nul )"

echo.
echo ======================================================================
echo   CURRENT REACHABLE ENDPOINTS:
echo   - USB Cable (Debugging): http://127.0.0.1:8000/
echo   - Android Emulator:      http://10.0.2.2:8000/
.\.venv\Scripts\python.exe -c "from backend.app.services.discovery import get_local_ipv4_addresses; ips = get_local_ipv4_addresses(); [print(f'  - Wi-Fi / Hotspot LAN:   http://{ip}:8000/') for ip in ips] if ips else print('  - Wi-Fi / Hotspot LAN:   (No active Wi-Fi/LAN adapter found)')"
echo   - Interactive API Docs:  http://localhost:8000/docs
echo.
echo   TIP FOR NON-USB CONNECTION:
echo   - Tap "Auto-Detect Active Server" in the app on the same Wi-Fi/Hotspot.
echo   - If college/public Wi-Fi blocks device-to-device traffic (AP Isolation),
echo     turn on your Phone Hotspot or Windows Mobile Hotspot and connect!
echo ======================================================================
echo.

REM 3. Start FastAPI Uvicorn Server
echo [3/3] Starting FastAPI Uvicorn Server on 0.0.0.0:8000...
.\.venv\Scripts\uvicorn.exe backend.app.main:app --host 0.0.0.0 --port 8000 --reload

pause
