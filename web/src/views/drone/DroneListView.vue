<template>
  <PageContainer title="飞控接入">
    <section class="drone-page">
      <!-- 顶部操作栏 -->
      <div class="drone-page__header">
        <div class="drone-page__header-left">
          <h3 class="section-title">设备总览</h3>
          <span class="section-meta">当前共 {{ dockDevices.length }} 台机场</span>
        </div>
      </div>

      <!-- 设备卡片区 -->
      <div v-if="loading" class="empty-state">加载中...</div>
      <div v-else-if="dockDevices.length" class="device-cards">
        <div
          v-for="device in dockDevices"
          :key="device.deviceSn"
          class="device-card"
        >
          <!-- 机场信息 -->
          <div class="device-card__header">
            <div class="device-card__title-row">
              <span class="device-card__icon device-card__icon--dock">&#9978;</span>
              <span class="device-card__name">{{ device.deviceName || '--' }}</span>
            </div>
            <span
              class="device-card__status"
              :class="dockStatusClass(device.modeCode)"
            >
              <i class="status-dot" />
              {{ normalizeDroneDeviceMode(device.modeCode) }}
            </span>
          </div>

          <dl class="device-card__info">
            <div>
              <dt>序列号</dt>
              <dd>{{ device.deviceSn }}</dd>
            </div>
            <div>
              <dt>固件版本</dt>
              <dd>{{ device.firmwareVersion || '--' }}</dd>
            </div>
            <div>
              <dt>平台</dt>
              <dd>{{ platformNameOf(device) }}</dd>
            </div>
          </dl>

          <!-- 绑定的无人机 -->
          <div class="drone-sub" v-if="device.droneInfo">
            <div class="drone-sub__header">
              <div class="drone-sub__title-row">
                <span class="device-card__icon device-card__icon--drone">&#9992;</span>
                <span class="drone-sub__name">{{ device.droneInfo.deviceName || '--' }}</span>
              </div>
              <span
                class="device-card__status device-card__status--sm"
                :class="droneStatusClass(device.droneInfo.modeCode)"
              >
                <i class="status-dot" />
                {{ normalizeDroneAircraftMode(device.droneInfo.modeCode) }}
              </span>
            </div>
            <dl class="drone-sub__info">
              <div>
                <dt>序列号</dt>
                <dd>{{ device.droneInfo.droneSn || '--' }}</dd>
              </div>
              <div>
                <dt>当前任务</dt>
                <dd>{{ currentMissionOf(device.deviceSn) }}</dd>
              </div>
            </dl>
          </div>
          <div class="drone-sub drone-sub--empty" v-else>
            <span class="drone-sub__placeholder">暂无绑定无人机</span>
          </div>

          <!-- 卡片操作 -->
          <div class="device-card__actions">
            <button type="button" class="action-link" @click="openDroneStatus(device)">
              查看状态
            </button>
            <button type="button" class="action-link" @click="openCreateDialog(device.deviceSn)">
              派发任务
            </button>
            <button type="button" class="action-link" @click="openWaylineDialog(device.deviceSn)">
              查看航线
            </button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">暂无设备数据。</div>

      <!-- 巡查任务表 -->
      <div class="patrol-section">
        <div class="patrol-section__header">
          <div class="patrol-section__title-row">
            <h3 class="section-title">巡查任务</h3>
          </div>
          <label class="patrol-filter">
            <ElSelect v-model="selectedJobStatus" class="patrol-filter__select" clearable placeholder="请选择任务状态" aria-label="任务状态">
              <ElOption v-for="item in droneJobStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </label>
        </div>
        <div v-if="patrolLoading" class="empty-state">加载中...</div>
        <template v-else-if="patrolJobs.length">
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
              <tr v-for="item in patrolJobs" :key="item.jobId">
                <td>{{ formatDroneTimestamp(item.executeTime) }}</td>
                <td>{{ formatDroneTimestamp(item.beginTime) }}</td>
                <td>{{ normalizeDroneJobStatus(item.status) }}</td>
                <td>{{ item.jobName || '--' }}</td>
                <td>{{ item.taskType || '--' }}</td>
                <td>{{ item.fileName || '--' }}</td>
                <td>{{ item.dockName || '--' }}</td>
                <td>{{ item.usernameCn || '--' }}</td>
                <td>
                  <div v-if="isJobInProgress(item.status)" class="table-actions">
                    <button type="button" class="action-link" @click="openDroneStatusByDockSn(item.dockSn || item.dock_sn as string || '')">
                      查看无人机
                    </button>
                    <button type="button" class="action-link" :disabled="!item.dockSn || patrolActionLoading" @click="handlePatrolReturnHome(item.dockSn || '')">
                      立即返航
                    </button>
                  </div>
                  <span v-else class="text-muted">--</span>
                </td>
              </tr>
            </tbody>
          </table>
          <footer class="patrol-pagination" aria-label="分页">
            <span class="patrol-pagination__total">共 {{ patrolTotal }} 条记录</span>
            <div class="patrol-pagination__right">
              <button type="button" class="pg-btn" aria-label="上一页" :disabled="patrolCurrentPage === 1 || patrolLoading" @click="changePatrolPage(patrolCurrentPage - 1)">&lt;</button>
              <button
                v-for="p in visiblePages"
                :key="p"
                type="button"
                class="pg-btn"
                :class="{ 'pg-btn--active': p === patrolCurrentPage }"
                :disabled="patrolLoading"
                @click="changePatrolPage(p)"
              >{{ p }}</button>
              <button type="button" class="pg-btn" aria-label="下一页" :disabled="patrolCurrentPage >= patrolTotalPages || patrolLoading" @click="changePatrolPage(patrolCurrentPage + 1)">&gt;</button>
              <select v-model="patrolPageSize" class="pg-size" aria-label="每页条数">
                <option :value="10">10 条/页</option>
                <option :value="20">20 条/页</option>
                <option :value="50">50 条/页</option>
                <option :value="100">100 条/页</option>
              </select>
            </div>
          </footer>
        </template>
        <div v-else class="empty-state">暂无任务数据。</div>
      </div>
    </section>

    <SystemDialog :open="dialogOpen" title="创建飞行任务" subtitle="飞行 / 巡检任务" @close="closeDialog">
      <div class="dialog-body">
        <label class="field-stack">
          <span>设备</span>
          <select v-model="createForm.dockSn" aria-label="任务设备">
            <option value="" disabled hidden>请选择无人机</option>
            <option v-for="item in droneOptionsForDialog" :key="item.dockSn" :value="item.dockSn">
              {{ item.label }}
            </option>
          </select>
        </label>
        <label class="field-stack">
          <span>航线</span>
          <select v-model="createForm.fileId" aria-label="任务航线">
            <option value="" disabled hidden>请选择航线</option>
            <option v-for="item in waylines" :key="String(item.id || item.file_id)" :value="String(item.id || item.file_id)">{{ item.name || item.file_name }}</option>
          </select>
        </label>
      </div>
      <div v-if="aiBindingList.length" class="ai-binding-section">
        <h5 class="ai-binding-title">算法绑定详情</h5>
        <div class="ai-binding-table-wrap">
          <table class="data-table ai-binding-table">
            <thead>
              <tr>
                <th>模型名称</th>
                <th>开始算法航点</th>
                <th>结束算法航点</th>
                <th>动作绑定</th>
                <th>选择音频</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(bind, idx) in aiBindingList" :key="idx">
                <td>{{ bind.modelName || bind.model_name || '--' }}</td>
                <td>{{ bind.startPointIndex ?? bind.start_point_index ?? '--' }}</td>
                <td>{{ bind.endPointIndex ?? bind.end_point_index ?? '--' }}</td>
                <td>{{ bind.actions || '--' }}</td>
                <td>{{ bind.fileId || bind.file_id || '--' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <p v-else-if="createForm.fileId && !aiBindingLoading" class="ai-binding-empty">该航线暂无算法绑定。</p>
      <template #footer>
        <button type="button" class="action-button action-button--secondary" @click="closeDialog">取消</button>
        <button type="button" class="action-button" :disabled="!createForm.dockSn || !createForm.fileId || actionLoading" @click="handleCreateJob">
          {{ actionLoading ? '提交中...' : '确认创建' }}
        </button>
      </template>
    </SystemDialog>
    <DroneStatusDialog
      :open="droneStatusOpen"
      :device-sn="droneStatusSn"
      :dock-video-url="droneStatusDockVideoUrl"
      :drone-video-url="droneStatusDroneVideoUrl"
      :initial-lng="droneStatusLng"
      :initial-lat="droneStatusLat"
      @close="closeDroneStatus"
    />
    <WaylineAlgorithmDialog
      :open="waylineDialogOpen"
      :dock-sn="waylineDialogDockSn"
      @close="waylineDialogOpen = false"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import SystemDialog from '../../components/system/SystemDialog.vue'
import DroneStatusDialog from '../../components/drone/DroneStatusDialog.vue'
import WaylineAlgorithmDialog from '../../components/drone/WaylineAlgorithmDialog.vue'
import {
  createDroneJob,
  droneJobStatusOptions,
  formatDroneTimestamp,
  getWaylineAiBindingDetail,
  listDroneDevicesPaged,
  listDroneJobs,
  listDroneJobsPage,
  listDroneWaylines,
  normalizeDroneDeviceMode,
  normalizeDroneAircraftMode,
  isDroneFlying,
  isDroneOffline,
  rewriteVideoUrl,
  normalizeDroneJobStatus,
  pauseResumeDroneJob,
  returnDroneJobHome,
  type AiBindingItem,
  type DroneDevice,
  type DroneJob,
  type DroneWayline
} from '../../api/drone'
import { usePagination } from '../../composables/usePagination'

// ─── State ───
const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref('')
const toast = useToast()

// ─── Wayline dialog ───
const waylineDialogOpen = ref(false)
const waylineDialogDockSn = ref('')

function openWaylineDialog(dockSn: string) {
  waylineDialogDockSn.value = dockSn
  waylineDialogOpen.value = true
}
const jobs = ref<DroneJob[]>([])
const waylines = ref<DroneWayline[]>([])
const selectedJobStatus = ref('')
const dialogOpen = ref(false)
const droneStatusOpen = ref(false)
const droneStatusSn = ref('')
const droneStatusLng = ref(0)
const droneStatusLat = ref(0)
const droneStatusDockVideoUrl = ref('')
const droneStatusDroneVideoUrl = ref('')
const createForm = reactive({
  dockSn: '',
  fileId: ''
})
const aiBindingList = ref<AiBindingItem[]>([])
const aiBindingLoading = ref(false)

const devicePagination = usePagination<DroneDevice, Record<string, never>>({
  pageSize: 10,
  filters: ref({} as Record<string, never>),
  fetcher: async (page, pageSize) => {
    return await listDroneDevicesPaged(page, pageSize)
  }
})

// 收集所有被绑定为子设备的无人机SN，用于过滤
const boundDroneSns = computed(() => {
  const sns = new Set<string>()
  for (const d of devicePagination.items.value) {
    if (d.childSn) sns.add(d.childSn)
    if (d.droneInfo?.droneSn) sns.add(d.droneInfo.droneSn)
  }
  return sns
})

// 只保留机场（Dock），过滤掉已作为子设备绑定的无人机
const dockDevices = computed(() =>
  devicePagination.items.value.filter((item) => !boundDroneSns.value.has(item.deviceSn))
)

const deviceOptions = computed(() => dockDevices.value.filter((item) => Boolean(item.deviceSn)))

// 派发任务时展示无人机，但值仍为 dockSn（API 需要）
const droneOptionsForDialog = computed(() =>
  dockDevices.value
    .filter((d) => d.deviceSn && d.droneInfo)
    .map((d) => ({
      dockSn: d.deviceSn,
      label: `${d.droneInfo!.deviceName || d.droneInfo!.droneSn || '--'} / ${d.droneInfo!.droneSn || '--'}`
    }))
)
const deviceNameMap = computed(() => new Map(devicePagination.items.value.map((item) => [item.deviceSn, item.deviceName])))

function platformNameOf(_device: DroneDevice) {
  return '大疆无人机平台'
}

function dockStatusClass(modeCode: number | null | undefined) {
  const label = normalizeDroneDeviceMode(modeCode)
  if (label === '离线') return 'status--offline'
  if (label === '空闲') return 'status--idle'
  if (label === '工作') return 'status--working'
  return 'status--other'
}

function droneStatusClass(modeCode: number | null | undefined) {
  if (isDroneOffline(modeCode)) return 'status--offline'
  if (modeCode === 0) return 'status--idle'
  if (isDroneFlying(modeCode)) return 'status--working'
  return 'status--other'
}

function currentMissionOf(deviceSn: string) {
  const currentJob = jobs.value.find((item) => {
    const sn = (item.dockSn || item.dock_sn) as string | undefined
    const name = (item.dockName || item.dock_name) as string | undefined
    const matchedByDockSn = sn && sn === deviceSn
    const matchedByDockName = name && name === deviceNameMap.value.get(deviceSn)
    return (matchedByDockSn || matchedByDockName) && [1, 2, '1', '2'].includes(item.status as never)
  })

  return (currentJob?.jobName || currentJob?.job_name || currentJob?.fileName || currentJob?.file_name || '--') as string
}

async function loadDevices() {
  await devicePagination.loadPage()
}

async function loadWaylines() {
  waylines.value = await listDroneWaylines()
}

async function loadJobs() {
  jobs.value = await listDroneJobs({
    status: selectedJobStatus.value ? Number(selectedJobStatus.value) : undefined
  })
}

async function refreshAll() {
  loading.value = true
  errorMessage.value = ''

  try {
    await Promise.all([loadDevices(), loadWaylines(), loadJobs(), refreshPatrolJobs()])
  } catch {
    errorMessage.value = '无人机页面数据加载失败，请稍后重试'
    toast.error(errorMessage.value)
    jobs.value = []
    waylines.value = []
  } finally {
    loading.value = false
  }
}

function openCreateDialog(deviceSn = '') {
  createForm.dockSn = deviceSn || deviceOptions.value[0]?.deviceSn || ''
  createForm.fileId = waylines.value[0]?.id != null ? String(waylines.value[0].id) : (waylines.value[0]?.file_id != null ? String(waylines.value[0].file_id) : '')
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  aiBindingList.value = []
}

async function loadAiBinding() {
  if (!createForm.fileId) return
  aiBindingLoading.value = true
  try {
    aiBindingList.value = await getWaylineAiBindingDetail(createForm.fileId)
  } catch {
    aiBindingList.value = []
  } finally {
    aiBindingLoading.value = false
  }
}

function openDroneStatus(device: DroneDevice) {
  droneStatusSn.value = device.deviceSn
  droneStatusLng.value = device.longitude ?? 0
  droneStatusLat.value = device.latitude ?? 0
  // 机场本身的视频流
  droneStatusDockVideoUrl.value = rewriteVideoUrl(device.videoPlayUrlWebRtc?.[0]?.videoList?.[0]?.playUrl)
  // 绑定无人机的视频流（有值代表无人机已开机）
  droneStatusDroneVideoUrl.value = rewriteVideoUrl(device.droneInfo?.videoPlayUrlWebRtc?.[0]?.videoList?.[0]?.playUrl)
  droneStatusOpen.value = true
}

function openDroneStatusByDockSn(dockSn: string) {
  const device = devicePagination.items.value.find((d) => d.deviceSn === dockSn)
  if (device) {
    openDroneStatus(device)
  }
}

function isJobInProgress(status: number | string | null | undefined): boolean {
  return [2, '2'].includes(status as never)
}

function closeDroneStatus() {
  droneStatusOpen.value = false
  droneStatusSn.value = ''
  droneStatusDockVideoUrl.value = ''
  droneStatusDroneVideoUrl.value = ''
}

async function handleCreateJob() {
  if (!createForm.dockSn || !createForm.fileId) {
    return
  }

  actionLoading.value = true
  errorMessage.value = ''

  try {
    await createDroneJob({
      dockSn: createForm.dockSn,
      fileId: createForm.fileId
    })
    dialogOpen.value = false
    await loadJobs()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建任务失败'
    toast.error(errorMessage.value)
  } finally {
    actionLoading.value = false
  }
}

// ─── Patrol state ───
const patrolLoading = ref(false)
const patrolActionLoading = ref(false)
const patrolErrorMessage = ref('')
const patrolJobs = ref<DroneJob[]>([])
const patrolTotal = ref(0)
const patrolCurrentPage = ref(1)
const patrolPageSize = ref(10)

const patrolTotalPages = computed(() => Math.max(1, Math.ceil(patrolTotal.value / patrolPageSize.value)))

// 生成可见页码列表（最多显示 7 个页码）
const visiblePages = computed(() => {
  const total = patrolTotalPages.value
  const current = patrolCurrentPage.value
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const pages: number[] = []
  let start = Math.max(1, current - 2)
  let end = Math.min(total, current + 2)
  if (start <= 2) {
    start = 1
    end = Math.min(5, total)
  }
  if (end >= total - 1) {
    end = total
    start = Math.max(1, total - 4)
  }
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

async function loadPatrolJobs() {
  const payload = await listDroneJobsPage({
    page: patrolCurrentPage.value,
    pageSize: patrolPageSize.value,
    status: selectedJobStatus.value ? Number(selectedJobStatus.value) : undefined
  })
  patrolJobs.value = payload.items ?? []
  patrolTotal.value = Number(payload.total ?? 0)
}

async function refreshPatrolJobs(resetPage = true) {
  patrolLoading.value = true
  patrolErrorMessage.value = ''
  if (resetPage) {
    patrolCurrentPage.value = 1
  }
  try {
    await loadPatrolJobs()
  } catch (error) {
    patrolErrorMessage.value = error instanceof Error ? error.message : '巡检任务加载失败'
    toast.error(patrolErrorMessage.value)
    patrolJobs.value = []
    patrolTotal.value = 0
  } finally {
    patrolLoading.value = false
  }
}

function changePatrolPage(page: number) {
  if (page < 1 || page > patrolTotalPages.value || page === patrolCurrentPage.value) {
    return
  }
  patrolCurrentPage.value = page
  void refreshPatrolJobs(false)
}

async function handlePatrolPauseResume(jobId: string, status: 0 | 1) {
  patrolActionLoading.value = true
  patrolErrorMessage.value = ''
  try {
    await pauseResumeDroneJob(jobId, { status })
    await loadPatrolJobs()
  } catch (error) {
    patrolErrorMessage.value = error instanceof Error ? error.message : '任务控制失败'
    toast.error(patrolErrorMessage.value)
  } finally {
    patrolActionLoading.value = false
  }
}

async function handlePatrolReturnHome(dockSn: string) {
  if (!dockSn) return
  patrolActionLoading.value = true
  patrolErrorMessage.value = ''
  try {
    await returnDroneJobHome({ dockSn })
    await loadPatrolJobs()
  } catch (error) {
    patrolErrorMessage.value = error instanceof Error ? error.message : '返航指令下发失败'
    toast.error(patrolErrorMessage.value)
  } finally {
    patrolActionLoading.value = false
  }
}

// ─── Watchers ───
watch(selectedJobStatus, () => {
  void refreshPatrolJobs()
})

watch(patrolPageSize, () => {
  void refreshPatrolJobs()
})

watch(() => createForm.fileId, (fileId) => {
  if (fileId) {
    void loadAiBinding()
  } else {
    aiBindingList.value = []
  }
})

watch(patrolTotalPages, (value) => {
  if (patrolCurrentPage.value > value) {
    patrolCurrentPage.value = value
  }
})

onMounted(() => {
  void refreshAll()
})
</script>

<style scoped>
@import '../admin-shared.css';

.drone-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── 顶部操作栏 ── */
.drone-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.drone-page__header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--fg-text-primary, #eef5ff);
}

.section-meta {
  font-size: 13px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

/* ── 设备卡片区 ── */
.device-cards {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.device-card {
  min-width: 280px;
  max-width: 380px;
  flex: 1;
  border: 1px solid rgba(103, 187, 246, 0.18);
  border-radius: 14px;
  background: rgba(8, 30, 50, 0.6);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.2s;
}

.device-card:hover {
  border-color: rgba(103, 187, 246, 0.35);
}

.device-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.device-card__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-card__icon {
  font-size: 18px;
  line-height: 1;
}

.device-card__icon--dock {
  color: #5ea2ff;
}

.device-card__icon--drone {
  color: #73ebff;
}

.device-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--fg-text-primary, #eef5ff);
}

/* 状态标签 */
.device-card__status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.device-card__status--sm {
  font-size: 11px;
  padding: 2px 8px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.status--idle {
  color: #6ee6a7;
  background: rgba(110, 230, 167, 0.1);
}

.status--idle .status-dot {
  background: #6ee6a7;
  box-shadow: 0 0 6px rgba(110, 230, 167, 0.5);
}

.status--working {
  color: #faad14;
  background: rgba(250, 173, 20, 0.1);
}

.status--working .status-dot {
  background: #faad14;
  box-shadow: 0 0 6px rgba(250, 173, 20, 0.5);
}

.status--offline {
  color: #ff7875;
  background: rgba(255, 120, 117, 0.1);
}

.status--offline .status-dot {
  background: #ff7875;
  box-shadow: 0 0 6px rgba(255, 120, 117, 0.5);
}

.status--other {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  background: rgba(205, 222, 248, 0.06);
}

.status--other .status-dot {
  background: rgba(205, 222, 248, 0.5);
}

/* 机场信息网格 */
.device-card__info {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin: 0;
}

.device-card__info div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-card__info dt {
  font-size: 11px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

.device-card__info dd {
  margin: 0;
  font-size: 13px;
  color: var(--fg-text-primary, #eef5ff);
  word-break: break-all;
}

/* 无人机子区域 */
.drone-sub {
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 10px;
  background: rgba(115, 235, 255, 0.04);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.drone-sub--empty {
  align-items: center;
  justify-content: center;
  padding: 18px 14px;
}

.drone-sub__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.drone-sub__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.drone-sub__name {
  font-size: 14px;
  font-weight: 500;
  color: #73ebff;
}

.drone-sub__info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin: 0;
}

.drone-sub__info div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.drone-sub__info dt {
  font-size: 11px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

.drone-sub__info dd {
  margin: 0;
  font-size: 13px;
  color: var(--fg-text-primary, #eef5ff);
  word-break: break-all;
}

.drone-sub__placeholder {
  font-size: 13px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

/* 卡片操作栏 */
.device-card__actions {
  display: flex;
  gap: 16px;
  border-top: 1px solid rgba(103, 187, 246, 0.1);
  padding-top: 12px;
  margin-top: auto;
}

/* ── 巡查任务区 ── */
.patrol-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-top: 1px solid rgba(103, 187, 246, 0.12);
  padding-top: 20px;
}

.patrol-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.patrol-section__title-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.patrol-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
}

.patrol-filter__select {
  width: 180px;
}

.patrol-filter__select :deep(.el-select__wrapper) {
  min-height: 34px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  background: rgba(2, 8, 16, 0.6);
  box-shadow: none;
}

.patrol-filter__select :deep(.el-select__selected-item) {
  color: #fff;
}

.patrol-filter__select :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.error-text {
  color: #ffb4b4;
  margin: 0;
}

.empty-state {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  padding: 24px 0;
}

.text-muted {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  font-size: 13px;
}

.dialog-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.ai-binding-section {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ai-binding-title {
  margin: 0;
  color: #8dc5ff;
  font-size: 13px;
  font-weight: 500;
}

.ai-binding-empty {
  margin: 12px 0 0;
  color: #8db0d0;
  font-size: 13px;
}

.ai-binding-table-wrap {
  max-height: 260px;
  overflow-y: auto;
  border: 1px solid rgba(103, 187, 246, 0.15);
  border-radius: 4px;
}

.ai-binding-table {
  font-size: 12px;
}

.ai-binding-table th,
.ai-binding-table td {
  padding: 8px 10px;
  white-space: nowrap;
}

.patrol-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  color: rgba(205, 222, 248, 0.78);
  font-size: 13px;
}

.patrol-pagination__total {
  white-space: nowrap;
}

.patrol-pagination__right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pg-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 6px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.pg-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #fff;
}

.pg-btn--active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
  font-weight: 600;
}

.pg-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.pg-size {
  margin-left: 8px;
  padding: 5px 10px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 6px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  font-size: 13px;
  font-family: inherit;
  appearance: none;
  cursor: pointer;
  transition: border-color 0.2s;
}

.pg-size:hover {
  border-color: rgba(64, 158, 255, 0.4);
}

.pg-size:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15);
}

@media (max-width: 720px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }

  .device-cards {
    flex-direction: column;
  }

  .device-card {
    max-width: 100%;
  }
}
</style>
