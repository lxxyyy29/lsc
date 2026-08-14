<template>
  <div class="vehicle-page">
    <!-- 顶部统计卡 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ stats.vehicleCount }}</div>
        <div class="stat-label">在管车辆</div>
      </div>
      <div class="stat-card track">
        <div class="stat-num">{{ stats.trackPoints7d }}</div>
        <div class="stat-label">7 天轨迹点</div>
      </div>
      <div class="stat-card enter">
        <div class="stat-num">{{ stats.todayInOut }}</div>
        <div class="stat-label">今日进出</div>
      </div>
      <div class="stat-card inside">
        <div class="stat-num">{{ stats.insideCount }}</div>
        <div class="stat-label">当前在社区内</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="filters">
        <select v-model="rangeDays" @change="onRangeChange" class="filter-select">
          <option :value="1">近 1 天</option>
          <option :value="3">近 3 天</option>
          <option :value="7">近 7 天</option>
        </select>
        <select v-model="recordType" @change="loadRecords(1)" class="filter-select">
          <option value="">全部进出</option>
          <option value="ENTER">进入</option>
          <option value="EXIT">离开</option>
        </select>
        <button class="btn-refresh" @click="reloadAll">刷新</button>
        <button class="btn-demo" @click="generateDemo" :disabled="generating">{{ generating ? '生成中...' : '生成演示数据' }}</button>
      </div>
      <span class="hint">结合视频监控 AI 抓拍，轨迹支持 7 天历史回溯</span>
    </div>

    <div class="main-grid">
      <!-- 左:车辆列表 -->
      <div class="vehicle-panel list-card">
        <div class="panel-title">车辆列表</div>
        <input v-model="keyword" @keyup.enter="loadVehicles" placeholder="搜索车牌号，如 粤S" class="search-input" />
        <div class="vehicle-list">
          <div v-for="v in vehicles" :key="v.vehicle_plate"
               :class="['vehicle-item', selectedPlate === v.vehicle_plate ? 'active' : '']"
               @click="selectVehicle(v)">
            <div class="vehicle-top">
              <span class="plate">{{ v.vehicle_plate }}</span>
              <span :class="['tag', v.inside ? 'tag-inside' : 'tag-outside']">{{ v.inside ? '在社区内' : '已离开' }}</span>
            </div>
            <div class="vehicle-sub">{{ v.last_addr || '—' }} · {{ v.point_count }} 个轨迹点</div>
            <div class="vehicle-sub dim">{{ fmtTime(v.last_at) }}</div>
          </div>
          <p v-if="!vehicles.length" class="empty-tip">暂无车辆轨迹数据</p>
        </div>
      </div>

      <!-- 右:轨迹地图 + 回放 -->
      <div class="map-panel list-card">
        <div class="map-header">
          <div class="panel-title">轨迹回放
            <span v-if="selectedPlate" class="selected-plate">{{ selectedPlate }}</span>
          </div>
          <div class="playback-controls" v-if="trackPoints.length">
            <button class="btn-play" @click="togglePlay">{{ playing ? '⏸ 暂停' : '▶ 播放' }}</button>
            <input type="range" :min="0" :max="trackPoints.length - 1" v-model.number="playIdx" class="play-slider"
                   @input="jumpToPlayIdx" />
            <span class="play-time">{{ playTimeLabel }}</span>
          </div>
        </div>
        <div id="vehicleTrackMap" class="map-canvas"></div>
        <div class="map-legend">
          <span class="legend-item"><i class="dot start"></i>起点</span>
          <span class="legend-item"><i class="dot end"></i>终点</span>
          <span class="legend-item"><i class="dot car"></i>车辆当前位置</span>
          <span class="legend-item"><i class="line-demo"></i>移动轨迹</span>
        </div>
      </div>
    </div>

    <!-- 进出记录 -->
    <div class="list-card">
      <div class="panel-title">车辆进出记录</div>
      <table class="data-table">
        <thead>
          <tr>
            <th>时间</th>
            <th>车牌号</th>
            <th>类型</th>
            <th>抓拍点位</th>
            <th>位置</th>
            <th>速度</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.id">
            <td>{{ fmtTime(r.captured_at) }}</td>
            <td><strong>{{ r.vehicle_plate }}</strong></td>
            <td><span :class="['tag', r.track_type === 'ENTER' ? 'tag-inside' : 'tag-outside']">{{ r.type_name }}</span></td>
            <td>{{ r.camera_name || '—' }}</td>
            <td>{{ r.address || '—' }}</td>
            <td>{{ r.speed != null ? r.speed + ' km/h' : '—' }}</td>
          </tr>
          <tr v-if="!records.length"><td colspan="6" class="empty-tip">暂无进出记录</td></tr>
        </tbody>
      </table>
      <div class="pagination" v-if="recordTotal > 10">
        <button :disabled="recordPage <= 1" @click="loadRecords(recordPage - 1)">上一页</button>
        <span>第 {{ recordPage }} / {{ Math.ceil(recordTotal / 10) }} 页（共 {{ recordTotal }} 条）</span>
        <button :disabled="recordPage >= Math.ceil(recordTotal / 10)" @click="loadRecords(recordPage + 1)">下一页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import http from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'

const stats = ref<any>({ vehicleCount: 0, trackPoints7d: 0, todayInOut: 0, insideCount: 0 })
const vehicles = ref<any[]>([])
const keyword = ref('')
const rangeDays = ref(7)
const selectedPlate = ref('')
const trackPoints = ref<any[]>([])
const records = ref<any[]>([])
const recordType = ref('')
const recordPage = ref(1)
const recordTotal = ref(0)
const generating = ref(false)

let AMapLib: any = null
let mapInstance: any = null
let trackPolyline: any = null
let trackOutline: any = null
let carMarker: any = null
let pointMarkers: any[] = []
let playTimer: any = null
const playing = ref(false)
const playIdx = ref(0)

const playTimeLabel = computed(() => {
  const p = trackPoints.value[playIdx.value]
  return p ? fmtTime(p.captured_at) : '—'
})

function fmtTime(iso: string) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function timeRange(): { start?: string; end?: string } {
  const end = new Date()
  const start = new Date(end.getTime() - rangeDays.value * 24 * 3600 * 1000)
  const fmt = (d: Date) => {
    const p = (n: number) => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  }
  return { start: fmt(start), end: fmt(end) }
}

async function loadStats() {
  try { stats.value = await http.get('/vehicle-tracks/stats') || {} } catch (e) { console.warn('统计加载失败', e) }
}

async function loadVehicles() {
  try {
    const res: any = await http.get('/vehicle-tracks/vehicles', { params: { keyword: keyword.value || undefined } })
    vehicles.value = res || []
  } catch (e) { console.warn('车辆列表加载失败', e) }
}

function clearTrackLayers() {
  if (trackPolyline) { trackPolyline.setMap(null); trackPolyline = null }
  if (trackOutline) { trackOutline.setMap(null); trackOutline = null }
  if (carMarker) { carMarker.setMap(null); carMarker = null }
  for (const m of pointMarkers) m.setMap(null)
  pointMarkers = []
}

async function drawTrack(points: any[]) {
  if (!mapInstance || !AMapLib) return
  clearTrackLayers()
  if (!points.length) return
  const coords = points
    .filter((p) => p.longitude != null && p.latitude != null)
    .map((p) => [Number(p.longitude), Number(p.latitude)])
  if (coords.length < 2) {
    // 单点:仅放置车辆标记
    mapInstance.setCenter(coords[0] || [113.9395, 22.9712])
    carMarker = new AMapLib.Marker({ position: coords[0], map: mapInstance, zIndex: 30 })
    return
  }
  trackOutline = new AMapLib.Polyline({
    path: coords, strokeColor: '#ffffff', strokeWeight: 6, strokeOpacity: 0.9,
    strokeStyle: 'solid', bubble: true, zIndex: 10, map: mapInstance
  })
  trackPolyline = new AMapLib.Polyline({
    path: coords, strokeColor: '#1890ff', strokeWeight: 3, strokeOpacity: 0.9,
    strokeStyle: 'solid', bubble: true, zIndex: 11, map: mapInstance
  })
  const start = new AMapLib.CircleMarker({ center: coords[0], radius: 5, fillColor: '#fff', fillOpacity: 1, strokeColor: '#52c41a', strokeWeight: 2, zIndex: 20, map: mapInstance })
  const end = new AMapLib.CircleMarker({ center: coords[coords.length - 1], radius: 5, fillColor: '#fff', fillOpacity: 1, strokeColor: '#ff4d4f', strokeWeight: 2, zIndex: 20, map: mapInstance })
  pointMarkers.push(start, end)
  carMarker = new AMapLib.Marker({
    position: coords[0],
    content: '<div style="width:14px;height:14px;border-radius:50%;background:#1890ff;border:2px solid #fff;box-shadow:0 1px 4px rgba(0,0,0,.4);"></div>',
    offset: new AMapLib.Pixel(-8, -8),
    zIndex: 30,
    map: mapInstance
  })
  mapInstance.setFitView([trackPolyline], false, [40, 40, 40, 40], 15)
}

async function selectVehicle(v: any) {
  selectedPlate.value = v.vehicle_plate
  playing.value = false
  clearPlayTimer()
  try {
    const { start, end } = timeRange()
    const pts: any[] = await http.get('/vehicle-tracks/trajectory', { params: { plate: v.vehicle_plate, start, end } }) || []
    trackPoints.value = pts
    playIdx.value = 0
    await drawTrack(pts)
    if (pts.length && pts[0].longitude != null) {
      mapInstance.setCenter([Number(pts[0].longitude), Number(pts[0].latitude)])
    }
  } catch (e) { console.warn('轨迹加载失败', e) }
}

function clearPlayTimer() {
  if (playTimer) { clearInterval(playTimer); playTimer = null }
}

function moveCar(idx: number) {
  const p = trackPoints.value[idx]
  if (!p || p.longitude == null || carMarker == null) return
  carMarker.setPosition([Number(p.longitude), Number(p.latitude)])
}

function jumpToPlayIdx() { moveCar(playIdx.value) }

function togglePlay() {
  if (playing.value) {
    playing.value = false
    clearPlayTimer()
    return
  }
  if (!trackPoints.value.length) return
  playing.value = true
  if (playIdx.value >= trackPoints.value.length - 1) { playIdx.value = 0; moveCar(0) }
  playTimer = setInterval(() => {
    if (playIdx.value >= trackPoints.value.length - 1) {
      playing.value = false
      clearPlayTimer()
      return
    }
    playIdx.value += 1
    moveCar(playIdx.value)
  }, 350)
}

async function loadRecords(page = 1) {
  recordPage.value = page
  try {
    const { start, end } = timeRange()
    const res: any = await http.get('/vehicle-tracks/records', {
      params: { type: recordType.value || undefined, page, size: 10, start, end }
    })
    records.value = res?.items || []
    recordTotal.value = res?.total || 0
  } catch (e) { console.warn('进出记录加载失败', e) }
}

function onRangeChange() {
  loadRecords(1)
  if (selectedPlate.value) {
    const v = vehicles.value.find((x) => x.vehicle_plate === selectedPlate.value)
    if (v) selectVehicle(v)
  }
}

async function generateDemo() {
  generating.value = true
  try {
    const res: any = await http.post('/vehicle-tracks/generate-demo')
    alert(`演示数据已生成:${res?.vehicles ?? 10} 辆车,${res?.records ?? 0} 条轨迹记录`)
    await reloadAll()
  } catch (e: any) {
    alert(e?.message || '生成失败')
  } finally {
    generating.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadStats(), loadVehicles()])
  if (selectedPlate.value) {
    const v = vehicles.value.find((x) => x.vehicle_plate === selectedPlate.value)
    if (v) await selectVehicle(v)
  }
  await loadRecords(recordPage.value)
}

onMounted(async () => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Polyline', 'AMap.Marker', 'AMap.CircleMarker']
  })
  mapInstance = new AMapLib.Map('vehicleTrackMap', { zoom: 14, center: [113.9395, 22.9712], mapStyle: 'amap://styles/normal' })
  await Promise.all([loadStats(), loadVehicles(), loadRecords(1)])
  if (vehicles.value.length) await selectVehicle(vehicles.value[0])
})

onBeforeUnmount(() => clearPlayTimer())
</script>

<style scoped>
.vehicle-page { display: flex; flex-direction: column; gap: 14px; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.stat-card { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.stat-card .stat-num { font-size: 26px; font-weight: 700; color: #333; }
.stat-card .stat-label { font-size: 13px; color: #888; margin-top: 2px; }
.stat-card.track .stat-num { color: #1890ff; }
.stat-card.enter .stat-num { color: #f59e0b; }
.stat-card.inside .stat-num { color: #52c41a; }
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 10px; align-items: center; }
.filter-select { padding: 6px 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; }
.btn-refresh, .btn-demo { padding: 6px 14px; border: 1px solid #ddd; border-radius: 6px; background: #fff; font-size: 13px; cursor: pointer; }
.btn-demo { background: #1890ff; color: #fff; border-color: #1890ff; }
.btn-demo:disabled { opacity: 0.6; cursor: not-allowed; }
.hint { font-size: 12px; color: #999; }
.main-grid { display: grid; grid-template-columns: 280px 1fr; gap: 14px; }
.list-card { background: #fff; border-radius: 10px; padding: 14px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.panel-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }
.search-input { width: 100%; box-sizing: border-box; padding: 7px 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 13px; margin-bottom: 8px; }
.vehicle-list { max-height: 460px; overflow-y: auto; }
.vehicle-item { padding: 10px; border: 1px solid #eee; border-radius: 8px; margin-bottom: 8px; cursor: pointer; }
.vehicle-item:hover { border-color: #bae7ff; }
.vehicle-item.active { border-color: #1890ff; background: #e6f7ff; }
.vehicle-top { display: flex; justify-content: space-between; align-items: center; }
.plate { font-weight: 600; font-size: 13px; color: #222; }
.tag { padding: 2px 8px; font-size: 11px; border-radius: 10px; white-space: nowrap; }
.tag-inside { background: #e8f5e9; color: #2e7d32; }
.tag-outside { background: #f5f5f5; color: #888; }
.vehicle-sub { font-size: 12px; color: #666; margin-top: 3px; }
.vehicle-sub.dim { color: #aaa; }
.empty-tip { text-align: center; color: #aaa; font-size: 13px; padding: 20px 0; }
.map-panel { display: flex; flex-direction: column; }
.map-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.selected-plate { color: #1890ff; margin-left: 8px; }
.playback-controls { display: flex; align-items: center; gap: 10px; }
.btn-play { padding: 4px 12px; border: 1px solid #1890ff; border-radius: 6px; background: #e6f7ff; color: #1890ff; font-size: 12px; cursor: pointer; }
.play-slider { width: 160px; }
.play-time { font-size: 12px; color: #666; min-width: 140px; }
.map-canvas { width: 100%; height: 420px; border-radius: 8px; }
.map-legend { display: flex; gap: 16px; margin-top: 8px; font-size: 12px; color: #666; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; }
.dot.start { background: #fff; border: 2px solid #52c41a; }
.dot.end { background: #fff; border: 2px solid #ff4d4f; }
.dot.car { background: #1890ff; border: 2px solid #fff; }
.line-demo { display: inline-block; width: 20px; height: 3px; background: #1890ff; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td { padding: 9px 10px; text-align: left; border-bottom: 1px solid #f0f0f0; }
.data-table th { background: #fafafa; color: #666; font-weight: 600; }
.pagination { display: flex; gap: 12px; align-items: center; justify-content: center; margin-top: 10px; font-size: 13px; color: #666; }
.pagination button { padding: 4px 12px; border: 1px solid #ddd; border-radius: 6px; background: #fff; cursor: pointer; }
.pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
