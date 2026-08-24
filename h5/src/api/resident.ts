/**
 * 居民端（微信小程序 / H5 居民入口）API 层
 *
 * 与工作人员端（/api/h5 前缀 + dgcp-oa-h5-session）完全隔离：
 * - 接口走 /api 前缀（WEB 会话），登录 clientType=web，否则后端 /resident/* 会拦截
 * - 会话 key 沿用 grid-mp-session，避免与工作人员会话串号
 * - MP-WEIXIN 下使用 uni.request / uni.uploadFile，baseURL 指向线上 HTTPS 域名
 */
import { HttpResponseError } from './http'

export const RESIDENT_AUTH_STORAGE_KEY = 'grid-mp-session'

export interface ResidentSession {
  token: string
  userId: number
  userName: string
  account: string
  roleCodes: string[]
  permissionCodes: string[]
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

/** 获取 uni 全局对象（H5 运行时 window.uni 可能是空占位对象，需检查方法再调用；小程序用微信全局 wx） */
/** 平台全局对象（小程序为 wx，H5 为全局 uni）：统一类型声明供存储/导航/提示共用 */
type ResidentUniLike = {
  reLaunch?: (options: { url: string }) => void
  showToast?: (options: { title: string; icon?: string; duration?: number }) => void
  getStorageSync?: (key: string) => unknown
  setStorageSync?: (key: string, value: unknown) => void
  removeStorageSync?: (key: string) => void
}

function getUni(): ResidentUniLike | undefined {
  // 条件编译分支内不提前 return，避免 TS 把后续分支判为不可达、丢失类型收窄
  let ref: ResidentUniLike | undefined
  // #ifdef MP-WEIXIN
  ref = (globalThis as { wx?: ResidentUniLike }).wx
  // #endif
  // #ifndef MP-WEIXIN
  ref = (globalThis as { uni?: ResidentUniLike }).uni
  // #endif
  if (!ref || typeof ref.reLaunch !== 'function') return undefined
  return ref
}

// ==================== 会话存储（平台兼容） ====================

function getStorageValue(): string {
  const uni = getUni()
  try {
    if (uni) return String(uni.getStorageSync?.(RESIDENT_AUTH_STORAGE_KEY) || '')
    return localStorage.getItem(RESIDENT_AUTH_STORAGE_KEY) || ''
  } catch {
    return ''
  }
}

function setStorageValue(value: string) {
  const uni = getUni()
  try {
    if (uni) {
      uni.setStorageSync?.(RESIDENT_AUTH_STORAGE_KEY, value)
    } else {
      localStorage.setItem(RESIDENT_AUTH_STORAGE_KEY, value)
    }
  } catch {
    // 存储不可用时忽略
  }
}

function removeStorageValue() {
  const uni = getUni()
  try {
    if (uni) {
      uni.removeStorageSync?.(RESIDENT_AUTH_STORAGE_KEY)
    } else {
      localStorage.removeItem(RESIDENT_AUTH_STORAGE_KEY)
    }
  } catch {
    // 忽略
  }
}

export function getResidentSession(): ResidentSession | null {
  const raw = getStorageValue()
  if (!raw) return null
  try {
    return JSON.parse(raw) as ResidentSession
  } catch {
    return null
  }
}

export function persistResidentSession(session: ResidentSession) {
  setStorageValue(JSON.stringify(session))
}

export function clearResidentSession() {
  removeStorageValue()
}

// ==================== HTTP 客户端 ====================

function resolveApiBaseUrl() {
  // #ifdef MP-WEIXIN
  return 'https://drone.kfktec.cn:8443/api'
  // #endif
  // #ifndef MP-WEIXIN
  return '/api'
  // #endif
}

function handleResident401() {
  clearResidentSession()
  const uni = getUni()
  if (uni) {
    uni.reLaunch?.({ url: '/pages/role-select/index' })
  }
}

// 后端错误信息 → 用户友好提示
const ERROR_MAP: Record<string, string> = {
  '外部事件 ID 不能为空': '请填写事件相关信息',
  '地点不能为空': '请填写问题发生地点',
  '发生时间不能为空': '请填写问题发生时间',
  '来源类型不能为空': '请选择来源类型',
  '事件类型不能为空': '请选择问题类型',
  '来源系统不能为空': '请选择来源系统',
  '标题不能为空': '请填写问题标题',
  '描述不能为空': '请填写问题描述',
  '账号和密码不能为空': '请填写账号和密码',
  '账号已存在': '该账号已被注册',
  '账号或密码错误': '账号或密码错误',
}

function translateError(msg: string): string {
  if (!msg) return '操作失败，请稍后重试'
  for (const [key, value] of Object.entries(ERROR_MAP)) {
    if (msg.includes(key)) return value
  }
  return msg
}

function showErrorToast(message: string) {
  const uni = getUni()
  uni?.showToast?.({ title: message, icon: 'none', duration: 2500 })
}

// #ifndef MP-WEIXIN
// ---- H5 浏览器环境：axios ----
import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

function createResidentHttpClient(config?: AxiosRequestConfig): AxiosInstance {
  const client = axios.create({
    baseURL: resolveApiBaseUrl(),
    timeout: 15000,
    ...config
  })

  client.interceptors.request.use((requestConfig) => {
    const session = getResidentSession()
    if (session?.token) {
      requestConfig.headers = requestConfig.headers ?? {}
      requestConfig.headers.Authorization = `Bearer ${session.token}`
    }
    return requestConfig
  })

  client.interceptors.response.use(
    (response): any => {
      const payload = response.data as ApiResponse<unknown>
      if (!payload || typeof payload !== 'object' || payload.success !== true) {
        const msg = translateError((payload as ApiResponse<unknown>)?.message || '请求失败，请稍后重试')
        if (!(response.config as { silent?: boolean })?.silent) {
          showErrorToast(msg)
        }
        throw new HttpResponseError(msg, response.status)
      }
      return payload.data
    },
    (error) => {
      const status = error.response?.status
      if (status === 401) {
        handleResident401()
      }
      const rawMsg = error.response?.data?.message || (error instanceof Error && error.message ? error.message : '网络错误，请检查网络连接')
      const message = translateError(rawMsg)
      const silent = (error.config as { silent?: boolean } | undefined)?.silent === true
      if (status !== 401 && !silent) {
        showErrorToast(message)
      }
      return Promise.reject(new HttpResponseError(message, status))
    }
  )

  return client
}

const residentClient = createResidentHttpClient()

async function requestAxios<T>(method: 'get' | 'post' | 'put' | 'delete', url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return (residentClient as any)[method](url, data, config) as Promise<T>
}
// #endif

// #ifdef MP-WEIXIN
// ---- 微信小程序环境：uni.request ----
type UniRequestMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

function uniRequest<T>(method: UniRequestMethod, url: string, data?: unknown, config?: { silent?: boolean }): Promise<T> {
  const silent = config?.silent === true
  return new Promise((resolve, reject) => {
    const session = getResidentSession()
    const headers: Record<string, string> = {}
    if (session?.token) {
      headers.Authorization = `Bearer ${session.token}`
    }

    uni.request({
      url: `${resolveApiBaseUrl()}${url}`,
      method,
      data: data as any,
      header: headers,
      success: (response) => {
        const payload = response.data as ApiResponse<T>
        if (!payload || typeof payload !== 'object' || payload.success !== true) {
          const message = payload && typeof payload === 'object' && 'message' in payload
            ? translateError(String(payload.message || '请求失败，请稍后重试'))
            : '请求失败，请稍后重试'
          if (response.statusCode === 401) {
            handleResident401()
          } else if (!silent) {
            showErrorToast(message)
          }
          reject(new HttpResponseError(message, response.statusCode))
          return
        }
        resolve(payload.data)
      },
      fail: (error) => {
        const msg = error?.errMsg?.includes('timeout') ? '请求超时，请稍后重试' : '网络错误，请检查网络连接'
        if (!silent) {
          showErrorToast(msg)
        }
        reject(new HttpResponseError(msg))
      }
    })
  })
}

function requestUni<T>(method: 'get' | 'post' | 'put' | 'delete', url: string, data?: unknown, config?: { silent?: boolean }): Promise<T> {
  // uni.request 要求大写 method，此处统一转换（兼修复小写 method 导致小程序请求失败的隐患）
  return uniRequest<T>(method.toUpperCase() as UniRequestMethod, url, data, config)
}
// #endif

// 统一 request 入口：H5 用 axios、小程序用 uni.request（条件编译拆分实现，let 赋值避开重复定义）
type ResidentRequestFn = <T = unknown>(method: 'get' | 'post' | 'put' | 'delete', url: string, data?: unknown, config?: { silent?: boolean }) => Promise<T>
let request: ResidentRequestFn
// #ifndef MP-WEIXIN
request = requestAxios as ResidentRequestFn
// #endif
// #ifdef MP-WEIXIN
request = requestUni as ResidentRequestFn
// #endif

// ==================== 认证 ====================

export interface ResidentLoginPayload {
  account: string
  password: string
}

export async function login(account: string, password: string): Promise<ResidentSession> {
  // 登录失败由调用方页面内联展示错误，不全局弹 toast
  const data = await request<ResidentSession>('post', '/auth/login', { account, password, clientType: 'web' }, { silent: true } as any)
  persistResidentSession(data)
  return data
}

/** 发送手机号验证码（测试模式：后端返回固定验证码 testCode） */
export async function sendSmsCode(phone: string): Promise<{ phone: string; testCode?: string; message?: string }> {
  return request('post', '/auth/sms-code', { phone })
}

/** 手机号验证码登录：后端按角色自动决定客户端类型（网格员=H5/居民=WEB），返回统一登录响应 */
export async function phoneLogin(phone: string, code: string): Promise<ResidentSession> {
  return request<ResidentSession>('post', '/auth/phone-login', { phone, code })
}

/** 微信手机号一键登录：getPhoneNumber 按钮的 code → 后端换手机号并登录（返回统一登录响应） */
export async function wechatLogin(code: string): Promise<ResidentSession> {
  return request<ResidentSession>('post', '/auth/wechat-login', { code })
}

export async function register(account: string, password: string, realName: string, phone: string): Promise<void> {
  await request<void>('post', '/auth/register', { account, password, realName, phone })
}

export function logout() {
  clearResidentSession()
}

// ==================== 事件上报 ====================

export async function reportEvent(data: Record<string, unknown>): Promise<unknown> {
  return request('post', '/events/public-report', data)
}

export async function getMyReports(params?: { page?: number; pageSize?: number }): Promise<unknown> {
  return request('get', `/events/my-reports?page=${params?.page ?? 1}&pageSize=${params?.pageSize ?? 10}`)
}

export async function rateEvent(eventId: number | string, data: { rating: number; comment?: string }): Promise<unknown> {
  return request('post', `/events/${eventId}/rate`, data)
}

// ==================== 居民互动 ====================

export async function getResidentActivities(): Promise<any[]> {
  return request<any[]>('get', '/resident/activities')
}

export async function signupActivity(activityId: number): Promise<unknown> {
  return request('post', `/resident/activities/${activityId}/signup`, {})
}

export async function cancelActivitySignup(activityId: number): Promise<unknown> {
  return request('delete', `/resident/activities/${activityId}/signup`)
}

/** 活动签到（限活动当天至结束后2天，仅一次，成功后发放积分） */
export async function checkinActivity(activityId: number): Promise<unknown> {
  return request('post', `/resident/activities/${activityId}/checkin`, {})
}

export async function getResidentPolicies(): Promise<any[]> {
  return request<any[]>('get', '/resident/policy-resources')
}

export async function getMyPoints(): Promise<unknown> {
  return request('get', '/resident/points')
}

export async function submitRepair(data: Record<string, unknown>): Promise<unknown> {
  return request('post', '/resident/repairs', data)
}

export async function getMyRepairs(): Promise<any[]> {
  return request<any[]>('get', '/resident/repairs')
}

export async function getRepairDetail(id: number): Promise<unknown> {
  return request('get', `/resident/repairs/${id}`)
}

// ==================== 图片上传 ====================

// #ifndef MP-WEIXIN
/** H5：上传 File 对象（multipart/form-data），返回含完整访问 URL 的文件信息 */
async function uploadMediaH5(file: File, businessType = 'PUBLIC_REPORT'): Promise<{ fileUrl?: string; url?: string }> {
  const form = new FormData()
  form.append('file', file)
  form.append('businessType', businessType)
  return residentClient.post('/media/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  }) as unknown as Promise<{ fileUrl?: string; url?: string }>
}
// #endif

// #ifdef MP-WEIXIN
/** 小程序：上传本地临时文件路径，返回含完整访问 URL 的文件信息 */
async function uploadMediaMp(filePath: string, businessType = 'PUBLIC_REPORT'): Promise<{ fileUrl?: string; url?: string }> {
  const session = getResidentSession()
  const headers: Record<string, string> = {}
  if (session?.token) {
    headers.Authorization = `Bearer ${session.token}`
  }
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${resolveApiBaseUrl()}/media/upload`,
      filePath,
      name: 'file',
      formData: { businessType },
      header: headers,
      success: (response) => {
        try {
          const payload = JSON.parse(response.data) as { success: boolean; message: string; data: { fileUrl?: string } }
          if (!payload.success) {
            reject(new HttpResponseError(payload.message || '上传失败', response.statusCode))
            return
          }
          resolve(payload.data)
        } catch (e) {
          reject(new HttpResponseError('上传失败'))
        }
      },
      fail: () => reject(new HttpResponseError('上传失败'))
    })
  })
}
// #endif

// 统一导出：H5 传 File、小程序传临时文件路径（let 赋值避开重复导出的 TS 误报）
export let uploadMedia: (fileOrPath: File | string, businessType?: string) => Promise<{ fileUrl?: string; url?: string }>
// #ifndef MP-WEIXIN
uploadMedia = uploadMediaH5 as typeof uploadMedia
// #endif
// #ifdef MP-WEIXIN
uploadMedia = uploadMediaMp as typeof uploadMedia
// #endif
