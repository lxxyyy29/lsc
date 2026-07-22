import { AxiosHeaders, type AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createHttpClient } from '../api/http'
import { clearWebSession, getWebSession, persistWebSession } from '../auth/session'

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
    token: 'web-token-1',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes: ['menu:dashboard:view', 'menu:event:list'],
    menuPermissionCodes: ['menu:dashboard:view', 'menu:event:list']
  }
}

describe('http client', () => {
  beforeEach(() => {
    storageState.clear()
    clearWebSession()
  })

  afterEach(() => {
    storageState.clear()
    clearWebSession()
  })

  it('injects bearer token from current session into request headers', async () => {
    persistWebSession(createValidSession())
    const client = createHttpClient()
    const [requestInterceptor] = client.interceptors.request.handlers ?? []
    expect(requestInterceptor?.fulfilled).toBeTypeOf('function')

    const config = await requestInterceptor!.fulfilled({ headers: new AxiosHeaders() })

    expect(config.headers.Authorization).toBe('Bearer web-token-1')
  })

  it('unwraps ApiResponse payloads from successful responses', async () => {
    const client = createHttpClient()
    const [responseInterceptor] = client.interceptors.response.handlers ?? []
    expect(responseInterceptor?.fulfilled).toBeTypeOf('function')

    const result = await responseInterceptor!.fulfilled!({
      data: {
        success: true,
        code: 'OK',
        message: 'Success',
        data: { id: 1, name: 'admin' }
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: { headers: new AxiosHeaders() }
    } as AxiosResponse)

    expect(result).toEqual({ id: 1, name: 'admin' })
  })

  it('clears session on 401 auth failure', async () => {
    persistWebSession(createValidSession())
    const client = createHttpClient()
    const [responseInterceptor] = client.interceptors.response.handlers ?? []
    expect(responseInterceptor?.rejected).toBeTypeOf('function')

    const rejected = responseInterceptor!.rejected!({
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

    await expect(rejected).rejects.toThrow('auth failed')
    expect(getWebSession()).toBeNull()
  })

  it('preserves session on 403 permission failure', async () => {
    persistWebSession(createValidSession())
    const client = createHttpClient()
    const [responseInterceptor] = client.interceptors.response.handlers ?? []
    expect(responseInterceptor?.rejected).toBeTypeOf('function')

    const rejected = responseInterceptor!.rejected!({
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

    await expect(rejected).rejects.toThrow('forbidden')
    expect(getWebSession()).not.toBeNull()
  })
})
