import { http } from './http'
import type { PagedResult } from './types'

export type BizEntityStatus = 'ACTIVE' | 'DISABLED'

export interface BizArea {
  id: number
  areaName: string
  principalName?: string
  principalPhone?: string
  roiJson?: string
  remark?: string
  status: BizEntityStatus
  createdAt?: string
  updatedAt?: string
}

export interface BizAreaOption {
  id: number
  areaName: string
}

export interface BizAreaQuery {
  keyword?: string
  status?: BizEntityStatus | ''
}

export interface BizAreaSavePayload {
  id?: number
  areaName: string
  principalName?: string
  principalPhone?: string
  roiJson?: string
  remark?: string
  status: BizEntityStatus
}

export async function listBizAreas(query: BizAreaQuery = {}): Promise<BizArea[]> {
  return await http.get('/areas', {
    params: {
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function listBizAreasPaged(page: number, pageSize: number, query: BizAreaQuery = {}): Promise<PagedResult<BizArea>> {
  return await http.get('/areas/paged', {
    params: {
      page,
      pageSize,
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function getBizAreaDetail(id: number): Promise<BizArea> {
  return await http.get(`/areas/${id}`)
}

export async function listBizAreaOptions(): Promise<BizAreaOption[]> {
  return await http.get('/areas/options')
}

export async function createBizArea(payload: BizAreaSavePayload): Promise<BizArea> {
  return await http.post('/areas', normalizeBizAreaPayload(payload))
}

export async function updateBizArea(id: number, payload: BizAreaSavePayload): Promise<BizArea> {
  return await http.put(`/areas/${id}`, normalizeBizAreaPayload(payload))
}

export async function deleteBizArea(id: number): Promise<void> {
  await http.delete(`/areas/${id}`)
}

function normalizeBizAreaPayload(payload: BizAreaSavePayload) {
  return {
    areaName: payload.areaName.trim(),
    principalName: payload.principalName?.trim() || null,
    principalPhone: payload.principalPhone?.trim() || null,
    roiJson: payload.roiJson?.trim() || null,
    remark: payload.remark?.trim() || null,
    status: payload.status
  }
}
