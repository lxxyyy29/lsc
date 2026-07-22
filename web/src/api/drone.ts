import { http } from './http'
import type { PagedResult } from './types'

export interface DroneWorkspace {
  id: number | string
  workspaceId: string
  workspaceName: string
  workspaceDesc?: string | null
  regionCode?: string | null
  platformName?: string | null
  bindCode?: string | null
}

export interface VideoStream {
  playUrl: string
  videoType?: string
  videoIndex?: string
  switchVideoTypeList?: string[]
}

export interface VideoPlayEntry {
  videoList: VideoStream[]
  cameraIndex?: string
}

export interface DroneInfo {
  droneSn?: string | null
  deviceName?: string | null
  nickname?: string | null
  modeCode?: number | null
  longitude?: number | null
  latitude?: number | null
  workspaceId?: string | null
  regionCode?: string | null
  videoPlayUrl?: VideoPlayEntry[] | null
  videoPlayUrlWebRtc?: VideoPlayEntry[] | null
  videoPlayUrlInner?: VideoPlayEntry[] | null
  videoPlayUrlWebRtcInner?: VideoPlayEntry[] | null
  videoUrl?: VideoPlayEntry[] | null
  videoPushStreamServerUrl?: VideoPlayEntry[] | null
  [key: string]: unknown
}

export interface DroneDevice {
  id: number | string
  deviceSn: string
  deviceName: string
  nickname?: string | null
  firmwareVersion?: string | null
  modeCode?: number | null
  longitude?: number | null
  latitude?: number | null
  workspaceId?: string | null
  workspaceName?: string | null
  regionCode?: string | null
  deviceType?: number | null
  domain?: number | null
  subType?: number | null
  childSn?: string | null
  version?: string | null
  boundStatus?: boolean | null
  boundTime?: number | null
  compatibleStatus?: boolean | null
  createTime?: number | null
  deviceIndex?: string | null
  videoPlayUrl?: VideoPlayEntry[] | null
  videoPlayUrlWebRtc?: VideoPlayEntry[] | null
  videoPlayUrlInner?: VideoPlayEntry[] | null
  videoPlayUrlWebRtcInner?: VideoPlayEntry[] | null
  videoUrl?: VideoPlayEntry[] | null
  videoPushStreamServerUrl?: VideoPlayEntry[] | null
  droneInfo?: DroneInfo | null
  [key: string]: unknown
}

export interface DroneWayline {
  id: number | string
  name?: string
  file_name?: string
  droneModelKey?: string | null
  drone_model_key?: string | null
  updateTime?: string | number | null
  update_time?: string | number | null
  [key: string]: unknown
}

export interface DroneWaylinePoints {
  waylines: Array<[number, number]>
}

export interface DroneJob {
  id?: number | string
  jobId?: string
  job_id?: string
  workspaceId?: string | null
  workspace_id?: string | null
  executeTime?: string | number | null
  execute_time?: string | number | null
  beginTime?: string | number | null
  begin_time?: string | number | null
  status?: number | string | null
  jobName?: string | null
  job_name?: string | null
  taskType?: string | null
  task_type?: string | null
  fileId?: string | null
  file_id?: string | null
  fileName?: string | null
  file_name?: string | null
  dockSn?: string | null
  dock_sn?: string | null
  dockName?: string | null
  dock_name?: string | null
  usernameCn?: string | null
  username_cn?: string | null
  mediaCount?: number | null
  media_count?: number | null
  uploadedCount?: number | null
  uploaded_count?: number | null
  [key: string]: unknown
}

export interface DroneAiModel {
  id: number | string
  name: string
  modelNo?: string | null
  labelList?: string[] | string | null
  status?: number | string | null
  latestTrainingTime?: string | number | null
  onlineTime?: string | number | null
  createTime?: string | number | null
}

export interface DronePaginationQuery {
  page?: number
  pageSize?: number
}

export interface DroneDeviceQuery extends DronePaginationQuery {}

export interface DroneWaylineQuery extends DronePaginationQuery {}

export interface DroneJobQuery extends DronePaginationQuery {
  status?: number
}

export interface CreateDroneJobPayload {
  dockSn: string
  fileId: string
}

export interface PauseResumeDroneJobPayload {
  status: 0 | 1
}

export interface ReturnHomePayload {
  dockSn: string
}

export interface DroneAiModelQuery extends DronePaginationQuery {}

export interface PageResult<T> {
  items?: T[]
  total?: number
  page?: number
  pageSize?: number
}

/** Dock (机场) mode_code → 显示标签 */
export const droneDeviceModeLabelMap: Record<number, string> = {
  0: '空闲',
  1: '调试',
  2: '远程调试',
  3: '升级',
  4: '工作',
  99: '离线'
}

/** Drone (无人机) mode_code → 显示标签，对齐上游 DroneModeCodeEnum */
export const droneAircraftModeLabelMap: Record<number, string> = {
  0: '待机',
  1: '起飞准备',
  2: '起飞完成',
  3: '手动飞行',
  4: '自动起飞',
  5: '航线飞行',
  6: '全景拍照',
  7: '智能跟随',
  8: 'ADS-B 躲避',
  9: '自动返航',
  10: '自动降落',
  11: '强制降落',
  12: '三桨叶降落',
  13: '升级中',
  14: '未连接',
  15: 'APAS',
  16: '虚拟摇杆',
  17: '指令飞行',
  18: 'RTK定位',
  19: '机场评估',
  20: '兴趣点环绕',
  99: '离线'
}

/** mode_code 处于飞行状态的集合 */
export const DRONE_FLYING_MODE_CODES = new Set([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 18, 19, 20])

/** 判断无人机 mode_code 是否为飞行中 */
export function isDroneFlying(modeCode?: number | null): boolean {
  return modeCode != null && DRONE_FLYING_MODE_CODES.has(modeCode)
}

/** 判断无人机 mode_code 是否为离线/未连接 */
export function isDroneOffline(modeCode?: number | null): boolean {
  return modeCode == null || modeCode === 14 || modeCode === 99
}

export const droneJobStatusLabelMap: Record<number, string> = {
  1: '待执行',
  2: '进行中',
  3: '成功',
  4: '取消',
  5: '失败'
}

export const droneJobStatusOptions = [
  { label: '待执行', value: '1' },
  { label: '进行中', value: '2' },
  { label: '成功', value: '3' },
  { label: '取消', value: '4' },
  { label: '失败', value: '5' }
] as const

export interface DroneSocketEnvelope<T = unknown> {
  biz_code?: string
  data?: T
}

export interface DroneDockOsd {
  temperature?: number
  rainfall?: number
  wind_speed?: number
  height?: number
  longitude?: number
  latitude?: number
  modeCode?: number
}

export interface DroneDeviceOsd {
  elevation?: number
  horizontal_speed?: number
  vertical_speed?: number
  height?: number
  longitude?: number
  latitude?: number
  modeCode?: number
  battery?: {
    capacity_percent?: number
  }
}

export interface DroneOfflineNotice {
  sn?: string
  online_status?: boolean
}

export interface DroneAlgorithmTaskStatus {
  currentWaypointIndex?: number
  isEnd?: boolean
  computingVideoPlayUrl?: string | null
  algTaskStaus?: Array<{
    status?: number
  }>
}

/** 格式化 Dock 机场 mode_code */
export function normalizeDroneDeviceMode(modeCode?: number | null) {
  if (modeCode == null) {
    return '未知'
  }

  return droneDeviceModeLabelMap[modeCode] ?? `状态 ${modeCode}`
}

/** 格式化 Drone 无人机 mode_code */
export function normalizeDroneAircraftMode(modeCode?: number | null) {
  if (modeCode == null) {
    return '未连接'
  }

  return droneAircraftModeLabelMap[modeCode] ?? `状态 ${modeCode}`
}

export function normalizeDroneJobStatus(status?: number | string | null) {
  if (status == null || status === '') {
    return '未知'
  }

  const numericStatus = typeof status === 'number' ? status : Number(status)
  return droneJobStatusLabelMap[numericStatus] ?? String(status)
}

/**
 * HTTPS 页面不允许请求 HTTP 资源（Mixed Content）。
 * 当页面是 HTTPS 时，将上游返回的 HTTP 视频 URL 重写为同域代理路径，
 * 由 Nginx `/mediamtx-proxy/` 反向代理到 MediaMTX 服务器。
 * 本地开发（HTTP）不受影响，保持原始 URL。
 */
export function rewriteVideoUrl(url: string | undefined | null): string {
  if (!url) return ''
  if (typeof window === 'undefined') return url

  // 仅在 HTTPS 页面且视频 URL 是 HTTP 时重写
  if (window.location.protocol === 'https:' && url.startsWith('http://')) {
    try {
      const parsed = new URL(url)
      return `/mediamtx-proxy${parsed.pathname}${parsed.search}`
    } catch {
      return url
    }
  }
  return url
}

export function normalizeDroneModelStatus(status?: number | string | null) {
  if (status === 0 || status === '0') {
    return '启用'
  }

  if (status === 1 || status === '1') {
    return '未启用'
  }

  return '未知'
}

export function parseDroneLabels(labelList?: string[] | string | null) {
  if (Array.isArray(labelList)) {
    return labelList.filter((item) => typeof item === 'string' && item.trim().length > 0)
  }

  if (typeof labelList !== 'string' || !labelList.trim()) {
    return []
  }

  try {
    const parsed = JSON.parse(labelList)
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === 'string' && item.trim().length > 0) : []
  } catch {
    return []
  }
}

export function formatDroneTimestamp(value?: string | number | null) {
  if (value == null || value === '') {
    return '--'
  }

  const normalizedValue = typeof value === 'number' ? value : /^\d+$/.test(value) ? Number(value) : value
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

export function buildDroneWebSocketUrl(deviceSn: string) {
  if (typeof window === 'undefined') {
    return ''
  }

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host

  // Browser WebSocket API cannot set custom headers, so pass token as query param
  let token = ''
  try {
    const raw = localStorage.getItem('dgcp-oa-web-session')
    if (raw) {
      const session = JSON.parse(raw)
      token = session?.token ?? ''
    }
  } catch {
    // ignore
  }

  let url = `${protocol}//${host}/api/ws/drone?deviceSn=${encodeURIComponent(deviceSn)}`
  if (token) {
    url += `&token=${encodeURIComponent(token)}`
  }
  return url
}


function normalizeDroneJob(raw: Record<string, unknown>): DroneJob {
  return {
    ...raw,
    jobId:       (raw.jobId       ?? raw.job_id)       as string | undefined,
    workspaceId: (raw.workspaceId ?? raw.workspace_id) as string | undefined,
    executeTime: (raw.executeTime ?? raw.execute_time) as string | number | undefined,
    beginTime:   (raw.beginTime   ?? raw.begin_time)   as string | number | undefined,
    jobName:     (raw.jobName     ?? raw.job_name)     as string | undefined,
    taskType:    (raw.taskType    ?? raw.task_type)    as string | undefined,
    fileId:      (raw.fileId      ?? raw.file_id)      as string | undefined,
    fileName:    (raw.fileName    ?? raw.file_name)    as string | undefined,
    dockSn:      (raw.dockSn      ?? raw.dock_sn)      as string | undefined,
    dockName:    (raw.dockName    ?? raw.dock_name)    as string | undefined,
    usernameCn:  (raw.usernameCn  ?? raw.username_cn)  as string | undefined,
    mediaCount:  (raw.mediaCount  ?? raw.media_count)  as number | undefined,
    uploadedCount:(raw.uploadedCount ?? raw.uploaded_count) as number | undefined,
  }
}

function extractPageItems<T>(payload: unknown) {
  if (Array.isArray(payload)) {
    return payload as T[]
  }

  if (!payload || typeof payload !== 'object') {
    return []
  }

  const page = payload as PageResult<T>
  if (Array.isArray(page.items)) {
    return page.items
  }

  return []
}

export async function listDroneWorkspaces(query: DronePaginationQuery = {}): Promise<DroneWorkspace[]> {
  const payload = await http.get<PageResult<DroneWorkspace>, PageResult<DroneWorkspace>>('/drone/workspaces', {
    params: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 100
    }
  })

  return extractPageItems(payload)
}

export async function listDroneDevices(query: DroneDeviceQuery = {}): Promise<DroneDevice[]> {
  const payload = await http.get<PageResult<DroneDevice>, PageResult<DroneDevice>>('/drone/devices', {
    params: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 100
    }
  })

  return extractPageItems(payload)
}

export async function listDroneDevicesPaged(page: number, pageSize: number): Promise<PagedResult<DroneDevice>> {
  const payload = await http.get<PageResult<DroneDevice>, PageResult<DroneDevice>>('/drone/devices', {
    params: { page, pageSize }
  })
  const items = Array.isArray(payload?.items) ? payload.items : []
  return { items, total: payload?.total ?? items.length, page: payload?.page ?? page, pageSize: payload?.pageSize ?? pageSize }
}

export async function listDroneWaylines(query: DroneWaylineQuery = {}): Promise<DroneWayline[]> {
  const payload = await http.get<PageResult<DroneWayline>, PageResult<DroneWayline>>('/drone/waylines', {
    params: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 100
    }
  })

  return extractPageItems(payload)
}

export async function getDroneWaylinePoints(id: string) {
  return await http.get<DroneWaylinePoints, DroneWaylinePoints>(`/drone/waylines/${id}/points`)
}

export async function listDroneJobsPage(query: DroneJobQuery = {}): Promise<PageResult<DroneJob>> {
  const payload = await http.get<PageResult<DroneJob>, PageResult<DroneJob>>('/drone/jobs', {
    params: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 100,
      ...(query.status ? { status: query.status } : {})
    }
  })

  const items = Array.isArray(payload?.items)
    ? payload.items.map((item) => normalizeDroneJob(item as Record<string, unknown>))
    : []
  return { ...payload, items }
}

export async function listDroneJobs(query: DroneJobQuery = {}): Promise<DroneJob[]> {
  const payload = await listDroneJobsPage(query)
  return extractPageItems(payload)
}

export async function createDroneJob(payload: CreateDroneJobPayload) {
  return await http.post('/drone/jobs', payload)
}

export async function pauseResumeDroneJob(jobId: string, payload: PauseResumeDroneJobPayload) {
  return await http.put(`/drone/jobs/${jobId}/pause-resume`, payload)
}

export async function returnDroneJobHome(payload: ReturnHomePayload) {
  return await http.post('/drone/jobs/return-home', payload)
}

export async function listDroneAiModels(query: DroneAiModelQuery = {}): Promise<DroneAiModel[]> {
  const payload = await http.get<PageResult<DroneAiModel>, PageResult<DroneAiModel>>('/drone/ai-models', {
    params: {
      page: query.page ?? 1,
      pageSize: query.pageSize ?? 100
    }
  })

  return extractPageItems(payload)
}

export async function listDroneAiModelsPaged(page: number, pageSize: number): Promise<PagedResult<DroneAiModel>> {
  const payload = await http.get<PageResult<DroneAiModel>, PageResult<DroneAiModel>>('/drone/ai-models', {
    params: { page, pageSize }
  })
  const items = Array.isArray(payload?.items) ? payload.items : []
  return { items, total: payload?.total ?? items.length, page: payload?.page ?? page, pageSize: payload?.pageSize ?? pageSize }
}

export interface AiBindingItem {
  modelName?: string
  model_name?: string
  modelNo?: string
  model_no?: string
  label?: string
  startPointIndex?: number
  start_point_index?: number
  endPointIndex?: number
  end_point_index?: number
  actions?: string
  fileId?: string
  file_id?: string
  classThresholdMap?: string
  confThreshold?: number
  skipFrame?: number
  pushFrequency?: number
  push_frequency?: number
  violationAreaIds?: number[]
  violation_area_ids?: number[]
}

export async function getWaylineAiBindingDetail(flyLineId: string): Promise<AiBindingItem[]> {
  return await http.get(`/drone/ai-models/binding/${flyLineId}`)
}

// --- Qwen Algorithm Model types and API ---

export interface QwenAlgorithmModel {
  id: number
  name: string
  label: string
  intervalSecond: number
  status: number
  description?: string | null
  createTime?: string | null
  updateTime?: string | null
}

export interface QwenBindingRequest {
  label: string
  startPointIndex: number
  endPointIndex: number
  intervalSecond: number
  violationAreaIds?: number[]
}

export async function listQwenModelsPaged(page = 1, pageSize = 10): Promise<PagedResult<QwenAlgorithmModel>> {
  const payload = await http.get<PageResult<QwenAlgorithmModel>, PageResult<QwenAlgorithmModel>>('/qwen-models', {
    params: { page, pageSize }
  })
  const items = Array.isArray(payload?.items) ? payload.items : []
  return { items, total: payload?.total ?? items.length, page: payload?.page ?? page, pageSize: payload?.pageSize ?? pageSize }
}

export async function listQwenModelsEnabled(): Promise<QwenAlgorithmModel[]> {
  const result = await http.get<QwenAlgorithmModel[], QwenAlgorithmModel[]>('/qwen-models/enabled')
  return Array.isArray(result) ? result : []
}

export async function createQwenModel(data: {
  name: string
  label: string
  intervalSecond?: number
  description?: string
}): Promise<QwenAlgorithmModel> {
  return await http.post<QwenAlgorithmModel, QwenAlgorithmModel>('/qwen-models', data)
}

export async function updateQwenModel(id: number, data: Partial<QwenAlgorithmModel>): Promise<QwenAlgorithmModel> {
  return await http.put<QwenAlgorithmModel, QwenAlgorithmModel>(`/qwen-models/${id}`, data)
}

export async function deleteQwenModel(id: number): Promise<void> {
  await http.delete(`/qwen-models/${id}`)
}

export async function bindQwenToWayline(flyLineId: string, bindings: QwenBindingRequest[]): Promise<void> {
  await http.post(`/drone/ai-models/waylines/${flyLineId}/bind-qwen`, { bindings })
}

// --- Media Center types and API ---

export interface MediaFolder {
  jobId: string
  fileName: string
  isDir: boolean
  createTime?: string | null
}

export interface MediaFile {
  id?: string | null
  fileName: string
  fileType?: string | null
  droneSn?: string | null
  payloadName?: string | null
  createTime?: string | null
  objectKey?: string | null
  subFileType?: number | null
  previewUrl?: string | null
}

export interface MediaFileQuery {
  fileName?: string
  startTime?: string
  endTime?: string
}

export async function listMediaFolders(query: MediaFileQuery = {}): Promise<MediaFolder[]> {
  const result = await http.get<MediaFolder[], MediaFolder[]>('/drone/media/files', {
    params: {
      ...(query.fileName ? { fileName: query.fileName } : {}),
      ...(query.startTime ? { startTime: query.startTime } : {}),
      ...(query.endTime ? { endTime: query.endTime } : {})
    }
  })

  return Array.isArray(result) ? result : []
}

export async function getMediaFilesByJobId(jobId: string): Promise<MediaFile[]> {
  const result = await http.get<MediaFile[], MediaFile[]>(`/drone/media/files/${encodeURIComponent(jobId)}`)
  return Array.isArray(result) ? result : []
}

export function isMediaVideo(fileName?: string | null): boolean {
  if (!fileName) return false
  return /\.(mp4|mov|avi|mkv|flv|wmv|webm)$/i.test(fileName)
}

export function isMediaImage(fileName?: string | null): boolean {
  if (!fileName) return false
  return /\.(jpg|jpeg|png|gif|bmp|tiff|webp|dng|raw)$/i.test(fileName)
}

export function getMediaTypeLabel(fileName?: string | null): string {
  if (isMediaVideo(fileName)) return '视频'
  if (isMediaImage(fileName)) return '图片'
  return '--'
}

// --- Legacy media record builder (kept for compatibility) ---

export function buildDroneMediaRecords(jobs: DroneJob[], devices: DroneDevice[]) {
  const deviceNameBySn = new Map(devices.map((item) => [item.deviceSn, item.deviceName]))

  return jobs.flatMap((job) => {
    const uploadedCount = Number(job.uploadedCount ?? job.uploaded_count ?? job.mediaCount ?? job.media_count ?? 0)
    if (!Number.isFinite(uploadedCount) || uploadedCount <= 0) {
      return []
    }

    const jobId = (job.jobId || job.job_id || job.id || 'job') as string
    const jobFileName = (job.fileName || job.file_name || job.jobName || job.job_name || '航线任务') as string
    const dockSn = (job.dockSn || job.dock_sn) as string | undefined
    const dockName = (job.dockName || job.dock_name) as string | undefined
    const jobTime = (job.beginTime || job.begin_time || job.executeTime || job.execute_time) as string | number | null

    return Array.from({ length: uploadedCount }, (_, index) => ({
      id: `${jobId}-${index + 1}`,
      fileName: `${jobFileName}-媒体-${index + 1}`,
      mediaType: index % 2 === 0 ? '图片' : '视频',
      deviceName: (dockSn ? deviceNameBySn.get(dockSn) : undefined) || dockName || '--',
      payloadName: '机载相机',
      createTime: jobTime || null
    }))
  })
}
