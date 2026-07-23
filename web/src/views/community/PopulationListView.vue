<template>
  <section class="page-container">
    <header class="page-header">
      <h2>实有人口管理</h2>
      <div class="page-header__actions">
        <el-button @click="handleExport">导出台账</el-button>
        <el-button type="primary" @click="handleAdd">新增人口</el-button>
      </div>
    </header>

    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select
            v-model="filterForm.gridId"
            :data="gridTree"
            :props="{ label: 'gridName', value: 'id' }"
            placeholder="选择网格"
            clearable
            check-strictly
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="姓名/身份证/电话" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="60" />
      <el-table-column prop="idCard" label="身份证号" width="180" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="householdType" label="户籍类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.householdType === 'LOCAL' ? 'primary' : row.householdType === 'RENTAL' ? 'warning' : 'info'">
            {{ householdTypeLabel(row.householdType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="address" label="居住地址" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogMode === 'add' ? '新增人口' : '编辑人口'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名" required>
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="MALE">男</el-radio>
            <el-radio value="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="出生日期">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="户籍类型">
          <el-select v-model="form.householdType" placeholder="选择类型">
            <el-option label="本地" value="LOCAL" />
            <el-option label="租住" value="RENTAL" />
            <el-option label="流动" value="FLOATING" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属网格">
          <el-tree-select
            v-model="form.gridId"
            :data="gridTree"
            :props="{ label: 'gridName', value: 'id' }"
            placeholder="选择网格"
            check-strictly
          />
        </el-form-item>
        <el-form-item label="楼栋号">
          <el-input v-model="form.buildingNo" placeholder="如: A栋" />
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="form.roomNo" placeholder="如: 301" />
        </el-form-item>
        <el-form-item label="居住地址">
          <el-input v-model="form.address" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
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
import { listPopulation, createPopulation, updatePopulation, deletePopulation, PopulationEntity } from '../../api/community'
import { getGridTree, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<PopulationEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const dialogVisible = ref(false)
const dialogMode = ref<'add' | 'edit'>('add')

const filterForm = reactive({
  gridId: undefined as number | undefined,
  keyword: ''
})

const defaultForm = (): PopulationEntity => ({
  id: undefined,
  gridId: undefined,
  name: '',
  idCard: '',
  phone: '',
  gender: 'MALE',
  birthday: undefined,
  householdType: 'LOCAL',
  address: '',
  buildingNo: '',
  roomNo: '',
  tags: '',
  remark: ''
})

const form = reactive<PopulationEntity>(defaultForm())

function householdTypeLabel(type?: string) {
  return type === 'LOCAL' ? '本地' : type === 'RENTAL' ? '租住' : type === 'FLOATING' ? '流动' : '-'
}

async function loadData() {
  loading.value = true
  try {
    const data = await listPopulation(filterForm.gridId)
    if (filterForm.keyword) {
      const kw = filterForm.keyword.toLowerCase()
      tableData.value = data.filter(p =>
        (p.name && p.name.includes(kw)) ||
        (p.idCard && p.idCard.includes(kw)) ||
        (p.phone && p.phone.includes(kw))
      )
    } else {
      tableData.value = data
    }
  } finally {
    loading.value = false
  }
}

async function loadGridTree() {
  try {
    gridTree.value = await getGridTree()
  } catch (e) {
    console.error('加载网格树失败', e)
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  filterForm.gridId = undefined
  filterForm.keyword = ''
  loadData()
}

function handleExport() {
  window.open('/api/community/export/population', '_blank')
}

function handleAdd() {
  Object.assign(form, defaultForm())
  dialogMode.value = 'add'
  dialogVisible.value = true
}

function handleEdit(row: PopulationEntity) {
  Object.assign(form, row)
  dialogMode.value = 'edit'
  dialogVisible.value = true
}

async function handleDelete(row: PopulationEntity) {
  await ElMessageBox.confirm(`确定删除人口「${row.name}」？`, '提示', { type: 'warning' })
  await deletePopulation(row.id!)
  ElMessage.success('删除成功')
  loadData()
}

async function handleSubmit() {
  if (!form.name) {
    ElMessage.warning('请输入姓名')
    return
  }
  if (dialogMode.value === 'add') {
    await createPopulation(form)
    ElMessage.success('新增成功')
  } else {
    await updatePopulation(form.id!, form)
    ElMessage.success('更新成功')
  }
  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadGridTree()
  loadData()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header__actions {
  display: flex;
  gap: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--fg-text-primary);
}

.page-filters {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--fg-bg-card);
  border: 1px solid var(--fg-border);
  border-radius: var(--fg-radius-lg);
}

/* 覆盖 Element Plus 组件为深色主题 */
:deep(.el-form-item__label) {
  color: var(--fg-text-secondary);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor.el-input__wrapper),
:deep(.el-textarea__inner),
:deep(.el-tree-select__popper) {
  background: var(--fg-bg-card-strong);
  border: 1px solid var(--fg-border);
  box-shadow: none;
}

:deep(.el-input__inner),
:deep(.el-select__placeholder),
:deep(.el-textarea__inner) {
  color: var(--fg-text-primary);
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: var(--fg-text-secondary);
}

:deep(.el-table) {
  background: var(--fg-bg-card);
  color: var(--fg-text-primary);
  border: 1px solid var(--fg-border);
  border-radius: var(--fg-radius-lg);
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background: var(--fg-bg-card-strong);
  color: var(--fg-text-secondary);
  border-bottom: 1px solid var(--fg-border);
}

:deep(.el-table td.el-table__cell) {
  background: var(--fg-bg-card);
  color: var(--fg-text-primary);
  border-bottom: 1px solid var(--fg-border);
}

:deep(.el-table tr:hover > td.el-table__cell) {
  background: rgba(94, 162, 255, 0.08);
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(94, 162, 255, 0.04);
}

:deep(.el-dialog) {
  background: var(--fg-bg-card);
  border: 1px solid var(--fg-border);
  border-radius: var(--fg-radius-lg);
}

:deep(.el-dialog__title) {
  color: var(--fg-text-primary);
}

:deep(.el-dialog__body) {
  color: var(--fg-text-primary);
}

:deep(.el-radio__label) {
  color: var(--fg-text-primary);
}

:deep(.el-select-dropdown__item) {
  color: var(--fg-text-primary);
}

:deep(.el-select-dropdown__item.hover),
:deep(.el-select-dropdown__item:hover) {
  background: rgba(94, 162, 255, 0.12);
}

:deep(.el-popper) {
  background: var(--fg-bg-card);
  border: 1px solid var(--fg-border);
}

:deep(.el-popper__arrow::before) {
  background: var(--fg-bg-card);
  border: 1px solid var(--fg-border);
}
</style>
