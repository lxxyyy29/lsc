<template>
  <div>
    <!-- 顶部轻提示 -->
    <transition name="toast">
      <div v-if="toast.visible" class="page-toast" :class="toast.type">
        <span class="toast-icon">{{ toast.type === 'error' ? '⛔' : '✔' }}</span>
        <span class="toast-msg">{{ toast.message }}</span>
        <button class="toast-close" @click="toast.visible = false">✕</button>
      </div>
    </transition>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">角色管理</h1>
        <p class="page-desc">系统固定 4 个内置角色（超级管理员/管理员/网格员/居民），不可新增或删除；可为角色分配菜单权限（勾选后该角色用户即可看到对应菜单）</p>
      </div>
    </div>

    <div class="card">
      <div class="filter-bar">
        <input v-model="searchKey" class="filter-input" placeholder="搜索角色名称 / 标识 / 备注..." />
        <span v-if="searchKey" style="color:#9ca3af;font-size:12px;">匹配 {{ filteredRoles.length }} / {{ roles.length }} 个角色</span>
      </div>
      <table class="data-table" style="width:100%;">
        <thead>
          <tr>
            <th>角色名称</th>
            <th>角色标识</th>
            <th>用户数</th>
            <th>菜单权限</th>
            <th>状态</th>
            <th>备注</th>
            <th style="width:110px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" style="text-align:center;color:#9ca3af;padding:32px;">加载中...</td></tr>
          <tr v-else-if="!filteredRoles.length"><td colspan="7" style="text-align:center;color:#9ca3af;padding:32px;">{{ searchKey ? '未找到匹配的角色' : '暂无角色' }}</td></tr>
          <tr v-for="r in filteredRoles" :key="r.id">
            <td style="font-weight:600;">{{ r.roleName }}</td>
            <td>
              <span style="font-size:13px;">{{ roleCodeName(r.roleCode) }}</span>
              <code style="background:#f1f5f9;padding:1px 6px;border-radius:4px;font-size:11px;color:#9ca3af;margin-left:6px;">{{ r.roleCode }}</code>
            </td>
            <td>{{ r.userCount }}</td>
            <td>
              <span v-if="r.webMenuChecked === undefined" style="color:#9ca3af;">-</span>
              <span v-else>{{ r.webMenuChecked }} / {{ totalMenuPerms }} <span v-if="r.webMenuChecked === 0" style="color:#ef4444;">（未勾选，该角色用户登录后无菜单）</span></span>
            </td>
            <td>
              <span :style="{ color: r.status === 'ACTIVE' ? '#059669' : '#9ca3af', fontSize: '12px' }">
                {{ r.status === 'ACTIVE' ? '正常' : '停用' }}
              </span>
            </td>
            <td style="color:#6b7280;font-size:12px;">{{ r.remark || '-' }}</td>
            <td>
              <div style="display:flex;gap:6px;">
                <button @click="openAssign(r)" class="btn btn-default" style="padding:4px 10px;font-size:12px;" :disabled="r.roleCode === 'SUPER_ADMIN'">分配权限</button>
                <span style="color:#9ca3af;font-size:12px;display:inline-flex;align-items:center;">内置角色</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分配权限弹窗：只展示本项目 web 菜单权限（web:menu:*），按模块分组 -->
    <div v-if="showAssign" class="modal-overlay" @click.self="showAssign = false">
      <div class="modal-box" style="width:640px;max-height:80vh;display:flex;flex-direction:column;">
        <h3 style="margin:0 0 4px;font-size:16px;">分配菜单权限 — {{ assignRole?.roleName }}</h3>
        <p style="color:#6b7280;font-size:12px;margin:0 0 12px;">勾选该角色可见的左侧导航菜单；接口权限由系统内置分配，不受此处影响</p>
        <div style="overflow-y:auto;flex:1;border:1px solid #e5e7eb;border-radius:8px;padding:8px 12px;">
          <div v-if="permLoading" style="text-align:center;color:#9ca3af;padding:24px;">加载中...</div>
          <div v-else v-for="group in permGroups" :key="group.name" style="margin-bottom:10px;">
            <div style="display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid #f1f5f9;">
              <input type="checkbox" :checked="isGroupAllChecked(group)" @change="toggleGroup(group, ($event.target as HTMLInputElement).checked)" />
              <span style="font-weight:600;font-size:13px;">{{ group.name }}</span>
              <span style="color:#9ca3af;font-size:11px;">{{ groupCheckedCount(group) }}/{{ group.items.length }}</span>
            </div>
            <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:4px;padding:6px 0 2px 24px;">
              <label v-for="p in group.items" :key="p.id" style="display:flex;align-items:center;gap:6px;font-size:13px;cursor:pointer;">
                <input type="checkbox" :value="p.id" v-model="checkedPermIds" />
                {{ p.permissionName }}
              </label>
            </div>
          </div>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px;">
          <button @click="showAssign = false" class="btn btn-default">取消</button>
          <button @click="submitAssign" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存权限' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getSystemRoles,
  getSystemRoleDetail, assignRolePermissions, getPermissionTree
} from '../api'
import { menuGroups } from '../menu'

// 菜单权限总数（与分配弹窗可勾选项一一对应，来自 menu.ts 单一数据源）
const totalMenuPerms = menuGroups.reduce((n, g) => n + g.items.length, 0)

const loading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const searchKey = ref('')

// 按名称/中文释义/英文标识/备注模糊搜索，角色多了可快速定位
const filteredRoles = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  if (!key) return roles.value
  return roles.value.filter(r =>
    (r.roleName || '').toLowerCase().includes(key)
    || (r.roleCode || '').toLowerCase().includes(key)
    || roleCodeName(r.roleCode).toLowerCase().includes(key)
    || (r.remark || '').toLowerCase().includes(key))
})

// 内置角色标识的中文释义（系统固定 4 个，不支持新增）
const ROLE_CODE_NAMES: Record<string, string> = {
  SUPER_ADMIN: '超级管理员',
  EVENT_OPERATOR: '管理员',
  GRID_WORKER: '网格员',
  PUBLIC: '居民'
}
function roleCodeName(code: string) {
  return ROLE_CODE_NAMES[code] || code
}

const toast = ref({ visible: false, type: 'error' as 'error' | 'success', message: '' })
let toastTimer: ReturnType<typeof setTimeout> | undefined

function notify(message: string, type: 'error' | 'success' = 'error') {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { visible: true, type, message }
  toastTimer = setTimeout(() => { toast.value.visible = false }, type === 'error' ? 8000 : 3000)
}

async function fetchRoles() {
  loading.value = true
  try {
    const data = await getSystemRoles()
    roles.value = Array.isArray(data) ? data : []
    enrichMenuCounts()
  } catch (e: any) {
    notify(`加载角色失败：${e?.message || '服务器异常'}`)
  } finally {
    loading.value = false
  }
}

// 列表“菜单权限”列口径与分配弹窗一致：只统计该角色实际勾选的 web:menu 权限
// （permissionCount 含 API 等非菜单权限，直接展示会与弹窗勾选状态对不上）
async function enrichMenuCounts() {
  const codes = new Set(menuGroups.flatMap(g => g.items.map(i => (i as any).permKey).filter(Boolean)))
  await Promise.all(roles.value.map(async (r: any) => {
    try {
      const detail = await getSystemRoleDetail(r.id)
      r.webMenuChecked = (detail?.permissionCodes || []).filter((c: string) => codes.has(c)).length
    } catch (e) {
      r.webMenuChecked = undefined
    }
  }))
}

/* ---------- 分配权限 ---------- */
const showAssign = ref(false)
const permLoading = ref(false)
const assignRole = ref<any>(null)
const checkedPermIds = ref<number[]>([])
const allMenuPerms = ref<any[]>([])

interface PermGroup { name: string; items: any[] }

// 只展示本项目 web 菜单权限（web:menu:*），按 remark 的"Web菜单-xxx"分组
const permGroups = computed<PermGroup[]>(() => {
  const map = new Map<string, any[]>()
  for (const p of allMenuPerms.value) {
    const m = (p.remark || '').match(/^Web菜单-(.+)$/)
    const groupName = m ? m[1] : '其他'
    if (!map.has(groupName)) map.set(groupName, [])
    map.get(groupName)!.push(p)
  }
  return [...map.entries()].map(([name, items]) => ({ name, items }))
})

function isGroupAllChecked(group: PermGroup) {
  return group.items.every(p => checkedPermIds.value.includes(p.id))
}

function groupCheckedCount(group: PermGroup) {
  return group.items.filter(p => checkedPermIds.value.includes(p.id)).length
}

function toggleGroup(group: PermGroup, checked: boolean) {
  const ids = group.items.map(p => p.id)
  if (checked) {
    checkedPermIds.value = [...new Set([...checkedPermIds.value, ...ids])]
  } else {
    checkedPermIds.value = checkedPermIds.value.filter(id => !ids.includes(id))
  }
}

async function openAssign(r: any) {
  assignRole.value = r
  showAssign.value = true
  permLoading.value = true
  try {
    if (!allMenuPerms.value.length) {
      const tree = await getPermissionTree('MENU')
      allMenuPerms.value = (Array.isArray(tree) ? tree : []).filter((p: any) => String(p.permissionCode || '').startsWith('web:menu:'))
    }
    const detail = await getSystemRoleDetail(r.id)
    // 只回显 web:menu 权限（历史遗留 menu: 权限不在本次配置范围内）
    const webMenuCodes = new Set(allMenuPerms.value.map(p => p.permissionCode))
    const codes: string[] = detail?.permissionCodes || []
    checkedPermIds.value = allMenuPerms.value
      .filter(p => codes.includes(p.permissionCode) && webMenuCodes.has(p.permissionCode))
      .map(p => p.id)
  } catch (e: any) {
    notify(`加载权限失败：${e?.message || '服务器异常'}`)
    showAssign.value = false
  } finally {
    permLoading.value = false
  }
}

async function submitAssign() {
  if (!assignRole.value) return
  saving.value = true
  try {
    // 后端覆盖式保存只保留 CATALOG/MENU 类型；把该角色已有的非 web:menu 历史菜单权限一并回传，避免覆盖丢失
    const detail = await getSystemRoleDetail(assignRole.value.id)
    const codes: string[] = detail?.permissionCodes || []
    const menuPerms: any[] = (await getPermissionTree('MENU')) as any[]
    const legacyIds = (Array.isArray(menuPerms) ? menuPerms : [])
      .filter(p => !String(p.permissionCode || '').startsWith('web:menu:') && codes.includes(p.permissionCode))
      .map(p => p.id)
    await assignRolePermissions(assignRole.value.id, [...checkedPermIds.value, ...legacyIds])
    notify('权限已保存，该角色用户重新登录后生效', 'success')
    showAssign.value = false
    fetchRoles()
  } catch (e: any) {
    notify(e?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

onMounted(fetchRoles)
</script>

<style scoped>
.form-label { font-size: 13px; color: #374151; font-weight: 500; }
.form-input { border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 12px; font-size: 13px; outline: none; }
.form-input:focus { border-color: #0284c7; }
.data-table th { text-align: left; font-size: 12px; color: #6b7280; font-weight: 600; padding: 8px 12px; border-bottom: 2px solid #e5e7eb; }
.data-table td { padding: 10px 12px; font-size: 13px; border-bottom: 1px solid #f1f5f9; }
.page-toast {
  position: fixed; top: 64px; left: 50%; transform: translateX(-50%);
  z-index: 10001; display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-radius: 8px; font-size: 13px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}
.page-toast.error { background: #fef2f2; color: #b91c1c; border: 1px solid #fecaca; }
.page-toast.success { background: #f0fdf4; color: #15803d; border: 1px solid #bbf7d0; }
.toast-close { border: none; background: none; cursor: pointer; color: inherit; font-size: 12px; }
.toast-enter-active, .toast-leave-active { transition: all 0.25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, -8px); }
</style>
