import { http } from './http'
import type { PagedResult } from './types'

export type ProcessTemplateStatus = 'ACTIVE' | 'DISABLED'
export type ProcessNodeMode = 'SEQUENTIAL'

export interface ProcessTemplateNode {
  id?: number
  nodeKey: string
  nodeName: string
  nodeOrder: number
  nodeMode: ProcessNodeMode
  assigneeUserId: number
  assigneeName: string
}

export interface ProcessTemplate {
  id: number
  templateName: string
  eventType: string
  version: number
  versionLabel: string
  status: ProcessTemplateStatus
  enabled: boolean
  remark: string
  createdAt: string
  updatedAt: string
  nodes: ProcessTemplateNode[]
}

export interface SaveProcessTemplatePayload {
  templateName: string
  eventType?: string
  enabled: boolean
  version?: number
  nodes: Array<{
    assigneeUserId: number
    assigneeName: string
    nodeName?: string
  }>
}

export type CreateProcessTemplatePayload = SaveProcessTemplatePayload

interface BackendProcessTemplateNode {
  id?: number | null
  nodeKey?: string | null
  nodeName?: string | null
  nodeOrder?: number | null
  nodeMode?: string | null
  assigneeUserId?: number | null
  assigneeName?: string | null
}

interface BackendProcessTemplate {
  id?: number | null
  templateName?: string | null
  eventType?: string | null
  businessType?: string | null
  version?: number | null
  versionNo?: number | null
  status?: string | null
  enabled?: boolean | null
  remark?: string | null
  createdAt?: string | null
  updatedAt?: string | null
  nodes?: BackendProcessTemplateNode[] | null
}

function normalizeStatus(status?: string | null): ProcessTemplateStatus {
  return status === 'ACTIVE' ? 'ACTIVE' : 'DISABLED'
}

function mapNode(node: BackendProcessTemplateNode, index: number): ProcessTemplateNode {
  return {
    id: node.id ?? undefined,
    nodeKey: node.nodeKey?.trim() || `node_${index + 1}`,
    nodeName: node.nodeName?.trim() || `审批节点${index + 1}`,
    nodeOrder: Number(node.nodeOrder ?? index + 1),
    nodeMode: 'SEQUENTIAL',
    assigneeUserId: Number(node.assigneeUserId ?? 0),
    assigneeName: node.assigneeName?.trim() || '未指派'
  }
}

function mapTemplate(template: BackendProcessTemplate): ProcessTemplate {
  const version = Number(template.version ?? template.versionNo ?? 1)
  const status = normalizeStatus(template.status)
  return {
    id: Number(template.id ?? 0),
    templateName: template.templateName?.trim() || '未命名流程',
    eventType:
      template.eventType?.trim() === 'DEFAULT' || template.businessType?.trim() === 'DEFAULT'
        ? ''
        : template.eventType?.trim() || template.businessType?.trim() || '',
    version,
    versionLabel: `v${version}`,
    status,
    enabled: template.enabled ?? status === 'ACTIVE',
    remark: template.remark?.trim() || '',
    createdAt: template.createdAt || '',
    updatedAt: template.updatedAt || '',
    nodes: (template.nodes || []).map(mapNode)
  }
}

export async function listProcessTemplates(): Promise<ProcessTemplate[]> {
  const data = await http.get<BackendProcessTemplate[], BackendProcessTemplate[]>('/processes/templates')
  return (data || []).map(mapTemplate)
}

export async function listProcessTemplatesPaged(
  page: number,
  pageSize: number,
  keyword?: string
): Promise<PagedResult<ProcessTemplate>> {
  const raw = await http.get<PagedResult<BackendProcessTemplate>, PagedResult<BackendProcessTemplate>>(
    '/processes/templates/paged',
    { params: { page, pageSize, keyword } }
  )
  return {
    items: (raw.items || []).map(mapTemplate),
    total: raw.total,
    page: raw.page,
    pageSize: raw.pageSize
  }
}

function buildSavePayload(payload: SaveProcessTemplatePayload) {
  return {
    templateName: payload.templateName.trim(),
    eventType: payload.eventType?.trim() || undefined,
    enabled: payload.enabled,
    version: payload.version ?? 1,
    nodes: payload.nodes.map((node, index) => ({
      nodeKey: `node_${index + 1}`,
      nodeName: node.nodeName?.trim() || `审批节点${index + 1}`,
      nodeOrder: index + 1,
      nodeMode: 'SEQUENTIAL',
      assigneeUserId: node.assigneeUserId,
      assigneeName: node.assigneeName.trim()
    }))
  }
}

export async function createProcessTemplate(payload: CreateProcessTemplatePayload): Promise<ProcessTemplate> {
  const data = await http.post<BackendProcessTemplate, BackendProcessTemplate>('/processes/templates', buildSavePayload(payload))
  return mapTemplate(data)
}

export async function getProcessTemplate(id: number): Promise<ProcessTemplate> {
  const data = await http.get<BackendProcessTemplate, BackendProcessTemplate>(`/processes/templates/${id}`)
  return mapTemplate(data)
}

export async function updateProcessTemplate(id: number, payload: SaveProcessTemplatePayload): Promise<ProcessTemplate> {
  const data = await http.put<BackendProcessTemplate, BackendProcessTemplate>(`/processes/templates/${id}`, buildSavePayload(payload))
  return mapTemplate(data)
}

export async function deleteProcessTemplate(id: number): Promise<void> {
  await http.delete(`/processes/templates/${id}`)
}
