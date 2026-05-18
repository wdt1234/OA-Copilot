后端：
netstat -ano | findstr :8080
taskkill /F /PID <进程号>


cd C:\Users\ah79h\Desktop\OA_Integration_Copilot\backend
.\mvnw.ps1 spring-boot:run
前端：
cd C:\Users\ah79h\Desktop\OA_Integration_Copilot\frontend
npm run dev

后端端口 8080，前端端口 5173（默认 Vite）。