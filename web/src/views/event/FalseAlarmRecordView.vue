<template>
  <PageContainer title="误报记录">
    <WebListPageTemplate filter-title="查询条件" table-title="误报记录列表" :table-meta="`共 ${total} 条记录`">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.keyword" aria-label="关键字" placeholder="事件编号或名称" clearable @change="resetAndReload" />
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
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无误报记录。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>事件名称</th>
              <th>事件类型</th>
              <th>忽略原因</th>
              <th>操作人</th>
              <th>忽略时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.eventCode || '--' }}</td>
              <td>{{ item.eventTitle || '--' }}</td>
              <td>{{ getEventTypeLabel(item.eventType) || '--' }}</td>
              <td>{{ item.reason || '--' }}</td>
              <td>{{ item.ignoredBy || '--' }}</td>
              <td>{{ item.ignoredAt || '--' }}</td>
              <td>
                <RouterLink :to="`/events/${item.eventId}`" class="action-link">查看详情</RouterLink>
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
import { onMounted, reactive, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import { RouterLink } from 'vue-router'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import ZhDateRangePicker from '../../components/common/ZhDateRangePicker.vue'
import { usePagination } from '../../composables/usePagination'
import { listFalseAlarmRecords, getEventTypeLabel, type FalseAlarmRecord } from '../../api/event'

const filters = reactive({
  keyword: '',
  startDate: '',
  endDate: ''
})

const { items, total, loading, errorMessage, currentPage, pageSize, changePage, resetAndReload } =
  usePagination<FalseAlarmRecord>({
    fetcher: (page, ps) =>
      listFalseAlarmRecords(page, ps, {
        keyword: filters.keyword,
        startDate: filters.startDate,
        endDate: filters.endDate
      }),
    filters: () => filters
  })

const toast = useToast()
watch(errorMessage, (msg) => { if (msg) toast.error(msg) })

onMounted(() => {
  resetAndReload()
})
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

.action-row {
  display: flex;
  gap: 8px;
}

.error-text {
  color: #ffb4b4;
}

.empty-state {
  color: rgba(205, 222, 248, 0.88);
}
</style>
