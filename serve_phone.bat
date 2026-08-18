@echo off
setlocal
echo ==========================================================
echo  MoneyTracker - Wi-Fi APK Download Server for Phone
echo ==========================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve_phone.ps1"

pause
