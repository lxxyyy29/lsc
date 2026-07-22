import { http } from './http'
import type { EventStatus } from './event'

export type WorkOrderState = 'WAITING_DISPATCH' | 'DISPATCHED' | 'PROCESSING' | 'WAITING_CLOSE_CONFIRM' | 'CLOSED' | 'COMPLETED'

export interface WorkOrderFlowRecord {
  id: string
  title: string
  description: string
  operator: string
  timestamp: string
  status: WorkOrderState
  subjectType?: string | null
  subjectId?: number | null
  subjectName?: string | null
  attachments?: Array<{ fileName: string; fileUrl: string; fileType?: string; mimeType?: string }>
}

export interface WorkOrderListItem {
  id: number
  code: string
  eventCode: string
  eventTitle: string
  assignee: string
  currentHandler: string
  dispatcherName: string
  dispatchTime: string
  state: WorkOrderState
  districtName: string
}

export interface WorkOrderDetail extends WorkOrderListItem {
  sourceEventId?: number
  sourceEventState: EventStatus
  sourceEventSummary: string
  flowRecords: WorkOrderFlowRecord[]
  dispatcherName: string
  closeReason: string
  processInstanceId?: number
  createdAt?: string
  updatedAt?: string
  completedAt?: string
  closedAt?: string
}

export interface WorkOrderAttachmentPayload {
  fileName: string
  fileUrl: string
  fileType?: string
  mimeType?: string
}

export interface HandleWorkOrderPayload {
  result: 'APPROVED' | 'REJECTED' | 'RETURNED'
  remark?: string
  attachments?: WorkOrderAttachmentPayload[]
  subjectType?: 'MERCHANT' | 'VENDOR' | null
  subjectId?: number | null
}

interface BackendWorkOrderSummary {
  id?: number | null
  workOrderNo?: string | null
  sourceEventId?: number | null
  eventCode?: string | null
  eventTitle?: string | null
  status?: string | null
  assigneeName?: string | null
  dispatcherName?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  areaId?: number | null
  areaName?: string | null
}

interface BackendWorkOrderFlowRecord {
  id?: number | null
  action?: string | null
  status?: string | null
  remark?: string | null
  operatorName?: string | null
  nodeName?: string | null
  occurredAt?: string | null
  subjectType?: string | null
  subjectId?: number | null
  subjectName?: string | null
  attachments?: Array<{ fileName?: string; fileUrl?: string; fileType?: string; mimeType?: string }> | null
}

interface BackendWorkOrderDetail extends BackendWorkOrderSummary {
  processInstanceId?: number | null
  eventType?: string | null
  sourceType?: string | null
  eventStatus?: string | null
  description?: string | null
  closeReason?: string | null
  completedAt?: string | null
  closedAt?: string | null
  flowRecords?: BackendWorkOrderFlowRecord[] | null
}

function mapState(status?: string | null): WorkOrderState {
  switch (status) {
    case 'COMPLETED':
      return 'COMPLETED'
    case 'CLOSED':
      return 'CLOSED'
    case 'PROCESSING':
    default:
      return 'PROCESSING'
  }
}

function toDisplayTime(value?: string | null) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 16)
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function mapSummary(item: BackendWorkOrderSummary): WorkOrderListItem {
  return {
    id: Number(item.id ?? 0),
    code: item.workOrderNo || `WO-${item.id ?? '--'}`,
    eventCode: item.eventCode || `EVENT-${item.sourceEventId ?? '--'}`,
    eventTitle: item.eventTitle || '未命名事件',
    assignee: item.assigneeName || '未指派',
    currentHandler: item.assigneeName || '未指派',
    dispatcherName: item.dispatcherName || '系统',
    dispatchTime: toDisplayTime(item.createdAt),
    state: mapState(item.status),
    districtName: item.areaName || '无'
  }
}

function mapFlowRecordTitle(action?: string | null, nodeName?: string | null) {
  switch (action) {
    case 'WORK_ORDER_START':
      return '工单流程已启动'
    case 'WORK_ORDER_DISPATCH':
      return '工单已派发'
    case 'WORK_ORDER_HANDLE':
      return nodeName ? `${nodeName}已处理` : '处理节点已完成'
    case 'WORK_ORDER_COMPLETE':
      return '工单已完成'
    default:
      return nodeName || '工单流转更新'
  }
}

function mapFlowRecordStatus(status?: string | null): WorkOrderState {
  switch (status) {
    case 'APPROVED':
      return 'COMPLETED'
    case 'REJECTED':
      return 'CLOSED'
    case 'RUNNING':
    default:
      return 'PROCESSING'
  }
}

function buildFlowRecords(item: BackendWorkOrderDetail): WorkOrderFlowRecord[] {
  return (item.flowRecords || [])
    .map((record) => ({
      id: String(record.id ?? `${record.action ?? 'flow'}-${record.occurredAt ?? Math.random()}`),
      title: mapFlowRecordTitle(record.action, record.nodeName),
      description: record.remark || item.description || '工单已进入下一步处理。',
      operator: record.operatorName || '系统',
      timestamp: toDisplayTime(record.occurredAt),
      status: mapFlowRecordStatus(record.status),
      subjectType: record.subjectType ?? null,
      subjectId: record.subjectId ?? null,
      subjectName: record.subjectName ?? null,
      attachments: (typeof record.attachments === 'string' ? JSON.parse(record.attachments) : (record.attachments || [])).map((a: any) => ({
        fileName: a.fileName || '',
        fileUrl: a.fileUrl || '',
        fileType: a.fileType ?? undefined,
        mimeType: a.mimeType ?? undefined
      }))
    }))
    .filter((record) => record.timestamp)
}

export interface PagedWorkOrders {
  items: WorkOrderListItem[]
  total: number
  page: number
  pageSize: number
}

interface BackendPagedWorkOrders {
  items?: BackendWorkOrderSummary[] | null
  total?: number | null
  page?: number | null
  pageSize?: number | null
}

export async function listWorkOrders(): Promise<WorkOrderListItem[]> {
  const data = await http.get<BackendWorkOrderSummary[], BackendWorkOrderSummary[]>('/work-orders/export')
  return (data || []).map(mapSummary)
}

export async function listWorkOrdersPaged(page = 1, pageSize = 10, status?: string, assignee?: string, areaId?: number): Promise<PagedWorkOrders> {
  const params: Record<string, unknown> = { page, pageSize }
  if (status) params.status = status
  if (assignee) params.assignee = assignee
  if (areaId) params.areaId = areaId
  const data = await http.get<BackendPagedWorkOrders, BackendPagedWorkOrders>('/work-orders', { params })
  return {
    items: (data?.items || []).map(mapSummary),
    total: Number(data?.total ?? 0),
    page: Number(data?.page ?? page),
    pageSize: Number(data?.pageSize ?? pageSize)
  }
}

export async function getWorkOrderDetail(id: number): Promise<WorkOrderDetail | undefined> {
  if (!Number.isFinite(id)) {
    return undefined
  }

  try {
    const data = await http.get<BackendWorkOrderDetail, BackendWorkOrderDetail>(`/work-orders/${id}`)
    const summary = mapSummary(data)
    return {
      ...summary,
      sourceEventId: data.sourceEventId ?? undefined,
      sourceEventState: ((data.eventStatus as EventStatus | undefined) ?? 'WAITING_DISPATCH'),
      sourceEventSummary: data.description || '暂无事件摘要。',
      flowRecords: buildFlowRecords(data),
      dispatcherName: data.dispatcherName || '系统',
      closeReason: data.closeReason || '',
      processInstanceId: data.processInstanceId ?? undefined,
      createdAt: toDisplayTime(data.createdAt),
      updatedAt: toDisplayTime(data.updatedAt),
      completedAt: toDisplayTime(data.completedAt),
      closedAt: toDisplayTime(data.closedAt)
    }
  } catch {
    return undefined
  }
}

export async function handleWorkOrder(id: number, payload: HandleWorkOrderPayload): Promise<void> {
  await http.post(`/work-orders/${id}/handle`, {
    result: payload.result,
    remark: payload.remark?.trim() || undefined,
    attachments: payload.attachments?.map((item) => ({
      fileName: item.fileName,
      fileUrl: item.fileUrl,
      fileType: item.fileType,
      mimeType: item.mimeType
    })) || [],
    subjectType: payload.subjectType ?? null,
    subjectId: payload.subjectId ?? null
  })
}
