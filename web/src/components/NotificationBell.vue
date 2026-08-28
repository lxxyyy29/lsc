<template>
  <div style="position:relative;display:inline-block;">
    <!-- 铃铛按钮 -->
    <button @click="togglePanel" style="position:relative;border:none;background:none;font-size:18px;color:#6b7280;cursor:pointer;padding:6px;">
      <i class="fas fa-bell"></i>
      <span v-if="unreadCount > 0" style="position:absolute;top:0;right:0;background:#ff4d4f;color:#fff;font-size:10px;border-radius:8px;padding:1px 5px;min-width:16px;text-align:center;">
        {{ unreadCount > 99 ? '99+' : unreadCount }}
      </span>
    </button>

    <!-- 下拉面板 -->
    <div v-if="showPanel" style="position:absolute;top:100%;right:0;width:340px;background:#fff;border-radius:12px;box-shadow:0 8px 32px rgba(0,0,0,0.15);z-index:1000;overflow:hidden;">
      <!-- 头部 -->
      <div style="display:flex;justify-content:space-between;align-items:center;padding:12px 16px;border-bottom:1px solid #e5e7eb;">
        <span style="font-size:14px;font-weight:600;">消息通知</span>
        <button v-if="unreadCount > 0" @click="markAllRead" style="border:none;background:none;font-size:12px;color:#1890ff;cursor:pointer;">全部已读</button>
      </div>

      <!-- 通知列表 -->
      <div style="max-height:360px;overflow-y:auto;">
        <div v-if="loading" style="text-align:center;padding:20px;color:#9ca3af;font-size:13px;">加载中...</div>
        <div v-else-if="!notifications.length" style="text-align:center;padding:30px;color:#9ca3af;font-size:13px;">
          <i class="fas fa-inbox" style="font-size:24px;margin-bottom:8px;"></i>
          <p>暂无通知</p>
        </div>
        <div v-else>
          <div v-for="n in notifications" :key="n.id"
               style="padding:10px 16px;border-bottom:1px solid #f3f4f6;cursor:pointer;transition:background 0.2s;"
               :style="n.isRead ? '' : 'background:#f0f7ff;'"
               @click="handleClick(n)">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;">
              <div style="flex:1;">
                <div style="font-size:13px;font-weight:500;color:#374151;">
                  <span v-if="!n.isRead" style="display:inline-block;width:6px;height:6px;background:#1890ff;border-radius:50%;margin-right:6px;vertical-align:middle;"></span>
                  {{ n.title }}
                </div>
                <div style="font-size:12px;color:#6b7280;margin-top:4px;">{{ n.content }}</div>
                <div style="font-size:11px;color:#9ca3af;margin-top:4px;">{{ formatTime(n.createdAt) }}</div>
              </div>
              <button @click.stop="deleteNotif(n.id)" style="border:none;background:none;color:#9ca3af;font-size:12px;cursor:pointer;padding:2px 4px;">&times;</button>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部 -->
      <div style="padding:8px 16px;border-top:1px solid #e5e7eb;text-align:center;">
        <button @click="goToAll" style="border:none;background:none;font-size:12px;color:#1890ff;cursor:pointer;">查看全部</button>
      </div>
    </div>

    <!-- 点击外部关闭 -->
    <div v-if="showPanel" style="position:fixed;top:0;left:0;right:0;bottom:0;z-index:999;" @click="showPanel = false;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, getUnreadCount, markNotificationRead, markAllNotificationsRead, deleteNotification } from '../api'

const router = useRouter()
const showPanel = ref(false)
const loading = ref(false)
const unreadCount = ref(0)
const notifications = ref<any[]>([])

function togglePanel() {
  showPanel.value = !showPanel.value
  if (showPanel.value) {
    loadNotifications()
  }
}

async function loadUnreadCount() {
  try {
    unreadCount.value = await getUnreadCount() || 0
  } catch (e) {}
}

async function loadNotifications() {
  loading.value = true
  try {
    const result = await getNotifications({ page: 1, pageSize: 10 })
    notifications.value = result?.items || []
  } catch (e) {
    notifications.value = []
  } finally {
    loading.value = false
  }
}

async function markAllRead() {
  try {
    await markAllNotificationsRead()
    unreadCount.value = 0
    notifications.value.forEach(n => n.isRead = 1)
  } catch (e) {}
}

async function handleClick(n: any) {
  if (!n.isRead) {
    await markNotificationRead(n.id)
    n.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  // 跳转到关联业务
  if (n.relatedType === 'WORK_ORDER' && n.relatedId) {
    router.push('/work-orders')
  } else if (n.relatedType === 'PWD_RESET') {
    router.push('/org-members')
  }
  showPanel.value = false
}

async function deleteNotif(id: number) {
  try {
    await deleteNotification(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
  } catch (e) {}
}

function goToAll() {
  showPanel.value = false
  // 可以跳转到独立的消息中心页面，目前关闭面板即可
}

function formatTime(time: any) {
  if (!time) return ''
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return time
    const now = new Date()
    const diff = now.getTime() - d.getTime()
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
    return d.toLocaleDateString('zh-CN')
  } catch {
    return time
  }
}

// 定时刷新未读数量
let timer: number

onMounted(() => {
  loadUnreadCount()
  timer = window.setInterval(loadUnreadCount, 60000)  // 每分钟刷新
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
