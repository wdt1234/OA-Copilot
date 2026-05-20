@echo off
chcp 65001 >nul 2>&1

echo ========================================
echo   OA Integration Copilot - Stop Script
echo ========================================
echo.

echo [1/2] Stopping backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if not errorlevel 1 echo       Stopped PID: %%a
)

echo [2/2] Stopping frontend (port 5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173 ^| findstr LISTENING') do (
    taskkill /F /PID %%a >nul 2>&1
    if not errorlevel 1 echo       Stopped PID: %%a
)

echo.
echo ========================================
echo   All services stopped
echo ========================================
echo.
ping -n 1 -w 500 127.0.0.1 >nul
