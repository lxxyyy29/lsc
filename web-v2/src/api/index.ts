import axios, { type AxiosRequestConfig } from 'axios'

const axiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

axiosInstance.interceptors.request.use(config => {
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`
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

export async function getMenuTree() {
  return http.get('/auth/menu-tree')
}

export async function getDashboardOverview() {
  return http.get('/community/dashboard/overview')
}

export async function getGridStats() {
  return http.get('/community/dashboard/grid-stats')
}

export async function getGridTree() {
  return http.get('/community/grids/tree')
}

export async function getEvents(params?: { page?: number; size?: number; status?: string; urgencyLevel?: string; startDate?: string; endDate?: string; areaId?: number }) {
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

export async function closeEvent(id: number, reason: string) {
  return http.put(`/events/${id}/close`, { reason })
}

export async function reopenEvent(id: number) {
  return http.put(`/events/${id}/reopen`)
}

export async function dispatchEvent(id: number, data: { assigneeUserId: number; remark?: string }) {
  return http.post(`/events/${id}/dispatch`, data)
}

export async function archiveEvent(id: number) {
  return http.post(`/events/${id}/archive`)
}

export async function importFrom12345(data: {
  title: string
  description?: string
  eventType?: string
  location?: string
  reporterName?: string
  reporterPhone?: string
  externalNo?: string
}) {
  return http.post('/events/12345-import', data)
}

export async function reportFromProperty(data: {
  title: string
  description?: string
  eventType?: string
  location?: string
  propertyName?: string
  reporterName?: string
}) {
  return http.post('/events/property-report', data)
}

export async function getEventTimeline(id: number) {
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
export async function getAudits(params?: { page?: number; pageSize?: number; status?: string }) {
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

export default http
