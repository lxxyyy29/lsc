<template>
  <view class="grid-worker-tab-bar">
    <view
      v-for="item in items"
      :key="item.path"
      class="tab-item"
      :class="{ active: isActive(item.path) }"
      @click="go(item.path)"
    >
      <text class="tab-icon">{{ item.icon }}</text>
      <text class="tab-label">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  current: string
}>()

const items = [
  { path: '/pages/workbench/index', icon: '🏠', label: '工作台' },
  { path: '/pages/map/index', icon: '🗺️', label: '地图' },
  { path: '/pages/patrol/checkin', icon: '🚶', label: '巡查' },
  { path: '/pages/mine/index', icon: '👤', label: '我的' }
]

const isActive = (path: string) => props.current === path

function go(path: string) {
  if (path === props.current) return
  uni.reLaunch({ url: path })
}
</script>

<style>
.grid-worker-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: #ffffff;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 1000;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  color: #9ca3af;
  font-size: 22rpx;
}
.tab-item.active {
  color: #1890ff;
}
.tab-icon {
  font-size: 40rpx;
  line-height: 1;
}
</style>
