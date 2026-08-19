<template>
  <view class="page">
    <view class="page-glow"></view>

    <!-- Combined filter card: search + tabs -->
    <view class="filter-card">
      <view class="search-box">
        <AppIcon name="search" class="search-icon" size="32rpx" />
        <input
          v-model.trim="keyword"
          class="search-input"
          placeholder="查询：工单号 / 事件标题 / 地点"
          placeholder-class="search-placeholder"
        />
      </view>

      <view class="filter-divider"></view>

      <view class="tab-bar">
        <view
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ 'tab-item--active': activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <text class="tab-label" :class="{ 'tab-label--active': activeTab === tab.key }">{{ tab.label }}</text>
          <text
            v-if="tab.count > 0"
            class="tab-count"
            :class="{ 'tab-count--active': activeTab === tab.key }"
          >{{ tab.count }}</text>
        </view>
      </view>
    </view>

    <!-- Loading -->
    <view v-if="isLoading" class="empty-card">
      <text class="empty-title">加载中...</text>
    </view>

    <!-- Task list -->
    <view v-else-if="activeItems.length" class="task-list">
      <view
        v-for="item in activeItems"
        :key="item.id"
        class="task-card"
        :class="`task-card--${activeTab}`"
        @click="openDetail(item.id)"
      >
        <!-- Card header -->
        <view class="task-top">
          <view style="display:flex;align-items:center;gap:8rpx;">
            <text class="urgency-dot" :class="`urgency-dot--${item.urgencyLevel || 'NONE'}`"></text>
            <text class="task-no">{{ item.workOrderNo }}</text>
          </view>
          <view class="task-top-right">
            <view class="status-badge" :class="`status-badge--${activeTab}`">
              <text v-if="activeTab === 'myPending'" class="pulse-dot"></text>
              <text class="status-badge-text">{{ activeTabLabel }}</text>
            </view>
            <text class="card-chevron">›</text>
          </view>
        </view>

        <!-- Event title -->
        <text class="task-title">{{ item.eventTitle }}</text>

        <!-- Meta grid -->
        <view class="meta-grid">
          <view class="meta-item">
            <text class="meta-label">当前处理人</text>
            <text class="meta-value">{{ item.assigneeName }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">当前节点</text>
            <text class="meta-value">{{ item.currentNodeName || '--' }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">派单人</text>
            <text class="meta-value">{{ item.dispatcherName || '--' }}</text>
          </view>
          <view class="meta-item">
            <text class="meta-label">派单时间</text>
            <text class="meta-value">{{ formatShortTime(item.createdAt) }}</text>
          </view>
          <view v-if="item.areaName" class="meta-item">
            <text class="meta-label">所属片区</text>
            <text class="meta-value">{{ item.areaName }}</text>
          </view>
          <view v-if="item.eventLocation" class="meta-item">
            <text class="meta-label">事件地点</text>
            <text class="meta-value location-text">{{ item.eventLocation }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Empty state -->
    <view v-else class="empty-card">
      <text class="empty-title">{{ keyword ? '暂无匹配任务' : '暂无工单' }}</text>
      <text class="empty-description">{{ keyword ? '请修改搜索关键词' : '该分类下暂无工单' }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import { onShow } from '@dcloudio/uni-app'
import { getWorkOrders, type WorkOrderItem } from '../../src/api/workorder'
import { ensureAuthenticated, navigateToPath } from '../../src/uni/navigation'

type GroupKey = 'myPending' | 'processing' | 'completed'

interface WorkOrderGroup {
  key: GroupKey
  label: string
  items: WorkOrderItem[]
}

interface Tab {
  key: GroupKey
  label: string
  count: number
}

const workOrders = ref<WorkOrderItem[]>([])
const loadError = ref(false)
const isLoading = ref(false)
const keyword = ref('')
const activeTab = ref<GroupKey>('myPending')
const processedStatuses = ['PROCESSING', 'WAITING_CLOSE_CONFIRM', 'WAITING_VERIFY']
const completedStatuses = ['COMPLETED', 'CLOSED', 'TIMEOUT']

const filteredOrders = computed(() => {
  const search = keyword.value.trim()
  if (!search) return workOrders.value
  return workOrders.value.filter((item) =>
    [item.workOrderNo, item.eventTitle, item.eventLocation, item.merchantName].some((v) => v?.includes(search))
  )
})

// 紧急度排序权重：RED > YELLOW > GREEN > 其他
function urgencyWeight(level: string): number {
  if (level === 'RED') return 0
  if (level === 'YELLOW') return 1
  if (level === 'GREEN') return 2
  return 3
}

function sortByUrgency(items: WorkOrderItem[]): WorkOrderItem[] {
  return [...items].sort((a, b) => urgencyWeight(a.urgencyLevel ?? '') - urgencyWeight(b.urgencyLevel ?? ''))
}

const allGroups = computed<WorkOrderGroup[]>(() => {
  const source = filteredOrders.value
  return [
    {
      key: 'myPending',
      label: '待处理',
      items: sortByUrgency(source.filter((o) => o.isCurrentHandler && o.status === 'PROCESSING'))
    },
    {
      key: 'processing',
      label: '已处理',
      items: sortByUrgency(source.filter((o) => !o.isCurrentHandler && processedStatuses.includes(o.status)))
    },
    {
      key: 'completed',
      label: '已办结',
      items: source.filter((o) => completedStatuses.includes(o.status))
    }
  ]
})

const tabs = computed<Tab[]>(() =>
  allGroups.value.map((g) => ({ key: g.key, label: g.label, count: g.items.length }))
)

const activeItems = computed(() => allGroups.value.find((g) => g.key === activeTab.value)?.items ?? [])
const activeTabLabel = computed(() => tabs.value.find((t) => t.key === activeTab.value)?.label ?? '')
const totalCount = computed(() => tabs.value.reduce((sum, t) => sum + t.count, 0))

function openDetail(id: string) {
  navigateToPath(`/work-orders/${id}`)
}

function formatShortTime(value: string) {
  return value ? value.slice(5, 16) : '--'
}

onShow(async () => {
  if (!ensureAuthenticated('/work-orders')) return

  try {
    const pendingTab = uni.getStorageSync?.('workorder-pending-tab')
    if (pendingTab === 'myPending' || pendingTab === 'processing' || pendingTab === 'completed') {
      activeTab.value = pendingTab
    }
    uni.removeStorageSync?.('workorder-pending-tab')
  } catch {
    // ignore storage errors
  }

  try {
    isLoading.value = true
    loadError.value = false
    workOrders.value = await getWorkOrders()
  } catch {
    loadError.value = true
    workOrders.value = []
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 20rpx 208rpx;
  background:
    radial-gradient(circle at top, rgba(25, 77, 124, 0.32) 0, rgba(25, 77, 124, 0) 38%),
    #081421;
  color: #eef6ff;
  position: relative;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: -120rpx;
  right: -90rpx;
  width: 320rpx;
  height: 320rpx;
  border-radius: 999rpx;
  background: radial-gradient(circle, rgba(77, 185, 255, 0.16) 0%, rgba(77, 185, 255, 0) 72%);
  pointer-events: none;
}

.group-head,
.group-title-row,
.task-top,
.status-badge {
  display: flex;
  align-items: center;
}

.group-head,
.task-top {
  justify-content: space-between;
}

.group-title-row,
.status-badge {
  gap: 12rpx;
}

/* Filter card (search + tabs combined) */
.filter-card {
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(20, 35, 54, 0.94) 0%, rgba(13, 25, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.1);
  box-shadow: 0 16rpx 32rpx rgba(3, 11, 20, 0.28);
  padding: 16rpx 16rpx 10rpx;
  margin-bottom: 22rpx;
  position: relative;
  z-index: 1;
}

.search-box {
  height: 84rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 0 20rpx;
  border-radius: 14rpx;
  background: rgba(7, 18, 31, 0.72);
  border: 1px solid rgba(118, 189, 255, 0.1);
}

.search-icon {
  color: #89a8c7;
}

.search-input {
  flex: 1;
  color: #eaf3fd;
  font-size: 32rpx;
}

.search-placeholder {
  color: rgba(214, 225, 239, 0.45);
  font-size: 32rpx;
}

.filter-divider {
  height: 1px;
  margin: 14rpx -4rpx 4rpx;
  background: linear-gradient(90deg, rgba(118, 189, 255, 0) 0%, rgba(118, 189, 255, 0.18) 50%, rgba(118, 189, 255, 0) 100%);
}

/* Error */
.error-banner {
  display: block;
  padding: 18rpx 20rpx;
  border-radius: 14rpx;
  background: rgba(117, 32, 48, 0.34);
  color: #ffd7de;
  font-size: 24rpx;
  margin-bottom: 18rpx;
  position: relative;
  z-index: 1;
}

/* Tab bar (sits inside filter-card, underline style) */
.tab-bar {
  display: flex;
  gap: 4rpx;
  padding: 0;
}

.tab-item {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  border-radius: 0;
  position: relative;
}

.tab-item--active::after {
  content: '';
  position: absolute;
  left: 30%;
  right: 30%;
  bottom: 6rpx;
  height: 4rpx;
  border-radius: 999rpx;
  background: linear-gradient(90deg, #5ea2ff 0%, #5fd2ff 100%);
  box-shadow: 0 0 10rpx rgba(94, 162, 255, 0.5);
}

.tab-label {
  font-size: 32rpx;
  font-weight: 500;
  color: rgba(214, 225, 239, 0.5);
}

.tab-label--active {
  color: #eaf3fd;
  font-weight: 700;
}

.tab-count {
  min-width: 32rpx;
  height: 28rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: rgba(118, 189, 255, 0.1);
  color: rgba(214, 225, 239, 0.5);
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-count--active {
  background: rgba(94, 162, 255, 0.2);
  color: #a8d0ff;
}

/* Groups */
.group-list,
.task-list {
  display: grid;
  gap: 16rpx;
  position: relative;
  z-index: 1;
}

.group-section {
  display: grid;
  gap: 12rpx;
}

.group-head {
  padding: 0 4rpx;
}

.group-copy {
  display: grid;
  gap: 6rpx;
}

.group-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.group-dot--myPending {
  background: #5ea2ff;
  box-shadow: 0 0 10rpx rgba(94, 162, 255, 0.9);
}

.group-dot--processing {
  background: #5fd2ff;
  box-shadow: 0 0 10rpx rgba(95, 210, 255, 0.9);
}

.group-dot--completed {
  background: #1fbea6;
  box-shadow: 0 0 8rpx rgba(31, 190, 166, 0.6);
}

.group-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.group-description {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.65);
}

.group-count {
  min-width: 52rpx;
  height: 34rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  padding: 0 12rpx;
  font-size: 22rpx;
  background: rgba(111, 202, 255, 0.12);
  border: 1px solid rgba(111, 202, 255, 0.14);
  color: #d7f0ff;
}

/* Task cards */
.task-card {
  display: grid;
  gap: 14rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  border: 1px solid rgba(118, 189, 255, 0.08);
  box-shadow: 0 8rpx 20rpx rgba(3, 11, 20, 0.2);
}

.task-card--myPending {
  background: linear-gradient(180deg, rgba(20, 42, 72, 0.98) 0%, rgba(12, 28, 50, 0.98) 100%);
  border-color: rgba(94, 162, 255, 0.3);
  border-left: 5rpx solid #5ea2ff;
}

.task-card--processing {
  background: linear-gradient(180deg, rgba(20, 42, 72, 0.98) 0%, rgba(12, 28, 50, 0.98) 100%);
  border-color: rgba(95, 210, 255, 0.25);
  border-left: 5rpx solid #5fd2ff;
}

.task-card--completed {
  background: linear-gradient(180deg, rgba(20, 42, 72, 0.98) 0%, rgba(12, 28, 50, 0.98) 100%);
  border-color: rgba(31, 190, 166, 0.25);
  border-left: 5rpx solid #1fbea6;
}

.task-no {
  font-size: 22rpx;
  letter-spacing: 0.5rpx;
  color: rgba(214, 225, 239, 0.78);
}

/* Status badges */
.status-badge {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  gap: 6rpx;
}

.status-badge--myPending {
  background: rgba(94, 162, 255, 0.15);
  color: #a8d0ff;
  border: 1px solid rgba(94, 162, 255, 0.28);
}

.status-badge--processing {
  background: rgba(95, 210, 255, 0.1);
  color: #d7f0ff;
  border: 1px solid rgba(95, 210, 255, 0.14);
}

.status-badge--completed {
  background: rgba(31, 190, 166, 0.1);
  color: #7eded0;
  border: 1px solid rgba(31, 190, 166, 0.2);
}

.status-badge-text {
  font-size: 22rpx;
}

.pulse-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 999rpx;
  background: #5fd2ff;
  box-shadow: 0 0 8rpx rgba(95, 210, 255, 0.9);
}

/* Urgency dot (三色督办) */
.urgency-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}
.urgency-dot--RED {
  background: #ff4d4f;
  box-shadow: 0 0 8rpx rgba(255, 77, 79, 0.8);
}
.urgency-dot--YELLOW {
  background: #faad14;
  box-shadow: 0 0 8rpx rgba(250, 173, 20, 0.7);
}
.urgency-dot--GREEN {
  background: #52c41a;
  box-shadow: 0 0 6rpx rgba(82, 196, 26, 0.6);
}
.urgency-dot--NONE {
  background: rgba(214, 225, 239, 0.3);
}

.task-title {
  font-size: 30rpx;
  font-weight: 700;
  line-height: 1.5;
  color: #f3f8ff;
}

/* Meta grid */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10rpx 20rpx;
}

.meta-item {
  display: grid;
  gap: 4rpx;
}

.meta-label {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.55);
}

.meta-value {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.9);
}

.location-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Card header right */
.task-top-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.card-chevron {
  font-size: 36rpx;
  color: rgba(214, 225, 239, 0.35);
  line-height: 1;
  margin-left: 4rpx;
}

/* Empty */
.empty-card {
  display: grid;
  gap: 10rpx;
  padding: 40rpx 24rpx;
  text-align: center;
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(18, 31, 46, 0.98) 0%, rgba(11, 24, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
  position: relative;
  z-index: 1;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.empty-description {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.65);
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>