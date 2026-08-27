<template>
  <div style="height:100vh;display:flex;flex-direction:column;">
    <!-- 顶部导航 -->
    <nav class="top-nav">
      <h1><img :src="logoSvg" alt="logo" class="brand-logo" />东莞杰瑞智慧网格治理平台</h1>
      <div style="display:flex;align-items:center;gap:16px;">
        <router-link to="/help" style="font-size:16px;color:#6b7280;text-decoration:none;padding:6px;" title="帮助中心">
          <i class="fas fa-question-circle"></i>
        </router-link>
        <NotificationBell />
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
            <div class="dropdown-item" @click="openPwdModal">
              <i class="fas fa-key"></i>
              <span>修改密码</span>
            </div>
            <div class="dropdown-item" @click="handleLogout">
              <i class="fas fa-sign-out-alt"></i>
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </nav>

    <!-- 修改密码弹窗 -->
    <div v-if="showPwdModal" class="pwd-modal-overlay" @click.self="showPwdModal = false">
      <div class="pwd-modal">
        <h3 style="font-size:16px;font-weight:600;margin:0 0 16px;">修改密码</h3>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <input v-model="pwdForm.oldPassword" type="password" class="pwd-input" placeholder="当前密码" autocomplete="current-password" />
          <input v-model="pwdForm.newPassword" type="password" class="pwd-input" placeholder="新密码（6-64 位）" autocomplete="new-password" />
          <input v-model="pwdForm.confirm" type="password" class="pwd-input" placeholder="再次输入新密码" autocomplete="new-password" />
          <p v-if="pwdError" style="color:#ef4444;font-size:12px;margin:0;">{{ pwdError }}</p>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px;">
          <button @click="showPwdModal = false" class="pwd-btn">取消</button>
          <button @click="submitPwdChange" :disabled="pwdLoading" class="pwd-btn pwd-btn-primary">
            {{ pwdLoading ? '提交中...' : '确认修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 侧边栏 + 主内容 -->
    <div style="display:flex;flex:1;overflow:hidden;">
      <aside class="sidebar">
        <div style="padding:16px 0;">
          <div style="padding:0 16px 10px;">
            <span style="font-size:11px;color:#9ca3af;font-weight:600;">功能导航</span>
          </div>
          <div style="padding:0 12px 10px;">
            <div class="menu-search">
              <i class="fas fa-search" style="font-size:11px;color:#9ca3af;"></i>
              <input v-model="searchKey" placeholder="搜索功能..." style="flex:1;border:none;outline:none;font-size:12px;background:transparent;color:#1e293b;min-width:0;" />
              <i v-if="searchKey" class="fas fa-times-circle" style="font-size:11px;color:#cbd5e1;cursor:pointer;" @click="searchKey = ''"></i>
            </div>
          </div>
          <div v-if="filteredGroups.length === 0" style="padding:28px 16px;text-align:center;font-size:12px;color:#9ca3af;">未找到匹配的功能</div>
          <div v-for="group in filteredGroups" :key="group.name" class="menu-group">
            <div class="group-header" @click="toggleGroup(group.name)">
              <i :class="group.icon" style="color:#0284c7;font-size:14px;width:20px;text-align:center;"></i>
              <span style="flex:1;">{{ group.name }}</span>
              <i class="fas fa-chevron-down" style="font-size:10px;color:#9ca3af;transition:transform 0.2s;" :style="{ transform: openGroups.includes(group.name) ? 'rotate(0deg)' : 'rotate(-90deg)' }"></i>
            </div>
            <div v-show="openGroups.includes(group.name) || searchKey" class="group-items">
              <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="sidebar-link sub-item" :class="{ active: currentPath === item.path }">
                <span style="width:6px;height:6px;border-radius:50%;background:#d1d5db;flex-shrink:0;" :style="currentPath === item.path ? { background: '#0284c7' } : {}"></span>
                <span style="flex:1;">{{ item.name }}</span>
                <span v-if="item.badgeKey && badgeCounts[item.badgeKey] > 0"
                      style="background:#ff4d4f;color:#fff;font-size:10px;border-radius:8px;padding:0 5px;min-width:16px;height:16px;line-height:16px;text-align:center;flex-shrink:0;">
                  {{ badgeCounts[item.badgeKey] > 99 ? '99+' : badgeCounts[item.badgeKey] }}
                </span>
              </router-link>
            </div>
          </div>
        </div>
      </aside>

      <!-- 主内容区域（嵌套路由出口） -->
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSession, logout, getMenuBadges, markBadgeRead, changePassword, getMenuTree } from '../api'
import { menuGroups, visibleGroupsFor, isSuperAdminSession, firstVisiblePath } from '../menu'
import NotificationBell from '../components/NotificationBell.vue'
import { showMessage } from '../utils/message'
import logoSvg from '../assets/logo.svg'

const route = useRoute()
const router = useRouter()
const session = ref(getSession())
const currentPath = computed(() => route.path)
const showUserMenu = ref(false)

const badgeCounts = ref<Record<string, number>>({})
let badgeTimer: number | undefined

const pathBadgeMap: Record<string, string> = {
  '/events': 'eventsPending',
  '/work-orders': 'workOrdersPending',
  '/audits': 'auditsPending',
  '/resident-reports': 'residentReportsPending',
  '/trend-alerts': 'trendAlerts',
  '/org-members': 'pwdResetsPending'
}

watch(currentPath, (path) => {
  const key = pathBadgeMap[path]
  if (key && badgeCounts.value[key] > 0) {
    badgeCounts.value[key] = 0
    markBadgeRead(key).catch(() => {})
  }
})

async function loadBadges() {
  try {
    const res = await getMenuBadges()
    if (res && typeof res === 'object') {
      badgeCounts.value = (res as Record<string, number>)
    }
  } catch (e) {}
}

const isSuperAdmin = computed(() => isSuperAdminSession(session.value))

const dbMenuMap = ref<Map<string, { name: string; sortOrder: number }> | null>(null)

async function loadDbMenus() {
  try {
    const tree = await getMenuTree()
    const map = new Map<string, { name: string; sortOrder: number }>()
    for (const n of (Array.isArray(tree) ? tree : []) as any[]) {
      if (n?.permissionCode) {
        map.set(n.permissionCode, { name: n.permissionName || '', sortOrder: n.sortOrder ?? 0 })
      }
    }
    dbMenuMap.value = map
  } catch (e) {
    dbMenuMap.value = null
  }
}

const visibleGroups = computed(() => {
  const base = visibleGroupsFor(session.value)
  const map = dbMenuMap.value
  if (!map) return base
  return base
    .map(g => {
      const items = g.items
        .filter(i => !i.permKey || map.has(i.permKey))
        .map(i => {
          const override = i.permKey ? map.get(i.permKey) : undefined
          return override ? { ...i, name: override.name || i.name } : i
        })
        .sort((a, b) => {
          const sa = a.permKey && map.get(a.permKey) ? map.get(a.permKey)!.sortOrder : 0
          const sb = b.permKey && map.get(b.permKey) ? map.get(b.permKey)!.sortOrder : 0
          return sa - sb
        })
      return { ...g, items }
    })
    .filter(g => g.items.length > 0)
})

const openGroups = ref<string[]>([])
const searchKey = ref('')

const filteredGroups = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  const groups = visibleGroups.value
  if (!key) return groups
  return groups
    .map(g => {
      if (g.name.toLowerCase().includes(key)) return g
      const items = g.items.filter(i => i.name.toLowerCase().includes(key))
      return items.length ? { ...g, items } : null
    })
    .filter((g): g is typeof menuGroups[number] => g !== null)
})

const activeGroup = computed(() => visibleGroups.value.find(g => g.items.some(i => i.path === route.path))?.name)
watch(() => route.path, () => {
  if (activeGroup.value && !openGroups.value.includes(activeGroup.value)) {
    openGroups.value.push(activeGroup.value)
  }
}, { immediate: true })

function toggleGroup(name: string) {
  const idx = openGroups.value.indexOf(name)
  if (idx >= 0) {
    openGroups.value.splice(idx, 1)
  } else {
    openGroups.value.push(name)
  }
}

function handleLogout() {
  logout()
  session.value = null
  window.location.hash = '#/login'
  showUserMenu.value = false
}

const showPwdModal = ref(false)
const pwdLoading = ref(false)
const pwdError = ref('')
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

function openPwdModal() {
  showUserMenu.value = false
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdError.value = ''
  showPwdModal.value = true
}

async function submitPwdChange() {
  pwdError.value = ''
  if (!pwdForm.oldPassword) { pwdError.value = '请输入当前密码'; return }
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 64) { pwdError.value = '新密码长度须在 6 到 64 位之间'; return }
  if (pwdForm.newPassword !== pwdForm.confirm) { pwdError.value = '两次输入的新密码不一致'; return }
  pwdLoading.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    showPwdModal.value = false
    showMessage('密码修改成功，请使用新密码重新登录')
    handleLogout()
  } catch (e: any) {
    pwdError.value = e?.message || '修改失败，请稍后重试'
  } finally {
    pwdLoading.value = false
  }
}

function closeUserMenu() {
  showUserMenu.value = false
}

onMounted(() => {
  document.addEventListener('click', closeUserMenu)
  loadBadges()
  badgeTimer = window.setInterval(loadBadges, 30000)
  if (session.value?.token) loadDbMenus()
})

onUnmounted(() => {
  document.removeEventListener('click', closeUserMenu)
  if (badgeTimer) clearInterval(badgeTimer)
})
</script>

<style scoped>
.menu-search {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #f3f4f6;
  border-radius: 8px;
  border: 1px solid transparent;
  transition: border-color 0.15s, background 0.15s;
}

.menu-search:focus-within {
  background: #fff;
  border-color: #0284c7;
}

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

.pwd-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.pwd-modal {
  width: 360px;
  max-width: 90vw;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.pwd-input {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  outline: none;
  background: #f8fafc;
  box-sizing: border-box;
}

.pwd-input:focus {
  border-color: #0284c7;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1);
}

.pwd-btn {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.pwd-btn:hover {
  border-color: #cbd5e1;
  background: #f8fafc;
}

.pwd-btn-primary {
  border: none;
  background: #0284c7;
  color: #fff;
}

.pwd-btn-primary:hover {
  background: #0369a1;
}

.pwd-btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>