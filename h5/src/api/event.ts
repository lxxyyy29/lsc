import { getH5Session } from './auth'
import { HttpResponseError } from './http'

export interface EventDetail {
  id: number
  title: string
  eventType: string
  sourceSystem: string
  location: string
  longitude: number
  latitude: number
  occurredAt: string
  evidenceReferences: string[]
}

interface BackendEventDetail {
  id?: number | null
  title?: string | null
  eventType?: string | null
  sourceSystem?: string | null
  location?: string | null
  longitude?: number | string | null
  latitude?: number | string | null
  occurredAt?: string | null
  evidenceReferences?: string[] | null
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

/**
 * The event API lives under /api/events (shared), not /api/h5/events.
 * We derive the shared base from the H5 base URL.
 */
function resolveSharedApiBaseUrl(): string {
  const envValue = (globalThis as { __H5_API_BASE_URL__?: string }).__H5_API_BASE_URL__
  let h5Base: string
  if (typeof envValue === 'string' && envValue.trim().length > 0) {
    h5Base = envValue.trim()
  } else {
    // #ifdef MP-WEIXIN
    h5Base = 'https://drone.kfktec.cn:8768/api/h5'
    // #endif
    // #ifndef MP-WEIXIN
    h5Base = '/api/h5'
    // #endif
  }
  return h5Base.replace(/\/h5\/?$/, '')
}

/** H5 浏览器：将 MinIO 绝对 URL 转为相对代理路径，避免跨域；小程序保持原 URL */
function proxyMinioUrl(url: string): string {
  if (process.env.UNI_PLATFORM === 'h5') {
    const MINIO_ORIGIN = 'http://8.135.237.224:9001'
    if (url.startsWith(MINIO_ORIGIN)) {
      return '/minio-proxy' + url.substring(MINIO_ORIGIN.length)
    }
  }
  return url
}

function mapEventDetail(data: BackendEventDetail): EventDetail {
  return {
    id: Number(data.id ?? 0),
    title: data.title || '',
    eventType: data.eventType || '',
    sourceSystem: data.sourceSystem || '',
    location: data.location || '',
    longitude: Number(data.longitude ?? 0),
    latitude: Number(data.latitude ?? 0),
    occurredAt: data.occurredAt || '',
    evidenceReferences: Array.isArray(data.evidenceReferences)
      ? data.evidenceReferences.map(proxyMinioUrl)
      : []
  }
}

export async function getEventDetail(id: number): Promise<EventDetail | undefined> {
  if (!Number.isFinite(id) || id <= 0) return undefined

  const baseUrl = resolveSharedApiBaseUrl()
  const url = `${baseUrl}/events/${id}`
  const session = getH5Session()
  const headers: Record<string, string> = {}
  if (session?.token) {
    headers.Authorization = `Bearer ${session.token}`
  }

  // #ifdef MP-WEIXIN
  return new Promise((resolve) => {
    uni.request({
      url,
      method: 'GET',
      header: headers,
      success: (response) => {
        const payload = response.data as ApiResponse<BackendEventDetail>
        if (payload?.success && payload.data) {
          resolve(mapEventDetail(payload.data))
        } else {
          resolve(undefined)
        }
      },
      fail: () => {
        resolve(undefined)
      }
    })
  })
  // #endif

  // #ifndef MP-WEIXIN
  try {
    const response = await fetch(url, { headers })
    if (!response.ok) return undefined
    const payload: ApiResponse<BackendEventDetail> = await response.json()
    if (payload?.success && payload.data) {
      return mapEventDetail(payload.data)
    }
    return undefined
  } catch {
    return undefined
  }
  // #endif
}
