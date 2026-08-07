import { hasMenuPermission } from '../auth/permissions'
import { hasH5Session } from '../api/auth'
import { h5NavigationItems } from '../navigation'

declare const uni: any

function getUni() {
  // 注意：不能用 `uni.reLaunch` 直接判断 —— uni-app 编译期会把 `uni.xxx` 静态替换为
  // 运行时模块函数引用（永远存在），导致误判 window.uni（H5 运行时中仅空对象占位）可用，
  // 随后在空对象上调用 reLaunch 抛错。必须通过 globalThis 访问真实全局 uni 并检查方法。
  const globalUni = (globalThis as { uni?: { reLaunch?: unknown } }).uni
  if (globalUni && typeof globalUni.reLaunch === 'function') return globalUni
  return undefined
}
const REDIRECT_STORAGE_KEY = 'dgcp-oa-h5-redirect'

export function getFirstAccessibleRoute() {
  return h5NavigationItems.find((item) => hasMenuPermission(item.permission))?.to ?? '/login'
}

export function setPendingRedirect(path: string) {
  if (!path || path === '/login') {
    return
  }
  const uni = getUni()
  try {
    if (uni) {
      uni.setStorageSync(REDIRECT_STORAGE_KEY, path)
    } else {
      // 浏览器环境 fallback
      localStorage.setItem(REDIRECT_STORAGE_KEY, path)
    }
  } catch {
    // 存储不可用时忽略，不影响登录跳转
  }
}

export function consumePendingRedirect() {
  const uni = getUni()
  try {
    let value: string
    if (uni) {
      value = uni.getStorageSync(REDIRECT_STORAGE_KEY)
      uni.removeStorageSync(REDIRECT_STORAGE_KEY)
    } else {
      // 浏览器环境 fallback
      value = localStorage.getItem(REDIRECT_STORAGE_KEY) || ''
      localStorage.removeItem(REDIRECT_STORAGE_KEY)
    }
    return typeof value === 'string' && value.length > 0 ? value : ''
  } catch {
    return ''
  }
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
    case '/map':
      return '/pages/map/index'
    case '/patrol':
      return '/pages/patrol/checkin'
    case '/mine':
      return '/pages/mine/index'
    case '/volunteer':
      return '/pages/volunteer/index'
    // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
    // case '/messages':
    //   return '/pages/message/index'
    default:
      return '/pages/workbench/index'
  }
}

const TAB_BAR_PAGES = [
  '/pages/workbench/index',
  '/pages/map/index',
  '/pages/patrol/checkin',
  '/pages/mine/index'
]

export function navigateToPath(path: string) {
  const url = toPageUrl(path)
  const uni = getUni()
  if (uni) {
    if (TAB_BAR_PAGES.includes(url) && typeof uni.switchTab === 'function') {
      uni.switchTab({ url })
    } else {
      uni.navigateTo({ url })
    }
  } else {
    // 浏览器环境 fallback
    window.location.hash = '#' + url
  }
}

export function redirectToPath(path: string) {
  const url = toPageUrl(path)
  const uni = getUni()
  if (uni) {
    if (TAB_BAR_PAGES.includes(url) && typeof uni.switchTab === 'function') {
      uni.switchTab({ url })
    } else {
      uni.reLaunch({ url })
    }
  } else {
    // 浏览器环境 fallback：使用 hash 路由跳转
    window.location.hash = '#' + url
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
  } else {
    // 浏览器环境 fallback
    window.location.hash = '#/pages/login/index'
  }
  return false
}
