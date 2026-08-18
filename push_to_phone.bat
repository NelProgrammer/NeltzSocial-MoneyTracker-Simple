@echo off
setlocal
echo ================================================
echo  MoneyTracker - Push APK to Android Phone
echo ================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0push_to_phone.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Push to phone encountered an issue.
    pause
) else (
    echo.
    echo [SUCCESS] App pushed and running on your phone!
    timeout /t 3 >nul
)
