<template>
  <view class="page">
    <!-- 顶部标题栏 -->
    <view class="top-bar">
      <view class="back-btn" @click="goBack">‹</view>
      <text class="top-title">应急公告</text>
    </view>

    <view class="header">
      <text class="header-title">📢 应急公告</text>
      <text class="header-sub">社区应急调度信息，请关注并配合</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in notices" :key="item.id" class="card" @click="openDetail(item)">
        <view class="card-top">
          <text class="badge-type">{{ item.type_name }}</text>
          <text :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</text>
        </view>
        <text class="title">{{ item.title }}</text>
        <text class="desc">{{ shortContent(item.content) }}</text>
        <view class="card-bottom">
          <text class="time">{{ item.dispatch_time || '' }}</text>
          <text class="progress">已响应 {{ item.responded_count }}/{{ item.receiver_count }} 人</text>
        </view>
      </view>
      <view v-if="!notices.length" class="empty">暂无应急公告</view>
    </view>

    <!-- 详情弹窗 -->
    <view v-if="detail" class="mask" @click="detail = null">
      <view class="dialog" @click.stop>
        <text class="dialog-title">{{ detail.title }}</text>
        <view class="dialog-tags">
          <text class="badge-type">{{ detail.type_name }}</text>
          <text class="badge-level">{{ detail.level_name }}</text>
          <text :class="['status', statusClass(detail.status)]">{{ statusLabel(detail.status) }}</text>
        </view>
        <text class="dialog-content">{{ detail.content }}</text>
        <view class="dialog-meta">
          <text class="meta-line">发起人：{{ detail.creator_name }} · 编号 {{ detail.dispatch_no }}</text>
          <text class="meta-line">下发时间：{{ detail.dispatch_time || '' }}</text>
          <text class="meta-line">响应进度：{{ detail.responded_count }}/{{ detail.receiver_count }} 人已响应</text>
        </view>
        <button class="btn-close" @click="detail = null">我知道了</button>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/mine/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getEmergencyNotices, getEmergencyNoticeDetail } from '../../../src/api/resident'

const loading = ref(false)
const notices = ref<any[]>([])
const detail = ref<any>(null)

function statusLabel(status: string) {
  const map: any = {
    DISPATCHED: '已下达',
    RESPONDING: '响应中',
    COMPLETED: '已结束'
  }
  return map[status] || status
}
function statusClass(status: string) {
  const map: any = {
    DISPATCHED: 'status-pending',
    RESPONDING: 'status-processing',
    COMPLETED: 'status-closed'
  }
  return map[status] || 'status-pending'
}
function shortContent(content: string) {
  if (!content) return ''
  return content.length > 60 ? content.slice(0, 60) + '...' : content
}

function goBack() {
  const pages = getCurrentPages()
  if (pages.length > 1) {
    uni.navigateBack()
  } else {
    uni.reLaunch({ url: '/pages/resident/mine/index' })
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await getEmergencyNotices()
    notices.value = res?.items || res?.data?.items || []
  } catch (e) {
    console.warn('应急公告加载失败:', e)
  } finally {
    loading.value = false
  }
}

async function openDetail(item: any) {
  try {
    detail.value = await getEmergencyNoticeDetail(item.id)
  } catch (e) {
    console.warn('公告详情加载失败:', e)
    detail.value = item
  }
}

onLoad(() => { load() })
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; padding-bottom: 160rpx; }
.top-bar {
  display: flex; align-items: center; height: 88rpx; padding: 0 24rpx;
  background: #d9363e; color: #fff; position: sticky; top: 0; z-index: 10;
}
.back-btn { font-size: 52rpx; width: 64rpx; line-height: 88rpx; }
.top-title { flex: 1; text-align: center; font-size: 34rpx; font-weight: 600; margin-right: 64rpx; }
.header { padding: 32rpx 32rpx 24rpx; background: linear-gradient(135deg, #d9363e, #a61b24); color: #fff; }
.header-title { display: block; font-size: 40rpx; font-weight: 700; }
.header-sub { display: block; font-size: 24rpx; opacity: 0.9; margin-top: 8rpx; }
.loading, .empty { padding: 80rpx 0; text-align: center; color: #999; font-size: 28rpx; }
.card {
  margin: 24rpx 32rpx; padding: 28rpx; background: #fff; border-radius: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.05);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.badge-type { display: inline-block; padding: 4rpx 16rpx; font-size: 22rpx; border-radius: 20rpx; background: #fdecea; color: #d9363e; }
.badge-level { display: inline-block; padding: 4rpx 16rpx; font-size: 22rpx; border-radius: 20rpx; background: #eef2ff; color: #4f6bf0; }
.title { display: block; margin: 8rpx 0; font-size: 30rpx; font-weight: 600; color: #222; }
.desc { display: block; font-size: 26rpx; color: #666; line-height: 1.5; margin-bottom: 12rpx; }
.card-bottom { display: flex; justify-content: space-between; align-items: center; }
.time { font-size: 22rpx; color: #999; }
.progress { font-size: 22rpx; color: #4f6bf0; }
.status { padding: 4rpx 16rpx; font-size: 22rpx; border-radius: 20rpx; white-space: nowrap; }
.status-pending { background: #fff3e0; color: #f59e0b; }
.status-processing { background: #e8f4fd; color: #0284c7; }
.status-closed { background: #e8f5e9; color: #2e7d32; }
.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 100;
  display: flex; align-items: center; justify-content: center; padding: 48rpx;
}
.dialog { width: 100%; max-width: 640rpx; background: #fff; border-radius: 28rpx; padding: 40rpx; }
.dialog-title { display: block; font-size: 34rpx; font-weight: 700; color: #222; margin-bottom: 16rpx; }
.dialog-tags { display: flex; gap: 12rpx; flex-wrap: wrap; margin-bottom: 20rpx; }
.dialog-content { display: block; font-size: 28rpx; color: #444; line-height: 1.7; white-space: pre-wrap; margin-bottom: 20rpx; }
.dialog-meta { background: #f7f8fa; border-radius: 16rpx; padding: 20rpx 24rpx; }
.meta-line { display: block; font-size: 24rpx; color: #666; margin: 6rpx 0; }
.btn-close {
  width: 100%; margin-top: 28rpx; padding: 20rpx 0; border-radius: 20rpx;
  background: #d9363e; color: #fff; font-size: 28rpx;
}
.btn-close::after { border: none; }
</style>
