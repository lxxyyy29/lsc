<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">组织人员管理</h1>
        <p class="page-desc">网格员、社区工作人员、志愿者等信息维护</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="handleSync" class="btn btn-default">
          <i class="fas fa-sync"></i>同步网格员
        </button>
        <button @click="openAssign" class="btn btn-default">
          <i class="fas fa-sitemap"></i>人员划分
        </button>
        <button v-if="canApprove" @click="showApproval = true; fetchPending()" class="btn btn-default">
          <i class="fas fa-user-check"></i>注册审批
          <span v-if="pendingCount > 0" style="background:#dc2626;color:#fff;border-radius:10px;padding:1px 6px;font-size:11px;margin-left:4px;">{{ pendingCount }}</span>
        </button>
        <button v-if="canResetPwd" @click="showPwdReset = true; fetchPwdResets()" class="btn btn-default">
          <i class="fas fa-key"></i>密码重置
          <span v-if="pwdResetCount > 0" style="background:#dc2626;color:#fff;border-radius:10px;padding:1px 6px;font-size:11px;margin-left:4px;">{{ pwdResetCount }}</span>
        </button>
        <button @click="showAdd = true" class="btn btn-primary">
          <i class="fas fa-plus"></i>添加组织人员
        </button>
      </div>
    </div>

    <div class="card">
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
            <tr v-for="p in list" :key="p.id">
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
          <p>暂无网格员，点击"添加网格员"开始添加</p>
        </div>
      </template>
    </div>

    <!-- 注册审批弹窗 -->
    <div v-if="showApproval" class="modal-overlay" @click.self="showApproval = false">
      <div class="modal-box" style="width:600px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">注册审批</h3>
        <table class="table" style="font-size:13px;">
          <thead><tr><th>用户名</th><th>姓名</th><th>手机号</th><th>申请时间</th><th>来源</th><th>身份</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="r in pendingList" :key="r.id">
              <td>{{ r.username }}</td>
              <td>{{ r.realName }}</td>
              <td>{{ r.phone }}</td>
              <td>{{ r.createdAt }}</td>
              <td>
                <span :class="r.regSource === 'WEB' ? 'tag tag-green' : 'tag tag-blue'">
                  {{ r.regSource === 'WEB' ? 'Web管理员注册' : '小程序注册' }}
                </span>
              </td>
              <td>
                <select v-if="r.regSource !== 'WEB'" v-model="approvalTypes[r.id]" class="form-select" style="padding:3px 8px;font-size:12px;">
                  <option value="GRID_WORKER">网格员</option>
                  <option value="STAFF">社区工作人员</option>
                </select>
                <span v-else style="font-size:12px;color:#059669;">普通管理员</span>
              </td>
              <td>
                <button @click="approveReg(r)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;">通过</button>
                <button @click="rejectReg(r)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">拒绝</button>
              </td>
            </tr>
            <tr v-if="!pendingList.length"><td colspan="7" style="text-align:center;color:#999;padding:20px;">暂无待审批</td></tr>
          </tbody>
        </table>
        <div style="text-align:right;margin-top:16px;">
          <button @click="showApproval = false" class="btn btn-default">关闭</button>
        </div>
      </div>
    </div>

    <!-- 密码重置申请弹窗 -->
    <div v-if="showPwdReset" class="modal-overlay" @click.self="showPwdReset = false">
      <div class="modal-box" style="width:640px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">密码重置申请</h3>
        <table class="table" style="font-size:13px;">
          <thead><tr><th>姓名</th><th>账号</th><th>手机号</th><th>申请时间</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="r in pwdResetList" :key="r.id">
              <td>{{ r.real_name || '-' }}</td>
              <td>{{ r.account }}</td>
              <td>{{ r.phone }}</td>
              <td>{{ formatTs(r.created_at) }}</td>
              <td>
                <span :class="r.status === 'PENDING' ? 'tag tag-orange' : (r.status === 'APPROVED' ? 'tag tag-green' : 'tag tag-red')">
                  {{ r.status === 'PENDING' ? '待处理' : (r.status === 'APPROVED' ? '已重置' : '已驳回') }}
                </span>
                <span v-if="r.status !== 'PENDING' && r.handled_by_name" style="font-size:11px;color:#999;margin-left:4px;">{{ r.handled_by_name }}处理</span>
              </td>
              <td>
                <template v-if="r.status === 'PENDING'">
                  <button @click="approvePwdReset(r)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;">重置</button>
                  <button @click="rejectPwdReset(r)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">驳回</button>
                </template>
                <span v-else style="font-size:12px;color:#999;">{{ formatTs(r.handled_at) }}</span>
              </td>
            </tr>
            <tr v-if="!pwdResetList.length"><td colspan="6" style="text-align:center;color:#999;padding:20px;">暂无密码重置申请</td></tr>
          </tbody>
        </table>
        <p style="font-size:12px;color:#999;margin-top:12px;">重置后新密码为手机号后 6 位，请通过电话/微信告知用户本人</p>
        <div style="text-align:right;margin-top:12px;">
          <button @click="showPwdReset = false" class="btn btn-default">关闭</button>
        </div>
      </div>
    </div>

    <!-- 添加/编辑弹窗 -->
    <div v-if="showAdd || showEdit" class="modal-overlay" @click.self="closeModal">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ showEdit ? '编辑组织人员' : '添加组织人员' }}</h3>
        <div class="form-group">
          <label class="form-label">姓名 <span class="required">*</span></label>
          <input v-model="form.name" class="form-input" placeholder="请输入姓名" />
        </div>
        <div class="form-group">
          <label class="form-label">电话</label>
          <input v-model="form.phone" class="form-input" placeholder="请输入电话" />
        </div>
        <div class="form-group">
          <label class="form-label">人员类型 <span class="required">*</span></label>
          <select v-model="form.memberType" class="form-select">
            <option v-for="t in memberTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">职务</label>
          <input v-model="form.position" class="form-input" placeholder="如：网格员、网格长" />
        </div>
        <div class="form-group">
          <label class="form-label">所属社区</label>
          <input :value="'拔蛟窝社区'" class="form-input" disabled style="background:#f9fafb;" />
        </div>
        <div v-if="form.memberType === 'GRID_WORKER'" class="form-group">
          <label class="form-label">所属小网格 <span class="required">*</span></label>
          <select v-model="form.gridId" class="form-select">
            <option :value="null">请选择小网格</option>
            <option v-for="g in grids" :key="g.id" :value="g.id">{{ g.gridName }}</option>
          </select>
          <p v-if="!grids.length" style="font-size:12px;color:#d97706;margin-top:4px;">
            ⚠️ 暂无小网格，请先添加网格数据
          </p>
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
          <button @click="handleSubmit" class="btn btn-primary">{{ showEdit ? '保存' : '添加' }}</button>
        </div>
      </div>
    </div>

    <!-- 人员划分弹窗：选择组长后勾选属下网格员 -->
    <div v-if="showAssign" class="modal-overlay" @click.self="showAssign = false">
      <div class="modal-box" style="width:640px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">人员划分</h3>
        <div class="form-group">
          <label class="form-label">选择组长 <span class="required">*</span></label>
          <select v-model="assignLeaderId" class="form-select" @change="syncCheckedByLeader">
            <option :value="null">请选择组长</option>
            <option v-for="l in leaderCandidates" :key="l.id" :value="l.id">
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
              <input type="checkbox" :value="m.id" v-model="assignChecked" />
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
import http, { syncGridWorkersToOrgMembers, hasPermission } from '../api'
import { showMessage } from '../utils/message'
import { confirmDialog, promptDialog } from '../utils/dialog'

// 注册审批仅超级管理员可见（后端同样校验 api:system:user:create）
const canApprove = hasPermission('api:system:user:create')
// 密码重置处理：超管与普通管理员均可（后端校验 api:password-reset:handle）
const canResetPwd = hasPermission('api:password-reset:handle')

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const showAdd = ref(false)
const showEdit = ref(false)
const showApproval = ref(false)
const pendingList = ref<any[]>([])
const pendingCount = ref(0)
const approvalTypes = ref<Record<number, string>>({})
const showPwdReset = ref(false)
const pwdResetList = ref<any[]>([])
const pwdResetCount = ref(0)

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

async function handleSync() {
  if (!await confirmDialog({ message: '将系统用户中的网格员同步到组织人员表，继续吗？', okText: '继续' })) return
  try {
    const res: any = await syncGridWorkersToOrgMembers()
    showMessage(res?.message || '同步完成')
    await fetchData()
  } catch(e: any) {
    showMessage('同步失败：' + (e?.message || '未知错误'))
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
  // 待审批数量单独拉取：无审批权限的普通管理员不影响页面正常使用
  if (canApprove) {
    try {
      const pending = await http.get('/registration/pending') || []
      pendingCount.value = pending.length
    } catch { pendingCount.value = 0 }
  }
  // 待处理密码重置申请数：超管与普通管理员均可见
  if (canResetPwd) {
    try {
      const resets: any[] = await http.get('/password-reset/list', { params: { status: 'PENDING' } }) || []
      pwdResetCount.value = resets.length
    } catch { pwdResetCount.value = 0 }
  }
}

function formatTs(value: any): string {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function fetchPwdResets() {
  pwdResetList.value = await http.get('/password-reset/list') || []
}

async function approvePwdReset(row: any) {
  if (!await confirmDialog({ message: `确认将 ${row.real_name || row.account} 的密码重置为手机号后 6 位吗？`, okText: '确认重置' })) return
  try {
    const res: any = await http.post(`/password-reset/${row.id}/approve`)
    showMessage(`已重置成功！\n新密码：${res?.newPassword}\n\n请通过电话/微信告知用户本人，提醒其登录后自行修改`, 'success', 8000)
    await fetchPwdResets()
    await fetchData()
  } catch (e: any) {
    showMessage('重置失败：' + (e?.message || '请稍后重试'))
  }
}

async function rejectPwdReset(row: any) {
  const remark = await promptDialog({ title: '驳回密码重置', placeholder: '请输入驳回原因（必填）', required: true })
  if (remark === null) return
  try {
    await http.post(`/password-reset/${row.id}/reject`, { remark })
    showMessage('已驳回')
    await fetchPwdResets()
    await fetchData()
  } catch (e: any) {
    showMessage('驳回失败：' + (e?.message || '请稍后重试'))
  }
}

async function fetchPending() {
  pendingList.value = await http.get('/registration/pending') || []
  // web 管理员注册默认审批为普通管理员（STAFF），小程序注册默认网格员
  for (const r of pendingList.value) {
    if (!approvalTypes.value[r.id]) {
      approvalTypes.value[r.id] = r.regSource === 'WEB' ? 'STAFF' : 'GRID_WORKER'
    }
  }
}

async function approveReg(row: any) {
  await http.post(`/registration/${row.id}/approve`, { remark: '审批通过', memberType: approvalTypes.value[row.id] || 'GRID_WORKER' })
  showMessage(row.regSource === 'WEB' ? '已通过，账号已获得普通管理员权限，可登录 web 管理端' : '已通过，用户已分配网格员身份并可登录 H5 端')
  delete approvalTypes.value[row.id]
  await fetchPending()
  await fetchData()
}

async function rejectReg(row: any) {
  const remark = await promptDialog({ title: '拒绝注册申请', placeholder: '请输入拒绝原因（必填）', required: true })
  if (remark === null) return
  await http.post(`/registration/${row.id}/reject`, { remark })
  showMessage('已拒绝')
  await fetchPending()
  await fetchData()
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
  form.value = { id: null, name: '', phone: '', position: '', memberType: 'GRID_WORKER', gridId: null, status: 'ACTIVE' }
}

function handleEdit(item: any) {
  form.value = { ...item }
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
  try {
    if (showEdit.value && form.value.id) {
      await http.put(`/community/org-members/${form.value.id}`, form.value)
      showMessage('保存成功')
    } else {
      await http.post('/community/org-members', form.value)
      showMessage('添加成功')
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
