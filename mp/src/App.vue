<template>
  <div>
    <router-view />
    <!-- 底部导航 -->
    <nav v-if="showTabBar" class="tab-bar">
      <router-link to="/report" class="tab-item" :class="{ active: $route.path === '/report' }">
        <i class="fas fa-camera"></i>
        <span>随手拍</span>
      </router-link>
      <router-link to="/history" class="tab-item" :class="{ active: $route.path === '/history' }">
        <i class="fas fa-list"></i>
        <span>我的上报</span>
      </router-link>
      <router-link to="/services" class="tab-item" :class="{ active: $route.path.startsWith('/services') }">
        <i class="fas fa-th-large"></i>
        <span>服务</span>
      </router-link>
      <router-link to="/mine" class="tab-item" :class="{ active: $route.path === '/mine' }">
        <i class="fas fa-user"></i>
        <span>我的</span>
      </router-link>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const showTabBar = computed(() => {
  return ['/report', '/history', '/mine'].includes(route.path) || route.path.startsWith('/services')
})
</script>

<style scoped>
.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-around;
  z-index: 100;
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  text-decoration: none;
  color: #9ca3af;
  font-size: 11px;
  padding: 8px 16px;
}
.tab-item i {
  font-size: 20px;
}
.tab-item.active {
  color: #1890ff;
}
</style>
