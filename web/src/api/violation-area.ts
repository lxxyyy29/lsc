import { http } from './http'
import type { PagedResult } from './types'

export type ViolationAreaStatus = 'ACTIVE' | 'DISABLED'
export type ViolationAreaType = 'ILLEGAL_STALL' | 'ILLEGAL_ROAD_OCCUPATION' | 'ILLEGAL_ADVERTISING' | 'ILLEGAL_PARKING'

export const VIOLATION_AREA_TYPE_OPTIONS: { label: string; value: ViolationAreaType }[] = [
  { label: '违规摆摊', value: 'ILLEGAL_STALL' },
  { label: '违规占道经营', value: 'ILLEGAL_ROAD_OCCUPATION' },
  { label: '违规打广告', value: 'ILLEGAL_ADVERTISING' },
  { label: '违规停车', value: 'ILLEGAL_PARKING' }
]

export const VIOLATION_AREA_TYPE_LABEL_MAP: Record<string, string> = Object.fromEntries(
  VIOLATION_AREA_TYPE_OPTIONS.map((o) => [o.value, o.label])
)

export interface ViolationArea {
  id: number
  areaName: string
  areaType?: string
  roiJson?: string
  remark?: string
  status: ViolationAreaStatus
  createdAt?: string
  updatedAt?: string
}

export interface ViolationAreaQuery {
  keyword?: string
  status?: ViolationAreaStatus | ''
}

export interface ViolationAreaSavePayload {
  areaName: string
  areaType?: string
  roiJson?: string
  remark?: string
  status: ViolationAreaStatus
}

export async function listViolationAreas(query: ViolationAreaQuery = {}): Promise<ViolationArea[]> {
  return await http.get('/violation-areas', {
    params: {
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function listViolationAreasPaged(page: number, pageSize: number, query: ViolationAreaQuery = {}): Promise<PagedResult<ViolationArea>> {
  return await http.get('/violation-areas/paged', {
    params: {
      page,
      pageSize,
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function getViolationAreaDetail(id: number): Promise<ViolationArea> {
  return await http.get(`/violation-areas/${id}`)
}

export async function createViolationArea(payload: ViolationAreaSavePayload): Promise<ViolationArea> {
  return await http.post('/violation-areas', normalizePayload(payload))
}

export async function updateViolationArea(id: number, payload: ViolationAreaSavePayload): Promise<ViolationArea> {
  return await http.put(`/violation-areas/${id}`, normalizePayload(payload))
}

export async function deleteViolationArea(id: number): Promise<void> {
  await http.delete(`/violation-areas/${id}`)
}

function normalizePayload(payload: ViolationAreaSavePayload) {
  return {
    areaName: payload.areaName.trim(),
    areaType: payload.areaType?.trim() || null,
    roiJson: payload.roiJson?.trim() || null,
    remark: payload.remark?.trim() || null,
    status: payload.status
  }
}
