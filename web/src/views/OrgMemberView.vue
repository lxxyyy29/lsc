<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">组织人员管理</h1>
        <p class="page-desc">网格组长、网格员与所属网格绑定，以及社区工作人员、志愿者等信息维护</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="handleSyncFromUsers" class="btn btn-default" :disabled="syncing">
          <i class="fas fa-sync"></i>{{ syncing ? '同步中…' : '同步账号' }}
        </button>
        <button @click="openAssign" class="btn btn-default">
          <i class="fas fa-sitemap"></i>人员划分
        </button>
      </div>
    </div>

    <div class="card">
      <!-- 搜索筛选栏：姓名/电话 + 所属区域 + 组长 + 状态 -->
      <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px;">
        <input v-model="filters.keyword" class="filter-input" style="width:220px;" placeholder="搜索姓名 / 电话" />
        <select v-model="filters.gridId" class="filter-select" style="min-width:140px;">
          <option :value="null">全部区域</option>
          <option v-for="g in grids" :key="g.id" :value="Number(g.id)">{{ g.gridName }}</option>
        </select>
        <select v-model="filters.leaderId" class="filter-select" style="min-width:140px;">
          <option :value="null">全部组长</option>
          <option v-for="l in leaderOptions" :key="l.id" :value="Number(l.id)">{{ l.name }}</option>
        </select>
        <select v-model="filters.status" class="filter-select" style="min-width:110px;">
          <option value="">全部状态</option>
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">停用</option>
        </select>
        <button @click="resetFilters" class="btn btn-default" style="padding:6px 14px;font-size:12px;">重置</button>
      </div>
      <div v-if="loading" class="empty-state">
        <i class="fas fa-spinner fa-spin"></i>
        <p>加载中...</p>
      </div>
      <div v-else-if="error" class="empty-state">
        <i class="fas fa-exclamation-circle" style="color:#dc2626;"></i>
        <p style="color:#dc2626;">{{ error }}</p>
        <button @click="fetchData" class="btn btn-default" style="margin-top:12px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead><tr><th>姓名</th><th>电话</th><th>类型</th><th>职务</th><th>所属网格</th><th>组长</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="p in filteredList" :key="p.id">
              <td>{{ p.name }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td><span class="tag tag-blue">{{ memberTypeLabel(p.memberType) }}</span></td>
              <td>{{ p.position || '-' }}</td>
              <td>{{ p.gridName || '-' }}</td>
              <td>{{ p.leaderName || '-' }}</td>
              <td>
                <span :class="p.status === 'ACTIVE' ? 'tag tag-green' : 'tag tag-red'">
                  {{ p.status === 'ACTIVE' ? '启用中' : '已停用' }}
                </span>
              </td>
              <td>
                <div style="display:flex;gap:6px;">
                  <button @click="handleEdit(p)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                  <button @click="handleDelete(p)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!list.length" class="empty-state">
          <i class="fas fa-users"></i>
          <p>暂无组织人员，请先在「账号管理」创建网格员/网格组长账号，或点右上角「同步账号」拉取已有账号</p>
        </div>
      </template>
    </div>

    <!-- 编辑弹窗（本模块仅用于组长↔网格员关系绑定，不提供添加、不修改人员类型/职务） -->
    <div v-if="showEdit" class="modal-overlay">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">编辑组织人员</h3>
        <p style="font-size:12px;color:#6b7280;margin:0 0 12px;">人员类型与职务由后台账号统一管理，此处仅可调整联系方式与所属网格</p>
        <div class="form-group">
          <label class="form-label">姓名 <span class="required">*</span></label>
          <input v-model="form.name" class="form-input" placeholder="请输入姓名" />
        </div>
        <div class="form-group">
          <label class="form-label">电话</label>
          <input v-model="form.phone" class="form-input" placeholder="请输入电话" />
        </div>
        <div class="form-group">
          <label class="form-label">所属社区</label>
          <input :value="'拔蛟窝社区'" class="form-input" disabled style="background:#f9fafb;" />
        </div>
        <div v-if="['GRID_WORKER', 'LEADER'].includes(form.memberType)" class="form-group">
          <label class="form-label">职位 <span class="required">*</span></label>
          <select v-model="form.position" class="form-select">
            <option v-for="p in positionOptions" :key="p" :value="p">{{ p }}</option>
          </select>
          <p style="font-size:12px;color:#6b7280;margin-top:4px;">保存后账号角色将按职位自动绑定（组长/网格长→网格组长角色）</p>
        </div>
        <!-- 网格绑定：网格员填「所属小网格」，网格组长/网格长填「管辖小网格」 -->
        <div v-if="['GRID_WORKER', 'LEADER'].includes(form.memberType)" class="form-group">
          <label class="form-label">
            {{ form.memberType === 'LEADER' ? '管辖小网格' : '所属小网格' }}
            <span v-if="form.memberType === 'GRID_WORKER'" class="required">*</span>
          </label>
          <select v-model="form.gridId" class="form-select">
            <option :value="null">{{ form.memberType === 'LEADER' ? '暂不绑定' : '请选择小网格' }}</option>
            <option v-for="g in grids" :key="g.id" :value="Number(g.id)">{{ g.gridName }}</option>
          </select>
          <p v-if="!grids.length" style="font-size:12px;color:#d97706;margin-top:4px;">
            ⚠️ 暂无小网格，请先到「网格管理」添加网格
          </p>
          <p v-else style="font-size:12px;color:#6b7280;margin-top:4px;">
            {{ form.memberType === 'LEADER'
              ? '绑定后该组长即为该小网格负责人，H5「我的网格」与派单/审核范围按此生效'
              : '绑定后联动 H5「我的网格」、巡查任务与派单' }}
          </p>
          <label v-if="isLeaderForm && form.gridId"
                 style="display:flex;align-items:flex-start;gap:6px;font-size:12px;color:#374151;margin-top:8px;cursor:pointer;line-height:1.5;">
            <input type="checkbox" v-model="form.autoAssignWorkers" style="margin-top:2px;" />
            <span>同时把该小网格下的在岗网格员划入该组长名下（已在其他组长名下的会被改划）</span>
          </label>
        </div>
        <div class="form-group">
          <label class="form-label">状态</label>
          <select v-model="form.status" class="form-select">
            <option value="ACTIVE">启用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="closeModal" class="btn btn-default">取消</button>
          <button @click="handleSubmit" class="btn btn-primary">保存</button>
        </div>
      </div>
    </div>

    <!-- 人员划分弹窗：选择组长后勾选属下网格员 -->
    <div v-if="showAssign" class="modal-overlay">
      <div class="modal-box" style="width:640px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">人员划分</h3>
        <div class="form-group">
          <label class="form-label">选择组长 <span class="required">*</span></label>
          <select v-model="assignLeaderId" class="form-select" @change="syncCheckedByLeader">
            <option :value="null">请选择组长</option>
            <option v-for="l in leaderCandidates" :key="l.id" :value="Number(l.id)">
              {{ l.name }}（{{ l.position || memberTypeLabel(l.memberType) }}）
            </option>
          </select>
          <p v-if="!leaderCandidates.length" style="font-size:12px;color:#d97706;margin-top:4px;">⚠️ 暂无组长候选人，请先添加职务含“组长/网格长”的人员或社区领导</p>
        </div>
        <div class="form-group">
          <label class="form-label" style="display:flex;align-items:center;justify-content:space-between;">
            <span>属下网格员（勾选后划入所选组长名下）</span>
            <label style="font-size:12px;color:#666;font-weight:normal;cursor:pointer;">
              <input type="checkbox" :checked="allChecked" @change="toggleAll" style="margin-right:4px;">全选
            </label>
          </label>
          <div style="max-height:280px;overflow-y:auto;border:1px solid #e5e7eb;border-radius:6px;padding:8px 12px;">
            <div v-for="m in assignableMembers" :key="m.id" style="display:flex;align-items:center;gap:8px;padding:6px 0;border-bottom:1px solid #f3f4f6;">
              <input type="checkbox" :value="Number(m.id)" v-model="assignChecked" />
              <span style="flex:1;">{{ m.name }} <span style="font-size:12px;color:#999;">{{ m.position || '-' }} · {{ m.gridName || '未分配网格' }}</span></span>
              <span v-if="m.leaderName" style="font-size:12px;color:#0284c7;">当前：{{ m.leaderName }}</span>
              <button v-if="m.leaderId" @click="unassignOne(m)" class="btn btn-danger" style="padding:2px 8px;font-size:11px;">取消划分</button>
            </div>
            <p v-if="!assignableMembers.length" style="text-align:center;color:#999;padding:16px 0;">暂无可划分的网格员</p>
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showAssign = false" class="btn btn-default">取消</button>
          <button @click="handleAssign" class="btn btn-primary">确认划分</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import http from '../api'
import { showMessage } from '../utils/message'
import { confirmDialog, promptDialog } from '../utils/dialog'

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const showAdd = ref(false)
const showEdit = ref(false)

// ==================== 搜索筛选 ====================
const filters = ref({ keyword: '', gridId: null as number | null, leaderId: null as number | null, status: '' })

// 组长下拉选项：从列表提取（memberType=LEADER 或 leaderName 非空的人员）
const leaderOptions = computed(() => {
  const seen = new Map<number, string>()
  list.value.forEach((m: any) => {
    if (m.memberType === 'LEADER' && m.id) seen.set(m.id, m.name)
  })
  list.value.forEach((m: any) => {
    if (m.leaderId && m.leaderName && !seen.has(m.leaderId)) seen.set(m.leaderId, m.leaderName)
  })
  return Array.from(seen, ([id, name]) => ({ id, name }))
})

const filteredList = computed(() => {
  const f = filters.value
  const kw = f.keyword.trim().toLowerCase()
  return list.value.filter((p: any) => {
    if (kw && !((p.name || '').toLowerCase().includes(kw) || (p.phone || '').toLowerCase().includes(kw))) return false
    if (f.gridId != null && Number(p.gridId) !== Number(f.gridId)) return false
    if (f.leaderId != null && Number(p.leaderId) !== Number(f.leaderId)) return false
    if (f.status && p.status !== f.status) return false
    return true
  })
})

function resetFilters() {
  filters.value = { keyword: '', gridId: null, leaderId: null, status: '' }
}

// 人员划分：组长 ↔ 属下网格员勾选
const showAssign = ref(false)
const leaderCandidates = ref<any[]>([])
const assignLeaderId = ref<number | null>(null)
const assignChecked = ref<number[]>([])

const form = ref({
  id: null as number | null,
  name: '',
  phone: '',
  position: '',
  memberType: 'GRID_WORKER', // GRID_WORKER=网格员, STAFF=社区工作人员
  gridId: null as number | null,
  status: 'ACTIVE',
  // 组长保存管辖网格时，是否把该网格下在岗网格员一并划入其名下
  autoAssignWorkers: false,
})

/** 当前编辑的是否为网格组长/网格长（与后端组长判定口径一致） */
const isLeaderForm = computed(() => {
  const pos = form.value.position || ''
  return form.value.memberType === 'LEADER' || pos.includes('组长') || pos.includes('网格长')
})

const memberTypes = [
  { value: 'GRID_WORKER', label: '网格员' },
  { value: 'STAFF', label: '社区工作人员' },
  { value: 'LEADER', label: '社区领导' },
  { value: 'VOLUNTEER', label: '志愿者' },
]

const memberTypeMap: Record<string, string> = {}
memberTypes.forEach(t => memberTypeMap[t.value] = t.label)
function memberTypeLabel(type: string) { return memberTypeMap[type] || type || '-' }

// 可划分成员：排除组长候选人与志愿者，仅网格员/社区工作人员参与划分
const assignableMembers = computed(() => {
  const leaderIds = new Set(leaderCandidates.value.map((l: any) => l.id))
  return list.value.filter(m => !leaderIds.has(m.id) && (m.memberType === 'GRID_WORKER' || m.memberType === 'STAFF'))
})
const allChecked = computed(() => assignableMembers.value.length > 0 && assignableMembers.value.every(m => assignChecked.value.includes(m.id)))
function toggleAll(e: Event) {
  assignChecked.value = (e.target as HTMLInputElement).checked ? assignableMembers.value.map(m => m.id) : []
}
function syncCheckedByLeader() {
  // 切换组长时，默认勾选已划在该组长名下的成员，方便查看/调整
  assignChecked.value = assignLeaderId.value
    ? assignableMembers.value.filter(m => m.leaderId === assignLeaderId.value).map(m => m.id)
    : []
}
// 职位下拉（职位与系统角色绑定：组长/网格长→网格组长角色）
const positionOptions = computed(() => {
  if (form.value.memberType === 'LEADER') return ['组长', '网格长']
  return ['网格员', '组长', '网格长']
})

// 同步账号：把已有网格员/网格组长账号补录进组织人员表（不改变已绑定的网格）
const syncing = ref(false)
async function handleSyncFromUsers() {
  syncing.value = true
  try {
    const res: any = await http.post('/community/org-members/sync-from-users')
    if (res?.success === false) {
      showMessage(res.message || '同步失败')
    } else {
      showMessage(res?.message || '同步完成')
      await fetchData()
    }
  } catch (e: any) {
    showMessage(e?.message || '同步失败')
  } finally {
    syncing.value = false
  }
}

async function openAssign() {
  assignLeaderId.value = null
  assignChecked.value = []
  try {
    leaderCandidates.value = await http.get('/community/org-members/leader-candidates') || []
  } catch(e: any) {
    showMessage('加载组长候选人失败：' + (e?.message || '未知错误'))
    return
  }
  showAssign.value = true
}
async function handleAssign() {
  if (!assignLeaderId.value) { showMessage('请先选择组长'); return }
  if (!assignChecked.value.length) { showMessage('请勾选要划分的属下网格员'); return }
  const leader = leaderCandidates.value.find(l => l.id === assignLeaderId.value)
  if (!await confirmDialog({ message: `确认将选中的 ${assignChecked.value.length} 名成员划入组长「${leader?.name}」名下吗？` })) return
  try {
    await http.post('/community/org-members/assign', { leaderId: assignLeaderId.value, memberIds: assignChecked.value })
    showMessage('划分成功')
    showAssign.value = false
    await fetchData()
  } catch(e: any) {
    showMessage(e?.message || '划分失败')
  }
}
async function unassignOne(m: any) {
  if (!await confirmDialog({ message: `确认取消「${m.name}」与组长「${m.leaderName}」的划分关系吗？` })) return
  try {
    await http.post('/community/org-members/assign', { leaderId: null, memberIds: [m.id] })
    showMessage('已取消划分')
    await fetchData()
    if (showAssign.value) await openAssign()
  } catch(e: any) {
    showMessage(e?.message || '操作失败')
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get('/community/org-members') || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function fetchGrids() {
  try {
    const tree = await http.get('/community/grids/tree') || []
    // 只提取小网格（level=3）
    const smallGrids: any[] = []
    const extractSmall = (nodes: any[]) => {
      for (const n of nodes) {
        if (n.gridLevel === 3) smallGrids.push(n)
        if (n.children) extractSmall(n.children)
      }
    }
    extractSmall(tree)
    grids.value = smallGrids
  } catch(e) {}
}

function closeModal() {
  showAdd.value = false
  showEdit.value = false
  form.value = { id: null, name: '', phone: '', position: '', memberType: 'GRID_WORKER', gridId: null, status: 'ACTIVE', autoAssignWorkers: false }
}

function handleEdit(item: any) {
  // autoAssignWorkers 是请求开关，不从列表数据回显，每次编辑默认不勾选
  form.value = { ...item, autoAssignWorkers: false }
  showEdit.value = true
}

async function handleDelete(item: any) {
  if (!await confirmDialog({ message: `确定要删除网格员「${item.name}」吗？`, danger: true, okText: '删除' })) return
  try {
    await http.delete(`/community/org-members/${item.id}`)
    showMessage('删除成功')
    fetchData()
  } catch(e: any) {
    showMessage(e?.message || '删除失败')
  }
}

async function handleSubmit() {
  if (!form.value.name) { showMessage('请输入姓名'); return }
  if (form.value.memberType === 'GRID_WORKER' && !form.value.gridId) { showMessage('请选择所属小网格'); return }
  // 组长绑定管辖网格并勾选「一并划分」时，会覆盖该网格下网格员的归属关系，先二次确认
  const willAssign = isLeaderForm.value && !!form.value.gridId && form.value.autoAssignWorkers
  if (willAssign) {
    const gridName = grids.value.find(g => Number(g.id) === Number(form.value.gridId))?.gridName || '所选小网格'
    if (!await confirmDialog({
      message: `保存后将把「${gridName}」下全部在岗网格员划入「${form.value.name}」名下（原本挂在其他组长名下的会被改划），确认继续吗？`,
      okText: '确认保存'
    })) return
  }
  try {
    if (showEdit.value && form.value.id) {
      // 非组长不携带划分开关，避免误触发自动划分
      await http.put(`/community/org-members/${form.value.id}`, { ...form.value, autoAssignWorkers: willAssign })
      showMessage(willAssign ? '保存成功，该网格下的网格员已划入其名下' : '保存成功')
    } else {
      showMessage('请通过后台账号管理添加组织人员')
      return
    }
    closeModal()
    await fetchData()
  } catch(e: any) {
    showMessage(e?.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
})
</script>