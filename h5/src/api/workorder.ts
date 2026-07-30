import { http } from './http'

// ─── Backend VO types ───────────────────────────────────────────────────────

export interface ProcessNodeVo {
  nodeOrder: number
  nodeName: string
  assigneeUserId: number
  assigneeName: string
  status: string // PENDING | APPROVED | REJECTED | WAITING
}

export interface ActionRecordVo {
  action: string
  result: string
  remark: string
  attachments: Array<{ fileName: string; fileUrl: string; fileType: string }> | string | null
  operatedAt: string
  operatorName: string
  nodeOrder: number
  subjectType?: 'MERCHANT' | 'VENDOR' | null
  subjectId?: number | null
  subjectName?: string | null
}

interface BackendWorkOrder {
  id: number
  workOrderNo: string
  status: string // PROCESSING | WAITING_CLOSE_CONFIRM | WAITING_VERIFY | COMPLETED | CLOSED | TIMEOUT
  assigneeUserId: number
  assigneeName: string
  dispatcherName: string
  sourceEventId: number
  processInstanceId: number
  // Event info
  eventTitle: string
  eventLocation: string
  eventDescription: string
  merchantName: string | null
  eventType: string
  // Current user context
  isCurrentHandler: boolean
  currentNodeName: string | null
  areaName: string | null
  urgencyLevel: string | null
  // Nested data
  processNodes: ProcessNodeVo[]
  actionRecords: ActionRecordVo[]
  // Timestamps
  createdAt: string
  updatedAt: string
  completedAt: string | null
  closedAt: string | null
}

interface BackendWorkbenchSummary {
  totalCount: number
  waitingAcceptCount: number
  pendingCloseCount: number
  closedCount: number
}

// ─── Frontend types ──────────────────────────────────────────────────────────

export interface WorkOrderItem {
  id: string
  sourceEventId?: number
  workOrderNo: string
  status: string // PROCESSING | WAITING_CLOSE_CONFIRM | WAITING_VERIFY | COMPLETED | CLOSED | TIMEOUT
  statusText: string
  isCurrentHandler: boolean
  assigneeName: string
  dispatcherName: string
  eventTitle: string
  eventLocation: string
  eventDescription: string
  merchantName: string | null
  eventType: string
  currentNodeName: string | null
  areaName: string | null
  urgencyLevel: string | null
  processNodes: ProcessNodeVo[]
  actionRecords: ActionRecordVo[]
  createdAt: string
  updatedAt: string
  completedAt: string | null
  closedAt: string | null
}

export interface PendingCountItem {
  key: string
  label: string
  count: number
}

export interface ShortcutItem {
  key: string
  label: string
  description: string
  to: string
}

export interface WorkbenchData {
  pendingCounts: PendingCountItem[]
  shortcuts: ShortcutItem[]
  latestPendingOrders: WorkOrderItem[]
}

export interface WorkOrderAttachmentPayload {
  fileName: string
  fileUrl: string
  fileType?: string
  mimeType?: string
}

// ─── Helpers ────────────────────────────────────────────────────────────────

function formatDateTime(value?: string | null): string {
  if (!value) return ''
  const normalized = value.replace('T', ' ')
  return normalized.length >= 16 ? normalized.slice(0, 16) : normalized
}

function toStatusText(status: string, isCurrentHandler: boolean): string {
  switch (status) {
    case 'COMPLETED':
    case 'CLOSED':
      return '已完成'
    case 'TIMEOUT':
      return '已超时'
    case 'WAITING_CLOSE_CONFIRM':
      return '待关闭确认'
    case 'WAITING_VERIFY':
      return '待补充证据'
    case 'PROCESSING':
      return isCurrentHandler ? '待我处理' : '处理中'
    default:
      return status
  }
}

function mapBackendWorkOrder(order: BackendWorkOrder): WorkOrderItem {
  const isCurrent = order.isCurrentHandler ?? false
  const isFinalized = order.status === 'COMPLETED' || order.status === 'CLOSED' || order.status === 'TIMEOUT'
  const hasNoActiveAssignee = isFinalized || order.status === 'WAITING_CLOSE_CONFIRM'
  return {
    id: String(order.id),
    sourceEventId: order.sourceEventId || undefined,
    workOrderNo: order.workOrderNo,
    status: order.status,
    statusText: toStatusText(order.status, isCurrent),
    isCurrentHandler: isCurrent,
    assigneeName: order.assigneeName || (hasNoActiveAssignee ? '无' : '待分配'),
    dispatcherName: order.dispatcherName || '',
    eventTitle: order.eventTitle || `工单 ${order.workOrderNo}`,
    eventLocation: order.eventLocation || '',
    eventDescription: order.eventDescription || '',
    merchantName: order.merchantName ?? null,
    eventType: order.eventType || '',
    currentNodeName: order.currentNodeName ?? null,
    areaName: order.areaName ?? null,
    urgencyLevel: order.urgencyLevel ?? null,
    processNodes: Array.isArray(order.processNodes) ? order.processNodes : [],
    actionRecords: Array.isArray(order.actionRecords) ? order.actionRecords : [],
    createdAt: formatDateTime(order.createdAt),
    updatedAt: formatDateTime(order.updatedAt),
    completedAt: order.completedAt ? formatDateTime(order.completedAt) : null,
    closedAt: order.closedAt ? formatDateTime(order.closedAt) : null
  }
}

const staticShortcuts: ShortcutItem[] = [
  { key: 'todo', label: '我的待办', description: '查看当前节点由我处理的工单', to: '/work-orders' },
  { key: 'history', label: '历史记录', description: '查看已完成任务与历史凭证', to: '/history' }
]

// ─── Public API ──────────────────────────────────────────────────────────────

export async function getWorkbenchData(): Promise<WorkbenchData> {
  const [summary, workOrders] = await Promise.all([
    http.get<BackendWorkbenchSummary>('/workbench'),
    getWorkOrders()
  ])

  const myPending = workOrders.filter((o) => o.isCurrentHandler && o.status === 'PROCESSING')

  return {
    pendingCounts: [
      { key: 'total', label: '总处理工单', count: summary.totalCount },
      { key: 'waitingAccept', label: '待办工单', count: summary.waitingAcceptCount },
      { key: 'pendingClose', label: '待办结工单', count: summary.pendingCloseCount },
      { key: 'closed', label: '已办结工单', count: summary.closedCount }
    ],
    shortcuts: staticShortcuts,
    latestPendingOrders: myPending.slice(0, 5)
  }
}

export async function getWorkOrders(): Promise<WorkOrderItem[]> {
  const orders = await http.get<BackendWorkOrder[]>('/work-orders')
  return orders.map(mapBackendWorkOrder)
}

export async function getWorkOrderDetail(id: string): Promise<WorkOrderItem | null> {
  const order = await http.get<BackendWorkOrder>(`/work-orders/${id}`)
  return mapBackendWorkOrder(order)
}

export async function handleWorkOrder(
  id: string,
  payload: {
    result: string
    remark: string
    attachments?: WorkOrderAttachmentPayload[]
    subjectType?: 'MERCHANT' | 'VENDOR' | null
    subjectId?: number | null
  }
): Promise<WorkOrderItem> {
  const order = await http.post<
    {
      result: string
      remark: string
      attachments?: WorkOrderAttachmentPayload[]
      subjectType?: 'MERCHANT' | 'VENDOR' | null
      subjectId?: number | null
    },
    BackendWorkOrder
  >(`/work-orders/${id}/handle`, {
    result: payload.result,
    remark: payload.remark,
    attachments: payload.attachments || [],
    subjectType: payload.subjectType ?? null,
    subjectId: payload.subjectId ?? null
  })
  return mapBackendWorkOrder(order)
}

// ─── Legacy-compat exports ───────────────────────────────────────────────────
// These are kept for pages that have not yet been migrated.

export const verifyResultOptions = ['属实并已处理', '不属实', '需补充证据']

export interface VerificationRecord {
  id: string
  orderNo: string
  result: string
  note: string
  submittedAt: string
}

export interface UploadRecord {
  id: string
  fileName: string
  type: 'image' | 'video'
  uploadedAt: string
  relatedOrderNo: string
}

export interface HistoryData {
  completedOrders: WorkOrderItem[]
  verifyRecords: VerificationRecord[]
  uploadRecords: UploadRecord[]
}

/** Returns completed work orders from the real backend, with empty legacy sub-lists. */
export async function getHistoryData(): Promise<HistoryData> {
  const orders = await getWorkOrders()
  return {
    completedOrders: orders.filter((o) => ['COMPLETED', 'CLOSED', 'TIMEOUT'].includes(o.status)),
    verifyRecords: [],
    uploadRecords: []
  }
}
