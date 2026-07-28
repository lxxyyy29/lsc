<template>
  <div v-if="session?.token">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <div style="display:flex;align-items:center;">
        <i class="fas fa-building" style="color:#1890FF;font-size:20px;margin-right:10px;"></i>
        <h1 style="font-size:16px;font-weight:600;">拔蛟窝社区小网格综合治理系统</h1>
      </div>
      <div style="display:flex;align-items:center;gap:16px;">
        <span style="font-size:12px;color:#9ca3af;">端口: 8080</span>
        <span style="font-size:12px;color:#9ca3af;">{{ session?.userName || '管理员' }}</span>
        <div style="width:32px;height:32px;background:#1890FF;color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:13px;">{{ session?.userName?.slice(0,1) || '管' }}</div>
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
    <aside class="sidebar">
      <div style="padding:16px 0;">
        <div style="padding:0 16px 12px;margin-bottom:8px;border-bottom:1px solid #f3f4f6;">
          <span style="font-size:11px;color:#9ca3af;">治理功能</span>
        </div>
        <router-link v-for="item in menuItems" :key="item.path" :to="item.path" class="sidebar-link" :class="{ active: currentPath === item.path }">
          <i :class="item.icon"></i>
          <span>{{ item.name }}</span>
        </router-link>
      </div>
    </aside>

    <!-- 主内容 -->
    <main class="main-content">
      <router-view />
    </main>
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

// Web 管理端菜单（全部功能）
const menuItems = [
  { path: '/', name: '全域态势看板', icon: 'fas fa-tachometer-alt' },
  { path: '/gis', name: 'GIS网格可视化', icon: 'fas fa-map-marked-alt' },
  { path: '/population', name: '实有人口库', icon: 'fas fa-users' },
  { path: '/buildings', name: '房屋/出租屋库', icon: 'fas fa-home' },
  { path: '/places', name: '场所资源库', icon: 'fas fa-store' },
  { path: '/events', name: '事件闭环处置', icon: 'fas fa-tasks' },
  { path: '/ledger', name: '场所台账', icon: 'fas fa-clipboard-list' },
  { path: '/patrol', name: '网格巡查', icon: 'fas fa-shoe-prints' },
  { path: '/safety', name: '安全防控', icon: 'fas fa-shield-alt' },
  { path: '/party', name: '智慧党建', icon: 'fas fa-flag' },
  { path: '/parking', name: '停车管理', icon: 'fas fa-parking' },
  { path: '/drones', name: '设备管理', icon: 'fas fa-helicopter' },
  { path: '/work-orders', name: '工单中心', icon: 'fas fa-clipboard-list' },
  { path: '/audits', name: '审核中心', icon: 'fas fa-gavel' },
  { path: '/processes', name: '流程中心', icon: 'fas fa-project-diagram' },
  { path: '/reports', name: '数据报表', icon: 'fas fa-chart-bar' },
  { path: '/ai-models', name: 'AI 模型', icon: 'fas fa-brain' },
  { path: '/assessment', name: '考核研判', icon: 'fas fa-chart-line' },
  { path: '/integration', name: '信息互通', icon: 'fas fa-plug' },
]

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
