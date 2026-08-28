<template>
  <view class="chat-page">
    <!-- 会话列表 -->
    <scroll-view v-if="!activePartner" class="conv-list" scroll-y>
      <view v-if="loading" class="state-row">加载中...</view>
      <view v-else-if="conversations.length === 0" class="state-row">暂无会话</view>
      <view
        v-for="conv in conversations"
        v-else
        :key="conv.partner_id"
        class="conv-item"
        @click="openConversation(conv)"
      >
        <view class="conv-avatar"><text>{{ conv.partner?.real_name?.slice(-1) || '?' }}</text></view>
        <view class="conv-main">
          <view class="conv-top">
            <text class="conv-name">{{ conv.partner?.real_name || conv.partner?.username || `用户${conv.partner_id}` }}</text>
            <text class="conv-time">{{ formatTime(conv.last_created) }}</text>
          </view>
          <view class="conv-bottom">
            <text class="conv-last">{{ conv.last_content }}</text>
            <text v-if="conv.unread_count > 0" class="conv-badge">{{ conv.unread_count }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 聊天详情 -->
    <view v-else class="chat-detail">
      <view class="chat-header">
        <view class="back-btn" @click="closeConversation">
          <text class="back-icon">‹</text>
        </view>
        <text class="chat-title">{{ activePartner.partner?.real_name || activePartner.partner?.username || `用户${activePartner.partner_id}` }}</text>
      </view>

      <scroll-view class="msg-list" scroll-y :scroll-into-view="scrollToMsgId" scroll-with-animation>
        <view
          v-for="msg in messages"
          :key="msg.id"
          :id="`msg-${msg.id}`"
          class="msg-row"
          :class="{ 'msg-row--self': msg.senderId === myUserId }"
        >
          <view class="msg-bubble">
            <text class="msg-text">{{ msg.content }}</text>
          </view>
          <text class="msg-time">{{ formatTime(msg.createdAt) }}</text>
        </view>
        <view v-if="!messages.length" class="msg-empty">发送第一条消息开始对话</view>
      </scroll-view>

      <view class="chat-input-bar">
        <input
          v-model="inputText"
          class="chat-input"
          placeholder="输入消息..."
          confirm-type="send"
          @confirm="sendMessage"
        />
        <view class="send-btn" :class="{ 'send-btn--active': inputText.trim() }" @click="sendMessage">
          <text>发送</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { getH5Session } from '../../src/api/auth'
import { h5ChatClient } from '../../src/api/chat'
import {
  ConversationItem,
  MessageItem,
  getMessageConversations,
  getMessageHistory,
  markMessagesRead,
} from '../../src/api/message'

const conversations = ref<ConversationItem[]>([])
const loading = ref(false)
const activePartner = ref<ConversationItem | null>(null)
const messages = ref<MessageItem[]>([])
const inputText = ref('')
const scrollToMsgId = ref('')

const myUserId = computed(() => getH5Session()?.userId ?? 0)

function formatTime(value: string) {
  if (!value) return ''
  const s = value.replace('T', ' ')
  return s.length >= 16 ? s.slice(5, 16) : s
}

async function loadConversations() {
  loading.value = true
  try {
    conversations.value = await getMessageConversations()
  } catch (e) {
    console.error('加载会话失败', e)
  } finally {
    loading.value = false
  }
}

async function openConversation(conv: ConversationItem) {
  activePartner.value = conv
  try {
    messages.value = await getMessageHistory(conv.partner_id, 100)
    await markMessagesRead(conv.partner_id)
    conv.unread_count = 0
    await nextTick()
    scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

function closeConversation() {
  activePartner.value = null
  loadConversations()
}

function scrollToBottom() {
  if (messages.value.length > 0) {
    const last = messages.value[messages.value.length - 1]
    scrollToMsgId.value = `msg-${last.id}`
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !activePartner.value) return
  h5ChatClient.send(activePartner.value.partner_id, text)
  // 本地立即显示（服务端也会通过 WS 推送回执）
  messages.value.push({
    id: Date.now(),
    senderId: myUserId.value,
    receiverId: activePartner.value.partner_id,
    content: text,
    contentType: 'TEXT',
    readAt: null,
    createdAt: new Date().toISOString(),
  })
  inputText.value = ''
  nextTick(() => scrollToBottom())
}

function onWsMessage(msg: any) {
  // 收到实时消息
  const myId = myUserId.value
  // 如果消息与当前会话相关，添加到列表
  if (
    activePartner.value &&
    ((msg.senderId === activePartner.value.partner_id && msg.receiverId === myId) ||
      (msg.senderId === myId && msg.receiverId === activePartner.value.partner_id))
  ) {
    // 避免重复（自己发的消息回执）
    if (msg.senderId !== myId) {
      messages.value.push({
        id: msg.id || Date.now(),
        senderId: msg.senderId,
        receiverId: msg.receiverId,
        content: msg.content,
        contentType: msg.contentType || 'TEXT',
        readAt: null,
        createdAt: msg.createdAt || new Date().toISOString(),
      })
      // 标记已读
      markMessagesRead(msg.senderId)
      nextTick(() => scrollToBottom())
    }
  }
  // 刷新会话列表
  loadConversations()
}

onMounted(() => {
  loadConversations()
  h5ChatClient.onMessage = onWsMessage
  h5ChatClient.connect()
})

onUnmounted(() => {
  h5ChatClient.onMessage = null
})
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #030913;
}

.state-row {
  text-align: center;
  color: #8db0d0;
  padding: 40rpx;
  font-size: 28rpx;
}

// 会话列表
.conv-list {
  flex: 1;
  padding: 16rpx;
}

.conv-item {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.conv-avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #57b9ff, #3d8ad6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  margin-right: 20rpx;
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}

.conv-name {
  color: #fff;
  font-size: 30rpx;
  font-weight: 500;
}

.conv-time {
  color: #8db0d0;
  font-size: 22rpx;
}

.conv-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-last {
  flex: 1;
  color: #8db0d0;
  font-size: 26rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 16rpx;
}

.conv-badge {
  background: #ff4d4f;
  color: #fff;
  font-size: 20rpx;
  min-width: 36rpx;
  height: 36rpx;
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 10rpx;
}

// 聊天详情
.chat-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 24rpx;
  background: rgba(6, 18, 31, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.back-btn {
  margin-right: 20rpx;
  padding: 0 12rpx;
}

.back-icon {
  color: #57b9ff;
  font-size: 48rpx;
  line-height: 1;
}

.chat-title {
  color: #fff;
  font-size: 32rpx;
  font-weight: 500;
}

.msg-list {
  flex: 1;
  padding: 24rpx;
  overflow-y: auto;
}

.msg-row {
  margin-bottom: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-start;

  &--self {
    align-items: flex-end;

    .msg-bubble {
      background: linear-gradient(135deg, #57b9ff, #3d8ad6);
    }
  }
}

.msg-bubble {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16rpx;
  padding: 20rpx 28rpx;
  max-width: 80%;
  word-break: break-all;
}

.msg-text {
  color: #fff;
  font-size: 30rpx;
  line-height: 1.5;
}

.msg-time {
  color: #8db0d0;
  font-size: 22rpx;
  margin-top: 8rpx;
  padding: 0 8rpx;
}

.msg-empty {
  text-align: center;
  color: #8db0d0;
  font-size: 26rpx;
  padding: 60rpx 0;
}

// 输入栏
.chat-input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: rgba(6, 18, 31, 0.95);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.chat-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 32rpx;
  padding: 16rpx 28rpx;
  color: #fff;
  font-size: 28rpx;
  margin-right: 16rpx;
}

.send-btn {
  padding: 16rpx 32rpx;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 32rpx;
  color: #8db0d0;
  font-size: 28rpx;

  &--active {
    background: linear-gradient(135deg, #57b9ff, #3d8ad6);
    color: #fff;
  }
}
</style>
