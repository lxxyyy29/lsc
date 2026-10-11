<template>
  <view class="page">
    <view class="crumb-row">
      <text>历史归档</text>
      <text>/</text>
      <text>结论回看</text>
      <text>/</text>
      <text>凭证追溯</text>
    </view>

    <text class="hero-copy">汇总已办结工单、核查结论与现场凭证，和“我的”页共用同一组个人入口。</text>

    <view class="hero-card">
      <view>
        <text class="hero-label">当前查看</text>
        <text class="hero-title">历史归档</text>
      </view>
      <button class="hero-button" @click="goMine">回到我的</button>
    </view>

    <view class="summary-grid">
      <view class="summary-card summary-card--large">
        <view class="summary-icon"><AppIcon name="archive" size="26rpx" /></view>
        <view>
          <text class="summary-label">历史总览</text>
          <text class="summary-value">{{ totalHistoryCount || '0' }}</text>
        </view>
      </view>
      <view class="summary-card summary-card--compact">
        <view class="summary-icon"><AppIcon name="calendar" size="26rpx" /></view>
        <view>
          <text class="summary-label">本周摘要</text>
          <text class="summary-value summary-value--small">{{ history.verifyRecords.length }}</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口：原来 6 个图标是纯静态占位、无任何点击事件（用户反馈"不能点击使用"），
         现改为真实可用入口，并让下方直接展示已办结工单明细 -->
    <view class="action-section">
      <text class="section-title">快捷入口</text>
      <view class="action-grid">
        <view class="action-item" @click="goMerchants"><view class="action-icon"><AppIcon name="briefcase" size="24rpx" /></view><text class="action-text">商户管理</text></view>
        <view class="action-item" @click="goVendors"><view class="action-icon"><AppIcon name="folder" size="24rpx" /></view><text class="action-text">摊贩管理</text></view>
        <view class="action-item" @click="goWorkbench"><view class="action-icon"><AppIcon name="plus" size="24rpx" /></view><text class="action-text">开启新任务</text></view>
      </view>
    </view>

    <!-- 已办结工单：渲染真实数据并可点击进入工单详情（原实现只有一个空面板） -->
    <view class="detail-section">
      <text class="section-title">已办结工单（{{ history.completedOrders.length }}）</text>
      <view v-if="history.completedOrders.length" class="record-list">
        <view v-for="order in history.completedOrders" :key="order.id" class="record-card" @click="openOrder(order.id)">
          <view class="record-head">
            <text class="record-title">{{ order.eventTitle || '工单' }}</text>
            <text class="record-status">{{ order.statusText || order.status }}</text>
          </view>
          <text class="record-meta">{{ order.workOrderNo }}<text v-if="order.eventLocation"> · {{ order.eventLocation }}</text></text>
          <text class="record-meta">办结时间：{{ order.closedAt || order.completedAt || order.updatedAt || '-' }}</text>
        </view>
      </view>
      <view v-else class="empty-panel">
        <view class="empty-icon"><AppIcon name="archive" size="32rpx" /></view>
        <text class="empty-title">{{ emptyTitle }}</text>
        <text class="empty-copy">办结后的工单会自动归档到这里，点击可查看处置全过程</text>
        <button class="empty-button" @click="goWorkbench">开启新任务</button>
      </view>
    </view>


  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import { onShow } from '@dcloudio/uni-app'
import { getHistoryData, type HistoryData } from '../../src/api/workorder'
import { ensureAuthenticated, navigateToPath } from '../../src/uni/navigation'

const history = reactive<HistoryData>({ completedOrders: [], verifyRecords: [], uploadRecords: [] })
const isLoading = ref(true)
const loadError = ref(false)

const totalHistoryCount = computed(() => history.completedOrders.length + history.verifyRecords.length + history.uploadRecords.length)
const emptyTitle = computed(() => {
  if (loadError.value) return '暂无已办结工单'
  if (history.completedOrders.length) return history.completedOrders[0].eventTitle
  return '暂无已办结工单'
})


function goMine() {
  navigateToPath('/mine')
}

function goWorkbench() {
  navigateToPath('/workbench')
}

function goMerchants() {
  navigateToPath('/merchants')
}

function goVendors() {
  navigateToPath('/vendors')
}

// 已办结工单 → 工单详情（与工单列表页保持同一跳转方式）
function openOrder(id: string) {
  navigateToPath(`/work-orders/${id}`)
}

onShow(async () => {
  if (!ensureAuthenticated('/history')) return
  isLoading.value = true
  try {
    loadError.value = false
    const response = await getHistoryData()
    history.completedOrders = response.completedOrders
    history.verifyRecords = response.verifyRecords
    history.uploadRecords = response.uploadRecords
  } catch {
    loadError.value = true
    history.completedOrders = []
    history.verifyRecords = []
    history.uploadRecords = []
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
    radial-gradient(circle at top, rgba(25, 77, 124, 0.28) 0, rgba(25, 77, 124, 0) 38%),
    #081421;
  color: #eef6ff;
}

.crumb-row,
.hero-card,
.action-grid,
.bottom-nav,
.bottom-nav__item {
  display: flex;
}

.summary-grid {
  display: grid;
}

.section-title,
.hero-title,
.summary-value,
.empty-title {
  color: #f3f8ff;
}

.crumb-row,
.hero-copy,
.hero-label,
.summary-label,
.action-text,
.empty-copy {
  color: rgba(214, 225, 239, 0.74);
  font-size: 24rpx;
}

.crumb-row {
  gap: 8rpx;
  margin-bottom: 18rpx;
  color: #8ddcff;
}

.hero-copy {
  line-height: 1.7;
  margin-bottom: 18rpx;
}

.hero-card,
.summary-card,
.action-item,
.empty-panel {
  border-radius: 16rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(13, 25, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
  box-shadow: 0 16rpx 32rpx rgba(3, 11, 20, 0.24);
}

.hero-card {
  position: relative;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx;
  margin-bottom: 18rpx;
  overflow: hidden;
}

.hero-card::after {
  content: '';
  position: absolute;
  right: -12rpx;
  top: -14rpx;
  width: 88rpx;
  height: 88rpx;
  background: url('../../src/assets/login-hex.svg') center/contain no-repeat;
  opacity: 0.14;
}

.hero-label {
  display: block;
  margin-bottom: 8rpx;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
}

.hero-button,
.empty-button {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 12rpx;
  background: linear-gradient(90deg, #74d3ff 0%, #31d9ff 100%);
  color: #04111d;
  font-size: 26rpx;
  box-shadow: 0 14rpx 28rpx rgba(49, 217, 255, 0.18);
}

.summary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.summary-card {
  display: grid;
  gap: 12rpx;
  padding: 18rpx;
}

.summary-card--large {
  grid-column: span 2;
  min-height: 144rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.summary-card--compact {
  min-height: 132rpx;
  align-content: start;
}

.summary-caption {
  color: rgba(141, 220, 255, 0.8);
  font-size: 22rpx;
  letter-spacing: 2rpx;
}

.summary-icon {
  width: 44rpx;
  height: 44rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  color: #d8efff;
  background: #162a3d;
}

.summary-value {
  font-size: 44rpx;
  font-weight: 700;
}

.summary-value--small {
  font-size: 44rpx;
  line-height: 1;
}

.action-section,
.detail-section {
  display: grid;
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
}

.action-grid {
  flex-wrap: wrap;
  gap: 14rpx;
}

.action-item {
  width: calc((100% - 28rpx) / 3);
  display: grid;
  justify-items: center;
  gap: 12rpx;
  padding: 20rpx 8rpx;
  box-sizing: border-box;
}

.action-icon {
  width: 54rpx;
  height: 54rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14rpx;
  background: #162a3d;
  color: #eef6ff;
}

.action-icon text {
  color: #eef6ff;
  font-size: 24rpx;
}

.empty-panel {
  display: grid;
  justify-items: center;
  gap: 14rpx;
  padding: 52rpx 24rpx 44rpx;
}

/* 已办结工单列表 */
.record-list {
  display: grid;
  gap: 14rpx;
}

.record-card {
  display: grid;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(13, 25, 38, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.record-title {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #f3f8ff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #8ddcff;
}

.record-meta {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.6);
}

.empty-icon {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  background: #0c1725;
  color: #b7cbdf;
}

.empty-icon text {
  font-size: 34rpx;
}

.empty-title {
  font-size: 30rpx;
  font-weight: 700;
}

.empty-copy {
  text-align: center;
  line-height: 1.7;
}

.empty-button {
  width: 220rpx;
}

.bottom-nav {
  position: fixed;
  left: 20rpx;
  right: 20rpx;
  bottom: 20rpx;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 22rpx 14rpx;
  border-radius: 20rpx;
  background: rgba(18, 31, 46, 0.94);
  border: 1px solid rgba(118, 189, 255, 0.08);
  box-shadow: 0 20rpx 40rpx rgba(2, 10, 18, 0.4);
  backdrop-filter: blur(18rpx);
}

.bottom-nav__item {
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 8rpx;
  color: rgba(214, 225, 239, 0.56);
  font-size: 22rpx;
}

.bottom-nav__item--active {
  color: #dff5ff;
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>