import { http } from './http'

export interface DistrictReportQuery {
  areaName?: string
  startDate?: string
  endDate?: string
}

export interface DistrictReportItem {
  areaId: number
  areaName: string
  totalEvents: number
  pendingEvents: number
  waitingDispatchEvents: number
  processingEvents: number
  closedEvents: number
  ignoredEvents: number
  totalWorkOrders: number
  completedWorkOrders: number
  avgCompletionHours: number | null
  workOrderCount: number
  closureRate: number
}

export interface DistrictReportSummary {
  totalAreas: number
  totalEvents: number
  closedEvents: number
  pendingEvents: number
  overallClosureRate: number
}

interface BackendDistrictSummary {
  areaId: number
  areaName: string
  totalEvents: number
  pendingEvents: number
  waitingDispatchEvents: number
  processingEvents: number
  closedEvents: number
  ignoredEvents: number
  totalWorkOrders: number
  completedWorkOrders: number
  avgCompletionHours: number | null
}

function toReportItem(row: BackendDistrictSummary): DistrictReportItem {
  const closureRate = row.totalEvents > 0 ? row.closedEvents / row.totalEvents : 0
  return {
    ...row,
    workOrderCount: row.totalWorkOrders,
    closureRate
  }
}

/**
 * Fetch district summary from backend (flat list) and return as paged result.
 * Backend returns all areas at once — pagination is done client-side.
 */
export async function listDistrictReportPaged(
  page: number,
  pageSize: number,
  query: DistrictReportQuery = {}
): Promise<{ items: DistrictReportItem[]; total: number; page: number; pageSize: number }> {
  const params: Record<string, unknown> = {}
  if (query.startDate) params.startDate = query.startDate
  if (query.endDate) params.endDate = query.endDate

  const raw = await http.get<BackendDistrictSummary[]>('/reports/district-summary', { params })
  const allRows = (Array.isArray(raw) ? raw : []).map(toReportItem)

  // Client-side filter by area name (backend doesn't support this filter)
  const keyword = query.areaName?.trim().toLowerCase() || ''
  const filtered = keyword
    ? allRows.filter((r) => r.areaName.toLowerCase().includes(keyword))
    : allRows

  // Client-side pagination
  const start = (page - 1) * pageSize
  const items = filtered.slice(start, start + pageSize)

  return { items, total: filtered.length, page, pageSize }
}

/**
 * Compute summary from the full district list (no separate backend endpoint).
 */
export async function getDistrictReportSummary(
  query: DistrictReportQuery = {}
): Promise<DistrictReportSummary> {
  const params: Record<string, unknown> = {}
  if (query.startDate) params.startDate = query.startDate
  if (query.endDate) params.endDate = query.endDate

  const raw = await http.get<BackendDistrictSummary[]>('/reports/district-summary', { params })
  const rows = Array.isArray(raw) ? raw : []

  const keyword = query.areaName?.trim().toLowerCase() || ''
  const filtered = keyword
    ? rows.filter((r) => r.areaName.toLowerCase().includes(keyword))
    : rows

  const totalAreas = filtered.length
  const totalEvents = filtered.reduce((sum, r) => sum + (r.totalEvents || 0), 0)
  const closedEvents = filtered.reduce((sum, r) => sum + (r.closedEvents || 0), 0)
  const pendingEvents = filtered.reduce((sum, r) => sum + (r.pendingEvents || 0) + (r.waitingDispatchEvents || 0), 0)
  const overallClosureRate = totalEvents > 0 ? closedEvents / totalEvents : 0

  return { totalAreas, totalEvents, closedEvents, pendingEvents, overallClosureRate }
}

export async function exportDistrictReport(query: DistrictReportQuery = {}): Promise<void> {
  const params: Record<string, unknown> = {}
  if (query.startDate) params.startDate = query.startDate
  if (query.endDate) params.endDate = query.endDate

  const raw = await http.get<BackendDistrictSummary[]>('/reports/district-summary/export', { params })
  const rows = Array.isArray(raw) ? raw : []

  // Build CSV with BOM for Excel UTF-8 compatibility
  const headers = ['片区名称', '事件总数', '待处理', '待派遣', '处理中', '已关闭', '已忽略', '工单总数', '已完成工单', '平均完成时长(小时)']
  const csvLines = [headers.join(',')]
  for (const r of rows) {
    csvLines.push([
      r.areaName || '',
      r.totalEvents ?? 0,
      r.pendingEvents ?? 0,
      r.waitingDispatchEvents ?? 0,
      r.processingEvents ?? 0,
      r.closedEvents ?? 0,
      r.ignoredEvents ?? 0,
      r.totalWorkOrders ?? 0,
      r.completedWorkOrders ?? 0,
      r.avgCompletionHours != null ? r.avgCompletionHours.toFixed(2) : ''
    ].join(','))
  }

  const BOM = '\uFEFF'
  const blob = new Blob([BOM + csvLines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `数据报表_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  URL.revokeObjectURL(url)
}
