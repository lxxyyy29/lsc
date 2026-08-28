<template>
  <!-- 仅当页面处于导航栈深处（由其他页面跳转进入）时显示，避免首屏页出现无效返回 -->
  <view v-if="canGoBack" class="back-bar">
    <view class="back-btn" @click="goBack">
      <text class="back-arrow">‹</text>
      <text class="back-text">返回</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'

const canGoBack = ref(false)

function refresh() {
  try {
    canGoBack.value = getCurrentPages().length > 1
  } catch {
    canGoBack.value = false
  }
}

function goBack() {
  uni.navigateBack({ delta: 1 })
}

onShow(refresh)
</script>

<style scoped>
.back-bar {
  display: flex;
  padding: 8rpx 0 16rpx;
}
.back-btn {
  display: flex;
  align-items: center;
  gap: 4rpx;
  padding: 8rpx 24rpx 8rpx 16rpx;
  border-radius: 32rpx;
  background: rgba(17, 24, 39, 0.06);
}
.back-arrow {
  font-size: 44rpx;
  line-height: 40rpx;
  color: #374151;
  font-weight: 600;
}
.back-text {
  font-size: 26rpx;
  color: #374151;
}
</style>
