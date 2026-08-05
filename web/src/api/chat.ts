import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { getSession } from './index'

/**
 * 信息互通 WebSocket 客户端（STOMP over SockJS）
 * - 连接 /ws，通过 token query param 认证
 * - 订阅 /user/queue/messages 接收实时消息
 */
class ChatClient {
  private client: Client | null = null
  private subscription: StompSubscription | null = null
  onMessage: ((msg: any) => void) | null = null
  onConnected: (() => void) | null = null
  onDisconnected: (() => void) | null = null

  connect() {
    const session = getSession()
    if (!session?.token) {
      console.warn('[ChatClient] no session token, skip WS connect')
      return
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(`/api/ws?token=${encodeURIComponent(session.token)}`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('[ChatClient] STOMP connected, subscribing to /user/queue/messages')
        // 订阅个人消息队列
        this.subscription = this.client?.subscribe('/user/queue/messages', (message: IMessage) => {
          try {
            const body = JSON.parse(message.body)
            console.log('[ChatClient] message received:', body)
            this.onMessage?.(body)
          } catch (e) {
            console.error('WS message parse error:', e)
          }
        })
        this.onConnected?.()
      },
      onDisconnect: () => {
        this.onDisconnected?.()
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
      },
    })
    this.client.activate()
  }

  /** 发送消息 */
  send(receiverId: number, content: string) {
    console.log('[ChatClient] send called, connected=', this.client?.connected, 'receiverId=', receiverId, 'content=', content)
    if (!this.client?.connected) {
      console.warn('[ChatClient] cannot send: STOMP client not connected')
      return
    }
    this.client.publish({
      destination: '/app/send',
      body: JSON.stringify({ receiverId, content, contentType: 'TEXT' }),
    })
    console.log('[ChatClient] message published to /app/send')
  }

  disconnect() {
    this.subscription?.unsubscribe()
    this.client?.deactivate()
    this.client = null
  }

  get connected() {
    return this.client?.connected ?? false
  }
}

// 单例
export const chatClient = new ChatClient()
