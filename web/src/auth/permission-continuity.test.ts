import { afterEach, describe, expect, it } from 'vitest'
import { createAppRouter } from '../router'
import { clearWebSession, persistWebSession, type WebSession } from './session'

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

function createSession(menuPermissionCodes: string[]): WebSession {
  return {
    token: 'permission-test-token',
    userId: 1,
    userName: '权限测试用户',
    account: 'permission-tester',
    roleCodes: ['admin'],
    permissionCodes: [...menuPermissionCodes],
    menuPermissionCodes
  }
}

afterEach(() => {
  storageState.clear()
  clearWebSession()
})

describe('audit menu permission continuity', () => {
  it('keeps /audits protected by menu:audit:list after the list page rebuild', async () => {
    persistWebSession(createSession(['menu:event:list']))

    const router = createAppRouter()
    await router.push('/audits')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/events')
  })

  it('still allows /audits when menu:audit:list is present', async () => {
    persistWebSession(createSession(['menu:event:list', 'menu:audit:list']))

    const router = createAppRouter()
    await router.push('/audits')
    await router.isReady()

    expect(router.currentRoute.value.path).toBe('/audits')
  })
})
