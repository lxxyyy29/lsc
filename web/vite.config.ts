import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  // 代理目标：默认生产 docker 映射端口 10081；
  // 本地开发（mvnw 起后端 8080）时在 web/.env.local 配置 VITE_PROXY_TARGET=http://127.0.0.1:8080 覆盖，
  // .env.local 已被 .gitignore 忽略，不会误提交，避免反复切换端口。
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:10081'

  return {
    plugins: [vue()],
    define: {
      // sockjs-client 在浏览器中需要 Node.js 的 global 对象
      global: 'window',
    },
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          ws: true,
        }
      }
    }
  }
})
