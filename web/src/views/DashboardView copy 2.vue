<template>
  <!-- 全域态势感知大屏 v2：浅色科技风，聚焦地图，主色 #0284c7 不变 -->
  <div ref="screenRef" class="dash-screen">
    <!-- 背景装饰：浅色渐变 + 细网格线 + 主色柔光 -->
    <div class="bg-decor">
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
    </div>

    <!-- 地图主体（浅色底图） -->
    <div id="gisMap" class="dash-map"></div>
    <!-- 扫描光效（淡主色，不挡交互） -->
    <div class="scan-line"></div>

    <!-- 数据源标识 -->
    <div class="data-badge" :class="isLive ? 'live' : 'demo'">
      <i class="fas fa-circle"></i>{{ isLive ? '实时数据' : '演示数据' }}
    </div>

    <!-- 网格预警色图例 -->
    <div class="grid-legend">
      <span class="gl-title"><i class="fas fa-shield-alt"></i>网格预警</span>
      <span class="gl-item"><i style="background:#0284c7"></i>平稳</span>
      <span class="gl-item"><i style="background:#22c55e"></i>一般</span>
      <span class="gl-item"><i style="background:#f59e0b"></i>重点</span>
      <span class="gl-item"><i style="background:#ef4444"></i>紧急</span>
    </div>

    <!-- ============ 顶部：时钟 | 标题 | KPI + 全屏 ============ -->
    <header class="dash-header">
      <div class="header-left">
        <div class="clock">
          <div class="clock-time">{{ clock.time }}</div>
          <div class="clock-date">{{ clock.date }} <span class="clock-week">{{ week }}</span></div>
        </div>
      </div>

      <div class="header-title">
        <div class="title-line"></div>
        <div class="title-main">
          <h1><i class="fas fa-satellite-dish"></i>智慧网格 · 全域态势感知一张图</h1>
          <p>一屏观全域 · 以图管格 · 以格管人</p>
        </div>
        <div class="title-line"></div>
      </div>

      <div class="header-right">
        <div class="kpi-chip">
          <b>{{ fmtArea }}<em>km²</em></b>
          <span><i class="fas fa-map"></i> 社区面积</span>
        </div>
        <div class="kpi-chip">
          <b>{{ largeGridNum }}</b>
          <span><i class="fas fa-layer-group"></i> 大网格</span>
        </div>
        <div class="kpi-chip">
          <b>{{ smallGridNum }}</b>
          <span><i class="fas fa-th"></i> 小网格</span>
        </div>
        <button class="fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏看地图'">
          <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
        </button>
      </div>
    </header>

    <!-- ============ 左侧面板：事件三色分级 + 近7日趋势 ============ -->
    <aside class="panel-col left">
      <section class="panel">
        <div class="panel-head">
          <span class="panel-title"><i class="fas fa-chart-pie"></i>事件三色分级</span>
          <span class="badge badge-red" v-if="(overview.eventRed || 0) > 0">
            <i class="fas fa-fire"></i> 紧急 {{ overview.eventRed }}
          </span>
        </div>
        <div id="chartRing" class="chart-ring"></div>
        <div class="ring-legend">
          <div class="legend-item" v-for="lv in levelRows" :key="lv.key">
            <span class="legend-dot" :style="{ background: lv.color, boxShadow: `0 0 8px ${lv.color}` }"></span>
            <span class="legend-name">{{ lv.name }}</span>
            <span class="legend-bar"><i :style="{ width: lv.pct + '%', background: lv.color }"></i></span>
            <span class="legend-num">{{ lv.count }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-title"><i class="fas fa-chart-line"></i>近 7 日事件趋势</span>
          <span class="panel-tag">件 / 日</span>
        </div>
        <div id="chartTrend" class="chart-trend"></div>
      </section>
    </aside>

    <!-- ============ 右侧面板：最新事件 + 网格人口排名 ============ -->
    <aside class="panel-col right">
      <section class="panel events-panel">
        <div class="panel-head">
          <span class="panel-title"><i class="fas fa-bolt"></i>最新事件</span>
          <span class="panel-tag">{{ events.length }} 条</span>
        </div>
        <div class="events-list">
          <div v-for="evt in events" :key="evt.id" class="event-item"
               :class="{ active: selectedEvent?.id === evt.id }" @click="focusEvent(evt)">
            <span class="tag" :class="urgencyTag(evt.urgencyLevel)">{{ urgencyText(evt.urgencyLevel) }}</span>
            <div class="event-text">
              <p class="event-title">{{ evt.title }}</p>
              <p class="event-meta">{{ statusLabel(evt.currentStatus) }} · {{ formatTime(evt.createdAt) }}</p>
            </div>
            <i v-if="evt.longitude && evt.latitude" class="fas fa-crosshairs event-locate"></i>
          </div>
          <p v-if="!events.length" class="panel-empty">暂无事件</p>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <span class="panel-title"><i class="fas fa-users"></i>网格人口排名</span>
          <span class="panel-tag">TOP 8</span>
        </div>
        <div id="chartRank" class="chart-rank"></div>
        <p v-if="!hasPopulation" class="panel-empty">暂无人口数据</p>
      </section>
    </aside>

    <!-- ============ 底部跑马灯：实时预警 ============ -->
    <div class="dash-ticker">
      <span class="ticker-label"><i class="fas fa-bullhorn"></i>实时预警</span>
      <div class="ticker-view">
        <div class="ticker-track">
          <span v-for="(t, i) in tickerItems" :key="'a' + i" class="ticker-item">
            <i class="fas fa-circle" :style="{ color: t.color, fontSize: '8px' }"></i>{{ t.text }}
          </span>
          <span v-for="(t, i) in tickerItems" :key="'b' + i" class="ticker-item">
            <i class="fas fa-circle" :style="{ color: t.color, fontSize: '8px' }"></i>{{ t.text }}
          </span>
        </div>
      </div>
    </div>

    <!-- ============ 事件详情悬浮卡片 ============ -->
    <div v-if="selectedEvent" class="event-popup glass-panel">
      <div class="popup-head">
        <span class="tag" :class="urgencyTag(selectedEvent.urgencyLevel)">{{ urgencyText(selectedEvent.urgencyLevel) }}</span>
        <span class="popup-status">{{ statusLabel(selectedEvent.currentStatus) }}</span>
        <button class="popup-close" @click="selectedEvent = null">&times;</button>
      </div>
      <p class="popup-title">{{ selectedEvent.title }}</p>
      <div class="popup-meta">
        <div v-if="selectedEvent.address"><i class="fas fa-map-marker-alt"></i> {{ selectedEvent.address }}</div>
        <div v-if="selectedEvent.createdAt"><i class="fas fa-clock"></i> {{ formatTime(selectedEvent.createdAt) }}</div>
      </div>
      <p v-if="selectedEvent.description" class="popup-desc">{{ selectedEvent.description }}</p>
      <button class="popup-detail-btn" @click="goEventDetail(selectedEvent)">
        <i class="fas fa-arrow-right"></i> 查看详情
      </button>
    </div>

    <!-- ============ 网格选中信息面板 ============ -->
    <div v-if="selectedGrid" class="grid-popup glass-panel">
      <div class="popup-head">
        <span class="popup-grid-name">{{ selectedGrid.gridName }}</span>
        <button class="popup-close" @click="selectedGrid = null">&times;</button>
      </div>
      <div class="popup-meta">
        <div><i class="fas fa-sitemap"></i> {{ selectedGrid.gridLevel === 2 ? '二级网格（大网格）' : selectedGrid.gridLevel === 3 ? '三级网格（小网格）' : '一级网格（社区）' }}</div>
        <div v-if="gridUrgencyOf(selectedGrid)">
          <i class="fas fa-exclamation-triangle" :style="{ color: urgencyColor(gridUrgencyOf(selectedGrid)) }"></i>
          预警：{{ urgencyText(gridUrgencyOf(selectedGrid)) }}
        </div>
        <div><i class="fas fa-user"></i> 负责人：{{ selectedGrid.managerName || '-' }}</div>
        <div>
          <i class="fas fa-circle" :style="{ color: selectedGrid.status === 'ACTIVE' ? '#16a34a' : '#dc2626', fontSize: '8px' }"></i>
          {{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}
          <template v-if="selectedGrid.area"> · {{ selectedGrid.area }} km²</template>
        </div>
      </div>
    </div>

    <!-- 悬停提示框 -->
    <div v-if="hoverInfo.visible" class="hover-tip" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
      {{ hoverInfo.name }}
    </div>

    <!-- 错误提示（悬浮顶部中央） -->
    <div v-if="loadError" class="dash-error">
      <i class="fas fa-exclamation-circle"></i>
      <span>数据加载异常：{{ loadError }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watchEffect, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardOverview, getGridStats, getGridTree, getEvents, getBigScreenData } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import * as echarts from 'echarts'

const router = useRouter()

/* ================================================================
 * 数据说明（预留接口）：
 *  - 概览 KPI（面积/网格数/事件数）：GET /community/dashboard/overview
 *  - 大屏聚合数据（可选，结构对齐后自动覆盖）：GET /community/dashboard/big-screen
 *  - 网格统计（人口排名）：GET /community/dashboard/grid-stats
 *  - 网格树（地图多边形）：GET /community/grids/tree
 *  - 事件列表（地图点位+右侧列表）：GET /events?page=1&size=20
 *  - 近 7 日趋势：可用 GET /events/statistics 或 big-screen 提供，当前内置演示
 *  所有接口失败或为空时自动回退到内置演示数据，保证大屏始终完整可用。
 * ================================================================ */

// ==================== 网格预警色（白边分割 + 透色填充，参考 copy 版） ====================
const GRID_STYLE: Record<string, { fill: string; fillOpacity: number; hoverOpacity: number }> = {
  RED:    { fill: '#ef4444', fillOpacity: 0.18, hoverOpacity: 0.32 },
  YELLOW: { fill: '#f59e0b', fillOpacity: 0.18, hoverOpacity: 0.32 },
  GREEN:  { fill: '#22c55e', fillOpacity: 0.15, hoverOpacity: 0.26 },
  NONE:   { fill: '#0284c7', fillOpacity: 0.10, hoverOpacity: 0.20 }
}

/** 射线法判断点是否在多边形内 */
function pointInPolygon(pt: [number, number], poly: number[][]): boolean {
  let inside = false
  for (let i = 0, j = poly.length - 1; i < poly.length; j = i++) {
    const [xi, yi] = poly[i]
    const [xj, yj] = poly[j]
    const hit = (yi > pt[1]) !== (yj > pt[1]) &&
      pt[0] < ((xj - xi) * (pt[1] - yi)) / (yj - yi) + xi
    if (hit) inside = !inside
  }
  return inside
}

/** 依据网格内事件计算紧急度：RED > YELLOW > GREEN，无事件返回 ''（平稳） */
function computeGridUrgency(coords: number[][], evts: any[]): string {
  let lv = ''
  for (const e of evts) {
    if (e.longitude == null || e.latitude == null) continue
    if (!pointInPolygon([e.longitude, e.latitude], coords)) continue
    if (e.urgencyLevel === 'RED') return 'RED'
    if (e.urgencyLevel === 'YELLOW') lv = 'YELLOW'
    else if (!lv) lv = 'GREEN'
  }
  return lv
}

/** 网格预警级别：优先接口自带字段，否则按网格内事件自动判定 */
function gridUrgencyOf(grid: any): string {
  if (!grid) return ''
  if (grid.urgencyLevel) return grid.urgencyLevel
  try {
    const coords = JSON.parse(grid.roiJson)
    if (Array.isArray(coords) && coords.length >= 3) return computeGridUrgency(coords, events.value)
  } catch (e) { /* 忽略非法坐标 */ }
  return ''
}

// ==================== 演示数据兜底 ====================
const MOCK_CENTER: [number, number] = [113.9395, 22.9712] // 东莞示例坐标

function rectRoi(cx: number, cy: number, w: number, h: number): number[][] {
  return [
    [cx - w, cy - h], [cx + w, cy - h], [cx + w, cy + h], [cx - w, cy + h], [cx - w, cy - h]
  ]
}

/** 生成演示网格树：1 社区 → 3×3 大网格 → 每格 2 个小网格 */
function buildMockTree(): any[] {
  const [cx, cy] = MOCK_CENTER
  const cw = 0.014, ch = 0.02
  const gw = (2 * cw) / 3, gh = (2 * ch) / 3
  const cos = Math.cos(22.97 * Math.PI / 180)
  const area = (w: number, h: number) => +(w * h * 111 * 111 * cos).toFixed(2)
  const bigs: any[] = []
  const pops = [3260, 2840, 2980, 2510, 3420, 2190, 1870, 2630, 1740]
  const demoLv = ['RED', 'GREEN', 'YELLOW', 'YELLOW', 'RED', 'GREEN', 'GREEN', 'YELLOW', 'RED']
  let idx = 1
  for (let r = 0; r < 3; r++) {
    for (let c = 0; c < 3; c++) {
      const bx = cx - cw + gw / 2 + c * gw
      const by = cy - ch + gh / 2 + r * gh
      const sw = gw / 2
      const kids: any[] = []
      for (let k = 0; k < 2; k++) {
        const sx = bx - gw / 2 + sw / 2 + k * sw
        kids.push({
          id: 1000 + idx * 2 + k, gridLevel: 3,
          gridName: `第 ${idx} 网格-${k + 1} 区`,
          area: area(sw, gh),
          roiJson: JSON.stringify(rectRoi(sx, by, sw / 2, gh / 2)),
          managerName: `网格员-${idx}${k + 1}`, status: 'ACTIVE'
        })
      }
      bigs.push({
        id: 900 + idx, gridLevel: 2,
        gridName: `第 ${idx} 网格`, area: area(gw, gh),
        roiJson: JSON.stringify(rectRoi(bx, by, gw / 2, gh / 2)),
        managerName: `网格长-${idx}`, status: 'ACTIVE', populationCount: pops[idx - 1],
        urgencyLevel: demoLv[idx - 1], // 演示用预警级别；真实接口可不传，自动按网格内事件判定
        children: kids
      })
      idx++
    }
  }
  return [{
    id: 1, gridLevel: 1, gridName: '示范社区', area: area(2 * cw, 2 * ch),
    roiJson: JSON.stringify(rectRoi(cx, cy, cw, ch)),
    managerName: '社区书记', status: 'ACTIVE', children: bigs
  }]
}

const MOCK_OVERVIEW = { largeGridCount: 9, smallGridCount: 18, eventTotal: 36, eventRed: 3, eventYellow: 9, eventGreen: 24, communityArea: 3.4 }

function buildMockEvents(): any[] {
  const at = (h: number) => new Date(Date.now() - h * 3600e3).toISOString()
  return [
    { id: 1001, title: '东城路 23 号占道经营影响通行', urgencyLevel: 'YELLOW', currentStatus: 'WAITING_DISPATCH', longitude: 113.9428, latitude: 22.9748, address: '东城路 23 号门口', createdAt: at(0.6), description: '商贩占用机动车道摆摊，造成早高峰拥堵，已现场劝离一次，需网格员跟进复查。' },
    { id: 1002, title: '幸福里小区 3 栋飞线充电', urgencyLevel: 'RED', currentStatus: 'AUDIT_APPROVED', longitude: 113.9352, latitude: 22.9731, address: '幸福里小区 3 栋', createdAt: at(2.1), description: '业主从 6 楼私拉电线为电动车充电，存在火灾隐患，已通知物业并上报。' },
    { id: 1003, title: '中心公园休闲椅损坏', urgencyLevel: 'GREEN', currentStatus: 'CLOSED', longitude: 113.9401, latitude: 22.9679, address: '中心公园东门', createdAt: at(5.5), description: '公园东门两处休闲椅木板断裂，已安排维修更换。' },
    { id: 1004, title: '文明路井盖缺失', urgencyLevel: 'RED', currentStatus: 'PENDING_AUDIT', longitude: 113.9456, latitude: 22.9696, address: '文明路与新风路交叉口', createdAt: at(8.0), description: '雨水井盖破损缺失，夜间通行存在安全隐患，需立即围蔽处理。' },
    { id: 1005, title: '阳光花园垃圾分类点满溢', urgencyLevel: 'YELLOW', currentStatus: 'DISPATCHED_TO_WORK_ORDER', longitude: 113.9378, latitude: 22.9688, address: '阳光花园北门', createdAt: at(12.0), description: '垃圾未及时清运，气味影响周边居民，已派单环卫。' },
    { id: 1006, title: '夜市摊贩噪音扰民投诉', urgencyLevel: 'YELLOW', currentStatus: 'IN_AUDIT', longitude: 113.9431, latitude: 22.9719, address: '滨河夜市', createdAt: at(16.0), description: '夜间摊贩高音喇叭扰民，多名居民通过 12345 反映。' },
    { id: 1007, title: '老旧小区楼道杂物堆积', urgencyLevel: 'GREEN', currentStatus: 'CLOSED', longitude: 113.9382, latitude: 22.9756, address: '新安里小区 2 栋', createdAt: at(22.0), description: '楼道堆放纸箱杂物，已联合物业清理完毕。' },
    { id: 1008, title: '施工工地扬尘污染', urgencyLevel: 'GREEN', currentStatus: 'IGNORED', longitude: 113.9447, latitude: 22.9762, address: '东城路与文明路工地', createdAt: at(30.0), description: '工地未按规定洒水降尘，经核实已安装喷淋设备。' }
  ]
}

function mockTrend() {
  const days: string[] = []
  const base = [4, 6, 5, 8, 7, 9, 6]
  for (let i = 0; i < 7; i++) {
    const d = new Date()
    d.setDate(d.getDate() - (6 - i))
    days.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return { days, values: base }
}

// ==================== 状态 ====================
const overview = ref<any>({})
const events = ref<any[]>([])
const loadError = ref('')
const areaRaw = ref(MOCK_OVERVIEW.communityArea)
const allGrids = ref<any[]>([])
const hasPopulation = ref(false)
const isLive = ref(false)

let mapInstance: any = null
let chartRing: any = null
let chartTrend: any = null
let chartRank: any = null
let hoverId = 0
let clockTimer: number | undefined
let resizeHandler: (() => void) | null = null

const screenRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const clock = reactive({ time: '--:--:--', date: '---- -- --', })
const week = ref('')

const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '', id: 0 })
const selectedGrid = ref<any>(null)
const selectedEvent = ref<any>(null)

// ==================== KPI 数字滚动 ====================
function useCount(target: () => number, duration = 1000) {
  const display = ref(0)
  let raf = 0
  watchEffect(() => {
    const end = Number(target()) || 0
    cancelAnimationFrame(raf)
    const from = display.value
    const t0 = performance.now()
    const step = (t: number) => {
      const p = Math.min((t - t0) / duration, 1)
      const e = 1 - Math.pow(1 - p, 3)
      display.value = Math.round(from + (end - from) * e)
      if (p < 1) raf = requestAnimationFrame(step)
    }
    raf = requestAnimationFrame(step)
  })
  return display
}

const largeGridNum = useCount(() => overview.value.largeGridCount || MOCK_OVERVIEW.largeGridCount)
const smallGridNum = useCount(() => overview.value.smallGridCount || MOCK_OVERVIEW.smallGridCount)
const fmtArea = computed(() => (Number(areaRaw.value) || 0).toFixed(2))

// ==================== 三色分级 ====================
const total = computed(() =>
  (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const pctOf = (n: number) => total.value ? (n / total.value * 100) : 0
const levelRows = computed(() => [
  { key: 'green', name: '一般', color: '#22c55e', count: overview.value.eventGreen || 0, pct: pctOf(overview.value.eventGreen || 0) },
  { key: 'yellow', name: '重点', color: '#f59e0b', count: overview.value.eventYellow || 0, pct: pctOf(overview.value.eventYellow || 0) },
  { key: 'red', name: '紧急', color: '#ef4444', count: overview.value.eventRed || 0, pct: pctOf(overview.value.eventRed || 0) }
])

// ==================== 跑马灯 ====================
const tickerItems = computed(() => {
  if (!events.value.length) return [{ color: '#0284c7', text: '暂无实时预警，全域态势平稳' }]
  return events.value.slice(0, 8).map((e: any) => ({
    color: e.urgencyLevel === 'RED' ? '#ef4444' : e.urgencyLevel === 'YELLOW' ? '#f59e0b' : '#22c55e',
    text: `【${urgencyText(e.urgencyLevel)}】${e.title}`
  }))
})

// ==================== 工具函数 ====================
function urgencyText(lv: string) {
  return lv === 'RED' ? '紧急' : lv === 'YELLOW' ? '重点' : '一般'
}
function urgencyColor(lv: string) {
  return lv === 'RED' ? '#ef4444' : lv === 'YELLOW' ? '#f59e0b' : '#22c55e'
}
function urgencyTag(lv: string) {
  return lv === 'RED' ? 'tag-red' : lv === 'YELLOW' ? 'tag-orange' : 'tag-green'
}
function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核', IN_AUDIT: '审核中', AUDIT_APPROVED: '已通过', AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单', DISPATCHED_TO_WORK_ORDER: '已派单', CLOSED: '已关闭', IGNORED: '已忽略'
  }
  return map[status] || status || '未知'
}
function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}
function goEventDetail(evt: any) {
  router.push('/events/' + (evt.id || evt.externalEventId))
}
function focusEvent(evt: any) {
  selectedEvent.value = evt
  if (mapInstance && evt.longitude && evt.latitude) {
    mapInstance.setCenter([evt.longitude, evt.latitude])
    mapInstance.setZoom(15)
  }
}

// ==================== 全屏 ====================
async function toggleFullscreen() {
  try {
    if (!document.fullscreenElement) {
      await screenRef.value?.requestFullscreen()
    } else {
      await document.exitFullscreen()
    }
  } catch (e) { /* 不支持全屏的环境静默降级 */ }
}
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  setTimeout(() => {
    mapInstance?.resize?.()
    chartRing?.resize?.(); chartTrend?.resize?.(); chartRank?.resize?.()
  }, 120)
}

// ==================== 时钟 ====================
function tickClock() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  clock.time = `${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
  clock.date = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
  week.value = '周' + '日一二三四五六'[d.getDay()]
}

// ==================== 地图 ====================
async function initMap(tree: any[]) {
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker'] })
    mapInstance = new AMap.Map('gisMap', {
      zoom: 14, center: MOCK_CENTER,
      mapStyle: 'amap://styles/normal',
      features: ['bg', 'road', 'building'] // 保留底图要素，网格多边形叠加其上
    })
    const map = mapInstance

    // 社区轮廓（level 1）：主色细描边，不填充（参考 copy 版 strokeWeight 2）
    const drawCommunity = (nodes: any[]) => {
      for (const node of nodes) {
        if (node.gridLevel === 1 && node.roiJson) {
          try {
            const coords = JSON.parse(node.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              new AMap.Polygon({
                path: coords, fillColor: '#0284c7', fillOpacity: 0,
                strokeColor: '#0284c7', strokeWeight: 2, strokeStyle: 'solid',
                strokeOpacity: 0.85, zIndex: 1, bubble: true, map
              })
            }
          } catch (e) {}
        }
        if (node.children) drawCommunity(node.children)
      }
    }

    // 大网格（level 2）：透色填充 + 按紧急程度变色，白色细描边分割（参考 copy 版）
    const drawLarge = (nodes: any[]) => {
      for (const grid of nodes) {
        if (grid.gridLevel === 2 && grid.roiJson) {
          try {
            const coords = JSON.parse(grid.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const lv = grid.urgencyLevel || computeGridUrgency(coords, events.value)
              const st = GRID_STYLE[lv] || GRID_STYLE.NONE
              const lvText = lv ? urgencyText(lv) : '平稳'
              const myId = ++hoverId
              const polygon = new AMap.Polygon({
                path: coords, fillColor: st.fill, fillOpacity: st.fillOpacity,
                strokeColor: '#ffffff', strokeWeight: 2, strokeOpacity: 1,
                zIndex: 5, bubble: false, map
              })
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: st.hoverOpacity, strokeWeight: 2.5, zIndex: 20 })
                const px = map.lngLatToContainer(e.lnglat)
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km² · ${lvText}`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => {
                const px = map.lngLatToContainer(e.lnglat)
                hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
              })
              polygon.on('mouseout', () => {
                if (hoverInfo.id !== myId) return
                polygon.setOptions({ fillOpacity: st.fillOpacity, strokeWeight: 2, zIndex: 5 })
                hoverInfo.visible = false
              })
              polygon.on('click', () => { selectedGrid.value = grid })
            }
          } catch (e) {}
        }
        if (grid.children) drawLarge(grid.children)
      }
    }

    // 小网格（level 3）：淡填充 + 白色细描边分割（参考 copy 版 strokeWeight 1）
    const drawSmall = (nodes: any[]) => {
      for (const grid of nodes) {
        if (grid.gridLevel === 3 && grid.roiJson) {
          try {
            const coords = JSON.parse(grid.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const myId = ++hoverId
              const polygon = new AMap.Polygon({
                path: coords, fillColor: '#0284c7', fillOpacity: 0.06,
                strokeColor: '#ffffff', strokeWeight: 1, strokeOpacity: 1,
                zIndex: 5, bubble: false, map
              })
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: 0.14, strokeWeight: 1.5, zIndex: 20 })
                const px = map.lngLatToContainer(e.lnglat)
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km²`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => {
                const px = map.lngLatToContainer(e.lnglat)
                hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
              })
              polygon.on('mouseout', () => {
                if (hoverInfo.id !== myId) return
                polygon.setOptions({ fillOpacity: 0.06, strokeWeight: 1, zIndex: 5 })
                hoverInfo.visible = false
              })
              polygon.on('click', () => { selectedGrid.value = grid })
            }
          } catch (e) {}
        }
        if (grid.children) drawSmall(grid.children)
      }
    }

    drawCommunity(tree)
    drawLarge(tree)
    drawSmall(tree)

    // 事件标记：呼吸波纹点，点击弹出详情
    for (const evt of events.value) {
      if (!evt.longitude || !evt.latitude) continue
      const color = evt.urgencyLevel === 'RED' ? '#ef4444' : evt.urgencyLevel === 'YELLOW' ? '#f59e0b' : '#22c55e'
      const marker = new AMap.Marker({
        position: [evt.longitude, evt.latitude],
        zIndex: 12,
        content: `<div class="evt-wrap" style="color:${color}">
          <span class="evt-ripple"></span><span class="evt-ripple r2"></span><span class="evt-ripple r3"></span>
          <span class="evt-dot"></span></div>`,
        offset: new AMap.Pixel(-8, -8),
        map,
        extData: evt
      })
      marker.on('mouseover', (e: any) => {
        const px = map.lngLatToContainer(e.lnglat)
        hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
        hoverInfo.name = evt.title || '事件'; hoverInfo.id = ++hoverId
      })
      marker.on('mousemove', (e: any) => {
        const px = map.lngLatToContainer(e.lnglat)
        hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
      })
      marker.on('mouseout', () => { hoverInfo.visible = false })
      marker.on('click', () => { selectedEvent.value = evt })
    }
  } catch (e: any) {
    loadError.value = '地图初始化失败: ' + (e?.message || e)
  }
}

// ==================== 图表 ====================
function renderRing() {
  const el = document.getElementById('chartRing')
  if (!el) return
  if (!chartRing) chartRing = echarts.init(el)
  chartRing.setOption({
    backgroundColor: 'transparent',
    graphic: [
      { type: 'text', left: 'center', top: '36%', style: { text: '事件总数', fill: '#94a3b8', fontSize: 12, textAlign: 'center' } },
      { type: 'text', left: 'center', top: '45%', style: { text: String(total.value), fill: '#0284c7', fontSize: 26, fontWeight: 700, textAlign: 'center' } }
    ],
    series: [{
      type: 'pie', radius: ['60%', '80%'], center: ['50%', '50%'],
      avoidLabelOverlap: false, silent: true,
      itemStyle: { borderRadius: 4, borderColor: '#ffffff', borderWidth: 2 },
      label: { show: false },
      data: [
        { value: overview.value.eventGreen || 0, name: '一般', itemStyle: { color: '#22c55e' } },
        { value: overview.value.eventYellow || 0, name: '重点', itemStyle: { color: '#f59e0b' } },
        { value: overview.value.eventRed || 0, name: '紧急', itemStyle: { color: '#ef4444' } }
      ]
    }]
  })
}

function renderTrend(days: string[], values: number[]) {
  const el = document.getElementById('chartTrend')
  if (!el) return
  if (!chartTrend) chartTrend = echarts.init(el)
  chartTrend.setOption({
    backgroundColor: 'transparent',
    grid: { left: 8, right: 14, top: 16, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155', fontSize: 12 } },
    xAxis: {
      type: 'category', data: days, boundaryGap: false,
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 10 }
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { color: '#0284c7', width: 2.5, shadowColor: 'rgba(2,132,199,0.35)', shadowBlur: 8 },
      itemStyle: { color: '#0284c7', borderColor: '#fff', borderWidth: 1.5 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(2,132,199,0.28)' },
          { offset: 1, color: 'rgba(2,132,199,0.02)' }
        ])
      }
    }]
  })
}

function renderRank(ranking: any[]) {
  const el = document.getElementById('chartRank')
  if (!el) return
  if (!chartRank) chartRank = echarts.init(el)
  const top = ranking.slice(0, 8).reverse()
  chartRank.setOption({
    backgroundColor: 'transparent',
    grid: { left: 70, right: 34, top: 8, bottom: 6 },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#94a3b8', fontSize: 10 },
      splitLine: { lineStyle: { color: '#eef2f7' } }
    },
    yAxis: {
      type: 'category', data: top.map((r: any) => r.gridName),
      axisLabel: { color: '#475569', fontSize: 11 },
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisTick: { show: false }
    },
    series: [{
      type: 'bar', barWidth: 12,
      data: top.map((r: any) => r.populationCount),
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: 'rgba(2,132,199,0.30)' },
          { offset: 1, color: '#0284c7' }
        ]),
        shadowColor: 'rgba(2,132,199,0.25)', shadowBlur: 6
      },
      label: { show: true, position: 'right', color: '#334155', fontSize: 10 }
    }]
  })
}

function renderCharts(stats: any) {
  renderRing()
  const trend = mockTrend()
  renderTrend(trend.days, trend.values)
  if (stats?.populationRanking?.length) {
    hasPopulation.value = true
    renderRank(stats.populationRanking)
  }
}

// ==================== 数据加载 ====================
async function loadData() {
  const errors: string[] = []
  let anyLive = false

  // 概览 KPI
  try {
    const data = await getDashboardOverview()
    if (data && Object.keys(data).length) {
      overview.value = data
      if (data.communityArea) areaRaw.value = Number(data.communityArea)
      anyLive = true
    }
  } catch (e: any) { errors.push('概览: ' + (e?.message || e)) }

  // 大屏聚合数据（可选覆盖，字段与 overview 兼容时生效）
  try {
    const big = await getBigScreenData()
    if (big && typeof big === 'object') {
      overview.value = { ...overview.value, ...big }
      if (big.communityArea) areaRaw.value = Number(big.communityArea)
      anyLive = true
    }
  } catch (e) { /* 可选接口，失败静默 */ }

  // 网格统计（人口排名）
  let stats: any = {}
  try {
    const s = await getGridStats()
    if (s) stats = s
    anyLive = true
  } catch (e: any) { errors.push('网格统计: ' + (e?.message || e)) }

  // 网格树（地图多边形）
  let tree: any[] = []
  try {
    const t = await getGridTree()
    if (Array.isArray(t) && t.length) tree = t
  } catch (e: any) { errors.push('网格树: ' + (e?.message || e)) }
  if (!tree.length) tree = buildMockTree()

  // 事件列表
  try {
    const r = await getEvents()
    if (r?.items?.length) {
      events.value = r.items
      anyLive = true
    }
  } catch (e: any) { errors.push('事件: ' + (e?.message || e)) }
  if (!events.value.length) events.value = buildMockEvents()

  isLive.value = anyLive
  if (errors.length && !anyLive) loadError.value = errors.join('；')

  // 提取网格数据（面积等）
  allGrids.value = []
  const flatten = (nodes: any[]) => {
    for (const n of nodes) {
      allGrids.value.push(n)
      if (n.children) flatten(n.children)
    }
  }
  flatten(tree)
  const community = allGrids.value.find((g: any) => g.gridLevel === 1)
  if (community?.area && !anyLive) areaRaw.value = Number(community.area)

  return { tree, stats }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  resizeHandler = () => { chartRing?.resize?.(); chartTrend?.resize?.(); chartRank?.resize?.() }
  window.addEventListener('resize', resizeHandler)

  const { tree, stats } = await loadData()
  await initMap(tree)
  renderCharts(stats)
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
  ;[chartRing, chartTrend, chartRank].forEach(c => { if (c) { c.dispose(); c = null } })
})
</script>

<style scoped>
/* ================= 大屏容器：地图铺满主内容区 ================= */
.dash-screen {
  position: relative;
  margin: -24px; /* 抵消 main-content padding，地图成为真正主角 */
  height: calc(100vh - 56px);
  overflow: hidden;
  background: linear-gradient(165deg, #eaf2f9 0%, #f5f9fd 45%, #eef4fa 100%);
}
.dash-screen:fullscreen { height: 100vh; }
.dash-map { position: absolute; inset: 0; z-index: 0; width: 100%; height: 100%; }

/* ================= 背景装饰 ================= */
.bg-decor { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(2, 132, 199, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(2, 132, 199, 0.05) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.6), transparent 78%);
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.6), transparent 78%);
}
.bg-glow { position: absolute; border-radius: 50%; filter: blur(90px); }
.bg-glow-1 { width: 520px; height: 520px; background: rgba(2, 132, 199, 0.10); top: -160px; left: 32%; }
.bg-glow-2 { width: 460px; height: 460px; background: rgba(56, 189, 248, 0.10); bottom: -150px; right: 6%; }

/* ================= 扫描光效 ================= */
.scan-line {
  position: absolute; left: 0; right: 0; height: 110px; top: 0; z-index: 8;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(2,132,199,0.04) 40%, rgba(2,132,199,0.14) 78%, transparent);
  border-bottom: 1px solid rgba(2, 132, 199, 0.22);
  animation: scanMove 9s linear infinite;
}
@keyframes scanMove { 0% { top: -120px; } 100% { top: 100%; } }

/* ================= 数据源标识 ================= */
.data-badge {
  position: absolute; z-index: 32; right: 20px; bottom: 52px;
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 11px; padding: 4px 10px; border-radius: 14px;
  background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(8px);
  border: 1px solid rgba(2, 132, 199, 0.18); color: #64748b;
}
.data-badge i { font-size: 8px; }
.data-badge.live i { color: #22c55e; }
.data-badge.demo i { color: #f59e0b; }

/* ================= 网格预警图例 ================= */
.grid-legend {
  position: absolute; z-index: 31; left: 344px; top: 100px;
  display: flex; align-items: center; gap: 10px;
  padding: 6px 12px; border-radius: 8px;
  background: rgba(255, 255, 255, 0.88); backdrop-filter: blur(8px);
  border: 1px solid rgba(2, 132, 199, 0.16);
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
  font-size: 11px; color: #64748b; pointer-events: none;
}
.gl-title { display: inline-flex; align-items: center; gap: 4px; font-weight: 600; color: #075985; }
.gl-title i { color: #0284c7; font-size: 11px; }
.gl-item { display: inline-flex; align-items: center; gap: 4px; }
.gl-item i { width: 9px; height: 9px; border-radius: 50%; display: inline-block; box-shadow: 0 0 4px rgba(15,23,42,0.12); }

/* ================= 顶部 ================= */
.dash-header {
  position: absolute; top: 0; left: 0; right: 0; height: 84px; z-index: 30;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 18px; pointer-events: none;
  background: linear-gradient(180deg, rgba(255,255,255,0.85) 0%, rgba(255,255,255,0.35) 70%, transparent 100%);
}
.header-left, .header-right { display: flex; align-items: center; gap: 10px; width: 360px; }
.header-right { justify-content: flex-end; }
.header-left { justify-content: flex-start; }

.clock { text-align: left; }
.clock-time {
  font-size: 26px; font-weight: 700; color: #0f172a; line-height: 1.1;
  font-variant-numeric: tabular-nums; letter-spacing: 1px;
}
.clock-date { font-size: 11px; color: #64748b; margin-top: 2px; }
.clock-week { color: #0284c7; font-weight: 600; }

.header-title { display: flex; align-items: center; gap: 16px; flex: 1; justify-content: center; }
.title-main { text-align: center; }
.title-main h1 {
  margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 4px;
  background: linear-gradient(90deg, #0284c7, #0ea5e9 50%, #0284c7);
  -webkit-background-clip: text; background-clip: text; color: transparent;
  filter: drop-shadow(0 2px 10px rgba(2, 132, 199, 0.30));
  display: flex; align-items: center; justify-content: center; gap: 10px;
}
.title-main h1 i { color: #0284c7; -webkit-text-fill-color: #0284c7; }
.title-main p { margin: 4px 0 0; font-size: 11px; color: #64748b; letter-spacing: 3px; }
.title-line {
  width: 120px; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(2, 132, 199, 0.55), transparent);
  position: relative;
}
.title-line::after {
  content: ''; position: absolute; top: -3px; width: 8px; height: 8px;
  background: #0284c7; transform: rotate(45deg); opacity: 0.75;
}
.header-title .title-line:first-child::after { right: 0; }
.header-title .title-line:last-child::after { left: 0; }

.kpi-chip {
  pointer-events: auto;
  min-width: 96px; text-align: center; padding: 6px 12px;
  background: rgba(255, 255, 255, 0.88); backdrop-filter: blur(8px);
  border: 1px solid rgba(2, 132, 199, 0.20); border-radius: 10px;
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.06);
}
.kpi-chip b { display: block; font-size: 20px; color: #0284c7; font-weight: 700; line-height: 1.15; }
.kpi-chip b em { font-size: 11px; color: #64748b; font-style: normal; margin-left: 2px; font-weight: 400; }
.kpi-chip span { display: block; font-size: 11px; color: #64748b; margin-top: 2px; }
.kpi-chip span i { color: #0284c7; margin-right: 3px; font-size: 10px; }

.fullscreen-btn {
  pointer-events: auto; flex-shrink: 0;
  width: 38px; height: 38px; display: flex; align-items: center; justify-content: center;
  background: rgba(255, 255, 255, 0.88); backdrop-filter: blur(8px);
  border: 1px solid rgba(2, 132, 199, 0.30); border-radius: 10px;
  color: #0284c7; font-size: 14px; cursor: pointer; transition: all 0.2s;
}
.fullscreen-btn:hover { background: #0284c7; color: #fff; }

/* ================= 玻璃面板通用 ================= */
.glass-panel {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(2, 132, 199, 0.14);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.08);
  color: #334155;
}
.panel-empty { font-size: 12px; color: #94a3b8; text-align: center; padding: 18px 0; margin: 0; }

/* ================= 左右面板列 ================= */
.panel-col {
  position: absolute; top: 96px; bottom: 52px; width: 316px; z-index: 20;
  display: flex; flex-direction: column; gap: 12px;
}
.panel-col.left { left: 14px; }
.panel-col.right { right: 14px; }

.panel {
  position: relative; flex: 1; min-height: 0; overflow: hidden;
  background: rgba(255, 255, 255, 0.90);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(2, 132, 199, 0.14);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.08);
  padding: 12px 14px;
}
/* 四角科技装饰 */
.panel::before, .panel::after {
  content: ''; position: absolute; width: 16px; height: 16px; pointer-events: none; z-index: 1;
}
.panel::before { top: -1px; left: -1px; border-top: 2px solid #0284c7; border-left: 2px solid #0284c7; border-top-left-radius: 12px; }
.panel::after { bottom: -1px; right: -1px; border-bottom: 2px solid #0284c7; border-right: 2px solid #0284c7; border-bottom-right-radius: 12px; }

.panel-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.panel-title {
  display: flex; align-items: center; gap: 7px;
  font-size: 13px; font-weight: 600; color: #075985;
  padding-left: 9px; border-left: 3px solid #0284c7; line-height: 1.2;
}
.panel-title i { color: #0284c7; }
.panel-tag { font-size: 11px; color: #94a3b8; }
.badge-red {
  display: inline-flex; align-items: center; gap: 4px;
  background: #fef2f2; color: #dc2626; border: 1px solid #fecaca;
  font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600;
}
.badge-red i { font-size: 10px; }

/* ================= 环形图 + 图例 ================= */
.chart-ring { height: 150px; margin-top: -4px; }
.ring-legend { display: flex; flex-direction: column; gap: 8px; margin-top: 4px; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.legend-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.legend-name { width: 34px; color: #475569; flex-shrink: 0; }
.legend-bar { flex: 1; height: 7px; background: #eef2f7; border-radius: 4px; overflow: hidden; }
.legend-bar i { display: block; height: 100%; border-radius: 4px; transition: width 0.5s; }
.legend-num { width: 26px; text-align: right; font-weight: 600; color: #0f172a; }

/* ================= 趋势图 ================= */
.chart-trend { height: calc(100% - 26px); }

/* ================= 事件列表 ================= */
.events-list { flex: 1; overflow-y: auto; margin: 0 -6px; padding: 2px 6px; min-height: 0; }
.events-list::-webkit-scrollbar { width: 4px; }
.events-list::-webkit-scrollbar-thumb { background: rgba(2, 132, 199, 0.25); border-radius: 2px; }
.event-item {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 8px; border-radius: 8px; cursor: pointer; transition: background 0.15s;
  border-bottom: 1px solid #f1f5f9;
}
.event-item:hover { background: rgba(2, 132, 199, 0.06); }
.event-item.active { background: rgba(2, 132, 199, 0.12); }
.event-text { flex: 1; min-width: 0; }
.event-title {
  margin: 0; font-size: 12.5px; color: #0f172a;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.event-meta { margin: 2px 0 0; font-size: 10.5px; color: #94a3b8; }
.event-locate { color: rgba(2, 132, 199, 0.7); font-size: 11px; flex-shrink: 0; }

/* ================= 人口排名 ================= */
.chart-rank { height: calc(100% - 26px); }

/* ================= 底部跑马灯 ================= */
.dash-ticker {
  position: absolute; left: 0; right: 0; bottom: 0; height: 40px; z-index: 22;
  display: flex; align-items: center;
  background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(8px);
  border-top: 1px solid rgba(2, 132, 199, 0.16);
}
.ticker-label {
  flex-shrink: 0; margin: 0 12px; padding: 4px 12px;
  background: linear-gradient(90deg, #0284c7, #0ea5e9);
  color: #fff; border-radius: 6px; font-size: 12px; font-weight: 600; letter-spacing: 1px;
  display: inline-flex; align-items: center; gap: 6px;
}
.ticker-view { flex: 1; overflow: hidden; height: 100%; position: relative; }
.ticker-track {
  display: inline-flex; align-items: center; height: 100%; white-space: nowrap;
  animation: tickerScroll 30s linear infinite;
}
.ticker-track:hover { animation-play-state: paused; }
@keyframes tickerScroll { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }
.ticker-item {
  display: inline-flex; align-items: center; gap: 6px;
  margin-right: 44px; font-size: 12px; color: #475569; white-space: nowrap;
}

/* ================= 事件详情悬浮卡片 ================= */
.event-popup {
  position: absolute; z-index: 40; left: 50%; bottom: 56px; transform: translateX(-50%);
  width: 430px; max-width: calc(100% - 32px); padding: 14px 18px;
  animation: popupIn 0.2s ease-out;
}
@keyframes popupIn { from { opacity: 0; transform: translate(-50%, 12px); } to { opacity: 1; transform: translate(-50%, 0); } }
.popup-head { display: flex; align-items: center; gap: 8px; }
.popup-status { font-size: 11px; color: #64748b; }
.popup-close {
  margin-left: auto; border: none; background: none; color: #94a3b8;
  font-size: 18px; cursor: pointer; padding: 0 4px; line-height: 1;
}
.popup-close:hover { color: #0f172a; }
.popup-title { margin: 8px 0; font-size: 15px; font-weight: 600; color: #0f172a; }
.popup-meta { font-size: 12px; color: #475569; display: flex; flex-direction: column; gap: 4px; }
.popup-meta i { width: 14px; color: #0284c7; }
.popup-desc {
  margin: 8px 0 0; font-size: 12px; color: #64748b; line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.popup-detail-btn {
  margin-top: 12px; width: 100%; padding: 8px 0; border: none; border-radius: 8px;
  background: #0284c7; color: #fff; font-size: 13px; cursor: pointer; transition: background 0.2s;
}
.popup-detail-btn:hover { background: #0369a1; }

/* ================= 网格选中面板 ================= */
.grid-popup {
  position: absolute; z-index: 35; top: 96px; left: 50%; transform: translateX(-50%);
  padding: 12px 18px; min-width: 250px;
}
.popup-grid-name { font-size: 14px; font-weight: 700; color: #0f172a; }

/* ================= 悬停提示 ================= */
.hover-tip {
  position: absolute; z-index: 45; transform: translate(-50%, calc(-100% - 14px));
  background: rgba(15, 23, 42, 0.88); color: #fff; padding: 6px 12px; border-radius: 6px;
  font-size: 12px; font-weight: 600; pointer-events: none; white-space: nowrap;
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.25);
}

/* ================= 错误提示 ================= */
.dash-error {
  position: absolute; z-index: 50; top: 92px; left: 50%; transform: translateX(-50%);
  background: rgba(254, 226, 226, 0.94); backdrop-filter: blur(8px);
  border: 1px solid rgba(220, 38, 38, 0.25); border-radius: 8px;
  color: #b91c1c; font-size: 12px; padding: 8px 16px; max-width: 70%;
  display: flex; align-items: center; gap: 8px;
}

.event-popup .tag, .event-item .tag { flex-shrink: 0; }
</style>

<style>
/* 地图事件点：呼吸波纹（marker content 为动态插入，需非 scoped 样式） */
.evt-wrap { position: relative; width: 16px; height: 16px; cursor: pointer; }
.evt-dot {
  position: absolute; left: 50%; top: 50%; width: 11px; height: 11px;
  margin: -5.5px 0 0 -5.5px; border-radius: 50%; z-index: 2;
  border: 2px solid #fff;
  box-shadow: 0 1px 5px rgba(15, 23, 42, 0.35);
}
.evt-ripple {
  position: absolute; left: 50%; top: 50%; width: 11px; height: 11px;
  margin: -5.5px 0 0 -5.5px; border-radius: 50%;
  background: currentColor; opacity: 0.35;
  animation: evtPing 1.9s ease-out infinite;
}
.evt-ripple.r2 { animation-delay: 0.6s; }
.evt-ripple.r3 { animation-delay: 1.2s; }
@keyframes evtPing {
  0% { transform: scale(1); opacity: 0.40; }
  100% { transform: scale(4.4); opacity: 0; }
}
</style>
