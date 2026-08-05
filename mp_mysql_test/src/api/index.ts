import axios from 'axios'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const session = JSON.parse(localStorage.getItem('grid-mp-session') || '{}')
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

http.interceptors.response.use(
  response => {
    const data = response.data
    if (data && data.success === true) {
      return data.data
    }
    return Promise.reject(translateError(data?.message || '请求失败'))
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('grid-mp-session')
      window.location.href = '#/login'
    }
    return Promise.reject(translateError(error.response?.data?.message || '网络错误，请检查网络连接'))
  }
)

export async function login(account: string, password: string) {
  const data = await http.post('/auth/login', { account, password, clientType: 'web' })
  localStorage.setItem('grid-mp-session', JSON.stringify(data))
  return data
}

export async function register(account: string, password: string, realName: string, phone: string) {
  await http.post('/test/register', { account, password, realName, phone })
}

export function getSession() {
  return JSON.parse(localStorage.getItem('grid-mp-session') || '{}')
}

export function logout() {
  localStorage.removeItem('grid-mp-session')
}

export async function reportEvent(data: any) {
  return http.post('/events/public-report', data)
}

// 上传现场照片（multipart/form-data），返回带完整访问 URL 的文件信息
export async function uploadMedia(file: File, businessType = 'PUBLIC_REPORT') {
  const form = new FormData()
  form.append('file', file)
  form.append('businessType', businessType)
  return http.post('/media/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export async function getMyReports(params?: { page?: number; pageSize?: number }) {
  return http.get('/events/my-reports', { params: { page: 1, pageSize: 10, ...params } })
}

export async function rateEvent(eventId: number | string, data: { rating: number; comment?: string }) {
  return http.post(`/events/${eventId}/rate`, data)
}

// ==================== 居民互动 ====================

export async function getResidentActivities() {
  return http.get('/resident/activities')
}
export async function signupActivity(activityId: number) {
  return http.post(`/resident/activities/${activityId}/signup`, {})
}
export async function cancelActivitySignup(activityId: number) {
  return http.delete(`/resident/activities/${activityId}/signup`)
}

export async function getResidentPolicies() {
  return http.get('/resident/policy-resources')
}

export async function getMyPoints() {
  return http.get('/resident/points')
}

export async function submitRepair(data: any) {
  return http.post('/resident/repairs', data)
}
export async function getMyRepairs() {
  return http.get('/resident/repairs')
}
export async function getRepairDetail(id: number) {
  return http.get(`/resident/repairs/${id}`)
}

export default http
