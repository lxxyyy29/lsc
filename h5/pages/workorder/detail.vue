<template>
  <view class="page">

    <!-- Loading / error states -->
    <text v-if="isLoading" class="state-text">加载中...</text>
    <text v-else-if="!detail" class="state-text">未找到对应工单。</text>

    <template v-else>
      <!-- ① 工单信息卡片 -->
      <view class="section-card">
        <view class="card-head">
          <text class="card-title">工单信息</text>
          <view class="status-chip" :class="statusChipClass(detail.status)">
            <text class="status-chip-text">{{ detail.statusText }}</text>
          </view>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="detail-label">工单编号</text>
            <text class="detail-value detail-value--mono">{{ detail.workOrderNo }}</text>
          </view>
          <view class="info-item">
            <text class="detail-label">当前处理人</text>
            <text class="detail-value">{{ detail.assigneeName }}</text>
          </view>
          <view class="info-item">
            <text class="detail-label">派发人</text>
            <text class="detail-value">{{ detail.dispatcherName || '--' }}</text>
          </view>
          <view class="info-item">
            <text class="detail-label">创建时间</text>
            <text class="detail-value">{{ detail.createdAt || '--' }}</text>
          </view>
          <view v-if="detail.completedAt" class="info-item">
            <text class="detail-label">完成时间</text>
            <text class="detail-value">{{ detail.completedAt }}</text>
          </view>
          <view v-if="detail.closedAt" class="info-item">
            <text class="detail-label">关闭时间</text>
            <text class="detail-value">{{ detail.closedAt }}</text>
          </view>
        </view>
      </view>

      <!-- ② 告警现场（照片+地图） -->
      <view v-if="sourceEvent" class="section-card">
        <text class="card-title">告警现场</text>

        <!-- 现场照片 -->
        <view v-if="mediaReferences.length" class="evidence-section">
          <text class="evidence-subtitle">现场照片</text>
          <scroll-view class="evidence-scroll" scroll-x>
            <view class="evidence-list">
              <view
                v-for="(url, index) in mediaReferences"
                :key="index"
                class="evidence-thumb"
                @click="previewImages(index)"
              >
                <image class="evidence-img" :src="toImageUrl(url)" mode="aspectFill" />
              </view>
            </view>
          </scroll-view>
          <text class="evidence-hint">点击查看大图</text>
        </view>

        <!-- 事发位置地图 -->
        <view v-if="hasMapCoords" class="map-section">
          <text class="evidence-subtitle">事发位置</text>
          <!-- #ifdef MP-WEIXIN -->
          <map
            class="detail-map"
            :latitude="mapCenter.lat"
            :longitude="mapCenter.lng"
            :markers="mapMarkers"
            :scale="16"
          />
          <!-- #endif -->
          <!-- #ifndef MP-WEIXIN -->
          <div class="detail-map" :id="mapElId"></div>
          <!-- #endif -->
          <text class="map-coord-text">经度: {{ sourceEvent.longitude }}  纬度: {{ sourceEvent.latitude }}</text>
          <view class="nav-btn-wrap">
            <button class="nav-btn" @click="navigateToScene">
              <text class="nav-btn-icon">🧭</text>
              <text class="nav-btn-text">导航到现场</text>
            </button>
          </view>
        </view>
      </view>

      <!-- ③ 非处理人提示横幅 -->
      <view v-if="!detail.isCurrentHandler && detail.status === 'PROCESSING'" class="observer-banner">
        <AppIcon name="info" class="observer-icon" size="24rpx" />
        <text class="observer-text">当前处理人：{{ detail.assigneeName }}，仅可查看详情，无法操作。</text>
      </view>

      <!-- ④ 流程节点时间线 -->
      <view v-if="detail.processNodes.length" class="section-card">
        <text class="card-title">流程节点</text>
        <view class="node-timeline">
          <view
            v-for="(node, index) in detail.processNodes"
            :key="node.nodeOrder"
            class="node-step"
          >
            <view class="node-left">
              <view class="node-circle" :class="nodeCircleClass(node.status)">
                <text class="node-order-text">{{ node.nodeOrder }}</text>
              </view>
              <view
                v-if="index < detail.processNodes.length - 1"
                class="node-line"
                :class="node.status === 'APPROVED' ? 'node-line--done' : 'node-line--pending'"
              ></view>
            </view>
            <view class="node-content">
              <view class="node-head">
                <text class="node-name">{{ node.nodeName }}</text>
                <text class="node-tag" :class="nodeTagClass(node.status)">{{ nodeStatusText(node.status) }}</text>
              </view>
              <text class="node-assignee">{{ node.assigneeName }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ⑤ 处理记录时间线 -->
      <view v-if="detail.actionRecords.length" class="section-card">
        <text class="card-title">处理记录</text>
        <view class="record-timeline">
          <view
            v-for="(record, index) in sortedRecords"
            :key="index"
            class="record-item"
          >
            <view class="record-left">
              <view class="record-dot" :class="recordDotClass(record.result)"></view>
              <view v-if="index < sortedRecords.length - 1" class="record-line"></view>
            </view>
            <view class="record-body">
              <view class="record-head">
                <text class="record-operator">{{ record.operatorName }}</text>
                <text class="record-action-tag" :class="recordTagClass(record.result)">{{ actionLabel(record.action) }}</text>
              </view>
              <text v-if="record.remark" class="record-remark">{{ record.remark }}</text>
              <!-- Subject association -->
              <view v-if="record.subjectType && record.subjectName" class="record-subject">
                <text class="record-subject-text">{{ record.subjectType === 'MERCHANT' ? '关联商户' : '关联摊贩' }}：{{ record.subjectName }}</text>
              </view>
              <view v-if="parseAttachments(record.attachments).length" class="record-attachments">
                <image
                  v-for="(att, ai) in parseAttachments(record.attachments).filter(a => a.fileType === 'IMAGE')"
                  :key="ai"
                  class="record-attach-thumb"
                  :src="toImageUrl(att.fileUrl)"
                  mode="aspectFill"
                  @click="previewRecordImage(record.attachments, ai)"
                />
                <view
                  v-for="(att, ai) in parseAttachments(record.attachments).filter(a => a.fileType !== 'IMAGE')"
                  :key="'f' + ai"
                  class="record-attach-file"
                >
                  <AppIcon name="upload" size="18rpx" class="attachment-icon" />
                  <text class="attachment-text">{{ att.fileName }}</text>
                </view>
              </view>
              <text class="record-time">{{ record.operatedAt }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- ⑥ 操作表单（当前处理人 + 有权限时展示） -->
      <view v-if="detail.isCurrentHandler && canHandle" class="section-card action-form">
        <text class="card-title">提交处理</text>

        <!-- 操作选择 -->
        <view class="op-row">
          <view
            v-for="op in operationOptions"
            :key="op.value"
            class="op-btn"
            :class="selectedOp === op.value ? `op-btn--active-${op.value.toLowerCase()}` : ''"
            @click="selectedOp = op.value"
          >
            <text class="op-btn-text">{{ op.label }}</text>
          </view>
        </view>

        <!-- 意见输入 -->
        <view class="form-group">
          <view class="form-label-row">
            <text class="form-label">处理意见</text>
            <text v-if="remarkRequired" class="form-required">* 必填</text>
          </view>
          <textarea
            v-model="remark"
            class="form-textarea"
            placeholder="请输入处理意见..."
            placeholder-class="form-placeholder"
            :maxlength="500"
          />
          <text class="char-count">{{ remark.length }}/500</text>
        </view>

        <!-- 关联商户/摊贩 -->
        <view class="form-group">
          <text class="form-label">关联商户/摊贩（可选）</text>
          <SubjectSelector
            v-model:subjectType="subjectType"
            v-model:subjectId="subjectId"
            v-model:subjectName="subjectName"
          />
        </view>

        <!-- 附件上传 -->
        <view class="form-group">
          <text class="form-label">上传附件（可选）</text>
          <view class="evidence-grid">
            <view class="evidence-card" @click="chooseAndUpload('image')">
              <view class="evidence-icon evidence-icon--photo"><AppIcon name="photo" size="34rpx" /></view>
              <text class="evidence-label">拍照上传</text>
            </view>
            <view class="evidence-card" @click="chooseAndUpload('video')">
              <view class="evidence-icon evidence-icon--video"><AppIcon name="video" size="34rpx" /></view>
              <text class="evidence-label">录制视频</text>
            </view>
          </view>
          <view v-if="uploadedAttachments.length" class="attachment-list">
            <view v-for="item in uploadedAttachments" :key="item.id" class="attachment-item">
              <view>
                <text class="attachment-name">{{ item.fileName }}</text>
                <text class="attachment-type">{{ item.fileType === 'VIDEO' ? '视频凭证' : '图片凭证' }}</text>
              </view>
              <text class="attachment-remove" @click.stop="removeAttachment(item.id)">移除</text>
            </view>
          </view>
        </view>

        <!-- 提交反馈 -->
        <text v-if="submitSuccess" class="submit-success">✓ 提交成功，正在返回...</text>

        <!-- 提交按钮 -->
        <button
          class="submit-btn"
          :class="isSubmitting ? 'submit-btn--disabled' : ''"
          :disabled="isSubmitting"
          @click="submitHandle"
        >
          {{ isSubmitting ? '提交中...' : '确认提交' }}
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import SubjectSelector from '../../src/components/SubjectSelector.vue'
import { onLoad } from '@dcloudio/uni-app'
import { hasButtonPermission } from '../../src/auth/permissions'
import {
  getWorkOrderDetail,
  handleWorkOrder,
  type WorkOrderItem,
  type ActionRecordVo
} from '../../src/api/workorder'
import { uploadFile, toImageUrl, fetchAccessPrefix } from '../../src/api/upload'
import { getEventDetail, type EventDetail } from '../../src/api/event'
import { ensureAuthenticated } from '../../src/uni/navigation'
import { openNavigation } from '../../src/utils/map-navigation'

// ─── State ───────────────────────────────────────────────────────────────────
const detail = ref<WorkOrderItem | null>(null)
const isLoading = ref(true)
const loadError = ref(false)
const currentId = ref('')
const sourceEvent = ref<EventDetail>()

const mapElId = 'wo-detail-map-' + Math.random().toString(36).slice(2, 8)
let detailMap: any = null

const mediaReferences = computed(() => sourceEvent.value?.evidenceReferences || [])
const mapCenter = computed(() => ({
  lat: sourceEvent.value?.latitude ?? 0,
  lng: sourceEvent.value?.longitude ?? 0
}))

const mapMarkers = computed(() => {
  if (!sourceEvent.value) return []
  return [{
    id: 1,
    latitude: sourceEvent.value.latitude,
    longitude: sourceEvent.value.longitude,
    width: 28,
    height: 36
  }]
})

const hasMapCoords = computed(() => {
  if (!sourceEvent.value) return false
  const lng = sourceEvent.value.longitude
  const lat = sourceEvent.value.latitude
  return lng !== 0 && lat !== 0 && Number.isFinite(lng) && Number.isFinite(lat)
})

function previewImages(index: number) {
  const urls = mediaReferences.value.map((u) => toImageUrl(u))
  uni.previewImage({
    urls,
    current: urls[index] || urls[0]
  })
}

// Upload attachment type
interface UploadedAttachment {
  id: string
  fileName: string
  fileUrl: string
  fileType: 'IMAGE' | 'VIDEO'
}

// Action form state
const selectedOp = ref('APPROVED')
const remark = ref('')
const uploadedAttachments = ref<UploadedAttachment[]>([])
const uploadError = ref('')
const isUploading = ref(false)
const isSubmitting = ref(false)
const submitError = ref('')
const submitSuccess = ref(false)

// Subject selection state
const subjectType = ref<'MERCHANT' | 'VENDOR' | null>(null)
const subjectId = ref<number | null>(null)
const subjectName = ref<string | null>(null)

// ─── Computed ─────────────────────────────────────────────────────────────────
const canHandle = computed(
  () =>
    hasButtonPermission('button:h5:workorder:handle') ||
    hasButtonPermission('api:h5:workorder:handle')
)

const operationOptions = [
  { label: '通过', value: 'APPROVED' },
  { label: '驳回', value: 'REJECTED' }
]

const remarkRequired = computed(() => ['REJECTED', 'RETURNED'].includes(selectedOp.value))

const sortedRecords = computed<ActionRecordVo[]>(() => {
  if (!detail.value) return []
  return [...detail.value.actionRecords].sort(
    (a, b) => new Date(b.operatedAt).getTime() - new Date(a.operatedAt).getTime()
  )
})

// ─── Helpers ─────────────────────────────────────────────────────────────────
function navigateToScene() {
  if (!sourceEvent.value) return
  openNavigation(
    sourceEvent.value.longitude,
    sourceEvent.value.latitude,
    '告警现场'
  )
}

function statusChipClass(status: string) {
  if (status === 'COMPLETED' || status === 'CLOSED') return 'status-chip--success'
  if (status === 'TIMEOUT') return 'status-chip--danger'
  return 'status-chip--primary'
}

function nodeCircleClass(status: string) {
  if (status === 'APPROVED') return 'node-circle--done'
  if (status === 'REJECTED' || status === 'RETURNED') return 'node-circle--rejected'
  if (status === 'WAITING') return 'node-circle--waiting'
  return 'node-circle--pending'
}

function nodeTagClass(status: string) {
  if (status === 'APPROVED') return 'tag--success'
  if (status === 'REJECTED' || status === 'RETURNED') return 'tag--danger'
  if (status === 'WAITING') return 'tag--muted'
  return 'tag--warning'
}

function nodeStatusText(status: string): string {
  switch (status) {
    case 'APPROVED': return '已通过'
    case 'REJECTED': return '已驳回'
    case 'RETURNED': return '已退回'
    case 'WAITING': return '等待中'
    case 'PENDING': return '待处理'
    default: return status
  }
}

function recordDotClass(result: string) {
  if (result === 'APPROVED') return 'record-dot--success'
  if (result === 'REJECTED' || result === 'RETURNED') return 'record-dot--danger'
  return 'record-dot--primary'
}

function recordTagClass(result: string) {
  if (result === 'APPROVED') return 'tag--success'
  if (result === 'REJECTED' || result === 'RETURNED') return 'tag--danger'
  return 'tag--primary'
}

function actionLabel(action: string): string {
  switch (action) {
    case 'APPROVED': return '通过'
    case 'REJECTED': return '驳回'
    case 'RETURNED': return '退回'
    case 'ACCEPT': return '接单'
    case 'HANDLE': return '处理'
    case 'DISPATCH': return '派发'
    case 'WORK_ORDER_START': return '流程启动'
    case 'WORK_ORDER_DISPATCH': return '工单派发'
    case 'WORK_ORDER_HANDLE': return '节点处理'
    case 'WORK_ORDER_COMPLETE': return '工单完成'
    default: return action
  }
}

function eventTypeLabel(eventType: string | null | undefined): string {
  if (!eventType) return ''
  const map: Record<string, string> = {
    DRONE_ALARM: '无人机告警',
    MANUAL_REPORT: '人工上报',
    VIDEO_ALARM: '视频告警',
    SENSOR_ALARM: '传感器告警',
    PATROL_ISSUE: '巡查问题'
  }
  return map[eventType] || eventType
}

// ─── Map ────────────────────────────────────────────────────────────────────
async function initDetailMap() {
  // #ifdef MP-WEIXIN
  return
  // #endif
  if (!sourceEvent.value || !hasMapCoords.value) return
  const container = document.getElementById(mapElId)
  if (!container) return

  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  const AMapLoader = (await import('@amap/amap-jsapi-loader')).default
  const AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: []
  })

  const lng = sourceEvent.value.longitude
  const lat = sourceEvent.value.latitude

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  detailMap = new AMapLib.Map(container, {
    zoom: 16,
    center: [lng, lat],
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer],
    resizeEnable: false
  })

  new AMapLib.Marker({
    position: new AMapLib.LngLat(lng, lat),
    anchor: 'bottom-center',
    map: detailMap
  })
}

// ─── Upload ──────────────────────────────────────────────────────────────────
async function chooseAndUpload(type: 'image' | 'video') {
  uploadError.value = ''
  if (type === 'image') {
    uni.chooseImage?.({
      count: Math.min(6 - uploadedAttachments.value.length, 3),
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const filePaths = (res as UniApp.ChooseImageSuccessCallbackResult).tempFilePaths || []
        isUploading.value = true
        for (const filePath of filePaths) {
          try {
            const uploaded = await uploadFile(filePath, 'workorder', 'image')
            uploadedAttachments.value = [
              ...uploadedAttachments.value,
              {
                id: `${uploaded.name}-${uploaded.objectName}`,
                fileName: uploaded.name,
                fileUrl: toImageUrl(uploaded.objectName),
                fileType: 'IMAGE'
              }
            ]
          } catch {
            uploadError.value = '图片上传失败，请重试'
          }
        }
        isUploading.value = false
      }
    })
    return
  }

  uni.chooseVideo?.({
    sourceType: ['album', 'camera'],
    success: async (res) => {
      const filePath = (res as UniApp.ChooseVideoSuccessCallbackResult).tempFilePath
      if (!filePath) return
      isUploading.value = true
      try {
        const uploaded = await uploadFile(filePath, 'workorder', 'video')
        uploadedAttachments.value = [
          ...uploadedAttachments.value,
          {
            id: `${uploaded.name}-${uploaded.objectName}`,
            fileName: uploaded.name,
            fileUrl: toImageUrl(uploaded.objectName),
            fileType: 'VIDEO'
          }
        ]
      } catch {
        uploadError.value = '视频上传失败，请重试'
      } finally {
        isUploading.value = false
      }
    }
  })
}

function removeAttachment(id: string) {
  uploadedAttachments.value = uploadedAttachments.value.filter((item) => item.id !== id)
  if (uploadError.value) uploadError.value = ''
}

// ─── Submit ──────────────────────────────────────────────────────────────────
async function submitHandle() {
  if (!detail.value || !currentId.value) return
  submitError.value = ''

  if (remarkRequired.value && !remark.value.trim()) {
    submitError.value = '驳回或退回时必须填写处理意见'
    return
  }

  try {
    isSubmitting.value = true
    await handleWorkOrder(currentId.value, {
      result: selectedOp.value,
      remark: remark.value.trim(),
      attachments: uploadedAttachments.value.map((f) => ({
        fileName: f.fileName,
        fileUrl: f.fileUrl,
        fileType: f.fileType
      })),
      subjectType: subjectType.value,
      subjectId: subjectId.value
    })
    submitSuccess.value = true
    setTimeout(() => {
      uni.navigateBack()
    }, 1200)
  } catch (err) {
    const msg = err instanceof Error ? err.message : ''
    submitError.value = msg && /[\u4e00-\u9fa5]/.test(msg) ? msg : '提交失败，请重试'
  } finally {
    isSubmitting.value = false
  }
}

interface ParsedAttachment {
  fileName: string
  fileUrl: string
  fileType: string
}

type AttachmentsRaw = ParsedAttachment[] | string | null

function parseAttachments(attachments: AttachmentsRaw): ParsedAttachment[] {
  if (!attachments) return []
  // 后端已返回真实数组（新格式）
  if (Array.isArray(attachments)) return attachments
  // 兼容旧格式：JSON 字符串
  try {
    const list = JSON.parse(attachments) as ParsedAttachment[]
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

function previewRecordImage(attachments: AttachmentsRaw, index: number) {
  const images = parseAttachments(attachments)
    .filter((a) => a.fileType === 'IMAGE')
    .map((a) => toImageUrl(a.fileUrl))
  if (images.length === 0) return
  uni.previewImage({ urls: images, current: images[index] || images[0] })
}

// ─── Load ────────────────────────────────────────────────────────────────────
onLoad(async (query?: Record<string, unknown>) => {
  const id = typeof query?.id === 'string' ? query.id : ''
  const redirectPath = id ? `/work-orders/${id}` : '/work-orders'
  if (!ensureAuthenticated(redirectPath)) return

  if (!id) {
    isLoading.value = false
    return
  }

  currentId.value = id
  // 确保 OSS 前缀就绪，避免图片 URL 构建时前缀还未缓存
  await fetchAccessPrefix().catch(() => {})
  try {
    loadError.value = false
    detail.value = await getWorkOrderDetail(id)

    if (detail.value?.sourceEventId) {
      sourceEvent.value = await getEventDetail(detail.value.sourceEventId)
      void nextTick(() => {
        setTimeout(() => initDetailMap(), 200)
      })
    }
  } catch {
    loadError.value = true
    detail.value = null
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 16rpx 100rpx;
  background:
    radial-gradient(circle at top left, rgba(20, 60, 110, 0.28) 0, rgba(20, 60, 110, 0) 40%),
    #060f18;
  color: #eef6ff;
}

/* State text */
.state-text {
  display: block;
  padding-top: 160rpx;
  text-align: center;
  color: rgba(214, 225, 239, 0.65);
  font-size: 30rpx;
}

/* Cards */
.section-card {
  margin: 16rpx 16rpx 0;
  padding: 22rpx 20rpx;
  border-radius: 16rpx;
  background: rgba(13, 30, 50, 0.95);
  border: 1px solid rgba(125, 163, 220, 0.12);
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.2);
  display: grid;
  gap: 16rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f3f8ff;
}

/* Type badge */
.type-badge {
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(94, 162, 255, 0.14);
  border: 1px solid rgba(94, 162, 255, 0.24);
}

.type-badge-text {
  font-size: 22rpx;
  color: #a8d0ff;
}

/* Status chip */
.status-chip {
  padding: 4rpx 14rpx;
  border-radius: 8rpx;
}

.status-chip-text {
  font-size: 24rpx;
  font-weight: 700;
}

.status-chip--primary {
  background: rgba(56, 152, 253, 0.14);
  border: 1px solid rgba(56, 152, 253, 0.28);
}
.status-chip--primary .status-chip-text { color: #7ab8ff; }

.status-chip--success {
  background: rgba(31, 190, 166, 0.12);
  border: 1px solid rgba(31, 190, 166, 0.24);
}
.status-chip--success .status-chip-text { color: #7eded0; }

.status-chip--danger {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.24);
}
.status-chip--danger .status-chip-text { color: #fca5a5; }

/* Event info */
.event-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #f3f8ff;
  line-height: 1.5;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.location-icon {
  color: #5ea2ff;
  flex-shrink: 0;
}

.location-text {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.85);
}

.event-desc {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.75);
  line-height: 1.6;
}

/* Info rows */
.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 0;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
}

.detail-label {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.55);
}

.detail-value {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.9);
}

.detail-value--mono {
  font-family: monospace;
  color: #98cbff;
}

/* Info grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx 20rpx;
}

.info-item {
  display: grid;
  gap: 6rpx;
}

/* Observer banner */
.observer-banner {
  margin: 16rpx 16rpx 0;
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  padding: 18rpx 16rpx;
  border-radius: 12rpx;
  background: rgba(56, 152, 253, 0.08);
  border: 1px solid rgba(56, 152, 253, 0.2);
}

.observer-icon {
  color: #5ea2ff;
  flex-shrink: 0;
  margin-top: 2rpx;
}

.observer-text {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.8);
  line-height: 1.5;
}

/* Process nodes timeline */
.node-timeline {
  display: grid;
  gap: 0;
}

.node-step {
  display: flex;
  gap: 16rpx;
  align-items: stretch;
}

.node-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 40rpx;
}

.node-circle {
  width: 40rpx;
  height: 40rpx;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(22, 38, 54, 0.9);
}

.node-order-text {
  font-size: 22rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.node-circle--done {
  background: linear-gradient(135deg, #4da4ff 0%, #2982ec 100%);
  border-color: transparent;
  box-shadow: 0 0 12rpx rgba(56, 152, 253, 0.5);
}

.node-circle--done .node-order-text { color: #fff; }

.node-circle--rejected {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.5);
}

.node-circle--rejected .node-order-text { color: #fca5a5; }

.node-circle--waiting {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.2);
}

.node-circle--waiting .node-order-text { color: rgba(214, 225, 239, 0.4); }

.node-circle--pending {
  background: rgba(234, 179, 8, 0.12);
  border-color: rgba(234, 179, 8, 0.35);
}

.node-circle--pending .node-order-text { color: #fcd34d; }

.node-line {
  flex: 1;
  width: 2rpx;
  min-height: 24rpx;
  margin: 4rpx 0;
}

.node-line--done {
  background: rgba(77, 164, 255, 0.5);
}

.node-line--pending {
  background: rgba(255, 255, 255, 0.08);
}

.node-content {
  flex: 1;
  padding-bottom: 20rpx;
  display: grid;
  gap: 6rpx;
}

.node-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.node-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #f3f8ff;
}

.node-assignee {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.65);
}

/* Tags */
.node-tag,
.record-action-tag {
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.tag--success {
  background: rgba(31, 190, 166, 0.12);
  color: #7eded0;
  border: 1px solid rgba(31, 190, 166, 0.24);
}

.tag--danger {
  background: rgba(239, 68, 68, 0.12);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.24);
}

.tag--warning {
  background: rgba(234, 179, 8, 0.12);
  color: #fcd34d;
  border: 1px solid rgba(234, 179, 8, 0.24);
}

.tag--muted {
  background: rgba(148, 163, 184, 0.08);
  color: rgba(214, 225, 239, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

.tag--primary {
  background: rgba(56, 152, 253, 0.12);
  color: #7ab8ff;
  border: 1px solid rgba(56, 152, 253, 0.24);
}

/* Action records */
.record-timeline {
  display: grid;
  gap: 0;
}

.record-item {
  display: flex;
  gap: 14rpx;
  align-items: stretch;
}

.record-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 14rpx;
}

.record-dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}

.record-dot--primary {
  background: #3898fd;
  box-shadow: 0 0 8rpx rgba(56, 152, 253, 0.6);
}

.record-dot--success {
  background: #1fbea6;
  box-shadow: 0 0 8rpx rgba(31, 190, 166, 0.6);
}

.record-dot--danger {
  background: #ef4444;
  box-shadow: 0 0 8rpx rgba(239, 68, 68, 0.5);
}

.record-line {
  flex: 1;
  width: 2rpx;
  min-height: 20rpx;
  margin: 4rpx 0;
  background: rgba(255, 255, 255, 0.06);
}

.record-body {
  flex: 1;
  padding-bottom: 24rpx;
  display: grid;
  gap: 8rpx;
}

.record-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10rpx;
}

.record-operator {
  font-size: 26rpx;
  font-weight: 600;
  color: #f3f8ff;
}

.record-remark {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.8);
  line-height: 1.5;
  padding: 10rpx 14rpx;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 8rpx;
  border-left: 3rpx solid rgba(56, 152, 253, 0.4);
}

.record-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 8rpx;
}

.record-attach-thumb {
  width: 120rpx;
  height: 120rpx;
  border-radius: 8rpx;
  border: 1px solid rgba(94, 162, 255, 0.25);
}

.record-attach-file {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.attachment-icon {
  color: #5ea2ff;
}

.attachment-text {
  font-size: 24rpx;
  color: #5ea2ff;
}

.record-time {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.45);
}

/* Action form */
.action-form {
  margin-bottom: 30rpx;
}

.op-row {
  display: flex;
  gap: 14rpx;
}

.op-btn {
  flex: 1;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.op-btn--active-approved {
  background: rgba(56, 152, 253, 0.18);
  border-color: rgba(56, 152, 253, 0.45);
}

.op-btn--active-rejected {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.4);
}

.op-btn--active-returned {
  background: rgba(234, 179, 8, 0.14);
  border-color: rgba(234, 179, 8, 0.4);
}

.op-btn-text {
  font-size: 28rpx;
  font-weight: 600;
  color: rgba(214, 225, 239, 0.8);
}

.op-btn--active-approved .op-btn-text { color: #7ab8ff; }
.op-btn--active-rejected .op-btn-text { color: #fca5a5; }
.op-btn--active-returned .op-btn-text { color: #fcd34d; }

.form-group {
  display: grid;
  gap: 10rpx;
}

.form-label-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.form-label {
  font-size: 26rpx;
  color: rgba(214, 225, 239, 0.8);
  font-weight: 500;
}

.form-required {
  font-size: 22rpx;
  color: #f87171;
}

.form-textarea {
  width: 100%;
  min-height: 160rpx;
  padding: 16rpx;
  border-radius: 12rpx;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(125, 163, 220, 0.15);
  color: #eef6ff;
  font-size: 28rpx;
  line-height: 1.6;
  box-sizing: border-box;
}

.form-placeholder {
  color: rgba(214, 225, 239, 0.35);
}

.char-count {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.4);
  text-align: right;
}

/* Evidence upload grid */
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.evidence-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
  padding: 32rpx 16rpx 26rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, rgba(13, 30, 50, 0.98) 0%, rgba(10, 24, 40, 0.98) 100%);
  border: 1px solid rgba(125, 163, 220, 0.1);
  box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.18);
}

.evidence-icon {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20rpx;
  color: #eef6ff;
  box-shadow: inset 0 0 0 1px rgba(143, 217, 255, 0.08);
}

.evidence-icon--photo {
  background: linear-gradient(180deg, #21384f 0%, #142638 100%);
}

.evidence-icon--video {
  background: linear-gradient(180deg, #183246 0%, #102130 100%);
}

.evidence-label {
  font-size: 26rpx;
  color: #f3f8ff;
}

/* Attachment list */
.attachment-list {
  display: grid;
  gap: 12rpx;
  margin-top: 16rpx;
}

.attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: rgba(10, 24, 40, 0.98);
  border: 1px solid rgba(125, 163, 220, 0.1);
}

.attachment-name {
  display: block;
  font-size: 26rpx;
  color: #f3f8ff;
}

.attachment-type {
  display: block;
  margin-top: 6rpx;
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.55);
}

.attachment-remove {
  font-size: 26rpx;
  color: #5ea2ff;
  flex-shrink: 0;
}

.upload-error {
  font-size: 24rpx;
  color: #f87171;
}

/* Submit feedback */
.submit-success {
  font-size: 26rpx;
  color: #1fbea6;
  text-align: center;
}

.submit-error {
  font-size: 24rpx;
  color: #f87171;
  text-align: center;
}

/* Submit button */
.submit-btn {
  height: 88rpx;
  line-height: 88rpx;
  border: none;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #4da4ff 0%, #2982ec 100%);
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 700;
  box-shadow: 0 10rpx 24rpx rgba(41, 130, 236, 0.4);
}

.submit-btn--disabled {
  opacity: 0.55;
  box-shadow: none;
}

/* Record subject association */
.record-subject {
  display: flex;
  align-items: center;
  gap: 4rpx;
  padding: 6rpx 12rpx;
  border-radius: 8rpx;
  background: rgba(94, 162, 255, 0.08);
  border: 1px solid rgba(94, 162, 255, 0.18);
}

.record-subject-text {
  font-size: 24rpx;
  color: #a8d0ff;
}

/* ── 告警现场 section ─────────────────────────────────── */
.evidence-section {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 16rpx;
}

.evidence-subtitle {
  font-size: 26rpx;
  font-weight: 500;
  color: rgba(214, 225, 239, 0.7);
}

.evidence-scroll {
  width: 100%;
  white-space: nowrap;
}

.evidence-list {
  display: flex;
  gap: 16rpx;
}

.evidence-thumb {
  flex-shrink: 0;
  width: 220rpx;
  height: 160rpx;
  border-radius: 12rpx;
  overflow: hidden;
  border: 1px solid rgba(125, 163, 220, 0.15);
  background: rgba(7, 27, 46, 0.5);
}

.evidence-img {
  width: 220rpx;
  height: 160rpx;
}

.evidence-hint {
  font-size: 22rpx;
  color: rgba(214, 225, 239, 0.4);
}

.map-section {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  margin-top: 20rpx;
}

.detail-map {
  width: 100%;
  height: 400rpx;
  border-radius: 12rpx;
  border: 1px solid rgba(125, 163, 220, 0.15);
}

.map-coord-text {
  font-size: 24rpx;
  color: rgba(214, 225, 239, 0.55);
}

.nav-btn-wrap {
  margin-top: 16rpx;
}

.nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  width: 100%;
  height: 80rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #3898fd, #2272d9);
  border: none;
  padding: 0;
}

.nav-btn-icon {
  font-size: 32rpx;
}

.nav-btn-text {
  font-size: 30rpx;
  font-weight: 600;
  color: #fff;
}
</style>