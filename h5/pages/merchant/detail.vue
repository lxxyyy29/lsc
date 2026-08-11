<template>
  <view class="page">
    <view class="empty-state">
      <text class="empty-icon">📋</text>
      <text class="empty-title">商户详情</text>
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

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
