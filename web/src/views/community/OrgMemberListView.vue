<template>
  <section class="page-container">
    <header class="page-header">
      <h2>组织力量管理</h2>
      <div class="page-header__actions">
        <el-button @click="handleExport">导出台账</el-button>
        <el-button type="primary" @click="handleAdd">新增人员</el-button>
      </div>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select v-model="filterForm.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" clearable check-strictly />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.memberType" placeholder="选择类型" clearable>
            <el-option label="两委干部" value="TWO_COMMITTEE" />
            <el-option label="网格员" value="GRID_MEMBER" />
            <el-option label="党员" value="PARTY_MEMBER" />
            <el-option label="志愿者" value="VOLUNTEER" />
            <el-option label="职能下沉" value="FUNCTIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="姓名/电话" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="memberType" label="类型" width="100">
        <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.memberType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增人员' : '编辑人员'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.memberType" placeholder="选择类型">
            <el-option label="两委干部" value="TWO_COMMITTEE" />
            <el-option label="网格员" value="GRID_MEMBER" />
            <el-option label="党员" value="PARTY_MEMBER" />
            <el-option label="志愿者" value="VOLUNTEER" />
            <el-option label="职能下沉" value="FUNCTIONAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
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
import { listOrgMembers, createOrgMember, updateOrgMember, deleteOrgMember, OrgMemberEntity } from '../../api/community'
import { getGridTree, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<OrgMemberEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')
const filterForm = reactive({ gridId: undefined as number | undefined, memberType: '', keyword: '' })
const defaultForm = (): OrgMemberEntity => ({ id: undefined, gridId: undefined, memberType: '', name: '', phone: '', remark: '' })
const form = reactive<OrgMemberEntity>(defaultForm())

function typeLabel(type?: string) {
  const map: Record<string, string> = { TWO_COMMITTEE: '两委干部', GRID_MEMBER: '网格员', PARTY_MEMBER: '党员', VOLUNTEER: '志愿者', FUNCTIONAL: '职能下沉' }
  return map[type || ''] || '-'
}

async function loadData() {
  loading.value = true
  try {
    const data = await listOrgMembers(filterForm.gridId)
    let filtered = data
    if (filterForm.memberType) filtered = filtered.filter(m => m.memberType === filterForm.memberType)
    if (filterForm.keyword) {
      const kw = filterForm.keyword.toLowerCase()
      filtered = filtered.filter(m => (m.name && m.name.includes(kw)) || (m.phone && m.phone.includes(kw)))
    }
    tableData.value = filtered
  } finally { loading.value = false }
}

async function loadGridTree() { try { gridTree.value = await getGridTree() } catch (e) { console.error(e) } }
function handleSearch() { loadData() }
function handleReset() { filterForm.gridId = undefined; filterForm.memberType = ''; filterForm.keyword = ''; loadData() }
function handleExport() { window.open('/api/community/export/events', '_blank') }
function handleAdd() { Object.assign(form, defaultForm()); dialogMode.value = 'add'; dialogVisible.value = true }
function handleEdit(row: OrgMemberEntity) { Object.assign(form, row); dialogMode.value = 'edit'; dialogVisible.value = true }

async function handleDelete(row: OrgMemberEntity) {
  await ElMessageBox.confirm(`确定删除人员「${row.name}」？`, '提示', { type: 'warning' })
  await deleteOrgMember(row.id!); ElMessage.success('删除成功'); loadData()
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入姓名'); return }
  if (dialogMode.value === 'add') { await createOrgMember(form); ElMessage.success('新增成功') }
  else { await updateOrgMember(form.id!, form); ElMessage.success('更新成功') }
  dialogVisible.value = false; loadData()
}

onMounted(() => { loadGridTree(); loadData() })
</script>

<style scoped>
.page-container { padding: 20px; height: 100%; display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header__actions { display: flex; gap: 8px; }
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
