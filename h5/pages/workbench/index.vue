<template>
  <view class="page">
    <!-- Hero: greeting + avatar +待办提示 -->
    <view class="hero-card">
      <view class="hero-glow"></view>
      <view class="hero-top">
        <view class="hero-copy">
          <text class="hero-greeting">{{ timeGreeting }}，{{ userName }}</text>
          <view class="hero-sub">
            <text class="hero-sub-label">{{ isAdmin ? '今日有' : '您有' }}</text>
            <text class="hero-sub-count" :class="{ 'hero-sub-count--urgent': pendingTotal > 0 }">{{ pendingTotal }}</text>
            <text class="hero-sub-label">{{ isAdmin ? '条待办工单' : '条待处理' }}</text>
          </view>
        </view>
        <view class="avatar-chip"><text>{{ userInitial }}</text></view>
      </view>
    </view>

    <!-- 离线采集待同步横幅(有未同步数据时显示) -->
    <view v-if="offlineCount > 0" class="offline-banner" @click="syncOffline">
      <view class="offline-icon"><AppIcon name="upload" size="32rpx" /></view>
      <view class="offline-copy">
        <text class="offline-title">离线采集数据待同步</text>
        <text class="offline-sub">{{ syncing ? '正在同步...' : `共 ${offlineCount} 条数据保存在本地,点击立即同步` }}</text>
      </view>
      <view class="offline-btn"><text>{{ syncing ? '…' : '同步' }}</text></view>
    </view>

    <!-- 事件上报入口（H5 端均为工作人员登录，上报直接进入事件闭环） -->
    <view class="action-section">
      <text class="section-title">事件上报</text>
      <scroll-view scroll-x class="action-scroll" :show-scrollbar="false">
        <view class="action-row">
          <view class="action-capsule" @click="openPath('/pages/event/report')">
            <view class="action-icon"><AppIcon name="camera" size="30rpx" /></view>
            <text class="action-label">事件上报</text>
          </view>
          <view class="action-capsule" @click="openPath('/pages/event/history')">
            <view class="action-icon"><AppIcon name="history" size="30rpx" /></view>
            <text class="action-label">我的上报</text>
          </view>
          <view class="action-capsule" @click="openPath('/pages/history/index')">
            <view class="action-icon"><AppIcon name="list" size="30rpx" /></view>
            <text class="action-label">处理进度</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Stats strip: 3 equal cards -->
    <view class="stats-strip">
      <view v-if="isAdmin" class="stat-card stat-card--accent" @click="openWorkOrders('myPending')">
        <text class="stat-label">待处理工单</text>
        <text class="stat-value stat-value--accent">{{ waitingAcceptText }}</text>
      </view>
      <view v-if="isAdmin" class="stat-card" @click="openWorkOrders('processing')">
        <text class="stat-label">已处理工单</text>
        <text class="stat-value">{{ pendingCloseText }}</text>
      </view>
      <view v-if="isAdmin" class="stat-card" @click="openWorkOrders('completed')">
        <text class="stat-label">已办结工单</text>
        <text class="stat-value">{{ closedText }}</text>
      </view>
      <view v-if="isPublic" class="stat-card stat-card--accent" @click="openPath('/pages/event/report')">
        <text class="stat-label">事件上报</text>
        <text class="stat-value stat-value--accent">去上报</text>
      </view>
      <view v-if="isPublic" class="stat-card" @click="openPath('/pages/event/history')">
        <text class="stat-label">我的上报</text>
        <text class="stat-value">{{ myReportCount }}</text>
      </view>
      <view v-if="isPublic" class="stat-card" @click="openPath('/pages/history/index')">
        <text class="stat-label">处理进度</text>
        <text class="stat-value">查看</text>
      </view>
    </view>

    <!-- Quick actions: horizontal scroll capsules -->
    <view v-if="allActionCards.length" class="action-section">
      <text class="section-title">快捷动作</text>
      <scroll-view scroll-x class="action-scroll" :show-scrollbar="false">
        <view class="action-row">
          <view v-for="item in allActionCards" :key="item.key" class="action-capsule" @click="openPath(item.to)">
            <view class="action-icon"><AppIcon :name="item.icon" size="30rpx" /></view>
            <text class="action-label">{{ item.label }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- Task list: compact -->
    <view class="task-section">
      <view class="section-head">
        <text class="section-title">最近待办</text>
        <text class="section-link" @click="openPath('/work-orders')">查看全部 ›</text>
      </view>

      <view v-if="latestOrders.length" class="task-list">
        <view v-for="item in displayOrders" :key="item.id" class="task-row" @click="openDetail(item.id)">
          <view class="task-dot-col">
            <view class="task-dot"></view>
          </view>
          <view class="task-body">
            <text class="task-title">{{ item.eventTitle }}</text>
            <view class="task-meta">
              <text class="task-meta-text">{{ secondaryTimeText(item) }} · {{ primaryTimeText(item) }}</text>
              <text class="task-badge" :class="statusClass(item.status)">{{ item.statusText }}</text>
            </view>
          </view>
          <text class="task-chevron">›</text>
        </view>
      </view>

      <view v-else class="empty-card">
        <text class="empty-title">当前暂无可处理任务</text>
        <text class="empty-description">有新工单时会自动展示在这里</text>
      </view>

    </view>
    <GridWorkerTabBar current="/pages/workbench/index" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import GridWorkerTabBar from '../../src/components/GridWorkerTabBar.vue'
import AppIcon from '../../src/components/AppIcon.vue'
import { onShow } from '@dcloudio/uni-app'
import { getH5Session } from '../../src/api/auth'
import { hasMenuPermission } from '../../src/auth/permissions'
import { getWorkbenchData, type PendingCountItem, type ShortcutItem, type WorkOrderItem } from '../../src/api/workorder'
import { getMyReportedEvents } from '../../src/api/event'
import { ensureAuthenticated, navigateToPath } from '../../src/uni/navigation'
import { getOfflineTasks, retryOfflineQueue } from '../../src/utils/offlineQueue'

interface ManagementShortcut {
  key: string
  label: string
  to: string
  icon: string
}

const pendingCounts = ref<PendingCountItem[]>([])
const shortcuts = ref<ShortcutItem[]>([])
const latestOrders = ref<WorkOrderItem[]>([])
const loadError = ref(false)
const myReportCount = ref(0)
const offlineCount = ref(0)
const syncing = ref(false)

function refreshOfflineCount() {
  offlineCount.value = getOfflineTasks().length
}

async function syncOffline() {
  if (syncing.value) return
  syncing.value = true
  const result = await retryOfflineQueue()
  syncing.value = false
  refreshOfflineCount()
  if (result.success > 0) {
    uni.showToast({ title: `已同步 ${result.success} 条`, icon: 'success' })
  } else if (result.failed > 0) {
    uni.showToast({ title: '同步失败,请检查网络', icon: 'none' })
  }
}

const filteredShortcuts = computed(() =>
  shortcuts.value.filter((item: ShortcutItem) => (item.to === '/history' ? hasMenuPermission('menu:h5:history:view') : true))
)
const userName = computed(() => getH5Session()?.userName || '用户')
const userInitial = computed(() => userName.value.slice(0, 1))
const userRoles = computed(() => getH5Session()?.roleCodes || [])
const isAdmin = computed(() => userRoles.value.includes('SUPER_ADMIN') || userRoles.value.includes('DISPATCHER') || userRoles.value.includes('AUDITOR') || userRoles.value.includes('H5_WORKER'))
const isPublic = computed(() => !isAdmin.value)
const shortcutCards = computed(() =>
  filteredShortcuts.value.map((item) => ({
    ...item,
    icon: item.key === 'todo' ? 'todo' : item.key === 'verify' ? 'verify' : 'history'
  }))
)
const displayOrders = computed(() => latestOrders.value.slice(0, 5))
const managementShortcuts = computed<ManagementShortcut[]>(() => {
  const items: ManagementShortcut[] = []
  // 消息通知（全员可用，接口层鉴权）
  items.push({ key: 'notices', label: '消息通知', to: '/pages/notice/index', icon: 'chat' })
  if (hasMenuPermission('menu:h5:merchant:view')) {
    items.push({ key: 'merchants', label: '商户管理', to: '/merchants', icon: 'briefcase' })
  }
  if (hasMenuPermission('menu:h5:vendor:view')) {
    items.push({ key: 'vendors', label: '摊贩管理', to: '/vendors', icon: 'gavel' })
  }
  if (hasMenuPermission('menu:h5:volunteer:view')) {
    items.push({ key: 'volunteer', label: '志愿服务', to: '/volunteer', icon: 'activity' })
  }
  // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
  // if (hasMenuPermission('menu:h5:message:view')) {
  //   items.push({ key: 'messages', label: '信息互通', to: '/messages', icon: 'chat' })
  // }
  return items
})
const allActionCards = computed(() => [...shortcutCards.value, ...managementShortcuts.value])

const waitingAcceptText = computed(() => formatCount(findPending('waitingAccept')))
const pendingCloseText = computed(() => formatCount(findPending('pendingClose')))
const closedText = computed(() => formatCount(findPending('closed')))

const pendingTotal = computed(() => findPending('waitingAccept') + findPending('pendingClose'))

const timeGreeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '早上好'
  if (hour < 13) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function findPending(key: string) {
  return pendingCounts.value.find((item) => item.key === key)?.count ?? 0
}

function formatCount(value: number) {
  return String(value).padStart(2, '0')
}

function openPath(path: string) {
  navigateToPath(path)
}

function openWorkOrders(tab: 'myPending' | 'processing' | 'completed') {
  try {
    uni.setStorageSync('workorder-pending-tab', tab)
  } catch {
    // ignore storage errors
  }
  navigateToPath('/work-orders')
}

function openDetail(id: string) {
  navigateToPath(`/work-orders/${id}`)
}

function statusClass(status: string) {
  if (status === 'COMPLETED' || status === 'CLOSED') return 'task-badge--done'
  if (status === 'TIMEOUT') return 'task-badge--warn'
  return 'task-badge--active'
}

function primaryTimeText(item: WorkOrderItem) {
  if (item.updatedAt) return item.updatedAt.slice(5, 16)
  return item.createdAt ? item.createdAt.slice(5, 16) : ''
}

function secondaryTimeText(item: WorkOrderItem) {
  return item.currentNodeName || item.eventType || ''
}

onShow(async () => {
  if (!ensureAuthenticated('/workbench')) return
  refreshOfflineCount()

  try {
    const response = await getWorkbenchData()
    loadError.value = false
    pendingCounts.value = response.pendingCounts
    latestOrders.value = response.latestPendingOrders
    shortcuts.value = response.shortcuts
    getMyReportedEvents()
      .then((items) => { myReportCount.value = items.length })
      .catch(() => { myReportCount.value = 0 })
  } catch {
    loadError.value = true
    pendingCounts.value = []
    latestOrders.value = []
    shortcuts.value = []
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 0 208rpx;
  background:
    radial-gradient(ellipse at top, rgba(20, 60, 110, 0.3) 0%, rgba(20, 60, 110, 0) 50%),
    #081421;
  color: #eef6ff;
}

/* ─── 离线采集待同步横幅 ─── */
.offline-banner {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin: 0 20rpx 16rpx;
  padding: 18rpx 20rpx;
  background: linear-gradient(135deg, rgba(250, 173, 20, 0.16), rgba(250, 140, 22, 0.10));
  border: 1rpx solid rgba(250, 173, 20, 0.45);
  border-radius: 14rpx;
}
.offline-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: rgba(250, 173, 20, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.offline-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2rpx;
}
.offline-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #ffd591;
}
.offline-sub {
  font-size: 22rpx;
  color: #d8b98a;
}
.offline-btn {
  flex-shrink: 0;
  padding: 10rpx 22rpx;
  background: linear-gradient(135deg, #faad14, #fa8c16);
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 600;
  color: #1a1a1a;
}

/* ─── Hero card ───────────────────────────────────── */
.hero-card {
  position: relative;
  margin: calc(env(safe-area-inset-top) + 32rpx) 20rpx 20rpx;
  padding: 32rpx 28rpx;
  border-radius: 24rpx;
  background:
    linear-gradient(135deg, rgba(94, 162, 255, 0.18) 0%, rgba(31, 190, 166, 0.06) 55%, rgba(12, 24, 38, 0.9) 100%),
    linear-gradient(180deg, rgba(20, 42, 72, 0.96) 0%, rgba(12, 24, 38, 0.96) 100%);
  border: 1px solid rgba(118, 189, 255, 0.22);
  box-shadow: 0 16rpx 36rpx rgba(3, 11, 20, 0.32);
  overflow: hidden;
}

.hero-glow {
  position: absolute;
  top: -80rpx;
  right: -60rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 999rpx;
  background: radial-gradient(circle, rgba(77, 185, 255, 0.28) 0%, rgba(77, 185, 255, 0) 70%);
  pointer-events: none;
}

.hero-top {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24rpx;
  z-index: 1;
}

.hero-copy {
  display: grid;
  gap: 10rpx;
  flex: 1;
  min-width: 0;
}

.hero-greeting {
  font-size: 42rpx;
  font-weight: 700;
  color: #f3f8ff;
  letter-spacing: 0.5rpx;
}

.hero-sub {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
}

.hero-sub-label {
  font-size: 28rpx;
  color: rgba(214, 225, 239, 0.75);
}

.hero-sub-count {
  font-size: 34rpx;
  font-weight: 700;
  color: #a8d0ff;
  padding: 0 4rpx;
}

.hero-sub-count--urgent {
  color: #ffb86c;
}

.avatar-chip {
  width: 84rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
  background: linear-gradient(135deg, #ffd4b7 0%, #ffb98b 100%);
  color: #6a2f11;
  font-size: 38rpx;
  font-weight: 700;
  box-shadow: 0 8rpx 18rpx rgba(255, 160, 80, 0.25);
  flex-shrink: 0;
}

/* ─── Stats strip ─────────────────────────────────── */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  padding: 0 20rpx;
  margin-bottom: 28rpx;
}

.stat-card {
  display: grid;
  gap: 10rpx;
  padding: 20rpx 18rpx;
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(16, 30, 48, 0.98) 0%, rgba(12, 24, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.1);
}

.stat-card--accent {
  background: linear-gradient(180deg, rgba(255, 145, 60, 0.14) 0%, rgba(255, 120, 30, 0.08) 100%);
  border-color: rgba(255, 160, 80, 0.25);
}

.stat-label {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.65);
}

.stat-value {
  font-size: 48rpx;
  font-weight: 700;
  color: #f3f8ff;
  line-height: 1;
}

.stat-value--accent {
  color: #ffb86c;
}

/* ─── Quick actions ───────────────────────────────── */
.action-section {
  padding: 0 20rpx;
  margin-bottom: 28rpx;
  display: grid;
  gap: 14rpx;
}

.action-scroll {
  white-space: nowrap;
  overflow: hidden;
}

.action-row {
  display: inline-flex;
  gap: 14rpx;
  padding: 4rpx 0;
}

.action-capsule {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 24rpx 32rpx;
  border-radius: 999rpx;
  background: rgba(16, 30, 48, 0.95);
  border: 1px solid rgba(118, 189, 255, 0.1);
  flex-shrink: 0;
}

.action-icon {
  width: 48rpx;
  height: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: linear-gradient(135deg, rgba(94, 162, 255, 0.2) 0%, rgba(77, 185, 255, 0.12) 100%);
  color: #a8d0ff;
}

.action-label {
  font-size: 30rpx;
  font-weight: 600;
  color: #eaf3fd;
  white-space: nowrap;
}

/* ─── Section common ──────────────────────────────── */
.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-link {
  font-size: 24rpx;
  color: rgba(168, 208, 255, 0.8);
}

/* ─── Task list ───────────────────────────────────── */
.task-section {
  padding: 0 20rpx;
  display: grid;
  gap: 16rpx;
}

.task-list {
  border-radius: 18rpx;
  background: rgba(13, 26, 42, 0.95);
  border: 1px solid rgba(118, 189, 255, 0.08);
  overflow: hidden;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 22rpx 20rpx;
  border-bottom: 1px solid rgba(118, 189, 255, 0.06);
}

.task-row:last-child {
  border-bottom: none;
}

.task-dot-col {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.task-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 999rpx;
  background: #5ea2ff;
  box-shadow: 0 0 10rpx rgba(94, 162, 255, 0.7);
}

.task-body {
  flex: 1;
  min-width: 0;
  display: grid;
  gap: 8rpx;
}

.task-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #f3f8ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.task-meta-text {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.55);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-badge {
  flex-shrink: 0;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  font-size: 22rpx;
  font-weight: 500;
}

.task-badge--active {
  background: rgba(94, 162, 255, 0.14);
  color: #a8d0ff;
  border: 1px solid rgba(94, 162, 255, 0.25);
}

.task-badge--warn {
  background: rgba(255, 170, 64, 0.14);
  color: #ffd8a0;
  border: 1px solid rgba(255, 170, 64, 0.25);
}

.task-badge--done {
  background: rgba(31, 190, 166, 0.12);
  color: #7eded0;
  border: 1px solid rgba(31, 190, 166, 0.2);
}

.task-chevron {
  flex-shrink: 0;
  font-size: 34rpx;
  color: rgba(214, 225, 239, 0.3);
  line-height: 1;
}

/* ─── Empty / Error ───────────────────────────────── */
.empty-card {
  display: grid;
  gap: 10rpx;
  padding: 40rpx 24rpx;
  text-align: center;
  border-radius: 18rpx;
  background: rgba(13, 26, 42, 0.95);
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.empty-title {
  color: #eef6ff;
  font-size: 28rpx;
  font-weight: 600;
}

.empty-description {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.55);
}

.error-banner {
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(117, 32, 48, 0.34);
  color: #ffd7de;
  font-size: 24rpx;
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
