@echo off
title Bridge Phone USB
cd /d "%~dp0"
echo Connecting phone via USB bridge (port 8000)...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Phone connected! In the app, use http://127.0.0.1:8000/
) else (
    echo [ERROR] Phone not found. Ensure USB debugging is ON and phone is unlocked.
)
timeout /t 3

