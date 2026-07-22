<template>
  <view class="page">
    <view class="section-block section-block--tight">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="section-mark"></text>
          <text class="section-title">核查任务信息</text>
        </view>
      </view>
      <view class="task-card">
        <text class="task-label">任务编号</text>
        <view class="task-row">
          <text class="task-value">{{ currentTask?.workOrderNo || '--' }}</text>
          <text class="status-chip">{{ currentTask?.statusText || '待处理' }}</text>
        </view>
      </view>
    </view>

    <view class="section-block">
      <view class="section-head between">
        <view class="section-title-wrap">
          <text class="section-mark"></text>
          <text class="section-title">现场凭证概览</text>
        </view>
        <text class="section-note">继续补充现场凭证</text>
      </view>
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
      <view v-if="attachments.length" class="attachment-list">
        <view v-for="item in attachments" :key="item.id" class="attachment-item">
          <view>
            <text class="attachment-name">{{ item.fileName }}</text>
            <text class="attachment-type">{{ item.fileType === 'VIDEO' ? '视频凭证' : '图片凭证' }}</text>
          </view>
          <text class="attachment-remove" @click.stop="removeAttachment(item.id)">移除</text>
        </view>
      </view>
    </view>

    <view class="section-block">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="section-mark"></text>
          <text class="section-title">核查状态与结论</text>
        </view>
      </view>
      <view class="option-grid">
        <view
          v-for="option in verifyResultOptions"
          :key="option"
          class="option-btn"
          :class="{ 'option-btn--active': selectedResult === option }"
          @click="selectResult(option)"
        >
          <text class="option-text">{{ option }}</text>
        </view>
      </view>
      <view class="note-block">
        <view class="note-head">
          <text class="note-title">核查说明</text>
          <text class="note-count">{{ note.length }}/200</text>
        </view>
        <textarea
          v-model="note"
          class="note-textarea"
          maxlength="200"
          placeholder="请输入核查意见、补充要求或不通过原因"
          placeholder-class="note-placeholder"
        />
      </view>
    </view>

    <view class="bottom-bar">
      <button class="bottom-btn bottom-btn--ghost" @click="goBack">保存草稿</button>
      <button v-if="canSubmit" class="bottom-btn bottom-btn--primary" @click="submit">提交核查结果</button>
      <view v-else class="bottom-btn bottom-btn--disabled"><text>无提交权限</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import { onShow } from '@dcloudio/uni-app'
import { hasButtonPermission } from '../../src/auth/permissions'
import { uploadFile } from '../../src/api/upload'
import { getWorkOrders, handleWorkOrder, verifyResultOptions, type WorkOrderAttachmentPayload, type WorkOrderItem } from '../../src/api/workorder'
import { ensureAuthenticated } from '../../src/uni/navigation'

interface UploadedAttachment {
  id: string
  fileName: string
  fileUrl: string
  fileType: 'IMAGE' | 'VIDEO'
  mimeType?: string
}

const isLoading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const currentTask = ref<WorkOrderItem | null>(null)
const selectedResult = ref(verifyResultOptions[0])
const note = ref('')
const attachments = ref<UploadedAttachment[]>([])

const canSubmit = computed(() => hasButtonPermission('button:h5:workorder:verify'))

function selectResult(option: string) {
  selectedResult.value = option
}

async function chooseAndUpload(type: 'image' | 'video') {
  const chooser = type === 'image' ? uni.chooseImage : uni.chooseVideo
  if (type === 'image') {
    chooser?.({
      count: 6,
      success: async (result) => {
        const filePaths = (result as UniApp.ChooseImageSuccessCallbackResult).tempFilePaths || []
        for (const filePath of filePaths) {
          const uploaded = await uploadFile(filePath, 'workorder', 'image')
          attachments.value = [
            ...attachments.value,
            {
              id: `${uploaded.name}-${uploaded.url}`,
              fileName: uploaded.name,
              fileUrl: uploaded.url,
              fileType: 'IMAGE'
            }
          ]
        }
      }
    })
    return
  }

  chooser?.({
    success: async (result) => {
      const filePath = (result as UniApp.ChooseVideoSuccessCallbackResult).tempFilePath
      if (!filePath) {
        return
      }
      const uploaded = await uploadFile(filePath, 'workorder', 'video')
      attachments.value = [
        ...attachments.value,
        {
          id: `${uploaded.name}-${uploaded.url}`,
          fileName: uploaded.name,
          fileUrl: uploaded.url,
          fileType: 'VIDEO'
        }
      ]
    }
  })
}

function removeAttachment(id: string) {
  attachments.value = attachments.value.filter((item) => item.id !== id)
}

async function submit() {
  if (!canSubmit.value || !currentTask.value) return
  submitting.value = true
  try {
    const uploadedAttachments: WorkOrderAttachmentPayload[] = attachments.value.map((item) => ({
      fileName: item.fileName,
      fileUrl: item.fileUrl,
      fileType: item.fileType,
      mimeType: item.mimeType
    }))
    await handleWorkOrder(currentTask.value.id, {
      result: selectedResult.value,
      remark: note.value,
      attachments: uploadedAttachments
    })
    uni.showToast?.({ title: '已提交', icon: 'success' })
    attachments.value = []
    note.value = ''
  } catch (error) {
    const msg = error instanceof Error ? error.message : ''
    uni.showToast?.({ title: msg && /[\u4e00-\u9fa5]/.test(msg) ? msg : '提交失败，请重试', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

onShow(async () => {
  if (!ensureAuthenticated('/verify')) return
  isLoading.value = true
  loadError.value = false
  try {
    const orders = await getWorkOrders()
    currentTask.value = orders.find((item) => item.isCurrentHandler && item.status === 'PROCESSING') ?? null
  } catch {
    loadError.value = true
    currentTask.value = null
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20rpx 18rpx 244rpx;
  background:
    radial-gradient(circle at top, rgba(29, 69, 108, 0.34) 0, rgba(29, 69, 108, 0) 42%),
    #081421;
  color: #eef6ff;
}

.section-title-wrap,
.section-head,
.task-row,
.bottom-bar,
.evidence-card,
.evidence-icon,
.option-btn {
  display: flex;
  align-items: center;
}

.task-row,
.bottom-bar,
.between,
.section-head {
  justify-content: space-between;
}

.section-title,
.task-value,
.evidence-label,
.note-title,
.option-text,
.bottom-btn text {
  color: #f3f8ff;
}

.section-block {
  display: grid;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.section-block--tight {
  margin-bottom: 26rpx;
}

.section-mark {
  width: 6rpx;
  height: 24rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #d7f7ff 0%, #52d4ff 100%);
  box-shadow: 0 0 12rpx rgba(82, 212, 255, 0.35);
  margin-right: 12rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
}

.section-note,
.task-label,
.note-count,
.note-placeholder {
  color: rgba(214, 225, 239, 0.72);
  font-size: 24rpx;
}

.task-card,
.evidence-card,
.note-block {
  border-radius: 18rpx;
  background: linear-gradient(180deg, rgba(18, 32, 49, 0.98) 0%, rgba(14, 26, 40, 0.98) 100%);
  border: 1px solid rgba(118, 189, 255, 0.08);
  box-shadow: 0 18rpx 36rpx rgba(3, 11, 20, 0.28);
}

.task-card {
  position: relative;
  padding: 24rpx 20rpx 22rpx;
  overflow: hidden;
}

.task-card::after {
  content: '';
  position: absolute;
  top: -22rpx;
  right: -18rpx;
  width: 96rpx;
  height: 96rpx;
  background: url('../../src/assets/login-hex.svg') center/contain no-repeat;
  opacity: 0.18;
}

.task-label {
  display: block;
  margin-bottom: 10rpx;
}

.task-value {
  font-size: 38rpx;
  font-weight: 700;
}

.status-chip {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(82, 161, 255, 0.16);
  color: #d9efff;
  font-size: 24rpx;
  border: 1px solid rgba(143, 217, 255, 0.14);
}

.evidence-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
}

.evidence-card {
  flex-direction: column;
  justify-content: center;
  gap: 16rpx;
  padding: 32rpx 16rpx 26rpx;
}

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
  background: rgba(14, 26, 40, 0.98);
  border: 1px solid rgba(118, 189, 255, 0.08);
}

.attachment-name,
.attachment-type,
.attachment-remove {
  display: block;
}

.attachment-name {
  color: #f3f8ff;
  font-size: 26rpx;
}

.attachment-type {
  margin-top: 6rpx;
  color: rgba(214, 225, 239, 0.72);
  font-size: 24rpx;
}

.attachment-remove {
  color: #8fd9ff;
  font-size: 26rpx;
}

.evidence-icon {
  width: 72rpx;
  height: 72rpx;
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
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14rpx;
}

.option-btn {
  height: 76rpx;
  justify-content: center;
  border-radius: 12rpx;
  background: #091521;
  border: 1px solid rgba(118, 189, 255, 0.06);
}

.option-text {
  font-size: 26rpx;
}

.option-btn--active {
  border-color: #8fd9ff;
  box-shadow: inset 0 0 0 2rpx rgba(143, 217, 255, 0.2);
  background: linear-gradient(180deg, #16304a 0%, #102338 100%);
}

.note-block {
  padding: 18rpx;
  display: grid;
  gap: 12rpx;
}

.note-title {
  font-size: 24rpx;
}

.note-textarea {
  width: 100%;
  min-height: 188rpx;
  border-radius: 12rpx;
  background: #091521;
  padding: 20rpx;
  color: #f3f8ff;
  font-size: 26rpx;
  box-sizing: border-box;
}

.note-count {
  justify-self: end;
  line-height: 1;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 18rpx;
  gap: 16rpx;
  background: linear-gradient(180deg, rgba(8, 20, 33, 0.02) 0%, #081421 22%);
  backdrop-filter: blur(18rpx);
}

.bottom-btn {
  flex: 1;
  height: 92rpx;
  line-height: 92rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
}

.bottom-btn--ghost {
  background: #122031;
  color: #f3f8ff;
  border: 1px solid rgba(118, 189, 255, 0.1);
}

.bottom-btn--primary {
  background: linear-gradient(90deg, #7ccfff 0%, #34d9ff 100%);
  color: #04111d;
  font-weight: 700;
  box-shadow: 0 18rpx 34rpx rgba(49, 217, 255, 0.28);
}

.bottom-btn--disabled {
  flex: 1;
  height: 92rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #223243;
  color: rgba(243, 248, 255, 0.56);
}
</style>