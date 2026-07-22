<template>
  <section class="page-container">
    <header class="page-header">
      <h2>巡查记录管理</h2>
      <div class="page-header__actions">
        <el-button @click="handleExport">导出台账</el-button>
      </div>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select v-model="filterForm.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" clearable check-strictly />
        </el-form-item>
        <el-form-item label="巡查类型">
          <el-select v-model="filterForm.patrolType" placeholder="选择类型" clearable>
            <el-option label="日常巡查" value="NORMAL" />
            <el-option label="专项巡查" value="SPECIAL" />
            <el-option label="复查" value="REVIEW" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filterForm.keyword" placeholder="内容/地址" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="gridName" label="所属网格" width="120" />
      <el-table-column prop="userName" label="巡查员" width="100" />
      <el-table-column prop="patrolType" label="类型" width="100">
        <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.patrolType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="content" label="巡查内容" show-overflow-tooltip />
      <el-table-column prop="address" label="位置" width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="照片" width="80">
        <template #default="{ row }">
          <el-button v-if="row.photoUrls" size="small" link type="primary" @click="showPhotos(row)">查看</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="photoDialogVisible" title="现场照片" width="600px">
      <div class="photo-viewer">
        <image v-for="(url, idx) in currentPhotos" :key="idx" :src="url" class="photo-img" fit="contain" />
      </div>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listPatrolRecords, getGridTree, PatrolRecordEntity, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<PatrolRecordEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const photoDialogVisible = ref(false)
const currentPhotos = ref<string[]>([])
const filterForm = reactive({ gridId: undefined as number | undefined, patrolType: '', keyword: '' })

function typeLabel(type?: string) {
  const map: Record<string, string> = { NORMAL: '日常巡查', SPECIAL: '专项巡查', REVIEW: '复查' }
  return map[type || ''] || '-'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').replace(/\.\d+$/, '')
}

async function loadData() {
  loading.value = true
  try {
    const data = await listPatrolRecords()
    let filtered = data
    if (filterForm.gridId) filtered = filtered.filter(r => r.gridId === filterForm.gridId)
    if (filterForm.patrolType) filtered = filtered.filter(r => r.patrolType === filterForm.patrolType)
    if (filterForm.keyword) {
      const kw = filterForm.keyword.toLowerCase()
      filtered = filtered.filter(r => (r.content && r.content.includes(kw)) || (r.address && r.address.includes(kw)))
    }
    tableData.value = filtered
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadGridTree() { try { gridTree.value = await getGridTree() } catch (e) { console.error(e) } }
function handleSearch() { loadData() }
function handleReset() { filterForm.gridId = undefined; filterForm.patrolType = ''; filterForm.keyword = ''; loadData() }
function handleExport() { window.open('/api/community/export/patrols', '_blank') }
function showPhotos(row: PatrolRecordEntity) {
  currentPhotos.value = row.photoUrls ? row.photoUrls.split(',').filter(Boolean) : []
  photoDialogVisible.value = true
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
.photo-viewer { display: flex; flex-wrap: wrap; gap: 12px; }
.photo-img { width: 100%; max-height: 400px; border-radius: 8px; }
</style>
