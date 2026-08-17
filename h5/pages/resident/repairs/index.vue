<template>
  <view class="resident-repairs-page" :style="{ paddingTop: statusBarPadding }">
    <view class="header">
      <text class="header-title">🔧 便民报修</text>
      <text class="header-sub">提交报修申请，查看处理进度</text>
    </view>

    <!-- 提交报修按钮 -->
    <button @click="goForm" class="btn-submit">+ 提交报修</button>

    <!-- 我的报修列表 -->
    <view class="section-title">我的报修</view>
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="r in repairs" :key="r.id" class="card">
        <view class="card-top">
          <text class="title">{{ r.title }}</text>
          <text :class="['status', statusClass(r.status)]">{{ statusLabel(r.status) }}</text>
        </view>
        <text class="desc">{{ r.description }}</text>
        <view class="card-bottom">
          <text class="type">{{ repairTypeLabel(r.repairType) }}</text>
          <text class="time">{{ r.createdAt }}</text>
        </view>
        <view v-if="r.handleResult" class="result">
          <text class="result-label">处理结果：</text>{{ r.handleResult }}
        </view>
      </view>
      <text v-if="!repairs.length" class="empty">暂无报修记录</text>
    </view>

    <ResidentTabBar current="/pages/resident/repairs/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getMyRepairs } from '../../../src/api/resident'
import { useStatusBar } from '../../../src/utils/useStatusBar'

const { statusBarPadding } = useStatusBar()

const loading = ref(false)
const repairs = ref<any[]>([])

function goForm() {
  uni.navigateTo({ url: '/pages/resident/repairs/form' })
}

function statusLabel(status: string) {
  const map: any = { PENDING: '待处理', ASSIGNED: '已派单', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回' }
  return map[status] || status
}
function statusClass(status: string) {
  if (status === 'COMPLETED') return 'status-green'
  if (status === 'ASSIGNED' || status === 'PROCESSING') return 'status-blue'
  if (status === 'REJECTED') return 'status-red'
  return 'status-orange'
}
function repairTypeLabel(t: string) {
  const map: any = { WATER: '水电', ELEVATOR: '电梯', DOOR: '门禁', PIPE: '管道', ROOF: '屋面', OTHER: '其他' }
  return map[t] || t
}

async function loadData() {
  loading.value = true
  try {
    repairs.value = await getMyRepairs() || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onLoad(loadData)
</script>

<style>
/* 居民端-便民报修页面 全局样式 */
.resident-repairs-page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.resident-repairs-page .header {
  background: linear-gradient(135deg, #fa541c 0%, #d4380d 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.resident-repairs-page .header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; color: #fff; }
.resident-repairs-page .header-sub { font-size: 26rpx; opacity: 0.8; color: #fff; }
.resident-repairs-page .btn-submit {
  width: 100%; height: 88rpx; line-height: 88rpx; padding: 0; background: #fa541c; color: #fff; border: none;
  border-radius: 12rpx; font-size: 30rpx; font-weight: 600; margin-bottom: 32rpx;
}
.resident-repairs-page .section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; color: #1f2937; }
.resident-repairs-page .card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.resident-repairs-page .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.resident-repairs-page .title { font-size: 28rpx; font-weight: 600; flex: 1; margin-right: 16rpx; color: #111827; }
.resident-repairs-page .status { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; flex-shrink: 0; }
.resident-repairs-page .status-green { background: #f6ffed; color: #52c41a; }
.resident-repairs-page .status-blue { background: #e6f7ff; color: #1890ff; }
.resident-repairs-page .status-orange { background: #fff7e6; color: #fa8c1c; }
.resident-repairs-page .status-red { background: #fff1f0; color: #ff4d4f; }
.resident-repairs-page .desc { font-size: 26rpx; color: #4b5563; margin-bottom: 16rpx; display: block; line-height: 1.5; }
.resident-repairs-page .card-bottom { display: flex; justify-content: space-between; font-size: 22rpx; color: #6b7280; }
.resident-repairs-page .result { margin-top: 20rpx; padding: 16rpx; background: #f6ffed; border-radius: 12rpx; font-size: 24rpx; color: #389e0d; }
.resident-repairs-page .result-label { font-weight: 600; }
.resident-repairs-page .loading { text-align: center; padding: 40rpx; color: #6b7280; }
.resident-repairs-page .empty { text-align: center; padding: 40rpx; color: #6b7280; font-size: 26rpx; display: block; }
</style>
