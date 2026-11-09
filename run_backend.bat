@echo off
title Water Leakage Detection - Backend & Auto USB Bridge
cd /d "%~dp0"

echo ======================================================================
echo   Water Leakage Detection - FastAPI ML Backend & Auto USB Bridge
echo ======================================================================
echo.

set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

REM 1. Set up USB Bridge immediately
echo [1/3] Initializing USB Bridge for connected devices...
%ADB% reverse tcp:8000 tcp:8000 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo   [OK] USB Bridge established: Phone can connect via http://127.0.0.1:8000/
) else (
    echo   [INFO] No USB device detected yet. Auto-bridge daemon will attach when plugged in.
)

REM 2. Start minimized background daemon to keep USB reverse alive continuously
echo [2/3] Starting continuous background USB bridge daemon...
start /min "WaterLeakage-ADB-Bridge" cmd /c "for /l %%i in () do ( %ADB% reverse tcp:8000 tcp:8000 >nul 2>&1 & timeout /t 3 /nobreak >nul )"

echo.
echo ======================================================================
echo   AVAILABLE ENDPOINTS FOR YOUR PHONE:
echo   - USB Cable (Debugging): http://127.0.0.1:8000/
echo   - Wi-Fi LAN:             http://192.168.29.251:8000/
echo   - Emulator:              http://10.0.2.2:8000/
echo   - Interactive API Docs:  http://localhost:8000/docs
echo ======================================================================
echo.

REM 3. Start FastAPI Uvicorn Server
echo [3/3] Starting FastAPI Uvicorn Server on 0.0.0.0:8000...
set PYTHONPATH=.
.\.venv\Scripts\uvicorn.exe backend.app.main:app --host 0.0.0.0 --port 8000 --reload

pause
