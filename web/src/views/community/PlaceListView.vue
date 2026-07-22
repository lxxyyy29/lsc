<template>
  <section class="page-container">
    <header class="page-header">
      <h2>场所资源管理</h2>
      <el-button type="primary" @click="handleAdd">新增场所</el-button>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select v-model="filterForm.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" clearable check-strictly />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.placeType" placeholder="选择类型" clearable>
            <el-option label="九小场所" value="NINE_SMALL_SHOP" />
            <el-option label="学校" value="SCHOOL" />
            <el-option label="市场" value="MARKET" />
            <el-option label="物业小区" value="PROPERTY" />
            <el-option label="充电桩" value="CHARGING" />
            <el-option label="餐饮后厨" value="RESTAURANT" />
            <el-option label="工地" value="CONSTRUCTION" />
            <el-option label="商超" value="SUPERMARKET" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="场所名称/地址" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="placeName" label="场所名称" width="150" />
      <el-table-column prop="placeType" label="类型" width="100">
        <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.placeType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="address" label="地址" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" width="100" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="fireFacilities" label="消防设施" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增场所' : '编辑场所'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="场所名称" required><el-input v-model="form.placeName" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.placeType" placeholder="选择类型">
            <el-option label="九小场所" value="NINE_SMALL_SHOP" />
            <el-option label="学校" value="SCHOOL" />
            <el-option label="市场" value="MARKET" />
            <el-option label="物业小区" value="PROPERTY" />
            <el-option label="充电桩" value="CHARGING" />
            <el-option label="餐饮后厨" value="RESTAURANT" />
            <el-option label="工地" value="CONSTRUCTION" />
            <el-option label="商超" value="SUPERMARKET" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="消防设施"><el-input v-model="form.fireFacilities" /></el-form-item>
        <el-form-item label="所属网格">
          <el-tree-select v-model="form.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" check-strictly />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPlaces, createPlace, updatePlace, deletePlace, PlaceEntity } from '../../api/community'
import { getGridTree, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<PlaceEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const filterForm = reactive({ gridId: undefined as number | undefined, placeType: '', keyword: '' })
const defaultForm = (): PlaceEntity => ({ id: undefined, gridId: undefined, placeName: '', placeType: '', address: '', contactName: '', contactPhone: '', fireFacilities: '', remark: '' })
const form = reactive<PlaceEntity>(defaultForm())

function typeLabel(type?: string) {
  const map: Record<string, string> = { NINE_SMALL_SHOP: '九小场所', SCHOOL: '学校', MARKET: '市场', PROPERTY: '物业小区', CHARGING: '充电桩', RESTAURANT: '餐饮后厨', CONSTRUCTION: '工地', SUPERMARKET: '商超' }
  return map[type || ''] || '-'
}

async function loadData() {
  loading.value = true
  try {
    const data = await listPlaces(filterForm.gridId)
    let filtered = data
    if (filterForm.placeType) filtered = filtered.filter(p => p.placeType === filterForm.placeType)
    if (filterForm.keyword) {
      const kw = filterForm.keyword.toLowerCase()
      filtered = filtered.filter(p => (p.placeName && p.placeName.includes(kw)) || (p.address && p.address.includes(kw)))
    }
    tableData.value = filtered
  } finally { loading.value = false }
}

async function loadGridTree() { try { gridTree.value = await getGridTree() } catch (e) { console.error(e) } }
function handleSearch() { loadData() }
function handleReset() { filterForm.gridId = undefined; filterForm.placeType = ''; filterForm.keyword = ''; loadData() }
function handleAdd() { Object.assign(form, defaultForm()); dialogMode.value = 'add'; dialogVisible.value = true }
function handleEdit(row: PlaceEntity) { Object.assign(form, row); dialogMode.value = 'edit'; dialogVisible.value = true }

async function handleDelete(row: PlaceEntity) {
  await ElMessageBox.confirm(`确定删除场所「${row.placeName}」？`, '提示', { type: 'warning' })
  await deletePlace(row.id!); ElMessage.success('删除成功'); loadData()
}

async function handleSubmit() {
  if (!form.placeName) { ElMessage.warning('请输入场所名称'); return }
  if (dialogMode.value === 'add') { await createPlace(form); ElMessage.success('新增成功') }
  else { await updatePlace(form.id!, form); ElMessage.success('更新成功') }
  dialogVisible.value = false; loadData()
}

onMounted(() => { loadGridTree(); loadData() })
</script>

<style scoped>
.page-container { padding: 20px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 18px; color: var(--fg-text-primary); }
.page-filters { margin-bottom: 16px; padding: 16px; background: var(--fg-bg-card); border: 1px solid var(--fg-border); border-radius: var(--fg-radius-lg); }
:deep(.el-form-item__label) { color: var(--fg-text-secondary); }
:deep(.el-input__wrapper), :deep(.el-select__wrapper), :deep(.el-date-editor.el-input__wrapper), :deep(.el-textarea__inner) { background: var(--fg-bg-card-strong); border: 1px solid var(--fg-border); box-shadow: none; }
:deep(.el-input__inner), :deep(.el-select__placeholder), :deep(.el-textarea__inner) { color: var(--fg-text-primary); }
:deep(.el-table) { background: var(--fg-bg-card); color: var(--fg-text-primary); border: 1px solid var(--fg-border); border-radius: var(--fg-radius-lg); overflow: hidden; }
:deep(.el-table th.el-table__cell) { background: var(--fg-bg-card-strong); color: var(--fg-text-secondary); border-bottom: 1px solid var(--fg-border); }
:deep(.el-table td.el-table__cell) { background: var(--fg-bg-card); color: var(--fg-text-primary); border-bottom: 1px solid var(--fg-border); }
:deep(.el-table tr:hover > td.el-table__cell) { background: rgba(94, 162, 255, 0.08); }
:deep(.el-dialog) { background: var(--fg-bg-card); border: 1px solid var(--fg-border); border-radius: var(--fg-radius-lg); }
:deep(.el-dialog__title), :deep(.el-dialog__body) { color: var(--fg-text-primary); }
</style>
