<template>
  <PageContainer title="媒体中心">
    <!-- Tabs -->
    <div class="media-tabs">
      <button
        class="media-tab"
        :class="{ 'media-tab--active': activeTab === 'dock' }"
        @click="switchTab('dock')"
      >
        机场文件
      </button>
      <button
        class="media-tab"
        :class="{ 'media-tab--active': activeTab === 'remote' }"
        @click="switchTab('remote')"
      >
        遥控器文件
      </button>
    </div>

    <WebListPageTemplate
      filter-title="全部文件"
      :table-title="currentFolderName ? '' : ''"
      :table-meta="viewMode === 'folders' ? `当前共 ${folders.length} 个文件夹` : `当前共 ${files.length} 个文件`"
    >
      <template #filters>
        <!-- Breadcrumb + Back -->
        <div v-if="viewMode === 'files'" class="breadcrumb-row">
          <button class="action-button action-button--secondary" @click="goBackToFolders">返 回</button>
          <span class="breadcrumb-text">全部文件 / {{ currentFolderName }}</span>
        </div>

        <!-- Search filters (only in folder view) -->
        <QueryPanel v-if="viewMode === 'folders'">
          <label class="field-stack">
            <ElInput v-model="searchFileName" aria-label="文件名" placeholder="文件名" clearable />
          </label>
          <label class="field-stack">
            <ZhDateRangePicker v-model:start="searchStartTime" v-model:end="searchEndTime" placeholder="选择创建日期范围" />
          </label>
          <div class="query-actions">
            <button class="action-button" @click="handleSearch">查 询</button>
            <button class="action-button action-button--secondary" @click="handleReset">重 置</button>
          </div>
        </QueryPanel>
      </template>

      <template #table>
        <!-- Folder list -->
        <table v-if="viewMode === 'folders'" class="data-table">
          <thead>
            <tr>
              <th>文件名</th>
              <th>图片/视频</th>
              <th>无人机</th>
              <th>拍摄负载</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading" class="loading-row">
              <td colspan="6">
                <div class="empty-state">加载中...</div>
              </td>
            </tr>
            <tr v-else-if="folders.length === 0">
              <td colspan="6">
                <div class="empty-state">暂无数据</div>
              </td>
            </tr>
            <tr
              v-for="folder in folders"
              :key="folder.jobId"
              class="folder-row"
              @click="openFolder(folder)"
            >
              <td>
                <span class="folder-icon">&#x1F4C1;</span>
                {{ folder.fileName || '--' }}
              </td>
              <td>---</td>
              <td>---</td>
              <td>---</td>
              <td>{{ formatTime(folder.createTime) }}</td>
              <td>
                <button
                  class="action-button--secondary action-button action-button--small"
                  :disabled="!!folderDownloading"
                  @click.stop="downloadFolder(folder)"
                >
                  {{ folderDownloading === folder.jobId ? '打包中...' : '下载文件' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- File list -->
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>文件名</th>
              <th>图片/视频</th>
              <th>无人机</th>
              <th>拍摄负载</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loadingFiles" class="loading-row">
              <td colspan="6">
                <div class="empty-state">加载中...</div>
              </td>
            </tr>
            <tr v-else-if="files.length === 0">
              <td colspan="6">
                <div class="empty-state">暂无文件</div>
              </td>
            </tr>
            <tr v-for="file in files" :key="file.id || file.fileName">
              <td>{{ file.fileName || '--' }}</td>
              <td class="media-cell">
                <!-- 图片：previewUrl（小缩略图）或 objectKey -->
                <img
                  v-if="isImage(file.fileName) && (file.previewUrl || file.objectKey)"
                  :src="file.previewUrl || toImageUrl(file.objectKey)"
                  class="media-thumb"
                  title="点击预览"
                  @click="openPreview(file)"
                />
                <!-- 视频：objectKey 直接作为 video src，内嵌缩略帧 -->
                <video
                  v-else-if="isVideo(file.fileName) && file.objectKey"
                  :src="toImageUrl(file.objectKey)"
                  class="media-thumb media-thumb--video"
                  title="点击预览视频"
                  @click="openPreview(file)"
                />
                <!-- 兜底类型标记 -->
                <span v-else-if="isVideo(file.fileName)" class="media-badge media-badge--video" title="视频">&#9654;</span>
                <span v-else-if="isImage(file.fileName)" class="media-badge media-badge--image" title="图片">&#128247;</span>
                <span v-else>--</span>
              </td>
              <td>{{ file.droneSn || '--' }}</td>
              <td>{{ file.payloadName || '--' }}</td>
              <td>{{ formatTime(file.createTime) }}</td>
              <td class="file-actions">
                <button
                  class="action-link"
                  title="下载"
                  @click="downloadFile(file)"
                >&#11015;</button>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </WebListPageTemplate>

    <ImagePreviewOverlay v-model="previewVisible" :images="previewImages" :index="previewIndex" />
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import ZhDateRangePicker from '../../components/common/ZhDateRangePicker.vue'
import ImagePreviewOverlay from '../../components/ImagePreviewOverlay.vue'
import { toImageUrl, fetchAccessPrefix } from '../../api/upload'
import {
  listMediaFolders,
  getMediaFilesByJobId,
  isMediaVideo,
  isMediaImage,
  formatDroneTimestamp,
  type MediaFolder,
  type MediaFile
} from '../../api/drone'

const toast = useToast()

const activeTab = ref<'dock' | 'remote'>('dock')
const viewMode = ref<'folders' | 'files'>('folders')

// Folder list state
const folders = ref<MediaFolder[]>([])
const loading = ref(false)
const searchFileName = ref('')
const searchStartTime = ref('')
const searchEndTime = ref('')

// File list state
const files = ref<MediaFile[]>([])
const loadingFiles = ref(false)
const currentFolderName = ref('')
const currentJobId = ref('')

// Preview state
const previewVisible = ref(false)
const previewImages = ref<string[]>([])
const previewIndex = ref(0)

function formatTime(value?: string | number | null) {
  return formatDroneTimestamp(value)
}

function isVideo(fileName?: string | null) {
  return isMediaVideo(fileName)
}

function isImage(fileName?: string | null) {
  return isMediaImage(fileName)
}

async function loadFolders() {
  loading.value = true
  try {
    folders.value = await listMediaFolders({
      fileName: searchFileName.value || undefined,
      startTime: searchStartTime.value || undefined,
      endTime: searchEndTime.value || undefined
    })
  } catch (err) {
    console.error('Failed to load media folders:', err)
    folders.value = []
  } finally {
    loading.value = false
  }
}

async function loadFiles(jobId: string) {
  loadingFiles.value = true
  try {
    files.value = await getMediaFilesByJobId(jobId)
  } catch (err) {
    console.error('Failed to load media files:', err)
    files.value = []
  } finally {
    loadingFiles.value = false
  }
}

function openFolder(folder: MediaFolder) {
  currentFolderName.value = folder.fileName || '--'
  currentJobId.value = folder.jobId
  viewMode.value = 'files'
  loadFiles(folder.jobId)
}

function goBackToFolders() {
  viewMode.value = 'folders'
  currentFolderName.value = ''
  currentJobId.value = ''
  files.value = []
}

function handleSearch() {
  loadFolders()
}

function handleReset() {
  searchFileName.value = ''
  searchStartTime.value = ''
  searchEndTime.value = ''
  loadFolders()
}

function switchTab(tab: 'dock' | 'remote') {
  activeTab.value = tab
  goBackToFolders()
  loadFolders()
}

function openPreview(file: MediaFile) {
  if (isMediaVideo(file.fileName)) {
    // 视频在新标签页原生播放
    const url = file.objectKey ? toImageUrl(file.objectKey) : ''
    if (url) window.open(url, '_blank')
    return
  }
  // 图片：收集当前文件夹所有图片，支持左右切换
  const imageFiles = files.value.filter(f => isMediaImage(f.fileName) && (f.previewUrl || f.objectKey))
  const idx = imageFiles.findIndex(f => f === file)
  previewImages.value = imageFiles
    .map(f => f.previewUrl || toImageUrl(f.objectKey) || '')
    .filter(Boolean)
  previewIndex.value = Math.max(0, idx)
  previewVisible.value = true
}

const folderDownloading = ref<string | null>(null)

async function downloadFolder(folder: MediaFolder) {
  if (folderDownloading.value) return
  folderDownloading.value = folder.jobId

  const token = JSON.parse(localStorage.getItem('dgcp-oa-web-session') || '{}').token || ''
  try {
    const response = await fetch(`/api/drone/media/download/${encodeURIComponent(folder.jobId)}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!response.ok) throw new Error(`下载失败 (${response.status})`)

    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = (folder.fileName || folder.jobId) + '.zip'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    toast.error('下载失败，请稍后重试')
  } finally {
    folderDownloading.value = null
  }
}

function downloadFile(file: MediaFile) {
  // objectKey 是 MinIO 中文件的完整 URL
  const rawUrl = file.objectKey ? toImageUrl(file.objectKey) : (file.previewUrl || '')
  if (!rawUrl) {
    toast.info('该文件暂无下载地址')
    return
  }
  // 追加 MinIO 参数强制返回 Content-Type: application/octet-stream
  // 让浏览器触发下载（显示工具栏进度）而非在线播放/预览
  const sep = rawUrl.includes('?') ? '&' : '?'
  const downloadUrl = rawUrl + sep + 'response-content-type=application%2Foctet-stream'
  const a = document.createElement('a')
  a.href = downloadUrl
  a.download = file.fileName || ''
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
}

onMounted(() => {
  fetchAccessPrefix().catch(() => {}) // 预取 OSS 前缀，供 objectKey 拼 URL 使用
  loadFolders()
})
</script>

<style scoped>
@import '../admin-shared.css';

.media-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
  border-bottom: 1px solid rgba(103, 187, 246, 0.2);
}

.media-tab {
  padding: 10px 24px;
  border: none;
  background: transparent;
  color: rgba(205, 222, 248, 0.78);
  font-size: 14px;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.media-tab:hover {
  color: #eef5ff;
}

.media-tab--active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.breadcrumb-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.breadcrumb-text {
  color: rgba(205, 222, 248, 0.88);
  font-size: 14px;
}

.query-actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-top: 24px;
}

.action-button--small {
  padding: 6px 14px;
  font-size: 12px;
}

.folder-row {
  cursor: pointer;
  transition: background 0.15s;
}

.folder-row:hover {
  background: rgba(64, 158, 255, 0.08);
}

.folder-icon {
  margin-right: 6px;
  font-size: 16px;
}

.media-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  font-size: 14px;
}

.media-badge--video {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
}

.media-badge--image {
  background: rgba(103, 194, 58, 0.15);
  color: #67c23a;
}

.media-cell {
  text-align: center;
}

.media-thumb {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid rgba(64, 158, 255, 0.2);
  transition: opacity 0.15s, border-color 0.15s;
  display: block;
  margin: 0 auto;
}

.media-thumb:hover {
  opacity: 0.8;
  border-color: rgba(64, 158, 255, 0.6);
}

.media-badge--clickable {
  cursor: pointer;
  transition: opacity 0.15s;
}

.media-badge--clickable:hover {
  opacity: 0.75;
}

.file-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.media-thumb--video {
  pointer-events: all;
  background: #000;
}

.loading-row td {
  text-align: center;
}

.empty-state {
  padding: 24px 12px;
  text-align: center;
  color: rgba(205, 222, 248, 0.78);
}
</style>
