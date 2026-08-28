<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">🏠 便民服务</text>
      <text class="header-sub">便民报修 / 政策查询</text>
    </view>

    <view class="service-grid">
      <view class="service-item" @click="goToRepair">
        <text class="service-icon">🔧</text>
        <text class="service-name">便民报修</text>
      </view>
      <view class="service-item" @click="goToPolicies">
        <text class="service-icon">📋</text>
        <text class="service-name">政策查询</text>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/services/index" />
  </view>
</template>

<script setup lang="ts">
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'

const { statusBarPadding } = useStatusBar()

function goToRepair() { uni.navigateTo({ url: '/pages/resident/repairs/index' }) }
function goToPolicies() { uni.navigateTo({ url: '/pages/resident/policies/index' }) }
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.service-grid { display: flex; flex-wrap: wrap; gap: 24rpx; margin-bottom: 40rpx; }
.service-item {
  display: flex; flex-direction: column; align-items: center; gap: 12rpx;
  padding: 32rpx 16rpx; background: #fff; border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); width: calc((100% - 72rpx) / 2);
  box-sizing: border-box;
}
.service-icon { font-size: 56rpx; }
.service-name { font-size: 24rpx; color: #374151; }
</style>
