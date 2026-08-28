// #ifndef MP-WEIXIN
import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
// #endif
import { clearH5Session, getH5Session } from './auth'

type HttpLikeClient = {
  get<T = unknown, R = T>(url: string, config?: unknown): Promise<R>
  post<T = unknown, R = T>(url: string, data?: unknown, config?: unknown): Promise<R>
  put<T = unknown, R = T>(url: string, data?: unknown, config?: unknown): Promise<R>
  delete<T = unknown, R = T>(url: string, config?: unknown): Promise<R>
  upload<T = unknown>(url: string, filePath: string, formData?: Record<string, unknown>): Promise<T>
}

interface ApiResponse<T> {
  success: boolean
  code: string
  message: string
  data: T
}

export class HttpResponseError extends Error {
  status?: number

  constructor(message: string, status?: number) {
    super(message)
    this.name = 'HttpResponseError'
    this.status = status
  }
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    // 拦截常见英文网络错误
    if (error.message === 'Network Error') return '网络连接失败，请检查网络'
    if (error.message.includes('timeout')) return '请求超时，请稍后重试'
    if (error.message.startsWith('Request failed')) return '请求失败，请稍后重试'
    return error.message
  }
  return '请求失败，请稍后重试'
}

function resolveApiBaseUrl() {
  const envValue = (globalThis as { __H5_API_BASE_URL__?: string }).__H5_API_BASE_URL__
  if (typeof envValue === 'string' && envValue.trim().length > 0) return envValue.trim()
  // #ifdef MP-WEIXIN
  return 'https://drone.kfktec.cn:8443/api/h5'
  // #endif
  return '/api/h5'
}

/**
 * Handle a 401 response from any code path.
 * If the response body carries code='AUTH_PASSWORD_CHANGED', show an explanatory
 * modal before redirecting — otherwise redirect immediately.
 */
function handle401(responseCode?: string) {
  clearH5Session()
  // 小程序端：全局 uni 不存在，使用微信全局 wx；先声明再条件赋值避开重复声明的 TS 误报
  let uniRef: { reLaunch?: (options: { url: string }) => void; showModal?: (options: Record<string, unknown>) => void } | undefined
  // #ifdef MP-WEIXIN
  uniRef = (globalThis as { wx?: typeof uniRef }).wx
  // #endif
  // #ifndef MP-WEIXIN
  // 通过 globalThis 获取全局 uni（uni-app 编译期不会替换 globalThis 访问），
  // H5 运行时 window.uni 可能为空占位对象，需检查 reLaunch 方法再调用
  uniRef = (globalThis as { uni?: typeof uniRef }).uni
  // #endif
  if (!uniRef || typeof uniRef.reLaunch !== 'function') return
  // 先提取方法引用：闭包（showModal success）内 TS 类型收窄不会保留
  const launch = uniRef.reLaunch
  const isPasswordChanged = responseCode === 'AUTH_PASSWORD_CHANGED'
  if (isPasswordChanged) {
    if (typeof uniRef.showModal !== 'function') return
    uniRef.showModal({
      title: '密码已变更',
      content: '管理员已修改您的密码，请重新登录',
      showCancel: false,
      confirmText: '去登录',
      success() {
        launch({ url: '/pages/role-select/index' })
      }
    })
  } else {
    launch({ url: '/pages/role-select/index' })
  }
}

function showErrorToast(message: string) {
  let uniRef: { showToast?: (options: { title: string; icon: string; duration: number }) => void } | undefined
  // #ifdef MP-WEIXIN
  uniRef = (globalThis as { wx?: typeof uniRef }).wx
  // #endif
  // #ifndef MP-WEIXIN
  // H5 运行时 window.uni 可能为空占位对象，后续调用前已做存在性检查
  uniRef = (globalThis as { uni?: typeof uniRef }).uni
  // #endif
  uniRef?.showToast?.({ title: message, icon: 'none', duration: 2500 })
}

// #ifndef MP-WEIXIN
export function createHttpClient(config?: AxiosRequestConfig) {
  const client = axios.create({
    timeout: 10000,
    ...config
  })

  client.interceptors.request.use((requestConfig) => {
    const session = getH5Session()
    if (session?.token) {
      requestConfig.headers = requestConfig.headers ?? {}
      requestConfig.headers.Authorization = `Bearer ${session.token}`
    }

    return requestConfig
  })

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
  const translateError = (msg: string): string => {
    if (!msg) return '操作失败，请稍后重试'
    for (const [key, value] of Object.entries(ERROR_MAP)) {
      if (msg.includes(key)) return value
    }
    return msg
  }

  client.interceptors.response.use(
    (response: any): any => {
      const payload = response.data as ApiResponse<unknown>
      if (!payload || typeof payload !== 'object' || payload.success !== true) {
        const msg = translateError(payload?.message || '请求失败，请稍后重试')
        if (!(response.config as { silent?: boolean })?.silent) {
          showErrorToast(msg)
        }
        throw new HttpResponseError(msg, response.status)
      }

      return payload.data
    },
    (error: AxiosError<ApiResponse<unknown>>) => {
      const status = error.response?.status
      if (status === 401) {
        const responseCode = error.response?.data?.code
        handle401(responseCode)
      }

      const rawMsg = error.response?.data?.message || getErrorMessage(error)
      const message = translateError(rawMsg)
      const silent = (error.config as { silent?: boolean } | undefined)?.silent === true
      if (status !== 401 && !silent) {
        showErrorToast(message)
      }
      return Promise.reject(new HttpResponseError(message, status))
    }
  )

  return client as AxiosInstance
}

const axiosClient = createHttpClient({
  baseURL: resolveApiBaseUrl()
})

const axiosHttp: HttpLikeClient = {
  get: (url, config) => (axiosClient as any).get(url, config),
  post: (url, data, config) => (axiosClient as any).post(url, data, config),
  put: (url, data, config) => (axiosClient as any).put(url, data, config),
  delete: (url, config) => (axiosClient as any).delete(url, config),
  upload: (url, filePath, formData) => {
    const form = new FormData()
    form.append('file', filePath as any)
    if (formData) {
      Object.entries(formData).forEach(([key, value]) => {
        if (value != null) form.append(key, String(value))
      })
    }
    return (axiosClient as any).post(url, form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
// #endif

// #ifdef MP-WEIXIN
function createUniHttpClient(): HttpLikeClient {
  type UniRequestMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

  const resolveRequestBaseUrl = (config?: unknown): string => {
    const base = (config as { baseURL?: string } | undefined)?.baseURL
    return typeof base === 'string' && base.trim().length > 0 ? base.trim() : resolveApiBaseUrl()
  }

  const request = <R>(method: UniRequestMethod, url: string, data?: unknown, config?: unknown): Promise<R> => {
    const silent = (config as { silent?: boolean } | undefined)?.silent === true
    return new Promise((resolve, reject) => {
      const session = getH5Session()
      const headers: Record<string, string> = {}
      if (session?.token) {
        headers.Authorization = `Bearer ${session.token}`
      }

      uni.request({
        url: `${resolveRequestBaseUrl(config)}${url}`,
        method,
        data: data as any,
        header: headers,
        success: (response) => {
          const payload = response.data as ApiResponse<R>
          if (!payload || typeof payload !== 'object' || payload.success !== true) {
            const message = payload && typeof payload === 'object' && 'message' in payload ? String(payload.message || '请求失败，请稍后重试') : '请求失败，请稍后重试'
            if (response.statusCode === 401) {
              const responseCode = payload && typeof payload === 'object' && 'code' in payload ? String(payload.code ?? '') : undefined
              handle401(responseCode)
            } else if (!silent) {
              showErrorToast(message)
            }
            reject(new HttpResponseError(message, response.statusCode))
            return
          }

          resolve(payload.data)
        },
        fail: (error) => {
          const msg = getErrorMessage(error)
          if (!silent) {
            showErrorToast(msg)
          }
          reject(new HttpResponseError(msg))
        }
      })
    })
  }

  return {
    get(url, config) {
      return request('GET', url, undefined, config)
    },
    post(url, data, config) {
      return request('POST', url, data, config)
    },
    put(url, data, config) {
      return request('PUT', url, data, config)
    },
    delete(url, config) {
      return request('DELETE', url, undefined, config)
    },
    upload(url, filePath, formData) {
      return new Promise((resolve, reject) => {
        const session = getH5Session()
        const headers: Record<string, string> = {}
        if (session?.token) {
          headers.Authorization = `Bearer ${session.token}`
        }
        uni.uploadFile({
          url: `${resolveApiBaseUrl()}${url}`,
          filePath: filePath as string,
          name: 'file',
          formData: formData as Record<string, string>,
          header: headers,
          success: (response) => {
            try {
              const payload = JSON.parse(response.data)
              if (payload && payload.success) {
                resolve(payload.data)
              } else {
                reject(new HttpResponseError(payload?.message || '上传失败'))
              }
            } catch (e) {
              reject(new HttpResponseError('上传失败'))
            }
          },
          fail: () => reject(new HttpResponseError('上传失败'))
        })
      })
    }
  }
}

// #endif

// 默认导出 http：小程序环境为 uni.request 实现，其他环境为 axios 实现
// 用条件赋值避开两处 export 同名变量的 TS 重复声明误报
let http: HttpLikeClient
// #ifdef MP-WEIXIN
// 微信小程序环境：uni.request 实现
http = createUniHttpClient()
// #endif
// #ifndef MP-WEIXIN
// axios 版本（非微信小程序环境）
http = axiosHttp
// #endif
export { http }
