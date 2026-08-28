import { http } from './http'

export interface ConversationItem {
  partner_id: number
  last_content: string
  last_created: string
  unread_count: number
  partner?: {
    id: number
    username: string
    real_name: string
  }
}

export interface MessageItem {
  id: number
  senderId: number
  receiverId: number
  content: string
  contentType: string
  readAt: string | null
  createdAt: string
}

// 消息互通 API 位于 /api/messaging（非 /api/h5），需要覆盖 baseURL
// 小程序端必须使用绝对 HTTPS 地址；先声明再条件赋值避开重复声明的 TS 误报
let webApiConfig = { baseURL: '/api' }
// #ifdef MP-WEIXIN
webApiConfig = { baseURL: 'https://drone.kfktec.cn:8443/api' }
// #endif

/** 获取当前用户的会话列表 */
export async function getMessageConversations(): Promise<ConversationItem[]> {
  return http.get<ConversationItem[]>('/messaging/conversations', webApiConfig as any)
}

/** 获取与某用户的历史消息 */
export async function getMessageHistory(partnerId: number, limit = 50): Promise<MessageItem[]> {
  return http.get<MessageItem[]>(`/messaging/history/${partnerId}`, { ...webApiConfig, params: { limit } } as any)
}

/** 标记与某用户的对话为已读 */
export async function markMessagesRead(partnerId: number): Promise<number> {
  return http.post<number, number>(`/messaging/read/${partnerId}`, undefined, webApiConfig as any)
}
