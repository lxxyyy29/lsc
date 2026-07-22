<template>
  <ol class="record-timeline">
    <li v-for="(record, index) in records" :key="record.id" class="record-timeline__item">
      <div class="record-timeline__flow">
        <div class="record-timeline__dot" :class="`status-${record.status.toLowerCase()}`"></div>
        <div class="record-timeline__line" v-if="index !== records.length - 1">
          <i class="record-timeline__arrow"></i>
        </div>
      </div>
      <div class="record-timeline__content">
        <div class="record-timeline__meta">
          <strong>{{ record.title }}</strong>
          <StatusTag :status="record.status" />
        </div>
        <p>{{ record.description || '无执行详情' }}</p>
        <small>{{ record.operator }} · {{ record.timestamp }}</small>
        <div v-if="record.subjectType && record.subjectId" class="record-timeline__subject">
          <span class="record-timeline__subject-label">
            {{ record.subjectType === 'MERCHANT' ? '关联商户' : '关联摊贩' }}：
          </span>
          <span class="record-timeline__subject-value">
            {{ record.subjectName || `#${record.subjectId}` }}
          </span>
        </div>
        <div v-if="record.attachments && record.attachments.length" class="record-timeline__attachments">
          <template v-for="att in record.attachments" :key="att.fileUrl">
            <img
              v-if="isImage(att)"
              :src="toImageUrl(att.fileUrl)"
              :alt="att.fileName"
              class="record-timeline__attachment-img"
              @click="openAttachmentPreview(record.attachments!, att)"
              @error="onImgError"
            />
            <span v-else class="record-timeline__attachment-tag">
              {{ fileTypeLabel(att.fileType) }}{{ att.fileName }}
            </span>
          </template>
        </div>
      </div>
    </li>
  </ol>
  <ImagePreviewOverlay :images="previewImages" :index="previewIndex" v-model="previewVisible" />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import StatusTag from './StatusTag.vue'
import ImagePreviewOverlay from '../ImagePreviewOverlay.vue'
import { toImageUrl } from '../../api/upload'

interface TimelineAttachment {
  fileName: string
  fileUrl: string
  fileType?: string
  mimeType?: string
}

interface TimelineRecord {
  id: string
  title: string
  description: string
  operator: string
  timestamp: string
  status: string
  subjectType?: string | null
  subjectId?: number | null
  subjectName?: string | null
  attachments?: TimelineAttachment[]
}

defineProps<{
  records: TimelineRecord[]
}>()

const previewVisible = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

function isImage(att: TimelineAttachment): boolean {
  if (att.mimeType?.startsWith('image/')) return true
  if (att.fileType === 'IMAGE') return true
  const ext = att.fileName?.toLowerCase() || att.fileUrl?.toLowerCase() || ''
  return /\.(png|jpe?g|gif|webp|bmp|svg)(\?.*)?$/i.test(ext)
}

function openAttachmentPreview(attachments: TimelineAttachment[], clicked: TimelineAttachment) {
  const images = attachments.filter(isImage).map((a) => toImageUrl(a.fileUrl))
  const idx = images.indexOf(toImageUrl(clicked.fileUrl))
  previewImages.value = images
  previewIndex.value = idx >= 0 ? idx : 0
  previewVisible.value = true
}

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

function fileTypeLabel(fileType?: string): string {
  switch (fileType) {
    case 'FINE':
      return '[罚单] '
    case 'EVIDENCE':
      return '[误报证据] '
    case 'REFERENCE':
      return '[参考] '
    default:
      return ''
  }
}
</script>

<style scoped>
.record-timeline {
  list-style: none;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0;
  margin: 0;
  overflow-x: auto;
  padding-bottom: 12px;
}

.record-timeline__item {
  flex: 1;
  flex-shrink: 0;
  min-width: 250px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-timeline__flow {
  display: flex;
  align-items: center;
  height: 14px;
}

.record-timeline__dot {
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #23a0fa;
  box-shadow: 0 0 0 3px rgba(35, 160, 250, 0.2);
  z-index: 2;
}

.record-timeline__dot.status-completed {
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.2);
}

.record-timeline__line {
  flex: 1;
  height: 2px;
  background: rgba(115, 235, 255, 0.25);
  margin-left: 8px;
  margin-right: 16px;
  position: relative;
}

.record-timeline__arrow {
  position: absolute;
  right: -2px;
  top: -3.5px;
  border: solid rgba(115, 235, 255, 0.6);
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 3px;
  transform: rotate(-45deg);
}

.record-timeline__content {
  padding-right: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-timeline__meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.record-timeline__meta strong {
  font-size: 15px;
  color: #eaf5ff;
}

.record-timeline__content p {
  margin: 0;
  font-size: 13px;
  color: #dce9f5;
  line-height: 1.5;
}

.record-timeline__content small {
  color: #8db0d0;
  font-size: 12px;
}

.record-timeline__subject {
  font-size: 12px;
  color: #8db0d0;
}

.record-timeline__subject-label {
  color: #8db0d0;
}

.record-timeline__subject-value {
  color: #67c23a;
  font-weight: 500;
}

.record-timeline__attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.record-timeline__attachment-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(35, 160, 250, 0.12);
  color: #8db0d0;
  border: 1px solid rgba(35, 160, 250, 0.2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.record-timeline__attachment-img {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid rgba(103, 187, 246, 0.2);
  background: rgba(0, 0, 0, 0.2);
  cursor: pointer;
  transition: border-color 0.2s, transform 0.2s;
}

.record-timeline__attachment-img:hover {
  border-color: rgba(64, 158, 255, 0.6);
  transform: scale(1.05);
}
</style>


<style scoped>
.record-timeline {
  list-style: none;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  padding: 0;
  margin: 0;
  overflow-x: auto;
  padding-bottom: 12px;
}

.record-timeline__item {
  flex: 1;
  flex-shrink: 0;
  min-width: 250px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.record-timeline__flow {
  display: flex;
  align-items: center;
  height: 14px;
}

.record-timeline__dot {
  flex-shrink: 0;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #23a0fa;
  box-shadow: 0 0 0 3px rgba(35, 160, 250, 0.2);
  z-index: 2;
}

.record-timeline__dot.status-completed {
  background: #67c23a;
  box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.2);
}

.record-timeline__line {
  flex: 1;
  height: 2px;
  background: rgba(115, 235, 255, 0.25);
  margin-left: 8px;
  margin-right: 16px;
  position: relative;
}

.record-timeline__arrow {
  position: absolute;
  right: -2px;
  top: -3.5px;
  border: solid rgba(115, 235, 255, 0.6);
  border-width: 0 2px 2px 0;
  display: inline-block;
  padding: 3px;
  transform: rotate(-45deg);
}

.record-timeline__content {
  padding-right: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-timeline__meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.record-timeline__meta strong {
  font-size: 15px;
  color: #eaf5ff;
}

.record-timeline__content p {
  margin: 0;
  font-size: 13px;
  color: #dce9f5;
  line-height: 1.5;
}

.record-timeline__content small {
  color: #8db0d0;
  font-size: 12px;
}
</style>
