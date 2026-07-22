<template>
  <PageContainer title="巡检任务">
    <WebListPageTemplate
      filter-title="查询条件"
      table-title="主列表"
      :table-meta="`当前共 ${total} 条`"
    >
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElSelect v-model="selectedJobStatus" clearable placeholder="请选择任务状态" aria-label="任务状态">
              <ElOption v-for="item in droneJobStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </label>
        </QueryPanel>
      </template>

      <template #table>
        <div v-if="loading" class="empty-state">加载中...</div>
        <template v-else-if="jobs.length">
          <table class="data-table">
            <thead>
              <tr>
                <th>计划起飞时间</th>
                <th>实际时间</th>
                <th>执行状态</th>
                <th>任务名称</th>
                <th>类型</th>
                <th>航线名称</th>
                <th>设备名称</th>
                <th>创建人</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in jobs" :key="item.jobId">
                <td>{{ formatDroneTimestamp(item.executeTime) }}</td>
                <td>{{ formatDroneTimestamp(item.beginTime) }}</td>
                <td>{{ normalizeDroneJobStatus(item.status) }}</td>
                <td>{{ item.jobName || '--' }}</td>
                <td>{{ item.taskType || '--' }}</td>
                <td>{{ item.fileName || '--' }}</td>
                <td>{{ item.dockName || '--' }}</td>
                <td>{{ item.usernameCn || '--' }}</td>
                <td>
                  <div class="table-actions">
                    <button type="button" class="action-link" :disabled="!item.jobId || actionLoading" @click="handlePauseResume(item.jobId || item.job_id || '', 0)">
                      挂起
                    </button>
                    <button type="button" class="action-link" :disabled="!item.jobId || actionLoading" @click="handlePauseResume(item.jobId || item.job_id || '', 1)">
                      恢复
                    </button>
                    <button type="button" class="action-link" :disabled="!item.dockSn || actionLoading" @click="handleReturnHome(item.dockSn || '')">
                      立即返航
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <footer class="patrol-pagination" aria-label="分页">
            <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
            <div class="patrol-pagination__controls">
              <button type="button" aria-label="上一页" :disabled="currentPage === 1 || loading" @click="changePage(currentPage - 1)">&lt;</button>
              <button type="button" aria-label="下一页" :disabled="currentPage >= totalPages || loading" @click="changePage(currentPage + 1)">&gt;</button>
            </div>
          </footer>
        </template>
        <div v-else class="empty-state">暂无任务数据。</div>
      </template>
    </WebListPageTemplate>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import {
  droneJobStatusOptions,
  formatDroneTimestamp,
  listDroneJobsPage,
  normalizeDroneJobStatus,
  pauseResumeDroneJob,
  returnDroneJobHome,
  type DroneJob
} from '../../api/drone'

const pageSize = 10
const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const toast = useToast()
const jobs = ref<DroneJob[]>([])
const total = ref(0)
const selectedJobStatus = ref('')
const currentPage = ref(1)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

async function loadJobs() {
  const payload = await listDroneJobsPage({
    page: currentPage.value,
    pageSize,
    status: selectedJobStatus.value ? Number(selectedJobStatus.value) : undefined
  })
  jobs.value = payload.items ?? []
  total.value = Number(payload.total ?? 0)
}

async function refreshJobs(resetPage = true) {
  loading.value = true
  errorMessage.value = ''
  if (resetPage) {
    currentPage.value = 1
  }
  try {
    await loadJobs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '巡检任务加载失败'
    toast.error(errorMessage.value)
    jobs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handlePauseResume(jobId: string, status: 0 | 1) {
  actionLoading.value = true
  errorMessage.value = ''
  try {
    await pauseResumeDroneJob(jobId, { status })
    await loadJobs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '任务控制失败'
    toast.error(errorMessage.value)
  } finally {
    actionLoading.value = false
  }
}

function changePage(page: number) {
  if (page < 1 || page > totalPages.value || page === currentPage.value) {
    return
  }

  currentPage.value = page
  void refreshJobs(false)
}

async function handleReturnHome(dockSn: string) {
  if (!dockSn) {
    return
  }

  actionLoading.value = true
  errorMessage.value = ''
  try {
    await returnDroneJobHome({ dockSn })
    await loadJobs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '返航指令下发失败'
    toast.error(errorMessage.value)
  } finally {
    actionLoading.value = false
  }
}

watch(selectedJobStatus, () => {
  void refreshJobs()
})

watch(totalPages, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

onMounted(() => {
  void refreshJobs()
})
</script>

<style scoped>
@import '../admin-shared.css';

:deep(.data-table) {
  border: 0;
  border-radius: 18px;
  overflow: hidden;
}

:deep(.data-table th) {
  color: #5f6b85;
  font-size: 12px;
  font-weight: 600;
  background: rgba(94, 162, 255, 0.08);
}

.error-text,
.empty-state {
  margin-top: 16px;
}

.patrol-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: rgba(205, 222, 248, 0.78);
}

.patrol-pagination__controls {
  display: flex;
  gap: 8px;
}

.patrol-pagination__controls button {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 10px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  cursor: pointer;
}

.patrol-pagination__controls button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.error-text {
  color: #ffb4b4;
}
</style>
