import { getH5Session } from '../api/auth'

const BUTTON_MENU_PERMISSION_FALLBACKS: Record<string, string[]> = {
  'button:h5:merchant:create': ['menu:h5:merchant:view'],
  'button:h5:merchant:update': ['menu:h5:merchant:view'],
  'button:h5:merchant:delete': ['menu:h5:merchant:view'],
  'button:h5:vendor:create': ['menu:h5:vendor:view'],
  'button:h5:vendor:update': ['menu:h5:vendor:view'],
  'button:h5:vendor:delete': ['menu:h5:vendor:view'],
  'button:h5:workorder:handle': ['menu:h5:workorder:list'],
  'button:h5:workorder:verify': ['menu:h5:workorder:list']
}

export function getPermissionCodes() {
  return getH5Session()?.permissionCodes ?? []
}

export function getMenuPermissionCodes() {
  return getH5Session()?.menuPermissionCodes ?? []
}

export function hasPermission(code: string) {
  const session = getH5Session()
  if (!session) {
    return false
  }

  if (session.permissionCodes.includes(code) || session.menuPermissionCodes.includes(code)) {
    return true
  }

  if (code.startsWith('api:')) {
    // 接口权限不再由角色单独配置；H5 前端只判断登录态，具体业务范围交给接口和服务层校验。
    return true
  }

  return false
}

export function hasAnyPermission(codes: string[]) {
  return codes.some((code) => hasPermission(code))
}

export function hasMenuPermission(code: string) {
  return getMenuPermissionCodes().includes(code)
}

export function hasButtonPermission(code: string) {
  if (hasPermission(code)) {
    return true
  }

  // H5 端按钮权限跟随所在菜单：角色只配置菜单后，页面内新增/编辑/删除等操作同步可用。
  const fallbackMenuCodes = BUTTON_MENU_PERMISSION_FALLBACKS[code] ?? []
  return fallbackMenuCodes.some((menuCode) => hasMenuPermission(menuCode))
}
