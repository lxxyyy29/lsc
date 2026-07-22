<template>
  <PageContainer title="工单列表">
    <WebListPageTemplate
      filter-title="查询条件"
      table-title=""
      :table-meta="`当前共 ${total} 条记录`"
    >
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElSelect v-model="selectedState" clearable placeholder="请选择工单状态" aria-label="工单状态">
              <ElOption v-for="item in stateOptions" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </label>
          <label class="field-stack">
            <ElInput v-model="selectedAssignee" aria-label="处理人" placeholder="请输入处理人" clearable />
          </label>
          <label class="field-stack">
            <ElSelect v-model="selectedAreaId" clearable placeholder="请选择所属片区" aria-label="所属片区">
              <ElOption v-for="item in allAreas" :key="item.id" :label="item.name" :value="item.id" />
            </ElSelect>
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <button type="button" class="action-button" :disabled="!pageItems.length || exporting" @click="exportWorkOrders">
            {{ exporting ? '导出中...' : '导出' }}
          </button>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="workorder-message">加载中...</div>
        <table v-else-if="pageItems.length" class="data-table">
          <thead>
            <tr>
              <th>工单编号</th>
              <th>事件名称</th>
              <th>所属片区</th>
              <th>当前处理人</th>
              <th>派单人</th>
              <th>派单时间</th>
              <th>工单状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in pageItems" :key="`row-${item.id}`">
              <td>
                <strong>{{ item.code }}</strong>
              </td>
              <td>{{ item.eventTitle }}</td>
              <td>{{ item.districtName }}</td>
              <td>{{ item.currentHandler }}</td>
              <td>{{ item.dispatcherName }}</td>
              <td>{{ item.dispatchTime || '-' }}</td>
              <td><StatusTag :status="item.state" /></td>
              <td>
                <RouterLink :to="`/work-orders/${item.id}`" class="workorder-row-link">查看详情</RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination v-if="total > 0" :total="total" :current-page="currentPage" :page-size="pageSize" :disabled="loading" @change="changePage" />
        <div v-if="!loading && !pageItems.length" class="workorder-message">暂无工单数据。</div>
      </template>
    </WebListPageTemplate>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { listWorkOrders, listWorkOrdersPaged, type WorkOrderListItem } from '../../api/workorder'
import { listBizAreas, type BizArea } from '../../api/biz-area'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import ListPagination from '../../components/admin/ListPagination.vue'

const pageItems = ref<WorkOrderListItem[]>([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const errorMessage = ref('')
const toast = useToast()
const currentPage = ref(1)
const pageSize = 10
const selectedState = ref('')
const selectedAssignee = ref('')
const selectedAreaId = ref<number | undefined>(undefined)

// Area data for dropdown
const allAreas = ref<Array<{ id: number; name: string }>>([])
const stateOptions = [
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已关闭', value: 'CLOSED' }
] as const

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadAreas() {
  try {
    const areas = await listBizAreas({ status: 'ACTIVE' })
    allAreas.value = areas.map((a) => ({ id: a.id, name: a.areaName })).filter((a) => a.name)
  } catch {
    allAreas.value = []
  }
}

async function loadPage() {
  loading.value = true
  errorMessage.value = ''
  try {
    const statusParam = selectedState.value || undefined
    const assigneeParam = selectedAssignee.value || undefined
    const result = await listWorkOrdersPaged(currentPage.value, pageSize, statusParam, assigneeParam, selectedAreaId.value)
    pageItems.value = result.items
    total.value = result.total
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '工单列表加载失败'
    toast.error(errorMessage.value)
    pageItems.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changePage(page: number) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) return
  currentPage.value = page
  void loadPage()
}

function getStateLabel(state: string) {
  switch (state) {
    case 'PROCESSING': return '处理中'
    case 'COMPLETED': return '已完成'
    case 'CLOSED': return '已关闭'
    case 'WAITING_DISPATCH': return '待派单'
    case 'DISPATCHED': return '已派单'
    case 'WAITING_CLOSE_CONFIRM': return '待关单'
    default: return state || '—'
  }
}

async function exportWorkOrders() {
  exporting.value = true
  errorMessage.value = ''
  try {
    // Fetch ALL work orders for export (via /work-orders/export)
    const allOrders = await listWorkOrders()

    const headers = ['工单编号', '事件名称', '所属片区', '当前处理人', '派单人', '派单时间', '工单状态']
    const rows = allOrders.map((item) => [
      item.code,
      item.eventTitle,
      item.districtName,
      item.currentHandler,
      item.dispatcherName,
      item.dispatchTime || '',
      getStateLabel(item.state)
    ])

    const csvContent = [headers, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\n')

    const bom = '\uFEFF'
    const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `工单列表_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '导出失败'
    toast.error(errorMessage.value)
  } finally {
    exporting.value = false
  }
}

// When filters change, reset page and reload from backend
watch(selectedState, () => {
  currentPage.value = 1
  void loadPage()
})

watch(selectedAssignee, () => {
  currentPage.value = 1
  void loadPage()
})

watch(selectedAreaId, () => {
  currentPage.value = 1
  void loadPage()
})

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

onMounted(async () => {
  await loadAreas()
  void loadPage()
})
</script>

<style scoped>
@import '../admin-shared.css';

.workorder-row-link {
  font-weight: 600;
}

.workorder-message {
  margin-top: 16px;
}

.workorder-message--error {
  color: #ffb4b4;
}

.toolbar-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 720px) {
  :deep(.data-table) {
    display: block;
    overflow-x: auto;
  }
}
</style>
