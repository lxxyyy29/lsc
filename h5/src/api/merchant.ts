import { http } from './http'

export interface MerchantItem {
  id: number
  merchantName: string
  merchantPhotoUrl: string | null
  longitude: number | null
  latitude: number | null
  legalPersonName: string | null
  legalPersonPhotoUrl: string | null
  legalPersonPhone: string | null
  areaId: number | null
  areaName: string | null
  areaMatchMode: 'MANUAL' | 'AUTO'
  remark: string | null
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface MerchantUpsert {
  merchantName: string
  merchantPhotoUrl?: string | null
  longitude?: number | null
  latitude?: number | null
  legalPersonName?: string | null
  legalPersonPhotoUrl?: string | null
  legalPersonPhone?: string | null
  areaId?: number | null
  areaMatchMode?: 'MANUAL' | 'AUTO'
  remark?: string | null
  status?: 'ACTIVE' | 'DISABLED'
}

export interface AreaOption {
  id: number
  areaName: string
}

export function listMerchants() {
  return http.get<MerchantItem[]>('/merchants')
}

export function getMerchant(id: number) {
  return http.get<MerchantItem>(`/merchants/${id}`)
}

export function createMerchant(data: MerchantUpsert) {
  return http.post<MerchantItem>('/merchants', data)
}

export function updateMerchant(id: number, data: MerchantUpsert) {
  return http.put<MerchantItem>(`/merchants/${id}`, data)
}

export function deleteMerchant(id: number) {
  return http.delete<void>(`/merchants/${id}`)
}

export function listAreaOptions() {
  return http.get<AreaOption[]>('/areas/options')
}
