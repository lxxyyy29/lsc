import { defineConfig, type AliasOptions, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import uniModule from '@dcloudio/vite-plugin-uni'
import { preJs } from '@dcloudio/uni-cli-shared'

type UniPluginFactory = (options?: unknown) => PluginOption

type UniModuleShape = {
  default?: { default?: UniPluginFactory } | UniPluginFactory
}

const uni =
  ((uniModule as unknown as UniModuleShape).default && typeof (uniModule as unknown as UniModuleShape).default === 'object'
    ? ((uniModule as unknown as { default: { default?: UniPluginFactory } }).default.default as UniPluginFactory | undefined)
    : ((uniModule as unknown as { default?: UniPluginFactory }).default as UniPluginFactory | undefined)) ??
  (uniModule as unknown as UniPluginFactory)

if (typeof uni !== 'function') {
  throw new Error('Failed to resolve @dcloudio/vite-plugin-uni plugin function')
}

export default defineConfig(({ mode }) => {
  const isTest = mode === 'test' || process.env.VITEST === 'true'

  // 测试环境不走 uni 插件，条件编译注释不会被剥离；
  // 此处按 H5 平台语义对 js/ts 做预处理（#ifdef MP-WEIXIN 移除、#ifndef 保留），
  // 避免 mp 分支与 h5 分支重复声明/导出导致 esbuild 报错。
  const stripUniConditional = (): PluginOption => ({
    name: 'uni:test-strip-conditional',
    enforce: 'pre',
    transform(code, id) {
      if (id.includes('node_modules') || !/\.(m?[jt]s)$/.test(id)) return null
      const stripped = preJs(code, id)
      return stripped === code ? null : { code: stripped, map: null }
    }
  })

  const alias: AliasOptions = isTest
    ? []
    : [
        { find: 'vue', replacement: '@dcloudio/uni-h5-vue' },
        { find: 'vue/package.json', replacement: '@dcloudio/uni-h5-vue/package.json' }
      ]

  const isH5 = process.env.UNI_PLATFORM === 'h5'

  return {
    base: isH5 ? '/h5/' : '/',
    plugins: [isTest ? vue() : uni()].concat(isTest ? [stripUniConditional()] : []),
    // sockjs-client 在浏览器中需要 Node.js 的 global 对象
    define: {
      global: 'window',
    },
    resolve: {
      alias
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          ws: true,
        },
        // 原生 WebSocket 端点（H5 端 STOMP 直连）
        '/ws-native': {
          target: 'ws://localhost:8080',
          changeOrigin: true,
          ws: true,
        },
        '/dgcp_oa': {
          target: 'https://drone.kfktec.cn:8768',
          changeOrigin: true
        },
        '/minio-proxy': {
          target: 'http://8.135.237.224:9001',
          changeOrigin: true,
          rewrite: (path: string) => path.replace(/^\/minio-proxy/, '')
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
  }
})
