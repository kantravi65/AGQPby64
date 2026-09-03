@echo off
title QPby64 Internet Tunnel Gateway
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start_tunnel.ps1"
if %errorlevel% neq 0 (
    echo.
    echo Press any key to close...
    pause >nul
)
