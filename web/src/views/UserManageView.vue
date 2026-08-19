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
        <h1 class="page-title">账号管理</h1>
        <p class="page-desc">维护 Web 管理端账号，分配角色后账号即拥有对应角色的菜单与操作权限</p>
      </div>
      <button @click="openCreate" class="btn btn-primary">+ 新增账号</button>
    </div>

    <div class="card">
      <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center;">
        <input v-model="searchKey" class="form-input" style="width:260px;" placeholder="搜索姓名 / 账号 / 手机号..." />
        <select v-model="filterRoleId" class="form-input" style="width:200px;">
          <option :value="0">全部角色</option>
          <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}</option>
          <option :value="-1">未分配角色</option>
        </select>
        <button v-if="filterRoleId === -1" @click="filterRoleId = 0" class="btn btn-default" style="padding:6px 12px;font-size:12px;">清除筛选</button>
        <span style="color:#9ca3af;font-size:12px;">共 {{ filteredUsers.length }} 个账号</span>
      </div>
      <table class="data-table" style="width:100%;">
        <thead>
          <tr>
            <th>姓名</th>
            <th>登录账号</th>
            <th>手机号</th>
            <th>角色</th>
            <th>状态</th>
            <th style="width:300px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" style="text-align:center;color:#9ca3af;padding:32px;">加载中...</td></tr>
          <tr v-else-if="!filteredUsers.length"><td colspan="6" style="text-align:center;color:#9ca3af;padding:32px;">{{ (searchKey || filterRoleId !== 0) ? '未找到匹配的账号' : '暂无账号' }}</td></tr>
          <tr v-for="u in filteredUsers" :key="u.id">
            <td style="font-weight:600;">{{ u.realName || '-' }}</td>
            <td><code style="background:#f1f5f9;padding:2px 8px;border-radius:4px;font-size:12px;">{{ u.username }}</code></td>
            <td>{{ u.phone || '-' }}</td>
            <td>
              <span v-for="rn in (u.roleNames || [])" :key="rn"
                    style="display:inline-block;background:#e0f2fe;color:#0369a1;font-size:11px;border-radius:10px;padding:2px 8px;margin:2px 4px 2px 0;">
                {{ rn }}
              </span>
              <span v-if="!(u.roleNames || []).length" style="color:#9ca3af;font-size:12px;">未分配</span>
            </td>
            <td>
              <span :style="{ color: u.status === 'ACTIVE' ? '#059669' : '#9ca3af', fontSize: '12px', cursor: 'pointer' }"
                    @click="toggleStatus(u)" :title="u.status === 'ACTIVE' ? '点击停用' : '点击启用'">
                {{ u.status === 'ACTIVE' ? '正常' : '停用' }}
              </span>
            </td>
            <td>
              <div style="display:flex;gap:6px;flex-wrap:wrap;">
                <button @click="openEdit(u)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                <button @click="openRoles(u)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">分配角色</button>
                <button @click="openResetPwd(u)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">重置密码</button>
                <button @click="handleDelete(u)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;" :disabled="isSuperAdmin(u)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新增/编辑账号弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-box" style="width:480px;">
        <h3 style="margin:0 0 16px;font-size:16px;">{{ form.id ? '编辑账号' : '新增账号' }}</h3>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <label class="form-label">登录账号 <span style="color:#ef4444;">*</span></label>
          <input v-model="form.username" class="form-input" placeholder="登录用账号" :disabled="!!form.id" />
          <template v-if="!form.id">
            <label class="form-label">初始密码 <span style="color:#ef4444;">*</span></label>
            <input v-model="form.password" type="password" class="form-input" placeholder="6-64 位" />
          </template>
          <label class="form-label">姓名 <span style="color:#ef4444;">*</span></label>
          <input v-model="form.realName" class="form-input" placeholder="真实姓名" />
          <label class="form-label">手机号 <span style="color:#ef4444;">*</span></label>
          <input v-model="form.phone" class="form-input" placeholder="用于移动端登录与找回密码" />
          <label class="form-label">状态</label>
          <select v-model="form.status" class="form-input">
            <option value="ACTIVE">正常</option>
            <option value="DISABLED">停用</option>
          </select>
          <template v-if="!form.id">
            <label class="form-label">角色</label>
            <div style="display:flex;flex-wrap:wrap;gap:8px;">
              <label v-for="r in roles" :key="r.id" style="display:flex;align-items:center;gap:4px;font-size:13px;cursor:pointer;">
                <input type="checkbox" :value="r.id" v-model="form.roleIds" />
                {{ r.roleName }}
              </label>
            </div>
          </template>
          <p v-if="formError" style="color:#ef4444;font-size:12px;margin:0;">{{ formError }}</p>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px;">
          <button @click="showForm = false" class="btn btn-default">取消</button>
          <button @click="submitForm" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 分配角色弹窗 -->
    <div v-if="showRoles" class="modal-overlay" @click.self="showRoles = false">
      <div class="modal-box" style="width:420px;">
        <h3 style="margin:0 0 16px;font-size:16px;">分配角色 — {{ roleTarget?.realName || roleTarget?.username }}</h3>
        <div style="display:flex;flex-direction:column;gap:10px;max-height:320px;overflow-y:auto;">
          <label v-for="r in roles" :key="r.id" style="display:flex;align-items:center;gap:8px;font-size:13px;cursor:pointer;">
            <input type="checkbox" :value="r.id" v-model="checkedRoleIds" />
            <span style="font-weight:600;">{{ r.roleName }}</span>
            <span v-if="r.remark" style="color:#9ca3af;font-size:11px;">{{ r.remark }}</span>
          </label>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px;">
          <button @click="showRoles = false" class="btn btn-default">取消</button>
          <button @click="submitRoles" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div v-if="showPwd" class="modal-overlay" @click.self="showPwd = false">
      <div class="modal-box" style="width:420px;">
        <h3 style="margin:0 0 16px;font-size:16px;">重置密码 — {{ pwdTarget?.realName || pwdTarget?.username }}</h3>
        <div style="display:flex;flex-direction:column;gap:12px;">
          <input v-model="newPassword" type="password" class="form-input" placeholder="新密码（6-64 位）" />
          <input v-model="confirmPassword" type="password" class="form-input" placeholder="再次输入新密码" />
          <p v-if="pwdError" style="color:#ef4444;font-size:12px;margin:0;">{{ pwdError }}</p>
        </div>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:20px;">
          <button @click="showPwd = false" class="btn btn-default">取消</button>
          <button @click="submitResetPwd" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '确认重置' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  getSystemUsers, getSystemUserDetail, createSystemUser, updateSystemUser,
  updateSystemUserStatus, assignUserRoles, resetSystemUserPassword, deleteSystemUser,
  getSystemRoles
} from '../api'

const loading = ref(false)
const saving = ref(false)
const users = ref<any[]>([])
const roles = ref<any[]>([])
const searchKey = ref('')
// 角色筛选：0=全部，-1=未分配角色，其余为角色 id
const filterRoleId = ref(0)

const toast = ref({ visible: false, type: 'error' as 'error' | 'success', message: '' })
let toastTimer: ReturnType<typeof setTimeout> | undefined

function notify(message: string, type: 'error' | 'success' = 'error') {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { visible: true, type, message }
  toastTimer = setTimeout(() => { toast.value.visible = false }, type === 'error' ? 8000 : 3000)
}

const filteredUsers = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  return users.value.filter(u => {
    if (filterRoleId.value === -1 && (u.roleCodes || []).length > 0) return false
    if (filterRoleId.value > 0) {
      const role = roles.value.find(r => r.id === filterRoleId.value)
      if (role && !(u.roleCodes || []).includes(role.roleCode)) return false
    }
    if (!key) return true
    return (u.realName || '').toLowerCase().includes(key)
      || (u.username || '').toLowerCase().includes(key)
      || (u.phone || '').includes(key)
  })
})

/** 超管账号禁止删除/停用的兜底判断（角色含 SUPER_ADMIN） */
function isSuperAdmin(u: any) {
  return (u.roleCodes || []).includes('SUPER_ADMIN')
}

async function fetchData() {
  loading.value = true
  try {
    const [userList, roleList] = await Promise.all([getSystemUsers(), getSystemRoles()])
    users.value = Array.isArray(userList) ? userList : []
    roles.value = Array.isArray(roleList) ? roleList : []
  } catch (e: any) {
    notify(`加载失败：${e?.message || '服务器异常'}`)
  } finally {
    loading.value = false
  }
}

/* ---------- 新增/编辑 ---------- */
const showForm = ref(false)
const formError = ref('')
const form = ref<{ id: number | null; username: string; password: string; realName: string; phone: string; status: string; roleIds: number[] }>({
  id: null, username: '', password: '', realName: '', phone: '', status: 'ACTIVE', roleIds: []
})

function openCreate() {
  form.value = { id: null, username: '', password: '', realName: '', phone: '', status: 'ACTIVE', roleIds: [] }
  formError.value = ''
  showForm.value = true
}

function openEdit(u: any) {
  form.value = { id: u.id, username: u.username, password: '', realName: u.realName || '', phone: u.phone || '', status: u.status, roleIds: [] }
  formError.value = ''
  showForm.value = true
}

async function submitForm() {
  formError.value = ''
  if (!form.value.username.trim()) { formError.value = '请输入登录账号'; return }
  if (!form.value.realName.trim()) { formError.value = '请输入姓名'; return }
  if (!/^1\d{10}$/.test(form.value.phone.trim())) { formError.value = '请输入正确的 11 位手机号（后端必填，移动端登录要用）'; return }
  if (!form.value.id) {
    if (!form.value.password || form.value.password.length < 6) { formError.value = '初始密码至少 6 位'; return }
    if (!form.value.roleIds.length) { formError.value = '请至少勾选一个角色'; return }
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateSystemUser(form.value.id, {
        username: form.value.username.trim(),
        realName: form.value.realName.trim(),
        phone: form.value.phone.trim() || undefined,
        status: form.value.status
      })
      notify('账号已更新', 'success')
    } else {
      await createSystemUser({
        username: form.value.username.trim(),
        password: form.value.password,
        realName: form.value.realName.trim(),
        phone: form.value.phone.trim() || undefined,
        status: form.value.status,
        roleIds: form.value.roleIds
      })
      notify('账号已创建', 'success')
    }
    showForm.value = false
    fetchData()
  } catch (e: any) {
    formError.value = e?.message || '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

/* ---------- 启停 ---------- */
async function toggleStatus(u: any) {
  if (isSuperAdmin(u) && u.status === 'ACTIVE') {
    notify('超级管理员账号不可停用')
    return
  }
  const next = u.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  if (!window.confirm(`确定${next === 'DISABLED' ? '停用' : '启用'}账号「${u.realName || u.username}」吗？`)) return
  try {
    await updateSystemUserStatus(u.id, next)
    notify(`账号已${next === 'DISABLED' ? '停用' : '启用'}`, 'success')
    fetchData()
  } catch (e: any) {
    notify(e?.message || '操作失败，请稍后重试')
  }
}

/* ---------- 分配角色 ---------- */
const showRoles = ref(false)
const roleTarget = ref<any>(null)
const checkedRoleIds = ref<number[]>([])

async function openRoles(u: any) {
  roleTarget.value = u
  showRoles.value = true
  try {
    const detail = await getSystemUserDetail(u.id)
    checkedRoleIds.value = detail?.roleIds || []
  } catch (e: any) {
    notify(`加载角色失败：${e?.message || '服务器异常'}`)
    showRoles.value = false
  }
}

async function submitRoles() {
  if (!roleTarget.value) return
  if (!checkedRoleIds.value.length) {
    notify('请至少勾选一个角色')
    return
  }
  saving.value = true
  try {
    await assignUserRoles(roleTarget.value.id, checkedRoleIds.value)
    notify('角色已保存，该用户重新登录后生效', 'success')
    showRoles.value = false
    fetchData()
  } catch (e: any) {
    notify(e?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

/* ---------- 重置密码 ---------- */
const showPwd = ref(false)
const pwdTarget = ref<any>(null)
const newPassword = ref('')
const confirmPassword = ref('')
const pwdError = ref('')

function openResetPwd(u: any) {
  pwdTarget.value = u
  newPassword.value = ''
  confirmPassword.value = ''
  pwdError.value = ''
  showPwd.value = true
}

async function submitResetPwd() {
  pwdError.value = ''
  if (!newPassword.value || newPassword.value.length < 6 || newPassword.value.length > 64) { pwdError.value = '新密码长度须在 6 到 64 位之间'; return }
  if (newPassword.value !== confirmPassword.value) { pwdError.value = '两次输入的密码不一致'; return }
  saving.value = true
  try {
    await resetSystemUserPassword(pwdTarget.value.id, newPassword.value)
    notify('密码已重置，请通知用户使用新密码登录', 'success')
    showPwd.value = false
  } catch (e: any) {
    pwdError.value = e?.message || '重置失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

/* ---------- 删除 ---------- */
async function handleDelete(u: any) {
  if (isSuperAdmin(u)) {
    notify('超级管理员账号不可删除')
    return
  }
  if (!window.confirm(`确定删除账号「${u.realName || u.username}」吗？删除后不可恢复。`)) return
  try {
    await deleteSystemUser(u.id)
    notify('账号已删除', 'success')
    fetchData()
  } catch (e: any) {
    notify(e?.message || '删除失败，请稍后重试')
  }
}

onMounted(fetchData)
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
