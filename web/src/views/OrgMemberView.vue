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
        <button @click="showApproval = true; fetchPending()" class="btn btn-default">
          <i class="fas fa-user-check"></i>注册审批
          <span v-if="pendingCount > 0" style="background:#dc2626;color:#fff;border-radius:10px;padding:1px 6px;font-size:11px;margin-left:4px;">{{ pendingCount }}</span>
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
          <thead><tr><th>姓名</th><th>电话</th><th>类型</th><th>职务</th><th>所属网格</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="p in list" :key="p.id">
              <td>{{ p.name }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td><span class="tag tag-blue">{{ memberTypeLabel(p.memberType) }}</span></td>
              <td>{{ p.position || '-' }}</td>
              <td>{{ p.gridName || '-' }}</td>
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
          <thead><tr><th>用户名</th><th>姓名</th><th>手机号</th><th>申请时间</th><th>身份</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="r in pendingList" :key="r.id">
              <td>{{ r.username }}</td>
              <td>{{ r.realName }}</td>
              <td>{{ r.phone }}</td>
              <td>{{ r.createdAt }}</td>
              <td>
                <select v-model="approvalTypes[r.id]" class="form-select" style="padding:3px 8px;font-size:12px;">
                  <option value="GRID_WORKER">网格员</option>
                  <option value="STAFF">社区工作人员</option>
                </select>
              </td>
              <td>
                <button @click="approveReg(r)" class="btn btn-primary" style="padding:4px 10px;font-size:12px;">通过</button>
                <button @click="rejectReg(r)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">拒绝</button>
              </td>
            </tr>
            <tr v-if="!pendingList.length"><td colspan="6" style="text-align:center;color:#999;padding:20px;">暂无待审批</td></tr>
          </tbody>
        </table>
        <div style="text-align:right;margin-top:16px;">
          <button @click="showApproval = false" class="btn btn-default">关闭</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http, { syncGridWorkersToOrgMembers } from '../api'

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

async function handleSync() {
  if (!confirm('将系统用户中的网格员同步到组织人员表，继续吗？')) return
  try {
    const res: any = await syncGridWorkersToOrgMembers()
    alert(res?.message || '同步完成')
    await fetchData()
  } catch(e: any) {
    alert('同步失败：' + (e?.message || '未知错误'))
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get('/community/org-members') || []
    // 获取待审批数量
    const pending = await http.get('/registration/pending') || []
    pendingCount.value = pending.length
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function fetchPending() {
  pendingList.value = await http.get('/registration/pending') || []
}

async function approveReg(row: any) {
  await http.post(`/registration/${row.id}/approve`, { remark: '审批通过', memberType: approvalTypes.value[row.id] || 'GRID_WORKER' })
  alert('已通过，用户已分配网格员身份并可登录 H5 端')
  delete approvalTypes.value[row.id]
  await fetchPending()
  await fetchData()
}

async function rejectReg(row: any) {
  const remark = prompt('拒绝原因：')
  if (remark === null) return
  await http.post(`/registration/${row.id}/reject`, { remark })
  alert('已拒绝')
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
  if (!confirm(`确定要删除网格员「${item.name}」吗？`)) return
  try {
    await http.delete(`/community/org-members/${item.id}`)
    alert('删除成功')
    fetchData()
  } catch(e: any) {
    alert(e?.message || '删除失败')
  }
}

async function handleSubmit() {
  if (!form.value.name) { alert('请输入姓名'); return }
  if (form.value.memberType === 'GRID_WORKER' && !form.value.gridId) { alert('请选择所属小网格'); return }
  try {
    if (showEdit.value && form.value.id) {
      await http.put(`/community/org-members/${form.value.id}`, form.value)
      alert('保存成功')
    } else {
      await http.post('/community/org-members', form.value)
      alert('添加成功')
    }
    closeModal()
    await fetchData()
  } catch(e: any) {
    alert(e?.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
})
</script>
