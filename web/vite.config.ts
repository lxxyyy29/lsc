import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        // target: 'https://drone.kfktec.cn:8768',
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true,
        proxyTimeout: 600000,
        timeout: 600000
      },
      '/minio-proxy': {
        target: 'http://8.135.237.224:9001',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/minio-proxy/, '')
      },
      '/dj-cloud-bucket': {
        target: 'http://8.135.237.224:9001',
        changeOrigin: true
      },
      '/dgcp_oa': {
        target: 'https://drone.kfktec.cn:8768',
        changeOrigin: true
      },
      '/mediamtx-proxy': {
        target: 'http://8.135.237.224:30006',
        changeOrigin: true,
        rewrite: (path: string) => path.replace(/^\/mediamtx-proxy/, '')
      }
    }
  },
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: {
        url: 'http://localhost/'
      }
    },
    globals: true,
    css: false
  }
})
