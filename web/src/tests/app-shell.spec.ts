import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from '../App.vue'
import {
  WEB_SESSION_STORAGE_KEY,
  clearWebSession,
  getWebSession,
  persistWebSession,
  recoverWebSession
} from '../auth/session'
import { HttpResponseError } from '../api/http'
import { createAppRouter } from '../router'

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

function setStoredSession(rawValue: string) {
  storageState.set(WEB_SESSION_STORAGE_KEY, rawValue)
}

function createValidSession(overrides: Partial<ReturnType<typeof getWebSession>> = {}) {
  return {
    token: 'web-token-1',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes: ['menu:dashboard:view', 'menu:event:list'],
    menuPermissionCodes: ['menu:dashboard:view', 'menu:event:list'],
    ...overrides
  }
}

async function renderAt(path: string) {
  const router = createAppRouter()
  await router.push(path)

  render(App, {
    global: {
      plugins: [createPinia(), router, ElementPlus]
    }
  })

  return router
}

beforeEach(() => {
  storageState.clear()
  clearWebSession()
})

afterEach(() => {
  cleanup()
  storageState.clear()
  clearWebSession()
})

describe('app shell', () => {
  it('renders the real login route with account, password and submit button', async () => {
    const router = await renderAt('/login')

    expect(router.currentRoute.value.path).toBe('/login')
    expect(await screen.findByRole('heading', { name: '欢迎登录' })).toBeTruthy()
    expect(screen.getByText('居里智能低空巡检综合监管平台')).toBeTruthy()
    expect(screen.getByLabelText('账号')).toBeTruthy()
    expect(screen.getByLabelText('密码')).toBeTruthy()
    expect(screen.getByRole('button', { name: '登录平台' })).toBeTruthy()
  })

  it('redirects unauthenticated users from protected routes to /login', async () => {
    const router = await renderAt('/dashboard')

    expect(router.currentRoute.value.fullPath).toBe('/login?redirect=/dashboard')
    expect(await screen.findByRole('heading', { name: '欢迎登录' })).toBeTruthy()
    expect(screen.queryByRole('navigation', { name: '主导航' })).toBeNull()
  })

  it('allows protected navigation when a valid structured session exists', async () => {
    persistWebSession(createValidSession())

    const router = await renderAt('/dashboard')

    expect(router.currentRoute.value.path).toBe('/dashboard')
    expect(await screen.findByRole('heading', { name: '居里智能低空巡检综合监管平台' })).toBeTruthy()
    expect(screen.getByRole('navigation', { name: '主导航' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '首页' })).toBeTruthy()
    expect(screen.queryByRole('link', { name: '综合监管' })).toBeNull()
    expect(screen.getByText('管理员')).toBeTruthy()
    expect(screen.getByText('admin')).toBeTruthy()
    expect(screen.getByRole('button', { name: '退出登录' })).toBeTruthy()
    expect(screen.getByRole('main').textContent).toContain('待派单事件')
  })

  it.each(['{}', '{"token":"   "}', 'not-json'])(
    'rejects invalid stored session %s and clears it',
    async (rawValue) => {
      setStoredSession(rawValue)

      const router = await renderAt('/dashboard')

      expect(router.currentRoute.value.fullPath).toBe('/login?redirect=/dashboard')
      expect(await screen.findByRole('heading', { name: '欢迎登录' })).toBeTruthy()
      expect(storageState.has(WEB_SESSION_STORAGE_KEY)).toBe(false)
      expect(getWebSession()).toBeNull()
    }
  )

  it('keeps login redirect handling with structured session', async () => {
    persistWebSession(createValidSession())

    const router = await renderAt('/login?redirect=/events')

    expect(router.currentRoute.value.fullPath).toBe('/events')
    expect(await screen.findByRole('navigation', { name: '主导航' })).toBeTruthy()
  })

  it('recovers current user at startup when a token exists', async () => {
    persistWebSession(createValidSession({ userName: '旧名称' }))

    const recovered = await recoverWebSession(async () => ({
      id: 9,
      username: 'admin',
      realName: '系统管理员',
      phone: '13800000000',
      roleCodes: ['SUPER_ADMIN'],
      permissionCodes: ['menu:dashboard:view', 'menu:event:list', 'api:auth:me'],
      menuPermissionCodes: ['menu:dashboard:view', 'menu:event:list'],
      menuTree: createValidSession().menuTree
    }))

    expect(recovered).toBeTruthy()
    expect(getWebSession()).toEqual(
      expect.objectContaining({
        token: 'web-token-1',
        userId: 9,
        userName: '系统管理员',
        account: 'admin'
      })
    )
  })

  it('clears stored session when startup recovery confirms auth failure', async () => {
    persistWebSession(createValidSession())

    const recovered = await recoverWebSession(async () => {
      throw new HttpResponseError('expired token', 401)
    })

    expect(recovered).toBeNull()
    expect(getWebSession()).toBeNull()
    expect(storageState.has(WEB_SESSION_STORAGE_KEY)).toBe(false)
  })

  it('preserves stored session when startup recovery fails for non-auth reasons', async () => {
    persistWebSession(createValidSession())

    const recovered = await recoverWebSession(async () => {
      throw new Error('network unavailable')
    })

    expect(recovered).toEqual(expect.objectContaining({ token: 'web-token-1' }))
    expect(getWebSession()).toEqual(expect.objectContaining({ token: 'web-token-1' }))
    expect(storageState.has(WEB_SESSION_STORAGE_KEY)).toBe(true)
  })
})
