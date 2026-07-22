import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import App from '../App.vue'
import StatusTag from '../components/admin/StatusTag.vue'
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

function createValidSession(overrides: Partial<WebSession> = {}): WebSession {
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
      'menu:system:view',
      'menu:biz:area',
      'menu:biz:merchant',
      'menu:biz:vendor'
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
      'menu:system:view',
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

describe('core admin pages', () => {
  it('renders dashboard as a figma-style operations cockpit instead of a placeholder page', async () => {
    await renderAt('/dashboard')

    expect(await screen.findByRole('heading', { name: '仪表盘' })).toBeTruthy()
    expect(screen.getByText('待派单事件')).toBeTruthy()
    expect(screen.getByText('高频操作')).toBeTruthy()
    expect(screen.getByText('最新事件')).toBeTruthy()
    expect(screen.getByText('核心指标')).toBeTruthy()
    expect(screen.getByText('待处理工单与审核流程进度')).toBeTruthy()
    expect(screen.queryByText('仪表盘页面占位')).toBeNull()
  })

  it('renders event list as a figma-aligned ledger with shared list template sections and filters', async () => {
    await renderAt('/events')

    expect((await screen.findAllByRole('heading', { name: '事件列表' })).length).toBeGreaterThan(0)
    expect(screen.getByTestId('web-list-page-template')).toBeTruthy()
    expect(screen.getByText('事件总量')).toBeTruthy()
    expect(screen.getByText('待审核事件')).toBeTruthy()
    expect(screen.getByText('待派单事件')).toBeTruthy()
    expect(screen.getByText('处理中事件')).toBeTruthy()
    expect(screen.getByLabelText('来源类型')).toBeTruthy()
    expect(screen.getByLabelText('事件类型')).toBeTruthy()
    expect(screen.getByLabelText('当前状态')).toBeTruthy()
    expect(screen.getByText('发生时间')).toBeTruthy()
    expect(screen.getByText('区域')).toBeTruthy()
    expect(screen.getByText('操作')).toBeTruthy()
  })

  it('filters event list rows by selected source type', async () => {
    await renderAt('/events')

    expect(await screen.findByText('EV-20260314-001')).toBeTruthy()
    expect(screen.getByText('EV-20260314-002')).toBeTruthy()

    await fireEvent.update(screen.getByLabelText('来源类型'), '无人机识别')

    expect(await screen.findByText('EV-20260314-001')).toBeTruthy()
    expect(screen.queryByText('EV-20260314-002')).toBeNull()
  })

  it('renders event detail through the shared web detail page template', async () => {
    await renderAt('/events/1')

    expect(await screen.findByRole('heading', { name: '事件详情' })).toBeTruthy()
    expect(screen.getByTestId('web-detail-page-template')).toBeTruthy()
    expect(screen.getByText('事件摘要')).toBeTruthy()
    expect(screen.getByText('桥沥村疑似违法施工')).toBeTruthy()
    expect(screen.getByText('事件编号')).toBeTruthy()
    expect(screen.getAllByText('EV-20260314-001').length).toBeGreaterThan(0)
    expect(screen.getByText('处置坐标')).toBeTruthy()
    expect(screen.getByText('113.9982, 22.9811')).toBeTruthy()
    expect(screen.getByText('取证附件')).toBeTruthy()
    expect(screen.getByText('现场照片-01.jpg')).toBeTruthy()
    expect(screen.getByText('现场视频-01.mp4')).toBeTruthy()
    expect(screen.getByText('流转概览')).toBeTruthy()
    expect(screen.getByText('事件接入')).toBeTruthy()
    expect(screen.getByText('审核通过')).toBeTruthy()
    expect(screen.getByText('等待派单')).toBeTruthy()
    expect(screen.queryByText('核心信息')).toBeNull()
    expect(screen.queryByText('媒体取证')).toBeNull()
    expect(screen.queryByText('生命周期记录')).toBeNull()
  })

  it('updates event detail when navigating to another id on the same component', async () => {
    const { router } = await renderAt('/events/1')

    expect((await screen.findAllByText('EV-20260314-001')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('低空巡查接入平台').length).toBeGreaterThan(0)

    await router.push('/events/2')

    expect((await screen.findAllByText('EV-20260314-002')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('视频算法平台').length).toBeGreaterThan(0)
  })

  it('shows not-found state for a missing event detail instead of falling back', async () => {
    await renderAt('/events/999')

    expect(await screen.findByText('未找到事件')).toBeTruthy()
    expect(screen.queryByText('EV-20260314-001')).toBeNull()
  })

  it('renders audit list as a figma-aligned audit ledger with template selection entry points', async () => {
    await renderAt('/audits')

    expect((await screen.findAllByRole('heading', { name: '审核列表' })).length).toBeGreaterThan(0)
    expect(screen.getByText('审核中心 / 事件列表')).toBeTruthy()
    expect(screen.getByText('待审核任务')).toBeTruthy()
    expect(screen.getByText('驳回待复核')).toBeTruthy()
    expect(screen.getAllByText('选流程送审').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('审核状态')).toBeTruthy()
    expect(screen.getAllByText('审核节点').length).toBeGreaterThan(0)
    expect(screen.getAllByText('流程模板').length).toBeGreaterThan(0)
    expect(screen.getByText('列表内容')).toBeTruthy()
  })

  it('filters audit list rows by selected audit status', async () => {
    await renderAt('/audits')

    expect((await screen.findAllByText('EV-20260314-001')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('EV-20260314-002').length).toBeGreaterThan(0)

    await fireEvent.update(screen.getByLabelText('审核状态'), 'REJECTED')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).getByText('EV-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('EV-20260314-001')).toBeNull()
  })

  it('renders rebuilt audit detail sections including evidence, submission flow, and node progress', async () => {
    await renderAt('/audits/1')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    expect(screen.getByLabelText('流程模板')).toBeTruthy()
    expect(screen.getByText('证据')).toBeTruthy()
    expect(screen.getByText('现场照片-01.jpg')).toBeTruthy()
    expect(screen.getByText('流程送审')).toBeTruthy()
    expect(screen.getByText('送审与复核说明')).toBeTruthy()
    expect(screen.getByText('节点进度')).toBeTruthy()
    expect(screen.getByText('审核操作')).toBeTruthy()
  })

  it('disables template reselection when current role cannot reselect the audit template', async () => {
    await renderAt('/audits/3')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    const templateSelect = screen.getByLabelText('流程模板') as HTMLSelectElement
    expect(templateSelect.disabled).toBe(true)
    expect(screen.getByText('当前角色仅可查看冻结模板版本。')).toBeTruthy()
  })

  it('keeps editable audit template selection wired to local state', async () => {
    await renderAt('/audits/2')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    const templateSelect = screen.getByLabelText('流程模板') as HTMLSelectElement
    expect(templateSelect.disabled).toBe(false)
    expect(templateSelect.value).toBe('2')

    await fireEvent.update(templateSelect, '1')

    expect(templateSelect.value).toBe('1')
  })

  it('keeps audit node progress tone mapping when showing localized node labels', async () => {
    await renderAt('/audits/3')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    const nodeSection = screen.getByRole('heading', { name: '流程节点轨迹' }).closest('section')
    expect(nodeSection).toBeTruthy()
    const pendingNode = within(nodeSection as HTMLElement).getByText('镇街初审').closest('li')
    expect(pendingNode).toBeTruthy()
    expect(within(pendingNode as HTMLElement).getByText('镇街审核员')).toBeTruthy()
    const pendingTag = within(pendingNode as HTMLElement).getByText('待审核')
    expect(pendingTag.className).toContain('status-tag--warning')
    expect(pendingTag.className).not.toContain('status-tag--default')
  })

  it('keeps audit status consistent between list and detail for the same record', async () => {
    const { router } = await renderAt('/audits')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    const approvedRow = within(tableBody as HTMLElement).getByText('EV-20260314-001')
    const approvedAuditRow = approvedRow.closest('tr')
    expect(approvedAuditRow).toBeTruthy()
    expect(within(approvedAuditRow as HTMLElement).getAllByText('已通过').length).toBeGreaterThan(0)

    await router.push('/audits/1')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    const summarySection = screen.getByRole('heading', { name: '审核摘要' }).closest('section')
    expect(summarySection).toBeTruthy()
    expect(within(summarySection as HTMLElement).getByText('已通过')).toBeTruthy()
  })

  it('keeps shared pending wording generic outside audit surfaces', async () => {
    render(StatusTag, {
      props: { status: 'PENDING' }
    })

    expect(screen.getByText('待处理')).toBeTruthy()
    expect(screen.queryByText('待审核')).toBeNull()
  })

  it('uses consistent audit-specific pending wording between audit list and detail', async () => {
    const { router } = await renderAt('/audits')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    const pendingRow = within(tableBody as HTMLElement).getByText('EV-20260314-003')
    const pendingAuditRow = pendingRow.closest('tr')
    expect(pendingAuditRow).toBeTruthy()
    expect(within(pendingAuditRow as HTMLElement).getAllByText('待审核').length).toBeGreaterThan(0)
    expect(within(pendingAuditRow as HTMLElement).queryByText('待处理')).toBeNull()

    await router.push('/audits/3')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    const summarySection = screen.getByRole('heading', { name: '审核摘要' }).closest('section')
    expect(summarySection).toBeTruthy()
    const auditStatusField = within(summarySection as HTMLElement).getByText('审核状态').closest('div')
    expect(auditStatusField).toBeTruthy()
    expect(within(auditStatusField as HTMLElement).getByText('待审核')).toBeTruthy()
    expect(within(auditStatusField as HTMLElement).queryByText('待处理')).toBeNull()
  })

  it('uses the same rejected status wording within audit detail page', async () => {
    await renderAt('/audits/2')

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    expect(screen.getAllByText('已驳回').length).toBeGreaterThanOrEqual(2)
    expect(screen.queryByText('待处理')).toBeNull()
  })

  it('updates audit detail when navigating to another id on the same component', async () => {
    const { router } = await renderAt('/audits/1')

    expect((await screen.findAllByText('桥沥村疑似违法施工')).length).toBeGreaterThan(0)
    expect(screen.getByText('派单录入')).toBeTruthy()

    await router.push('/audits/2')

    expect((await screen.findAllByText('朗洲村主干道占道经营')).length).toBeGreaterThan(0)
    expect(screen.queryByText('派单录入')).toBeNull()
  })

  it('shows not-found state for a missing audit detail', async () => {
    await renderAt('/audits/999')

    expect(await screen.findByText('未找到审核记录')).toBeTruthy()
    expect(screen.queryByText('桥沥村疑似违法施工')).toBeNull()
  })

  it('renders process template list as a figma-aligned ledger with shared list template sections and edit links', async () => {
    await renderAt('/processes')

    expect((await screen.findAllByRole('heading', { name: '流程配置' })).length).toBeGreaterThan(0)
    expect(screen.getByTestId('web-list-page-template')).toBeTruthy()
    expect(screen.getAllByText('主列表').length).toBeGreaterThan(0)
    expect(screen.getByText('启用模板')).toBeTruthy()
    expect(screen.getByText('停用模板')).toBeTruthy()
    expect(screen.getByText('模板总量')).toBeTruthy()
    expect(screen.getByText('总节点数')).toBeTruthy()
    expect(screen.getByText('查询条件')).toBeTruthy()
    expect(screen.getByLabelText('事件类型')).toBeTruthy()
    expect(screen.getByLabelText('模板状态')).toBeTruthy()
    expect(screen.getByText('流程说明')).toBeTruthy()
    expect(screen.getByText('最近操作')).toBeTruthy()
    expect(screen.getAllByText('编辑模板').length).toBeGreaterThan(0)
  })

  it('filters process template rows by selected template status', async () => {
    await renderAt('/processes')

    expect(await screen.findByText('违法施工标准审核流程')).toBeTruthy()
    expect(screen.getByText('占道经营简化流程')).toBeTruthy()

    await fireEvent.update(screen.getByLabelText('模板状态'), 'DISABLED')

    expect(await screen.findByText('占道经营简化流程')).toBeTruthy()
    expect(screen.queryByText('违法施工标准审核流程')).toBeNull()
  })

  it('renders process template edit metadata and node editing controls', async () => {
    await renderAt('/processes/1/edit')

    expect(await screen.findByRole('heading', { name: '编辑流程模板' })).toBeTruthy()
    expect(screen.getByText('模板版本')).toBeTruthy()
    expect(screen.getByText('流程说明')).toBeTruthy()
    expect(screen.getByText('节点总数')).toBeTruthy()
    expect(screen.getByText('适用事件类型')).toBeTruthy()
    expect(screen.getByDisplayValue('违法施工标准审核流程')).toBeTruthy()
    expect(screen.getByText('适用于违法施工事件的三级审核流程。')).toBeTruthy()
    expect(screen.getByText('v1.2')).toBeTruthy()
    expect(screen.getByLabelText('模板名称')).toBeTruthy()
    expect(screen.getByLabelText('启用状态')).toBeTruthy()
    expect(screen.getByText('节点顺序')).toBeTruthy()
    expect(screen.getAllByLabelText('节点模式').length).toBeGreaterThan(0)
  })

  it('keeps process template edit controls wired to local state across same-component route changes', async () => {
    const { router } = await renderAt('/processes/1/edit')

    const nameInput = (await screen.findByLabelText('模板名称')) as HTMLInputElement
    const eventTypeSelect = screen.getByLabelText('适用事件类型') as HTMLSelectElement
    const statusSelect = screen.getByLabelText('启用状态') as HTMLSelectElement
    const nodeModeSelects = screen.getAllByLabelText('节点模式') as HTMLSelectElement[]

    expect(nameInput.value).toBe('违法施工标准审核流程')
    expect(eventTypeSelect.value).toBe('违法施工')
    expect(statusSelect.value).toBe('ENABLED')
    expect(nodeModeSelects[0]?.value).toBe('MANUAL')

    await fireEvent.update(nameInput, '违法施工标准审核流程-本地编辑')
    await fireEvent.update(eventTypeSelect, '占道经营')
    await fireEvent.update(statusSelect, 'DISABLED')
    await fireEvent.update(nodeModeSelects[0]!, 'AUTO')

    expect(nameInput.value).toBe('违法施工标准审核流程-本地编辑')
    expect(eventTypeSelect.value).toBe('占道经营')
    expect(statusSelect.value).toBe('DISABLED')
    expect(nodeModeSelects[0]?.value).toBe('AUTO')

    expect(screen.getByDisplayValue('违法施工标准审核流程-本地编辑')).toBeTruthy()

    await router.push('/processes/2/edit')

    expect((await screen.findByLabelText('模板名称') as HTMLInputElement).value).toBe('占道经营简化流程')
    expect((screen.getByLabelText('适用事件类型') as HTMLSelectElement).value).toBe('占道经营')
    expect((screen.getByLabelText('启用状态') as HTMLSelectElement).value).toBe('DISABLED')
    expect((screen.getAllByLabelText('节点模式') as HTMLSelectElement[])[0]?.value).toBe('AUTO')
  })

  it('shows not-found state for a missing process template', async () => {
    await renderAt('/processes/999/edit')

    expect(await screen.findByText('未找到流程模板')).toBeTruthy()
    expect(screen.queryByText('节点顺序')).toBeNull()
  })

  it('renders work-order list as a figma-aligned operations queue with summary cards and filters', async () => {
    await renderAt('/work-orders')

    expect((await screen.findAllByRole('heading', { name: '工单列表' })).length).toBeGreaterThan(0)
    expect(screen.getByText('工单中心 / 列表页')).toBeTruthy()
    expect(screen.getByText('查询条件')).toBeTruthy()
    expect(screen.getByText('待派单工单')).toBeTruthy()
    expect(screen.getAllByText('待关单确认').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('工单状态')).toBeTruthy()
    expect(screen.getByLabelText('处理人')).toBeTruthy()
    expect(screen.getByText('工单台账')).toBeTruthy()
    expect(screen.getByText('列表内容')).toBeTruthy()
    expect(screen.getAllByText('来源事件').length).toBeGreaterThan(0)
    expect(screen.getByText('操作')).toBeTruthy()
  })

  it('filters work-order rows by selected state', async () => {
    await renderAt('/work-orders')

    expect((await screen.findAllByText('WO-20260314-001')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('WO-20260314-002').length).toBeGreaterThan(0)

    await fireEvent.update(screen.getByLabelText('工单状态'), 'WAITING_CLOSE_CONFIRM')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).getByText('WO-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('WO-20260314-001')).toBeNull()
  })

  it('filters work-order rows by selected assignee', async () => {
    await renderAt('/work-orders')

    expect((await screen.findAllByText('WO-20260314-001')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('WO-20260314-002').length).toBeGreaterThan(0)

    await fireEvent.update(screen.getByLabelText('处理人'), '朗洲村综合网格员')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).getByText('WO-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('WO-20260314-001')).toBeNull()
  })

  it('renders work-order detail actions by state', async () => {
    const { router } = await renderAt('/work-orders/1')

    expect(await screen.findByRole('heading', { name: '工单详情' })).toBeTruthy()
    expect(screen.getByText('派单状态')).toBeTruthy()
    expect(screen.getByText('等待调度中心分派责任人')).toBeTruthy()
    expect(screen.getByText('流程节点')).toBeTruthy()

    await router.push('/work-orders/2')

    expect(await screen.findByText('关闭确认')).toBeTruthy()
    expect(screen.queryByText('派单状态')).toBeNull()
  })

  it('shows not-found state for a missing work order', async () => {
    await renderAt('/work-orders/999')

    expect(await screen.findByText('未找到工单')).toBeTruthy()
    expect(screen.queryByText('WO-20260314-001')).toBeNull()
  })

  it('hides sidebar menu items when corresponding menu permissions are absent', async () => {
    const limitedSession = createValidSession({
      permissionCodes: ['menu:dashboard:view', 'menu:event:list'],
      menuPermissionCodes: ['menu:dashboard:view', 'menu:event:list']
    })

    await renderAt('/dashboard', limitedSession)

    expect(await screen.findByRole('link', { name: '仪表盘' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '事件' })).toBeTruthy()
    expect(screen.queryByRole('link', { name: '审核' })).toBeNull()
    expect(screen.queryByRole('link', { name: '流程' })).toBeNull()
    expect(screen.queryByRole('link', { name: '工单' })).toBeNull()
    expect(screen.queryByRole('link', { name: '巡查任务' })).toBeNull()
    expect(screen.queryByRole('link', { name: '无人机' })).toBeNull()
    expect(screen.queryByRole('link', { name: '综合监管' })).toBeNull()
    expect(screen.queryByRole('link', { name: '地图总览' })).toBeNull()
    expect(screen.queryByRole('link', { name: '授权管理' })).toBeNull()
    expect(screen.queryByRole('link', { name: 'AI 模型配置' })).toBeNull()
    expect(screen.queryByRole('link', { name: '媒体上传' })).toBeNull()
    expect(screen.queryByRole('link', { name: '系统配置' })).toBeNull()
  })

  it('blocks route navigation when route menu permission is missing', async () => {
    const limitedSession = createValidSession({
      permissionCodes: ['menu:event:list'],
      menuPermissionCodes: ['menu:event:list']
    })

    const { router } = await renderAt('/audits', limitedSession)

    expect(router.currentRoute.value.path).toBe('/events')
    expect((await screen.findAllByRole('heading', { name: '事件列表' })).length).toBeGreaterThan(0)
    expect(screen.queryByRole('heading', { name: '审核列表' })).toBeNull()
  })

  it('shows audit approve and reject buttons from menu permissions without button permissions', async () => {
    const session = createValidSession({
      permissionCodes: createValidSession().permissionCodes.filter(
        (code) => code !== 'button:audit:approve' && code !== 'button:audit:reject'
      )
    })

    await renderAt('/audits/1', session)

    expect(await screen.findByRole('heading', { name: '审核详情' })).toBeTruthy()
    expect(screen.getByText('审核操作')).toBeTruthy()
    expect(screen.getByRole('button', { name: '通过' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '驳回' })).toBeTruthy()
  })

  it('hides work-order confirm-close and return buttons without button permissions', async () => {
    const session = createValidSession({
      permissionCodes: createValidSession().permissionCodes.filter(
        (code) => code !== 'button:workorder:confirm-close' && code !== 'button:workorder:return'
      )
    })

    await renderAt('/work-orders/2', session)

    expect(await screen.findByRole('heading', { name: '工单详情' })).toBeTruthy()
    expect(screen.getByText('关闭确认')).toBeTruthy()
    expect(screen.queryByRole('button', { name: '确认关闭' })).toBeNull()
    expect(screen.queryByRole('button', { name: '退回补充' })).toBeNull()
  })

  it('filters sidebar items from the same menu permission source used by route access', async () => {
    const session = createValidSession({
      permissionCodes: ['menu:dashboard:view', 'menu:workorder:list', 'button:workorder:confirm-close'],
      menuPermissionCodes: ['menu:dashboard:view', 'menu:workorder:list']
    })

    const { router } = await renderAt('/work-orders', session)

    expect(router.currentRoute.value.path).toBe('/work-orders')
    expect((await screen.findAllByRole('heading', { name: '工单列表' })).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: '仪表盘' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '工单中心' })).toBeTruthy()
    expect(screen.queryByRole('link', { name: '综合监管' })).toBeNull()
    expect(screen.queryByRole('link', { name: '地图总览' })).toBeNull()
    expect(screen.queryByRole('link', { name: '事件中心' })).toBeNull()
    expect(screen.queryByRole('link', { name: '业务管理' })).toBeNull()
    expect(screen.queryByRole('link', { name: '流程配置' })).toBeNull()
    expect(screen.queryByRole('link', { name: '授权管理' })).toBeNull()
  })

  it('renders business management pages and respects menu/button permissions', async () => {
    const session = createValidSession()
    const { router } = await renderAt('/areas', session)

    expect(router.currentRoute.value.path).toBe('/areas')
    expect(await screen.findByRole('heading', { name: '片区管理' })).toBeTruthy()
    expect(screen.getByText('业务管理')).toBeTruthy()
    expect(screen.getByRole('link', { name: '片区管理' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '商户管理' })).toBeTruthy()
    expect(screen.getByRole('link', { name: '流动摊贩管理' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增片区' })).toBeTruthy()

    await router.push('/merchants')
    expect(await screen.findByRole('heading', { name: '商户管理' })).toBeTruthy()
    expect(screen.getByLabelText('所属片区筛选')).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增商户' })).toBeTruthy()

    await router.push('/mobile-vendors')
    expect(await screen.findByRole('heading', { name: '流动摊贩管理' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增摊贩' })).toBeTruthy()
  })

  it('shows business buttons from menu permissions when corresponding button permissions are absent', async () => {
    const session = createValidSession({
      permissionCodes: createValidSession().permissionCodes.filter(
        (code) => !code.startsWith('button:biz:area:') && !code.startsWith('button:biz:merchant:') && !code.startsWith('button:biz:vendor:')
      )
    })

    const { router } = await renderAt('/areas', session)
    expect(await screen.findByRole('heading', { name: '片区管理' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增片区' })).toBeTruthy()

    await router.push('/merchants')
    expect(await screen.findByRole('heading', { name: '商户管理' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增商户' })).toBeTruthy()

    await router.push('/mobile-vendors')
    expect(await screen.findByRole('heading', { name: '流动摊贩管理' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '新增摊贩' })).toBeTruthy()
  })
})
