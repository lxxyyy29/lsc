import { http } from './http'

export interface VendorItem {
  id: number
  vendorName: string
  vendorPhotoUrl: string | null
  legalPersonName: string | null
  legalPersonPhotoUrl: string | null
  legalPersonPhone: string | null
  remark: string | null
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
  updatedAt: string
}

export interface VendorUpsert {
  vendorName: string
  vendorPhotoUrl?: string | null
  legalPersonName?: string | null
  legalPersonPhotoUrl?: string | null
  legalPersonPhone?: string | null
  remark?: string | null
  status?: 'ACTIVE' | 'DISABLED'
}

export function listVendors() {
  return http.get<VendorItem[]>('/mobile-vendors')
}

export function getVendor(id: number) {
  return http.get<VendorItem>(`/mobile-vendors/${id}`)
}

export function createVendor(data: VendorUpsert) {
  return http.post<VendorItem>('/mobile-vendors', data)
}

export function updateVendor(id: number, data: VendorUpsert) {
  return http.put<VendorItem>(`/mobile-vendors/${id}`, data)
}

export function deleteVendor(id: number) {
  return http.delete<void>(`/mobile-vendors/${id}`)
}
