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
  const localStorageCandidate = (globalThis as { localStorage?: Storage }).localStorage
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

  const rawValue = storage.getItem(H5_AUTH_STORAGE_KEY)
  if (!rawValue) {
    return null
  }

  try {
    const session = normalizeH5Session(JSON.parse(rawValue))

    if (!session) {
      storage.removeItem(H5_AUTH_STORAGE_KEY)
      memorySession = null
      return null
    }

    memorySession = session
    return session
  } catch {
    storage.removeItem(H5_AUTH_STORAGE_KEY)
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

  storage.setItem(H5_AUTH_STORAGE_KEY, JSON.stringify(normalized))
}

export function clearH5Session() {
  memorySession = null

  const storage = getStorage()
  if (!storage) {
    return
  }

  storage.removeItem(H5_AUTH_STORAGE_KEY)
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

export async function loginH5(payload: H5LoginPayload): Promise<H5Session> {
  const response = await http.post<H5LoginResponse, H5LoginResponse>('/auth/login', payload)
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
  await http.post<void, void>('/api/registration/submit', payload)
}

export async function logoutH5() {
  try {
    await http.post<void, void>('/auth/logout')
  } finally {
    clearH5Session()
  }
}

export async function fetchCurrentH5User() {
  return http.get<CurrentH5User, CurrentH5User>('/auth/me')
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
