import { HttpResponseError } from './http'
import { getH5Session } from './auth'

export interface UploadedFile {
  objectName: string
  name: string
  size: string
}

function resolveUploadBaseUrl(): string {
  // #ifdef MP-WEIXIN
  return 'https://drone.kfktec.cn:8443/api'
  // #endif
  // #ifndef MP-WEIXIN
  return '/api'
  // #endif
}

let cachedAccessPrefix: string | null = null

export async function fetchAccessPrefix(): Promise<string> {
  if (cachedAccessPrefix !== null) return cachedAccessPrefix
  const session = getH5Session()
  const baseUrl = resolveUploadBaseUrl()
  const headers: Record<string, string> = {}
  if (session?.token) headers.Authorization = `Bearer ${session.token}`

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${baseUrl}/upload/access-prefix?clientType=H5`,
      method: 'GET',
      header: headers,
      success: (res) => {
        const payload = res.data as { success?: boolean; message?: string; data?: { accessPrefix?: string } }
        if (!payload?.success) {
          reject(new Error(payload?.message || '获取访问前缀失败'))
          return
        }
        cachedAccessPrefix = payload.data?.accessPrefix || ''
        resolve(cachedAccessPrefix!)
      },
      fail: () => reject(new Error('获取访问前缀失败'))
    })
  })
}

function ensureHttps(url: string): string {
  // #ifdef MP-WEIXIN
  url = url.replace('http://8.137.79.139:8768', 'https://drone.kfktec.cn:8443')
  url = url.replace('http://8.135.237.224:9001', 'https://drone.kfktec.cn:8443/minio-proxy')
  url = url.replace('http://127.0.0.1:8768', 'https://drone.kfktec.cn:8443/minio')
  return url
  // #endif
  // #ifndef MP-WEIXIN
  return url
  // #endif
}

export function toImageUrl(objectName: string | null | undefined): string {
  if (!objectName) return ''
  const prefix = cachedAccessPrefix

  // H5 浏览器模式：将绝对 OSS URL 转为相对路径，走 Vite/nginx 代理
  if (process.env.UNI_PLATFORM === 'h5') {
    if (objectName.startsWith('http://') || objectName.startsWith('https://')) {
      if (prefix) {
        try {
          const origin = new URL(prefix).origin  // "http://8.137.79.139:8768"
          if (objectName.startsWith(origin)) {
            return objectName.slice(origin.length) // "/dgcp_oa/workorder/abc.jpg"
          }
        } catch { /* ignore */ }
      }
      return objectName
    }
    // 相对路径：用前缀 pathname 拼接
    if (prefix) {
      try {
        const pathname = new URL(prefix).pathname  // "/dgcp_oa/"
        return pathname + objectName               // "/dgcp_oa/workorder/abc.jpg"
      } catch { /* ignore */ }
    }
    return objectName
  }

  // 小程序及其他平台：使用完整绝对 URL
  if (objectName.startsWith('http://') || objectName.startsWith('https://')) {
    return ensureHttps(objectName)
  }
  return ensureHttps((prefix || '') + objectName)
}

export async function uploadFile(filePath: string, bizType = 'common', name = 'file'): Promise<UploadedFile> {
  // Ensure access prefix is loaded before upload so toImageUrl works immediately
  await fetchAccessPrefix()
  const session = getH5Session()
  return await new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${resolveUploadBaseUrl()}/upload`,
      filePath,
      name: 'file',
      formData: {
        bizType,
        clientType: 'H5'
      },
      header: session?.token ? { Authorization: `Bearer ${session.token}` } : {},
      success: (response) => {
        try {
          const payload = JSON.parse(response.data) as { success: boolean; message: string; data: UploadedFile }
          if (!payload.success) {
            reject(new HttpResponseError(payload.message || '上传失败', response.statusCode))
            return
          }
          resolve(payload.data)
        } catch (error) {
          reject(new HttpResponseError(error instanceof Error ? error.message : '上传失败', response.statusCode))
        }
      },
      fail: (error) => reject(new HttpResponseError(error instanceof Error ? error.message : '文件上传失败'))
    })
  })
}

/** 用于存储时：无论平台，始终返回完整绝对 URL，确保数据库保存一致的完整地址 */
export function toFullImageUrl(objectName: string | null | undefined): string {
  if (!objectName) return ''
  if (objectName.startsWith('http://') || objectName.startsWith('https://')) return objectName
  const prefix = cachedAccessPrefix || ''
  return prefix + objectName  // "http://8.137.79.139:8768/dgcp_oa/merchant/abc.jpg"
}

export function buildDownloadUrl(url: string, fileName?: string) {
  let qs = `url=${encodeURIComponent(url)}&clientType=H5`
  if (fileName) {
    qs += `&fileName=${encodeURIComponent(fileName)}`
  }
  return `${resolveUploadBaseUrl()}/upload/download?${qs}`
}
