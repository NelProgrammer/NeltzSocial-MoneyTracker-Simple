# PowerShell Script: Build & Push MoneyTracker APK to Connected Android Phone
$ErrorActionPreference = "Stop"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host " MoneyTracker - Push APK to Android Phone" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan

# 1. Locate ADB
$adbPath = "adb"
if (-not (Get-Command "adb" -ErrorAction SilentlyContinue)) {
    $sdkAdb = "C:\Users\Drow\AppData\Local\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdkAdb) {
        $adbPath = $sdkAdb
    } else {
        Write-Host "Error: Could not find adb.exe. Please ensure Android SDK platform-tools is installed." -ForegroundColor Red
        exit 1
    }
}

# 2. Query Connected Devices
Write-Host "`n[1/4] Detecting connected Android devices..." -ForegroundColor Yellow

$devicesRaw = & $adbPath devices -l
Write-Host $devicesRaw -ForegroundColor DarkGray

$allLines = $devicesRaw -split "`r?`n" | Where-Object { $_.Trim() -ne "" -and $_ -notmatch "^List of devices" }
$unauthorized = $allLines | Where-Object { $_ -match "\bunauthorized\b" }

if ($unauthorized) {
    Write-Host "`n⚠️ Samsung Phone is UNAUTHORIZED:" -ForegroundColor Red
    Write-Host "  Please unlock your Samsung phone screen now and tap 'Allow USB Debugging' (check 'Always allow')." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
    $devicesRaw = & $adbPath devices -l
    $allLines = $devicesRaw -split "`r?`n" | Where-Object { $_.Trim() -ne "" -and $_ -notmatch "^List of devices" }
}

$activeLines = $allLines | Where-Object { $_ -match "\bdevice\b" }
$physicalDevices = @()
$emulatorDevices = @()

foreach ($line in $activeLines) {
    $parts = $line -split "\s+"
    if ($parts.Count -ge 2) {
        $serial = $parts[0]
        $model = if ($line -match "model:(\S+)") { $matches[1] } else { $serial }
        $deviceInfo = [PSCustomObject]@{
            Serial = $serial
            Model  = $model
            Raw    = $line
        }
        if ($serial -like "emulator-*") {
            $emulatorDevices += $deviceInfo
        } else {
            $physicalDevices += $deviceInfo
        }
    }
}

if ($physicalDevices.Count -eq 0) {
    Write-Host "`n[Wireless Debugging Setup]" -ForegroundColor Cyan
    Write-Host "On your Samsung phone: Settings > Developer options > Wireless debugging" -ForegroundColor Gray
    Write-Host "Look at 'IP address & Port' (e.g., 192.168.1.50:41235)" -ForegroundColor Gray
    
    $ipPort = Read-Host "`nEnter Phone Wireless IP:Port (or press Enter to skip)"
    if ($ipPort -and $ipPort.Trim() -ne "") {
        $ipPort = $ipPort.Trim()
        Write-Host "Connecting to $ipPort..." -ForegroundColor Yellow
        $connectRes = & $adbPath connect $ipPort 2>&1
        Write-Host $connectRes
        Start-Sleep -Milliseconds 1000
        
        $devicesRaw = & $adbPath devices -l
        $allLines = $devicesRaw -split "`r?`n" | Where-Object { $_.Trim() -ne "" -and $_ -notmatch "^List of devices" }
        $activeLines = $allLines | Where-Object { $_ -match "\bdevice\b" }
        
        $physicalDevices = @()
        foreach ($line in $activeLines) {
            $parts = $line -split "\s+"
            if ($parts.Count -ge 2) {
                $serial = $parts[0]
                $model = if ($line -match "model:(\S+)") { $matches[1] } else { $serial }
                if ($serial -notlike "emulator-*") {
                    $physicalDevices += [PSCustomObject]@{ Serial = $serial; Model = $model; Raw = $line }
                }
            }
        }
    }
}

if ($physicalDevices.Count -eq 0) {
    Write-Host "`n⚠️ No physical phone (Samsung) connected to ADB!" -ForegroundColor Red
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "  1. Is your phone on the same Wi-Fi network as this PC?"
    Write-Host "  2. Wireless debugging is toggled ON in Samsung Developer options."
    Write-Host "  3. You entered the exact current IP and Port shown on your phone screen."
    
    if ($emulatorDevices.Count -gt 0) {
        Write-Host "`nOnly an Android Emulator ($($emulatorDevices[0].Serial)) was found running." -ForegroundColor Cyan
        $useEmulator = Read-Host "Install to Emulator instead? (y/N)"
        if ($useEmulator -notmatch "^[yY]") {
            Write-Host "Aborted. Please connect your Samsung phone and try again." -ForegroundColor Yellow
            exit 1
        }
        $targetDevice = $emulatorDevices[0]
    } else {
        exit 1
    }
} else {
    $targetDevice = $physicalDevices[0]
    Write-Host "`n-> Target Samsung Phone: $($targetDevice.Model) ($($targetDevice.Serial))" -ForegroundColor Green
}

$targetSerial = $targetDevice.Serial

# 3. Build APK
Write-Host "`n[2/4] Building latest Debug APK..." -ForegroundColor Yellow
$gradleCmd = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradlew" }
& $gradleCmd assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Please check error output above." -ForegroundColor Red
    exit 1
}

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "Error: APK not found at $apkPath" -ForegroundColor Red
    exit 1
}
Write-Host "APK Built successfully: $apkPath" -ForegroundColor Green

# 4. Install APK onto Selected Phone (preserves user data)
Write-Host "`n[3/4] Installing APK onto $($targetDevice.Model) ($targetSerial)..." -ForegroundColor Yellow
$installOutput = & $adbPath -s $targetSerial install -r -t -d -g $apkPath 2>&1
Write-Host $installOutput

if ($installOutput -notmatch "Success" -and $LASTEXITCODE -ne 0) {
    Write-Host "`nInstallation failed." -ForegroundColor Red
    Write-Host "Troubleshooting on Samsung phones:" -ForegroundColor Yellow
    Write-Host "  1. Unlock your phone screen and check for any 'Allow USB debugging?' or 'Verify Apps' popups."
    Write-Host "  2. In Samsung Settings > Security and privacy > Auto Blocker: temporarily toggle OFF if blocking USB installs."
    Write-Host "  3. In Settings > Developer options > Ensure 'USB debugging' is ON and 'Install via USB' is ALLOWED."
    exit 1
}
Write-Host "Installation Successful!" -ForegroundColor Green

# 5. Force-stop previous instance and launch fresh app
Write-Host "`n[4/4] Restarting MoneyTracker on $($targetDevice.Model)..." -ForegroundColor Yellow
& $adbPath -s $targetSerial shell am force-stop com.moneytracker
Start-Sleep -Milliseconds 500
& $adbPath -s $targetSerial shell am start -n com.moneytracker/.MainActivity

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host " Done! MoneyTracker is now updated and running on your Samsung phone." -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
