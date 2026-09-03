@echo off
setlocal enabledelayedexpansion
title QPby64 Internet Tunnel Gateway - Setup Wizard

echo ================================================================
echo       QPby64 Internet Tunnel Gateway - Windows Installer
echo ================================================================
echo.
echo Installing dedicated Windows application for QPby64 Internet Tunnel...
echo.

set "INSTALL_DIR=%LOCALAPPDATA%\QPby64TunnelGateway"
set "CSC_EXE=C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
set "ADB_DIR=%LOCALAPPDATA%\Android\Sdk\platform-tools"
if not exist "%ADB_DIR%" set "ADB_DIR=C:\Users\kantr\AppData\Local\Android\Sdk\platform-tools"

:: 1. Compile Executable if not present or newer
echo [1/5] Compiling native Windows GUI executable...
if not exist "%~dp0QPby64TunnelGateway.exe" (
    "%CSC_EXE%" /target:winexe /optimize+ /r:System.Windows.Forms.dll,System.Drawing.dll,System.dll /out:"%~dp0QPby64TunnelGateway.exe" "%~dp0QPby64TunnelGateway.cs"
    if %errorlevel% neq 0 (
        echo [ERROR] Compilation failed. Please ensure .NET Framework 4.0/4.5 is installed.
        pause
        exit /b 1
    )
)
echo      Compilation OK!

:: 2. Create Destination Folder
echo [2/5] Creating installation directory: %INSTALL_DIR%
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

:: 3. Copy Application Files
echo [3/5] Installing application components...
copy /Y "%~dp0QPby64TunnelGateway.exe" "%INSTALL_DIR%\" >nul
if exist "%~dp0cloudflared.exe" (
    copy /Y "%~dp0cloudflared.exe" "%INSTALL_DIR%\" >nul
) else (
    echo      Downloading cloudflared edge binary...
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe' -OutFile '%INSTALL_DIR%\cloudflared.exe'"
)

:: Copy ADB components for 100% standalone execution
if exist "%ADB_DIR%\adb.exe" copy /Y "%ADB_DIR%\adb.exe" "%INSTALL_DIR%\" >nul
if exist "%ADB_DIR%\AdbWinApi.dll" copy /Y "%ADB_DIR%\AdbWinApi.dll" "%INSTALL_DIR%\" >nul
if exist "%ADB_DIR%\AdbWinUsbApi.dll" copy /Y "%ADB_DIR%\AdbWinUsbApi.dll" "%INSTALL_DIR%\" >nul

:: 4. Create Desktop & Start Menu Shortcuts using PowerShell
echo [4/5] Creating Desktop and Start Menu shortcuts...
powershell -NoProfile -Command ^
  "$ws = New-Object -ComObject WScript.Shell; " ^
  "$desk = [Environment]::GetFolderPath('Desktop'); " ^
  "$scDesk = $ws.CreateShortcut((Join-Path $desk 'QPby64 Internet Tunnel.lnk')); " ^
  "$scDesk.TargetPath = '%INSTALL_DIR%\QPby64TunnelGateway.exe'; " ^
  "$scDesk.WorkingDirectory = '%INSTALL_DIR%'; " ^
  "$scDesk.Description = 'QPby64 Internet Tunnel Gateway for Live Examination & Portals'; " ^
  "$scDesk.Save(); " ^
  "$programs = [Environment]::GetFolderPath('Programs'); " ^
  "$scProg = $ws.CreateShortcut((Join-Path $programs 'QPby64 Internet Tunnel.lnk')); " ^
  "$scProg.TargetPath = '%INSTALL_DIR%\QPby64TunnelGateway.exe'; " ^
  "$scProg.WorkingDirectory = '%INSTALL_DIR%'; " ^
  "$scProg.Description = 'QPby64 Internet Tunnel Gateway for Live Examination & Portals'; " ^
  "$scProg.Save();"

:: 5. Create Uninstaller
echo [5/5] Generating uninstaller...
(
    echo @echo off
    echo title Uninstall QPby64 Internet Tunnel Gateway
    echo echo Removing QPby64 Internet Tunnel Gateway...
    echo del "%USERPROFILE%\Desktop\QPby64 Internet Tunnel.lnk" 2^>nul
    echo del "%APPDATA%\Microsoft\Windows\Start Menu\Programs\QPby64 Internet Tunnel.lnk" 2^>nul
    echo rd /s /q "%INSTALL_DIR%" 2^>nul
    echo echo Uninstallation complete!
    echo pause
) > "%INSTALL_DIR%\Uninstall.bat"

echo.
echo ================================================================
echo     INSTALLATION SUCCESSFUL!
echo ================================================================
echo.
echo Application installed to: %INSTALL_DIR%
echo Shortcuts created on your Desktop and in the Windows Start Menu!
echo.
set /p LAUNCH="Do you want to launch QPby64 Internet Tunnel Gateway now? (Y/N): "
if /i "%LAUNCH%"=="Y" (
    start "" "%INSTALL_DIR%\QPby64TunnelGateway.exe"
)
exit /b 0
