<template>
  <view class="page">
    <view class="header">
      <view class="avatar">{{ session?.userName?.slice(0, 1) || '用' }}</view>
      <view class="info">
        <text class="name">{{ session?.userName || '用户' }}</text>
        <text class="role">普通群众</text>
      </view>
    </view>

    <view class="stats">
      <view class="stat-item">
        <text class="num">{{ totalReports }}</text>
        <text class="label">总上报</text>
      </view>
      <view class="stat-item">
        <text class="num">{{ processingCount }}</text>
        <text class="label">处理中</text>
      </view>
      <view class="stat-item">
        <text class="num">{{ completedCount }}</text>
        <text class="label">已办结</text>
      </view>
    </view>

    <view class="menu">
      <view class="menu-item" @click="goHistory">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的上报</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="goGridWorker">
        <text class="menu-icon">👷</text>
        <text class="menu-text">网格员入口</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="showAbout = true">
        <text class="menu-icon">ℹ️</text>
        <text class="menu-text">关于平台</text>
        <text class="arrow">›</text>
      </view>
      <view class="menu-item" @click="handleLogout">
        <text class="menu-icon">🚪</text>
        <text class="menu-text">退出登录</text>
        <text class="arrow">›</text>
      </view>
    </view>

    <!-- 关于弹窗 -->
    <view v-if="showAbout" class="mask" @click="showAbout = false">
      <view class="dialog" @click.stop>
        <text class="dialog-title">关于平台</text>
        <text class="dialog-text">拔蛟窝智慧网格治理平台</text>
        <text class="dialog-text">居民服务端 v1.0</text>
        <text class="dialog-text">发现身边问题，一键上报，共建美好社区</text>
        <button @click="showAbout = false" class="btn-close">确定</button>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/mine/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getResidentSession, clearResidentSession, getMyReports } from '../../../src/api/resident'

const session = ref(getResidentSession())
const showAbout = ref(false)
const totalReports = ref(0)
const processingCount = ref(0)
const completedCount = ref(0)

function goHistory() {
  uni.reLaunch({ url: '/pages/resident/history/index' })
}

function goGridWorker() {
  uni.reLaunch({ url: '/pages/role-select/index' })
}

function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定退出登录吗？',
    success: (res) => {
      if (!res.confirm) return
      clearResidentSession()
      uni.reLaunch({ url: '/pages/role-select/index' })
    }
  })
}

async function loadStats() {
  try {
    const res: any = await getMyReports()
    const items = res?.items || res?.data?.items || []
    totalReports.value = items.length
    processingCount.value = items.filter((i: any) => i.status === 'PROCESSING').length
    completedCount.value = items.filter((i: any) => i.status === 'CLOSED').length
  } catch (e) {
    console.error(e)
  }
}

onLoad(() => { loadStats() })
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 24rpx; padding: 48rpx 40rpx; color: #fff;
  display: flex; align-items: center; gap: 32rpx; margin-bottom: 32rpx;
}
.avatar {
  width: 112rpx; height: 112rpx; background: rgba(255,255,255,0.2); border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 48rpx; font-weight: 600;
}
.info { display: flex; flex-direction: column; }
.name { font-size: 36rpx; font-weight: 600; }
.role { font-size: 24rpx; opacity: 0.8; margin-top: 8rpx; }
.stats { display: flex; gap: 24rpx; margin-bottom: 32rpx; }
.stat-item {
  flex: 1; background: #fff; border-radius: 24rpx; padding: 32rpx; text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); display: flex; flex-direction: column;
}
.stat-item .num { font-size: 48rpx; font-weight: 700; color: #1890ff; }
.stat-item .label { font-size: 24rpx; color: #6b7280; margin-top: 8rpx; }
.menu {
  background: #fff; border-radius: 24rpx; overflow: hidden;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.menu-item {
  display: flex; align-items: center; gap: 24rpx; padding: 32rpx;
  border-bottom: 1px solid #f3f4f6; color: #374151; font-size: 28rpx;
}
.menu-item:last-child { border-bottom: none; }
.menu-icon { font-size: 36rpx; width: 48rpx; text-align: center; }
.menu-text { flex: 1; }
.arrow { color: #d1d5db; font-size: 36rpx; }
.mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog {
  background: #fff; border-radius: 32rpx; padding: 48rpx; width: 80%; max-width: 640rpx;
  text-align: center; display: flex; flex-direction: column;
}
.dialog-title { font-size: 32rpx; font-weight: 600; margin-bottom: 24rpx; }
.dialog-text { font-size: 26rpx; color: #6b7280; margin-bottom: 16rpx; }
.btn-close {
  margin-top: 32rpx; padding: 16rpx 48rpx; background: #1890ff; color: #fff;
  border: none; border-radius: 12rpx; font-size: 28rpx; line-height: 1.5;
}
</style>
