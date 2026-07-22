<template>
  <PageContainer title="工单详情">
    <p v-if="loading" class="workorder-detail-state">加载中...</p>
    <NotFoundState
      v-else-if="!detail"
      title="未找到工单"
      description="当前工单不存在或已被移除，请返回工单列表重新选择。"
    />

    <section v-else class="workorder-detail-page panel" data-testid="web-detail-page-template">
      <header class="detail-header">
        <div class="detail-header__kicker">
          <span class="detail-header__icon">📋</span>
          <span>工单中心 / 查看</span>
        </div>
        <h3 class="detail-header__title">工单信息</h3>
      </header>

      <section class="detail-info-grid">
        <div class="info-item">
          <span class="info-label">工单编号：</span>
          <span class="info-value">{{ detail.code }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">状态：</span>
          <strong class="info-value info-value--highlight"><StatusTag :status="detail.state" /></strong>
        </div>
        <div class="info-item">
          <span class="info-label">来源事件：</span>
          <span class="info-value">{{ detail.eventTitle }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">事件编号：</span>
          <span class="info-value">{{ detail.eventCode }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">处理人：</span>
          <span class="info-value">{{ detail.assignee }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">派单人：</span>
          <span class="info-value">{{ detail.dispatcherName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">事件状态：</span>
          <span class="info-value">{{ getEventStatusLabel(detail.sourceEventState) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间：</span>
          <span class="info-value">{{ detail.createdAt || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">更新时间：</span>
          <span class="info-value">{{ detail.updatedAt || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">完成时间：</span>
          <span class="info-value">{{ detail.completedAt || '--' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">关闭时间：</span>
          <span class="info-value">{{ detail.closedAt || '--' }}</span>
        </div>
        <div class="info-item info-item--span-2">
          <span class="info-label">事件摘要：</span>
          <span class="info-value">{{ detail.sourceEventSummary || '--' }}</span>
        </div>
        <div class="info-item info-item--span-2">
          <span class="info-label">关闭原因：</span>
          <span class="info-value">{{ detail.closeReason || '--' }}</span>
        </div>
      </section>

      <section v-if="sourceEvent" class="detail-subsection">
        <h4>告警信息</h4>
        <div class="detail-info-grid">
          <div class="info-item">
            <span class="info-label">告警名称：</span>
            <span class="info-value">{{ sourceEvent.title || '--' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">告警类型：</span>
            <span class="info-value">{{ getEventTypeLabel(sourceEvent.eventType) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">发现时间：</span>
            <span class="info-value">{{ sourceEvent.occurredAt || '--' }}</span>
          </div>
          <div class="info-item info-item--span-2">
            <span class="info-label">告警地点：</span>
            <span class="info-value">{{ resolvedLocation || sourceEvent.location || '--' }}</span>
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
            :longitude="sourceEvent.longitude"
            :latitude="sourceEvent.latitude"
            :label="coordinateText"
            :areas="areaOverlays"
          />
        </div>
      </section>

      <section class="detail-subsection">
        <h4>流转记录</h4>
        <RecordTimeline v-if="detail.flowRecords.length" :records="detail.flowRecords" />
        <p v-else class="workorder-detail-state">暂无流转记录。</p>
      </section>

      <section v-if="detail.state === 'PROCESSING' && canHandle" class="detail-subsection">
        <h4>处置动作</h4>
        <label class="field-stack">
          <span>处置结果</span>
          <select v-model="result" aria-label="处置结果">
            <option value="APPROVED">通过</option>
            <option value="REJECTED">驳回</option>
          </select>
        </label>
        <label class="field-stack">
          <span>备注</span>
          <textarea v-model="remark" aria-label="处置备注" rows="4" placeholder="请输入处置备注"></textarea>
        </label>

        <div class="field-stack">
          <span>关联商户/摊贩（可选）</span>
          <SubjectSelector
            v-model:subjectType="subjectType"
            v-model:subjectId="subjectId"
            v-model:subjectName="subjectName"
          />
        </div>

        <section class="upload-card">
          <div class="upload-card__header">
            <h5>上传凭证</h5>
            <span>上传后自动回填附件元数据并随处置结果提交</span>
          </div>

          <div class="upload-actions">
            <input
              ref="fileInputRef"
              class="upload-input"
              type="file"
              multiple
              accept="image/*,video/*"
              @change="handleFileChange"
            />
            <button type="button" class="upload-trigger" @click="openFilePicker">+ 选择图片或视频</button>
            <button v-if="attachments.length" type="button" class="upload-clear" @click="clearAttachments">清空</button>
          </div>

          <div v-if="attachments.length" class="upload-list">
            <article v-for="item in attachments" :key="item.id" class="upload-item">
              <div>
                <strong>{{ item.name }}</strong>
                <p>{{ item.typeLabel }} · {{ item.sizeLabel }}{{ item.uploading ? ' · 上传中...' : '' }}</p>
              </div>
              <button type="button" class="upload-remove" @click="removeAttachment(item.id)">移除</button>
            </article>
          </div>
          <p v-else class="upload-empty">暂未选择文件，可上传图片或视频作为处置凭证。</p>

          <div class="upload-tags">
            <button type="button" class="tag-btn" @click="appendHint('远景照全貌')">远景照全貌</button>
            <button type="button" class="tag-btn" @click="appendHint('近景门头')">近景门头</button>
            <button type="button" class="tag-btn" @click="appendHint('整改前状态')">整改前状态</button>
          </div>
        </section>

        <p v-if="actionMessage" class="workorder-detail-state">{{ actionMessage }}</p>
        <div class="detail-footer detail-footer--inline">
          <button type="button" class="action-button" :disabled="submitting" @click="submitHandle">
            {{ submitting ? '提交中...' : '确认处置' }}
          </button>
        </div>
      </section>


      <footer class="detail-footer">
        <button type="button" class="action-button action-button--secondary" @click="$router.back()">返回</button>
      </footer>
    </section>

    <ImagePreviewOverlay
      v-model="previewVisible"
      :images="mediaReferences"
      :index="previewIndex"
      @update:index="previewIndex = $event"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { hasPermission } from '../../auth/permissions'
import { getWebSession } from '../../auth/session'
import { deleteUploadedFile, fetchAccessPrefix, uploadFile } from '../../api/upload'
import { getWorkOrderDetail, handleWorkOrder, type HandleWorkOrderPayload, type WorkOrderDetail } from '../../api/workorder'
import { getEventDetail, getEventStatusLabel, getEventTypeLabel, getSourceSystemLabel, type EventDetail } from '../../api/event'
import { listBizAreas, type BizArea } from '../../api/biz-area'
import NotFoundState from '../../components/admin/NotFoundState.vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import RecordTimeline from '../../components/admin/RecordTimeline.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import SubjectSelector from '../../components/admin/SubjectSelector.vue'
import AMapPointViewer from '../../components/biz/AMapPointViewer.vue'
import ImagePreviewOverlay from '../../components/ImagePreviewOverlay.vue'

interface PendingAttachment {
  id: string
  file?: File
  name: string
  typeLabel: string
  sizeLabel: string
  fileUrl?: string
  mimeType?: string
  fileType?: string
  uploading?: boolean
}

const route = useRoute()
const detail = ref<WorkOrderDetail>()
const loading = ref(false)
const submitting = ref(false)
const result = ref<HandleWorkOrderPayload['result']>('APPROVED')
const remark = ref('')
const actionMessage = ref('')
const attachments = ref<PendingAttachment[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const subjectType = ref<'MERCHANT' | 'VENDOR' | null>(null)
const subjectId = ref<number | null>(null)
const subjectName = ref<string | null>(null)
const currentUserName = computed(() => getWebSession()?.userName?.trim() || '')
const canHandle = computed(() => {
  if (!hasPermission('api:workorder:handle')) {
    return false
  }
  const assignee = detail.value?.assignee?.trim()
  const currentUser = currentUserName.value
  return Boolean(assignee && currentUser && assignee === currentUser)
})

const sourceEvent = ref<EventDetail>()
const bizAreas = ref<BizArea[]>([])
const previewVisible = ref(false)
const previewIndex = ref(0)
const resolvedLocation = ref('')

const mediaReferences = computed(() => sourceEvent.value?.evidenceReferences || [])
const coordinateText = computed(() => {
  if (!sourceEvent.value) return '--'
  const lng = sourceEvent.value.longitude
  const lat = sourceEvent.value.latitude
  if (lng && lat) return `${Number(lng).toFixed(6)}, ${Number(lat).toFixed(6)}`
  return sourceEvent.value.location || '--'
})
const areaOverlays = computed(() =>
  bizAreas.value
    .filter((a) => a.status === 'ACTIVE' && a.roiJson)
    .map((a) => ({ areaName: a.areaName, roiJson: a.roiJson! }))
)

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

function formatFileSize(size: number) {
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function buildAttachment(file: File): PendingAttachment {
  const isVideo = file.type.startsWith('video/')
  return {
    id: `${file.name}-${file.size}-${file.lastModified}-${Math.random().toString(36).slice(2, 8)}`,
    file,
    name: file.name,
    typeLabel: isVideo ? '视频凭证' : '图片凭证',
    sizeLabel: formatFileSize(file.size),
    mimeType: file.type || undefined,
    fileType: isVideo ? 'VIDEO' : 'IMAGE',
    uploading: true
  }
}

async function loadDetail(id: number) {
  if (!Number.isFinite(id)) {
    detail.value = undefined
    sourceEvent.value = undefined
    return
  }

  loading.value = true
  resolvedLocation.value = ''
  try {
    await fetchAccessPrefix()
    detail.value = await getWorkOrderDetail(id)

    if (detail.value?.sourceEventId) {
      const [eventData] = await Promise.all([
        getEventDetail(detail.value.sourceEventId),
        listBizAreas({ status: 'ACTIVE' }).then((areas) => { bizAreas.value = areas }).catch(() => { bizAreas.value = [] })
      ])
      sourceEvent.value = eventData
      if (eventData && !eventData.location && eventData.longitude && eventData.latitude) {
        reverseGeocode(eventData.longitude, eventData.latitude)
      }
    } else {
      sourceEvent.value = undefined
      bizAreas.value = []
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
    // geocoding is non-critical
  }
}

function openFilePicker() {
  fileInputRef.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const files = Array.from(input?.files || [])
  if (!files.length) {
    return
  }
  const stagedAttachments = files.map(buildAttachment)
  attachments.value = [...attachments.value, ...stagedAttachments]
  if (input) {
    input.value = ''
  }

  for (const attachment of stagedAttachments) {
    try {
      const uploaded = await uploadFile(attachment.file as File, 'workorder')
      attachments.value = attachments.value.map((item) =>
        item.id === attachment.id
          ? {
              ...item,
              name: uploaded.name,
              sizeLabel: formatFileSize(Number(uploaded.size || 0)),
              fileUrl: uploaded.objectName,
              uploading: false
            }
          : item
      )
    } catch (error) {
      attachments.value = attachments.value.filter((item) => item.id !== attachment.id)
      actionMessage.value = error instanceof Error ? error.message : '附件上传失败'
    }
  }
}

async function removeAttachment(id: string) {
  const target = attachments.value.find((item) => item.id === id)
  if (target?.fileUrl) {
    try {
      await deleteUploadedFile(target.fileUrl)
    } catch {
      // ignore delete failures; keep UI responsive
    }
  }
  attachments.value = attachments.value.filter((item) => item.id !== id)
}

function clearAttachments() {
  attachments.value = []
}

function appendHint(hint: string) {
  const current = remark.value.trim()
  remark.value = current ? `${current}；${hint}` : hint
}

async function submitHandle() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    return
  }

  submitting.value = true
  actionMessage.value = ''
  try {
    await handleWorkOrder(id, {
      result: result.value,
      remark: remark.value,
      attachments: attachments.value
        .filter((item) => item.fileUrl)
        .map((item) => ({
          fileName: item.name,
          fileUrl: item.fileUrl as string,
          fileType: item.fileType,
          mimeType: item.mimeType
        })),
      subjectType: subjectType.value,
      subjectId: subjectId.value
    })
    actionMessage.value = attachments.value.length ? '处置成功，附件已上传并提交。' : '处置成功'
    attachments.value = []
    subjectType.value = null
    subjectId.value = null
    subjectName.value = null
    await loadDetail(id)
  } catch (error) {
    actionMessage.value = error instanceof Error ? error.message : '处置失败'
  } finally {
    submitting.value = false
  }
}

watch(
  () => Number(route.params.id),
  (id) => {
    attachments.value = []
    actionMessage.value = ''
    subjectType.value = null
    subjectId.value = null
    subjectName.value = null
    void loadDetail(id)
  },
  { immediate: true }
)
</script>

<style scoped>
@import '../admin-shared.css';

.workorder-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: calc(100vh - 120px);
}

.workorder-detail-state {
  margin: 0;
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

.detail-header__title {
  margin: 0;
  color: #eaf5ff;
  font-size: 18px;
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px 24px;
  padding: 0 0 12px 0;
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

.info-value--highlight {
  font-weight: bold;
}

.detail-subsection {
  display: grid;
  gap: 12px;
}

.detail-subsection h4,
.detail-subsection h5,
.flow-item p,
.flow-item small,
.upload-empty,
.upload-item p {
  margin: 0;
}

.flow-list,
.upload-list {
  display: grid;
  gap: 12px;
}

.flow-item,
.upload-item,
.upload-card {
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 12px;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.flow-item__top,
.upload-card__header,
.upload-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.flow-item small,
.upload-card__header span,
.upload-item p,
.upload-empty {
  color: #8db0d0;
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
  min-height: 36px;
  width: 100%;
  box-sizing: border-box;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-radius: 4px;
  background: #1a344b;
  color: #eaf5ff;
  font: inherit;
  padding: 8px 12px;
}

.field-stack textarea {
  min-height: 96px;
  resize: vertical;
}

.upload-input {
  display: none;
}

.upload-actions,
.upload-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.upload-trigger,
.upload-clear,
.tag-btn,
.upload-remove {
  border-radius: 8px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  cursor: pointer;
}

.upload-trigger,
.upload-clear,
.upload-remove {
  padding: 8px 14px;
}

.tag-btn {
  padding: 6px 12px;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

.detail-footer--inline {
  padding-top: 0;
}

@media (max-width: 1024px) {
  .detail-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 720px) {
  .detail-info-grid {
    grid-template-columns: 1fr;
  }
  .info-item--span-2 {
    grid-column: span 1;
  }
  .flow-item__top,
  .upload-card__header,
  .upload-item {
    flex-direction: column;
    align-items: flex-start;
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

.detail-map-section > :deep(.amap-point-viewer) {
  flex: 1 1 0;
  min-width: 0;
  height: 480px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid rgba(103, 187, 246, 0.18);
}

.media-gallery-section {
  flex: 0 0 auto;
  width: 280px;
  min-width: 180px;
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
</style>
