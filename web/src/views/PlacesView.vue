<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">场所资源库</h2>
        <p style="font-size:13px;color:#6b7280;">九小场所、商铺、工地、物业小区等</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="showImport = true" class="filter-action ghost">
          <i class="fas fa-file-import"></i> 导入
        </button>
        <button @click="openCreate" class="filter-action">
          <i class="fas fa-plus"></i> 新增场所
        </button>
      </div>
    </div>
    <div class="card">
      <!-- 筛选栏 -->
      <div class="filter-bar" style="margin-bottom:16px;flex-wrap:wrap;">
        <input v-model="filterKeyword" class="form-input" placeholder="搜索名称/地址/联系人" style="width:220px;" @keyup.enter="fetchData" />
        <select v-model="filterType" class="filter-select" @change="fetchData">
          <option value="">全部场所类型</option>
          <option v-for="t in placeTypes" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="filterCategory" class="filter-select" @change="fetchData">
          <option value="">全部经营类别</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
        <button @click="fetchData" class="filter-action ghost">
          <i class="fas fa-search"></i> 查询
        </button>
        <button @click="resetFilters" class="filter-action ghost" style="border-color:#d1d5db;">
          <i class="fas fa-undo"></i> 重置
        </button>
      </div>
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
          <thead><tr><th>名称</th><th>类型</th><th>经营类别</th><th>地址</th><th>联系人</th><th>电话</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="(p, idx) in list" :key="p.id || ('ledger-' + idx)">
              <td>{{ p.placeName }}</td>
              <td><span class="tag tag-blue">{{ p.placeType || '-' }}</span></td>
              <td><span v-if="p.businessCategory" class="tag tag-green">{{ p.businessCategory }}</span><span v-else>-</span></td>
              <td>{{ p.address || '-' }}</td>
              <td>{{ p.contactName || '-' }}</td>
              <td>{{ p.contactPhone || '-' }}</td>
              <td>
                <div style="display:flex;gap:6px;">
                  <!-- 详情：台账行（无 id）也能看，直接展示该行已有字段 -->
                  <button @click="openDetail(p)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">详情</button>
                  <template v-if="p.id">
                    <button @click="openEdit(p)" class="btn btn-default" style="padding:4px 10px;font-size:12px;">编辑</button>
                    <button @click="handleDelete(p)" class="btn btn-danger" style="padding:4px 10px;font-size:12px;">删除</button>
                  </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="showForm" class="modal-overlay">
      <div class="modal-box" style="width:560px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">{{ form.id ? '编辑场所' : '新增场所' }}</h3>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:0 16px;">
          <div class="form-group">
            <label class="form-label">场所名称 <span class="required">*</span></label>
            <input v-model="form.placeName" class="form-input" placeholder="请输入场所名称" />
          </div>
          <div class="form-group">
            <label class="form-label">场所类型</label>
            <select v-model="form.placeType" class="form-select">
              <option value="">请选择</option>
              <option v-for="t in placeTypes" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">经营类别</label>
            <select v-model="form.businessCategory" class="form-select">
              <option value="">请选择</option>
              <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">联系人</label>
            <input v-model="form.contactName" class="form-input" placeholder="请输入联系人" />
          </div>
          <div class="form-group">
            <label class="form-label">联系电话</label>
            <input v-model="form.contactPhone" class="form-input" placeholder="请输入联系电话" />
          </div>
          <div class="form-group">
            <label class="form-label">消防设施</label>
            <input v-model="form.fireFacilities" class="form-input" placeholder="如：灭火器、烟感" />
          </div>
          <div class="form-group">
            <label class="form-label">风险标签</label>
            <input v-model="form.riskTags" class="form-input" placeholder="如：三合一、通道堵塞" />
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
            <option v-for="g in grids" :key="g.id" :value="Number(g.id)">{{ g.gridName }}</option>
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

    <!-- 场所详情弹窗（需求：不能查看场所详情 → 可查看详情，并显示类型、经营类别） -->
    <div v-if="detailVisible" class="modal-overlay" @click.self="detailVisible = false">
      <div class="modal-box" style="width:560px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">场所详情</h3>
        <div v-if="detailLoading" style="text-align:center;padding:30px;color:#9ca3af;">加载中...</div>
        <div v-else-if="detailItem" style="font-size:13px;color:#374151;line-height:2;">
          <div><span style="color:#9ca3af;">场所名称：</span>{{ detailItem.placeName || '-' }}</div>
          <div><span style="color:#9ca3af;">类型：</span>{{ detailItem.placeType || '-' }}</div>
          <div><span style="color:#9ca3af;">经营类别：</span>{{ detailItem.businessCategory || '-' }}</div>
          <div><span style="color:#9ca3af;">地址：</span>{{ detailItem.address || '-' }}</div>
          <div><span style="color:#9ca3af;">联系人：</span>{{ detailItem.contactName || '-' }}</div>
          <div><span style="color:#9ca3af;">联系电话：</span>{{ detailItem.contactPhone || '-' }}</div>
          <div><span style="color:#9ca3af;">消防设施：</span>{{ detailItem.fireFacilities || '-' }}</div>
          <div><span style="color:#9ca3af;">风险标签：</span>{{ detailItem.riskTags || '-' }}</div>
          <div><span style="color:#9ca3af;">所属网格：</span>{{ detailItem.gridName || '-' }}</div>
          <div><span style="color:#9ca3af;">状态：</span>{{ detailItem.status === 'ACTIVE' ? '启用' : (detailItem.status || '-') }}</div>
          <div><span style="color:#9ca3af;">备注：</span>{{ detailItem.remark || '-' }}</div>
          <div><span style="color:#9ca3af;">更新时间：</span>{{ detailItem.updatedAt || '-' }}</div>
        </div>
        <div style="display:flex;justify-content:flex-end;margin-top:16px;">
          <button @click="detailVisible = false" class="btn btn-default">关闭</button>
        </div>
      </div>
    </div>

    <!-- 导入对话框 -->
    <ImportDialog v-model:visible="showImport" type="places" @success="fetchData" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http, { getDictItems } from '../api'
import ImportDialog from '../components/ImportDialog.vue'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

// 场所类型：优先读字典 place_type（可在「系统设置 → 字典管理」里新增），
// 字典无配置时回退到内置列表（缺陷 BUG-07：经营类别和类型也要能在字典内创建）
const PLACE_TYPE_DICT = 'place_type'
const PLACE_CATEGORY_DICT = 'place_business_category'
const placeTypes = ref<string[]>(['出租屋', '小档口', '小娱乐场所', '小作坊', '工业园', '住宅小区', '其他场所'])

const list = ref<any[]>([])
const grids = ref<any[]>([])
const categories = ref<string[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const showImport = ref(false)
const showForm = ref(false)
// 筛选条件
const filterKeyword = ref('')
const filterType = ref('')
const filterCategory = ref('')

const emptyForm = () => ({
  id: null as number | null,
  placeName: '', placeType: '', businessCategory: '', address: '',
  contactName: '', contactPhone: '',
  fireFacilities: '', riskTags: '',
  gridId: null as number | null, remark: '',
  status: 'ACTIVE',
})
const form = ref(emptyForm())

// ===== 场所详情（需求：不能查看场所详情）======
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailItem = ref<any>(null)

/** 打开详情：台账合并行没有 id，直接用行数据展示；正式场所记录再拉一次详情接口取最新值 */
async function openDetail(row: any) {
  detailItem.value = { ...row }
  detailVisible.value = true
  if (!row?.id) return
  detailLoading.value = true
  try {
    const detail = await http.get(`/community/places/${row.id}`)
    if (detail) detailItem.value = { ...row, ...detail }
  } catch (e) {
    // 详情接口失败时保留列表行数据，不影响查看
  } finally {
    detailLoading.value = false
  }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = {}
    if (filterKeyword.value.trim()) params.keyword = filterKeyword.value.trim()
    if (filterType.value) params.placeType = filterType.value
    if (filterCategory.value) params.businessCategory = filterCategory.value
    list.value = await http.get('/community/places', { params }) || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filterKeyword.value = ''
  filterType.value = ''
  filterCategory.value = ''
  fetchData()
}

/** 字典项 → 选项值（按 sortOrder 排序，取 itemValue 作为存库值，与历史数据一致） */
function dictValues(items: any): string[] {
  if (!Array.isArray(items)) return []
  return items
    .filter((i: any) => (i.status ?? 'ACTIVE') === 'ACTIVE')
    .sort((a: any, b: any) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0))
    .map((i: any) => String(i.itemValue ?? i.itemLabel ?? '').trim())
    .filter(Boolean)
}

/** 场所类型 / 经营类别读字典：管理员在字典管理里新增即可扩展（缺陷 BUG-07） */
async function loadDictOptions() {
  try {
    const values = dictValues(await getDictItems(PLACE_TYPE_DICT, true))
    if (values.length) placeTypes.value = values
  } catch (e) { /* 无权限或字典未配置：保留内置列表 */ }
  try {
    const values = dictValues(await getDictItems(PLACE_CATEGORY_DICT, true))
    if (values.length) categories.value = values
  } catch (e) { /* 回退到台账推导 */ }
}

async function fetchCategories() {
  // 先读字典（管理员可维护）；字典没配置时再用台账里已有的经营类别兜底
  await loadDictOptions()
  if (categories.value.length) return
  try {
    categories.value = await http.get('/community/places/categories') || []
  } catch(e) {}
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
    placeName: p.placeName || '', placeType: p.placeType || '', businessCategory: p.businessCategory || '', address: p.address || '',
    contactName: p.contactName || '', contactPhone: p.contactPhone || '',
    fireFacilities: p.fireFacilities || '', riskTags: p.riskTags || '',
    gridId: p.gridId || null, remark: p.remark || '',
    status: p.status || 'ACTIVE',
  }
  showForm.value = true
}

async function handleSubmit() {
  if (!form.value.placeName.trim()) { showMessage('请输入场所名称'); return }
  saving.value = true
  try {
    const payload: any = { ...form.value }
    if (!payload.gridId) payload.gridId = null
    if (form.value.id) {
      await http.put(`/community/places/${form.value.id}`, payload)
      showMessage('保存成功')
    } else {
      await http.post('/community/places', payload)
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
  if (!await confirmDialog({ message: `确定删除场所「${p.placeName}」吗？删除后不可恢复。`, danger: true, okText: '删除' })) return
  try {
    await http.delete(`/community/places/${p.id}`)
    showMessage('删除成功')
    fetchData()
  } catch(e: any) {
    showMessage(e?.message || '删除失败')
  }
}

onMounted(() => {
  fetchData()
  fetchGrids()
  fetchCategories()
})
</script>