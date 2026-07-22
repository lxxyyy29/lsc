<template>
  <section class="page-container">
    <header class="page-header">
      <h2>房屋/出租屋管理</h2>
      <el-button type="primary" @click="handleAdd">新增房屋</el-button>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select v-model="filterForm.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" clearable check-strictly />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="楼栋号/地址/房东" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="buildingNo" label="楼栋编号" width="100" />
      <el-table-column prop="address" label="地址" show-overflow-tooltip />
      <el-table-column prop="householdCount" label="分户数" width="80" />
      <el-table-column prop="landlordName" label="房东" width="100" />
      <el-table-column prop="landlordPhone" label="房东电话" width="130" />
      <el-table-column prop="fireRiskLevel" label="消防风险" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.fireRiskLevel === 'HIGH' ? 'danger' : row.fireRiskLevel === 'MEDIUM' ? 'warning' : 'success'">
            {{ riskLabel(row.fireRiskLevel) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="群租房" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.isGroupRental" size="small" type="danger">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增房屋' : '编辑房屋'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="楼栋编号" required><el-input v-model="form.buildingNo" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="分户数"><el-input-number v-model="form.householdCount" :min="0" /></el-form-item>
        <el-form-item label="房东姓名"><el-input v-model="form.landlordName" /></el-form-item>
        <el-form-item label="房东电话"><el-input v-model="form.landlordPhone" /></el-form-item>
        <el-form-item label="消防风险">
          <el-select v-model="form.fireRiskLevel" placeholder="选择等级">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="群租房"><el-checkbox v-model="form.isGroupRental" :true-label="1" :false-label="0" /></el-form-item>
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
import { listBuildings, createBuilding, updateBuilding, deleteBuilding, BuildingEntity } from '../../api/community'
import { getGridTree, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<BuildingEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const filterForm = reactive({ gridId: undefined as number | undefined, keyword: '' })
const defaultForm = (): BuildingEntity => ({ id: undefined, gridId: undefined, buildingNo: '', address: '', householdCount: 0, landlordName: '', landlordPhone: '', fireRiskLevel: 'LOW', isGroupRental: 0, remark: '' })
const form = reactive<BuildingEntity>(defaultForm())

function riskLabel(level?: string) { return level === 'HIGH' ? '高' : level === 'MEDIUM' ? '中' : level === 'LOW' ? '低' : '-' }

async function loadData() {
  loading.value = true
  try {
    const data = await listBuildings(filterForm.gridId)
    if (filterForm.keyword) {
      const kw = filterForm.keyword.toLowerCase()
      tableData.value = data.filter(b => (b.buildingNo && b.buildingNo.includes(kw)) || (b.address && b.address.includes(kw)) || (b.landlordName && b.landlordName.includes(kw)))
    } else { tableData.value = data }
  } finally { loading.value = false }
}

async function loadGridTree() { try { gridTree.value = await getGridTree() } catch (e) { console.error(e) } }
function handleSearch() { loadData() }
function handleReset() { filterForm.gridId = undefined; filterForm.keyword = ''; loadData() }
function handleAdd() { Object.assign(form, defaultForm()); dialogMode.value = 'add'; dialogVisible.value = true }
function handleEdit(row: BuildingEntity) { Object.assign(form, row); dialogMode.value = 'edit'; dialogVisible.value = true }

async function handleDelete(row: BuildingEntity) {
  await ElMessageBox.confirm(`确定删除房屋「${row.buildingNo}」？`, '提示', { type: 'warning' })
  await deleteBuilding(row.id!); ElMessage.success('删除成功'); loadData()
}

async function handleSubmit() {
  if (!form.buildingNo) { ElMessage.warning('请输入楼栋编号'); return }
  if (dialogMode.value === 'add') { await createBuilding(form); ElMessage.success('新增成功') }
  else { await updateBuilding(form.id!, form); ElMessage.success('更新成功') }
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
:deep(.el-radio__label) { color: var(--fg-text-primary); }
</style>
