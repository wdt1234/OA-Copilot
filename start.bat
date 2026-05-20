@echo off
chcp 65001 >nul 2>&1
powershell -command "Add-Type -Name W -Namespace N -MemberDefinition '[DllImport(\"user32.dll\")]public static extern bool ShowWindow(IntPtr h,int c);'; [N.W]::ShowWindow((Get-Process -Id $PID).MainWindowHandle,2)"

echo ========================================
echo   OA Integration Copilot - Start Script
echo ========================================
echo.

echo [1/2] Starting backend (port 8080)...
cd /d "%~dp0\backend"
start /min "OA-Backend" cmd /c "java -Dfile.encoding=UTF-8 -Dmaven.multiModuleProjectDirectory=%cd% -classpath %cd%\.mvn\wrapper\maven-wrapper.jar org.apache.maven.wrapper.MavenWrapperMain spring-boot:run"
cd /d "%~dp0"

echo       Waiting for backend to start...
timeout /t 15 /nobreak >nul

echo [2/2] Starting frontend (port 5173)...
cd /d "%~dp0\frontend"
start /min "OA-Frontend" cmd /c "npm run dev"
cd /d "%~dp0"

echo.
echo ========================================
echo   Startup complete!
echo   Backend: http://localhost:8080
echo   Frontend: http://localhost:5173
echo ========================================
echo.
