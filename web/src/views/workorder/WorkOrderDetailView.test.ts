import { cleanup, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { clearWebSession, persistWebSession } from '../../auth/session'
import WorkOrderDetailView from './WorkOrderDetailView.vue'

const getWorkOrderDetail = vi.fn()

vi.mock('../../api/workorder', async () => {
  const actual = await vi.importActual<typeof import('../../api/workorder')>('../../api/workorder')
  return {
    ...actual,
    getWorkOrderDetail: (...args: unknown[]) => getWorkOrderDetail(...args),
    handleWorkOrder: vi.fn()
  }
})

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/work-orders/:id',
        component: WorkOrderDetailView
      }
    ]
  })
}

async function renderAt(path: string, permissionCodes: string[] = []) {
  clearWebSession()

  if (permissionCodes.length > 0) {
    persistWebSession({
      token: 'test-token',
      userId: 1,
      userName: '测试用户',
      account: 'tester',
      roleCodes: ['admin'],
      permissionCodes,
      menuPermissionCodes: ['menu:workorder:list']
    })
  }

  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(WorkOrderDetailView, {
    global: {
      plugins: [router]
    }
  })

  return router
}

afterEach(() => {
  cleanup()
  clearWebSession()
})

beforeEach(() => {
  getWorkOrderDetail.mockReset()
  getWorkOrderDetail.mockImplementation(async (id: number) => {
    if (id === 2) {
      return {
        id: 2,
        code: 'WO-20260314-002',
        eventCode: 'EVT-20260314-002',
        eventTitle: '朗洲村主干道占道经营',
        assignee: '测试用户',
        state: 'PROCESSING',
        sourceEventState: 'DISPATCHED_TO_WORK_ORDER',
        sourceEventSummary: '现场已处置完成，等待管理员关闭确认。',
        flowRecords: [
          {
            id: '1',
            title: '工单已派发',
            description: '管理员已派发工单。',
            operator: 'Web管理员',
            timestamp: '2026-03-14 12:20',
            status: 'PROCESSING'
          },
          {
            id: '2',
            title: '现场处置完成',
            description: '现场已处置完成，等待管理员关闭确认。',
            operator: '朗洲村综合网格员',
            timestamp: '2026-03-14 12:40',
            status: 'COMPLETED'
          }
        ],
        dispatcherName: 'Web管理员',
        closeReason: '',
        processInstanceId: 5,
        createdAt: '2026-03-14 12:00',
        updatedAt: '2026-03-14 12:40',
        completedAt: '',
        closedAt: ''
      }
    }

    return {
      id: 1,
      code: 'WO-20260314-001',
      eventCode: 'EVT-20260314-001',
      eventTitle: '测试工单',
      assignee: '待分派',
      state: 'WAITING_DISPATCH',
      sourceEventState: 'WAITING_DISPATCH',
      sourceEventSummary: '待派单，尚未提交现场反馈',
      flowRecords: [],
      dispatcherName: '系统',
      closeReason: '',
      processInstanceId: undefined,
      createdAt: '2026-03-14 10:00',
      updatedAt: '2026-03-14 10:00',
      completedAt: '',
      closedAt: ''
    }
  })
})

describe('WorkOrderDetailView', () => {
  it('renders real flow records returned by the detail API', async () => {
    await renderAt('/work-orders/2')

    await waitFor(() => {
      expect(getWorkOrderDetail).toHaveBeenCalledWith(2)
    })

    expect(screen.getByTestId('web-detail-page-template')).toBeTruthy()
    expect(screen.getByText('工单中心 / 查看')).toBeTruthy()
    expect(screen.getByRole('heading', { name: '工单信息' })).toBeTruthy()
    expect(screen.getByText('WO-20260314-002')).toBeTruthy()
    expect(screen.getByText('朗洲村主干道占道经营')).toBeTruthy()
    expect(screen.getAllByText('现场已处置完成，等待管理员关闭确认。').length).toBeGreaterThan(0)
    expect(screen.getByText('工单已派发')).toBeTruthy()
    expect(screen.getByText('现场处置完成')).toBeTruthy()
    expect(screen.getByText('Web管理员 · 2026-03-14 12:20')).toBeTruthy()
    expect(screen.getByText('朗洲村综合网格员 · 2026-03-14 12:40')).toBeTruthy()
  })

  it('shows empty timeline state when no flow records are returned', async () => {
    await renderAt('/work-orders/1')

    await waitFor(() => {
      expect(getWorkOrderDetail).toHaveBeenCalledWith(1)
    })

    expect(screen.getByText('测试工单')).toBeTruthy()
    expect(screen.getByText('待派单，尚未提交现场反馈')).toBeTruthy()
    expect(screen.getByText('暂无流转记录。')).toBeTruthy()
    expect(screen.queryByRole('textbox')).toBeNull()
    expect(screen.queryByRole('button', { name: '确认处置' })).toBeNull()
  })

  it('shows handle action when current user matches assignee and has handle permission', async () => {
    await renderAt('/work-orders/2', ['api:workorder:handle'])

    await waitFor(() => {
      expect(getWorkOrderDetail).toHaveBeenCalledWith(2)
    })

    expect(screen.getByRole('button', { name: '确认处置' })).toBeTruthy()
    expect(screen.getByLabelText('处置结果')).toBeTruthy()
    expect(screen.getByLabelText('处置备注')).toBeTruthy()
  })
})
