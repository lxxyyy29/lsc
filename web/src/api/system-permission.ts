import { http } from './http'

export type SystemPermissionType = 'CATALOG' | 'MENU' | 'BUTTON' | 'API'

export interface SystemPermission {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: SystemPermissionType
  clientType: 'WEB' | 'H5'
  parentId: number | null
  path: string
  sortOrder: number
  status: string
  remark: string
  children?: SystemPermission[]
}

export const systemPermissionTypeLabelMap: Record<SystemPermissionType, string> = {
  CATALOG: '目录权限',
  MENU: '菜单权限',
  BUTTON: '按钮权限',
  API: '接口权限'
}

export async function listSystemPermissions(): Promise<SystemPermission[]> {
  const tree: SystemPermission[] = await http.get('/system/permissions/tree')
  return flattenPermissions(tree)
}

export async function listSystemPermissionTree(): Promise<SystemPermission[]> {
  return await http.get('/system/permissions/tree')
}

function flattenPermissions(items: SystemPermission[]): SystemPermission[] {
  return items.flatMap((item) => [item, ...(item.children ? flattenPermissions(item.children) : [])])
}

export function groupSystemPermissions(items: SystemPermission[]) {
  return items.reduce<Record<string, SystemPermission[]>>((groups, item) => {
    groups[item.permissionType] ??= []
    groups[item.permissionType].push(item)
    return groups
  }, {})
}

export function getSystemPermissionTypeLabel(type: SystemPermissionType) {
  return systemPermissionTypeLabelMap[type] ?? type
}
