<template>
  <PageContainer title="数据报表">
    <WebListPageTemplate filter-title="查询条件" table-title="数据统计">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.areaName" aria-label="片区名称" placeholder="请输入片区名称" clearable @change="resetAndReload" />
          </label>
          <label class="field-stack">
            <ZhDateRangePicker v-model:start="filters.startDate" v-model:end="filters.endDate" placeholder="选择时间范围" @change="resetAndReload" />
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="resetAndReload">
              {{ loading ? '加载中...' : '刷新数据' }}
            </button>
            <button type="button" class="action-button" :disabled="exporting" @click="handleExport">
              {{ exporting ? '导出中...' : '导出' }}
            </button>
          </div>
        </div>
      </template>

      <template #table>
        <!-- Summary cards -->
        <div v-if="summary" class="summary-cards">
          <div class="summary-card">
            <span class="summary-card__label">覆盖片区</span>
            <span class="summary-card__value">{{ summary.totalAreas }}</span>
          </div>
          <div class="summary-card">
            <span class="summary-card__label">总事件数</span>
            <span class="summary-card__value">{{ summary.totalEvents }}</span>
          </div>
          <div class="summary-card">
            <span class="summary-card__label">已办结</span>
            <span class="summary-card__value summary-card__value--success">{{ summary.closedEvents }}</span>
          </div>
          <div class="summary-card">
            <span class="summary-card__label">待处理</span>
            <span class="summary-card__value summary-card__value--warning">{{ summary.pendingEvents }}</span>
          </div>
          <div class="summary-card">
            <span class="summary-card__label">整体办结率</span>
            <span class="summary-card__value">{{ formatRate(summary.overallClosureRate) }}</span>
          </div>
        </div>

        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无报表数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>片区名称</th>
              <th>总事件数</th>
              <th>已办结</th>
              <th>待处理</th>
              <th>工单总数</th>
              <th>已完成工单</th>
              <th>办结率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.areaId">
              <td>{{ item.areaName }}</td>
              <td>{{ item.totalEvents }}</td>
              <td class="cell--success">{{ item.closedEvents }}</td>
              <td class="cell--warning">{{ item.pendingEvents }}</td>
              <td>{{ item.workOrderCount }}</td>
              <td>{{ item.completedWorkOrders }}</td>
              <td>
                <span class="rate-badge" :class="rateClass(item.closureRate)">
                  {{ formatRate(item.closureRate) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination
          v-if="total > 0"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :disabled="loading"
          @change="changePage"
        />
      </template>
    </WebListPageTemplate>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import ZhDateRangePicker from '../../components/common/ZhDateRangePicker.vue'
import { usePagination } from '../../composables/usePagination'
import {
  listDistrictReportPaged,
  getDistrictReportSummary,
  exportDistrictReport,
  type DistrictReportQuery,
  type DistrictReportSummary
} from '../../api/report'

const filters = reactive<DistrictReportQuery>({
  areaName: '',
  startDate: '',
  endDate: ''
})

const summary = ref<DistrictReportSummary | null>(null)
const exporting = ref(false)
const exportError = ref('')
const toast = useToast()

const { items, total, loading, errorMessage, currentPage, pageSize, changePage, resetAndReload } =
  usePagination({
    fetcher: (page, ps) =>
      listDistrictReportPaged(page, ps, {
        areaName: filters.areaName,
        startDate: filters.startDate,
        endDate: filters.endDate
      }),
    filters: () => filters
  })

watch(errorMessage, (msg) => { if (msg) toast.error(msg) })

async function loadSummary() {
  try {
    summary.value = await getDistrictReportSummary({
      areaName: filters.areaName,
      startDate: filters.startDate,
      endDate: filters.endDate
    })
  } catch {
    // summary is non-critical; silently ignore
  }
}

onMounted(async () => {
  await Promise.all([resetAndReload(), loadSummary()])
})

async function handleExport() {
  if (exporting.value) return
  exporting.value = true
  exportError.value = ''
  try {
    await exportDistrictReport({
      areaName: filters.areaName,
      startDate: filters.startDate,
      endDate: filters.endDate
    })
  } catch (error) {
    exportError.value = error instanceof Error ? error.message : '导出失败，请稍后重试'
    toast.error(exportError.value)
  } finally {
    exporting.value = false
  }
}

function formatRate(rate: number): string {
  if (rate == null) return '—'
  return `${(rate * 100).toFixed(1)}%`
}

function rateClass(rate: number): string {
  if (rate >= 0.9) return 'rate-badge--high'
  if (rate >= 0.6) return 'rate-badge--mid'
  return 'rate-badge--low'
}
</script>

<style scoped>
@import '../admin-shared.css';

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.error-text {
  color: #ffb4b4;
}

.empty-state {
  color: rgba(205, 222, 248, 0.88);
}

.summary-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
}

.summary-card {
  flex: 1 1 140px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid rgba(64, 158, 255, 0.18);
  border-radius: 12px;
  background: rgba(8, 30, 50, 0.6);
}

.summary-card__label {
  font-size: 12px;
  color: rgba(205, 222, 248, 0.7);
  letter-spacing: 0.04em;
}

.summary-card__value {
  font-size: 24px;
  font-weight: 700;
  color: #eef5ff;
  line-height: 1.2;
}

.summary-card__value--success {
  color: #52c41a;
}

.summary-card__value--warning {
  color: #faad14;
}

.cell--success {
  color: #52c41a;
}

.cell--warning {
  color: #faad14;
}

.rate-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.rate-badge--high {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
  border: 1px solid rgba(82, 196, 26, 0.3);
}

.rate-badge--mid {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
  border: 1px solid rgba(250, 173, 20, 0.3);
}

.rate-badge--low {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
  border: 1px solid rgba(255, 77, 79, 0.3);
}
</style>
