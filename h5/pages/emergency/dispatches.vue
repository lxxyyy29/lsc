<template>
  <view class="page">
    <view class="page-glow"></view>

    <!-- 加载中 -->
    <view v-if="loading" class="empty-card">
      <text class="empty-title">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view v-else-if="!items.length" class="empty-card">
      <text class="empty-emoji">⚡</text>
      <text class="empty-title">暂无应急指令</text>
      <text class="empty-sub">指挥中心下达的应急调度指令会出现在这里</text>
    </view>

    <!-- 指令列表 -->
    <view v-else class="dispatch-list">
      <view
        v-for="item in items"
        :key="item.id"
        class="dispatch-card"
        :class="{ 'dispatch-card--pending': item.my_status === 'PENDING' }"
        @click="openDetail(item.id)"
      >
        <view class="dispatch-top">
          <view class="type-badge" :class="`type-badge--${item.type}`">
            <text class="type-badge-text">{{ item.type_name || item.type }}</text>
          </view>
          <view class="my-status" :class="`mstatus--${item.my_status}`">
            <text class="my-status-text">{{ myStatusName(item.my_status) }}</text>
          </view>
        </view>
        <text class="dispatch-title">{{ item.title }}</text>
        <text class="dispatch-content">{{ brief(item.content) }}</text>
        <view class="dispatch-meta">
          <text class="meta-item">级别：{{ item.level_name || item.level }}</text>
          <text class="meta-item">{{ item.grid_name || '全域' }}</text>
          <text class="meta-item">{{ formatTime(item.dispatch_time) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyEmergencyDispatches, type EmergencyDispatch } from '../../src/api/emergency'
import { navigateToPath } from '../../src/uni/navigation'

const items = ref<EmergencyDispatch[]>([])
const loading = ref(true)

const myStatusName = (s?: string) =>
  ({ PENDING: '待响应', RECEIVED: '已接收', RESPONDING: '响应中', COMPLETED: '已完成' } as Record<string, string>)[s || ''] || ''

function brief(content?: string) {
  if (!content) return ''
  return content.length > 60 ? content.slice(0, 60) + '…' : content
}

function formatTime(t?: string | null) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(5, 16)
}

async function load() {
  loading.value = true
  try {
    items.value = await getMyEmergencyDispatches()
  } catch (e) {
    console.error('加载指令列表失败', e)
  } finally {
    loading.value = false
  }
}

function openDetail(id: number) {
  navigateToPath(`/pages/emergency/detail?id=${id}`)
}

onShow(load)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4fb 0%, #f6f9fc 100%);
  padding: 20rpx 24rpx 40rpx;
  position: relative;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: -160rpx;
  right: -120rpx;
  width: 420rpx;
  height: 420rpx;
  background: radial-gradient(circle, rgba(220, 38, 38, 0.14) 0%, rgba(220, 38, 38, 0) 70%);
  border-radius: 50%;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 140rpx 40rpx;
  gap: 14rpx;
}

.empty-emoji { font-size: 72rpx; }

.empty-title { font-size: 30rpx; font-weight: 600; color: #334155; }

.empty-sub { font-size: 24rpx; color: #94a3b8; text-align: center; }

.dispatch-list { display: flex; flex-direction: column; gap: 18rpx; }

.dispatch-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.dispatch-card--pending {
  border: 2rpx solid #fca5a5;
  background: #fff5f5;
}

.dispatch-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-badge {
  padding: 5rpx 18rpx;
  border-radius: 999rpx;
}

.type-badge--RAIN { background: #e0f2fe; }
.type-badge--FIRE { background: #fee2e2; }
.type-badge--MASS { background: #fef3c7; }
.type-badge--OTHER { background: #f1f5f9; }

.type-badge-text { font-size: 21rpx; font-weight: 600; }

.type-badge--RAIN .type-badge-text { color: #0369a1; }
.type-badge--FIRE .type-badge-text { color: #b91c1c; }
.type-badge--MASS .type-badge-text { color: #b45309; }
.type-badge--OTHER .type-badge-text { color: #475569; }

.my-status {
  padding: 5rpx 18rpx;
  border-radius: 999rpx;
  background: #f1f5f9;
}

.mstatus--RECEIVED { background: #fef3c7; }
.mstatus--RESPONDING { background: #e0f2fe; }
.mstatus--COMPLETED { background: #dcfce7; }
.mstatus--PENDING { background: #fee2e2; }

.my-status-text { font-size: 21rpx; font-weight: 600; color: #64748b; }
.mstatus--RECEIVED .my-status-text { color: #b45309; }
.mstatus--RESPONDING .my-status-text { color: #0369a1; }
.mstatus--COMPLETED .my-status-text { color: #15803d; }
.mstatus--PENDING .my-status-text { color: #b91c1c; }

.dispatch-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.4;
}

.dispatch-content {
  font-size: 24rpx;
  color: #475569;
  line-height: 1.6;
}

.dispatch-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8rpx 20rpx;
  padding-top: 10rpx;
  border-top: 1rpx solid #f1f5f9;
}

.meta-item { font-size: 22rpx; color: #94a3b8; }
</style>
