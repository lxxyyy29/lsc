import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from '../App.vue'
import { clearWebSession, persistWebSession } from '../auth/session'
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

function createValidSession() {
  return {
    token: 'web-token-1',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes: [
      'menu:dashboard:view',
      'menu:event:list',
      'menu:audit:list',
      'menu:process:list',
      'menu:workorder:list',
      'menu:patrol-task:list',
      'menu:drone:list',
      'menu:map:view',
      'menu:system:view'
    ],
    menuPermissionCodes: [
      'menu:dashboard:view',
      'menu:event:list',
      'menu:audit:list',
      'menu:process:list',
      'menu:workorder:list',
      'menu:patrol-task:list',
      'menu:drone:list',
      'menu:map:view',
      'menu:system:view'
    ]
  }
}

async function renderAt(path: string) {
  const router = createAppRouter()
  await router.push(path)

  return {
    router,
    ...render(App, {
      global: {
        plugins: [createPinia(), router, ElementPlus]
      }
    })
  }
}

beforeEach(() => {
  storageState.clear()
  clearWebSession()
  persistWebSession(createValidSession())
})

afterEach(() => {
  cleanup()
  storageState.clear()
  clearWebSession()
})

describe('skeleton pages', () => {
  it('renders dashboard cockpit metrics, shortcut, alert, and workflow sections', async () => {
    await renderAt('/dashboard')

    expect(await screen.findByRole('heading', { name: '仪表盘' })).toBeTruthy()
    expect(screen.getByText('运营热力图')).toBeTruthy()
    expect(screen.getByText('接入设备')).toBeTruthy()
    expect(screen.getByText('高频操作')).toBeTruthy()
    expect(screen.getByText('最新告警')).toBeTruthy()
    expect(screen.getByText('核心指标')).toBeTruthy()
    expect(screen.getByText('待处理工单与审核流程进度')).toBeTruthy()
    expect(screen.getByText('重点区域持续活跃')).toBeTruthy()
  })

  it('renders drone page with filters, device status, and monitor panel', async () => {
    await renderAt('/drones')

    expect((await screen.findAllByRole('heading', { name: '无人机接入档案' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('主列表').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('在线状态')).toBeTruthy()
    expect(screen.getByLabelText('平台来源')).toBeTruthy()
    expect(screen.getByText('监控面板')).toBeTruthy()
  })

  it('renders oversight page as the command-style supervision board', async () => {
    await renderAt('/oversight')

    expect(await screen.findByRole('heading', { name: '综合监管总览' })).toBeTruthy()
    expect(screen.getByText('重点片区热力图')).toBeTruthy()
    expect(screen.getByText('实时播报')).toBeTruthy()
    expect(screen.getByText('今日闭环进展')).toBeTruthy()
  })

  it('renders maps page as the dedicated map overview entry', async () => {
    const { router } = await renderAt('/maps')

    expect(router.currentRoute.value.path).toBe('/maps')
    expect(await screen.findByRole('heading', { name: '地图总览' })).toBeTruthy()
    expect(screen.getByLabelText('监管区域')).toBeTruthy()
    expect(screen.getByLabelText('监管主题')).toBeTruthy()
    expect(screen.getByText('综合态势地图')).toBeTruthy()
  })

  it('renders authorization, model config, media, and system config pages', async () => {
    await renderAt('/system')

    expect((await screen.findAllByRole('heading', { name: '授权管理' })).length).toBeGreaterThan(0)
    expect(screen.getByText('角色权限矩阵')).toBeTruthy()
    expect(screen.getByText('动态菜单清单')).toBeTruthy()

    await renderAt('/system/model-config')

    expect((await screen.findAllByRole('heading', { name: 'AI 模型配置' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('主列表').length).toBeGreaterThan(0)
    expect(screen.getByText('已发布模型')).toBeTruthy()

    await renderAt('/media/results')

    expect((await screen.findAllByRole('heading', { name: '媒体上传' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('主列表').length).toBeGreaterThan(0)
    expect(screen.getByText('今日上传')).toBeTruthy()

    await renderAt('/system/config')

    expect((await screen.findAllByRole('heading', { name: '系统配置' })).length).toBeGreaterThan(0)
    expect(screen.getByText('接口配置')).toBeTruthy()
    expect(screen.getByText('参数配置')).toBeTruthy()
  })

  it('renders patrol list/detail pages with drone mission content', async () => {
    await renderAt('/patrol-tasks')

    expect((await screen.findAllByRole('heading', { name: '巡检任务' })).length).toBeGreaterThan(0)
    expect(screen.getAllByText('主列表').length).toBeGreaterThan(0)
    expect(screen.getByText('计划起飞时间')).toBeTruthy()
    expect(screen.getByText('任务名称')).toBeTruthy()
    expect(screen.getByText('操作')).toBeTruthy()

    await renderAt('/patrol-tasks/1')

    expect(await screen.findByRole('heading', { name: '巡查任务详情' })).toBeTruthy()
    expect(screen.getByText('执行安排')).toBeTruthy()
    expect(screen.getByText('关联链路')).toBeTruthy()
    expect(screen.getByText('核查提示')).toBeTruthy()
  })
})
