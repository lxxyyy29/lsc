import { hasMenuPermission } from '../auth/permissions'
import { hasH5Session } from '../api/auth'
import { h5NavigationItems } from '../navigation'

declare const uni: any
declare const wx: any

/** 平台全局对象（小程序为 wx，H5 为全局 uni）：导航与存储方法统一类型声明 */
type NavUniLike = {
  reLaunch?: (options: { url: string }) => void
  switchTab?: (options: { url: string }) => void
  navigateTo?: (options: { url: string }) => void
  getStorageSync?: (key: string) => unknown
  setStorageSync?: (key: string, value: unknown) => void
  removeStorageSync?: (key: string) => void
}

function getUni(): NavUniLike | undefined {
  // 条件编译分支内不提前 return，避免 TS 把后续分支判为不可达、丢失类型收窄
  let ref: NavUniLike | undefined
  // #ifdef MP-WEIXIN
  // 小程序端：全局 uni 不存在（uni-app 仅编译期替换 uni.xxx 调用），直接使用微信全局 wx
  if (typeof wx !== 'undefined') ref = wx as NavUniLike
  // #endif
  // #ifndef MP-WEIXIN
  // 注意：不能用 `uni.reLaunch` 直接判断 —— uni-app 编译期会把 `uni.xxx` 静态替换为
  // 运行时模块函数引用（永远存在），导致误判 window.uni（H5 运行时中仅空对象占位）可用，
  // 随后在空对象上调用 reLaunch 抛错。必须通过 globalThis 访问真实全局 uni 并检查方法。
  ref = (globalThis as { uni?: NavUniLike }).uni
  // #endif
  if (!ref || typeof ref.reLaunch !== 'function') return undefined
  return ref
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
      uni.setStorageSync?.(REDIRECT_STORAGE_KEY, path)
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
      value = uni.getStorageSync?.(REDIRECT_STORAGE_KEY) as string
      uni.removeStorageSync?.(REDIRECT_STORAGE_KEY)
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
      return '/pages/role-select/index'
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
    // 志愿服务已取消（需求12.2），保留代码便于追溯后续开发
    // case '/volunteer':
    //   return '/pages/volunteer/index'
    // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
    // case '/messages':
    //   return '/pages/message/index'
    default:
      return '/pages/workbench/index'
  }
}

// 原生 tabBar 页面（switchTab 只能切这些）：小程序为居民端 3 tab，H5 为网格员端 4 tab
// 先声明再条件赋值避开重复声明的 TS 误报
let TAB_BAR_PAGES = [
  '/pages/workbench/index',
  '/pages/map/index',
  '/pages/patrol/checkin',
  '/pages/mine/index'
]
// #ifdef MP-WEIXIN
TAB_BAR_PAGES = [
  '/pages/resident/report/index',
  '/pages/resident/services/index',
  '/pages/resident/mine/index'
]
// #endif

export function navigateToPath(path: string) {
  const url = toPageUrl(path)
  const uni = getUni()
  if (uni) {
    if (TAB_BAR_PAGES.includes(url) && typeof uni.switchTab === 'function') {
      uni.switchTab({ url })
    } else {
      uni.navigateTo?.({ url })
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
      uni.reLaunch?.({ url })
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
    uni.reLaunch?.({ url: '/pages/role-select/index' })
  } else {
    // 浏览器环境 fallback
    window.location.hash = '/pages/role-select/index'
  }
  return false
}
