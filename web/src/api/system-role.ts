import { http } from './http'
import type { PagedResult } from './types'

export type SystemRoleStatus = 'ACTIVE' | 'DISABLED'

export interface SystemRole {
  id: number
  roleCode: string
  roleName: string
  status: SystemRoleStatus
  remark: string
  userCount: number
  permissionCount: number
}

export interface SystemRoleDetail extends SystemRole {
  permissionIds: number[]
  permissionCodes: string[]
  permissionNames: string[]
}

export interface SystemRoleSavePayload {
  id?: number
  roleCode: string
  roleName: string
  status: SystemRoleStatus
  remark: string
}

export interface SystemRolePermissionSavePayload {
  permissionIds: number[]
}

export async function listSystemRoles(): Promise<SystemRole[]> {
  return await http.get('/system/roles')
}

export async function listSystemRolesPaged(page: number, pageSize: number): Promise<PagedResult<SystemRole>> {
  return await http.get('/system/roles/paged', { params: { page, pageSize } })
}

export async function getSystemRoleDetail(id: number): Promise<SystemRoleDetail> {
  return await http.get(`/system/roles/${id}`)
}

export async function saveSystemRole(payload: SystemRoleSavePayload): Promise<SystemRoleDetail> {
  const request = {
    roleCode: payload.roleCode.trim(),
    roleName: payload.roleName.trim(),
    status: payload.status,
    remark: payload.remark.trim()
  }

  return payload.id
    ? await http.put(`/system/roles/${payload.id}`, request)
    : await http.post('/system/roles', request)
}

export async function saveSystemRolePermissions(id: number, payload: SystemRolePermissionSavePayload): Promise<SystemRoleDetail> {
  return await http.put(`/system/roles/${id}/permissions`, {
    permissionIds: [...payload.permissionIds]
  })
}

export async function updateSystemRoleStatus(id: number, status: SystemRoleStatus): Promise<void> {
  const detail: SystemRoleDetail = await getSystemRoleDetail(id)
  await saveSystemRole({
    id,
    roleCode: detail.roleCode,
    roleName: detail.roleName,
    status,
    remark: detail.remark
  })
}
