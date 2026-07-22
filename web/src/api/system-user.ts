import { http } from './http'
import type { PagedResult } from './types'

export type SystemUserStatus = 'ACTIVE' | 'DISABLED'

export interface SystemUser {
  id: number
  username: string
  realName: string
  phone: string
  status: SystemUserStatus
  roleCodes: string[]
  roleNames: string[]
}

export interface SystemUserDetail extends SystemUser {
  roleIds: number[]
  permissionCodes: string[]
}

export interface SystemUserQuery {
  keyword?: string
  roleId?: number
  status?: SystemUserStatus
}

export interface SystemUserSavePayload {
  id?: number
  username: string
  realName: string
  phone: string
  status: SystemUserStatus
  password?: string
  roleIds?: number[]
}

export async function listSystemUsers(_query: SystemUserQuery = {}): Promise<SystemUser[]> {
  return await http.get('/system/users')
}

export async function listSystemUsersPaged(
  page: number,
  pageSize: number,
  query: SystemUserQuery = {}
): Promise<PagedResult<SystemUser>> {
  return await http.get('/system/users/paged', {
    params: { page, pageSize, keyword: query.keyword, status: query.status }
  })
}

export async function getSystemUserDetail(id: number): Promise<SystemUserDetail> {
  return await http.get(`/system/users/${id}`)
}

export async function createSystemUser(payload: SystemUserSavePayload): Promise<SystemUserDetail> {
  return await http.post('/system/users', {
    username: payload.username.trim(),
    realName: payload.realName.trim(),
    phone: payload.phone?.trim() || null,
    status: payload.status,
    password: payload.password ?? 'changping2026',
    roleIds: payload.roleIds ?? []
  })
}

export async function updateSystemUser(id: number, payload: SystemUserSavePayload): Promise<SystemUserDetail> {
  return await http.put(`/system/users/${id}`, {
    username: payload.username.trim(),
    realName: payload.realName.trim(),
    phone: payload.phone?.trim() || null,
    status: payload.status
  })
}

export async function updateSystemUserStatus(id: number, status: SystemUserStatus): Promise<void> {
  await http.put(`/system/users/${id}/status`, { status })
}

export async function deleteSystemUser(id: number): Promise<void> {
  await http.delete(`/system/users/${id}`)
}

export async function assignSystemUserRoles(id: number, roleIds: number[]): Promise<void> {
  await http.put(`/system/users/${id}/roles`, { roleIds })
}

export async function changeUserPassword(id: number, newPassword: string): Promise<void> {
  await http.put(`/system/users/${id}/password`, { newPassword })
}
