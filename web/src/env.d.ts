declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}

declare module '@amap/amap-jsapi-loader' {
  const AMapLoader: {
    load(options: { key: string; version?: string; plugins?: string[] }): Promise<any>
  }
  export default AMapLoader
}
