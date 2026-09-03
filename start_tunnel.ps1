# Public Internet Tunnel Starter & Auto-Sync for QPby64
$Host.UI.RawUI.WindowTitle = "QPby64 Internet Tunnel Gateway"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "     QPby64 Dedicated 1-Click Internet Tunnel Gateway          " -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Locate ADB
$adb = "C:\Users\kantr\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adbCmd = Get-Command "adb.exe" -ErrorAction SilentlyContinue
    if ($adbCmd) { $adb = $adbCmd.Source }
}

# 2. Check Device & Forward Port
Write-Host "[1/3] Checking connected Android device via ADB..." -ForegroundColor Cyan
if (Test-Path $adb) {
    $devs = & $adb devices
    Write-Host $devs -ForegroundColor DarkGray
    Write-Host "[2/3] Forwarding port 8080 (PC -> Phone)..." -ForegroundColor Cyan
    & $adb reverse --remove-all 2>$null
    & $adb forward tcp:8080 tcp:8080 2>$null
} else {
    Write-Host "[WARNING] ADB not found. Tunnel will connect to local network." -ForegroundColor Yellow
}

# 3. Locate or Download Cloudflared
$cf = Join-Path $PSScriptRoot "cloudflared.exe"
if (-not (Test-Path $cf)) {
    Write-Host "[INFO] Downloading Cloudflare tunnel binary..." -ForegroundColor Yellow
    Invoke-WebRequest -Uri "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe" -OutFile $cf
}

Write-Host "[3/3] Launching Cloudflare High-Speed Edge Tunnel..." -ForegroundColor Cyan

$procInfo = New-Object System.Diagnostics.ProcessStartInfo
$procInfo.FileName = $cf
$procInfo.Arguments = "tunnel --url http://127.0.0.1:8080"
$procInfo.RedirectStandardError = $true
$procInfo.RedirectStandardOutput = $true
$procInfo.UseShellExecute = $false
$procInfo.CreateNoWindow = $true

$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $procInfo
$proc.Start() | Out-Null

$tunnelUrl = $null
$deadline = [DateTime]::Now.AddSeconds(25)

while ([DateTime]::Now -lt $deadline -and -not $tunnelUrl) {
    if (-not $proc.StandardError.EndOfStream) {
        $line = $proc.StandardError.ReadLine()
        if ($line -match "(https://[a-zA-Z0-9-]+\.trycloudflare\.com)") {
            $tunnelUrl = $matches[1]
            break
        }
    }
    Start-Sleep -Milliseconds 100
}

if ($tunnelUrl) {
    # Copy to PC Clipboard
    try { Set-Clipboard -Value $tunnelUrl } catch {}

    # Broadcast to Phone App (using explicit component for Android 8-15 compatibility)
    if (Test-Path $adb) {
        & $adb shell am broadcast -a com.example.SET_PUBLIC_TUNNEL_URL -n com.aistudio.questionbank.v1.agqpby64/com.example.service.TunnelUrlReceiver --es url "$tunnelUrl" | Out-Null
    }

    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host "         PUBLIC INTERNET TUNNEL IS ACTIVE & SYNCED!             " -ForegroundColor Yellow
    Write-Host "================================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host " PUBLIC URL:  " -NoNewline -ForegroundColor White
    Write-Host "$tunnelUrl" -ForegroundColor Cyan
    Write-Host " STATUS:      " -NoNewline -ForegroundColor White
    Write-Host "COPIED TO CLIPBOARD + AUTO-CONFIGURED IN PHONE APP" -ForegroundColor Green
    Write-Host ""
    Write-Host " -------------------- ACTIVE PUBLIC LINKS --------------------" -ForegroundColor DarkGray
    Write-Host "  1. Live Exam Portal:  $tunnelUrl/livetest" -ForegroundColor White
    Write-Host "  2. Results Portal:    $tunnelUrl/results" -ForegroundColor White
    Write-Host "  3. Supervisor Portal: $tunnelUrl/admin" -ForegroundColor White
    Write-Host "  4. Analytics Portal:  $tunnelUrl/dashboard" -ForegroundColor White
    Write-Host " -------------------------------------------------------------" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "Press Ctrl+C anytime in this window to stop the tunnel." -ForegroundColor DarkYellow
    Write-Host ""

    # Keep alive and show any incoming requests / logs
    while (-not $proc.HasExited) {
        if (-not $proc.StandardError.EndOfStream) {
            $l = $proc.StandardError.ReadLine()
            if ($l -match "HTTP/\d" -or $l -match "error" -or $l -match "connIndex") {
                Write-Host $l -ForegroundColor DarkGray
            }
        }
        Start-Sleep -Milliseconds 200
    }
} else {
    Write-Host "[ERROR] Could not obtain tunnel URL from Cloudflare." -ForegroundColor Red
    Write-Host "Make sure you have an active internet connection." -ForegroundColor Yellow
    $proc.Kill()
}
