import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // base 设为 /mp/：HTTPS 聚合入口(https://域名:8443/mp/)下资源引用正确；
  // 独立 HTTP 端口(10083)下 /mp/assets 会被 nginx 重写规则去前缀，不受影响
  base: '/mp/',
  plugins: [vue()],
  server: {
    port: 5176,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
