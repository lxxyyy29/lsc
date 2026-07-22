import { http } from './http'
import type { PagedResult } from './types'
import type { BizEntityStatus } from './biz-area'

export type AreaMatchMode = 'MANUAL' | 'AUTO'

export interface BizMerchant {
  id: number
  merchantName: string
  merchantPhotoUrl?: string
  longitude?: number | null
  latitude?: number | null
  legalPersonName?: string
  legalPersonPhotoUrl?: string
  legalPersonPhone?: string
  areaId?: number | null
  areaName?: string
  areaMatchMode?: AreaMatchMode
  remark?: string
  status: BizEntityStatus
  createdAt?: string
  updatedAt?: string
}

export interface BizMerchantQuery {
  keyword?: string
  areaId?: number | null
  status?: BizEntityStatus | ''
}

export interface BizMerchantSavePayload {
  id?: number
  merchantName: string
  merchantPhotoUrl?: string
  longitude?: number | null
  latitude?: number | null
  legalPersonName?: string
  legalPersonPhotoUrl?: string
  legalPersonPhone?: string
  areaId?: number | null
  areaMatchMode?: AreaMatchMode
  remark?: string
  status: BizEntityStatus
}

export async function listBizMerchants(query: BizMerchantQuery = {}): Promise<BizMerchant[]> {
  return await http.get('/merchants', {
    params: {
      keyword: query.keyword?.trim() || undefined,
      areaId: query.areaId || undefined,
      status: query.status || undefined
    }
  })
}

export async function listBizMerchantsPaged(page: number, pageSize: number, query: BizMerchantQuery = {}): Promise<PagedResult<BizMerchant>> {
  return await http.get('/merchants/paged', {
    params: {
      page,
      pageSize,
      keyword: query.keyword?.trim() || undefined,
      areaId: query.areaId || undefined,
      status: query.status || undefined
    }
  })
}

export async function getBizMerchantDetail(id: number): Promise<BizMerchant> {
  return await http.get(`/merchants/${id}`)
}

export async function createBizMerchant(payload: BizMerchantSavePayload): Promise<BizMerchant> {
  return await http.post('/merchants', normalizeBizMerchantPayload(payload))
}

export async function updateBizMerchant(id: number, payload: BizMerchantSavePayload): Promise<BizMerchant> {
  return await http.put(`/merchants/${id}`, normalizeBizMerchantPayload(payload))
}

export async function deleteBizMerchant(id: number): Promise<void> {
  await http.delete(`/merchants/${id}`)
}

function normalizeBizMerchantPayload(payload: BizMerchantSavePayload) {
  return {
    merchantName: payload.merchantName.trim(),
    merchantPhotoUrl: payload.merchantPhotoUrl?.trim() || null,
    longitude: payload.longitude ?? null,
    latitude: payload.latitude ?? null,
    legalPersonName: payload.legalPersonName?.trim() || null,
    legalPersonPhotoUrl: payload.legalPersonPhotoUrl?.trim() || null,
    legalPersonPhone: payload.legalPersonPhone?.trim() || null,
    areaId: payload.areaId ?? null,
    areaMatchMode: payload.areaMatchMode || 'MANUAL',
    remark: payload.remark?.trim() || null,
    status: payload.status
  }
}
