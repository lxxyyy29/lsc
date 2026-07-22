import { render, screen } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { clearWebSession, persistWebSession, type WebSession } from '../auth/session'
import AdminShellLayout from './AdminShellLayout.vue'

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

function createSession(menuPermissionCodes: string[], permissionCodes: string[] = menuPermissionCodes): WebSession {
  return {
    token: 'web-layout-token',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes,
    menuPermissionCodes
  }
}

beforeEach(() => {
  storageState.clear()
  clearWebSession()
})

afterEach(() => {
  storageState.clear()
  clearWebSession()
})

describe('AdminShellLayout', () => {
  it('renders permission-filtered navigation inside a dedicated left navigation rail', () => {
    persistWebSession(createSession(['menu:dashboard:view', 'menu:event:list', 'menu:process:list']))

    render(AdminShellLayout, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          },
          RouterView: {
            template: '<section data-testid="layout-view">页面主体内容</section>'
          }
        }
      }
    })

    expect(screen.getByRole('navigation', { name: '主导航' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '首页' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '事件中心' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '流程配置' })).toBeTruthy()
    expect(screen.queryByText('业务管理')).toBeNull()
    expect(screen.queryByRole('link', { name: '工单中心' })).toBeNull()
    expect(screen.queryByRole('link', { name: '用户管理' })).toBeNull()
  })

  it('renders a dark topbar, account area, logout action, and main content region', () => {
    persistWebSession(createSession(['menu:dashboard:view']))

    render(AdminShellLayout, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          },
          RouterView: {
            template: '<section data-testid="layout-view">页面主体内容</section>'
          }
        }
      }
    })

    expect(screen.getByRole('banner')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '居里智能低空巡检综合监管平台' })).toBeTruthy()
    expect(screen.getByText('管理员')).toBeTruthy()
    expect(screen.getByRole('button', { name: '退出登录' })).toBeTruthy()
    expect(screen.getByRole('main').className).toContain('admin-shell__main')
    expect(screen.getByTestId('layout-view')).toBeTruthy()
  })
})
