import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

type HttpLikeClient = {
  get<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
  post<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  put<T = unknown, R = T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<R>
  delete<T = unknown, R = T>(url: string, config?: AxiosRequestConfig): Promise<R>
}
import { clearWebSession, getWebSession } from '../auth/session'

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
    if (error.message === 'Network Error') return '网络连接失败，请检查网络'
    if (error.message.includes('timeout')) return '请求超时，请稍后重试'
    if (error.message.startsWith('Request failed')) return '请求失败，请稍后重试'
    return error.message
  }

  return '请求失败，请稍后重试'
}

export function createHttpClient(config?: AxiosRequestConfig) {
  const client = axios.create({
    timeout: 10000,
    ...config
  })

  client.interceptors.request.use((requestConfig) => {
    const session = getWebSession()
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
        const msg = payload?.message || '请求失败，请稍后重试'
        ElMessage.error(msg)
        throw new Error(msg)
      }

      return payload.data as any
    },
    (error: AxiosError<ApiResponse<unknown>>) => {
      const status = error.response?.status
      if (status === 401) {
        clearWebSession()
      }

      const message = error.response?.data?.message || getErrorMessage(error)
      if (status !== 401) {
        ElMessage.error(message)
      }
      return Promise.reject(new HttpResponseError(message, status))
    }
  )

  return client as AxiosInstance & HttpLikeClient
}

export const http: HttpLikeClient = createHttpClient({
  baseURL: '/api'
})
