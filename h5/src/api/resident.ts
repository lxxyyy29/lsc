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
function getUni() {
  // #ifdef MP-WEIXIN
  const wxInstance = (globalThis as { wx?: { reLaunch?: unknown; showToast?: unknown } }).wx
  if (wxInstance && typeof wxInstance.reLaunch === 'function') return wxInstance
  return undefined
  // #endif
  // #ifndef MP-WEIXIN
  const globalUni = (globalThis as { uni?: { reLaunch?: unknown; showToast?: unknown } }).uni
  if (globalUni && typeof globalUni.reLaunch === 'function') return globalUni
  return undefined
  // #endif
}

// ==================== 会话存储（平台兼容） ====================

function getStorageValue(): string {
  const uni = getUni()
  try {
    if (uni) return String(uni.getStorageSync(RESIDENT_AUTH_STORAGE_KEY) || '')
    return localStorage.getItem(RESIDENT_AUTH_STORAGE_KEY) || ''
  } catch {
    return ''
  }
}

function setStorageValue(value: string) {
  const uni = getUni()
  try {
    if (uni) {
      uni.setStorageSync(RESIDENT_AUTH_STORAGE_KEY, value)
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
      uni.removeStorageSync(RESIDENT_AUTH_STORAGE_KEY)
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
    uni.reLaunch({ url: '/pages/role-select/index' })
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
  if (uni?.showToast) {
    uni.showToast({ title: message, icon: 'none', duration: 2500 })
  }
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
        showErrorToast(msg)
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
      if (status !== 401) {
        showErrorToast(message)
      }
      return Promise.reject(new HttpResponseError(message, status))
    }
  )

  return client
}

const residentClient = createResidentHttpClient()

async function request<T>(method: 'get' | 'post' | 'put' | 'delete', url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return (residentClient as any)[method](url, data, config) as Promise<T>
}
// #endif

// #ifdef MP-WEIXIN
// ---- 微信小程序环境：uni.request ----
type UniRequestMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

function uniRequest<T>(method: UniRequestMethod, url: string, data?: unknown): Promise<T> {
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
          } else {
            showErrorToast(message)
          }
          reject(new HttpResponseError(message, response.statusCode))
          return
        }
        resolve(payload.data)
      },
      fail: (error) => {
        const msg = error?.errMsg?.includes('timeout') ? '请求超时，请稍后重试' : '网络错误，请检查网络连接'
        showErrorToast(msg)
        reject(new HttpResponseError(msg))
      }
    })
  })
}

function request<T>(method: UniRequestMethod, url: string, data?: unknown): Promise<T> {
  return uniRequest<T>(method, url, data)
}
// #endif

// ==================== 认证 ====================

export interface ResidentLoginPayload {
  account: string
  password: string
}

export async function login(account: string, password: string): Promise<ResidentSession> {
  const data = await request<ResidentSession>('post', '/auth/login', { account, password, clientType: 'web' })
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

export async function getResidentActivities(): Promise<unknown> {
  return request('get', '/resident/activities')
}

export async function signupActivity(activityId: number): Promise<unknown> {
  return request('post', `/resident/activities/${activityId}/signup`, {})
}

export async function cancelActivitySignup(activityId: number): Promise<unknown> {
  return request('delete', `/resident/activities/${activityId}/signup`)
}

export async function getResidentPolicies(): Promise<unknown> {
  return request('get', '/resident/policy-resources')
}

export async function getMyPoints(): Promise<unknown> {
  return request('get', '/resident/points')
}

export async function submitRepair(data: Record<string, unknown>): Promise<unknown> {
  return request('post', '/resident/repairs', data)
}

export async function getMyRepairs(): Promise<unknown> {
  return request('get', '/resident/repairs')
}

export async function getRepairDetail(id: number): Promise<unknown> {
  return request('get', `/resident/repairs/${id}`)
}

// ==================== 应急公告（居民只读） ====================

export async function getEmergencyNotices(params?: { page?: number; size?: number }): Promise<any> {
  return request('get', '/mp/emergency/dispatches', { params: { page: 1, size: 20, ...params } })
}

export async function getEmergencyNoticeDetail(id: number | string): Promise<any> {
  return request('get', `/mp/emergency/dispatches/${id}`)
}

// ==================== 图片上传 ====================

// #ifndef MP-WEIXIN
/** H5：上传 File 对象（multipart/form-data），返回含完整访问 URL 的文件信息 */
export async function uploadMedia(file: File, businessType = 'PUBLIC_REPORT'): Promise<{ fileUrl?: string; url?: string }> {
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
export async function uploadMedia(filePath: string, businessType = 'PUBLIC_REPORT'): Promise<{ fileUrl?: string; url?: string }> {
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
