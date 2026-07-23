<template>
  <PageContainer title="事件中心">
    <section class="event-list-page">
      <section class="event-filter-panel panel" aria-label="事件筛选面板">
        <header class="event-filter-panel__header">
          <h3 class="event-filter-panel__title">查询条件</h3>
          <button type="button" class="ghost-button" @click="handleExport">导出台账</button>
        </header>

        <div class="event-filter-panel__form" data-testid="event-filter-form">
          <label class="field-stack field-stack--wide">
            <ElInput v-model="draftKeyword" aria-label="告警名称" placeholder="请输入告警名称" clearable />
          </label>

          <label class="field-stack">
            <select v-model="draftUrgencyLevel" aria-label="紧急程度">
              <option value="">全部等级</option>
              <option value="RED">紧急</option>
              <option value="YELLOW">重点</option>
              <option value="GREEN">一般</option>
            </select>
          </label>

          <label class="field-stack field-stack--date-range">
            <ZhDateRangePicker v-model:start="draftStartDate" v-model:end="draftEndDate" placeholder="选择发现时间范围" />
          </label>

          <div class="event-filter-panel__actions">
            <button type="button" class="primary-button" @click="applyFilters">查询</button>
            <button type="button" class="ghost-button" @click="resetFilters">重置</button>
          </div>
        </div>
      </section>

      <section class="event-content-section">
        <p v-if="pagination.loading.value" class="event-state-text">加载中...</p>
        <p v-else-if="!pagination.items.value.length" class="event-state-text">暂无事件数据</p>

        <section v-else class="event-card-grid" aria-label="事件卡片列表">
          <article v-for="item in pagination.items.value" :key="item.id" class="event-card panel" data-testid="event-card">
            <div class="event-card__media">
              <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" class="event-card__media-image" />
              <div v-else class="event-card__media-overlay"></div>
            </div>

            <div class="event-card__body">
              <div class="event-card__headline">
                <div class="event-card__title-row">
                  <span class="event-card__icon">▣</span>
                  <strong>{{ item.title || '未命名事件' }}</strong>
                </div>
              </div>

              <p class="event-card__code">{{ item.shortCode }}</p>

              <div class="event-card__meta-row">
                <span>◌ 发现时间</span>
                <div class="event-card__meta-value">{{ item.occurredAt }}</div>
              </div>

              <div class="event-card__meta-row">
                <span>◉ 所属片区</span>
                <div class="event-card__meta-value">{{ item.districtName }}</div>
              </div>

              <div class="event-card__meta-row">
                <span>◫ 流程状态</span>
                <div class="event-card__status-wrap">
                  <span class="event-status-chip" :class="`event-status-chip--${item.statusTone}`">{{ item.statusLabel }}</span>
                  <span v-if="item.urgencyLevel" class="urgency-chip" :class="`urgency-chip--${item.urgencyLevel.toLowerCase()}`">{{ urgencyLabel(item.urgencyLevel) }}</span>
                </div>
              </div>

              <div class="event-card__meta-row">
                <span>◎ 当前节点</span>
                <div class="event-card__meta-stack">
                  <strong>{{ item.currentNodeName || '待配置流程' }}</strong>
                  <small>{{ item.currentNodeLabel }}</small>
                </div>
              </div>

              <div class="event-card__meta-row">
                <span>◈ 处置流程</span>
                <div class="event-card__meta-stack">
                  <strong>{{ item.processTemplateName || '未配置' }}</strong>
                  <small>{{ item.processHint }}</small>
                </div>
              </div>

              <div v-if="item.processNodes.length" class="event-card__node-list">
                <div v-for="node in item.processNodes" :key="node.id" class="event-card__node-item">
                  <span>{{ node.orderNo }}. {{ node.name }}</span>
                  <small>{{ node.roleName }}</small>
                </div>
              </div>

              <div class="event-card__actions">
                <button type="button" class="primary-button primary-button--small" :disabled="!item.dispatchable" @click="openDispatchDialog(item)">
                  {{ item.dispatchable ? '派单' : '已派单' }}
                </button>
                <button v-if="item.dispatchable" type="button" class="ghost-button ghost-button--small" @click="openIgnoreDialog(item)">忽略</button>
                <RouterLink :to="`/events/${item.id}`" class="ghost-link">查看详情</RouterLink>
              </div>
            </div>
          </article>
        </section>

        <footer class="event-pagination" aria-label="分页">
          <ListPagination
            :total="pagination.total.value"
            :current-page="pagination.currentPage.value"
            :page-size="pagination.pageSize"
            :disabled="pagination.loading.value"
            @change="pagination.changePage"
          />
        </footer>
      </section>
    </section>

    <SystemDialog :open="dialogOpen" title="选择处置流程" subtitle="事件处置" @close="closeDialog">
      <div v-if="selectedEvent" class="dispatch-dialog">
        <div class="dispatch-summary panel-lite">
          <p class="dispatch-summary__code">{{ selectedEvent.eventCode }}</p>
          <h4>{{ selectedEvent.title }}</h4>
          <p>{{ selectedEvent.area || '未配置位置' }}</p>
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
          <textarea v-model="dispatchForm.remark" aria-label="补充说明" placeholder="请输入补充说明" rows="4"></textarea>
        </label>

      </div>

      <template #footer>
        <button type="button" class="ghost-button" @click="closeDialog">取消</button>
        <button type="button" class="primary-button" :disabled="dispatchSubmitting" @click="submitDispatch">
          {{ dispatchSubmitting ? '提交中...' : '确认派单' }}
        </button>
      </template>
    </SystemDialog>

    <SystemDialog :open="ignoreDialogOpen" title="忽略事件" subtitle="事件处置" @close="closeIgnoreDialog">
      <div v-if="ignoreTarget" class="dispatch-dialog">
        <div class="dispatch-summary panel-lite">
          <p class="dispatch-summary__code">{{ ignoreTarget.eventCode }}</p>
          <h4>{{ ignoreTarget.title }}</h4>
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
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  dispatchEventDirectly,
  getEventStatusLabel,
  getWorkflowNodeStatusLabel,
  ignoreEvent,
  listAvailableProcessTemplates,
  listEventsPaged,
  type EventListItem,
  type EventProcessTemplateNode,
  type EventProcessTemplateOption
} from '../../api/event'
import PageContainer from '../../components/admin/PageContainer.vue'
import SystemDialog from '../../components/system/SystemDialog.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import { usePagination } from '../../composables/usePagination'
import ZhDateRangePicker from '../../components/common/ZhDateRangePicker.vue'
import { useToast } from '../../composables/useToast'

interface EventCardItem extends EventListItem {
  shortCode: string
  occurredDate: string
  statusLabel: string
  statusTone: 'warning' | 'primary' | 'success' | 'danger' | 'info'
  currentNodeLabel: string
  processHint: string
  processNodes: EventProcessTemplateNode[]
  coverImage?: string
  districtName: string
  urgencyLevel?: string
}

function urgencyLabel(level?: string) {
  return level === 'RED' ? '紧急' : level === 'YELLOW' ? '重点' : level === 'GREEN' ? '一般' : ''
}

interface DispatchFormState {
  processTemplateId: number
  remark: string
}

const draftKeyword = ref('')
const draftStartDate = ref('')
const draftEndDate = ref('')

// Applied filters (committed on query button click)
const appliedFilters = ref({ keyword: '', startDate: '', endDate: '', urgencyLevel: '' })

const draftUrgencyLevel = ref('')

const dialogOpen = ref(false)
const selectedEvent = ref<EventCardItem>()
const processOptions = ref<EventProcessTemplateOption[]>([])
const toast = useToast()
const dispatchSubmitting = ref(false)
const dialogErrorMessage = ref('')
const dispatchForm = reactive<DispatchFormState>({
  processTemplateId: 0,
  remark: ''
})

function statusToneOf(status: EventListItem['currentStatus']) {
  switch (status) {
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
}

function isImageReference(reference?: string) {
  if (!reference) {
    return false
  }

  const normalized = reference.toLowerCase().split('?')[0]
  return (
    normalized.startsWith('data:image/') ||
    /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(normalized)
  )
}

function resolveCoverImage(item: EventListItem) {
  return item.evidenceReferences.find((reference) => isImageReference(reference))
}

function toCardItem(item: EventListItem): EventCardItem {
  const area = item.area || ''
  return {
    ...item,
    shortCode: item.eventCode || area.replace('常平镇', '') || `EVT-${item.id}`,
    occurredDate: item.occurredAt.slice(0, 10),
    statusLabel: getEventStatusLabel(item.currentStatus),
    statusTone: statusToneOf(item.currentStatus),
    currentNodeLabel: getWorkflowNodeStatusLabel(item.currentNodeStatus),
    processHint: item.processTemplateName ? '按预设节点直接发起处置' : '需先配置处置流程',
    processNodes: [],
    coverImage: resolveCoverImage(item),
    districtName: item.areaName || item.area || '无'
  }
}

function patchEventCard(item: EventCardItem, patch: Partial<EventListItem>) {
  return toCardItem({ ...item, ...patch })
}

const pagination = usePagination<EventCardItem, { keyword: string; startDate: string; endDate: string; urgencyLevel: string }>({
  pageSize: 10,
  filters: appliedFilters,
  fetcher: async (page, pageSize, filters) => {
    let result = await listEventsPaged(page, pageSize, {
      keyword: filters.keyword,
      startDate: filters.startDate,
      endDate: filters.endDate
    })
    if (filters.urgencyLevel) {
      const filtered = result.items.filter(i => i.urgencyLevel === filters.urgencyLevel)
      result = { ...result, items: filtered, total: filtered.length }
    }
    const items = result.items.map(toCardItem)
    return { items, total: result.total, page: result.page, pageSize: result.pageSize }
  }
})

const selectedTemplate = computed(() => processOptions.value.find((item) => item.id === dispatchForm.processTemplateId))

function applyFilters() {
  appliedFilters.value = {
    keyword: draftKeyword.value.trim(),
    startDate: draftStartDate.value,
    endDate: draftEndDate.value,
    urgencyLevel: draftUrgencyLevel.value
  }
  pagination.resetAndReload()
}

function resetFilters() {
  draftKeyword.value = ''
  draftStartDate.value = ''
  draftEndDate.value = ''
  draftUrgencyLevel.value = ''
  applyFilters()
}

function handleExport() {
  window.open('/api/community/export/events', '_blank')
}

function resetDispatchForm() {
  dispatchForm.processTemplateId = 0
  dispatchForm.remark = ''
  dialogErrorMessage.value = ''
}

async function openDispatchDialog(item: EventCardItem) {
  selectedEvent.value = item
  resetDispatchForm()
  processOptions.value = await listAvailableProcessTemplates(item.eventType)
  dispatchForm.processTemplateId = item.processTemplateId ?? processOptions.value[0]?.id ?? 0
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
  selectedEvent.value = undefined
  processOptions.value = []
  resetDispatchForm()
}

async function submitDispatch() {
  if (!selectedEvent.value) {
    return
  }
  if (!dispatchForm.processTemplateId) {
    dialogErrorMessage.value = '请选择流程模板'
    return
  }

  const currentEvent = selectedEvent.value
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

    pagination.items.value = pagination.items.value.map((item) =>
      item.id === currentEvent.id
        ? {
            ...patchEventCard(item, {
              currentStatus: result.currentStatus,
              processTemplateId: result.processTemplateId,
              processTemplateName: result.processTemplateName || templateName,
              currentNodeName: result.currentNodeName,
              currentNodeStatus: result.currentNodeStatus,
              dispatchable: false
            }),
            processNodes: []
          }
        : item
    )

    closeDialog()
  } catch (error) {
    dialogErrorMessage.value = error instanceof Error ? error.message : '发起处置失败'
    toast.error(dialogErrorMessage.value)
  } finally {
    dispatchSubmitting.value = false
  }
}

// Ignore dialog state
const ignoreDialogOpen = ref(false)
const ignoreTarget = ref<EventCardItem>()
const ignoreReason = ref('')
const ignoreSubmitting = ref(false)
const ignoreErrorMessage = ref('')

function openIgnoreDialog(item: EventCardItem) {
  ignoreTarget.value = item
  ignoreReason.value = ''
  ignoreErrorMessage.value = ''
  ignoreDialogOpen.value = true
}

function closeIgnoreDialog() {
  ignoreDialogOpen.value = false
  ignoreTarget.value = undefined
  ignoreReason.value = ''
  ignoreErrorMessage.value = ''
}

async function submitIgnore() {
  if (!ignoreTarget.value) return
  const reason = ignoreReason.value.trim()
  if (!reason) {
    ignoreErrorMessage.value = '请输入忽略原因'
    return
  }
  ignoreSubmitting.value = true
  ignoreErrorMessage.value = ''
  try {
    await ignoreEvent(ignoreTarget.value.id, reason)
    pagination.items.value = pagination.items.value.map((item) =>
      item.id === ignoreTarget.value!.id
        ? patchEventCard(item, { currentStatus: 'IGNORED', dispatchable: false })
        : item
    )
    closeIgnoreDialog()
  } catch (error) {
    ignoreErrorMessage.value = error instanceof Error ? error.message : '忽略失败，请稍后重试'
  } finally {
    ignoreSubmitting.value = false
  }
}

onMounted(() => {
  pagination.resetAndReload()
})
</script>

<style scoped>
@import '../admin-shared.css';

.event-list-page,
.event-content-section {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.panel {
  border: none;
  background: transparent;
  color: #eaf5ff;
}

.panel-lite {
  padding: 14px;
  border-radius: 10px;
  border: 1px solid rgba(103, 187, 246, 0.16);
  background: rgba(8, 30, 50, 0.55);
}

.event-filter-panel {
  padding: 0;
}

.event-filter-panel__header {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 2px;
}

.event-filter-panel__title {
  margin: 0;
  font-size: 18px;
  color: #eaf5ff;
}

.event-filter-panel__form {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 14px 18px;
  align-items: center;
  min-width: 0;
}

.field-stack {
  display: grid;
  gap: 8px;
  flex: 0 0 220px;
  min-width: 0;
}

.field-stack--wide {
  flex-basis: 280px;
}

.field-stack--date-range {
  flex-basis: 360px;
}

.field-stack span {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  font-size: 14px;
}

.field-stack .el-input,
.field-stack .el-select {
  width: 100%;
  height: 40px;
  --el-component-size: 40px;
  --el-input-height: 40px;
}

.field-stack :deep(.el-input__wrapper),
.field-stack :deep(.el-select__wrapper) {
  box-sizing: border-box;
  height: 40px !important;
  min-height: 40px !important;
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 8px;
  background: rgba(2, 8, 16, 0.6);
  box-shadow: none;
  transition: all 0.2s ease;
}

.field-stack :deep(.el-input__wrapper:hover),
.field-stack :deep(.el-select__wrapper:hover) {
  border-color: rgba(64, 158, 255, 0.4);
}

.field-stack :deep(.el-input__wrapper.is-focus),
.field-stack :deep(.el-select__wrapper.is-focused) {
  border-color: #409eff;
  background: rgba(2, 8, 16, 0.9);
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.15);
}

.field-stack :deep(.el-input__inner),
.field-stack :deep(.el-select__selected-item) {
  color: #fff;
}

.field-stack :deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.field-stack :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.field-stack textarea {
  min-height: 96px;
  padding: 10px 12px;
  resize: vertical;
}

.event-filter-panel__actions {
  display: flex;
  gap: 12px;
  align-self: center;
}

.primary-button--small,
.ghost-button--small,
.ghost-link {
  min-width: 86px;
  min-height: 28px;
  padding: 0 12px;
}

.event-state-text {
  margin: 0;
  color: #d9e8f5;
  font-size: 14px;
}

.event-card-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  align-items: start;
}

.event-card {
  overflow: hidden;
  border-radius: 6px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  background: rgba(8, 30, 50, 0.8);
}

.event-card__media {
  position: relative;
  height: 118px;
  background: linear-gradient(135deg, rgba(18, 14, 24, 0.96), rgba(23, 34, 42, 0.96));
}

.event-card__media-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 70% 35%, rgba(91, 134, 255, 0.22), transparent 12%), rgba(0, 0, 0, 0.28);
}

.event-card__media-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.event-card__detect-box {
  position: absolute;
  left: 64%;
  top: 34%;
  width: 14px;
  height: 18px;
  border: 2px solid rgba(89, 144, 255, 0.92);
  box-shadow: 0 0 10px rgba(89, 144, 255, 0.4);
}

.event-card__detect-label {
  position: absolute;
  left: calc(64% - 4px);
  top: calc(34% - 10px);
  padding: 1px 4px;
  border-radius: 2px;
  background: rgba(46, 97, 220, 0.88);
  color: #fff;
  font-size: 9px;
}

.event-card__body {
  display: grid;
  gap: 8px;
  padding: 8px;
}

.event-card__headline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.event-card__meta-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.event-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 4px;
  margin-top: 6px;
}

.event-card__title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.event-card__title-row strong {
  font-size: 13px;
  color: #f0f7ff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-card__icon {
  color: #d6dee7;
  font-size: 11px;
  flex-shrink: 0;
}

.event-card__code {
  margin: 0;
  color: #7f98af;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-card__meta-row > span {
  color: #8ca3b9;
  font-size: 11px;
  white-space: nowrap;
  flex-shrink: 0;
}

.event-card__meta-value,
.event-card__status-wrap,
.event-card__meta-stack {
  text-align: right;
  color: #dce9f5;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
}

.event-card__meta-stack {
  display: grid;
  gap: 2px;
  justify-items: end;
}

.event-card__meta-stack strong,
.event-card__meta-stack small {
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-card__meta-stack small {
  color: #8ca3b9;
}

.event-card__node-list,
.dispatch-node-list {
  display: grid;
  gap: 6px;
}

.event-card__node-item,
.dispatch-node-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 12px;
  color: #dce9f5;
}

.event-card__node-item small,
.dispatch-node-item small {
  color: #8ca3b9;
}

.dispatch-node-panel h5 {
  margin: 0 0 8px;
  color: #f0f7ff;
  font-size: 14px;
}

.event-status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 11px;
  white-space: nowrap;
}

.event-status-chip--warning {
  background: rgba(230, 162, 60, 0.18);
  color: #f7ba5a;
}

.event-status-chip--primary {
  background: rgba(64, 158, 255, 0.16);
  color: #8dc5ff;
}

.event-status-chip--success {
  background: rgba(103, 194, 58, 0.16);
  color: #8ce56d;
}

.event-status-chip--danger {
  background: rgba(245, 108, 108, 0.16);
  color: #ff9d9d;
}

.event-status-chip--info {
  background: rgba(144, 147, 153, 0.16);
  color: #d0d2d6;
}

/* 三色分级紧急程度标签 */
:deep(.urgency-chip) {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  font-size: 11px;
  white-space: nowrap;
  margin-left: 4px;
}

:deep(.urgency-chip--green) {
  background: rgba(103, 194, 58, 0.25);
  color: #8ce56d;
  border: 1px solid rgba(103, 194, 58, 0.4);
}

:deep(.urgency-chip--yellow) {
  background: rgba(230, 162, 60, 0.25);
  color: #f0c060;
  border: 1px solid rgba(230, 162, 60, 0.4);
}

:deep(.urgency-chip--red) {
  background: rgba(245, 108, 108, 0.3);
  color: #ff8080;
  border: 1px solid rgba(245, 108, 108, 0.5);
  animation: urgency-pulse 2s ease-in-out infinite;
}

@keyframes urgency-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.event-card__actions button,
.event-card__actions a {
  padding: 0 6px;
  min-width: 0;
  flex: 1;
  font-size: 12px;
}


.dispatch-dialog {
  display: grid;
  gap: 16px;
}

.dispatch-summary__code {
  margin: 0 0 8px;
  color: #8db0d0;
  font-size: 12px;
}

.dispatch-summary h4,
.dispatch-summary p,
.dispatch-template-copy {
  margin: 0;
}

.dispatch-summary h4 {
  color: #f0f7ff;
  font-size: 16px;
}

.dispatch-summary p,
.dispatch-template-copy {
  color: #9fb3c7;
  font-size: 13px;
}

@media (max-width: 1320px) {
  .event-filter-panel__actions {
    justify-items: start;
    grid-auto-flow: column;
    justify-content: start;
  }

  .event-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .event-filter-panel__actions {
    grid-column: auto;
    justify-items: start;
    grid-auto-flow: row;
  }
}

@media (max-width: 720px) {
  .event-filter-panel__form,
  .event-card-grid {
    grid-template-columns: 1fr;
  }

  .event-filter-panel__actions {
    grid-auto-flow: row;
  }

  .event-card__actions {
    flex-wrap: wrap;
  }

  .event-card__node-item,
  .dispatch-node-item {
    flex-direction: column;
    gap: 2px;
  }
}
</style>
