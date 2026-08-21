<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">实有人口库</h2>
        <p style="font-size:13px;color:#6b7280;">常住人口、流动人口、出租屋台账</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="showImport = true" class="filter-action ghost">
          <i class="fas fa-file-import"></i> 导入
        </button>
        <button @click="exportData" class="filter-action ghost">
          <i class="fas fa-download"></i> 导出Excel
        </button>
        <button @click="openCreate" class="filter-action">
          <i class="fas fa-plus"></i> 新增人员
        </button>
      </div>
    </div>

    <!-- 统一筛选栏：关键字模糊搜索 + 户籍类型 + 网格 -->
    <div class="card" style="padding:16px 24px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <input v-model="filters.keyword" class="filter-input" style="width:240px;"
               placeholder="模糊搜索姓名 / 电话 / 地址 / 身份证" @keyup.enter="fetchData" />
        <select v-model="filters.householdType" class="filter-select">
          <option value="">全部户籍类型</option>
          <option v-for="t in householdTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
        <select v-model="filters.gridId" class="filter-select">
          <option :value="null">全部网格</option>
          <option v-for="g in grids" :key="g.id" :value="g.id">{{ g.gridName }}</option>
        </select>
        <button @click="fetchData" class="filter-action"><i class="fas fa-search"></i> 查询</button>
        <button @click="resetFilters" class="filter-action ghost">重置</button>
      </div>
    </div>

    <div class="card">
      <!-- 加载中 -->
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <!-- 错误提示 -->
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <!-- 数据表格 -->
      <template v-else>
        <table class="table">
          <thead><tr><th>姓名</th><th>性别</th><th>电话</th><th>户籍类型</th><th>地址</th><th>楼栋/房号</th><th>网格</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="p in list" :key="p.id">
              <td>{{ p.name }}</td>
              <td>{{ p.gender || '-' }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td><span class="tag tag-blue">{{ getHouseholdTypeName(p.householdType) }}</span></td>
              <td>{{ p.address || '-' }}</td>
              <td>{{ p.buildingNo ? p.buildingNo + (p.roomNo ? '-' + p.roomNo : '') : '-' }}</td>
              <td>{{ p.gridName || '-' }}</td>
              <td>
                <div style="display:flex;gap:6px;">
                  <button @click="openEdit(p)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                  <button @click="handleDelete(p)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-box" style="width:560px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ form.id ? '编辑人员' : '新增人员' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px;">
          <div class="form-group">
            <label class="form-label">姓名 <span class="required">*</span></label>
            <input v-model="form.name" class="form-input" placeholder="请输入姓名" />
          </div>
          <div class="form-group">
            <label class="form-label">性别</label>
            <select v-model="form.gender" class="form-select">
              <option value="">请选择</option>
              <option value="男">男</option>
              <option value="女">女</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">联系电话</label>
            <input v-model="form.phone" class="form-input" placeholder="请输入电话" />
          </div>
          <div class="form-group">
            <label class="form-label">身份证号</label>
            <input v-model="form.idCard" class="form-input" placeholder="请输入身份证号" />
          </div>
          <div class="form-group">
            <label class="form-label">出生日期</label>
            <input v-model="form.birthday" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">户籍类型</label>
            <select v-model="form.householdType" class="form-select">
              <option value="">请选择</option>
              <option v-for="t in householdTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">楼栋号</label>
            <input v-model="form.buildingNo" class="form-input" placeholder="如：3栋" />
          </div>
          <div class="form-group">
            <label class="form-label">房号</label>
            <input v-model="form.roomNo" class="form-input" placeholder="如：502" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">居住地址</label>
          <input v-model="form.address" class="form-input" placeholder="请输入居住地址" />
        </div>
        <div class="form-group">
          <label class="form-label">所属网格</label>
          <select v-model="form.gridId" class="form-select" style="width:100%;">
            <option :value="null">请选择网格</option>
            <option v-for="g in grids" :key="g.id" :value="g.id">{{ g.gridName }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea v-model="form.remark" class="form-textarea" rows="2" placeholder="选填"></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:8px;">
          <button @click="showForm = false" class="btn btn-default">取消</button>
          <button @click="handleSubmit" class="btn btn-primary" :disabled="saving">{{ form.id ? '保存' : '添加' }}</button>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <ImportDialog v-model:visible="showImport" type="population" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import http from '../api'
import { getHouseholdTypeName } from '../utils/eventTypes'
import ImportDialog from '../components/ImportDialog.vue'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

const householdTypes = [
  { value: 'LOCAL', label: '本地户籍' },
  { value: 'NON_LOCAL', label: '外地户籍' },
  { value: 'FLOATING', label: '流动人口' },
  { value: 'LOW_INCOME', label: '低保户' },
  { value: 'SPECIAL_CARE', label: '优抚对象' },
  { value: 'OTHER', label: '其他' },
]

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const showImport = ref(false)
const showForm = ref(false)

const filters = reactive({
  keyword: '',
  householdType: '',
  gridId: null as number | null,
})

const emptyForm = () => ({
  id: null as number | null,
  name: '', gender: '', phone: '', idCard: '', birthday: '',
  householdType: '', address: '', buildingNo: '', roomNo: '',
  gridId: null as number | null, remark: '',
})
const form = ref(emptyForm())

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = {}
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (filters.householdType) params.householdType = filters.householdType
    if (filters.gridId) params.gridId = filters.gridId
    list.value = await http.get('/community/population', { params }) || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.keyword = ''
  filters.householdType = ''
  filters.gridId = null
  fetchData()
}

async function fetchGrids() {
  try {
    const tree = await http.get('/community/grids/tree') || []
    const all: any[] = []
    const walk = (nodes: any[]) => {
      for (const n of nodes) {
        all.push(n)
        if (n.children) walk(n.children)
      }
    }
    walk(tree)
    grids.value = all
  } catch(e) {}
}

function openCreate() {
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(p: any) {
  form.value = {
    id: p.id,
    name: p.name || '', gender: p.gender || '', phone: p.phone || '',
    idCard: p.idCard || '', birthday: p.birthday || '',
    householdType: p.householdType || '', address: p.address || '',
    buildingNo: p.buildingNo || '', roomNo: p.roomNo || '',
    gridId: p.gridId || null, remark: p.remark || '',
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) { showMessage('请输入姓名'); return }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    if (!payload.birthday) payload.birthday = null
    if (!payload.gridId) payload.gridId = null
    if (form.value.id) {
      await http.put(`/community/population/${form.value.id}`, payload)
      showMessage('保存成功')
    } else {
      await http.post('/community/population', payload)
      showMessage('添加成功')
    }
    showForm.value = false
    await fetchData()
  } catch(e: any) {
    showMessage(e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(p: any) {
  if (!await confirmDialog({ message: `确定删除人员「${p.name}」吗？删除后不可恢复。`, danger: true, okText: '删除' })) return
  try {
    await http.delete(`/community/population/${p.id}`)
    showMessage('删除成功')
    fetchData()
  } catch(e: any) {
    showMessage(e?.message || '删除失败')
  }
}

// 导出：携带当前户籍类型/网格/关键字筛选条件，原生 fetch 带鉴权下载
async function exportData() {
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  const qs = new URLSearchParams()
  if (filters.keyword.trim()) qs.set('keyword', filters.keyword.trim())
  if (filters.householdType) qs.set('householdType', filters.householdType)
  if (filters.gridId) qs.set('gridId', String(filters.gridId))
  try {
    const res = await fetch(`/api/community/population/export?${qs.toString()}`, {
      headers: { Authorization: `Bearer ${session.token}` }
    })
    if (!res.ok) {
      showMessage(res.status === 401 ? '登录已过期，请重新登录' : '导出失败，请稍后重试')
      return
    }
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '实有人口台账.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
    showMessage('导出失败，请检查网络')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
})
</script>
