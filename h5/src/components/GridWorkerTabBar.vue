<template>
  <!-- #ifndef MP-WEIXIN -->
  <!-- 小程序端使用原生 tabBar（pages.json 中配置），此处仅 H5 渲染 -->
  <view v-if="renderTabBar" class="grid-worker-tab-bar">
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
  <!-- #endif -->
</template>

<script setup lang="ts">
import { computed } from 'vue'

// 运行时双保险：小程序端即使模板条件编译失效也绝不渲染（小程序用原生 tabBar）
let isMpWeixin = false
// #ifdef MP-WEIXIN
isMpWeixin = true
// #endif

const props = defineProps<{
  current: string
}>()

const items = [
  { path: '/pages/workbench/index', icon: '🏠', label: '工作台' },
  { path: '/pages/map/index', icon: '🗺️', label: '地图' },
  { path: '/pages/patrol/checkin', icon: '🚶', label: '巡查' },
  { path: '/pages/mine/index', icon: '👤', label: '我的' }
]

const renderTabBar = computed(() => !isMpWeixin)

const isActive = (path: string) => props.current === path

function go(path: string) {
  if (path === props.current) return
  uni.reLaunch({ url: path })
}
</script>

<style>
/* 网格员端深色主题底部导航 */
.grid-worker-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 110rpx;
  background: rgba(6, 18, 31, 0.97);
  border-top: 1px solid rgba(125, 163, 220, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 1000;
  padding-bottom: env(safe-area-inset-bottom);
  box-sizing: content-box;
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  color: #8ba1b4;
  font-size: 22rpx;
}
.tab-item.active {
  color: #5ea2ff;
}
.tab-icon {
  font-size: 40rpx;
  line-height: 1;
}
</style>
