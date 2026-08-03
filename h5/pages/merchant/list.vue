<template>
  <view class="page">
    <view class="empty-state">
      <text class="empty-icon">🏪</text>
      <text class="empty-title">商户管理</text>
      <text class="empty-copy">功能开发中，敬请期待</text>
    </view>
  </view>
</template>

<script setup lang="ts">
</script>

<style scoped>
.page { min-height: 100vh; background: #f5f7fa; }
.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding-top: 200rpx;
}
.empty-icon { font-size: 80rpx; margin-bottom: 20rpx; }
.empty-title { font-size: 36rpx; font-weight: 600; color: #1f2937; margin-bottom: 12rpx; }
.empty-copy { font-size: 26rpx; color: #9ca3af; }
</style>
