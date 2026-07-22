import { cleanup, fireEvent, render, screen } from '@testing-library/vue'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import EventDetailView from './EventDetailView.vue'

const dispatchEventDirectly = vi.fn(async (_eventId: number, payload: { processTemplateId: number; remark?: string }) => ({
  eventId: 1,
  currentStatus: 'PROCESSING',
  processTemplateId: payload.processTemplateId,
  processTemplateName: '违法施工标准审核流程 v1.2',
  currentNodeName: '处置派发',
  currentNodeStatus: 'DISPATCHED'
}))

vi.mock('../../api/event', () => ({
  getEventDetail: vi.fn(async (id: number) => {
    if (id === 1) {
      return {
        id: 1,
        eventCode: 'EV-20260314-001',
        sourceType: '无人机识别',
        eventType: '违法施工',
        currentStatus: 'WAITING_DISPATCH',
        occurredAt: '2026-03-14 09:30',
        area: '常平镇桥沥村',
        title: '桥沥村疑似违法施工',
        sourceSystem: '低空巡查接入平台',
        description: '无人机巡查发现桥沥村河道旁新增施工设备，需进一步核验。',
        location: '东莞市常平镇桥沥村河道北侧',
        longitude: 113.9982,
        latitude: 22.9811,
        evidenceReferences: ['现场照片-01.jpg', '现场视频-01.mp4'],
        lifecycleRecords: [
          {
            id: 'record-1',
            title: '流程已启动',
            description: '已进入处置准备阶段。',
            operator: '系统',
            timestamp: '2026-03-14 09:40',
            status: 'ACTIVE'
          }
        ],
        processTemplateId: 1,
        processTemplateName: '违法施工标准审核流程 v1.2',
        currentNodeName: '处置派发',
        currentNodeStatus: 'ACTIVE',
        dispatchable: true
      }
    }

    if (id === 2) {
      return {
        id: 2,
        eventCode: 'EV-20260314-002',
        sourceType: '视频AI识别',
        eventType: '占道经营',
        currentStatus: 'PROCESSING',
        occurredAt: '2026-03-14 11:20',
        area: '常平镇朗洲村',
        title: '朗洲村主干道占道经营',
        sourceSystem: '视频算法平台',
        description: '固定摄像头连续识别到摊位占道，已派单处理中。',
        location: '常平镇朗洲村主干道东段',
        longitude: 114.0112,
        latitude: 22.9793,
        evidenceReferences: ['抓拍图-02.jpg'],
        lifecycleRecords: [],
        processTemplateId: 2,
        processTemplateName: '占道经营简化流程 v2.0',
        currentNodeName: '现场处置',
        currentNodeStatus: 'ACTIVE',
        dispatchable: false
      }
    }

    return undefined
  }),
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
      WAITING_DISPATCH: '未派单',
      PROCESSING: '已派单'
    })[status] ?? status
  ),
  getWorkflowNodeStatusLabel: vi.fn((status: string) =>
    ({
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
        path: '/events/:id',
        component: EventDetailView
      }
    ]
  })
}

async function renderAt(path = '/events/1') {
  const router = createTestRouter()
  await router.push(path)
  await router.isReady()

  render(EventDetailView, {
    global: {
      plugins: [router]
    }
  })

  return { router }
}

afterEach(() => {
  cleanup()
  dispatchEventDirectly.mockClear()
})

describe('EventDetailView', () => {
  it('renders event detail content with workflow progress', async () => {
    await renderAt('/events/1')

    await screen.findByText('桥沥村疑似违法施工')
    expect(screen.getByText('流程与处置信息')).toBeTruthy()
    expect(screen.getByText('节点进度')).toBeTruthy()
    expect(screen.getByText('流程已启动')).toBeTruthy()
  })

  it('updates displayed event content when the route id changes on the same component', async () => {
    const { router } = await renderAt('/events/1')

    await screen.findByText('桥沥村疑似违法施工')
    await router.push('/events/2')
    await screen.findByText('朗洲村主干道占道经营')
    expect(screen.queryByText('桥沥村疑似违法施工')).toBeNull()
  })

  it('opens process dialog and updates workflow state after dispatch', async () => {
    await renderAt('/events/1')

    await screen.findByText('桥沥村疑似违法施工')
    await fireEvent.click(screen.getByRole('button', { name: '派单' }))

    await screen.findByRole('dialog', { name: '选择处置流程' })
    expect(screen.getByText('预设节点承接')).toBeTruthy()
    expect(screen.queryByLabelText('承接队伍')).toBeNull()
    expect(screen.queryByLabelText('承接人')).toBeNull()

    await fireEvent.update(screen.getByLabelText('补充说明'), '立即到场核查')
    await fireEvent.click(screen.getByRole('button', { name: '确认派单' }))

    expect(dispatchEventDirectly).toHaveBeenCalledWith(1, {
      processTemplateId: 1,
      remark: '立即到场核查'
    })

    await screen.findByText('已派单')
    expect(screen.getByText('已派发')).toBeTruthy()
    expect(screen.getByText('镇街值守员')).toBeTruthy()
  })

  it('keeps the existing not-found state for unknown events', async () => {
    await renderAt('/events/999')

    await screen.findByText('未发现')
    expect(screen.queryByText('桥沥村疑似违法施工')).toBeNull()
  })
})
