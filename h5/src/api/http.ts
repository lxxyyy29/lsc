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
  return 'https://drone.kfktec.cn:8768/api/h5'
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
  const uniRef = typeof uni !== 'undefined' ? uni : (globalThis as any).uni
  if (!uniRef) return
  const isPasswordChanged = responseCode === 'AUTH_PASSWORD_CHANGED'
  if (isPasswordChanged) {
    uniRef.showModal({
      title: '密码已变更',
      content: '管理员已修改您的密码，请重新登录',
      showCancel: false,
      confirmText: '去登录',
      success() {
        uniRef.reLaunch({ url: '/pages/login/index' })
      }
    })
  } else {
    uniRef.reLaunch({ url: '/pages/login/index' })
  }
}

function showErrorToast(message: string) {
  const uniRef = typeof uni !== 'undefined' ? uni : (globalThis as any).uni
  if (uniRef?.showToast) {
    uniRef.showToast({ title: message, icon: 'none', duration: 2500 })
  }
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

  client.interceptors.response.use(
    (response: any): any => {
      const payload = response.data as ApiResponse<unknown>
      if (!payload || typeof payload !== 'object' || payload.success !== true) {
        const msg = payload?.message || '请求失败，请稍后重试'
        showErrorToast(msg)
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

      const message = error.response?.data?.message || getErrorMessage(error)
      if (status !== 401) {
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

  const request = <R>(method: UniRequestMethod, url: string, data?: unknown): Promise<R> => {
    return new Promise((resolve, reject) => {
      const session = getH5Session()
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
          const payload = response.data as ApiResponse<R>
          if (!payload || typeof payload !== 'object' || payload.success !== true) {
            const message = payload && typeof payload === 'object' && 'message' in payload ? String(payload.message || '请求失败，请稍后重试') : '请求失败，请稍后重试'
            if (response.statusCode === 401) {
              const responseCode = payload && typeof payload === 'object' && 'code' in payload ? String(payload.code ?? '') : undefined
              handle401(responseCode)
            } else {
              showErrorToast(message)
            }
            reject(new HttpResponseError(message, response.statusCode))
            return
          }

          resolve(payload.data)
        },
        fail: (error) => {
          const msg = getErrorMessage(error)
          showErrorToast(msg)
          reject(new HttpResponseError(msg))
        }
      })
    })
  }

  return {
    get(url) {
      return request('GET', url)
    },
    post(url, data) {
      return request('POST', url, data)
    },
    put(url, data) {
      return request('PUT', url, data)
    },
    delete(url) {
      return request('DELETE', url)
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

// 默认导出 axios 版本（非微信小程序环境）
// 微信小程序环境需要在构建时替换
export { axiosHttp as http }
