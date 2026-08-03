import { hasMenuPermission } from '../auth/permissions'
import { hasH5Session } from '../api/auth'
import { h5NavigationItems } from '../navigation'

declare const uni: any

function getUni() {
  if (typeof uni !== 'undefined') return uni
  return (globalThis as any).uni
}
const REDIRECT_STORAGE_KEY = 'dgcp-oa-h5-redirect'

export function getFirstAccessibleRoute() {
  return h5NavigationItems.find((item) => hasMenuPermission(item.permission))?.to ?? '/login'
}

export function setPendingRedirect(path: string) {
  if (!path || path === '/login') {
    return
  }
  getUni()?.setStorageSync?.(REDIRECT_STORAGE_KEY, path)
}

export function consumePendingRedirect() {
  const value = getUni()?.getStorageSync?.(REDIRECT_STORAGE_KEY)
  getUni()?.removeStorageSync?.(REDIRECT_STORAGE_KEY)
  return typeof value === 'string' && value.length > 0 ? value : ''
}

export function toPageUrl(path: string) {
  // 已经是完整路径则直接返回
  if (path.startsWith('/pages/')) {
    return path
  }

  if (path.startsWith('/work-orders/')) {
    const [detailPath] = path.split('?')
    const identity = detailPath.slice('/work-orders/'.length)
    if (/^\d+$/.test(identity)) {
      return `/pages/workorder/detail?id=${identity}`
    } else if (identity) {
      return `/pages/workorder/detail?orderNo=${encodeURIComponent(identity)}`
    }
    return '/pages/workorder/detail'
  }

  switch (path) {
    case '/login':
      return '/pages/login/index'
    case '/workbench':
      return '/pages/workbench/index'
    case '/work-orders':
      return '/pages/workorder/list'
    case '/verify':
      return '/pages/verify/index'
    case '/history':
      return '/pages/history/index'
    case '/mine':
      return '/pages/mine/index'
    // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
    // case '/messages':
    //   return '/pages/message/index'
    default:
      return '/pages/workbench/index'
  }
}

const TAB_BAR_PAGES = [
  '/pages/workbench/index',
  '/pages/workorder/list',
  '/pages/mine/index'
]

export function navigateToPath(path: string) {
  const url = toPageUrl(path)
  const uni = getUni()
  if (uni) {
    if (TAB_BAR_PAGES.includes(url)) {
      uni.switchTab({ url })
    } else {
      uni.navigateTo({ url })
    }
  }
}

export function redirectToPath(path: string) {
  const url = toPageUrl(path)
  const uni = getUni()
  if (uni) {
    uni.reLaunch({ url })
  }
}

export function ensureAuthenticated(targetPath: string) {
  if (hasH5Session()) {
    return true
  }

  setPendingRedirect(targetPath)
  const uni = getUni()
  if (uni) {
    uni.reLaunch({ url: '/pages/login/index' })
  }
  return false
}
