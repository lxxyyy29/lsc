import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs'
import { getH5Session } from './auth'

/**
 * H5 端信息互通 WebSocket 客户端（原生 WebSocket + STOMP）
 * - 连接 /ws，通过 token query param 认证
 * - 订阅 /user/queue/messages 接收实时消息
 * - 使用原生 WebSocket（H5 浏览器不需要 SockJS 回退）
 */
class H5ChatClient {
  private client: Client | null = null
  private subscription: StompSubscription | null = null
  onMessage: ((msg: any) => void) | null = null
  onConnected: (() => void) | null = null
  onDisconnected: (() => void) | null = null

  connect() {
    const session = getH5Session()
    if (!session?.token) {
      console.warn('[H5ChatClient] no session token, skip WS connect')
      return
    }

    const wsUrl = `${location.origin.replace(/^http/, 'ws')}/ws-native?token=${encodeURIComponent(session.token)}`
    console.log('[H5ChatClient] connecting to', wsUrl)

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        const sub = this.client?.subscribe('/user/queue/messages', (message: IMessage) => {
          try {
            const body = JSON.parse(message.body)
            this.onMessage?.(body)
          } catch (e) {
            console.error('[H5ChatClient] message parse error:', e)
          }
        })
        this.subscription = sub ?? null
        this.onConnected?.()
      },
      onDisconnect: () => {
        this.onDisconnected?.()
      },
      onStompError: (frame) => {
        console.error('[H5ChatClient] STOMP error:', frame)
      },
      debug: (str) => {
        console.debug('[H5ChatClient]', str)
      },
    })
    this.client.activate()
  }

  /** 发送消息 */
  send(receiverId: number, content: string) {
    if (!this.client?.connected) {
      console.warn('[H5ChatClient] cannot send: STOMP client not connected')
      return
    }
    this.client.publish({
      destination: '/app/send',
      body: JSON.stringify({ receiverId, content, contentType: 'TEXT' }),
    })
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
export const h5ChatClient = new H5ChatClient()
