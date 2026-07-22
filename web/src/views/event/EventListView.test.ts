import { cleanup, fireEvent, render, screen, within } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import EventListView from './EventListView.vue'

const dispatchEventDirectly = vi.fn(async (_eventId: number, payload: { processTemplateId: number; remark?: string }) => ({
  eventId: 1,
  currentStatus: 'PROCESSING',
  processTemplateId: payload.processTemplateId,
  processTemplateName: '违法施工标准审核流程 v1.2',
  currentNodeName: '处置派发',
  currentNodeStatus: 'DISPATCHED'
}))

vi.mock('../../api/event', () => ({
  listEvents: vi.fn(async () => [
    {
      id: 1,
      eventCode: 'EV-20260314-001',
      sourceType: '无人机识别',
      eventType: '违法施工',
      currentStatus: 'WAITING_DISPATCH',
      occurredAt: '2026-03-14 09:30',
      area: '常平镇桥沥村',
      title: '桥沥村疑似违法施工',
      processTemplateId: 1,
      processTemplateName: '违法施工标准审核流程 v1.2',
      currentNodeName: '处置派发',
      currentNodeStatus: 'DISPATCHED',
      dispatchable: true
    },
    {
      id: 2,
      eventCode: 'EV-20260314-002',
      sourceType: '视频AI识别',
      eventType: '占道经营',
      currentStatus: 'PROCESSING',
      occurredAt: '2026-03-14 11:20',
      area: '常平镇朗洲村',
      title: '朗洲村主干道占道经营',
      processTemplateId: 2,
      processTemplateName: '占道经营简化流程 v2.0',
      currentNodeName: '现场处置',
      currentNodeStatus: 'ACTIVE',
      dispatchable: false
    },
    {
      id: 3,
      eventCode: 'EV-20260314-003',
      sourceType: '网格员上报',
      eventType: '违规堆放',
      currentStatus: 'PENDING_AUDIT',
      occurredAt: '2026-03-14 14:10',
      area: '常平镇苏坑村',
      title: '苏坑村空地建筑垃圾堆放',
      currentNodeName: '流程配置',
      currentNodeStatus: 'PENDING',
      dispatchable: true
    }
  ]),
  listAvailableProcessTemplates: vi.fn(async () => [
    {
      id: 1,
      name: '违法施工标准审核流程',
      version: 'v1.2',
      eventType: '违法施工',
      description: '适用于违法施工事件的三级处置流程。',
      nodeCount: 3,
      nodes: [
        { id: 101, orderNo: 1, name: '镇街研判', roleName: '镇街值守员', mode: 'MANUAL' },
        { id: 102, orderNo: 2, name: '部门复核', roleName: '业务部门', mode: 'JOINT' },
        { id: 103, orderNo: 3, name: '现场处置', roleName: '执法中队', mode: 'MANUAL' }
      ]
    }
  ]),
  dispatchEventDirectly,
  getEventStatusLabel: vi.fn((status: string) =>
    ({
      PENDING_AUDIT: '待配置流程',
      WAITING_DISPATCH: '未派单',
      PROCESSING: '已派单'
    })[status] ?? status
  ),
  getWorkflowNodeStatusLabel: vi.fn((status: string) =>
    ({
      PENDING: '待开始',
      ACTIVE: '进行中',
      DISPATCHED: '已派发'
    })[status] ?? status
  )
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/events',
        component: EventListView
      },
      {
        path: '/events/:id',
        component: {
          template: '<div>detail</div>'
        }
      }
    ]
  })
}

async function renderAt(path = '/events') {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(EventListView, {
    global: {
      plugins: [router]
    }
  })

  await screen.findByText('查询条件')
  await screen.findByText('共3条记录')
  return { router }
}

afterEach(() => {
  cleanup()
  dispatchEventDirectly.mockClear()
})

describe('EventListView', () => {
  it('renders filters, cards and workflow information with async data', async () => {
    await renderAt('/events')

    const cardGrid = screen.getByLabelText('事件卡片列表')
    expect(within(cardGrid).getAllByTestId('event-card')).toHaveLength(3)
    expect(within(cardGrid).getByText('流程状态')).toBeTruthy()
    expect(within(cardGrid).getByText('当前节点')).toBeTruthy()
    expect(within(cardGrid).getByText('处置流程')).toBeTruthy()
    expect(screen.getAllByRole('link', { name: '查看详情' })[0]?.getAttribute('href')).toContain('/events/1')
  })

  it('filters by keyword and resets back to the full card list', async () => {
    await renderAt('/events')

    const keywordInput = screen.getByLabelText('告警名称')
    const searchButton = screen.getByRole('button', { name: '查询' })
    const resetButton = screen.getByRole('button', { name: '重置' })
    const cardGrid = screen.getByLabelText('事件卡片列表')

    await fireEvent.update(keywordInput, '桥沥')
    await fireEvent.click(searchButton)

    expect(within(cardGrid).getAllByTestId('event-card')).toHaveLength(1)
    await fireEvent.click(resetButton)
    expect(within(cardGrid).getAllByTestId('event-card')).toHaveLength(3)
  })

  it('opens the process dialog and dispatches without assignee fields', async () => {
    await renderAt('/events')

    await fireEvent.click(screen.getAllByRole('button', { name: '派单' })[0]!)
    await screen.findByRole('dialog', { name: '选择处置流程' })

    expect(screen.getByText('预设节点承接')).toBeTruthy()
    expect(screen.queryByLabelText('承接队伍')).toBeNull()
    expect(screen.queryByLabelText('承接人')).toBeNull()

    await fireEvent.update(screen.getByLabelText('补充说明'), '现场核查并同步结果')
    await fireEvent.click(screen.getByRole('button', { name: '确认派单' }))

    expect(dispatchEventDirectly).toHaveBeenCalledWith(1, {
      processTemplateId: 1,
      remark: '现场核查并同步结果'
    })

    await screen.findByText('已派单')
    expect(screen.getByText('已派发')).toBeTruthy()
    expect(screen.getByText('镇街值守员')).toBeTruthy()
  })
})
