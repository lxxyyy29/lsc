<template>
  <div class="video-page">
    <!-- 外接设备接入提示（右上角，可关闭） -->
    <div v-if="!tipClosed" class="device-tip">
      <div class="tip-icon">📷</div>
      <div class="tip-body">
        <div class="tip-title">外接摄像头接入说明</div>
        <div class="tip-text">
          ① 摄像头按 <b>GB28181 国标</b>接入客户流媒体平台，或 <b>RTSP 推流</b>至流媒体系统；<br/>
          ② 流媒体系统自动转 HLS/FLV 直播并提供 <b>录像存储（保留 7 天）</b>；<br/>
          ③ 点击「新增点位」，流地址填写流媒体平台提供的 HLS/FLV/RTSP 地址即可接入；<br/>
          ④ 录像在「录像回放」中按日期查看。
          <span class="tip-note">（当前为演示流，外接后自动切换）</span>
        </div>
      </div>
      <button class="tip-close" @click="tipClosed = true">×</button>
    </div>

    <!-- 顶部统计卡 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">监控点位总数</div>
      </div>
      <div class="stat-card online">
        <div class="stat-num">{{ stats.online }}</div>
        <div class="stat-label">在线点位</div>
      </div>
      <div class="stat-card offline">
        <div class="stat-num">{{ stats.offline }}</div>
        <div class="stat-label">离线点位</div>
      </div>
      <div class="stat-card playable">
        <div class="stat-num">{{ playableCount }}</div>
        <div class="stat-label">可播放(转流就绪)</div>
      </div>
    </div>

    <div class="video-layout">
      <!-- 左:点位列表 -->
      <div class="camera-panel">
        <div class="panel-header">
          <div class="panel-title">监控点位</div>
          <div class="panel-actions">
            <select v-model="filterStatus" class="filter-select" @change="loadCameras">
              <option value="">全部状态</option>
              <option value="ACTIVE">在线</option>
              <option value="OFFLINE">离线</option>
              <option value="MAINTENANCE">维护</option>
            </select>
            <button class="btn-add" @click="openForm()">＋ 新增点位</button>
          </div>
        </div>
        <div class="camera-list">
          <div v-for="c in cameras" :key="c.id" class="camera-item"
               :class="{ active: currentCamera?.id === c.id }" @click="playCamera(c)">
            <div class="camera-info">
              <div class="camera-name">
                <span class="status-dot" :class="c.status === 'ACTIVE' ? 'dot-on' : 'dot-off'"></span>
                {{ c.camera_name }}
              </div>
              <div class="camera-meta">
                <span class="tag">{{ c.camera_type === 'PTZ' ? '球机' : '固定' }}</span>
                <span class="tag">{{ c.grid_name || '未关联网格' }}</span>
                <span class="tag" v-if="!c.playable" style="color:#e67e22;">无转流</span>
              </div>
            </div>
            <div class="camera-ops" @click.stop>
              <button class="op-btn" title="编辑" @click="openForm(c)">✎</button>
              <button class="op-btn danger" title="删除" @click="removeCamera(c)">🗑</button>
            </div>
          </div>
          <div v-if="cameras.length === 0" class="empty-tip">暂无监控点位</div>
        </div>
      </div>

      <!-- 右:播放器 + 轮巡控制 -->
      <div class="player-panel">
        <div class="player-wrap">
          <video id="videoPlayer" class="video-player" controls autoplay muted></video>
          <div v-if="!currentCamera" class="player-placeholder">
            <div class="ph-icon">📹</div>
            <div>点击左侧点位开始播放</div>
          </div>
          <div v-if="loadingStream" class="player-loading">正在连接视频流…</div>
        </div>

        <div class="player-bar">
          <div class="now-info">
            <div class="now-name">{{ currentCamera ? currentCamera.camera_name : '未选择点位' }}</div>
            <div class="now-sub">
              {{ currentCamera ? `${currentCamera.device_no} ｜ ${currentCamera.address || '未填位置'}` : '选择点位或开启轮巡' }}
            </div>
          </div>
          <div class="patrol-control">
            <label class="patrol-label">轮巡间隔</label>
            <select v-model="patrolInterval" class="filter-select" :disabled="patrolActive">
              <option :value="5">5 秒</option>
              <option :value="10">10 秒</option>
              <option :value="15">15 秒</option>
              <option :value="30">30 秒</option>
            </select>
            <button class="btn-patrol" :class="{ running: patrolActive }" @click="togglePatrol">
              {{ patrolActive ? `⏸ 轮巡中 ${patrolIndex + 1}/${patrolPool.length}` : '▶ 开始轮巡' }}
            </button>
            <button class="btn-record" @click="openRecord()">🎞 录像回放</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div v-if="formVisible" class="modal-mask">
      <div class="modal-box">
        <div class="modal-header">
          <div class="modal-title">{{ form.id ? '编辑点位' : '新增点位' }}</div>
          <button class="modal-close" @click="formVisible = false">×</button>
        </div>
        <div class="form-grid">
          <label class="form-item">
            <span>点位名称 *</span>
            <input v-model="form.cameraName" placeholder="如: 社区入口摄像头" />
          </label>
          <label class="form-item">
            <span>设备编号 *</span>
            <input v-model="form.deviceNo" placeholder="如: CAM-ENTRANCE" :disabled="!!form.id" />
          </label>
          <label class="form-item">
            <span>类型</span>
            <select v-model="form.cameraType">
              <option value="FIXED">固定枪机</option>
              <option value="PTZ">球机(PTZ)</option>
            </select>
          </label>
          <label class="form-item">
            <span>流类型</span>
            <select v-model="form.streamType">
              <option value="HLS">HLS(浏览器直播)</option>
              <option value="FLV">HTTP-FLV</option>
              <option value="RTSP">RTSP(平台转流)</option>
            </select>
          </label>
          <label class="form-item" style="grid-column: span 2;">
            <span>视频流地址 *</span>
            <input v-model="form.streamUrl" placeholder="如: https://平台/hls/xxx.m3u8 或 proxy://设备编号(平台演示流)" />
          </label>
          <label class="form-item">
            <span>安装位置</span>
            <input v-model="form.address" placeholder="如: 社区主入口" />
          </label>
          <label class="form-item">
            <span>所属网格</span>
            <select v-model="form.gridId" @change="syncGridName">
              <option :value="null">未关联</option>
              <option v-for="g in gridOptions" :key="g.id" :value="g.id">{{ g.gridName }}</option>
            </select>
          </label>
          <label class="form-item">
            <span>经度</span>
            <input v-model.number="form.longitude" placeholder="113.9395" />
          </label>
          <label class="form-item">
            <span>纬度</span>
            <input v-model.number="form.latitude" placeholder="22.9712" />
          </label>
          <label class="form-item">
            <span>状态</span>
            <select v-model="form.status">
              <option value="ACTIVE">在线</option>
              <option value="OFFLINE">离线</option>
              <option value="MAINTENANCE">维护中</option>
            </select>
          </label>
          <label class="form-item" style="grid-column: span 2;">
            <span>备注</span>
            <input v-model="form.remark" placeholder="备注说明" />
          </label>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="formVisible = false">取消</button>
          <button class="btn-save" @click="saveCamera">保存</button>
        </div>
      </div>
    </div>
    <!-- 录像回放弹窗 -->
    <div v-if="recordVisible" class="modal-mask">
      <div class="modal-box record-box">
        <div class="modal-header">
          <div class="modal-title">🎞 录像回放（保留 7 天）</div>
          <button class="modal-close" @click="closeRecord">×</button>
        </div>
        <div class="record-toolbar">
          <select v-model="recordCameraId" class="filter-select" @change="onRecordCameraChange">
            <option v-for="c in cameras" :key="c.id" :value="c.id">{{ c.camera_name }}</option>
          </select>
          <select v-model="recordDate" class="filter-select" @change="loadRecordList">
            <option v-for="d in recordDates" :key="d" :value="d">{{ d.slice(0,4) }}-{{ d.slice(4,6) }}-{{ d.slice(6,8) }}</option>
          </select>
        </div>
        <div class="record-body">
          <div class="record-list">
            <div v-for="r in recordList" :key="r.file" class="record-item"
                 :class="{ playing: recordFile === r.file }" @click="playRecord(r)">
              <span class="rec-time">{{ r.startTime.slice(9) }}</span>
              <span class="rec-dur">时长 {{ r.duration / 60 }} 分钟</span>
              <span class="rec-size">{{ (r.size / 1024 / 1024).toFixed(1) }}MB</span>
            </div>
            <div v-if="recordList.length === 0 && !recordLoading" class="empty-tip">该日期暂无录像</div>
          </div>
          <div class="record-player-wrap">
            <video id="recordPlayer" class="record-player" controls></video>
            <div v-if="!recordFile" class="player-placeholder">
              <div class="ph-icon">🎞</div>
              <div>选择左侧录像时间段开始回放</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  getVideoCameras, getVideoCameraStatistics, createVideoCamera,
  updateVideoCamera, deleteVideoCamera, getVideoCameraStream, getGridTree, getSession,
  getVideoRecordDates, getVideoRecords
} from '../api'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

interface Camera {
  id: number
  camera_name: string
  camera_type: string
  device_no: string
  stream_type: string
  stream_url: string
  longitude?: number
  latitude?: number
  address?: string
  grid_id?: number
  grid_name?: string
  status: string
  remark?: string
  playable?: boolean
}

const stats = ref({ total: 0, online: 0, offline: 0 })
const cameras = ref<Camera[]>([])
const filterStatus = ref('')
const currentCamera = ref<Camera | null>(null)
const loadingStream = ref(false)
const playableCount = ref(0)

// 轮巡
const patrolActive = ref(false)
const patrolInterval = ref(10)
const patrolIndex = ref(0)
const patrolPool = computed(() => cameras.value.filter(c => c.status === 'ACTIVE' && c.playable))
let patrolTimer: number | undefined
let hlsPlayer: any = null

// 表单
const formVisible = ref(false)
const gridOptions = ref<{ id: number; gridName: string }[]>([])
const form = ref<Record<string, any>>({})

// 外接提示（右上角，可关闭）
const tipClosed = ref(false)

// 录像回放
const recordVisible = ref(false)
const recordCameraId = ref<number | null>(null)
const recordDates = ref<string[]>([])
const recordDate = ref('')
const recordList = ref<{ file: string; startTime: string; duration: number; size: number }[]>([])
const recordLoading = ref(false)
const recordFile = ref('')

async function loadCameras() {
  const res: any = await getVideoCameras({ status: filterStatus.value || undefined, page: 1, size: 100 })
  cameras.value = res.items || []
  playableCount.value = cameras.value.filter(c => c.playable).length
  // 当前播放点位被删除/过滤时清空
  if (currentCamera.value && !cameras.value.some(c => c.id === currentCamera.value?.id)) {
    currentCamera.value = null
  }
}

async function loadStats() {
  const res: any = await getVideoCameraStatistics()
  stats.value = res || { total: 0, online: 0, offline: 0 }
}

async function loadGrids() {
  try {
    const res: any = await getGridTree()
    const walk = (nodes: any[], out: any[]) => {
      for (const n of nodes || []) {
        out.push({ id: n.id, gridName: n.gridName })
        if (n.children) walk(n.children, out)
      }
    }
    const list: any[] = []
    walk(res || [], list)
    gridOptions.value = list
  } catch { /* 网格加载失败不阻塞点位管理 */ }
}

function destroyPlayer() {
  if (hlsPlayer) {
    try { hlsPlayer.destroy() } catch { }
    hlsPlayer = null
  }
}

async function playCamera(c: Camera) {
  if (!c.playable && c.stream_type !== 'RTSP') {
    // 无转流文件：仍尝试播放（可能外部流），但不自动轮巡
  }
  currentCamera.value = c
  loadingStream.value = true
  destroyPlayer()
  try {
    const res: any = await getVideoCameraStream(c.id)
    const stream: any = res || {}
    const url: string = stream.streamUrl || ''
    if (!url) {
      loadingStream.value = false
      return
    }
    if (stream.streamType === 'FLV' && !url.endsWith('.m3u8') && !url.includes('.mp4')) {
      loadingStream.value = false
      return
    }
    const session = getSession()
    const video = document.getElementById('videoPlayer') as HTMLVideoElement
    if (!video) { loadingStream.value = false; return }
    // 按 URL 后缀分流: .m3u8 -> hls.js; .mp4(含.live.mp4 fMP4直播) -> 浏览器原生 video 直出
    const urlNoQuery = url.split('?')[0]
    if (urlNoQuery.endsWith('.mp4')) {
      video.src = url
      video.autoplay = true
      video.muted = true
      video.playsInline = true
      video.play().catch(() => { })
      loadingStream.value = false
      return
    }
    const Hls = (await import('hls.js')).default
    if (Hls.isSupported()) {
      hlsPlayer = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        xhrSetup: (xhr: XMLHttpRequest) => {
          if (session?.token) xhr.setRequestHeader('Authorization', `Bearer ${session.token}`)
        },
        backBufferLength: 60,
        maxBufferLength: 30,
        maxMaxBufferLength: 60,
        liveSyncDurationCount: 2,
        liveMaxLatencyDurationCount: 5,
        manifestLoadingTimeOut: 10000,
        manifestLoadingMaxRetry: 3,
        fragLoadingTimeOut: 20000,
        fragLoadingMaxRetry: 6
      })
      hlsPlayer.loadSource(url)
      hlsPlayer.attachMedia(video)
      hlsPlayer.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(() => { })
        loadingStream.value = false
      })
      hlsPlayer.on(Hls.Events.ERROR, (_: any, data: any) => {
        if (data.fatal) {
          setTimeout(() => {
            if (hlsPlayer) {
              hlsPlayer.loadSource(url + '?t=' + Date.now())
              hlsPlayer.attachMedia(video)
            }
          }, 2000)
        }
      })
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = url
      video.play().catch(() => { })
      loadingStream.value = false
    } else {
      loadingStream.value = false
    }
  } catch (e) {
    console.error('播放失败:', e)
    loadingStream.value = false
  }
}

function togglePatrol() {
  if (patrolActive.value) {
    stopPatrol()
  } else {
    const pool = patrolPool.value
    if (pool.length === 0) {
      showMessage('没有可轮巡的点位(需在线且转流就绪)')
      return
    }
    // 从当前点位或第一路开始
    const startIdx = currentCamera.value ? pool.findIndex(c => c.id === currentCamera.value?.id) : 0
    patrolIndex.value = startIdx >= 0 ? startIdx : 0
    patrolActive.value = true
    playCamera(pool[patrolIndex.value])
    patrolTimer = window.setInterval(() => {
      const p = patrolPool.value
      if (p.length === 0) { stopPatrol(); return }
      patrolIndex.value = (patrolIndex.value + 1) % p.length
      playCamera(p[patrolIndex.value])
    }, patrolInterval.value * 1000)
  }
}

function stopPatrol() {
  patrolActive.value = false
  if (patrolTimer) {
    clearInterval(patrolTimer)
    patrolTimer = undefined
  }
}

function openForm(c?: Camera) {
  form.value = c
    ? {
        id: c.id, cameraName: c.camera_name, cameraType: c.camera_type, deviceNo: c.device_no,
        streamType: c.stream_type, streamUrl: c.stream_url, longitude: c.longitude, latitude: c.latitude,
        address: c.address, gridId: c.grid_id ?? null, gridName: c.grid_name, status: c.status, remark: c.remark
      }
    : { cameraName: '', cameraType: 'FIXED', deviceNo: '', streamType: 'HLS', streamUrl: '', longitude: undefined, latitude: undefined, address: '', gridId: null, gridName: '', status: 'ACTIVE', remark: '' }
  formVisible.value = true
}

function syncGridName() {
  const g = gridOptions.value.find(g => g.id === form.value.gridId)
  form.value.gridName = g ? g.gridName : ''
}

async function saveCamera() {
  if (!form.value.cameraName?.trim()) { showMessage('请填写点位名称'); return }
  if (!form.value.deviceNo?.trim()) { showMessage('请填写设备编号'); return }
  if (!form.value.streamUrl?.trim()) { showMessage('请填写视频流地址'); return }
  try {
    if (form.value.id) {
      await updateVideoCamera(form.value.id, form.value)
    } else {
      await createVideoCamera(form.value)
    }
    formVisible.value = false
    await loadCameras()
    await loadStats()
  } catch (e: any) {
    showMessage(e?.response?.data?.message || '保存失败')
  }
}

async function removeCamera(c: Camera) {
  if (!await confirmDialog({ message: `确认删除点位「${c.camera_name}」？`, danger: true, okText: '删除' })) return
  await deleteVideoCamera(c.id)
  if (currentCamera.value?.id === c.id) {
    currentCamera.value = null
    destroyPlayer()
  }
  await loadCameras()
  await loadStats()
}

// ==================== 录像回放 ====================
async function openRecord() {
  recordVisible.value = true
  recordCameraId.value = currentCamera.value?.id ?? cameras.value[0]?.id ?? null
  if (recordCameraId.value) {
    await loadRecordDates()
  }
}

function closeRecord() {
  recordVisible.value = false
  recordFile.value = ''
  const v = document.getElementById('recordPlayer') as HTMLVideoElement | null
  if (v) { v.pause(); v.removeAttribute('src'); v.load() }
}

async function onRecordCameraChange() {
  recordFile.value = ''
  recordDate.value = ''
  if (recordCameraId.value) await loadRecordDates()
}

async function loadRecordDates() {
  const res: any = await getVideoRecordDates(recordCameraId.value!)
  recordDates.value = res || []
  recordDate.value = recordDates.value[0] || ''
  if (recordDate.value) await loadRecordList()
}

async function loadRecordList() {
  recordLoading.value = true
  try {
    const res: any = await getVideoRecords(recordCameraId.value!, recordDate.value)
    recordList.value = res || []
  } finally {
    recordLoading.value = false
  }
}

async function playRecord(r: { file: string; startTime: string; duration: number; size: number }) {
  recordFile.value = r.file
  const session = getSession()
  try {
    // mp4 播放无法带请求头，用 fetch + blob 携带 Authorization 后本地播放（支持拖动）
    const resp = await fetch(`/api/video/record/${recordCameraId.value}/${recordDate.value}/${r.file}`, {
      headers: session?.token ? { Authorization: `Bearer ${session.token}` } : {}
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    const v = document.getElementById('recordPlayer') as HTMLVideoElement
    if (v) {
      v.src = url
      await v.play().catch(() => { })
    }
  } catch (e) {
    showMessage('录像加载失败: ' + (e as Error).message)
    recordFile.value = ''
  }
}

onMounted(async () => {
  await Promise.allSettled([loadCameras(), loadStats(), loadGrids()])
  // 登录态恢复竞态导致首轮请求失败时，延时重试一次避免页面空白
  setTimeout(async () => {
    await Promise.allSettled([loadCameras(), loadStats(), loadGrids()])
  }, 1500)
})

onUnmounted(() => {
  stopPatrol()
  destroyPlayer()
})
</script>

<style scoped>
.video-page { padding: 20px; display: flex; flex-direction: column; gap: 16px; }

/* 右上角外接设备提示 */
.device-tip {
  position: fixed; top: 64px; right: 16px; z-index: 90; max-width: 420px;
  background: #fff; border: 1px solid #bfdbfe; border-left: 4px solid #0284c7;
  border-radius: 10px; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  display: flex; gap: 10px; padding: 12px 14px; align-items: flex-start;
}
.tip-icon { font-size: 20px; }
.tip-body { flex: 1; }
.tip-title { font-size: 13px; font-weight: 700; color: #1e293b; margin-bottom: 4px; }
.tip-text { font-size: 12px; color: #475569; line-height: 1.7; }
.tip-note { color: #94a3b8; font-size: 11px; }
.tip-close {
  border: none; background: #f1f5f9; color: #64748b; width: 22px; height: 22px;
  border-radius: 6px; cursor: pointer; font-size: 14px; line-height: 1; flex-shrink: 0;
}
.btn-record {
  padding: 8px 14px; border: 1px solid #0284c7; background: #fff; color: #0284c7;
  border-radius: 8px; font-size: 13px; cursor: pointer; font-weight: 600;
}
.btn-record:hover { background: #eff6ff; }

/* 统计卡 */
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card {
  background: #fff; border-radius: 12px; padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); border-left: 4px solid #0284c7;
}
.stat-card.online { border-left-color: #28a745; }
.stat-card.offline { border-left-color: #dc3545; }
.stat-card.playable { border-left-color: #f59e0b; }
.stat-num { font-size: 28px; font-weight: 700; color: #1e293b; }
.stat-label { font-size: 13px; color: #64748b; margin-top: 4px; }

/* 主布局 */
.video-layout { display: grid; grid-template-columns: 340px 1fr; gap: 16px; min-height: 560px; }

.camera-panel, .player-panel {
  background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex; flex-direction: column; overflow: hidden;
}
.panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; border-bottom: 1px solid #eef2f7;
}
.panel-title { font-size: 15px; font-weight: 600; color: #1e293b; }
.panel-actions { display: flex; gap: 8px; align-items: center; }
.filter-select {
  padding: 6px 10px; border: 1px solid #dbe2ea; border-radius: 8px;
  font-size: 13px; color: #334155; background: #fff; outline: none;
}
.btn-add {
  padding: 6px 14px; background: #0284c7; color: #fff; border: none;
  border-radius: 8px; font-size: 13px; cursor: pointer;
}
.btn-add:hover { background: #0369a1; }

.camera-list { flex: 1; overflow-y: auto; padding: 8px; }
.camera-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 12px; border-radius: 10px; cursor: pointer; border: 1px solid transparent;
  transition: all 0.15s;
}
.camera-item:hover { background: #f8fafc; }
.camera-item.active { background: #eff6ff; border-color: #0284c7; }
.camera-name { font-size: 14px; font-weight: 600; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dot-on { background: #28a745; }
.dot-off { background: #dc3545; }
.camera-meta { display: flex; gap: 6px; margin-top: 6px; }
.tag {
  font-size: 11px; color: #475569; background: #f1f5f9; padding: 2px 8px; border-radius: 6px;
}
.camera-ops { display: flex; gap: 6px; }
.op-btn {
  width: 28px; height: 28px; border: 1px solid #dbe2ea; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 13px; line-height: 1;
}
.op-btn:hover { border-color: #0284c7; color: #0284c7; }
.op-btn.danger:hover { border-color: #dc3545; color: #dc3545; }
.empty-tip { text-align: center; color: #94a3b8; padding: 40px 0; font-size: 13px; }

/* 播放器 */
.player-panel { padding: 16px; gap: 12px; }
.player-wrap { position: relative; flex: 1; background: #0f172a; border-radius: 10px; overflow: hidden; }
.video-player { width: 100%; height: 100%; min-height: 380px; object-fit: contain; }
.player-placeholder {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center;
  justify-content: center; color: #64748b; gap: 10px;
}
.ph-icon { font-size: 48px; opacity: 0.5; }
.player-loading {
  position: absolute; top: 12px; left: 12px; background: rgba(0, 0, 0, 0.6);
  color: #fff; font-size: 12px; padding: 6px 12px; border-radius: 6px;
}
.player-bar { display: flex; justify-content: space-between; align-items: center; }
.now-name { font-size: 15px; font-weight: 600; color: #1e293b; }
.now-sub { font-size: 12px; color: #64748b; margin-top: 2px; }
.patrol-control { display: flex; align-items: center; gap: 8px; }
.patrol-label { font-size: 13px; color: #475569; }
.btn-patrol {
  padding: 8px 16px; border: none; border-radius: 8px; font-size: 13px; cursor: pointer;
  background: #0284c7; color: #fff; font-weight: 600;
}
.btn-patrol:hover { background: #0369a1; }
.btn-patrol.running { background: #e67e22; }
.btn-patrol.running:hover { background: #d35400; }

/* 弹窗 */
.modal-mask {
  position: fixed; inset: 0; background: rgba(15, 23, 42, 0.5); z-index: 100;
  display: flex; align-items: center; justify-content: center;
}
.modal-box {
  width: 640px; max-height: 86vh; overflow-y: auto; background: #fff;
  border-radius: 14px; padding: 20px; box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.modal-title { font-size: 16px; font-weight: 700; color: #1e293b; }
.modal-close {
  width: 30px; height: 30px; border: none; background: #f1f5f9; border-radius: 8px;
  font-size: 18px; cursor: pointer; color: #64748b;
}
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item span { font-size: 12px; color: #475569; font-weight: 600; }
.form-item input, .form-item select {
  padding: 8px 10px; border: 1px solid #dbe2ea; border-radius: 8px; font-size: 13px;
  outline: none; background: #fff;
}
.form-item input:focus, .form-item select:focus { border-color: #0284c7; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }
.btn-cancel {
  padding: 8px 18px; border: 1px solid #dbe2ea; background: #fff; border-radius: 8px;
  font-size: 13px; cursor: pointer; color: #475569;
}
.btn-save {
  padding: 8px 22px; background: #0284c7; color: #fff; border: none; border-radius: 8px;
  font-size: 13px; cursor: pointer; font-weight: 600;
}
.btn-save:hover { background: #0369a1; }

/* 录像回放弹窗 */
.record-box { width: 900px; }
.record-toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.record-body { display: grid; grid-template-columns: 260px 1fr; gap: 12px; min-height: 380px; }
.record-list {
  border: 1px solid #eef2f7; border-radius: 10px; overflow-y: auto; max-height: 420px; padding: 6px;
}
.record-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px;
  cursor: pointer; font-size: 12px; color: #334155;
}
.record-item:hover { background: #f8fafc; }
.record-item.playing { background: #eff6ff; border: 1px solid #0284c7; }
.rec-time { font-weight: 700; color: #0f172a; font-size: 13px; }
.rec-dur { color: #64748b; }
.rec-size { margin-left: auto; color: #94a3b8; }
.record-player-wrap { position: relative; background: #0f172a; border-radius: 10px; overflow: hidden; }
.record-player { width: 100%; max-height: 420px; background: #0f172a; }
</style>