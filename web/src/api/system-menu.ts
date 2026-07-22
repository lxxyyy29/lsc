import { http } from './http'

export type SystemMenuType = 'CATALOG' | 'MENU' | 'BUTTON'
export type SystemMenuStatus = 'ACTIVE' | 'DISABLED'

export interface SystemMenu {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: SystemMenuType
  clientType: 'WEB' | 'H5'
  parentId: number | null
  path: string
  component: string
  icon: string
  sortOrder: number
  status: SystemMenuStatus
  remark: string
  children?: SystemMenu[]
}

export interface SystemMenuSavePayload {
  id?: number
  parentId: number | null
  permissionCode: string
  permissionName: string
  permissionType: SystemMenuType
  clientType: 'WEB' | 'H5'
  path: string
  component: string
  icon: string
  sortOrder: number
  status: SystemMenuStatus
  remark: string
}

export async function listSystemMenus(): Promise<SystemMenu[]> {
  return await http.get('/system/menus/tree')
}

export async function saveSystemMenu(payload: SystemMenuSavePayload): Promise<SystemMenu> {
  const request = {
    parentId: payload.parentId,
    permissionCode: payload.permissionCode.trim(),
    permissionName: payload.permissionName.trim(),
    permissionType: payload.permissionType,
    clientType: payload.clientType,
    path: payload.path.trim() || null,
    component: payload.component.trim() || null,
    icon: payload.icon.trim() || null,
    sortOrder: payload.sortOrder,
    status: payload.status,
    remark: payload.remark.trim() || null
  }

  return payload.id
    ? await http.put(`/system/menus/${payload.id}`, request)
    : await http.post('/system/menus', request)
}

export async function deleteSystemMenu(id: number): Promise<void> {
  await http.delete(`/system/menus/${id}`)
}

export function flattenSystemMenus(tree: SystemMenu[]): SystemMenu[] {
  return tree.flatMap((item) => [item, ...(item.children ? flattenSystemMenus(item.children) : [])])
}
