import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  define: {
    // sockjs-client 在浏览器中需要 Node.js 的 global 对象
    global: 'window',
  },
  server: {
    proxy: {
      '/api': {
        // target: 'http://127.0.0.1:10081',
        
        // target: 'https://drone.kfktec.cn:8443',
        target: 'http://192.168.1.6:8080',
        changeOrigin: true,
        ws: true,
      }
    }
  }
})
