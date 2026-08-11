<template>
  <!-- #ifndef MP-WEIXIN -->
  <!-- 小程序端使用原生 tabBar（见构建脚本生成的 pages.json），此处仅 H5 渲染 -->
  <view v-if="renderTabBar" class="resident-tab-bar">
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
// #ifdef MP-WEIXIN
const isMpWeixin = true
// #endif
// #ifndef MP-WEIXIN
const isMpWeixin = false
// #endif

const props = defineProps<{
  current: string
}>()

const items = [
  { path: '/pages/resident/report/index', icon: '📸', label: '随手拍' },
  { path: '/pages/resident/history/index', icon: '📋', label: '我的上报' },
  { path: '/pages/resident/services/index', icon: '🏠', label: '服务' },
  { path: '/pages/resident/mine/index', icon: '👤', label: '我的' }
]

// 服务子页面（活动/报修/政策/积分）时也高亮"服务"
const isActive = (path: string) => {
  if (path === '/pages/resident/services/index') {
    return props.current.startsWith('/pages/resident/services') || props.current === '/pages/resident/activities/index'
      || props.current === '/pages/resident/repairs/index' || props.current === '/pages/resident/policies/index'
      || props.current === '/pages/resident/points/index'
  }
  return props.current === path
}

const renderTabBar = computed(() => !isMpWeixin && visible.value)

const visible = computed(() => {
  const c = props.current
  return c === '/pages/resident/report/index'
    || c === '/pages/resident/history/index'
    || c === '/pages/resident/mine/index'
    || c.startsWith('/pages/resident/services')
    || c === '/pages/resident/activities/index'
    || c === '/pages/resident/repairs/index'
    || c === '/pages/resident/policies/index'
    || c === '/pages/resident/points/index'
})

function go(path: string) {
  uni.reLaunch({ url: path })
}

</script>

<style scoped>
.resident-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
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
