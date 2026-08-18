# PowerShell Script: Build & Serve MoneyTracker APK over Local Wi-Fi for Direct Phone Download
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " MoneyTracker - Wi-Fi APK Download Server for Phone" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Build APK
Write-Host "`n[1/2] Building latest Debug APK..." -ForegroundColor Yellow
$gradleCmd = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradlew" }
& $gradleCmd assembleDebug

$apkPath = (Resolve-Path "app\build\outputs\apk\debug\app-debug.apk").Path
if (-not (Test-Path $apkPath)) {
    Write-Host "Error: APK not found at $apkPath" -ForegroundColor Red
    exit 1
}
Write-Host "APK Built successfully: $apkPath" -ForegroundColor Green

# 2. Find Local Wi-Fi IPv4 Address
$localIp = (Get-NetIPAddress -AddressFamily IPv4 -InterfaceAlias "*Wi-Fi*", "*Ethernet*" | Where-Object { $_.IPAddress -match "^(192\.168|10\.|172\.(1[6-9]|2[0-9]|3[0-1]))" } | Select-Object -First 1).IPAddress
if (-not $localIp) {
    $localIp = "localhost"
}

$port = 8080
$downloadUrl = "http://${localIp}:${port}/"

Write-Host "`n[2/2] Starting Local Download Server..." -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " OPEN THIS URL IN YOUR PHONE'S BROWSER (Chrome, etc.):" -ForegroundColor Cyan
Write-Host " -> $downloadUrl" -ForegroundColor Yellow
Write-Host " (Ensure your phone and PC are connected to the same Wi-Fi)" -ForegroundColor DarkGray
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Press Ctrl+C in this window to stop the server when done.`n" -ForegroundColor DarkGray

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://*:${port}/")
try {
    $listener.Start()
} catch {
    $listener = New-Object System.Net.HttpListener
    $listener.Prefixes.Add("http://localhost:${port}/")
    $listener.Start()
}

while ($listener.IsListening) {
    $context = $listener.GetContext()
    $request = $context.Request
    $response = $context.Response

    if ($request.Url.AbsolutePath -eq "/app-debug.apk" -or $request.Url.AbsolutePath -eq "/download") {
        Write-Host "Sending APK to phone ($($request.RemoteEndPoint.Address))..." -ForegroundColor Green
        $bytes = [System.IO.File]::ReadAllBytes($apkPath)
        $response.ContentType = "application/vnd.android.package-archive"
        $response.AddHeader("Content-Disposition", "attachment; filename=MoneyTracker.apk")
        $response.ContentLength64 = $bytes.Length
        $response.OutputStream.Write($bytes, 0, $bytes.Length)
        $response.OutputStream.Close()
        Write-Host "APK sent successfully!" -ForegroundColor Green
    } else {
        $html = @"
<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Download MoneyTracker APK</title>
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #121212; color: #fff; text-align: center; padding: 40px 20px; }
        .card { background: #1e1e1e; border-radius: 16px; padding: 24px; max-width: 400px; margin: 0 auto; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
        h1 { color: #4CAF50; font-size: 24px; margin-bottom: 8px; }
        p { color: #aaa; font-size: 14px; margin-bottom: 24px; }
        .btn { display: inline-block; background: #4CAF50; color: #fff; font-weight: bold; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-size: 16px; transition: background 0.2s; }
        .btn:hover { background: #43A047; }
    </style>
</head>
<body>
    <div class="card">
        <h1>MoneyTracker</h1>
        <p>Latest build with new features & charts</p>
        <a class="btn" href="/download">Download & Install APK</a>
    </div>
</body>
</html>
"@
        $buffer = [System.Text.Encoding]::UTF8.GetBytes($html)
        $response.ContentType = "text/html"
        $response.ContentLength64 = $buffer.Length
        $response.OutputStream.Write($buffer, 0, $buffer.Length)
        $response.OutputStream.Close()
    }
}
