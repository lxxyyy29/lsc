import type { EventStatus } from './event'
import { getEventDetail } from './event'
import { listProcessTemplates } from './process'

export type AuditFilterStatus = 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED'

export interface AuditListItem {
  id: number
  eventCode: string
  eventTitle: string
  status: AuditFilterStatus
  currentNodeStatus: string
  currentNodeName: string
  templateName: string
}

export interface AuditProgressNode {
  id: string
  name: string
  status: 'PENDING' | 'ACTIVE' | 'APPROVED' | 'REJECTED'
  assigneeRole: string
}

export interface AuditDetail {
  id: number
  eventId: number
  eventCode: string
  eventTitle: string
  eventSummary: string
  eventState: EventStatus
  currentAuditStatus: AuditFilterStatus
  selectedTemplateId: number
  selectedTemplateVersion: string
  templateEditable: boolean
  canReselectTemplate: boolean
  isResubmission: boolean
  nodeProgress: AuditProgressNode[]
}

const auditStatusLabelMap: Record<AuditFilterStatus, string> = {
  PENDING: '待审核',
  IN_PROGRESS: '进行中',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const auditNodeStatusLabelMap: Record<AuditProgressNode['status'], string> = {
  PENDING: '待审核',
  ACTIVE: '进行中',
  APPROVED: '已通过',
  REJECTED: '已驳回'
}

const auditDetails: Record<number, AuditDetail> = {
  1: {
    id: 1,
    eventId: 1,
    eventCode: 'EV-20260314-001',
    eventTitle: '桥沥村疑似违法施工',
    eventSummary: '无人机识别到桥沥村河道旁新增施工机械，需核验是否违规施工。',
    eventState: 'WAITING_DISPATCH',
    currentAuditStatus: 'APPROVED',
    selectedTemplateId: 1,
    selectedTemplateVersion: 'v1.2',
    templateEditable: false,
    canReselectTemplate: false,
    isResubmission: false,
    nodeProgress: [
      { id: 'n1', name: '镇街初审', status: 'APPROVED', assigneeRole: '镇街审核员' },
      { id: 'n2', name: '部门复审', status: 'APPROVED', assigneeRole: '业务部门' },
      { id: 'n3', name: '指挥中心终审', status: 'APPROVED', assigneeRole: '指挥中心' }
    ]
  },
  2: {
    id: 2,
    eventId: 2,
    eventCode: 'EV-20260314-002',
    eventTitle: '朗洲村主干道占道经营',
    eventSummary: '视频AI连续识别到占道经营，需提交简化审核。',
    eventState: 'REJECTED',
    currentAuditStatus: 'REJECTED',
    selectedTemplateId: 2,
    selectedTemplateVersion: 'v2.0',
    templateEditable: true,
    canReselectTemplate: true,
    isResubmission: true,
    nodeProgress: [
      { id: 'n4', name: '快速审核', status: 'REJECTED', assigneeRole: '值班审核员' },
      { id: 'n5', name: '复核确认', status: 'PENDING', assigneeRole: '执法队' }
    ]
  },
  3: {
    id: 3,
    eventId: 3,
    eventCode: 'EV-20260314-003',
    eventTitle: '苏坑村空地建筑垃圾堆放',
    eventSummary: '网格员上报空地存在建筑垃圾堆放，等待审核确认是否立案。',
    eventState: 'PENDING_AUDIT',
    currentAuditStatus: 'PENDING',
    selectedTemplateId: 1,
    selectedTemplateVersion: 'v1.2',
    templateEditable: true,
    canReselectTemplate: false,
    isResubmission: false,
    nodeProgress: [
      { id: 'n6', name: '镇街初审', status: 'PENDING', assigneeRole: '镇街审核员' }
    ]
  }
}

const auditList: AuditListItem[] = [
  {
    id: 1,
    eventCode: 'EV-20260314-001',
    eventTitle: '桥沥村疑似违法施工',
    status: 'APPROVED',
    currentNodeStatus: '已通过',
    currentNodeName: '指挥中心终审',
    templateName: '违法施工标准审核流程 v1.2'
  },
  {
    id: 2,
    eventCode: 'EV-20260314-002',
    eventTitle: '朗洲村主干道占道经营',
    status: 'REJECTED',
    currentNodeStatus: '已驳回',
    currentNodeName: '快速审核',
    templateName: '占道经营简化流程 v2.0'
  },
  {
    id: 3,
    eventCode: 'EV-20260314-003',
    eventTitle: '苏坑村空地建筑垃圾堆放',
    status: 'PENDING',
    currentNodeStatus: '待审核',
    currentNodeName: '镇街初审',
    templateName: '违法施工标准审核流程 v1.2'
  }
]

export function listAudits(): AuditListItem[] {
  return auditList
}

export function getAuditStatusLabel(status: AuditFilterStatus): string {
  return auditStatusLabelMap[status]
}

export function getAuditNodeStatusLabel(status: AuditProgressNode['status']): string {
  return auditNodeStatusLabelMap[status]
}

export async function getAuditDetail(id: number): Promise<(AuditDetail & {
  templateOptions: Awaited<ReturnType<typeof listProcessTemplates>>
  eventDetail: NonNullable<Awaited<ReturnType<typeof getEventDetail>>>
}) | undefined> {
  const detail = auditDetails[id]
  if (!detail) {
    return undefined
  }

  const templateOptions = await listProcessTemplates()
  const eventDetail = await getEventDetail(detail.eventId)
  if (!eventDetail) {
    return undefined
  }

  return {
    ...detail,
    templateOptions,
    eventDetail
  }
}
