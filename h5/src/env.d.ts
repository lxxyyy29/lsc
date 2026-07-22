declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

declare module '*.svg' {
  const src: string
  export default src
}

interface ImportMetaEnv {
  readonly VITEST?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare const uni: {
  request?: (options: {
    url: string
    method: 'GET' | 'POST'
    data?: unknown
    header?: Record<string, string>
    success: (response: { statusCode: number; data: unknown }) => void
    fail: (error: unknown) => void
  }) => void
  getStorageSync?: (key: string) => unknown
  setStorageSync?: (key: string, value: unknown) => void
  removeStorageSync?: (key: string) => void
  switchTab?: (options: { url: string }) => void
  navigateTo?: (options: { url: string }) => void
  redirectTo?: (options: { url: string }) => void
  reLaunch?: (options: { url: string }) => void
  navigateBack?: (options?: { delta?: number }) => void
  showToast?: (options: { title: string; icon?: 'none' | 'success' | 'error' }) => void
}
