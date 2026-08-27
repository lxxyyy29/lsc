<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">实有人口库</h2>
        <p style="font-size:13px;color:#6b7280;">常住人口、流动人口</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="openFieldConfig" class="filter-action ghost">
          <i class="fas fa-cog"></i> 字段配置
        </button>
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

    <!-- 常驻 / 流动 Tab -->
    <div style="display:flex;gap:8px;margin-bottom:16px;border-bottom:2px solid #e5e7eb;padding-bottom:12px;">
      <button v-for="tab in populationTabs" :key="tab.value" @click="switchTab(tab.value)"
              :class="['filter-btn', activeTab === tab.value ? 'active' : '']">
        {{ tab.label }}
      </button>
    </div>

    <!-- 统一筛选栏：关键字模糊搜索 + 户籍类型(仅常驻) + 网格 -->
    <div class="card" style="padding:16px 24px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <input v-model="filters.keyword" class="filter-input" style="width:220px;"
               placeholder="模糊搜索姓名 / 电话 / 地址 / 身份证" @keyup.enter="fetchData" />
        <select v-if="isResidentTab" v-model="filters.householdType" class="filter-select">
          <option value="">全部户籍类型</option>
          <option v-for="t in residentHouseholdTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
        </select>
        <select v-model="filters.gridId" class="filter-select">
          <option :value="null">全部网格</option>
          <option v-for="g in grids" :key="g.id" :value="Number(g.id)">{{ g.gridName }}</option>
        </select>
        <button @click="fetchData" class="filter-action"><i class="fas fa-search"></i> 查询</button>
        <button @click="resetFilters" class="filter-action ghost">重置</button>
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
          <thead><tr>
            <th>姓名</th><th>性别</th><th>年龄</th><th>电话</th>
            <th v-if="isResidentTab">户籍类型</th>
            <th v-if="isResidentTab">特殊人群</th>
            <th v-if="isResidentTab">与户主关系</th>
            <th>地址</th><th>楼栋/房号</th><th>网格</th><th>操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="p in list" :key="p.id" :style="isHead(p) ? 'background:#fffbe6;' : ''">
              <td>
                {{ p.name }}
                <span v-if="isHead(p)" class="tag tag-red" style="margin-left:4px;">户主</span>
              </td>
              <td>{{ p.gender || '-' }}</td>
              <td>{{ p.age != null ? p.age : '-' }}</td>
              <td>{{ p.phone || '-' }}</td>
              <td v-if="isResidentTab"><span class="tag tag-blue">{{ getHouseholdTypeName(p.householdType) }}</span></td>
              <td v-if="isResidentTab">
                <span v-if="p.specialPopulation == 1" class="tag tag-orange">{{ p.specialPopulationType || '特殊人群' }}</span>
                <span v-else>-</span>
              </td>
              <td v-if="isResidentTab">{{ p.relation || '-' }}</td>
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

    <!-- 新增/编辑弹窗（按字段配置动态渲染） -->
    <div v-if="showForm" class="modal-overlay">
      <div class="modal-box" style="width:600px;max-height:88vh;overflow-y:auto;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ form.id ? '编辑人员' : '新增人员' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px;">
          <template v-for="f in formFields" :key="f.fieldKey">
            <div v-if="isFormVisible(f)" class="form-group" :style="f.fieldKey === 'address' || f.fieldKey === 'gridId' || f.fieldKey === 'remark' ? 'grid-column:1 / -1;' : ''">
              <label class="form-label">{{ f.fieldLabel }} <span v-if="f.required" class="required">*</span></label>
              <!-- 特殊人群勾选 -->
              <div v-if="f.fieldKey === 'special_population'" class="form-checkbox">
                <label style="display:flex;align-items:center;gap:6px;cursor:pointer;">
                  <input type="checkbox" :checked="form.specialPopulation == 1" @change="form.specialPopulation = ($event.target as HTMLInputElement).checked ? 1 : 0" />
                  是特殊人群
                </label>
              </div>
              <!-- 户籍类型：流动库隐藏 -->
              <select v-else-if="f.fieldKey === 'householdType'" v-model="form.householdType" class="form-select">
                <option value="">请选择</option>
                <option v-for="t in residentHouseholdTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
              <!-- 出生日期 -->
              <input v-else-if="f.fieldType === 'date'" v-model="form.birthday" type="date" class="form-input" @change="autoFillAge" />
              <!-- 特殊人群类型 / 关系下拉（支持自定义） -->
              <div v-else-if="f.fieldKey === 'special_population_type' || f.fieldKey === 'relation'" class="form-custom-select">
                <select v-if="!customEditing[f.fieldKey]" v-model="form[f.fieldKey]" class="form-select" @change="startCustom(f.fieldKey, String(form[f.fieldKey]))">
                  <option value="">请选择</option>
                  <option v-for="opt in selectOptions(f)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <div v-else style="display:flex;gap:6px;align-items:center;">
                  <input v-model="form[f.fieldKey]" class="form-input" :placeholder="'请输入' + f.fieldLabel" style="flex:1;" @keyup.enter="customEditing[f.fieldKey] = false" />
                  <button type="button" class="btn btn-default" style="padding:4px 10px;font-size:12px;" @click="customEditing[f.fieldKey] = false">确定</button>
                </div>
              </div>
              <!-- 性别/户籍下拉 -->
              <select v-else-if="f.fieldType === 'select'" v-model="form[f.fieldKey]" class="form-select">
                <option value="">请选择</option>
                <option v-for="opt in selectOptions(f)" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
              <!-- 文本域 -->
              <textarea v-else-if="f.fieldType === 'textarea'" v-model="form[f.fieldKey]" class="form-textarea" rows="2" placeholder="选填"></textarea>
              <!-- 通用输入 -->
              <input v-else v-model="form[f.fieldKey]" class="form-input" :placeholder="'请输入' + f.fieldLabel" />
            </div>
          </template>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:8px;">
          <button @click="showForm = false" class="btn btn-default">取消</button>
          <button @click="handleSubmit" class="btn btn-primary" :disabled="saving">{{ form.id ? '保存' : '添加' }}</button>
        </div>
      </div>
    </div>

    <!-- 字段配置弹窗 -->
    <div v-if="showConfig" class="modal-overlay">
      <div class="modal-box" style="width:560px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">字段配置</h3>
        <p style="font-size:12px;color:#6b7280;margin-bottom:12px;">勾选启用的字段将显示在新增/编辑表单中，可调整排序与必填。（导入模板列固定，不受此配置影响）</p>
        <div style="max-height:55vh;overflow-y:auto;">
          <div v-for="(f, idx) in configFields" :key="f.id" style="display:flex;align-items:center;gap:8px;padding:8px 4px;border-bottom:1px solid #f3f4f6;">
            <input type="checkbox" v-model="f.enabled" style="accent-color:#1890ff;" />
            <input v-model="f.fieldLabel" class="form-input" style="width:120px;padding:4px 8px;font-size:12px;" />
            <span style="flex:1;font-size:12px;color:#9ca3af;">{{ f.fieldKey }}</span>
            <label style="display:flex;align-items:center;gap:4px;font-size:12px;color:#6b7280;cursor:pointer;">
              <input type="checkbox" v-model="f.required" style="accent-color:#ff4d4f;" />必填
            </label>
            <div style="display:flex;gap:4px;">
              <button @click="moveConfig(idx, -1)" :disabled="idx === 0" class="btn btn-default" style="padding:2px 8px;font-size:11px;">↑</button>
              <button @click="moveConfig(idx, 1)" :disabled="idx === configFields.length - 1" class="btn btn-default" style="padding:2px 8px;font-size:11px;">↓</button>
            </div>
          </div>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showConfig = false" class="btn btn-default">取消</button>
          <button @click="saveConfig" class="btn btn-primary" :disabled="saving">{{ saving ? '保存中...' : '保存配置' }}</button>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <ImportDialog v-model:visible="showImport" type="population" :columns="importColumns" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import http from '../api'
import { getHouseholdTypeName } from '../utils/eventTypes'
import { getFormFieldConfig, saveFormFieldConfig } from '../api'
import ImportDialog from '../components/ImportDialog.vue'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

// 常驻 / 流动 Tab
const populationTabs = [
  { value: 'RESIDENT', label: '常驻人口' },
  { value: 'FLOATING', label: '流动人口' },
]
const activeTab = ref<'RESIDENT' | 'FLOATING'>('RESIDENT')
const isResidentTab = computed(() => activeTab.value === 'RESIDENT')

// 户籍类型（常驻专用；流动库取消）
const residentHouseholdTypes = [
  { value: 'LOCAL', label: '本地户籍' },
  { value: 'NON_LOCAL', label: '外地户籍' },
  { value: 'LOW_INCOME', label: '低保户' },
  { value: 'SPECIAL_CARE', label: '优抚对象' },
  { value: 'OTHER', label: '其他' },
]

// 特殊人群类型预置 + 自定义
const specialPopulationTypes = ['低保户', '优抚对象', '残疾人', '孤寡老人', '困境儿童']
// 与户主关系预置
const relationTypes = ['户主', '配偶', '儿子', '女儿', '父亲', '母亲']

const list = ref<any[]>([])
const grids = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const showImport = ref(false)
const showForm = ref(false)
const showConfig = ref(false)

// 字段配置器数据
const configFields = ref<any[]>([])
const formFields = computed(() => configFields.value.filter(f => f.enabled == 1))

// 导入列：固定完整列序（与后端 ImportService 固定索引 0..9 对齐），避免动态缩减导致列错位
const IMPORT_COLUMNS = ['name', 'idCard', 'phone', 'householdType', 'specialPopulation', 'specialPopulationType', 'relation', 'address', 'gridName', 'tags']
const importColumns = computed(() => IMPORT_COLUMNS)

const filters = reactive({
  keyword: '',
  householdType: '',
  gridId: null as number | null,
})

const emptyForm = () => ({
  id: null as number | null,
  name: '', gender: '', age: null as number | null, phone: '', idCard: '', birthday: '',
  householdType: '', specialPopulation: 0, specialPopulationType: '', relation: '',
  address: '', buildingNo: '', roomNo: '',
  gridId: null as number | null, remark: '',
  status: 'ACTIVE',
})
const form = ref(emptyForm())

// 字段配置器中 select 类型解析选项
function selectOptions(f: any) {
  if (f.fieldKey === 'special_population_type') {
    return [...specialPopulationTypes.map(v => ({ value: v, label: v })), { value: '__custom__', label: '自定义...' }]
  }
  if (f.fieldKey === 'relation') {
    return [...relationTypes.map(v => ({ value: v, label: v })), { value: '__custom__', label: '自定义...' }]
  }
  if (f.fieldKey === 'householdType') return residentHouseholdTypes
  if (f.options) {
    return f.options.split(',').map((o: string) => {
      const [value, label] = o.split(':')
      return { value: value || o, label: label || value || o }
    })
  }
  return []
}

// 自定义输入模式（特殊人群类型/关系选中"自定义..."时切换为内联输入）
const customEditing = reactive<Record<string, boolean>>({})
function startCustom(field: 'special_population_type' | 'relation', val: string) {
  if (val === '__custom__') {
    form.value[field] = ''
    customEditing[field] = true
  }
}

// 是否在表单中显示该字段（流动库隐藏户籍类型）
function isFormVisible(f: any) {
  if (f.fieldKey === 'householdType' && !isResidentTab.value) return false
  return true
}

// 是否户主（用于列表高亮）
function isHead(p: any) {
  return isResidentTab.value && p.relation === '户主'
}

// 出生日期自动推算年龄
function autoFillAge() {
  if (!form.value.birthday) return
  const b = new Date(form.value.birthday)
  if (isNaN(b.getTime())) return
  const now = new Date()
  let age = now.getFullYear() - b.getFullYear()
  const m = now.getMonth() - b.getMonth()
  if (m < 0 || (m === 0 && now.getDate() < b.getDate())) age--
  form.value.age = age >= 0 ? age : null
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { populationType: activeTab.value }
    if (filters.keyword.trim()) params.keyword = filters.keyword.trim()
    if (isResidentTab.value && filters.householdType) params.householdType = filters.householdType
    if (filters.gridId) params.gridId = filters.gridId
    list.value = await http.get('/community/population', { params }) || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function switchTab(tab: 'RESIDENT' | 'FLOATING') {
  if (activeTab.value === tab) return
  activeTab.value = tab
  resetFilters()
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

async function loadFieldConfig() {
  try {
    configFields.value = await getFormFieldConfig('population') || []
  } catch(e) {
    configFields.value = []
  }
}

function openFieldConfig() {
  showConfig.value = true
}

function moveConfig(idx: number, dir: number) {
  const target = idx + dir
  if (target < 0 || target >= configFields.value.length) return
  const arr = configFields.value
  const tmp = arr[idx]
  arr[idx] = arr[target]
  arr[target] = tmp
  arr.forEach((f, i) => { f.sortOrder = i + 1 })
}

async function saveConfig() {
  saving.value = true
  try {
    await saveFormFieldConfig(configFields.value.map((f, i) => ({ ...f, sortOrder: i + 1 })))
    showMessage('配置已保存')
    showConfig.value = false
    await loadFieldConfig()
  } catch(e: any) {
    showMessage(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function openCreate() {
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(p: any) {
  form.value = {
    id: p.id,
    name: p.name || '', gender: p.gender || '', age: p.age != null ? p.age : null,
    phone: p.phone || '', idCard: p.idCard || '', birthday: p.birthday || '',
    householdType: p.householdType || '',
    specialPopulation: p.specialPopulation || 0,
    specialPopulationType: p.specialPopulationType || '',
    relation: p.relation || '',
    address: p.address || '', buildingNo: p.buildingNo || '', roomNo: p.roomNo || '',
    gridId: p.gridId || null, remark: p.remark || '',
    status: p.status || 'ACTIVE',
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!form.value.name.trim()) { showMessage('请输入姓名'); return }
  if (!form.value.idCard.trim()) { showMessage('身份证号必填'); return }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    if (!payload.age) payload.age = null
    if (!payload.birthday) payload.birthday = null
    if (!payload.gridId) payload.gridId = null
    if (!isResidentTab.value) payload.householdType = 'FLOATING'
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

// 导出：携带当前 tab/户籍/网格/关键字筛选条件
async function exportData() {
  const session = JSON.parse(localStorage.getItem('grid-session') || '{}')
  const qs = new URLSearchParams()
  qs.set('populationType', activeTab.value)
  if (filters.keyword.trim()) qs.set('keyword', filters.keyword.trim())
  if (isResidentTab.value && filters.householdType) qs.set('householdType', filters.householdType)
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
    a.download = activeTab.value === 'RESIDENT' ? '常驻人口台账.xlsx' : '流动人口台账.xlsx'
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
  loadFieldConfig()
})
</script>
