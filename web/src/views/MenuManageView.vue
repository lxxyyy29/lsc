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
        <h1 class="page-title">菜单管理</h1>
        <p class="page-desc">编辑左侧导航菜单的名称、排序与显隐；保存后刷新页面或重新登录生效。路由与权限码由系统识别，不在此处修改</p>
      </div>
      <button @click="fetchMenus" class="btn btn-default" :disabled="loading">{{ loading ? '加载中...' : '重新加载' }}</button>
    </div>

    <div class="card">
      <div v-if="loading" style="text-align:center;color:#9ca3af;padding:32px;">加载中...</div>
      <div v-else-if="!groups.length" style="text-align:center;color:#9ca3af;padding:32px;">暂无菜单数据</div>
      <template v-else>
        <div v-for="group in groups" :key="group.name" style="margin-bottom:20px;">
          <div style="font-weight:600;font-size:14px;color:#1e293b;padding:8px 0;border-bottom:2px solid #e5e7eb;margin-bottom:4px;">
            {{ group.name }}
            <span style="color:#9ca3af;font-size:12px;font-weight:400;">（{{ group.items.length }} 个菜单）</span>
          </div>
          <table class="data-table" style="width:100%;">
            <thead>
              <tr>
                <th style="width:220px;">菜单名称</th>
                <th style="width:200px;">权限码</th>
                <th style="width:160px;">路由</th>
                <th style="width:110px;">排序值</th>
                <th style="width:110px;">状态</th>
                <th style="width:120px;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="n in group.items" :key="n.id">
                <td>
                  <input v-model="edits[n.id].name" class="form-input" style="width:190px;padding:6px 10px;" />
                </td>
                <td><code style="background:#f1f5f9;padding:2px 8px;border-radius:4px;font-size:11px;color:#6b7280;">{{ n.permissionCode }}</code></td>
                <td style="color:#6b7280;font-size:12px;">{{ n.path || '-' }}</td>
                <td>
                  <input v-model.number="edits[n.id].sortOrder" type="number" min="0" class="form-input" style="width:80px;padding:6px 10px;" />
                </td>
                <td>
                  <select v-model="edits[n.id].status" class="form-input" style="padding:6px 10px;">
                    <option value="ACTIVE">显示</option>
                    <option value="DISABLED">隐藏</option>
                  </select>
                </td>
                <td>
                  <button @click="save(n)" class="btn btn-primary" style="padding:4px 12px;font-size:12px;"
                          :disabled="!isDirty(n) || savingId === n.id">
                    {{ savingId === n.id ? '保存中...' : '保存' }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p style="color:#9ca3af;font-size:12px;margin:0;">
          说明：「隐藏」对所有角色生效（含超级管理员）；角色可见范围仍由「角色管理 - 分配权限」控制。排序值越小越靠前。
        </p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getSystemMenuTree, updateSystemMenu } from '../api'
import { menuGroups } from '../menu'

interface MenuNode {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: string
  clientType: string
  parentId: number | null
  path: string | null
  component: string | null
  icon: string | null
  sortOrder: number
  status: string
  remark: string | null
}

// 编辑态：名称/排序/显隐可改，其余字段回传原值（后端 PUT 为全字段覆盖）
interface EditState { name: string; sortOrder: number; status: string }

const loading = ref(false)
const savingId = ref<number | null>(null)
const nodes = ref<MenuNode[]>([])
const edits = reactive<Record<number, EditState>>({})

// 分组顺序与侧边栏一致（menu.ts 单一数据源），未命中 remark 分组的归入"其他"
const groupOrder = [...menuGroups.map(g => g.name), '其他']

const toast = ref({ visible: false, type: 'error' as 'error' | 'success', message: '' })
let toastTimer: ReturnType<typeof setTimeout> | undefined

function notify(message: string, type: 'error' | 'success' = 'error') {
  if (toastTimer) clearTimeout(toastTimer)
  toast.value = { visible: true, type, message }
  toastTimer = setTimeout(() => { toast.value.visible = false }, type === 'error' ? 8000 : 3000)
}

async function fetchMenus() {
  loading.value = true
  try {
    const tree = await getSystemMenuTree()
    const list = (Array.isArray(tree) ? tree : []) as MenuNode[]
    // 只管理 Web 端菜单权限（web:menu:* 且类型为 MENU），接口/按钮权限不在此页展示
    nodes.value = list.filter(n => n.permissionType === 'MENU' && String(n.permissionCode || '').startsWith('web:menu:'))
    for (const key of Object.keys(edits)) delete edits[Number(key)]
    for (const n of nodes.value) {
      edits[n.id] = { name: n.permissionName, sortOrder: n.sortOrder ?? 0, status: n.status }
    }
  } catch (e: any) {
    notify(`加载菜单失败：${e?.message || '服务器异常'}`)
  } finally {
    loading.value = false
  }
}

const groups = computed(() => {
  const map = new Map<string, MenuNode[]>()
  for (const n of nodes.value) {
    // remark 存储的即为分组名（V104 清洗后已去掉 "Web菜单-" 前缀）
    const name = (n.remark || '').trim() || '其他'
    if (!map.has(name)) map.set(name, [])
    map.get(name)!.push(n)
  }
  return [...map.entries()]
    .map(([name, items]) => ({ name, items: [...items].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)) }))
    .sort((a, b) => {
      const ia = groupOrder.indexOf(a.name)
      const ib = groupOrder.indexOf(b.name)
      return (ia < 0 ? 999 : ia) - (ib < 0 ? 999 : ib)
    })
})

function isDirty(n: MenuNode) {
  const e = edits[n.id]
  if (!e) return false
  return e.name.trim() !== n.permissionName
    || Number(e.sortOrder) !== (n.sortOrder ?? 0)
    || e.status !== n.status
}

async function save(n: MenuNode) {
  const e = edits[n.id]
  if (!e) return
  if (!e.name.trim()) { notify('菜单名称不能为空'); return }
  if (e.sortOrder == null || Number(e.sortOrder) < 0) { notify('排序值不能为负数'); return }
  savingId.value = n.id
  try {
    // PUT 为全字段覆盖：未开放编辑的字段原值回传，避免被清空
    await updateSystemMenu(n.id, {
      permissionCode: n.permissionCode,
      permissionName: e.name.trim(),
      permissionType: n.permissionType,
      clientType: n.clientType,
      parentId: n.parentId,
      path: n.path,
      component: n.component,
      icon: n.icon,
      sortOrder: Number(e.sortOrder),
      status: e.status,
      remark: n.remark
    })
    n.permissionName = e.name.trim()
    n.sortOrder = Number(e.sortOrder)
    n.status = e.status
    notify('菜单已更新，刷新页面或重新登录后生效', 'success')
  } catch (err: any) {
    notify(err?.message || '保存失败，请稍后重试')
  } finally {
    savingId.value = null
  }
}

onMounted(fetchMenus)
</script>

<style scoped>
.form-input { border: 1px solid #d1d5db; border-radius: 8px; padding: 8px 12px; font-size: 13px; outline: none; }
.form-input:focus { border-color: #0284c7; }
.data-table th { text-align: left; font-size: 12px; color: #6b7280; font-weight: 600; padding: 8px 12px; border-bottom: 1px solid #e5e7eb; }
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
