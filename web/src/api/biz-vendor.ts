import { http } from './http'
import type { PagedResult } from './types'
import type { BizEntityStatus } from './biz-area'

export interface BizVendor {
  id: number
  vendorName: string
  vendorPhotoUrl?: string
  legalPersonName?: string
  legalPersonPhotoUrl?: string
  legalPersonPhone?: string
  remark?: string
  status: BizEntityStatus
  createdAt?: string
  updatedAt?: string
}

export interface BizVendorQuery {
  keyword?: string
  status?: BizEntityStatus | ''
}

export interface BizVendorSavePayload {
  id?: number
  vendorName: string
  vendorPhotoUrl?: string
  legalPersonName?: string
  legalPersonPhotoUrl?: string
  legalPersonPhone?: string
  remark?: string
  status: BizEntityStatus
}

export async function listBizVendors(query: BizVendorQuery = {}): Promise<BizVendor[]> {
  return await http.get('/mobile-vendors', {
    params: {
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function listBizVendorsPaged(page: number, pageSize: number, query: BizVendorQuery = {}): Promise<PagedResult<BizVendor>> {
  return await http.get('/mobile-vendors/paged', {
    params: {
      page,
      pageSize,
      keyword: query.keyword?.trim() || undefined,
      status: query.status || undefined
    }
  })
}

export async function getBizVendorDetail(id: number): Promise<BizVendor> {
  return await http.get(`/mobile-vendors/${id}`)
}

export async function createBizVendor(payload: BizVendorSavePayload): Promise<BizVendor> {
  return await http.post('/mobile-vendors', normalizeBizVendorPayload(payload))
}

export async function updateBizVendor(id: number, payload: BizVendorSavePayload): Promise<BizVendor> {
  return await http.put(`/mobile-vendors/${id}`, normalizeBizVendorPayload(payload))
}

export async function deleteBizVendor(id: number): Promise<void> {
  await http.delete(`/mobile-vendors/${id}`)
}

function normalizeBizVendorPayload(payload: BizVendorSavePayload) {
  return {
    vendorName: payload.vendorName.trim(),
    vendorPhotoUrl: payload.vendorPhotoUrl?.trim() || null,
    legalPersonName: payload.legalPersonName?.trim() || null,
    legalPersonPhotoUrl: payload.legalPersonPhotoUrl?.trim() || null,
    legalPersonPhone: payload.legalPersonPhone?.trim() || null,
    remark: payload.remark?.trim() || null,
    status: payload.status
  }
}
