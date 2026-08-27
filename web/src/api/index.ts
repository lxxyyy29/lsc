import axios, { type AxiosRequestConfig } from 'axios'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

axiosInstance.interceptors.request.use(config => {
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  if (session?.token) {
    if (!config.headers) {
      config.headers = {} as any
    }
    (config.headers as Record<string, string>).Authorization = `Bearer ${session.token}`
  }
  return config
})

// 后端错误信息 → 用户友好提示
const ERROR_MAP: Record<string, string> = {
  '外部事件 ID 不能为空': '请填写事件相关信息',
  '地点不能为空': '请填写问题发生地点',
  '发生时间不能为空': '请填写问题发生时间',
  '来源类型不能为空': '请选择来源类型',
  '事件类型不能为空': '请选择问题类型',
  '来源系统不能为空': '请选择来源系统',
  '标题不能为空': '请填写问题标题',
  '描述不能为空': '请填写问题描述',
  '账号和密码不能为空': '请填写账号和密码',
  '账号已存在': '该账号已被注册',
  '账号或密码错误': '账号或密码错误',
}

function translateError(msg: string): string {
  if (!msg) return '操作失败，请稍后重试'
  for (const [key, value] of Object.entries(ERROR_MAP)) {
    if (msg.includes(key)) return value
  }
  return msg
}

axiosInstance.interceptors.response.use(
  response => {
    const data = response.data
    if (data && data.success === true) {
      return data.data
    }
    return Promise.reject(new Error(translateError(data?.message || '请求失败')))
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('grid-session')
      window.location.href = '#/login'
    }
    return Promise.reject(new Error(translateError(error.response?.data?.message || '网络错误，请检查网络连接')))
  }
)

type ApiHttpClient = {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

const http = axiosInstance as unknown as ApiHttpClient

export async function login(account: string, password: string) {
  const data = await http.post('/auth/login', { account, password, clientType: 'web' })
  localStorage.setItem('grid-session', JSON.stringify(data))
  return data
}

export function getSession() {
  return JSON.parse(localStorage.getItem('grid-session') || '{}')
}

export function logout() {
  localStorage.removeItem('grid-session')
}

/** 判断当前登录会话是否拥有指定权限码（未登录返回 false） */
export function hasPermission(code: string): boolean {
  const session = getSession()
  return Array.isArray(session?.permissionCodes) && session.permissionCodes.includes(code)
}

/** web 端管理员注册（无需登录），提交后待超级管理员审批通过才能登录 */
export async function registerAdmin(payload: { account: string; password: string; realName: string; phone: string }) {
  return http.post('/registration/submit', { ...payload, source: 'WEB' })
}

/** 当前登录用户自助修改密码（需校验旧密码；成功后存量令牌失效需重新登录） */
export async function changePassword(oldPassword: string, newPassword: string) {
  return http.put('/auth/change-password', { oldPassword, newPassword })
}

export async function getDashboardOverview() {
  return http.get('/community/dashboard/overview')
}

// 菜单角标（微信式红点）：各模块待处理数量
// 返回 { eventsPending, workOrdersPending, auditsPending, residentReportsPending, trendAlerts }
export async function getMenuBadges() {
  return http.get('/community/dashboard/menu-badges')
}

// 标记菜单角标已读（进入对应页面时调用，红点消失，有新增才再亮）
export async function markBadgeRead(badgeKey: string) {
  return http.post(`/community/dashboard/badges/${badgeKey}/read`)
}

export async function getGridStats() {
  return http.get('/community/dashboard/grid-stats')
}

export async function getGridTree() {
  return http.get('/community/grids/tree')
}

// 网格管理：新增/更新/删除（Web 管理端手动调整网格区域与数量）
export async function createGrid(data: any) {
  return http.post('/community/grids', data)
}

export async function updateGrid(id: number, data: any) {
  return http.put(`/community/grids/${id}`, data)
}

export async function deleteGrid(id: number) {
  return http.delete(`/community/grids/${id}`)
}

export async function getEvents(params?: { page?: number; size?: number; status?: string; urgencyLevel?: string; startDate?: string; endDate?: string; areaId?: number; excludeHidden?: boolean }) {
  return http.get('/events', { params: { page: 1, size: 20, ...params } })
}

export async function getEventDetail(id: number | string) {
  // 如果 id 是数字，使用路径参数；如果是字符串（externalEventId），使用查询参数
  if (typeof id === 'number' || /^\d+$/.test(String(id))) {
    return http.get(`/events/${id}`)
  }
  return http.get(`/events/by-external/${encodeURIComponent(String(id))}`)
}

export async function createEvent(data: any) {
  return http.post('/events', data)
}

// 创建事件时上传现场照片（选填），返回 fileUrl 供随事件提交
export async function uploadEventImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/media/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export async function closeEvent(id: number, reason: string) {
  return http.put(`/events/${id}/close`, { reason })
}

export async function reopenEvent(id: number) {
  return http.put(`/events/${id}/reopen`)
}

export async function dispatchEvent(id: number, data: { assigneeUserId: number; remark?: string }) {
  return http.post(`/events/${id}/dispatch`, data)
}

// 智能分级派单：建议接口 + 一键智能派单
export async function getDispatchSuggestion(eventId: number) {
  return http.get('/work-orders/dispatch-suggestion', { params: { eventId } })
}

export async function smartDispatchEvent(eventId: number, remark?: string) {
  return http.post(`/work-orders/${eventId}/smart-dispatch`, { remark: remark || '' })
}

// 组长两级派单：Web 端组长派单（管理员可操作任意网格）
export async function getLeaderDispatchInfo(eventId: number) {
  return http.get(`/work-orders/leader/events/${eventId}/dispatch-info`)
}

export async function leaderDispatch(eventId: number, data: { assigneeUserId: number; remark?: string }) {
  return http.post(`/work-orders/leader/events/${eventId}/dispatch`, data)
}

export async function getLeaderPendingEvents() {
  return http.get('/work-orders/leader/pending-events')
}

// 派单规则管理（事件类型 → 受理角色）
export async function getDispatchRules() {
  return http.get('/dispatch-rules')
}

export async function createDispatchRule(data: any) {
  return http.post('/dispatch-rules', data)
}

export async function updateDispatchRule(id: number, data: any) {
  return http.put(`/dispatch-rules/${id}`, data)
}

export async function deleteDispatchRule(id: number) {
  return http.delete(`/dispatch-rules/${id}`)
}

// 趋势预判/反复投诉自动预警
// 自动扫描：同一网格同类型事件 7 天内 ≥3 起、同一地点 7 天内 ≥2 次 → 预警
export async function getTrendAlerts(params?: { status?: string; dimension?: string; page?: number; size?: number }) {
  return http.get('/trend-alerts', { params })
}

export async function getTrendAlertStatistics() {
  return http.get('/trend-alerts/statistics')
}

export async function scanTrendAlerts() {
  return http.post('/trend-alerts/scan')
}

export async function handleTrendAlert(id: number, remark: string) {
  return http.post(`/trend-alerts/${id}/handle`, { remark })
}

export async function archiveEvent(id: number) {
  return http.post(`/events/${id}/archive`)
}

// 事件展示隐藏切换：隐藏后大屏/GIS 等面板不再展示，仅事件闭环可见
export async function setEventHidden(id: number | string, hidden: boolean) {
  return http.put(`/events/${id}/hidden`, { hidden })
}

// 删除事件：级联删除关联工单/审核记录/附件，并同步清理 MongoDB 文档
export async function deleteEvents(ids: (number | string)[], reason: string) {
  return http.post('/events/batch-delete', { ids, reason })
}

export async function getEventTimeline(id: number | string) {
  return http.get(`/events/${id}/timeline`)
}

export async function getEventStatistics() {
  return http.get('/events/statistics')
}

export async function ignoreEvent(id: number, reason: string) {
  return http.post(`/events/${id}/ignore`, { reason })
}

export async function auditEvent(id: number, action: string, remark?: string) {
  return http.post(`/events/${id}/audit`, { action, remark })
}

export async function getDrones() {
  return http.get('/drone/dashboard/devices')
}

export async function getDroneJobs() {
  return http.get('/drone/dashboard/jobs')
}

export async function getAiModels(params?: { page?: number; pageSize?: number }) {
  return http.get('/drone/ai-models', { params: { page: 1, pageSize: 10, ...params } })
}

export async function getAiModelBinding(flyLineId: string) {
  return http.get(`/drone/ai-models/binding/${flyLineId}`)
}

export async function getWaylines(params?: { page?: number; pageSize?: number }) {
  return http.get('/drone/dashboard/waylines', { params })
}

// 喊话器
export async function getSpeakerFiles(params?: { page?: number; pageSize?: number }) {
  return http.get('/drone/speaker/files', { params })
}
export async function uploadSpeakerFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post('/drone/speaker/files', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export async function deleteSpeakerFile(id: string | number) {
  return http.delete(`/drone/speaker/files/${id}`)
}
export async function playSpeaker(deviceSn: string, fileId: number) {
  return http.post(`/drone/speaker/${deviceSn}/play`, { id: fileId })
}
export async function stopSpeaker(deviceSn: string) {
  return http.post(`/drone/speaker/${deviceSn}/stop`, {})
}
export async function setSpeakerVolume(deviceSn: string, volume: number) {
  return http.post(`/drone/speaker/${deviceSn}/volume`, { volume })
}

// 载荷/相机控制
export async function switchCameraMode(deviceSn: string, cameraMode: number) {
  return http.post(`/drone/devices/${deviceSn}/camera/mode`, { camera_mode: cameraMode })
}
export async function startRecording(deviceSn: string) {
  return http.post(`/drone/devices/${deviceSn}/camera/record-start`, {})
}
export async function stopRecording(deviceSn: string) {
  return http.post(`/drone/devices/${deviceSn}/camera/record-stop`, {})
}

// 飞行任务管理
export async function getJobs(params?: { page?: number; pageSize?: number; status?: number }) {
  return http.get('/drone/jobs', { params: { page: 1, pageSize: 10, ...params } })
}
export async function createJob(data: { dockSn: string; fileId: string }) {
  return http.post('/drone/jobs', data)
}
export async function pauseResumeJob(jobId: string, status: number) {
  return http.put(`/drone/jobs/${jobId}/pause-resume`, { status })
}
export async function returnHome(dockSn: string) {
  return http.post('/drone/jobs/return-home', { dockSn })
}

export async function getPatrolTasks(params?: { status?: string }) {
  return http.get('/community/patrol-tasks', { params })
}

export async function getOrgMembers() {
  return http.get('/community/org-members')
}

export async function getAllPatrolTasks() {
  return http.get('/community/patrol-tasks')
}

export async function getPatrolTaskStatistics() {
  return http.get('/community/patrol-tasks/statistics')
}

export async function generatePatrolTasks() {
  return http.post('/community/patrol-tasks/generate')
}

export async function markOverduePatrolTasks() {
  return http.post('/community/patrol-tasks/mark-overdue')
}

export async function remindUpcomingPatrolTasks() {
  return http.post('/community/patrol-tasks/upcoming-remind')
}

export async function getResidentReports(params?: { page?: number; pageSize?: number; status?: string }) {
  return http.get('/community/resident-reports', { params })
}

export async function getSystemUsers() {
  return http.get('/system/users')
}

export async function syncGridWorkersToOrgMembers() {
  return http.post('/community/org-members/sync-from-users')
}

export async function getPendingRegistrations() {
  return http.get('/registration/pending')
}

export async function getAllRegistrations() {
  return http.get('/registration/list')
}

export async function approveRegistration(id: number, remark?: string) {
  return http.post(`/registration/${id}/approve`, { remark: remark || '审批通过' })
}

export async function rejectRegistration(id: number, remark: string) {
  return http.post(`/registration/${id}/reject`, { remark })
}

export async function handleResidentReport(id: number, handleResult: string) {
  return http.put(`/community/resident-reports/${id}/handle`, { handleResult })
}

export async function completePatrolTask(id: number) {
  return http.post(`/community/patrol-tasks/${id}/complete`)
}

export async function getPatrolRecords() {
  return http.get('/community/patrol-records')
}

export async function createPatrolRecord(data: any) {
  return http.post('/community/patrol-records', data)
}

// 工单中心
export async function getWorkOrders(params?: { page?: number; pageSize?: number; status?: string; assignee?: string; areaId?: number }) {
  return http.get('/work-orders', { params: { page: 1, pageSize: 10, ...params } })
}
export async function getWorkOrderDetail(id: number | string) {
  return http.get(`/work-orders/${id}`)
}
export async function dispatchWorkOrder(eventId: string | number, data: { processTemplateId: number; remark?: string }) {
  return http.post(`/events/${eventId}/dispatch`, data)
}
export async function handleWorkOrder(id: number | string, data: { action: string; remark?: string; evidence?: any[] }) {
  return http.post(`/work-orders/${id}/handle`, data)
}

// 工单展示隐藏切换：隐藏后大屏等面板不再统计展示，仅工单中心可见
export async function setWorkOrderHidden(id: number | string, hidden: boolean) {
  return http.put(`/work-orders/${id}/hidden`, { hidden })
}

// 关闭确认
export async function confirmCloseWorkOrder(id: number | string, remark?: string) {
  return http.post(`/work-orders/${id}/confirm-close`, { remark: remark || '' })
}
export async function rejectCloseWorkOrder(id: number | string, remark?: string) {
  return http.post(`/work-orders/${id}/reject-close`, { remark: remark || '' })
}

// 信息互通
export async function getExternalSystems(params?: { page?: number; pageSize?: number; keyword?: string }) {
  return http.get('/integration/systems', { params: { page: 1, pageSize: 20, ...params } })
}
export async function getExternalSystem(id: number | string) {
  return http.get(`/integration/systems/${id}`)
}
export async function createExternalSystem(data: any) {
  return http.post('/integration/systems', data)
}
export async function updateExternalSystem(id: number | string, data: any) {
  return http.put(`/integration/systems/${id}`, data)
}
export async function deleteExternalSystem(id: number | string) {
  return http.delete(`/integration/systems/${id}`)
}
export async function triggerSystemSync(id: number | string) {
  return http.post(`/integration/systems/${id}/sync`, {})
}
export async function getSyncLogs(params?: { page?: number; pageSize?: number; systemCode?: string }) {
  return http.get('/integration/systems/sync-logs', { params: { page: 1, pageSize: 20, ...params } })
}
export async function getIntegrationStatistics() {
  return http.get('/integration/systems/statistics')
}

// 审核中心
// 返回 { items, total, page, pageSize, stats }，stats 含 pending/approved/rejected 三个状态的统计数
export async function getAudits(params?: { page?: number; pageSize?: number; status?: string; searchKey?: string }) {
  return http.get('/audits', { params: { page: 1, pageSize: 10, ...params } })
}
export async function getAuditDetail(eventId: number | string) {
  return http.get(`/audits/${eventId}`)
}
export async function startAudit(eventId: number | string, data: { processTemplateId: number; remark?: string }) {
  return http.post(`/audits/${eventId}/start`, data)
}

// 流程模板
export async function getProcessTemplates(params?: { page?: number; pageSize?: number; status?: string }) {
  return http.get('/processes/templates', { params: { page: 1, pageSize: 10, ...params } })
}
export async function getProcessTemplateDetail(id: number | string) {
  return http.get(`/processes/templates/${id}`)
}
export async function createProcessTemplate(data: any) {
  return http.post('/processes/templates', data)
}
export async function updateProcessTemplate(id: number | string, data: any) {
  return http.put(`/processes/templates/${id}`, data)
}

// 考核评价
export async function getRatingStats() {
  return http.get('/assessment/rating-stats')
}

// GIS 热力图与轨迹
export async function getEventHeatmap(params?: { startDate?: string; endDate?: string; eventType?: string }) {
  return http.get('/events/heatmap', { params })
}
export async function getEventMapPoints(params?: { startDate?: string; endDate?: string; status?: string }) {
  return http.get('/events/map-points', { params })
}
export async function getPatrolTrajectories(params?: { startDate?: string; endDate?: string; userId?: number }) {
  return http.get('/community/patrol-records/trajectories', { params })
}

// 审计日志增强
export async function getAuditLogs(params?: {
  tableName?: string; recordId?: string; operationType?: string;
  operatorId?: number; startTime?: string; endTime?: string;
  page?: number; pageSize?: number;
}) {
  return http.get('/audit-logs', { params: { page: 1, pageSize: 20, ...params } })
}
export async function getAuditLogDiff(id: number | string) {
  return http.get(`/audit-logs/${id}/diff`)
}
export async function previewAuditLogRollback(id: number | string) {
  return http.get(`/audit-logs/${id}/preview-rollback`)
}
export async function rollbackAuditLog(id: number | string) {
  return http.post(`/audit-logs/rollback/${id}`)
}
/** 获取单条审计日志详情 */
export async function getAuditLogById(id: number | string) {
  return http.get(`/audit-logs/${id}`)
}

// 站内通知
export async function getNotifications(params?: { page?: number; pageSize?: number }) {
  return http.get('/notifications', { params: { page: 1, pageSize: 20, ...params } })
}
export async function getUnreadCount() {
  return http.get('/notifications/unread-count')
}
export async function markNotificationRead(id: number | string) {
  return http.post(`/notifications/${id}/read`)
}
export async function markAllNotificationsRead() {
  return http.post('/notifications/read-all')
}
export async function deleteNotification(id: number | string) {
  return http.delete(`/notifications/${id}`)
}

// 数据导入
export async function previewImport(type: string, file: File, previewRows = 10) {
  const formData = new FormData()
  formData.append('type', type)
  formData.append('file', file)
  formData.append('previewRows', String(previewRows))
  return http.post('/community/import/preview', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export async function executeImport(type: string, file: File) {
  const formData = new FormData()
  formData.append('type', type)
  formData.append('file', file)
  return http.post('/community/import/execute', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// 表单字段配置器（人口库）
export async function getFormFieldConfig(module: string) {
  return http.get('/community/form-field-config', { params: { module } })
}
export async function saveFormFieldConfig(fields: any[]) {
  return http.post('/community/form-field-config', fields)
}

// 综合监管大屏
export async function getBigScreenData() {
  return http.get('/community/dashboard/big-screen')
}

// 事件评价
export async function submitRating(data: { eventId: number; score: number; content?: string; tags?: string }) {
  return http.post('/event-ratings', data)
}
export async function getEventRatings(eventId: number | string) {
  return http.get(`/event-ratings/event/${eventId}`)
}
export async function getEventRatingStats(eventId: number | string) {
  return http.get(`/event-ratings/event/${eventId}/stats`)
}
export async function getOverallRatingStats() {
  return http.get('/event-ratings/overall-stats')
}

// 批量操作
export async function batchAuditEvents(eventIds: number[], action: string) {
  return http.post('/events/batch-audit', { eventIds, action })
}
export async function batchDispatch(data: { eventIds: number[]; assigneeUserId: number; remark?: string }) {
  return http.post('/events/batch-dispatch', data)
}

// 信息互通（消息）
export async function getMessageConversations() {
  return http.get('/messaging/conversations')
}
export async function getMessageHistory(partnerId: number, limit = 50) {
  return http.get(`/messaging/history/${partnerId}`, { params: { limit } })
}
export async function markMessagesRead(partnerId: number) {
  return http.post(`/messaging/read/${partnerId}`)
}
export async function getGridWorkers() {
  return http.get('/messaging/workers')
}

// ==================== 固定摄像头视频轮巡 ====================

export async function getVideoCameras(params?: {
  keyword?: string
  gridId?: number
  status?: string
  page?: number
  size?: number
}) {
  return http.get('/video/cameras', { params })
}

export async function getVideoCameraStatistics() {
  return http.get('/video/cameras/statistics')
}

export async function createVideoCamera(data: Record<string, any>) {
  return http.post('/video/cameras', data)
}

export async function updateVideoCamera(id: number, data: Record<string, any>) {
  return http.put(`/video/cameras/${id}`, data)
}

export async function deleteVideoCamera(id: number) {
  return http.delete(`/video/cameras/${id}`)
}

export async function getVideoCameraStream(id: number) {
  return http.get(`/video/cameras/${id}/stream`)
}

// 录像回放：日期列表 / 按日录像分段列表（播放走 fetch blob 带认证头）
export async function getVideoRecordDates(id: number) {
  return http.get(`/video/cameras/${id}/records/dates`)
}

export async function getVideoRecords(id: number, date: string) {
  return http.get(`/video/cameras/${id}/records`, { params: { date } })
}

/* ---------- 系统设置：角色管理 ---------- */

export async function getSystemRoles() {
  return http.get('/system/roles')
}

export async function getSystemRoleDetail(id: number) {
  return http.get(`/system/roles/${id}`)
}

export async function createSystemRole(data: { roleCode: string; roleName: string; status?: string; remark?: string }) {
  return http.post('/system/roles', data)
}

export async function updateSystemRole(id: number, data: { roleCode: string; roleName: string; status?: string; remark?: string }) {
  return http.put(`/system/roles/${id}`, data)
}

export async function deleteSystemRole(id: number) {
  return http.delete(`/system/roles/${id}`)
}

/** 为角色分配权限（覆盖式；后端只保留 CATALOG/MENU 类型，API 权限不受影响） */
export async function assignRolePermissions(id: number, permissionIds: number[]) {
  return http.put(`/system/roles/${id}/permissions`, { permissionIds })
}

/** 权限树（按类型筛选，角色授权用 WEB 端菜单权限） */
export async function getPermissionTree(permissionType?: string) {
  return http.get('/system/permissions/tree', { params: permissionType ? { permissionType } : {} })
}

/* ---------- 系统设置：菜单管理 ---------- */

/** 菜单树（当前登录用户可见菜单，侧边栏名称/排序/显隐以数据库为准） */
export async function getMenuTree() {
  return http.get('/auth/menu-tree')
}

/** 全量菜单树（仅超管，菜单管理页编辑用） */
export async function getSystemMenuTree() {
  return http.get('/system/menus/tree')
}

/** 更新菜单项（名称/排序/显隐，需回传完整字段） */
export async function updateSystemMenu(id: number, data: any) {
  return http.put(`/system/menus/${id}`, data)
}

/* ---------- 系统设置：账号管理 ---------- */

export async function getSystemUserDetail(id: number) {
  return http.get(`/system/users/${id}`)
}

export async function createSystemUser(data: { username: string; password: string; realName: string; phone?: string; status?: string; roleIds?: number[]; gridId?: number | null }) {
  return http.post('/system/users', data)
}

export async function updateSystemUser(id: number, data: { username: string; realName: string; phone?: string; status?: string }) {
  return http.put(`/system/users/${id}`, data)
}

export async function updateSystemUserStatus(id: number, status: string) {
  return http.put(`/system/users/${id}/status`, { status })
}

export async function assignUserRoles(id: number, roleIds: number[]) {
  return http.put(`/system/users/${id}/roles`, { roleIds })
}

export async function resetSystemUserPassword(id: number, newPassword: string) {
  return http.put(`/system/users/${id}/password`, { newPassword })
}

export async function deleteSystemUser(id: number) {
  return http.delete(`/system/users/${id}`)
}

// ---------------- 系统字典 ----------------
export interface DictType {
  id: number
  dictCode: string
  dictName: string
  status: string
  remark: string | null
  itemCount: number
}

export interface DictItem {
  id: number
  dictCode: string
  itemValue: string
  itemLabel: string
  sortOrder: number
  status: string
  remark: string | null
}

export async function getDictTypes() {
  return http.get<DictType[]>('/system/dicts')
}

export async function getDictItems(code: string, activeOnly = false) {
  return http.get<DictItem[]>(`/system/dicts/${code}/items`, { params: { activeOnly } })
}

export async function createDictType(data: { dictCode: string; dictName: string; status?: string; remark?: string }) {
  return http.post('/system/dicts', data)
}

export async function updateDictType(id: number, data: { dictName: string; status?: string; remark?: string }) {
  return http.put(`/system/dicts/${id}`, data)
}

export async function deleteDictType(id: number) {
  return http.delete(`/system/dicts/${id}`)
}

export async function createDictItem(code: string, data: { itemValue: string; itemLabel: string; sortOrder?: number; status?: string; remark?: string }) {
  return http.post(`/system/dicts/${code}/items`, data)
}

export async function updateDictItem(itemId: number, data: { itemValue?: string; itemLabel?: string; sortOrder?: number; status?: string; remark?: string }) {
  return http.put(`/system/dicts/items/${itemId}`, data)
}

export async function deleteDictItem(itemId: number) {
  return http.delete(`/system/dicts/items/${itemId}`)
}

export default http
