import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createHttpClient, HttpResponseError, http } from '../api/http'
import { clearH5Session, getH5Session, logoutH5, persistH5Session } from '../api/auth'

const storageState = new Map<string, string>()

Object.defineProperty(window, 'localStorage', {
  value: {
    getItem(key: string) {
      return storageState.has(key) ? storageState.get(key)! : null
    },
    setItem(key: string, value: string) {
      storageState.set(key, value)
    },
    removeItem(key: string) {
      storageState.delete(key)
    }
  },
  configurable: true
})

function createValidSession() {
  return {
    token: 'h5-token-1',
    userId: 1,
    userName: '巡查员张三',
    account: 'inspector',
    roleCodes: ['H5_WORKER'],
    permissionCodes: ['menu:h5:workbench:view', 'menu:h5:workorder:list'],
    menuPermissionCodes: ['menu:h5:workbench:view', 'menu:h5:workorder:list']
  }
}

describe('h5 http client', () => {
  beforeEach(() => {
    storageState.clear()
    clearH5Session()
  })

  afterEach(() => {
    storageState.clear()
    clearH5Session()
  })

  it('injects bearer token from current session into request headers', async () => {
    persistH5Session(createValidSession())
    const client = createHttpClient()
    const [requestInterceptor] = (client.interceptors.request as any).handlers

    const config = await requestInterceptor.fulfilled({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer h5-token-1')
  })

  it('unwraps ApiResponse payloads from successful responses', async () => {
    const client = createHttpClient()
    const [responseInterceptor] = (client.interceptors.response as any).handlers

    const result = await responseInterceptor.fulfilled({
      status: 200,
      data: {
        success: true,
        code: 'OK',
        message: 'Success',
        data: { id: 1, name: 'worker' }
      }
    })

    expect(result).toEqual({ id: 1, name: 'worker' })
  })

  it('preserves backend message for failed ApiResponse payloads on 200 responses', () => {
    const client = createHttpClient()
    const [responseInterceptor] = (client.interceptors.response as any).handlers

    expect(() =>
      responseInterceptor.fulfilled({
        status: 200,
        data: {
          success: false,
          code: 'AUTH_INVALID_CREDENTIALS',
          message: '账号或密码错误',
          data: null
        }
      })
    ).toThrowError('账号或密码错误')
  })

  it('clears session on 401 auth failure', async () => {
    persistH5Session(createValidSession())
    const client = createHttpClient()
    const [responseInterceptor] = (client.interceptors.response as any).handlers

    const rejected = responseInterceptor.rejected({
      response: {
        status: 401,
        data: {
          success: false,
          code: 'AUTH_FAILED',
          message: 'auth failed',
          data: null
        }
      }
    })

    await expect(rejected).rejects.toEqual(expect.any(HttpResponseError))
    await expect(rejected).rejects.toThrow('auth failed')
    expect(getH5Session()).toBeNull()
  })

  it('preserves session on 403 permission failure', async () => {
    persistH5Session(createValidSession())
    const client = createHttpClient()
    const [responseInterceptor] = (client.interceptors.response as any).handlers

    const rejected = responseInterceptor.rejected({
      response: {
        status: 403,
        data: {
          success: false,
          code: 'AUTH_FORBIDDEN',
          message: 'forbidden',
          data: null
        }
      }
    })

    await expect(rejected).rejects.toEqual(expect.any(HttpResponseError))
    await expect(rejected).rejects.toThrow('forbidden')
    expect(getH5Session()).not.toBeNull()
  })

  it('clears local session even if logout transport fails', async () => {
    persistH5Session(createValidSession())
    vi.spyOn(http, 'post').mockRejectedValueOnce(new HttpResponseError('Network Error'))

    await expect(logoutH5()).rejects.toThrow('Network Error')
    expect(getH5Session()).toBeNull()
  })
})
