import { getWebSession } from './session'

const BUTTON_MENU_PERMISSION_FALLBACKS: Record<string, string[]> = {
  'button:audit:approve': ['menu:audit:list'],
  'button:audit:reject': ['menu:audit:list'],
  'button:biz:area:create': ['menu:biz:area', 'menu:biz:area:list'],
  'button:biz:area:update': ['menu:biz:area', 'menu:biz:area:list'],
  'button:biz:area:delete': ['menu:biz:area', 'menu:biz:area:list'],
  'button:biz:merchant:create': ['menu:biz:merchant', 'menu:biz:merchant:list'],
  'button:biz:merchant:update': ['menu:biz:merchant', 'menu:biz:merchant:list'],
  'button:biz:merchant:delete': ['menu:biz:merchant', 'menu:biz:merchant:list'],
  'button:biz:vendor:create': ['menu:biz:vendor', 'menu:biz:vendor:list'],
  'button:biz:vendor:update': ['menu:biz:vendor', 'menu:biz:vendor:list'],
  'button:biz:vendor:delete': ['menu:biz:vendor', 'menu:biz:vendor:list'],
  'button:biz:violation-area:create': ['menu:biz:violation-area', 'menu:biz:violation-area:list'],
  'button:biz:violation-area:update': ['menu:biz:violation-area', 'menu:biz:violation-area:list'],
  'button:biz:violation-area:delete': ['menu:biz:violation-area', 'menu:biz:violation-area:list'],
  'button:system:user:create': ['menu:system:user'],
  'button:system:user:update': ['menu:system:user'],
  'button:system:user:status': ['menu:system:user'],
  'button:system:user:assign-roles': ['menu:system:user'],
  'button:system:user:change-password': ['menu:system:user'],
  'button:system:user:delete': ['menu:system:user'],
  'button:system:role:create': ['menu:system:role'],
  'button:system:role:update': ['menu:system:role'],
  'button:system:role:assign-permissions': ['menu:system:role'],
  'button:system:menu:create': ['menu:system:menu'],
  'button:system:menu:update': ['menu:system:menu'],
  'button:system:menu:delete': ['menu:system:menu'],
  'button:workorder:confirm-close': ['menu:workorder:list'],
  'button:workorder:return': ['menu:workorder:list']
}

export function hasPermission(code: string) {
  const session = getWebSession()
  if (!session) {
    return false
  }

  if (session.permissionCodes.includes(code) || session.menuPermissionCodes.includes(code)) {
    return true
  }

  if (code.startsWith('api:')) {
    // 接口权限不再由角色单独配置；前端只根据登录态展示入口，接口侧负责登录态和业务校验。
    return true
  }

  if (code.startsWith('button:')) {
    // 按钮权限跟随所在菜单：角色只勾选菜单后，该菜单页面内的操作按钮同步可用。
    const fallbackMenuCodes = BUTTON_MENU_PERMISSION_FALLBACKS[code] ?? []
    return fallbackMenuCodes.some((menuCode) => session.menuPermissionCodes.includes(menuCode))
  }

  return false
}

export function hasAnyPermission(codes: string[]) {
  return codes.some((code) => hasPermission(code))
}

export function hasMenuPermission(code: string) {
  const session = getWebSession()
  if (!session) {
    return false
  }

  return session.menuPermissionCodes.includes(code)
}
