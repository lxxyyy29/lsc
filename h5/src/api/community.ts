import axios from 'axios'
import { getH5Session } from './auth'

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
  return webApi.get<GridTreeVo[]>('/community/grids/tree').then(res => res.data.data)
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
  return webApi.get<PatrolRecord[]>('/community/patrol-records').then(res => res.data.data)
}

export function createPatrolRecord(data: PatrolRecord) {
  return webApi.post<boolean>('/community/patrol-records', data).then(res => res.data.data)
}

// ==================== 居民上报 ====================
export interface ResidentReport {
  id?: number
  gridId?: number
  residentName?: string
  residentPhone?: string
  reportType: string
  title: string
  content?: string
  photoUrls?: string | string[]
  longitude?: number
  latitude?: number
  queryCode?: string
  status?: string
}

export function createResidentReport(data: ResidentReport) {
  return webApi.post<boolean>('/community/resident-reports', data).then(res => res.data.data)
}

export function getResidentReports() {
  return webApi.get<ResidentReport[]>('/community/resident-reports').then(res => res.data.data)
}

export function getResidentReportByCode(queryCode: string) {
  return webApi.get<ResidentReport>(`/community/resident-reports/code/${queryCode}`).then(res => res.data.data)
}

// 媒体文件上传
export function uploadMedia(file: string, businessType?: string, businessId?: number, fileType?: string) {
  const form = new FormData()
  form.append('file', file as any)
  form.append('businessType', businessType || 'GENERAL')
  if (businessId) form.append('businessId', String(businessId))
  if (fileType) form.append('fileType', fileType)
  return webApi.post<any>('/media/upload', form).then(res => res.data.data)
}
