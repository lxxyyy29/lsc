<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">房屋/出租屋库</h2>
        <p style="font-size:13px;color:#6b7280;">楼栋、分户信息、房东租客、消防隐患</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="showImport = true" class="filter-action ghost">
          <i class="fas fa-file-import"></i> 导入
        </button>
        <button @click="openCreate" class="filter-action">
          <i class="fas fa-plus"></i> 新增房屋
        </button>
      </div>
    </div>
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead><tr><th>楼栋编号</th><th>地址</th><th>房东</th><th>消防风险</th><th>群租房</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="b in list" :key="b.id">
              <td>{{ b.buildingNo }}</td>
              <td>{{ b.address || '-' }}</td>
              <td>{{ b.landlordName || '-' }}</td>
              <td><span :class="['tag', b.fireRiskLevel === 'HIGH' ? 'tag-red' : b.fireRiskLevel === 'MEDIUM' ? 'tag-orange' : 'tag-green']">{{ fireRiskLabel(b.fireRiskLevel) }}</span></td>
              <td>{{ b.isGroupRental ? '是' : '否' }}</td>
              <td>
                <div style="display:flex;gap:6px;">
                  <button @click="openEdit(b)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                  <button @click="handleDelete(b)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">删除</button>
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
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ form.id ? '编辑房屋' : '新增房屋' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px;">
          <div class="form-group">
            <label class="form-label">楼栋编号 <span class="required">*</span></label>
            <input v-model="form.buildingNo" class="form-input" placeholder="如：3栋" />
          </div>
          <div class="form-group">
            <label class="form-label">户数</label>
            <input v-model="form.householdCount" type="number" min="0" class="form-input" placeholder="如：12" />
          </div>
          <div class="form-group">
            <label class="form-label">房东姓名</label>
            <input v-model="form.landlordName" class="form-input" placeholder="请输入房东姓名" />
          </div>
          <div class="form-group">
            <label class="form-label">房东电话</label>
            <input v-model="form.landlordPhone" class="form-input" placeholder="请输入房东电话" />
          </div>
          <div class="form-group">
            <label class="form-label">消防风险等级</label>
            <select v-model="form.fireRiskLevel" class="form-select">
              <option value="">请选择</option>
              <option value="LOW">低风险</option>
              <option value="MEDIUM">中风险</option>
              <option value="HIGH">高风险</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">是否群租房</label>
            <select v-model="form.isGroupRental" class="form-select">
              <option :value="0">否</option>
              <option :value="1">是</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">地址</label>
          <input v-model="form.address" class="form-input" placeholder="请输入地址" />
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
    <ImportDialog v-model:visible="showImport" type="buildings" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'
import ImportDialog from '../components/ImportDialog.vue'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const showImport = ref(false)
const showForm = ref(false)

const emptyForm = () => ({
  id: null as number | null,
  buildingNo: '', address: '', householdCount: null as number | null,
  landlordName: '', landlordPhone: '', fireRiskLevel: '',
  isGroupRental: 0 as number, gridId: null as number | null, remark: '',
  status: 'ACTIVE',
})
const form = ref(emptyForm())

function fireRiskLabel(level: string) {
  if (level === 'HIGH') return '高风险'
  if (level === 'MEDIUM') return '中风险'
  if (level === 'LOW') return '低风险'
  return level || '-'
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    list.value = await http.get('/community/buildings') || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
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

function openEdit(b: any) {
  form.value = {
    id: b.id,
    buildingNo: b.buildingNo || '', address: b.address || '',
    householdCount: b.householdCount || null,
    landlordName: b.landlordName || '', landlordPhone: b.landlordPhone || '',
    fireRiskLevel: b.fireRiskLevel || '',
    isGroupRental: b.isGroupRental ? 1 : 0,
    gridId: b.gridId || null, remark: b.remark || '',
    status: b.status || 'ACTIVE',
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!form.value.buildingNo.trim()) { showMessage('请输入楼栋编号'); return }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    if (!payload.householdCount) payload.householdCount = null
    if (!payload.gridId) payload.gridId = null
    if (form.value.id) {
      await http.put(`/community/buildings/${form.value.id}`, payload)
      showMessage('保存成功')
    } else {
      await http.post('/community/buildings', payload)
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

async function handleDelete(b: any) {
  if (!await confirmDialog({ message: `确定删除房屋「${b.buildingNo}」吗？删除后不可恢复。`, danger: true, okText: '删除' })) return
  try {
    await http.delete(`/community/buildings/${b.id}`)
    showMessage('删除成功')
    fetchData()
  } catch(e: any) {
    showMessage(e?.message || '删除失败')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
})
</script>
