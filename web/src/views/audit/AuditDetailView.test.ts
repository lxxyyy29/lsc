import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { clearWebSession, persistWebSession } from '../../auth/session'
import AuditDetailView from './AuditDetailView.vue'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/audits/:id',
        component: AuditDetailView
      }
    ]
  })
}

async function renderAt(path: string, permissionCodes: string[] = ['menu:audit:list']) {
  clearWebSession()

  if (permissionCodes.length > 0) {
    persistWebSession({
      token: 'test-token',
      userId: 1,
      userName: '测试用户',
      account: 'tester',
      roleCodes: ['admin'],
      permissionCodes,
      menuPermissionCodes: ['menu:audit:list']
    })
  }

  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(AuditDetailView, {
    global: {
      plugins: [router]
    }
  })

  return { router }
}

afterEach(() => {
  cleanup()
  clearWebSession()
})

describe('AuditDetailView', () => {
  it('renders the audit detail content inside the canonical evidence and flow layout', async () => {
    await renderAt('/audits/1')

    expect(screen.getByRole('heading', { name: '审核详情' })).toBeTruthy()
    expect(screen.getByTestId('web-detail-page-template')).toBeTruthy()
    expect(screen.getByText('审核中心 / 详情页')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '桥沥村疑似违法施工' })).toBeTruthy()
    expect(screen.getAllByText('已通过').length).toBeGreaterThan(0)
    expect(screen.getByRole('heading', { name: '审核摘要' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '证据' })).toBeTruthy()
    expect(screen.getByText('现场照片-01.jpg')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '流程模板配置' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '流程送审' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '送审与复核说明' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '流程节点轨迹' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '审核操作' })).toBeTruthy()
    expect(screen.getByText('审核结论处理')).toBeTruthy()
    expect(screen.getAllByText('等待派单').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: '通过' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '驳回' })).toBeTruthy()
  })

  it('keeps editable template selection wired to local state in the rebuilt layout', async () => {
    await renderAt('/audits/2')

    const templateSelect = screen.getByLabelText('流程模板') as HTMLSelectElement
    expect(templateSelect.disabled).toBe(false)
    expect(templateSelect.value).toBe('2')

    await fireEvent.update(templateSelect, '1')

    expect(templateSelect.value).toBe('1')
    expect(screen.getByText('当前角色具备重选模板权限，可在提交前重新选择。')).toBeTruthy()
  })

  it('preserves audit-specific pending wording in the node trajectory section', async () => {
    await renderAt('/audits/3')

    const nodeSection = screen.getByRole('heading', { name: '流程节点轨迹' }).closest('section')
    expect(nodeSection).toBeTruthy()
    expect(within(nodeSection as HTMLElement).getByText('镇街初审')).toBeTruthy()
    const pendingTag = within(nodeSection as HTMLElement).getByText('待审核')
    expect(pendingTag.className).toContain('status-tag--warning')
    expect(within(nodeSection as HTMLElement).queryByText('待处理')).toBeNull()
  })

  it('updates the rebuilt detail content when navigating to another audit id on the same component', async () => {
    const { router } = await renderAt('/audits/1')

    expect(screen.getByRole('heading', { name: '桥沥村疑似违法施工' })).toBeTruthy()
    expect(screen.getAllByText('等待派单').length).toBeGreaterThan(0)
    expect(screen.getByText('派单录入')).toBeTruthy()

    await router.push('/audits/2')

    expect(await screen.findByRole('heading', { name: '朗洲村主干道占道经营' })).toBeTruthy()
    expect(screen.getAllByText('已驳回').length).toBeGreaterThan(0)
    expect(screen.queryByText('等待派单')).toBeNull()
    expect(screen.queryByText('派单录入')).toBeNull()
  })

  it('keeps the existing not-found state for unknown audit records', async () => {
    await renderAt('/audits/999')

    expect(screen.getByText('未找到审核记录')).toBeTruthy()
    expect(screen.queryByTestId('web-detail-page-template')).toBeNull()
  })
})
