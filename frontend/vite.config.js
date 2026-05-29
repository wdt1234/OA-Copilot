import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import os from 'os'

// 获取本机局域网IP
function getLocalIP() {
  const interfaces = os.networkInterfaces()
  for (const name of Object.keys(interfaces)) {
    for (const iface of interfaces[name]) {
      // 跳过内部回环和非IPv4地址
      if (iface.family === 'IPv4' && !iface.internal) {
        return iface.address
      }
    }
  }
  return 'localhost'
}

const localIP = getLocalIP()
console.log(`🌐 局域网访问地址: http://${localIP}:5173`)

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    host: '0.0.0.0', // 监听所有网络接口
    proxy: {
      '/api/': {
        // 使用 localhost 作为代理目标，确保无论通过什么地址访问都能正常工作
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
