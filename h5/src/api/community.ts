import { http } from './http'

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
  return http.get<GridTreeVo[], GridTreeVo[]>('/community/grids/tree')
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
  return http.get<PatrolRecord[], PatrolRecord[]>('/community/patrol-records')
}

export function createPatrolRecord(data: PatrolRecord) {
  return http.post<boolean, boolean>('/community/patrol-records', data)
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
  return http.post<boolean, boolean>('/community/resident-reports', data)
}

export function getResidentReports() {
  return http.get<ResidentReport[], ResidentReport[]>('/community/resident-reports')
}

export function getResidentReportByCode(queryCode: string) {
  return http.get<ResidentReport, ResidentReport>(`/community/resident-reports/code/${queryCode}`)
}
