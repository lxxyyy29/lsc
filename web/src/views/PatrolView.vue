<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">网格巡查</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">巡查打卡轨迹与记录查看</p>

    <!-- 巡查轨迹地图：比表格直观，选人+时间段直接看巡查路线 -->
    <div class="card" style="margin-top:20px;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;flex-wrap:wrap;gap:8px;">
        <h3 style="font-size:14px;font-weight:600;margin:0;">
          <i class="fas fa-route" style="color:#1890ff;margin-right:6px;"></i>巡查轨迹地图
        </h3>
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
          <select v-model="trackUser" class="filter-select" style="width:140px;">
            <option value="">全部网格员</option>
            <option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</option>
          </select>
          <div style="display:flex;gap:4px;">
            <button v-for="opt in rangeOptions" :key="opt.value" @click="trackRange = opt.value"
              style="height:32px;padding:0 12px;font-size:12px;border-radius:16px;border:1px solid #d1d5db;cursor:pointer;"
              :style="trackRange === opt.value ? 'background:#0284c7;color:#fff;border-color:#0284c7;' : 'background:#fff;color:#6b7280;'">
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
      <div style="position:relative;">
        <div id="patrolTrackMap" style="height:380px;border-radius:10px;"></div>
        <div v-if="hoverInfo.visible" :style="{
          position:'absolute', left:hoverInfo.x+'px', top:hoverInfo.y+'px', transform:'translate(-50%, calc(-100% - 12px))',
          background:'rgba(17,24,39,0.92)', color:'#fff', padding:'8px 12px', borderRadius:'8px',
          fontSize:'12px', pointerEvents:'none', zIndex:50, maxWidth:'260px', boxShadow:'0 4px 16px rgba(0,0,0,0.3)'
        }">
          <div style="font-weight:600;margin-bottom:2px;">{{ hoverInfo.title }}</div>
          <div style="color:#cbd5e1;line-height:1.6;">{{ hoverInfo.content }}</div>
          <div style="color:#93c5fd;margin-top:2px;">{{ hoverInfo.time }}</div>
        </div>
        <p v-if="!trackRecords.length" style="position:absolute;top:12px;left:50%;transform:translateX(-50%);background:rgba(255,255,255,0.9);padding:4px 12px;border-radius:12px;font-size:12px;color:#9ca3af;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
          当前筛选条件下暂无带定位的打卡记录
        </p>
      </div>
      <div style="display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;margin-top:8px;">
        <p style="font-size:12px;color:#9ca3af;margin:0;">▲ 三角形标记为巡查轨迹（颜色区分网格员，绿边起点、红边终点）；● 圆形标记为事件点位；悬停可查看详情</p>
        <div v-if="trackLegend.length" style="display:flex;align-items:center;gap:10px;flex-wrap:wrap;">
          <span style="font-size:11px;color:#6b7280;">网格员：</span>
          <span v-for="item in trackLegend" :key="item.name" style="display:inline-flex;align-items:center;gap:4px;font-size:11px;color:#374151;">
            <span
              :style="{
                width:'0', height:'0',
                borderLeft:'5px solid transparent',
                borderRight:'5px solid transparent',
                borderBottom:'9px solid ' + item.color,
                flexShrink:0
              }"
            ></span>
            {{ item.name }}
          </span>
        </div>
      </div>
    </div>

    <!-- 巡查记录 -->
    <div class="card" style="margin-top:20px;">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">巡查记录</h3>
      <table class="table">
        <thead><tr><th>网格</th><th>巡查员</th><th>类型</th><th>内容</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="r in filteredRecords" :key="r.id" @click="locateRecord(r)"
            style="cursor:pointer;" title="点击在轨迹地图上定位">
            <td>{{ r.gridName || '-' }}</td>
            <td>{{ r.userName || '-' }}</td>
            <td><span class="tag tag-blue">{{ r.patrolType || '日常' }}</span></td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ r.content || '-' }}</td>
            <td style="font-size:12px;color:#6b7280;">{{ r.createdAt || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!filteredRecords.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无巡查记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getPatrolRecords, getGridTree } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'

const records = ref<any[]>([])
const error = ref('')

// ============ 巡查轨迹地图 ============
const trackUser = ref('')
const trackRange = ref<'today' | '7d' | '30d' | 'all'>('7d')
const rangeOptions = [
  { value: 'today' as const, label: '今天' },
  { value: '7d' as const, label: '近7天' },
  { value: '30d' as const, label: '近30天' },
  { value: 'all' as const, label: '全部' }
]
const hoverInfo = reactive({ visible: false, x: 0, y: 0, title: '', content: '', time: '' })
let AMapLib: any = null
let trackMap: any = null
let trackOverlays: any[] = []
// 网格边界多边形（独立管理，不参与轨迹自适应视野）
let gridOverlays: any[] = []

// 有坐标的打卡记录（轨迹数据源）
const geoRecords = computed(() => records.value.filter(r => r.longitude && r.latitude))

// 网格员下拉（从打卡记录中提取去重）
const userList = computed(() => {
  const map = new Map<number, string>()
  for (const r of geoRecords.value) {
    if (r.userId && !map.has(r.userId)) map.set(r.userId, r.userName || `用户${r.userId}`)
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
})

// 网格员专属颜色（按 userId 稳定映射，不同网格员不同颜色）
const WORKER_COLORS = ['#0284c7', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
function workerColor(userId: number | string | null | undefined): string {
  if (userId == null) return '#1890ff'
  const n = typeof userId === 'number' ? userId : Number(userId)
  return WORKER_COLORS[Math.abs(Number.isFinite(n) ? n : 0) % WORKER_COLORS.length]
}
// 当前筛选范围内的网格员颜色图例
const trackLegend = computed(() => {
  const map = new Map<number, string>()
  for (const r of trackRecords.value) {
    if (r.userId != null && !map.has(Number(r.userId))) {
      map.set(Number(r.userId), r.userName || `用户${r.userId}`)
    }
  }
  return Array.from(map.entries()).map(([id, name]) => ({ name, color: workerColor(id) }))
})

// 按人 + 时间范围筛选并按时间正序（连线顺序）
const trackRecords = computed(() => {
  let list = geoRecords.value
  if (trackUser.value) list = list.filter(r => r.userId === Number(trackUser.value))
  if (trackRange.value !== 'all') {
    const days = trackRange.value === 'today' ? 0 : trackRange.value === '7d' ? 6 : 29
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    start.setDate(start.getDate() - days)
    list = list.filter(r => new Date(r.createdAt).getTime() >= start.getTime())
  }
  return [...list].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
})

// 表格与地图同步筛选，避免表格列出一大堆与地图对不上
const filteredRecords = computed(() => {
  let list = records.value
  if (trackUser.value) list = list.filter(r => r.userId === Number(trackUser.value))
  if (trackRange.value !== 'all') {
    const days = trackRange.value === 'today' ? 0 : trackRange.value === '7d' ? 6 : 29
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    start.setDate(start.getDate() - days)
    list = list.filter(r => new Date(r.createdAt).getTime() >= start.getTime())
  }
  return list
})

function clearTrackOverlays() {
  for (const o of trackOverlays) o.setMap(null)
  trackOverlays = []
}

/** 绘制当前筛选结果的轨迹：按网格员分组连线（线色=专属色），逐点画三角形（填充=专属色，描边=起终点语义） */
function drawTrack() {
  if (!AMapLib || !trackMap) return
  clearTrackOverlays()
  hoverInfo.visible = false
  const list = trackRecords.value
  if (!list.length) return

  // 按网格员分组：每人一条独立轨迹线，避免不同人点位被错误连成一线
  const groups = new Map<string | number, any[]>()
  for (const r of list) {
    const key = r.userId ?? 'unknown'
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key)!.push(r)
  }

  for (const [, recs] of groups) {
    const color = workerColor(recs[0].userId)
    // 同一人多打卡点才连线，线色=该网格员专属色
    if (recs.length >= 2) {
      const path = recs.map(r => [Number(r.longitude), Number(r.latitude)])
      trackOverlays.push(new AMapLib.Polyline({
        path, strokeColor: color, strokeWeight: 3, strokeOpacity: 0.85,
        strokeStyle: 'solid', zIndex: 10, map: trackMap
      }))
    }
    // 逐打卡点画三角形标记
    recs.forEach((r, idx) => {
      // 填充色 = 网格员专属色；描边 = 起终点语义（起点绿 / 终点红 / 中间白）
      const fillColor = color
      const strokeColor = idx === 0 ? '#52c41a' : (idx === recs.length - 1 && idx !== 0) ? '#ff4d4f' : '#ffffff'
      const size = idx === 0 || idx === recs.length - 1 ? 14 : 12
      // SVG 三角形：fill/stroke 原生支持，修复原 drop-shadow 描边失效问题
      const svg = `
        <svg width="${size}" height="${size}" viewBox="0 0 12 12"
             style="overflow:visible;cursor:pointer;transition:transform .2s;transform-origin:center;filter:drop-shadow(0 1px 2px rgba(0,0,0,.35));">
          <polygon points="6,1 11,11 1,11" fill="${fillColor}" stroke="${strokeColor}" stroke-width="2" stroke-linejoin="round"/>
        </svg>`
      const marker = new AMapLib.Marker({
        position: [Number(r.longitude), Number(r.latitude)],
        content: svg,
        offset: new AMapLib.Pixel(-size / 2, -size / 2),
        zIndex: 20,
        cursor: 'pointer',
        map: trackMap
      })
      marker.on('mouseover', (e: any) => {
        const el = marker.getContent()
        if (el instanceof SVGElement) el.style.transform = 'scale(1.3)'
        const px = trackMap.lngLatToContainer(e.lnglat)
        hoverInfo.visible = true
        hoverInfo.x = px.getX()
        hoverInfo.y = px.getY()
        hoverInfo.title = `${r.userName || '-'} · ${r.gridName || '-'}`
        hoverInfo.content = r.content || '（无内容）'
        hoverInfo.time = r.createdAt || ''
      })
      marker.on('mouseout', () => {
        const el = marker.getContent()
        if (el instanceof SVGElement) el.style.transform = 'scale(1)'
        hoverInfo.visible = false
      })
      trackOverlays.push(marker)
    })
  }
  // 视野自适应到轨迹范围
  trackMap.setFitView(trackOverlays, false, [60, 60, 60, 60])
}

/** 绘制网格边界：大网格(level2)橙描边、小网格(level3)绿描边，低填充不遮挡轨迹；悬停显示网格名 */
async function drawGridBoundaries() {
  if (!AMapLib || !trackMap) return
  for (const o of gridOverlays) o.setMap(null)
  gridOverlays = []
  try {
    const tree = await getGridTree() || []
    const draw = (nodes: any[]) => {
      for (const grid of nodes) {
        if ((grid.gridLevel === 2 || grid.gridLevel === 3) && grid.roiJson) {
          try {
            const coords = JSON.parse(grid.roiJson)
            if (!Array.isArray(coords) || coords.length < 3) continue
            const isSmall = grid.gridLevel === 3
            const poly = new AMapLib.Polygon({
              path: coords,
              fillColor: isSmall ? '#10b981' : '#f59e0b',
              fillOpacity: isSmall ? 0.10 : 0.12,
              strokeColor: 'rgba(255,255,255,0.85)',
              strokeWeight: isSmall ? 1.5 : 2,
              strokeStyle: 'dashed',
              zIndex: 5,
              bubble: true,
              map: trackMap,
            })
            poly.on('mouseover', (e: any) => {
              poly.setOptions({ fillOpacity: isSmall ? 0.35 : 0.4, strokeWeight: isSmall ? 3 : 4, zIndex: 8 })
              const px = trackMap.lngLatToContainer(e.lnglat)
              hoverInfo.visible = true
              hoverInfo.x = px.getX()
              hoverInfo.y = px.getY()
              hoverInfo.title = grid.gridName || ''
              hoverInfo.content = `网格层级：${grid.gridLevel === 3 ? '小网格' : '大网格'}`
              hoverInfo.time = ''
            })
            poly.on('mousemove', (e: any) => {
              const px = trackMap.lngLatToContainer(e.lnglat)
              hoverInfo.x = px.getX()
              hoverInfo.y = px.getY()
            })
            poly.on('mouseout', () => {
              poly.setOptions({ fillOpacity: isSmall ? 0.10 : 0.12, strokeWeight: isSmall ? 1.5 : 2, zIndex: 5 })
              hoverInfo.visible = false
            })
            gridOverlays.push(poly)
          } catch (e) { /* 单个网格边界解析失败跳过 */ }
        }
        if (grid.children) draw(grid.children)
      }
    }
    draw(tree)
    // 无轨迹数据时让视野覆盖所有网格边界，保证边界可见
    if (gridOverlays.length && !trackRecords.value.length) {
      trackMap.setFitView(gridOverlays, false, [60, 60, 60, 60])
    }
  } catch (e) {
    console.warn('网格边界加载失败:', e)
  }
}

/** 点击表格行：地图定位到该条打卡 */
function locateRecord(r: any) {
  if (!trackMap || !r.longitude || !r.latitude) return
  trackMap.setCenter([Number(r.longitude), Number(r.latitude)])
  trackMap.setZoom(17)
}

async function initTrackMap() {
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    AMapLib = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.Polygon', 'AMap.Polyline', 'AMap.CircleMarker']
    })
    trackMap = new AMapLib.Map('patrolTrackMap', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/normal' })
    // 画网格边界（与轨迹分层，边界在下/轨迹在上）
    await drawGridBoundaries()
    drawTrack()
  } catch (e) {
    console.warn('轨迹地图初始化失败:', e)
  }
}

watch([trackUser, trackRange], () => { drawTrack() })

// 加载巡查记录并初始化轨迹地图
async function loadRecords() {
  error.value = ''
  try {
    records.value = await getPatrolRecords() || []
    // 首次加载完成后初始化轨迹地图（重绘依赖 records，由 watch 无法触发首次绘制）
    if (!trackMap) initTrackMap()
    else drawTrack()
  } catch (e: any) {
    error.value = e?.message || '巡查记录加载失败'
    // 记录加载失败时仍尝试初始化地图（即使无轨迹数据，地图本身应展示）
    if (!trackMap) initTrackMap()
  }
}

onMounted(loadRecords)
</script>