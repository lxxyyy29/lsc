<template>
  <section class="page-container">
    <header class="page-header">
      <h2>居民上报管理</h2>
    </header>
    <div class="page-filters">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="所属网格">
          <el-tree-select v-model="filterForm.gridId" :data="gridTree" :props="{ label: 'gridName', value: 'id' }" placeholder="选择网格" clearable check-strictly />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterForm.reportType" placeholder="选择类型" clearable>
            <el-option label="矛盾纠纷" value="DISPUTE" />
            <el-option label="安全隐患" value="SAFETY" />
            <el-option label="环境卫生" value="ENVIRONMENT" />
            <el-option label="噪音扰民" value="NOISE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="选择状态" clearable>
            <el-option label="待处理" value="PENDING" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="查询码">
          <el-input v-model="filterForm.queryCode" placeholder="输入查询码" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="queryCode" label="查询码" width="100" />
      <el-table-column prop="gridName" label="所属网格" width="120" />
      <el-table-column prop="residentName" label="上报人" width="100" />
      <el-table-column prop="reportType" label="类型" width="100">
        <template #default="{ row }"><el-tag size="small">{{ typeLabel(row.reportType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="150" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'HANDLED' ? 'success' : 'warning'">
            {{ row.status === 'HANDLED' ? '已处理' : '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handlerUserName" label="处理人" width="100" />
      <el-table-column prop="createdAt" label="上报时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleProcess(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="上报详情" width="600px">
      <div v-if="currentRow" class="detail-content">
        <p><strong>查询码：</strong>{{ currentRow.queryCode }}</p>
        <p><strong>上报人：</strong>{{ currentRow.residentName }} {{ currentRow.residentPhone }}</p>
        <p><strong>网格：</strong>{{ currentRow.gridName }}</p>
        <p><strong>类型：</strong>{{ typeLabel(currentRow.reportType) }}</p>
        <p><strong>标题：</strong>{{ currentRow.title }}</p>
        <p><strong>内容：</strong>{{ currentRow.content }}</p>
        <p v-if="currentRow.photoUrls"><strong>照片：</strong></p>
        <div v-if="currentRow.photoUrls" class="photo-list">
          <el-image v-for="(url, idx) in currentPhotos" :key="idx" :src="url" :preview-src-list="currentPhotos" class="photo-thumb" />
        </div>
        <p v-if="currentRow.handleResult"><strong>处理结果：</strong>{{ currentRow.handleResult }}</p>
        <p v-if="currentRow.handlerUserName"><strong>处理人：</strong>{{ currentRow.handlerUserName }}</p>
      </div>
    </el-dialog>

    <!-- 处理弹窗 -->
    <el-dialog v-model="processDialogVisible" title="处理上报" width="500px">
      <el-form :model="processForm" label-width="80px">
        <el-form-item label="处理结果" required>
          <el-input v-model="processForm.handleResult" type="textarea" :rows="4" placeholder="请输入处理结果..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="processDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProcess">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listResidentReports, handleResidentReport, getGridTree, ResidentReportEntity, GridTreeVo } from '../../api/community'

const loading = ref(false)
const tableData = ref<ResidentReportEntity[]>([])
const gridTree = ref<GridTreeVo[]>([])
const detailDialogVisible = ref(false)
const processDialogVisible = ref(false)
const currentRow = ref<ResidentReportEntity | null>(null)
const currentPhotos = ref<string[]>([])
const processForm = reactive({ id: 0 as number, handleResult: '' })
const filterForm = reactive({ gridId: undefined as number | undefined, reportType: '', status: '', queryCode: '' })

function typeLabel(type?: string) {
  const map: Record<string, string> = { DISPUTE: '矛盾纠纷', SAFETY: '安全隐患', ENVIRONMENT: '环境卫生', NOISE: '噪音扰民', OTHER: '其他' }
  return map[type || ''] || '-'
}

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').replace(/\.\d+$/, '')
}

async function loadData() {
  loading.value = true
  try {
    const data = await listResidentReports()
    let filtered = data
    if (filterForm.gridId) filtered = filtered.filter(r => r.gridId === filterForm.gridId)
    if (filterForm.reportType) filtered = filtered.filter(r => r.reportType === filterForm.reportType)
    if (filterForm.status) filtered = filtered.filter(r => r.status === filterForm.status)
    if (filterForm.queryCode) filtered = filtered.filter(r => r.queryCode?.includes(filterForm.queryCode))
    tableData.value = filtered
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function loadGridTree() { try { gridTree.value = await getGridTree() } catch (e) { console.error(e) } }
function handleSearch() { loadData() }
function handleReset() { filterForm.gridId = undefined; filterForm.reportType = ''; filterForm.status = ''; filterForm.queryCode = ''; loadData() }

function showDetail(row: ResidentReportEntity) {
  currentRow.value = row
  currentPhotos.value = row.photoUrls ? row.photoUrls.split(',').filter(Boolean) : []
  detailDialogVisible.value = true
}

function handleProcess(row: ResidentReportEntity) {
  processForm.id = row.id!
  processForm.handleResult = ''
  processDialogVisible.value = true
}

async function submitProcess() {
  if (!processForm.handleResult.trim()) { ElMessage.warning('请输入处理结果'); return }
  try {
    await handleResidentReport(processForm.id, processForm.handleResult)
    ElMessage.success('处理成功')
    processDialogVisible.value = false
    loadData()
  } catch (e) { ElMessage.error('处理失败') }
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
.detail-content p { margin: 8px 0; color: var(--fg-text-primary); }
.photo-list { display: flex; flex-wrap: wrap; gap: 8px; margin: 8px 0; }
.photo-thumb { width: 100px; height: 100px; border-radius: 8px; }
</style>
