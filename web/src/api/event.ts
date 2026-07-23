import { http } from './http'
import { listProcessTemplates } from './process'
import type { PagedResult } from './types'

export type EventStatus =
  | 'PENDING_AUDIT'
  | 'AUDITING'
  | 'WAITING_DISPATCH'
  | 'PROCESSING'
  | 'WAITING_CLOSE_CONFIRM'
  | 'CLOSED'
  | 'REJECTED'
  | 'IGNORED'

export type EventWorkflowNodeStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'RETURNED' | 'DISPATCHED'

export interface EventListItem {
  id: number
  eventCode: string
  sourceType: string
  eventType: string
  currentStatus: EventStatus
  occurredAt: string
  area: string
  title: string
  evidenceReferences: string[]
  longitude?: number
  latitude?: number
  processTemplateId?: number
  processTemplateName?: string
  currentNodeName?: string
  currentNodeStatus?: EventWorkflowNodeStatus
  dispatchable?: boolean
  areaId?: number | null
  areaName?: string | null
  gridId?: number | null
  gridName?: string | null
  urgencyLevel?: string
  reportSource?: string
}

export interface EventLifecycleRecord {
  id: string
  title: string
  description: string
  operator: string
  timestamp: string
  status: EventWorkflowNodeStatus
}

export interface EventDetail extends EventListItem {
  sourceSystem: string
  description: string
  location: string
  longitude: number
  latitude: number
  evidenceReferences: string[]
  lifecycleRecords: EventLifecycleRecord[]
}

export interface EventProcessTemplateNode {
  id: number
  orderNo: number
  name: string
  roleName: string
  mode: string
}

export interface EventProcessTemplateOption {
  id: number
  name: string
  version: string
  eventType: string
  description: string
  nodeCount: number
  nodes: EventProcessTemplateNode[]
}

export interface DispatchEventPayload {
  processTemplateId: number
  remark?: string
}

interface BackendLifecycleRecord {
  action?: string
  status?: string
  remark?: string
  occurredAt?: string
  operator?: string
}

interface BackendEventDetail {
  id?: number | null
  eventCode?: string | null
  externalEventId?: string | null
  sourceType?: string | null
  sourceSystem?: string | null
  eventType?: string | null
  title?: string | null
  description?: string | null
  status?: string | null
  currentStatus?: string | null
  occurredAt?: string | null
  location?: string | null
  area?: string | null
  longitude?: number | string | null
  latitude?: number | string | null
  evidenceReferences?: string[] | null
  lifecycleRecords?: BackendLifecycleRecord[] | null
  processTemplateId?: number | null
  processTemplateName?: string | null
  currentNodeName?: string | null
  currentNodeStatus?: string | null
  dispatchable?: boolean | null
  areaId?: number | null
  areaName?: string | null
  gridId?: number | null
  gridName?: string | null
  urgencyLevel?: string | null
  reportSource?: string | null
}

interface BackendDispatchResponse {
  eventId?: number
  currentStatus?: string | null
  processTemplateId?: number | null
  processTemplateName?: string | null
  currentNodeName?: string | null
  currentNodeStatus?: string | null
}

function mapStatus(status?: string | null): EventStatus {
  switch (status) {
    case 'IN_AUDIT':
      return 'AUDITING'
    case 'DISPATCHED_TO_WORK_ORDER':
      return 'PROCESSING'
    case 'AUDIT_REJECTED':
      return 'REJECTED'
    case 'WAITING_CLOSE_CONFIRM':
      return 'WAITING_CLOSE_CONFIRM'
    case 'WAITING_DISPATCH':
      return 'WAITING_DISPATCH'
    case 'CLOSED':
      return 'CLOSED'
    case 'IGNORED':
      return 'IGNORED'
    case 'PENDING_AUDIT':
    default:
      return 'PENDING_AUDIT'
  }
}

function mapNodeStatus(status?: string | null): EventWorkflowNodeStatus {
  switch (status) {
    case 'ACTIVE':
    case 'IN_PROGRESS':
    case 'AUDITING':
    case 'IN_AUDIT':
      return 'ACTIVE'
    case 'APPROVED':
    case 'COMPLETED':
    case 'CLOSED':
      return 'COMPLETED'
    case 'REJECTED':
    case 'AUDIT_REJECTED':
    case 'RETURNED':
      return 'RETURNED'
    case 'DISPATCHED':
    case 'WAITING_DISPATCH':
      return 'DISPATCHED'
    case 'PENDING':
    case 'PENDING_AUDIT':
    default:
      return 'PENDING'
  }
}

export function getEventStatusLabel(status?: string | null): string {
  switch (status) {
    case 'PENDING_AUDIT':
      return '待配置流程'
    case 'AUDITING':
      return '节点处理中'
    case 'WAITING_DISPATCH':
      return '未派单'
    case 'PROCESSING':
    case 'DISPATCHED_TO_WORK_ORDER':
      return '已派单'
    case 'WAITING_CLOSE_CONFIRM':
      return '待关单'
    case 'CLOSED':
      return '已办结'
    case 'REJECTED':
      return '流程退回'
    case 'IGNORED':
      return '已忽略'
    default:
      return status || '—'
  }
}

export function getWorkflowNodeStatusLabel(status?: EventWorkflowNodeStatus | string | null): string {
  switch (status) {
    case 'PENDING':
      return '待开始'
    case 'ACTIVE':
      return '进行中'
    case 'COMPLETED':
      return '已完成'
    case 'RETURNED':
      return '已退回'
    case 'DISPATCHED':
      return '已派发'
    default:
      return status || '—'
  }
}

export function getEventTypeLabel(type?: string | null): string {
  switch (type) {
    case 'DRONE_ALARM':
      return '无人机告警'
    case 'VIDEO_ALARM':
      return '视频告警'
    case 'MANUAL_REPORT':
      return '人工上报'
    case 'SENSOR_ALARM':
      return '传感器告警'
    case 'PATROL_ISSUE':
      return '巡查问题'
    default:
      return type || '—'
  }
}

export function getSourceSystemLabel(system?: string | null): string {
  switch (system) {
    case 'THIRD_PARTY_DRONE':
      return '第三方无人机平台'
    case 'MANUAL':
      return '人工录入'
    case 'VIDEO_PLATFORM':
      return '视频平台'
    case 'SENSOR_PLATFORM':
      return '传感器平台'
    case 'PATROL_SYSTEM':
      return '巡查系统'
    default:
      return system || '—'
  }
}

export function getLifecycleRecordTitle(title?: string | null): string {
  switch (title) {
    case 'EVENT_INTAKE':
      return '事件接入'
    case 'WORKFLOW_SYNC':
      return '流程同步'
    case 'EVENT_IGNORE':
      return '事件忽略'
    case 'DISPATCH':
      return '事件派单'
    case 'AUDIT_START':
      return '审核发起'
    case 'AUDIT_APPROVE':
      return '审核通过'
    case 'AUDIT_REJECT':
      return '审核驳回'
    case 'NODE_APPROVE':
      return '节点通过'
    case 'NODE_REJECT':
      return '节点驳回'
    case 'CLOSE':
      return '事件关闭'
    default:
      return title || '—'
  }
}

export function getLifecycleRecordDesc(desc?: string | null): string {
  switch (desc) {
    case 'callback received':
      return '接收到告警回调'
    case 'sql workflow synced':
      return '流程状态已同步'
    case 'event dispatched':
      return '事件已派单'
    case 'event ignored':
      return '事件已标记为误报'
    default:
      return desc || '暂无补充说明'
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

function toNumber(value?: number | string | null) {
  if (typeof value === 'number') {
    return value
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isNaN(parsed) ? 0 : parsed
  }
  return 0
}

function mapLifecycleTitle(action?: string | null) {
  switch (action) {
    case 'CREATE':
      return '事件创建'
    case 'SELECT_PROCESS':
      return '流程已选择'
    case 'START_PROCESS':
      return '流程已启动'
    case 'DISPATCH':
      return '处置已派发'
    case 'RETURN':
      return '流程退回'
    default:
      return action || '流程记录'
  }
}

/**
 * Rewrite plain-HTTP MinIO URLs to go through the same-origin Nginx proxy,
 * avoiding mixed-content blocks on HTTPS pages.
 */
function proxyMinioUrl(url: string): string {
  const MINIO_ORIGIN = 'http://8.135.237.224:9001'
  if (url.startsWith(MINIO_ORIGIN)) {
    return '/minio-proxy' + url.substring(MINIO_ORIGIN.length)
  }
  return url
}

function mapLifecycleRecord(record: BackendLifecycleRecord, index: number): EventLifecycleRecord {
  return {
    id: `${record.action ?? 'record'}-${index}`,
    title: mapLifecycleTitle(record.action),
    description: record.remark ?? '',
    operator: record.operator || '系统',
    timestamp: toDisplayTime(record.occurredAt),
    status: mapNodeStatus(record.status)
  }
}

function mapEventDetail(item: BackendEventDetail): EventDetail | null {
  if (item.id == null) {
    return null
  }

  const currentStatus = mapStatus(item.currentStatus ?? item.status)
  const area = item.area || item.location || ''

  return {
    id: item.id,
    eventCode: item.eventCode || `EVT-${item.id}`,
    sourceType: item.sourceType || '',
    eventType: item.eventType || '',
    currentStatus,
    occurredAt: toDisplayTime(item.occurredAt),
    area,
    title: item.title || '',
    sourceSystem: item.sourceSystem || '',
    description: item.description || '',
    location: item.location || area,
    longitude: toNumber(item.longitude),
    latitude: toNumber(item.latitude),
    evidenceReferences: (item.evidenceReferences || []).map(proxyMinioUrl),
    lifecycleRecords: (item.lifecycleRecords || []).map(mapLifecycleRecord),
    processTemplateId: item.processTemplateId ?? undefined,
    processTemplateName: item.processTemplateName || undefined,
    currentNodeName: item.currentNodeName || undefined,
    currentNodeStatus: item.currentNodeStatus ? mapNodeStatus(item.currentNodeStatus) : undefined,
    dispatchable: item.dispatchable ?? ['PENDING_AUDIT', 'AUDITING', 'WAITING_DISPATCH', 'REJECTED'].includes(currentStatus),
    areaId: item.areaId ?? null,
    areaName: item.areaName ?? null,
    urgencyLevel: item.urgencyLevel ?? undefined,
    reportSource: item.reportSource ?? undefined,
    gridId: item.gridId ?? undefined,
    gridName: item.gridName ?? undefined
  }
}

export async function listEvents(): Promise<EventListItem[]> {
  const data = await http.get<BackendEventDetail[], BackendEventDetail[]>('/events', {
    params: {
      page: 1,
      size: 100
    }
  })

  return data
    .map(mapEventDetail)
    .filter((item): item is EventDetail => item !== null)
    .map(({ sourceSystem, description, location, lifecycleRecords, ...listItem }) => listItem)
}

interface BackendPagedEventResult {
  items?: BackendEventDetail[] | null
  total?: number | null
  page?: number | null
  pageSize?: number | null
}

export async function listEventsPaged(
  page: number,
  pageSize: number,
  filters: { keyword?: string; status?: string; startDate?: string; endDate?: string; areaId?: number }
): Promise<PagedResult<EventListItem>> {
  const params: Record<string, unknown> = { page, size: pageSize }
  if (filters.keyword) params.externalEventId = filters.keyword
  if (filters.status) params.status = filters.status
  if (filters.startDate) params.startDate = filters.startDate
  if (filters.endDate) params.endDate = filters.endDate
  if (filters.areaId) params.areaId = filters.areaId

  const raw = await http.get<BackendPagedEventResult | BackendEventDetail[], BackendPagedEventResult | BackendEventDetail[]>('/events', { params })

  // Backend now returns PagedResult; handle both shapes for safety
  if (Array.isArray(raw)) {
    const items = (raw as BackendEventDetail[])
      .map(mapEventDetail)
      .filter((item): item is EventDetail => item !== null)
      .map(({ sourceSystem, description, location, lifecycleRecords, ...listItem }) => listItem)
    return { items, total: items.length, page, pageSize }
  }

  const paged = raw as BackendPagedEventResult
  const rawItems: BackendEventDetail[] = Array.isArray(paged.items) ? paged.items : []
  const items = rawItems
    .map(mapEventDetail)
    .filter((item): item is EventDetail => item !== null)
    .map(({ sourceSystem, description, location, lifecycleRecords, ...listItem }) => listItem)
  return {
    items,
    total: paged.total ?? items.length,
    page: paged.page ?? page,
    pageSize: paged.pageSize ?? pageSize
  }
}

export async function getEventDetail(id: number): Promise<EventDetail | undefined> {
  if (!Number.isFinite(id)) {
    return undefined
  }

  try {
    const data = await http.get<BackendEventDetail, BackendEventDetail>(`/events/${id}`)
    return mapEventDetail(data) ?? undefined
  } catch {
    return undefined
  }
}

export async function listAvailableProcessTemplates(eventType?: string): Promise<EventProcessTemplateOption[]> {
  const templates = await listProcessTemplates()

  return templates
    .filter((template) => template.enabled)
    .filter((template) => !eventType || !template.eventType || template.eventType === eventType)
    .map((template) => ({
      id: template.id,
      name: template.templateName,
      version: template.versionLabel,
      eventType: template.eventType,
      description: template.remark,
      nodeCount: template.nodes.length,
      nodes: template.nodes.map((node) => ({
        id: node.id ?? 0,
        orderNo: node.nodeOrder,
        name: node.nodeName,
        roleName: node.assigneeName,
        mode: 'SEQUENTIAL'
      }))
    }))
}

export async function dispatchEventDirectly(eventId: number, payload: DispatchEventPayload) {
  const data = await http.post<BackendDispatchResponse, BackendDispatchResponse>(`/events/${eventId}/dispatch`, payload)

  return {
    eventId: data?.eventId ?? eventId,
    currentStatus: mapStatus(data?.currentStatus ?? 'PROCESSING'),
    processTemplateId: data?.processTemplateId ?? payload.processTemplateId,
    processTemplateName: data?.processTemplateName,
    currentNodeName: data?.currentNodeName || '处置派发',
    currentNodeStatus: mapNodeStatus(data?.currentNodeStatus ?? 'DISPATCHED')
  }
}

export async function ignoreEvent(eventId: number, reason: string): Promise<void> {
  await http.post(`/events/${eventId}/ignore`, { reason })
}

export async function updateEventUrgency(eventId: number, urgencyLevel: string): Promise<boolean> {
  return http.put<boolean, boolean>(`/events/${eventId}/urgency`, { urgencyLevel })
}

export function getUrgencyLabel(level?: string | null): string {
  switch (level) {
    case 'GREEN': return '一般'
    case 'YELLOW': return '重点'
    case 'RED': return '紧急'
    default: return '未分级'
  }
}

export function getUrgencyTone(level?: string | null): string {
  switch (level) {
    case 'GREEN': return 'green'
    case 'YELLOW': return 'yellow'
    case 'RED': return 'red'
    default: return 'none'
  }
}

export interface FalseAlarmRecord {
  id: number
  eventId: number
  eventCode: string
  eventTitle: string
  eventType: string
  reason: string
  ignoredBy: string
  ignoredAt: string
}

interface BackendFalseAlarmRecord {
  id?: number | null
  eventId?: number | null
  eventCode?: string | null
  eventTitle?: string | null
  eventType?: string | null
  reason?: string | null
  ignoredBy?: string | null
  ignoredAt?: string | null
}

interface BackendPagedFalseAlarmResult {
  items?: BackendFalseAlarmRecord[] | null
  total?: number | null
  page?: number | null
  pageSize?: number | null
}

export async function listFalseAlarmRecords(
  page: number,
  pageSize: number,
  filters: { keyword?: string; startDate?: string; endDate?: string } = {}
): Promise<PagedResult<FalseAlarmRecord>> {
  const params: Record<string, unknown> = { page, size: pageSize }
  if (filters.keyword) params.keyword = filters.keyword
  if (filters.startDate) params.startDate = filters.startDate
  if (filters.endDate) params.endDate = filters.endDate

  const raw = await http.get<BackendPagedFalseAlarmResult, BackendPagedFalseAlarmResult>(
    '/events/false-alarms',
    { params }
  )

  const rawItems: BackendFalseAlarmRecord[] = Array.isArray((raw as BackendPagedFalseAlarmResult).items)
    ? ((raw as BackendPagedFalseAlarmResult).items as BackendFalseAlarmRecord[])
    : Array.isArray(raw) ? (raw as BackendFalseAlarmRecord[]) : []

  const items: FalseAlarmRecord[] = rawItems.map((r) => ({
    id: r.id ?? 0,
    eventId: r.eventId ?? 0,
    eventCode: r.eventCode || '',
    eventTitle: r.eventTitle || '',
    eventType: r.eventType || '',
    reason: r.reason || '',
    ignoredBy: r.ignoredBy || '',
    ignoredAt: toDisplayTime(r.ignoredAt)
  }))

  const paged = raw as BackendPagedFalseAlarmResult
  return {
    items,
    total: paged.total ?? items.length,
    page: paged.page ?? page,
    pageSize: paged.pageSize ?? pageSize
  }
}
