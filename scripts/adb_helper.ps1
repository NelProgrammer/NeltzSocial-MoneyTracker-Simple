param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$Action,

    [Parameter(Position=1)]
    [string]$Arg1,

    [Parameter(Position=2)]
    [string]$Arg2
)

$ScreenshotsDir = Join-Path $PSScriptRoot "..\screenshots"
if (!(Test-Path $ScreenshotsDir)) {
    New-Item -ItemType Directory -Force -Path $ScreenshotsDir | Out-Null
}

switch ($Action.ToLower()) {
    "capture" {
        $name = if ($Arg1) { $Arg1 } else { "screen_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".png" }
        if (!$name.EndsWith(".png")) { $name = "$name.png" }
        $remotePath = "/sdcard/temp_screen.png"
        $localPath = Join-Path $ScreenshotsDir $name
        
        adb shell screencap -p $remotePath
        adb pull $remotePath $localPath | Out-Null
        Write-Host "Screenshot saved: $localPath"
    }
    "tap" {
        $x = $Arg1
        $y = $Arg2
        adb shell input tap $x $y
        Write-Host "Tapped: ($x, $y)"
    }
    "tap_and_capture" {
        $x = $Arg1
        $y = $Arg2
        adb shell input tap $x $y
        Start-Sleep -Milliseconds 1200
        
        $name = "screen_" + (Get-Date -Format "yyyyMMdd_HHmmss") + ".png"
        $remotePath = "/sdcard/temp_screen.png"
        $localPath = Join-Path $ScreenshotsDir $name
        
        adb shell screencap -p $remotePath
        adb pull $remotePath $localPath | Out-Null
        Write-Host "Screenshot saved: $localPath"
    }
    "open_transactions" {
        adb shell input tap 360 2250
        Start-Sleep -Milliseconds 1000
    }
    "install_and_launch" {
        Set-Location (Join-Path $PSScriptRoot "..")
        .\gradlew installDebug
        adb shell am start -n com.moneytracker/.MainActivity
        Start-Sleep -Seconds 3
    }
    default {
        Write-Host "Unknown action: $Action"
    }
}
