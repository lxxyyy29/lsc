import { http } from './http'

// ─── H5 站内通知（消息中心：应急调度指令/趋势预警等） ─────────────────────

export interface NoticeItem {
  id: number
  userId: number
  title: string
  content: string
  type: string
  level: string
  relatedType: string
  relatedId: number | null
  isRead: number
  readAt: string | null
  createdAt: string
}

export interface NoticePage {
  items: NoticeItem[]
  total: number
  page: number
  size: number
}

/** 我的通知分页列表 */
export async function getNotices(page = 1, size = 20): Promise<NoticePage> {
  return http.get('/notifications', { params: { page, size } })
}

/** 未读数量 */
export async function getNoticeUnreadCount(): Promise<number> {
  return http.get('/notifications/unread-count')
}

/** 标记已读 */
export async function markNoticeRead(id: number): Promise<boolean> {
  return http.post(`/notifications/${id}/read`)
}

/** 全部已读 */
export async function markAllNoticesRead(): Promise<number> {
  return http.post('/notifications/read-all')
}
