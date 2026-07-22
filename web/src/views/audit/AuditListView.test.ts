import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import AuditListView from './AuditListView.vue'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/audits',
        component: AuditListView
      },
      {
        path: '/audits/:id',
        component: {
          template: '<div>audit detail placeholder</div>'
        }
      }
    ]
  })
}

async function renderAt(path = '/audits') {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(AuditListView, {
    global: {
      plugins: [router]
    }
  })

  return { router }
}

afterEach(() => {
  cleanup()
})

describe('AuditListView', () => {
  it('renders the rebuilt audit list inside the canonical card-plus-ledger layout', async () => {
    await renderAt()

    expect(screen.getByRole('heading', { name: '审核列表' })).toBeTruthy()
    expect(screen.getByTestId('web-list-page-template')).toBeTruthy()
    expect(screen.getByText('审核中心 / 事件列表')).toBeTruthy()
    expect(screen.getByText('查询条件')).toBeTruthy()
    expect(screen.getAllByText('选流程送审').length).toBeGreaterThan(0)
    expect(screen.getByText('待审核任务')).toBeTruthy()
    expect(screen.getByText('驳回待复核')).toBeTruthy()
    expect(screen.getByLabelText('审核状态')).toBeTruthy()
    expect(screen.getAllByText('流程模板').length).toBeGreaterThan(0)
    expect(screen.getAllByText('审核节点').length).toBeGreaterThan(0)
    expect(screen.getByText('列表内容')).toBeTruthy()
  })

  it('filters audit rows by selected status in the rebuilt list layout', async () => {
    await renderAt()

    expect((await screen.findAllByText('EV-20260314-001')).length).toBeGreaterThan(0)
    expect(screen.getAllByText('EV-20260314-002').length).toBeGreaterThan(0)

    await fireEvent.update(screen.getByLabelText('审核状态'), 'REJECTED')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).getByText('EV-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('EV-20260314-001')).toBeNull()
  })

  it('keeps audit-specific pending wording and send-for-review action together on pending rows', async () => {
    await renderAt()

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    const pendingCode = within(tableBody as HTMLElement).getByText('EV-20260314-003')
    const pendingRow = pendingCode.closest('tr')
    expect(pendingRow).toBeTruthy()

    expect(within(pendingRow as HTMLElement).getAllByText('待审核').length).toBeGreaterThan(0)
    expect(within(pendingRow as HTMLElement).queryByText('待处理')).toBeNull()
    const sendForReviewLink = within(pendingRow as HTMLElement).getByRole('link', { name: '选流程送审' }) as HTMLAnchorElement
    expect(sendForReviewLink.getAttribute('href')).toBe('/audits/3')
  })
})
