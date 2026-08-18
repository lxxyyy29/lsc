import { HttpResponseError, http } from './http'

export interface H5Session {
  token: string
  userId: number
  userName: string
  account: string
  roleCodes: string[]
  permissionCodes: string[]
  menuPermissionCodes: string[]
}

export interface H5LoginPayload {
  account: string
  password: string
  captcha?: string
}

export interface H5LoginResponse {
  token: string
  userId: number
  userName: string
  account: string
  roleCodes: string[]
  permissionCodes: string[]
}

export interface CurrentH5User {
  id: number
  username: string
  realName: string
  phone: string | null
  roleCodes: string[]
  permissionCodes: string[]
  menuPermissionCodes: string[]
}

export const H5_AUTH_STORAGE_KEY = 'dgcp-oa-h5-session'

let memorySession: H5Session | null = null

function getStorage() {
  // 小程序端：全局 uni 不存在（uni-app 仅编译期替换 uni.xxx），直接使用微信全局 wx
  // #ifdef MP-WEIXIN
  const wxInstance = (globalThis as { wx?: { getStorageSync?: (key: string) => string | null | undefined; setStorageSync?: (key: string, value: string) => void; removeStorageSync?: (key: string) => void } }).wx
  if (
    wxInstance &&
    typeof wxInstance.getStorageSync === 'function' &&
    typeof wxInstance.setStorageSync === 'function' &&
    typeof wxInstance.removeStorageSync === 'function'
  ) {
    return {
      getItem(key: string) {
        const value = wxInstance.getStorageSync?.(key)
        return typeof value === 'string' ? value : null
      },
      setItem(key: string, value: string) {
        wxInstance.setStorageSync?.(key, value)
      },
      removeItem(key: string) {
        wxInstance.removeStorageSync?.(key)
      }
    }
  }
  return null
  // #endif
  // #ifndef MP-WEIXIN
  let localStorageCandidate: Storage | undefined
  try {
    localStorageCandidate = (globalThis as { localStorage?: Storage }).localStorage
  } catch {
    // 隐私模式/沙箱环境访问 localStorage 本身就会抛 SecurityError，视为不可用
    localStorageCandidate = undefined
  }
  if (
    localStorageCandidate &&
    typeof localStorageCandidate.getItem === 'function' &&
    typeof localStorageCandidate.setItem === 'function' &&
    typeof localStorageCandidate.removeItem === 'function'
  ) {
    return localStorageCandidate
  }

  const uniInstance = (globalThis as { uni?: { getStorageSync?: (key: string) => string | null | undefined; setStorageSync?: (key: string, value: string) => void; removeStorageSync?: (key: string) => void } }).uni
  if (
    uniInstance &&
    typeof uniInstance.getStorageSync === 'function' &&
    typeof uniInstance.setStorageSync === 'function' &&
    typeof uniInstance.removeStorageSync === 'function'
  ) {
    return {
      getItem(key: string) {
        const value = uniInstance.getStorageSync?.(key)
        return typeof value === 'string' ? value : null
      },
      setItem(key: string, value: string) {
        uniInstance.setStorageSync?.(key, value)
      },
      removeItem(key: string) {
        uniInstance.removeStorageSync?.(key)
      }
    }
  }

  return null
  // #endif
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

function normalizeStringArray(value: unknown) {
  if (!Array.isArray(value)) {
    return null
  }

  return value
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter((item) => item.length > 0)
}

export function normalizeH5Session(value: unknown): H5Session | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const candidate = value as Partial<H5Session>
  const token = isNonEmptyString(candidate.token) ? candidate.token.trim() : ''
  const account = isNonEmptyString(candidate.account) ? candidate.account.trim() : ''
  const userName = isNonEmptyString(candidate.userName) ? candidate.userName.trim() : ''
  const userId =
    typeof candidate.userId === 'number' && Number.isInteger(candidate.userId) && candidate.userId > 0
      ? candidate.userId
      : NaN
  const roleCodes = normalizeStringArray(candidate.roleCodes)
  const permissionCodes = normalizeStringArray(candidate.permissionCodes)
  const menuPermissionCodes = normalizeStringArray(candidate.menuPermissionCodes)

  if (!token || !account || !userName || Number.isNaN(userId)) {
    return null
  }

  if (!roleCodes || !permissionCodes || !menuPermissionCodes) {
    return null
  }

  return {
    token,
    userId,
    userName,
    account,
    roleCodes,
    permissionCodes,
    menuPermissionCodes
  }
}

export function getH5Session(): H5Session | null {
  const storage = getStorage()
  if (!storage) {
    return normalizeH5Session(memorySession)
  }

  let rawValue: string | null = null
  try {
    rawValue = storage.getItem(H5_AUTH_STORAGE_KEY)
  } catch {
    rawValue = null
  }
  // 存储不可读时回退到内存会话，避免存储异常导致会话丢失
  if (!rawValue) {
    return normalizeH5Session(memorySession)
  }

  try {
    const session = normalizeH5Session(JSON.parse(rawValue))

    if (!session) {
      try {
        storage.removeItem(H5_AUTH_STORAGE_KEY)
      } catch {
        // 忽略存储清理失败
      }
      memorySession = null
      return null
    }

    memorySession = session
    return session
  } catch {
    try {
      storage.removeItem(H5_AUTH_STORAGE_KEY)
    } catch {
      // 忽略存储清理失败
    }
    memorySession = null
    return null
  }
}

export function persistH5Session(session: H5Session) {
  const normalized = normalizeH5Session(session)
  if (!normalized) {
    clearH5Session()
    return
  }

  memorySession = normalized

  const storage = getStorage()
  if (!storage) {
    return
  }

  try {
    storage.setItem(H5_AUTH_STORAGE_KEY, JSON.stringify(normalized))
  } catch {
    // 存储写入失败（如配额已满）时仅保留内存会话，不影响登录流程
    memorySession = normalized
  }
}

export function clearH5Session() {
  memorySession = null

  const storage = getStorage()
  if (!storage) {
    return
  }

  try {
    storage.removeItem(H5_AUTH_STORAGE_KEY)
  } catch {
    // 忽略存储清理失败
  }
}

export function hasH5Session() {
  return getH5Session() !== null
}

export function hasH5Permission(code: string) {
  return getH5Session()?.permissionCodes.includes(code) ?? false
}

export function hasH5MenuPermission(code: string) {
  return getH5Session()?.menuPermissionCodes.includes(code) ?? false
}

export function createH5SessionFromLoginResponse(response: H5LoginResponse): H5Session {
  const roleCodes = Array.isArray(response.roleCodes) ? response.roleCodes : []
  const permissionCodes = Array.isArray(response.permissionCodes) ? response.permissionCodes : []
  return {
    token: response.token,
    userId: response.userId,
    userName: response.userName,
    account: response.account,
    roleCodes: [...roleCodes],
    permissionCodes: [...permissionCodes],
    menuPermissionCodes: permissionCodes.filter((code) => code.startsWith('menu:'))
  }
}

export function mergeCurrentUserIntoSession(session: H5Session, currentUser: CurrentH5User): H5Session {
  return {
    token: session.token,
    userId: currentUser.id,
    userName: currentUser.realName,
    account: currentUser.username,
    roleCodes: [...currentUser.roleCodes],
    permissionCodes: [...currentUser.permissionCodes],
    menuPermissionCodes: [...currentUser.menuPermissionCodes]
  }
}

export async function loginH5(payload: H5LoginPayload, options?: { silent?: boolean }): Promise<H5Session> {
  const response = await http.post<H5LoginResponse, H5LoginResponse>('/auth/login', payload, {
    silent: options?.silent === true
  } as any)
  const session = createH5SessionFromLoginResponse(response)
  persistH5Session(session)
  return session
}

export interface H5RegisterPayload {
  account: string
  password: string
  realName: string
  phone?: string
}

export async function registerH5(payload: H5RegisterPayload): Promise<void> {
  // 注册接口位于 /api/registration（非 /api/h5），需要覆盖 baseURL；
  // 小程序端 uni.request 只接受绝对 URL，相对路径会直接请求失败
  // #ifdef MP-WEIXIN
  await http.post<void, void>('/registration/submit', payload, { baseURL: 'https://drone.kfktec.cn:8443/api' })
  // #endif
  // #ifndef MP-WEIXIN
  await http.post<void, void>('/registration/submit', payload, { baseURL: '/api' })
  // #endif
}

/** 密码重置进度查询结果（公开接口，无需登录） */
export interface PasswordResetStatus {
  found: boolean
  status?: 'PENDING' | 'APPROVED' | 'REJECTED'
  createdAt?: string
  handledAt?: string
  remark?: string
}

/** 提交密码重置申请（无需登录）：账号+注册手机号校验，管理员审批后重置为手机号后6位 */
export async function submitPasswordReset(payload: { account: string; phone: string }): Promise<void> {
  // #ifdef MP-WEIXIN
  await http.post<void, void>('/password-reset/submit', payload, { baseURL: 'https://drone.kfktec.cn:8443/api' })
  // #endif
  // #ifndef MP-WEIXIN
  await http.post<void, void>('/password-reset/submit', payload, { baseURL: '/api' })
  // #endif
}

/** 查询密码重置申请进度（无需登录） */
export async function queryPasswordResetStatus(account: string, phone: string): Promise<PasswordResetStatus> {
  const url = `/password-reset/status?account=${encodeURIComponent(account)}&phone=${encodeURIComponent(phone)}`
  // #ifdef MP-WEIXIN
  return http.get<PasswordResetStatus, PasswordResetStatus>(url, { baseURL: 'https://drone.kfktec.cn:8443/api' })
  // #endif
  // #ifndef MP-WEIXIN
  return http.get<PasswordResetStatus, PasswordResetStatus>(url, { baseURL: '/api' })
  // #endif
}

export async function logoutH5() {
  try {
    await http.post<void, void>('/auth/logout')
  } finally {
    clearH5Session()
  }
}

export async function fetchCurrentH5User() {
  // 启动时会话恢复属于探测性请求，失败时静默，不向用户弹错
  return http.get<CurrentH5User, CurrentH5User>('/auth/me', { silent: true } as any)
}

export async function recoverH5Session(fetchCurrentUser: () => Promise<CurrentH5User>) {
  const session = getH5Session()
  if (!session?.token) {
    return null
  }

  try {
    const currentUser = await fetchCurrentUser()
    const recoveredSession = mergeCurrentUserIntoSession(session, currentUser)
    persistH5Session(recoveredSession)
    return recoveredSession
  } catch (error) {
    if (error instanceof HttpResponseError && error.status === 401) {
      clearH5Session()
      return null
    }

    return session
  }
}
