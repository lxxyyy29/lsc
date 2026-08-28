<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">📋 我的上报</text>
      <text class="header-sub">查看上报记录和处理进度</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in reports" :key="item.id" class="card">
        <view class="card-top">
          <text class="title">{{ item.title }}</text>
          <text :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</text>
        </view>
        <text class="desc">{{ item.description }}</text>
        <view class="card-bottom">
          <text class="time">{{ item.createdAt }}</text>
          <text class="code">查询码：{{ item.eventCode || item.id }}</text>
        </view>
        <!-- 办结后评价 -->
        <view v-if="item.status === 'CLOSED'" class="rating-section">
          <view v-if="item.rating" class="rating-display">
            <view class="stars">
              <text v-for="n in 5" :key="n" :class="['star', n <= item.rating ? 'star-active' : '']">★</text>
            </view>
            <text class="rating-text">{{ item.ratingText || '已评价' }}</text>
          </view>
          <view v-else class="rating-action">
            <text class="rating-label">请评价：</text>
            <view class="stars-input">
              <text v-for="n in 5" :key="n" class="star-input" @click="rateItem(item, n)">★</text>
            </view>
          </view>
        </view>
      </view>
      <text v-if="!reports.length" class="empty">暂无上报记录</text>
    </view>

    <ResidentTabBar current="/pages/resident/history/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getMyReports, rateEvent } from '../../../src/api/resident'

const { statusBarPadding } = useStatusBar()

const loading = ref(false)
const reports = ref<any[]>([])

function statusLabel(status: string) {
  const map: any = {
    PENDING: '待处理',
    WAITING_DISPATCH: '待派单',
    WAITING_LEADER_REVIEW: '组长审核',
    DISPATCHED_TO_WORK_ORDER: '处理中',
    PROCESSING: '处理中',
    WAITING_CLOSE_CONFIRM: '待确认',
    CLOSED: '已办结',
    IGNORED: '已忽略',
    REJECTED: '已驳回'
  }
  return map[status] || status || '未知'
}
function statusClass(status: string) {
  if (status === 'CLOSED') return 'status-green'
  if (status === 'DISPATCHED_TO_WORK_ORDER' || status === 'PROCESSING' || status === 'WAITING_CLOSE_CONFIRM') return 'status-blue'
  if (status === 'REJECTED' || status === 'IGNORED') return 'status-red'
  return 'status-orange'
}

async function rateItem(item: any, rating: number) {
  try {
    await rateEvent(item.id, { rating, comment: '' })
    item.rating = rating
    item.ratingText = ['', '很不满意', '不满意', '一般', '满意', '非常满意'][rating]
  } catch (e: any) {
    uni.showToast({ title: e?.message || '评价失败', icon: 'none' })
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMyReports()
    reports.value = res?.items || res?.data?.items || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onLoad(() => { loadData() })
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.loading { text-align: center; padding: 80rpx; color: #9ca3af; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.title { font-size: 28rpx; font-weight: 600; flex: 1; margin-right: 16rpx; }
.status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; flex-shrink: 0; }
.status-green { background: #f6ffed; color: #52c41a; }
.status-blue { background: #e6f7ff; color: #1890ff; }
.status-orange { background: #fff7e6; color: #fa8c16; }
.status-red { background: #fff1f0; color: #ff4d4f; }
.desc { font-size: 26rpx; color: #6b7280; margin-bottom: 16rpx; display: block; line-height: 1.5; }
.card-bottom { display: flex; justify-content: space-between; font-size: 22rpx; color: #9ca3af; }
.empty { text-align: center; padding: 80rpx; color: #9ca3af; display: block; }
.rating-section { margin-top: 24rpx; padding-top: 24rpx; border-top: 1px solid #f3f4f6; }
.rating-display { display: flex; align-items: center; gap: 16rpx; }
.stars { display: flex; gap: 4rpx; }
.star { font-size: 32rpx; color: #d1d5db; }
.star-active { color: #faad14; }
.rating-text { font-size: 24rpx; color: #6b7280; }
.rating-action { display: flex; align-items: center; gap: 16rpx; }
.rating-label { font-size: 24rpx; color: #6b7280; }
.stars-input { display: flex; gap: 8rpx; }
.star-input { font-size: 48rpx; color: #d1d5db; padding: 0 4rpx; }
.star-input:active { color: #faad14; }
</style>
