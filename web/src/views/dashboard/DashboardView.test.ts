import { render, screen } from '@testing-library/vue'
import { describe, expect, it, beforeEach, afterEach, afterAll } from 'vitest'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import DashboardView from './DashboardView.vue'
import { clearWebSession, persistWebSession, type WebSession } from '../../auth/session'
import { createAppRouter } from '../../router'

const storageState = new Map<string, string>()
const originalLocalStorageDescriptor = Object.getOwnPropertyDescriptor(window, 'localStorage')

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
    token: 'web-token-1',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes: menuPermissionCodes,
    menuPermissionCodes
  }
}

async function renderWithRouter(menuPermissionCodes: string[]) {
  persistWebSession(createSession(menuPermissionCodes))
  const router = createAppRouter()
  await router.push('/dashboard')
  await router.isReady()

  render(DashboardView, {
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
  storageState.clear()
  clearWebSession()
})

describe('DashboardView', () => {
  it('renders the figma-style dashboard sections for metrics, shortcuts, alerts, and workflow overview', async () => {
    await renderWithRouter([
      'menu:dashboard:view',
      'menu:event:list',
      'menu:audit:list',
      'menu:workorder:list',
      'menu:map:view'
    ])

    expect(screen.getByRole('heading', { name: '运营热力图' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '高频操作' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '最新告警' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '核心指标' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '待处理工单与审核流程进度' })).toBeTruthy()
    expect(screen.getByText('接入设备')).toBeTruthy()
    expect(screen.getByText('待审核识别结果')).toBeTruthy()
    expect(screen.getByText('火车站周边')).toBeTruthy()
    expect(screen.getByText('待完成 12 起事件初审')).toBeTruthy()
    expect(screen.getByRole('link', { name: /事件中心\s+查看\s+快速进入来源事件台账与详情核查。/ }).getAttribute('href')).toBe('/events')
    expect(screen.getByText('铁路站周边车辆及车牌异常聚集')).toBeTruthy()
    expect(screen.getByText('桥沥村复核链路待闭环')).toBeTruthy()
    expect(screen.getByText('重点区域态势已在地图总览统一呈现，此处同步展示协同关注重点。')).toBeTruthy()
  })

  it('shows only shortcut links allowed by menu permissions', async () => {
    await renderWithRouter(['menu:dashboard:view', 'menu:event:list', 'menu:workorder:list'])

    expect(screen.getByRole('link', { name: /事件中心\s+查看\s+快速进入来源事件台账与详情核查。/ }).getAttribute('href')).toBe('/events')
    expect(screen.getByRole('link', { name: /工单中心\s+管理\s+跟进派单、现场反馈与关单确认。/ }).getAttribute('href')).toBe('/work-orders')
    expect(screen.queryByRole('link', { name: /审核中心\s+进入\s+继续处理待审核节点与流程送审动作。/ })).toBeNull()
    expect(screen.queryByRole('link', { name: /地图总览\s+查看\s+进入地图态势页查看区域热力与设备布点。/ })).toBeNull()
  })
})

afterAll(() => {
  if (originalLocalStorageDescriptor) {
    Object.defineProperty(window, 'localStorage', originalLocalStorageDescriptor)
    return
  }

  Reflect.deleteProperty(window, 'localStorage')
})
