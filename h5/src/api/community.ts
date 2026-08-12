import { getH5Session } from './auth'

// #ifndef MP-WEIXIN
import axios from 'axios'

// H5 端调用 Web API（需要绕过 /api/h5 前缀，使用 /api 前缀）
const webApi = axios.create({ baseURL: '/api' })
webApi.interceptors.request.use((config) => {
  const session = getH5Session()
  if (session?.token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})
// #endif

// #ifdef MP-WEIXIN
/** 小程序端：调用 /api 前缀的 Web API（H5 专用接口在小程序同样使用），走 uni.request */
function resolveWebApiBase(): string {
  return 'https://drone.kfktec.cn:8443/api'
}

function requestWeb<T>(method: 'GET' | 'POST' | 'PUT' | 'DELETE', url: string, data?: unknown): Promise<T> {
  return new Promise((resolve, reject) => {
    const session = getH5Session()
    const headers: Record<string, string> = {}
    if (session?.token) headers.Authorization = `Bearer ${session.token}`
    uni.request({
      url: `${resolveWebApiBase()}${url}`,
      method,
      data: data as any,
      header: headers,
      success: (res) => {
        const payload = res.data as { success?: boolean; data?: T; message?: string }
        if (payload && payload.success !== false) {
          resolve(payload.data as T)
        } else {
          reject(new Error(payload?.message || '请求失败'))
        }
      },
      fail: () => reject(new Error('网络请求失败'))
    })
  })
}
// #endif

// ==================== 网格 ====================
export interface GridTreeVo {
  id: number
  gridCode: string
  gridName: string
  gridLevel: number
  parentId: number | null
  roiJson: string
  children?: GridTreeVo[]
}

export function getGridTree() {
  // #ifndef MP-WEIXIN
  return webApi.get<{ success: boolean; data: GridTreeVo[] }>('/community/grids/h5/tree').then(res => res.data.data)
  // #endif
  // #ifdef MP-WEIXIN
  return requestWeb<GridTreeVo[]>('GET', '/community/grids/h5/tree')
  // #endif
}

// ==================== 巡查记录 ====================
export interface PatrolRecord {
  id?: number
  gridId: number
  patrolType?: string
  longitude?: number
  latitude?: number
  address?: string
  content?: string
  photoUrls?: string | string[]
  status?: string
  createdAt?: string
}

export function getPatrolRecords() {
  // #ifndef MP-WEIXIN
  return webApi.get<{ success: boolean; data: PatrolRecord[] }>('/community/patrol-records/h5').then(res => res.data.data)
  // #endif
  // #ifdef MP-WEIXIN
  return requestWeb<PatrolRecord[]>('GET', '/community/patrol-records/h5')
  // #endif
}

export function createPatrolRecord(data: PatrolRecord) {
  // #ifndef MP-WEIXIN
  return webApi.post<{ success: boolean; data: boolean }>('/community/patrol-records', data).then(res => res.data.data)
  // #endif
  // #ifdef MP-WEIXIN
  return requestWeb<boolean>('POST', '/community/patrol-records', data)
  // #endif
}

// ==================== 媒体文件上传 ====================
export function uploadMedia(file: string, businessType?: string, businessId?: number, fileType?: string) {
  // #ifndef MP-WEIXIN
  const form = new FormData()
  form.append('file', file as any)
  form.append('businessType', businessType || 'GENERAL')
  if (businessId) form.append('businessId', String(businessId))
  if (fileType) form.append('fileType', fileType)
  return webApi.post<{ success: boolean; data: any }>('/media/upload', form).then(res => res.data.data)
  // #endif
  // #ifdef MP-WEIXIN
  return new Promise<any>((resolve, reject) => {
    const session = getH5Session()
    const headers: Record<string, string> = {}
    if (session?.token) headers.Authorization = `Bearer ${session.token}`
    uni.uploadFile({
      url: `${resolveWebApiBase()}/media/upload`,
      filePath: file,
      name: 'file',
      formData: {
        businessType: businessType || 'GENERAL',
        ...(businessId ? { businessId: String(businessId) } : {}),
        ...(fileType ? { fileType } : {})
      },
      header: headers,
      success: (res) => {
        try {
          const payload = JSON.parse(res.data)
          if (payload?.success) resolve(payload.data)
          else reject(new Error(payload?.message || '上传失败'))
        } catch {
          reject(new Error('上传失败'))
        }
      },
      fail: () => reject(new Error('上传失败'))
    })
  })
  // #endif
}
