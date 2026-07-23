<template>
  <PageContainer title="事件详情">
    <NotFoundState
      v-if="!loading && !event"
      title="未发现"
      description="当前记录不存在或已被移除。"
    />

    <section v-else class="event-detail-page">
      <header class="detail-header">
        <div class="detail-header__kicker">
          <span class="detail-header__icon">⚪</span>
          <span>AI告警中心 / 查看</span>
        </div>
        <div class="detail-header__main">
          <div>
            <h3 class="detail-header__title">事件处置详情</h3>
          </div>
          <div class="detail-header__actions">
            <span v-if="event?.urgencyLevel" class="urgency-badge" :class="`urgency-badge--${getUrgencyTone(event.urgencyLevel)}`">{{ getUrgencyLabel(event.urgencyLevel) }}</span>
            <span class="detail-status-chip" :class="`detail-status-chip--${statusTone}`">{{ statusLabel }}</span>
            <button type="button" class="ghost-button" @click="openUrgencyDialog">调整分级</button>
            <button v-if="event?.dispatchable" type="button" class="ghost-button" @click="openIgnoreDialog">
              忽略
            </button>
            <button type="button" class="primary-button" @click="openDispatchDialog">
              {{ event?.dispatchable ? '派单' : '派单' }}
            </button>
          </div>
        </div>
      </header>

      <div class="detail-section">
        <h4 class="section-title">流程与处置信息</h4>
        <div class="detail-info-grid">
          <div class="info-item">
            <span class="info-label">事件名称：</span>
            <span class="info-value">{{ event?.title || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">处置流程：</span>
            <span class="info-value">{{ event?.processTemplateName || '未配置' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">当前节点：</span>
            <span class="info-value">{{ event?.currentNodeName || '待配置流程' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">节点状态：</span>
            <span class="info-value info-value--warning">{{ currentNodeLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">流程状态：</span>
            <span class="info-value info-value--warning">{{ statusLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">最新流转时间：</span>
            <span class="info-value">{{ latestWorkflowRecord?.timestamp || '待发起处置后生成' }}</span>
          </div>
        </div>
      </div>

      <div v-if="selectedTemplateNodes.length" class="detail-section">
        <h4 class="section-title">预设节点承接</h4>
        <div class="template-node-list">
          <article v-for="node in selectedTemplateNodes" :key="node.id" class="template-node-card panel-lite">
            <strong>{{ node.orderNo }}. {{ node.name }}</strong>
            <span>{{ node.roleName }}</span>
            <small>{{ node.mode }}</small>
          </article>
        </div>
      </div>

      <div class="detail-section">
        <h4 class="section-title">告警信息</h4>
        <div class="detail-info-grid">
          <div class="info-item">
            <span class="info-label">告警名称：</span>
            <span class="info-value">{{ event?.title || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">告警类型：</span>
            <span class="info-value">{{ getEventTypeLabel(event?.eventType) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">发现时间：</span>
            <span class="info-value">{{ event?.occurredAt || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属片区：</span>
            <span class="info-value">{{ event?.areaName || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">所属网格：</span>
            <span class="info-value">{{ event?.gridName || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">上报来源：</span>
            <span class="info-value">{{ getEventSourceLabel(event?.reportSource, event?.sourceType) }}</span>
          </div>
          <div class="info-item info-item--span-2">
            <span class="info-label">告警地点：</span>
            <span class="info-value">{{ resolvedLocation || event?.location || '—' }}</span>
          </div>
        </div>

        <h5 v-if="mediaReferences.length" class="section-subtitle">现场照片或视频</h5>
        <div class="detail-map-section">
          <div v-if="mediaReferences.length" class="media-gallery-section">
            <div class="media-gallery">
              <div
                v-for="(reference, idx) in mediaReferences"
                :key="reference"
                class="media-thumb"
                :title="reference"
                @click="openPreview(idx)"
              >
                <img :src="reference" :alt="`告警证据 ${idx + 1}`" class="media-thumb-img" @error="onImgError">
              </div>
            </div>
          </div>
          <AMapPointViewer
            v-if="event"
            :longitude="event.longitude"
            :latitude="event.latitude"
            :label="coordinateText"
            :areas="areaOverlays"
          />
        </div>
      </div>

      <ImagePreviewOverlay
        v-model="previewVisible"
        :images="mediaReferences"
        :index="previewIndex"
        @update:index="previewIndex = $event"
      />
    </section>

    <SystemDialog :open="urgencyDialogOpen" title="调整紧急程度" subtitle="三色分级" @close="closeUrgencyDialog">
      <div v-if="event" class="dispatch-dialog">
        <div class="dispatch-summary panel-lite">
          <p class="dispatch-summary__code">{{ event.eventCode }}</p>
          <h4>{{ event.title }}</h4>
        </div>
        <label class="field-stack">
          <span>紧急程度</span>
          <select v-model="urgencyForm.urgencyLevel" aria-label="紧急程度">
            <option value="GREEN">一般（绿色）</option>
            <option value="YELLOW">重点（黄色）</option>
            <option value="RED">紧急（红色）</option>
          </select>
        </label>
      </div>
      <template #footer>
        <button type="button" class="ghost-button" @click="closeUrgencyDialog">取消</button>
        <button type="button" class="primary-button" :disabled="urgencySubmitting" @click="submitUrgency">
          {{ urgencySubmitting ? '提交中...' : '确认调整' }}
        </button>
      </template>
    </SystemDialog>

    <SystemDialog :open="ignoreDialogOpen" title="忽略事件" subtitle="事件处置" @close="closeIgnoreDialog">
      <div v-if="event" class="dispatch-dialog">
        <div class="dispatch-summary panel-lite">
          <p class="dispatch-summary__code">{{ event.eventCode }}</p>
          <h4>{{ event.title }}</h4>
        </div>
        <label class="field-stack">
          <span>忽略原因</span>
          <textarea v-model="ignoreReason" aria-label="忽略原因" rows="4" placeholder="请输入忽略原因（必填）"></textarea>
        </label>
      </div>
      <template #footer>
        <button type="button" class="ghost-button" @click="closeIgnoreDialog">取消</button>
        <button type="button" class="primary-button" :disabled="ignoreSubmitting" @click="submitIgnore">
          {{ ignoreSubmitting ? '提交中...' : '确认忽略' }}
        </button>
      </template>
    </SystemDialog>

    <SystemDialog :open="dialogOpen" title="选择处置流程" subtitle="事件处置" @close="closeDialog">
      <div v-if="event" class="dispatch-dialog">
        <div class="dispatch-summary panel-lite">
          <p class="dispatch-summary__code">{{ event.eventCode }}</p>
          <h4>{{ event.title }}</h4>
          <p>{{ event.location || event.area || '未配置位置' }}</p>
        </div>

        <label class="field-stack">
          <span>流程模板</span>
          <select v-model="dispatchForm.processTemplateId" aria-label="流程模板">
            <option :value="0" disabled hidden>请选择流程模板</option>
            <option v-for="item in processOptions" :key="item.id" :value="item.id">
              {{ item.name }} {{ item.version }} / {{ item.nodeCount }}节点
            </option>
          </select>
        </label>

        <p v-if="selectedTemplate" class="dispatch-template-copy">{{ selectedTemplate.description }}</p>

        <div v-if="selectedTemplate?.nodes.length" class="dispatch-node-panel panel-lite">
          <h5>预设节点承接</h5>
          <div class="dispatch-node-list">
            <div v-for="node in selectedTemplate.nodes" :key="node.id" class="dispatch-node-item">
              <span>{{ node.orderNo }}. {{ node.name }}</span>
              <small>{{ node.roleName }}</small>
            </div>
          </div>
        </div>

        <label class="field-stack">
          <span>补充说明</span>
          <textarea v-model="dispatchForm.remark" aria-label="补充说明" rows="4" placeholder="请输入补充说明"></textarea>
        </label>

      </div>

      <template #footer>
        <button type="button" class="ghost-button" @click="closeDialog">取消</button>
        <button type="button" class="primary-button" :disabled="dispatchSubmitting" @click="submitDispatch">
          {{ dispatchSubmitting ? '提交中...' : '确认派单' }}
        </button>
      </template>
    </SystemDialog>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  dispatchEventDirectly,
  getEventDetail,
  getEventStatusLabel,
  getEventTypeLabel,
  getLifecycleRecordDesc,
  getLifecycleRecordTitle,
  getSourceSystemLabel,
  getUrgencyLabel,
  getUrgencyTone,
  getWorkflowNodeStatusLabel,
  ignoreEvent,
  listAvailableProcessTemplates,
  updateEventUrgency,
  type EventDetail,
  type EventProcessTemplateNode,
  type EventProcessTemplateOption
} from '../../api/event'
import AMapPointViewer from '../../components/biz/AMapPointViewer.vue'
import { listBizAreas, type BizArea } from '../../api/biz-area'
import NotFoundState from '../../components/admin/NotFoundState.vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import SystemDialog from '../../components/system/SystemDialog.vue'
import ImagePreviewOverlay from '../../components/ImagePreviewOverlay.vue'
import { useToast } from '../../composables/useToast'

interface DispatchFormState {
  processTemplateId: number
  remark: string
}

const route = useRoute()
const event = ref<EventDetail>()
const loading = ref(false)
const bizAreas = ref<BizArea[]>([])
const dialogOpen = ref(false)
const processOptions = ref<EventProcessTemplateOption[]>([])
const dispatchSubmitting = ref(false)
const dialogErrorMessage = ref('')
const toast = useToast()
const dispatchForm = reactive<DispatchFormState>({
  processTemplateId: 0,
  remark: ''
})
const selectedTemplateNodes = ref<EventProcessTemplateNode[]>([])
const previewVisible = ref(false)
const previewIndex = ref(0)
const resolvedLocation = ref('')

const eventId = computed(() => Number(route.params.id))
const statusLabel = computed(() => getEventStatusLabel(event.value?.currentStatus))
const currentNodeLabel = computed(() => getWorkflowNodeStatusLabel(event.value?.currentNodeStatus))
const coordinateText = computed(() => {
  if (!event.value) return '—'
  const lng = event.value.longitude
  const lat = event.value.latitude
  if (lng && lat) return `${Number(lng).toFixed(6)}, ${Number(lat).toFixed(6)}`
  return event.value.location || '—'
})
const mediaReferences = computed(() => event.value?.evidenceReferences || [])
const INTERNAL_RECORD_TITLES = new Set(['EVENT_INTAKE', 'WORKFLOW_SYNC'])
const workflowRecords = computed(() =>
  (event.value?.lifecycleRecords || []).filter((r) => !INTERNAL_RECORD_TITLES.has(r.title))
)
const latestWorkflowRecord = computed(() => {
  const records = event.value?.lifecycleRecords || []
  return records.length ? records[records.length - 1] : undefined
})
const selectedTemplate = computed(() => processOptions.value.find((item) => item.id === dispatchForm.processTemplateId))
const statusTone = computed(() => {
  switch (event.value?.currentStatus) {
    case 'PENDING_AUDIT':
    case 'WAITING_DISPATCH':
      return 'warning'
    case 'AUDITING':
    case 'PROCESSING':
      return 'primary'
    case 'WAITING_CLOSE_CONFIRM':
      return 'info'
    case 'CLOSED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'IGNORED':
      return 'info'
    default:
      return 'info'
  }
})

const areaOverlays = computed(() =>
  bizAreas.value
    .filter((a) => a.status === 'ACTIVE' && a.roiJson)
    .map((a) => ({ areaName: a.areaName, roiJson: a.roiJson! }))
)

const urgencyDialogOpen = ref(false)
const urgencySubmitting = ref(false)
const urgencyForm = reactive({ urgencyLevel: 'GREEN' as string })

const ignoreDialogOpen = ref(false)
const ignoreReason = ref('')
const ignoreSubmitting = ref(false)
const ignoreErrorMessage = ref('')

function getEventSourceLabel(reportSource?: string | null, sourceType?: string | null): string {
  if (reportSource) {
    const map: Record<string, string> = { PATROL: '巡查上报', RESIDENT: '居民上报', DRONE: '无人机', MANUAL: '手动录入', SYSTEM: '系统生成' }
    return map[reportSource] || reportSource
  }
  if (sourceType) {
    const map: Record<string, string> = { DIRECT_REPORT: '直接上报', DRONE_ALARM: '无人机告警', PATROL: '巡查上报', RESIDENT: '居民上报' }
    return map[sourceType] || sourceType
  }
  return '—'
}

function openUrgencyDialog() {
  urgencyForm.urgencyLevel = event.value?.urgencyLevel || 'GREEN'
  urgencyDialogOpen.value = true
}

function closeUrgencyDialog() {
  urgencyDialogOpen.value = false
}

async function submitUrgency() {
  if (!event.value) return
  urgencySubmitting.value = true
  try {
    await updateEventUrgency(event.value.id, urgencyForm.urgencyLevel)
    event.value = { ...event.value, urgencyLevel: urgencyForm.urgencyLevel }
    closeUrgencyDialog()
  } catch (error) {
    console.error('调整紧急程度失败', error)
  } finally {
    urgencySubmitting.value = false
  }
}

function openIgnoreDialog() {
  ignoreReason.value = ''
  ignoreErrorMessage.value = ''
  ignoreDialogOpen.value = true
}

function closeIgnoreDialog() {
  ignoreDialogOpen.value = false
  ignoreReason.value = ''
  ignoreErrorMessage.value = ''
}

async function submitIgnore() {
  if (!event.value) return
  const reason = ignoreReason.value.trim()
  if (!reason) {
    ignoreErrorMessage.value = '请输入忽略原因'
    return
  }
  ignoreSubmitting.value = true
  ignoreErrorMessage.value = ''
  try {
    await ignoreEvent(event.value.id, reason)
    event.value = { ...event.value, currentStatus: 'IGNORED', dispatchable: false }
    closeIgnoreDialog()
  } catch (error) {
    ignoreErrorMessage.value = error instanceof Error ? error.message : '忽略失败，请稍后重试'
  } finally {
    ignoreSubmitting.value = false
  }
}

function resetDispatchForm() {
  dispatchForm.processTemplateId = 0
  dispatchForm.remark = ''
  dialogErrorMessage.value = ''
}

function openPreview(index: number) {
  previewIndex.value = index
  previewVisible.value = true
}

function closePreview() {
  previewVisible.value = false
}

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

async function loadEventDetail(id: number) {
  loading.value = true
  resolvedLocation.value = ''
  try {
    const [eventData] = await Promise.all([
      getEventDetail(id),
      listBizAreas({ status: 'ACTIVE' }).then((areas) => { bizAreas.value = areas }).catch(() => { bizAreas.value = [] })
    ])
    event.value = eventData
    // Reverse geocode if location is empty but coordinates exist
    if (eventData && !eventData.location && eventData.longitude && eventData.latitude) {
      reverseGeocode(eventData.longitude, eventData.latitude)
    }
  } finally {
    loading.value = false
  }
}

async function reverseGeocode(lng: number, lat: number) {
  try {
    const AMapLoader = (await import('@amap/amap-jsapi-loader')).default
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Geocoder'] })
    const geocoder = new AMap.Geocoder()
    geocoder.getAddress([lng, lat], (status: string, result: any) => {
      if (status === 'complete' && result?.regeocode?.formattedAddress) {
        resolvedLocation.value = result.regeocode.formattedAddress
      }
    })
  } catch {
    // geocoding is non-critical, silently ignore
  }
}

async function openDispatchDialog() {
  if (!event.value) {
    return
  }

  resetDispatchForm()
  processOptions.value = await listAvailableProcessTemplates(event.value.eventType)
  dispatchForm.processTemplateId = event.value.processTemplateId ?? processOptions.value[0]?.id ?? 0
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  processOptions.value = []
  resetDispatchForm()
}

async function submitDispatch() {
  if (!event.value) {
    return
  }
  if (!dispatchForm.processTemplateId) {
    dialogErrorMessage.value = '请选择流程模板'
    return
  }

  const currentEvent = event.value
  const remark = dispatchForm.remark.trim()

  dispatchSubmitting.value = true
  dialogErrorMessage.value = ''

  try {
    const result = await dispatchEventDirectly(currentEvent.id, {
      processTemplateId: dispatchForm.processTemplateId,
      remark: remark || undefined
    })

    const template = selectedTemplate.value
    const templateName = template ? `${template.name} ${template.version}` : currentEvent.processTemplateName
    selectedTemplateNodes.value = template?.nodes || selectedTemplateNodes.value

    event.value = {
      ...currentEvent,
      currentStatus: result.currentStatus,
      processTemplateId: result.processTemplateId,
      processTemplateName: result.processTemplateName || templateName,
      currentNodeName: result.currentNodeName,
      currentNodeStatus: result.currentNodeStatus,
      dispatchable: false,
      lifecycleRecords: [
        {
          id: `dispatch-${Date.now()}`,
          title: '处置已发起',
          description: remark || '已按预设流程发起处置。',
          operator: '系统',
          timestamp: new Date().toISOString().slice(0, 16).replace('T', ' '),
          status: 'DISPATCHED'
        },
        ...currentEvent.lifecycleRecords
      ]
    }

    closeDialog()
  } catch (error) {
    dialogErrorMessage.value = error instanceof Error ? error.message : '发起处置失败'
    toast.error(dialogErrorMessage.value)
  } finally {
    dispatchSubmitting.value = false
  }
}

watch(selectedTemplate, (template) => {
  if (template) {
    selectedTemplateNodes.value = template.nodes
  }
})

watch(
  eventId,
  (id) => {
    if (!Number.isFinite(id)) {
      event.value = undefined
      selectedTemplateNodes.value = []
      return
    }
    void loadEventDetail(id)
  },
  { immediate: true }
)
</script>

<style scoped>
@import '../admin-shared.css';

.event-detail-page {
  display: flex;
  flex-direction: column;
  gap: 32px;
  min-height: calc(100vh - 120px);
  padding-bottom: 40px;
}

.detail-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-header__kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8db0d0;
  font-size: 13px;
}

.detail-header__icon {
  font-size: 14px;
}

.detail-header__main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-header__title {
  margin: 0;
  color: #eaf5ff;
  font-size: 18px;
}

.detail-header__copy {
  margin: 8px 0 0;
  color: #8db0d0;
  font-size: 13px;
}

.detail-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-title {
  margin: 0;
  color: #eaf5ff;
  font-size: 15px;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(103, 187, 246, 0.1);
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px 24px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.info-item--span-2 {
  grid-column: span 2;
}

.info-label {
  color: #8db0d0;
  white-space: nowrap;
}

.info-value {
  color: #eaf5ff;
}

.info-value--warning {
  color: #f7ba5a;
}

.detail-status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
}

.detail-status-chip--warning {
  background: rgba(230, 162, 60, 0.18);
  color: #f7ba5a;
}

.detail-status-chip--primary {
  background: rgba(64, 158, 255, 0.16);
  color: #8dc5ff;
}

.detail-status-chip--success {
  background: rgba(103, 194, 58, 0.16);
  color: #8ce56d;
}

.detail-status-chip--danger {
  background: rgba(245, 108, 108, 0.16);
  color: #ff9d9d;
}

.detail-status-chip--info {
  background: rgba(144, 147, 153, 0.16);
  color: #d0d2d6;
}

.panel-lite {
  padding: 14px;
  border-radius: 10px;
  border: 1px solid rgba(103, 187, 246, 0.16);
  background: rgba(8, 30, 50, 0.55);
}

.template-node-list,
.dispatch-node-list,
.timeline-list,
.dispatch-dialog {
  display: grid;
  gap: 16px;
}

.template-node-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.template-node-card,
.dispatch-node-item {
  display: grid;
  gap: 6px;
}

.template-node-card strong,
.dispatch-node-item span {
  color: #eaf5ff;
}

.template-node-card span,
.dispatch-node-item small,
.template-node-card small,
.event-state-text,
.dispatch-summary p,
.dispatch-template-copy,
.timeline-item__content p {
  color: #9fb3c7;
  font-size: 13px;
}

.dispatch-node-panel h5,
.dispatch-summary h4,
.timeline-item__header strong {
  margin: 0;
  color: #f0f7ff;
}

.dispatch-summary__code {
  margin: 0 0 8px;
  color: #8db0d0;
  font-size: 12px;
}

.dispatch-summary h4,
.dispatch-summary p,
.dispatch-template-copy,
.timeline-item__content p {
  margin: 0;
}

.field-stack {
  display: grid;
  gap: 8px;
}

.field-stack span {
  color: #d0dfed;
  font-size: 14px;
}

.field-stack select,
.field-stack textarea {
  min-height: 32px;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-radius: 3px;
  background: #1a344b;
  color: #eaf5ff;
  font: inherit;
  padding: 0 12px;
}

.field-stack textarea {
  min-height: 96px;
  padding: 10px 12px;
  resize: vertical;
}

/* ── 节点进度：横向流式布局 ── */
.timeline-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  align-items: stretch;
}

.timeline-step {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 12px;
  background: rgba(8, 30, 50, 0.5);
  min-width: 160px;
  flex: 1;
}

.timeline-step__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.timeline-step__title {
  color: var(--fg-text-primary, #eef5ff);
  font-size: 13px;
}

.timeline-step__desc {
  color: #8db0d0;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.timeline-step__operator {
  color: #8dc5ff;
  font-size: 12px;
}

.timeline-step__right {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: auto;
}

.timeline-step__time {
  color: #8db0d0;
  font-size: 12px;
  white-space: nowrap;
}

.timeline-step__status {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  color: #73ebff;
}

.timeline-flow__arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(64, 158, 255, 0.5);
  font-size: 20px;
  padding: 0 10px;
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .timeline-flow {
    flex-direction: column;
    gap: 0;
  }
  .timeline-step {
    max-width: 100%;
  }
  .timeline-flow__arrow {
    transform: rotate(90deg);
    padding: 6px 0;
  }
}

.detail-map-section {
  width: 100%;
  display: flex;
  flex-direction: row;
  gap: 20px;
  margin-top: 8px;
  align-items: flex-start;
}

.media-gallery-section {
  flex: 0 0 auto;
  width: 280px;
  min-width: 180px;
}

.detail-map-section > :deep(.amap-point-viewer) {
  flex: 1 1 0;
  min-width: 0;
  height: 480px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid rgba(103, 187, 246, 0.18);
}

@media (max-width: 900px) {
  .detail-map-section {
    flex-direction: column;
  }
  .media-gallery-section {
    width: 100%;
  }
}

.media-gallery-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-subtitle {
  margin: 0;
  font-size: 13px;
  font-weight: 500;
  color: #8dc5ff;
}

.media-gallery {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.media-thumb {
  width: 320px;
  height: 240px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(103, 187, 246, 0.2);
  background: rgba(7, 27, 46, 0.5);
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s;
}

.media-thumb:hover {
  border-color: rgba(64, 158, 255, 0.6);
  transform: scale(1.03);
}

.media-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 1024px) {
  .detail-header__main,
  .detail-info-grid,
  .template-node-list {
    grid-template-columns: repeat(2, 1fr);
  }

  .detail-header__main {
    display: grid;
  }
}

@media (max-width: 720px) {
  .detail-info-grid,
  .template-node-list {
    grid-template-columns: 1fr;
  }

  .info-item--span-2 {
    grid-column: span 1;
  }

  .detail-header__actions,
  .timeline-item__header,
  .timeline-item__meta {
    flex-direction: column;
    align-items: flex-start;
  }
}

.urgency-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #fff;
}
.urgency-badge--green { background: #16a34a; }
.urgency-badge--yellow { background: #ca8a04; }
.urgency-badge--red { background: #dc2626; }
.urgency-badge--none { background: #6b7280; }
</style>
