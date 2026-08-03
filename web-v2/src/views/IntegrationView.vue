<template>
  <div class="page-container">
    <div class="page-header">
      <h2>信息互通</h2>
      <p class="page-desc">与网格员实时沟通，支持文本消息</p>
    </div>

    <div v-if="loadError" style="background:#fff2f0;border:1px solid #ffccc7;border-radius:8px;padding:12px 16px;margin-bottom:16px;color:#cf1322;font-size:13px;">
      {{ loadError }}
    </div>

    <div class="chat-layout">
      <!-- 左侧：会话列表 -->
      <div class="chat-sidebar">
        <div class="sidebar-header">
          <span>会话列表</span>
          <button class="btn btn-sm" @click="showWorkerPicker = true" title="发起新会话">
            <i class="fas fa-plus"></i>
          </button>
        </div>
        <div class="conversation-list">
          <div
            v-for="conv in conversations"
            :key="conv.partner_id"
            :class="['conversation-item', { active: activePartnerId === conv.partner_id }]"
            @click="selectConversation(conv)"
          >
            <div class="conv-avatar">
              {{ getPartnerName(conv).slice(0, 1) }}
            </div>
            <div class="conv-info">
              <div class="conv-top">
                <span class="conv-name">{{ getPartnerName(conv) }}</span>
                <span class="conv-time">{{ formatTime(conv.last_created) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-last">{{ conv.last_content || '暂无消息' }}</span>
                <span v-if="conv.unread_count > 0" class="conv-unread">{{ conv.unread_count }}</span>
              </div>
            </div>
          </div>
          <p v-if="!conversations.length" class="empty-tip">暂无会话，点击 + 发起沟通</p>
        </div>
      </div>

      <!-- 右侧：聊天面板 -->
      <div class="chat-main">
        <template v-if="activePartnerId">
          <div class="chat-header">
            <span class="chat-title">{{ activePartnerName }}</span>
            <span :class="['ws-status', wsConnected ? 'online' : 'offline']">
              {{ wsConnected ? '实时连接中' : '连接断开' }}
            </span>
          </div>

          <div class="chat-messages" ref="msgListRef">
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['msg-row', msg.senderId === currentUserId ? 'me' : 'other']"
            >
              <div class="msg-avatar">
                {{ (msg.senderId === currentUserId ? '我' : activePartnerName).slice(0, 1) }}
              </div>
              <div class="msg-body">
                <div class="msg-bubble">{{ msg.content }}</div>
                <div class="msg-time">{{ formatTime(msg.createdAt || msg.created_at) }}</div>
              </div>
            </div>
            <p v-if="!messages.length" class="empty-tip">发送第一条消息开始对话</p>
          </div>

          <div class="chat-input">
            <textarea
              v-model="inputText"
              placeholder="输入消息，Enter 发送，Shift+Enter 换行"
              rows="2"
              @keydown.enter.exact.prevent="sendText"
            ></textarea>
            <button class="btn btn-primary" :disabled="!inputText.trim() || !wsConnected" @click="sendText">
              发送
            </button>
          </div>
        </template>
        <div v-else class="chat-empty">
          <i class="fas fa-comments" style="font-size:48px;color:#d1d5db;"></i>
          <p>选择一个会话开始沟通</p>
        </div>
      </div>
    </div>

    <!-- 选择网格员对话框 -->
    <div v-if="showWorkerPicker" class="dialog-overlay" @click.self="showWorkerPicker = false">
      <div class="dialog" style="width:420px;">
        <div class="dialog-header">
          <h3>选择网格员发起会话</h3>
          <button @click="showWorkerPicker = false" class="btn-close">&times;</button>
        </div>
        <div class="dialog-body">
          <div v-for="w in workers" :key="w.id" class="worker-item" @click="startConversation(w)">
            <div class="conv-avatar">{{ (w.real_name || w.user_name || w.account).slice(0, 1) }}</div>
            <div>
              <div style="font-weight:500;">{{ w.real_name || w.user_name || w.account }}</div>
              <div style="font-size:12px;color:#9ca3af;">{{ w.account }}</div>
            </div>
          </div>
          <p v-if="!workers.length" class="empty-tip">暂无网格员</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, onErrorCaptured } from 'vue'
import {
  getMessageConversations,
  getMessageHistory,
  markMessagesRead,
  getGridWorkers
} from '../api'
import { chatClient } from '../api/chat'
import { getSession } from '../api'

const conversations = ref<any[]>([])
const workers = ref<any[]>([])
const messages = ref<any[]>([])
const activePartnerId = ref<number | null>(null)
const activePartnerName = ref('')
const inputText = ref('')
const showWorkerPicker = ref(false)
const wsConnected = ref(false)
const msgListRef = ref<HTMLElement | null>(null)
const loadError = ref('')

const currentUserId = computed(() => getSession()?.userId || getSession()?.id || 0)

async function loadConversations() {
  try {
    conversations.value = await getMessageConversations() || []
  } catch (e: any) {
    loadError.value = '加载会话失败: ' + (e.message || e)
    console.error('加载会话失败:', e)
  }
}

async function loadWorkers() {
  try {
    workers.value = await getGridWorkers() || []
  } catch (e: any) {
    console.error('加载网格员失败:', e)
  }
}

function getPartnerName(conv: any): string {
  const partner = conv.partner
  if (partner) {
    return partner.real_name || partner.user_name || partner.account || `用户${conv.partner_id}`
  }
  return `用户${conv.partner_id}`
}

async function selectConversation(conv: any) {
  activePartnerId.value = conv.partner_id
  activePartnerName.value = getPartnerName(conv)
  try {
    messages.value = await getMessageHistory(conv.partner_id) || []
    await markMessagesRead(conv.partner_id)
    conv.unread_count = 0
    scrollToBottom()
  } catch (e) { console.error('加载历史失败:', e) }
}

async function startConversation(w: any) {
  showWorkerPicker.value = false
  // 若会话已存在则直接选中，否则先创建占位
  const existing = conversations.value.find(c => c.partner_id === w.id)
  if (existing) {
    selectConversation(existing)
  } else {
    conversations.value.unshift({
      partner_id: w.id,
      partner: w,
      last_content: '',
      last_created: new Date().toISOString(),
      unread_count: 0
    })
    selectConversation(conversations.value[0])
  }
}

function sendText() {
  const content = inputText.value.trim()
  console.log('[InfoExchange] sendText: content=', content, 'partnerId=', activePartnerId.value, 'wsConnected=', wsConnected.value)
  if (!content || !activePartnerId.value || !wsConnected.value) {
    console.warn('[InfoExchange] send blocked: empty=', !content, 'noPartner=', !activePartnerId.value, 'notConnected=', !wsConnected.value)
    return
  }
  chatClient.send(activePartnerId.value, content)
  inputText.value = ''
}

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}

function formatTime(t: string) {
  if (!t) return ''
  try {
    const d = new Date(t)
    const now = new Date()
    const isToday = d.toDateString() === now.toDateString()
    if (isToday) {
      return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
    }
    return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return '' }
}

onMounted(() => {
  console.log('[InfoExchange] mounted, session:', getSession()?.token ? 'has token' : 'no token')
  loadConversations()
  loadWorkers()
  chatClient.onConnected = () => { wsConnected.value = true }
  chatClient.onDisconnected = () => { wsConnected.value = false }
  chatClient.onMessage = (msg) => {
    console.log('[InfoExchange] WS message received:', msg, 'activePartnerId=', activePartnerId.value)
    // 只处理与当前活跃会话相关的消息
    const related = msg.senderId === activePartnerId.value || msg.receiverId === activePartnerId.value
    if (related) {
      // 避免重复：发送方自己也会收到回执
      const exists = messages.value.some(m => m.id === msg.id)
      if (!exists) {
        messages.value.push(msg)
        scrollToBottom()
      }
    } else {
      console.log('[InfoExchange] message not related to active conversation, skip')
    }
    // 刷新会话列表（最后消息/未读）
    loadConversations()
  }
  try {
    chatClient.connect()
  } catch (e: any) {
    console.error('WS connect error:', e)
    loadError.value = '实时连接失败: ' + (e.message || e)
  }
})

onErrorCaptured((err) => {
  loadError.value = '页面渲染错误: ' + (err?.message || err)
  console.error('IntegrationView error:', err)
  return false
})

onUnmounted(() => {
  chatClient.disconnect()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { font-size: 20px; font-weight: 600; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #6b7280; }

.chat-layout {
  display: flex;
  height: calc(100vh - 160px);
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  overflow: hidden;
}

/* 左侧会话列表 */
.chat-sidebar {
  width: 280px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  font-weight: 600;
  border-bottom: 1px solid #f3f4f6;
}
.conversation-list {
  flex: 1;
  overflow-y: auto;
}
.conversation-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f9fafb;
  transition: background 0.15s;
}
.conversation-item:hover { background: #f9fafb; }
.conversation-item.active { background: #e6f4ff; }
.conv-avatar {
  width: 40px; height: 40px;
  background: #1890ff; color: #fff;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; flex-shrink: 0;
}
.conv-info { flex: 1; min-width: 0; }
.conv-top { display: flex; justify-content: space-between; align-items: baseline; }
.conv-name { font-weight: 500; font-size: 14px; }
.conv-time { font-size: 11px; color: #9ca3af; flex-shrink: 0; }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 2px; }
.conv-last {
  font-size: 12px; color: #6b7280;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  flex: 1;
}
.conv-unread {
  background: #ef4444; color: #fff;
  font-size: 11px; padding: 0 6px; border-radius: 10px;
  min-width: 18px; text-align: center; flex-shrink: 0; margin-left: 6px;
}

/* 右侧聊天面板 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f3f4f6;
}
.chat-title { font-weight: 600; font-size: 15px; }
.ws-status { font-size: 12px; }
.ws-status.online { color: #52c41a; }
.ws-status.offline { color: #ef4444; }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f7f8fa;
}
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.msg-row.me { flex-direction: row-reverse; }
.msg-avatar {
  width: 36px; height: 36px;
  background: #1890ff; color: #fff;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; flex-shrink: 0;
}
.msg-row.me .msg-avatar { background: #52c41a; }
.msg-body { max-width: 60%; }
.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.msg-row.me .msg-bubble { background: #95de64; }
.msg-time {
  font-size: 11px; color: #9ca3af; margin-top: 4px;
}
.msg-row.me .msg-time { text-align: right; }

.chat-input {
  display: flex;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid #f3f4f6;
  align-items: flex-end;
}
.chat-input textarea {
  flex: 1;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
}
.chat-input textarea:focus { outline: none; border-color: #1890ff; }

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #9ca3af;
}

.empty-tip { text-align: center; padding: 30px; color: #9ca3af; font-size: 13px; }

/* 按钮 & 对话框复用现有风格 */
.btn { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; background: #fff; font-size: 13px; cursor: pointer; }
.btn-primary { background: #1890ff; color: #fff; border-color: #1890ff; }
.btn-sm { padding: 3px 8px; font-size: 12px; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn:hover:not(:disabled) { opacity: 0.85; }
.dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { width: 480px; max-height: 80vh; background: #fff; border-radius: 12px; overflow: hidden; display: flex; flex-direction: column; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f3f4f6; }
.dialog-header h3 { font-size: 16px; font-weight: 600; }
.btn-close { border: none; background: none; font-size: 20px; cursor: pointer; color: #9ca3af; }
.dialog-body { padding: 20px; overflow-y: auto; }
.worker-item {
  display: flex; gap: 12px; align-items: center;
  padding: 12px; border-radius: 8px; cursor: pointer;
}
.worker-item:hover { background: #f0f7ff; }
</style>
