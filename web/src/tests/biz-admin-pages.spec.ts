import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from '../App.vue'
import { clearWebSession, persistWebSession, type WebSession } from '../auth/session'
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

vi.mock('../api/biz-area', () => ({
  listBizAreas: vi.fn(async () => [
    {
      id: 1,
      areaName: '中心片区',
      principalName: '张三',
      principalPhone: '13800138000',
      roiJson: '[{"lng":113.1,"lat":23.1},{"lng":113.2,"lat":23.2},{"lng":113.3,"lat":23.3}]',
      remark: '核心区域',
      status: 'ACTIVE',
      updatedAt: '2026-03-26 10:00:00'
    }
  ]),
  getBizAreaDetail: vi.fn(async () => ({
    id: 1,
    areaName: '中心片区',
    principalName: '张三',
    principalPhone: '13800138000',
    roiJson: '[{"lng":113.1,"lat":23.1},{"lng":113.2,"lat":23.2},{"lng":113.3,"lat":23.3}]',
    remark: '核心区域',
    status: 'ACTIVE',
    updatedAt: '2026-03-26 10:00:00'
  })),
  listBizAreaOptions: vi.fn(async () => [{ id: 1, areaName: '中心片区' }]),
  createBizArea: vi.fn(async (payload) => ({ id: 2, ...payload, createdAt: '2026-03-26 10:00:00', updatedAt: '2026-03-26 10:00:00' })),
  updateBizArea: vi.fn(async (_id, payload) => ({ id: 1, ...payload, createdAt: '2026-03-26 09:00:00', updatedAt: '2026-03-26 10:00:00' })),
  deleteBizArea: vi.fn(async () => undefined)
}))

vi.mock('../api/biz-merchant', () => ({
  listBizMerchants: vi.fn(async () => [
    {
      id: 1,
      merchantName: '示例商户',
      merchantPhotoUrl: 'https://example.com/merchant.png',
      longitude: 113.12,
      latitude: 23.12,
      legalPersonName: '李四',
      legalPersonPhotoUrl: 'https://example.com/legal.png',
      legalPersonPhone: '13900139000',
      areaId: 1,
      areaName: '中心片区',
      areaMatchMode: 'MANUAL',
      remark: '重点巡查',
      status: 'ACTIVE',
      updatedAt: '2026-03-26 10:00:00'
    }
  ]),
  getBizMerchantDetail: vi.fn(async () => ({
    id: 1,
    merchantName: '示例商户',
    merchantPhotoUrl: 'https://example.com/merchant.png',
    longitude: 113.12,
    latitude: 23.12,
    legalPersonName: '李四',
    legalPersonPhotoUrl: 'https://example.com/legal.png',
    legalPersonPhone: '13900139000',
    areaId: 1,
    areaName: '中心片区',
    areaMatchMode: 'MANUAL',
    remark: '重点巡查',
    status: 'ACTIVE',
    updatedAt: '2026-03-26 10:00:00'
  })),
  createBizMerchant: vi.fn(async (payload) => ({ id: 2, ...payload, areaName: '中心片区', createdAt: '2026-03-26 10:00:00', updatedAt: '2026-03-26 10:00:00' })),
  updateBizMerchant: vi.fn(async (_id, payload) => ({ id: 1, ...payload, areaName: '中心片区', createdAt: '2026-03-26 09:00:00', updatedAt: '2026-03-26 10:00:00' })),
  deleteBizMerchant: vi.fn(async () => undefined)
}))

vi.mock('../api/biz-vendor', () => ({
  listBizVendors: vi.fn(async () => [
    {
      id: 1,
      vendorName: '示例摊贩',
      vendorPhotoUrl: 'https://example.com/vendor.png',
      legalPersonName: '王五',
      legalPersonPhotoUrl: 'https://example.com/vendor-legal.png',
      legalPersonPhone: '13700137000',
      remark: '夜市档口',
      status: 'ACTIVE',
      updatedAt: '2026-03-26 10:00:00'
    }
  ]),
  getBizVendorDetail: vi.fn(async () => ({
    id: 1,
    vendorName: '示例摊贩',
    vendorPhotoUrl: 'https://example.com/vendor.png',
    legalPersonName: '王五',
    legalPersonPhotoUrl: 'https://example.com/vendor-legal.png',
    legalPersonPhone: '13700137000',
    remark: '夜市档口',
    status: 'ACTIVE',
    updatedAt: '2026-03-26 10:00:00'
  })),
  createBizVendor: vi.fn(async (payload) => ({ id: 2, ...payload, createdAt: '2026-03-26 10:00:00', updatedAt: '2026-03-26 10:00:00' })),
  updateBizVendor: vi.fn(async (_id, payload) => ({ id: 1, ...payload, createdAt: '2026-03-26 09:00:00', updatedAt: '2026-03-26 10:00:00' })),
  deleteBizVendor: vi.fn(async () => undefined)
}))

function createValidSession(overrides: Partial<WebSession> = {}): WebSession {
  return {
    token: 'web-token-1',
    userId: 1,
    userName: '管理员',
    account: 'admin',
    roleCodes: ['SUPER_ADMIN'],
    permissionCodes: [
      'menu:dashboard:view',
      'menu:biz:area',
      'menu:biz:merchant',
      'menu:biz:vendor'
    ],
    menuPermissionCodes: [
      'menu:dashboard:view',
      'menu:biz:area:list',
      'menu:biz:merchant:list',
      'menu:biz:vendor:list'
    ],
    ...overrides
  }
}

async function renderAt(path: string, session: WebSession = createValidSession()) {
  persistWebSession(session)
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
})

afterEach(() => {
  cleanup()
  storageState.clear()
  clearWebSession()
})

describe('business admin pages', () => {
  it('renders business nav group and all three business pages', async () => {
    const { router } = await renderAt('/areas')

    expect(await screen.findByText('业务管理')).toBeTruthy()
    expect(screen.getByRole('link', { name: '片区管理' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '商户管理' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '流动摊贩管理' })).toBeTruthy()
    expect(screen.getByText('片区列表')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增片区' })).toBeTruthy()

    await router.push('/merchants')
    expect(await screen.findByText('商户列表')).toBeTruthy()
    expect(screen.getByLabelText('所属片区筛选')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增商户' })).toBeTruthy()

    await router.push('/mobile-vendors')
    expect(await screen.findByText('流动摊贩列表')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增摊贩' })).toBeTruthy()
  })

  it('shows business create buttons from menu permissions without button permissions', async () => {
    const session = createValidSession({
      permissionCodes: createValidSession().permissionCodes.filter((code) => !code.startsWith('button:biz:'))
    })
    const { router } = await renderAt('/areas', session)

    expect(await screen.findByText('片区列表')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增片区' })).toBeTruthy()

    await router.push('/merchants')
    expect(await screen.findByText('商户列表')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增商户' })).toBeTruthy()

    await router.push('/mobile-vendors')
    expect(await screen.findByText('流动摊贩列表')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增摊贩' })).toBeTruthy()
  })

  it('shows roi validation message when area form submits invalid roi json', async () => {
    await renderAt('/areas')

    await fireEvent.click(screen.getByRole('button', { name: '新增片区' }))
    const dialog = (screen.getByText('业务管理 / 片区档案').closest('.system-dialog') as HTMLElement | null) ?? document.body
    await fireEvent.update(within(dialog).getByLabelText('片区名称'), '测试片区')
    await fireEvent.update(within(dialog).getByLabelText('ROI JSON'), '[{"lng":113.1,"lat":23.1}]')
    await fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByText('ROI JSON 必须是至少 3 个{lng,lat}点的数组')).toBeTruthy()
  })
})
