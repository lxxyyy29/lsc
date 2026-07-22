import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import WorkOrderListView from './WorkOrderListView.vue'

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/work-orders',
        component: WorkOrderListView
      },
      {
        path: '/work-orders/:id',
        component: {
          template: '<div>detail</div>'
        }
      }
    ]
  })
}

async function renderAt(path = '/work-orders') {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(WorkOrderListView, {
    global: {
      plugins: [router]
    }
  })

  return { router }
}

afterEach(() => {
  cleanup()
})

describe('WorkOrderListView', () => {
  it('renders the rebuilt work-order center list structure while keeping existing queue content', async () => {
    await renderAt()

    expect(screen.getByRole('heading', { name: '工单列表' })).toBeTruthy()
    expect(screen.getByText('工单中心 / 列表页')).toBeTruthy()
    expect(screen.getByText('查询条件')).toBeTruthy()
    expect(screen.getByText('工单台账')).toBeTruthy()
    expect(screen.getByText('列表内容')).toBeTruthy()
    expect(screen.getAllByText('工单编号 / 项目名称').length).toBeGreaterThan(0)
    expect(screen.getByText('待派单工单')).toBeTruthy()
    expect(screen.getAllByText('待关单确认').length).toBeGreaterThan(0)
    expect(screen.getByLabelText('工单状态')).toBeTruthy()
    expect(screen.getByLabelText('处理人')).toBeTruthy()
    expect(screen.getAllByRole('link', { name: '查看详情' }).length).toBeGreaterThan(0)
    expect(screen.getAllByText('WO-20260314-001').length).toBeGreaterThan(0)
    expect(screen.getAllByText('桥沥村疑似违法施工').length).toBeGreaterThan(0)
  })

  it('keeps state filtering behavior on the rebuilt list', async () => {
    await renderAt()

    expect(screen.getAllByText('WO-20260314-001').length).toBeGreaterThan(0)
    expect(screen.getAllByText('WO-20260314-002').length).toBeGreaterThan(0)

    await fireEvent.update(screen.getByLabelText('工单状态'), 'WAITING_CLOSE_CONFIRM')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('WO-20260314-001')).toBeNull()
    expect(within(tableBody as HTMLElement).getByText('WO-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('待派发')).toBeNull()
  })

  it('keeps assignee filtering behavior on the rebuilt list', async () => {
    await renderAt()

    await fireEvent.update(screen.getByLabelText('处理人'), '朗洲村综合网格员')

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    expect(within(tableBody as HTMLElement).queryByText('WO-20260314-001')).toBeNull()
    expect(within(tableBody as HTMLElement).getByText('WO-20260314-002')).toBeTruthy()
    expect(within(tableBody as HTMLElement).getByText('朗洲村综合网格员')).toBeTruthy()
  })

  it('keeps detail links pointed at the existing work-order route', async () => {
    await renderAt()

    const tableBody = screen.getByRole('table').querySelector('tbody')
    expect(tableBody).toBeTruthy()
    const row = within(tableBody as HTMLElement).getByText('WO-20260314-002').closest('tr')
    expect(row).toBeTruthy()
    const detailLink = within(row as HTMLElement).getByRole('link', { name: '查看详情' })

    expect(detailLink.getAttribute('href')).toBe('/work-orders/2')
  })
})
