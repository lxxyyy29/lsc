<template>
  <div v-if="open" class="ds-overlay" role="dialog" aria-modal="true" aria-label="无人机状态">
    <div class="ds-layout">
      <!-- Left: Map full height -->
      <div class="ds-map-area">
        <div class="ds-section-label">实时地图</div>
        <div ref="mapContainer" class="ds-map"></div>
        <div v-if="!mapReady" class="ds-map-loading">地图加载中...</div>
        <button type="button" class="ds-back-btn" @click="emit('close')">返回</button>
      </div>

      <!-- Right: Video (top) + Panels (bottom) -->
      <div class="ds-right">
        <!-- Video -->
        <div class="ds-video-area">
          <div class="ds-video-topbar">
            <div class="ds-video-topbar__left">
              <span>{{ isDroneActive ? '飞机直播视频' : '机场直播视频' }}</span>
              <span class="ds-video-topbar__status" :class="droneStatusClass">{{ droneStatusLabel }}</span>
            </div>
            <div class="ds-video-topbar__right">
              <span>{{ currentTime }}</span>
              <span v-if="fmtBattery !== NA">电量: {{ fmtBattery }}</span>
              <span v-if="fmtDockTemperature !== NA">温度: {{ fmtDockTemperature }}</span>
            </div>
          </div>
          <div class="ds-video-wrap">
            <WebRtcPlayer v-if="open && activeVideoUrl" :url="activeVideoUrl" :key="activeVideoUrl" />
            <div v-else class="ds-video-empty">暂无视频流</div>
          </div>
        </div>

        <!-- Status panels -->
        <div class="ds-panels">
          <!-- 机场信息 -->
          <section class="ds-panel">
            <h4 class="ds-panel__title">机场信息</h4>
        <div class="ds-panel__grid">
          <div class="ds-info-item">
            <span class="ds-info-icon">🌡</span>
            <span class="ds-info-label">舱内温度</span>
            <span class="ds-info-value">{{ fmtDockTemperature }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">💨</span>
            <span class="ds-info-label">风速</span>
            <span class="ds-info-value">{{ fmtDockWindSpeed }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">🌧</span>
            <span class="ds-info-label">降雨量</span>
            <span class="ds-info-value">{{ fmtDockRainfall }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">📐</span>
            <span class="ds-info-label">海拔高度</span>
            <span class="ds-info-value">{{ fmtDockHeight }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">⚡</span>
            <span class="ds-info-label">工作电压</span>
            <span class="ds-info-value">{{ fmtDockVoltage }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">⚡</span>
            <span class="ds-info-label">工作电流</span>
            <span class="ds-info-value">{{ fmtDockCurrent }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">💾</span>
            <span class="ds-info-label">可用存储</span>
            <span class="ds-info-value">{{ fmtDockStorage }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">❄</span>
            <span class="ds-info-label">空调</span>
            <span class="ds-info-value">{{ fmtDockAC }}</span>
          </div>
        </div>
      </section>

      <!-- 无人机信息 -->
      <section class="ds-panel">
        <h4 class="ds-panel__title">无人机信息</h4>
        <div class="ds-panel__grid">
          <div class="ds-info-item">
            <span class="ds-info-icon">🔋</span>
            <span class="ds-info-label">电池电量</span>
            <span class="ds-info-value">{{ fmtBattery }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">↕</span>
            <span class="ds-info-label">离地高度</span>
            <span class="ds-info-value">{{ fmtHeight }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">📡</span>
            <span class="ds-info-label">GPS</span>
            <span class="ds-info-value">{{ fmtGps }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">🏠</span>
            <span class="ds-info-label">返航距离</span>
            <span class="ds-info-value">{{ fmtHomeDistance }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">📐</span>
            <span class="ds-info-label">海拔高度</span>
            <span class="ds-info-value">{{ fmtElevation }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-label">水平速度</span>
            <span class="ds-info-value">{{ fmtHSpeed }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-label">垂直速度</span>
            <span class="ds-info-value">{{ fmtVSpeed }}</span>
          </div>
          <div class="ds-info-item">
            <span class="ds-info-icon">💨</span>
            <span class="ds-info-label">风速</span>
            <span class="ds-info-value">{{ fmtDroneWindSpeed }}</span>
          </div>
        </div>
      </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, toRef, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { useDroneWebSocket } from '../../composables/useDroneWebSocket'
import { normalizeDroneAircraftMode, isDroneFlying, isDroneOffline } from '../../api/drone'
import WebRtcPlayer from './WebRtcPlayer.vue'

const props = defineProps<{
  open: boolean
  deviceSn: string
  dockVideoUrl?: string   // 机场直播视频 URL
  droneVideoUrl?: string  // 无人机直播视频 URL（有值代表无人机已开机飞行）
  initialLng?: number
  initialLat?: number
}>()

const emit = defineEmits<{
  close: []
}>()

const NA = '--'

// --- Clock ---
const currentTime = ref('')
let clockTimer: ReturnType<typeof setInterval> | null = null

function updateClock() {
  const now = new Date()
  const y = now.getFullYear()
  const M = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${y}-${M}-${d} ${h}:${m}:${s}`
}

// --- WebSocket ---
const snRef = toRef(props, 'deviceSn')
const enabledRef = computed(() => props.open)
const {
  connected,
  connecting,
  errorMessage,
  dockState,
  deviceState,
} = useDroneWebSocket(snRef, { enabled: enabledRef })

// --- 无人机 mode_code 实时状态判定（替代静态 droneVideoUrl 判断） ---
const droneModeCode = computed(() => {
  if (!deviceState.value) return null
  const mc = (deviceState.value as Record<string, unknown>).mode_code ?? deviceState.value.modeCode
  return mc != null ? Number(mc) : null
})

const isOnline = computed(() => droneModeCode.value != null && !isDroneOffline(droneModeCode.value))

// 无人机在飞行中 → 显示无人机视频；否则显示机场视频
// 无人机已开机（有 device_osd 数据且不是离线/未连接）→ 显示无人机视频
// 无人机关机或无数据 → 显示机场视频
// 注意：无人机开机后摄像头即可用，不需要处于飞行状态
const isDroneActive = computed(() => {
  if (!deviceState.value) return false
  return !isDroneOffline(droneModeCode.value)
})

const droneStatusLabel = computed(() => {
  if (deviceState.value) {
    // 有无人机实时数据 → 显示精确状态
    return normalizeDroneAircraftMode(droneModeCode.value)
  }
  if (dockState.value) {
    // 有机场数据但无无人机数据 → 无人机未开机
    return '未连接'
  }
  // WS 尚未收到任何数据
  return connected.value || connecting.value ? '连接中...' : '待机'
})

const droneStatusClass = computed(() => {
  if (isDroneOffline(droneModeCode.value)) return 'offline'
  if (isDroneFlying(droneModeCode.value)) return 'online'
  return 'offline' // 待机
})

// 无人机在飞行中且有视频 URL 时用无人机流，否则回退到机场流
const activeVideoUrl = computed(() => {
  if (isDroneActive.value && props.droneVideoUrl) return props.droneVideoUrl
  return props.dockVideoUrl || ''
})

// --- Dock OSD ---
const fmtDockTemperature = computed(() => {
  const v = dockState.value?.temperature
  return v != null ? `${v}°C` : NA
})

const fmtDockWindSpeed = computed(() => {
  const v = dockState.value?.wind_speed
  return v != null ? `${(Number(v) / 10).toFixed(1)} m/s` : NA
})

const RAINFALL_MAP: Record<number, string> = { 0: '无雨', 1: '小雨', 2: '中雨', 3: '大雨' }

const fmtDockRainfall = computed(() => {
  const v = dockState.value?.rainfall
  return v != null ? (RAINFALL_MAP[v] ?? `${v}`) : NA
})

const fmtDockHeight = computed(() => {
  const v = dockState.value?.height
  return v != null ? `${Number(v).toFixed(1)}m` : NA
})

// Extended dock fields — from upstream DJI data, may not always be present
const rawDock = computed(() => dockState.value as Record<string, any> | null)

const fmtDockVoltage = computed(() => {
  const v = rawDock.value?.working_voltage ?? rawDock.value?.electric_supply_voltage
  return v != null ? `${v} V` : NA
})

const fmtDockCurrent = computed(() => {
  const v = rawDock.value?.working_current
  return v != null ? `${v} mA` : NA
})

const fmtDockStorage = computed(() => {
  const s = rawDock.value?.storage
  if (!s || s.total == null) return NA
  const pct = ((s.total - (s.used ?? 0)) / s.total * 100).toFixed(0)
  return `${pct}%`
})

const fmtDockAC = computed(() => {
  const ac = rawDock.value?.air_conditioner ?? rawDock.value?.air_conditioner_state
  if (ac == null) return NA
  if (typeof ac === 'object') return ac.air_conditioner_state === 1 ? '开启' : '关闭'
  return ac === 1 ? '开启' : '关闭'
})

// --- Device OSD ---
const fmtBattery = computed(() => {
  const v = deviceState.value?.battery?.capacity_percent
  return v != null ? `${v} %` : NA
})

const fmtHeight = computed(() => {
  const v = deviceState.value?.height
  return v != null ? `${Number(v).toFixed(2)} m` : NA
})

const fmtGps = computed(() => {
  const raw = deviceState.value as Record<string, any> | null
  const gps = raw?.position_state?.gps_number
  if (gps != null) return String(gps)
  if (!deviceState.value?.longitude || !deviceState.value?.latitude) return NA
  return `${deviceState.value.longitude.toFixed(4)}, ${deviceState.value.latitude.toFixed(4)}`
})

const fmtHomeDistance = computed(() => {
  const raw = deviceState.value as Record<string, any> | null
  if (raw?.home_distance != null) return `${Number(raw.home_distance).toFixed(2)} m`
  const lng = deviceState.value?.longitude
  const lat = deviceState.value?.latitude
  const dLng = dockState.value?.longitude
  const dLat = dockState.value?.latitude
  if (lng == null || lat == null || dLng == null || dLat == null) return NA
  const R = 6371000
  const dLatR = ((dLat - lat) * Math.PI) / 180
  const dLngR = ((dLng - lng) * Math.PI) / 180
  const a = Math.sin(dLatR / 2) ** 2 + Math.cos((lat * Math.PI) / 180) * Math.cos((dLat * Math.PI) / 180) * Math.sin(dLngR / 2) ** 2
  return `${(R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFixed(2)} m`
})

const fmtElevation = computed(() => {
  const v = deviceState.value?.elevation
  return v != null ? `${Number(v).toFixed(2)} m` : NA
})

const fmtHSpeed = computed(() => {
  const v = deviceState.value?.horizontal_speed
  return v != null ? `${Number(v).toFixed(2)} m/s` : NA
})

const fmtVSpeed = computed(() => {
  const v = deviceState.value?.vertical_speed
  return v != null ? `${Number(v).toFixed(2)} m/s` : NA
})

const fmtDroneWindSpeed = computed(() => {
  const raw = deviceState.value as Record<string, any> | null
  const v = raw?.wind_speed
  if (v != null) return `${(Number(v) / 10).toFixed(2)} m/s`
  return NA
})

// --- AMap ---
const mapContainer = ref<HTMLDivElement>()
const mapReady = ref(false)

let AMapLib: any = null
let mapInstance: any = null
let droneMarker: any = null
let dockMarker: any = null

async function initMap() {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }

  AMapLib = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: [] })
  if (!mapContainer.value) return

  const defaultCenter: [number, number] = [props.initialLng || 113.866, props.initialLat || 22.982]

  mapInstance = new AMapLib.Map(mapContainer.value, {
    zoom: 16,
    center: defaultCenter,
    viewMode: '2D',
    layers: [new AMapLib.TileLayer.Satellite(), new AMapLib.TileLayer.RoadNet()]
  })

  mapReady.value = true

  if (props.initialLng && props.initialLat) {
    dockMarker = new AMapLib.Marker({ position: [props.initialLng, props.initialLat], anchor: 'bottom-center' })
    mapInstance.add(dockMarker)
  }

  droneMarker = new AMapLib.Marker({
    position: defaultCenter,
    anchor: 'center-center',
    content: '<div style="width:20px;height:20px;background:#00e5ff;border-radius:50%;border:3px solid #fff;box-shadow:0 0 12px rgba(0,229,255,0.8);"></div>',
    offset: new AMapLib.Pixel(-10, -10)
  })
  mapInstance.add(droneMarker)
}

function destroyMap() {
  if (droneMarker && mapInstance) { mapInstance.remove(droneMarker); droneMarker = null }
  if (dockMarker && mapInstance) { mapInstance.remove(dockMarker); dockMarker = null }
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
  mapReady.value = false
}

watch(() => deviceState.value, (osd) => {
  if (!osd || !mapInstance || !droneMarker) return
  if (osd.longitude && osd.latitude) {
    droneMarker.setPosition([osd.longitude, osd.latitude])
    mapInstance.setCenter([osd.longitude, osd.latitude])
  }
}, { deep: true })

watch(() => dockState.value, (dock) => {
  if (!dock || !mapInstance) return
  if (dock.longitude && dock.latitude) {
    const pos = [dock.longitude, dock.latitude]
    if (dockMarker) { dockMarker.setPosition(pos) }
    else if (AMapLib) { dockMarker = new AMapLib.Marker({ position: pos, anchor: 'bottom-center' }); mapInstance.add(dockMarker) }
  }
}, { deep: true })

watch(() => props.open, async (isOpen) => {
  if (isOpen) {
    updateClock()
    clockTimer = setInterval(updateClock, 1000)
    await new Promise((r) => setTimeout(r, 100))
    await initMap()
  } else {
    destroyMap()
    if (clockTimer) { clearInterval(clockTimer); clockTimer = null }
  }
})

onBeforeUnmount(() => {
  destroyMap()
  if (clockTimer) { clearInterval(clockTimer); clockTimer = null }
})
</script>

<style scoped>
.ds-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  background: #0d1117;
}

/* ---- Main layout: left map | right (video + panels) ---- */
.ds-layout {
  flex: 1;
  display: flex;
  min-height: 0;
}

.ds-map-area {
  width: 45%;
  flex-shrink: 0;
  position: relative;
  border-right: 2px solid #1a2533;
}

.ds-map { width: 100%; height: 100%; }

.ds-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* Video takes remaining space */
.ds-video-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #000;
  min-height: 0;
}

/* Panels at bottom of right column */
.ds-panels {
  display: flex;
  flex-shrink: 0;
  border-top: 2px solid #1a2533;
  background: rgba(13,17,23,0.98);
}

.ds-section-label {
  position: absolute;
  top: 8px;
  left: 10px;
  z-index: 10;
  color: #fff;
  font-size: 13px;
  background: rgba(0,0,0,0.55);
  padding: 4px 12px;
  border-radius: 4px;
}

.ds-back-btn {
  position: absolute;
  bottom: 12px;
  left: 12px;
  z-index: 10;
  background: rgba(0,0,0,0.6);
  color: #8dc5ff;
  border: 1px solid rgba(64,158,255,0.3);
  border-radius: 4px;
  padding: 5px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}
.ds-back-btn:hover { background: rgba(64,158,255,0.2); color: #fff; }

.ds-map-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(13,17,23,0.85);
  color: #8db0d0;
  font-size: 14px;
  pointer-events: none;
}

/* Video */

.ds-video-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 14px;
  background: rgba(13,17,23,0.95);
  border-bottom: 1px solid #1a2533;
  font-size: 12px;
  color: rgba(205,222,248,0.7);
  flex-shrink: 0;
}

.ds-video-topbar__left {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #eef5ff;
  font-size: 13px;
}

.ds-video-topbar__status {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 3px;
}
.ds-video-topbar__status.online { color: #52c41a; }
.ds-video-topbar__status.offline { color: #faad14; }

.ds-video-topbar__right {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: rgba(205,222,248,0.6);
}

.ds-video-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 0;
}

.ds-video-wrap :deep(.webrtc-player-container) {
  width: 100%; height: 100%; aspect-ratio: auto; border-radius: 0;
}
.ds-video-wrap :deep(.webrtc-player-video) {
  width: 100%; height: 100%; object-fit: contain;
}

.ds-video-empty {
  color: rgba(205,222,248,0.3);
  font-size: 14px;
}

/* ---- Status Panels ---- */

.ds-panel {
  flex: 1;
  padding: 12px 20px 14px;
  border-right: 1px solid #1a2533;
}
.ds-panel:last-child { border-right: none; }

.ds-panel__title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #e74c3c;
  letter-spacing: 0.05em;
}
.ds-panel:last-child .ds-panel__title {
  color: #3498db;
}

.ds-panel__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px 12px;
}

.ds-info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.ds-info-icon {
  font-size: 13px;
  flex-shrink: 0;
  width: 16px;
  text-align: center;
}

.ds-info-label {
  font-size: 11px;
  color: rgba(205,222,248,0.5);
  white-space: nowrap;
  flex-shrink: 0;
}

.ds-info-value {
  font-size: 13px;
  font-weight: 500;
  color: #eef5ff;
  margin-left: 4px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .ds-layout { flex-direction: column; }
  .ds-map-area { width: 100%; height: 40%; border-right: none; border-bottom: 2px solid #1a2533; }
  .ds-panels { flex-direction: column; }
  .ds-panel { border-right: none; border-bottom: 1px solid #1a2533; }
  .ds-panel__grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
