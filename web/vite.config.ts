import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  // 代理目标：默认生产 docker 映射端口 9072；
  // 本地开发（mvnw 起后端 8080）时在 web/.env.local 配置 VITE_PROXY_TARGET=http://127.0.0.1:8080 覆盖，
  // .env.local 已被 .gitignore 忽略，不会误提交，避免反复切换端口。
  // 用 '.' 而不是 process.cwd()：npm 脚本的 cwd 就是项目根，两者等价，
  // 但可以避免为此引入 @types/node（tsconfig 的 types 只有 vite/client，
  // 引用 process 会让 vue-tsc 报 TS2591，进而让 npm run build 中断）
  const env = loadEnv(mode, '.', '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://127.0.0.1:9072'

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
