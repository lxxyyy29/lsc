import { defineConfig, type AliasOptions, type PluginOption } from 'vite'
import vue from '@vitejs/plugin-vue'
import uniModule from '@dcloudio/vite-plugin-uni'

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
  const alias: AliasOptions = isTest
    ? []
    : [
        { find: 'vue', replacement: '@dcloudio/uni-h5-vue' },
        { find: 'vue/package.json', replacement: '@dcloudio/uni-h5-vue/package.json' }
      ]

  const isH5 = process.env.UNI_PLATFORM === 'h5'

  return {
    base: isH5 ? '/h5/' : '/',
    plugins: [isTest ? vue() : uni()],
    resolve: {
      alias
    },
    server: {
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true
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
