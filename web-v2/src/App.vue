<template>
  <div v-if="session?.token" style="height:100vh;display:flex;flex-direction:column;">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <h1><i class="fas fa-building"></i>网格社区治理平台</h1>
      <div style="display:flex;align-items:center;gap:16px;">
        <span style="font-size:12px;color:#9ca3af;">{{ session?.userName || '管理员' }}</span>
        <div style="width:32px;height:32px;background:#0284c7;color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;">{{ session?.userName?.slice(0,1) || '管' }}</div>
        <div class="user-menu-wrapper" @click.stop="showUserMenu = !showUserMenu">
          <i class="fas fa-chevron-down" style="font-size:10px;color:#9ca3af;cursor:pointer;"></i>
          <div v-if="showUserMenu" class="user-dropdown" @click.stop>
            <div class="dropdown-header">
              <div class="dropdown-avatar">{{ session?.userName?.slice(0,1) || '管' }}</div>
              <div class="dropdown-info">
                <div class="dropdown-name">{{ session?.userName || '管理员' }}</div>
                <div class="dropdown-role">{{ session?.roleCodes?.join(', ') || 'SUPER_ADMIN' }}</div>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item" @click="handleLogout">
              <i class="fas fa-sign-out-alt"></i>
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- 侧边栏 -->
    <div style="display:flex;flex:1;overflow:hidden;">
      <aside class="sidebar">
        <div style="padding:16px 0;">
          <div style="padding:0 16px 12px;margin-bottom:8px;border-bottom:1px solid #f3f4f6;">
            <span style="font-size:11px;color:#9ca3af;font-weight:600;">功能导航</span>
          </div>
          <div v-for="group in menuGroups" :key="group.name" class="menu-group">
            <div class="group-header" @click="toggleGroup(group.name)">
              <i :class="group.icon" style="color:#0284c7;font-size:14px;width:20px;text-align:center;"></i>
              <span style="flex:1;">{{ group.name }}</span>
              <i class="fas fa-chevron-down" style="font-size:10px;color:#9ca3af;transition:transform 0.2s;" :style="{ transform: openGroups.includes(group.name) ? 'rotate(0deg)' : 'rotate(-90deg)' }"></i>
            </div>
            <div v-show="openGroups.includes(group.name)" class="group-items">
              <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="sidebar-link sub-item" :class="{ active: currentPath === item.path }">
                <span style="width:6px;height:6px;border-radius:50%;background:#d1d5db;flex-shrink:0;" :style="currentPath === item.path ? { background: '#0284c7' } : {}"></span>
                <span>{{ item.name }}</span>
              </router-link>
            </div>
          </div>
        </div>
      </aside>

      <!-- 主内容 -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
  <LoginView v-else @success="onLogin" />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSession, login, logout } from './api'
import LoginView from './views/LoginView.vue'

const session = ref(getSession())
const route = useRoute()
const currentPath = computed(() => route.path)
const showUserMenu = ref(false)

// 分组菜单（大类 → 子功能）
const menuGroups = [
  {
    name: '首页概览', icon: 'fas fa-tachometer-alt',
    items: [
      { path: '/', name: '全域态势看板' },
    ]
  },
  {
    name: '事件中心', icon: 'fas fa-tasks',
    items: [
      { path: '/events', name: '事件闭环处置' },
      { path: '/events/create', name: '创建事件' },
      { path: '/work-orders', name: '工单中心' },
    ]
  },
  {
    name: '网格治理', icon: 'fas fa-map-marked-alt',
    items: [
      { path: '/gis', name: 'GIS网格可视化' },
      { path: '/population', name: '实有人口库' },
      { path: '/buildings', name: '房屋/出租屋库' },
      { path: '/places', name: '场所资源库' },
      { path: '/org-members', name: '组织人员' },
      { path: '/resident-reports', name: '居民上报' },
    ]
  },
  {
    name: '巡查防控', icon: 'fas fa-shield-alt',
    items: [
      { path: '/patrol', name: '网格巡查' },
      { path: '/safety', name: '安全防控' },
      { path: '/parking', name: '停车管理' },
    ]
  },
  {
    name: '智能应用', icon: 'fas fa-helicopter',
    items: [
      { path: '/drones', name: '无人机管理' },
      { path: '/ai-models', name: 'AI 模型' },
      { path: '/qwen-models', name: '通义模型' },
      { path: '/integration', name: '信息互通' },
    ]
  },
  {
    name: '党建治理', icon: 'fas fa-flag',
    items: [
      { path: '/party', name: '智慧党建' },
    ]
  },
  {
    name: '业务管理', icon: 'fas fa-store',
    items: [
      { path: '/biz-areas', name: '辖区管理' },
      { path: '/ledger', name: '场所台账' },
    ]
  },
  {
    name: '数据决策', icon: 'fas fa-chart-bar',
    items: [
      { path: '/reports', name: '数据报表' },
      { path: '/assessment', name: '考核研判' },
      { path: '/audit-logs', name: '审计日志' },
    ]
  },
]

// 展开的菜单组（默认全部展开）
const openGroups = ref<string[]>(menuGroups.map(g => g.name))

function toggleGroup(name: string) {
  const idx = openGroups.value.indexOf(name)
  if (idx >= 0) {
    openGroups.value.splice(idx, 1)
  } else {
    openGroups.value.push(name)
  }
}

async function onLogin() {
  session.value = getSession()
}

function handleLogout() {
  logout()
  session.value = null
  showUserMenu.value = false
  window.location.hash = '#/login'
}

function closeUserMenu() {
  showUserMenu.value = false
}

// 点击外部关闭下拉菜单
onMounted(() => {
  document.addEventListener('click', closeUserMenu)
})
</script>

<style scoped>
/* === 分组菜单 === */
.menu-group {
  margin-bottom: 2px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}

.group-header:hover {
  background: #f9fafb;
}

.group-items {
  padding: 4px 0 4px 16px;
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 500px; }
}

.sub-item {
  padding-left: 16px !important;
  font-size: 12px !important;
}

/* === 用户菜单下拉 === */
.user-menu-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 220px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid #f1f5f9;
  z-index: 1000;
  animation: dropdownIn 0.15s ease-out;
  overflow: hidden;
}

@keyframes dropdownIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
}

.dropdown-avatar {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.dropdown-info {
  overflow: hidden;
}

.dropdown-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-role {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: background 0.15s;
}

.dropdown-item:hover {
  background: #f8fafc;
  color: #ef4444;
}

.dropdown-item i {
  font-size: 14px;
  width: 16px;
  text-align: center;
}
</style>
