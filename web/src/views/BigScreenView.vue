<template>
  <!-- 大屏容器：地图为绝对主角，所有统计面板悬浮在地图上（负 margin 抵消主内容区 padding） -->
  <div ref="screenRef" class="bs-screen">
    <!-- 地图主体 -->
    <div id="bsMap" class="bs-map"></div>

    <!-- 加载中 -->
    <div v-if="loading" class="bs-loading">
      <i class="fas fa-spinner fa-spin"></i> 数据加载中...
    </div>

    <template v-else>
      <!-- 顶部悬浮栏：标题 + 时间 + 全屏按钮 -->
      <div class="bs-topbar glass-panel">
        <h1><i class="fas fa-chart-line"></i> 网格社区综合监管大屏</h1>
        <span class="bs-time">{{ currentTime }}</span>
        <button class="bs-fullscreen-btn" @click="mapReset" title="回到中心位置">
          <i class="fas fa-compass"></i>
          回到中心
        </button>
        <button class="bs-fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏看地图'">
          <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </button>
      </div>

      <!-- KPI 悬浮指标条（点击触发对应分类的实时事件流） -->
      <div class="bs-kpis">
        <div class="kpi glass-panel kpi-blue" :class="{ 'kpi-active': streamCategory === 'all' }" @click="toggleStream('all')">
          <div class="kpi-icon"><i class="fas fa-exclamation-circle"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventTotal || 0 }}</p>
            <p class="kpi-label">事件总数</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-green" :class="{ 'kpi-active': streamCategory === 'today' }" @click="toggleStream('today')">
          <div class="kpi-icon"><i class="fas fa-calendar-day"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventToday || 0 }}</p>
            <p class="kpi-label">今日新增</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-orange" :class="{ 'kpi-active': streamCategory === 'pending' }" @click="toggleStream('pending')">
          <div class="kpi-icon"><i class="fas fa-clock"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventPending || 0 }}</p>
            <p class="kpi-label">待处置</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-purple" :class="{ 'kpi-active': streamCategory === 'closed' }" @click="toggleStream('closed')">
          <div class="kpi-icon"><i class="fas fa-check-circle"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventClosed || 0 }}</p>
            <p class="kpi-label">已处置</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-cyan" :class="{ 'kpi-active': streamCategory === 'woAll' }" @click="toggleStream('woAll')">
          <div class="kpi-icon"><i class="fas fa-clipboard-list"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.workOrderTotal || 0 }}</p>
            <p class="kpi-label">工单总数</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-red" :class="{ 'kpi-active': streamCategory === 'woProc' }" @click="toggleStream('woProc')">
          <div class="kpi-icon"><i class="fas fa-user-clock"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.workOrderProcessing || 0 }}</p>
            <p class="kpi-label">处置中</p>
          </div>
        </div>
      </div>

      <!-- 实时事件流（点击顶部 KPI 触发，悬浮于 KPI 下方；无收起按钮） -->
      <transition name="stream-fade">
        <div v-if="streamOpen" class="bs-stream glass-panel">
          <div class="stream-head">
            <span class="stream-title"><i class="fas fa-bolt"></i>{{ streamCatMeta[streamCategory]?.label }}</span>
            <span class="stream-count">{{ streamItems.length }} 条</span>
            <button class="stream-close" @click="closeStream" title="关闭">&times;</button>
          </div>
          <div class="stream-list">
            <div v-for="it in streamItems" :key="it.kind + '-' + (it.id ?? it.workOrderNo)" class="st-item"
                 :class="{ clickable: it.kind === 'event' && it.longitude && it.latitude }" @click="streamItemClick(it)">
              <span class="st-dot" :style="{ background: streamColor(it), boxShadow: `0 0 0 3px ${streamColorBg(it)}` }"></span>
              <div class="st-card">
                <div class="st-top">
                  <span class="st-tag" :style="{ background: streamColorBg(it), color: streamColor(it) }">
                    {{ it.kind === 'wo' ? '工单' : urgencyText(it.urgencyLevel) }}
                  </span>
                  <span class="st-status"><i class="fas" :class="streamStatusIcon(it)"></i> {{ streamStatusText(it) }}</span>
                  <span class="st-time">{{ formatTime(it.createdAt) }}</span>
                </div>
                <p class="st-title">{{ streamTitle(it) }}</p>
              </div>
            </div>
            <p v-if="!streamItems.length" class="panel-empty">无匹配数据</p>
          </div>
        </div>
      </transition>

      <!-- 左列悬浮面板：紧急程度 + 事件类型 TOP8 + 最新工单 -->
      <div class="bs-col bs-left">
        <div class="glass-panel chart-card">
          <h3 class="chart-title"><i class="fas fa-signal"></i> 紧急程度分布</h3>
          <div v-for="item in data.urgencyDist" :key="item.level" class="urgency-bar-row">
            <span class="urgency-label" :class="'urgency-' + (item.level || '').toLowerCase()">
              {{ urgencyLabel(item.level) }}
            </span>
            <div class="urgency-bar-track">
              <div class="urgency-bar-fill" :class="'bar-' + (item.level || '').toLowerCase()"
                   :style="{ width: getUrgencyWidth(item.count) + '%' }"></div>
            </div>
            <span class="urgency-count">{{ item.count }}</span>
          </div>
          <p v-if="!data.urgencyDist?.length" class="panel-empty">暂无数据</p>
        </div>

        <div class="glass-panel chart-card">
          <h3 class="chart-title"><i class="fas fa-list-ol"></i> 事件类型 TOP8</h3>
          <div v-for="(item, idx) in data.eventTypeDist" :key="item.type" class="type-bar-row">
            <span class="type-rank" :class="{ 'top3': idx < 3 }">{{ idx + 1 }}</span>
            <span class="type-label">{{ getEventTypeName(item.type) }}</span>
            <div class="type-bar-track">
              <div class="type-bar-fill" :style="{ width: getTypeWidth(item.count) + '%' }"></div>
            </div>
            <span class="type-count">{{ item.count }}</span>
          </div>
          <p v-if="!data.eventTypeDist?.length" class="panel-empty">暂无数据</p>
        </div>

        <div class="glass-panel chart-card">
          <h3 class="chart-title"><i class="fas fa-tasks"></i> 最新工单动态</h3>
          <div class="scroll-area">
            <div v-for="item in data.recentWorkOrders" :key="item.workOrderNo" class="wo-row">
              <span class="wo-no" :title="item.title || item.workOrderNo">{{ item.title || item.workOrderNo }}</span>
              <span v-if="isActiveOrder(item.status)" class="wo-dot" title="处理中"></span>
              <span class="wo-status" :class="'status-' + (item.status || '').toLowerCase()">{{ workOrderStatusLabel(item.status) }}</span>
              <span class="wo-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <p v-if="!data.recentWorkOrders?.length" class="panel-empty">暂无工单</p>
          </div>
        </div>
      </div>

      <!-- 右列悬浮面板：7 天趋势 + 网格事件排名 -->
      <div class="bs-col bs-right">
        <div class="glass-panel chart-card">
          <h3 class="chart-title"><i class="fas fa-chart-bar"></i> 近 7 天事件趋势</h3>
          <div class="trend-bars">
            <div v-for="item in data.weeklyTrend" :key="item.date" class="trend-bar-col">
              <div class="trend-bar" :style="{ height: getTrendHeight(item.count) + '%' }">
                <span class="trend-value">{{ item.count }}</span>
              </div>
              <span class="trend-date">{{ formatTrendDate(item.date) }}</span>
            </div>
          </div>
          <p v-if="!data.weeklyTrend?.length" class="panel-empty">暂无数据</p>
        </div>

        <div class="glass-panel chart-card">
          <h3 class="chart-title"><i class="fas fa-trophy"></i> 各网格事件排名</h3>
          <div class="scroll-area">
            <div v-for="(item, idx) in data.gridRanking" :key="item.gridName" class="rank-row">
              <span class="rank-num" :class="{ 'top3': idx < 3 }">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.gridName || '未知' }}</span>
              <div class="rank-track">
                <div class="rank-fill" :style="{ width: getRankWidth(item.count) + '%' }"></div>
              </div>
              <span class="rank-count">{{ item.count }}</span>
            </div>
            <p v-if="!data.gridRanking?.length" class="panel-empty">暂无数据</p>
          </div>
        </div>
      </div>

      <!-- 事件详情悬浮卡片（点击地图事件点后弹出） -->
      <div v-if="selectedEvent" class="event-popup glass-panel">
        <div class="popup-head">
          <span class="popup-tag" :class="selectedEvent.urgencyLevel === 'RED' ? 'tag-red' : selectedEvent.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green'">
            {{ selectedEvent.urgencyLevel === 'RED' ? '紧急' : selectedEvent.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
          </span>
          <span class="popup-status">{{ eventStatusLabel(selectedEvent.currentStatus) }}</span>
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

      <!-- 悬停提示框 -->
      <div v-if="hoverInfo.visible" class="hover-tip" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
        {{ hoverInfo.name }}
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBigScreenData, getGridTree, getEvents, getWorkOrders } from '../api'
import http from '../api'
import { getEventTypeName } from '../utils/eventTypes'
import AMapLoader from '@amap/amap-jsapi-loader'

const router = useRouter()

const loading = ref(true)
const data = ref<any>({})
const currentTime = ref('')
const isFullscreen = ref(false)
const screenRef = ref<HTMLElement | null>(null)
const selectedEvent = ref<any>(null)
const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '' })

// ==================== 实时事件流（点击顶部 KPI 触发） ====================
const streamOpen = ref(false)
const streamCategory = ref<'all' | 'today' | 'pending' | 'closed' | 'woAll' | 'woProc'>('all')
const liveEvents = ref<any[]>([])
const liveWorkOrders = ref<any[]>([])

const streamCatMeta: Record<string, { label: string }> = {
  all: { label: '全部事件' },
  today: { label: '今日新增事件' },
  pending: { label: '待处置事件' },
  closed: { label: '已处置事件' },
  woAll: { label: '全部工单' },
  woProc: { label: '处理中工单' }
}

function urgencyText(lv: string) {
  return lv === 'RED' ? '紧急' : lv === 'YELLOW' ? '重点' : '一般'
}
function urgencyColor(lv: string) {
  return lv === 'RED' ? '#ef4444' : lv === 'YELLOW' ? '#f59e0b' : '#22c55e'
}
function urgencyBg(lv: string) {
  return lv === 'RED' ? 'rgba(239,68,68,0.12)' : lv === 'YELLOW' ? 'rgba(245,158,11,0.12)' : 'rgba(34,197,94,0.12)'
}
function statusIcon(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: 'fa-hourglass-half', IN_AUDIT: 'fa-spinner', AUDIT_APPROVED: 'fa-check-circle',
    AUDIT_REJECTED: 'fa-times-circle', WAITING_DISPATCH: 'fa-paper-plane', WAITING_LEADER_REVIEW: 'fa-user-shield',
    DISPATCHED_TO_WORK_ORDER: 'fa-tools', CLOSED: 'fa-check-double', IGNORED: 'fa-eye-slash'
  }
  return map[status] || 'fa-info-circle'
}

// 点击 KPI：同分类再次点击关闭，否则切换并打开
function toggleStream(cat: typeof streamCategory.value) {
  if (streamOpen.value && streamCategory.value === cat) {
    streamOpen.value = false
  } else {
    streamCategory.value = cat
    streamOpen.value = true
    loadStreamData()
  }
}
function closeStream() { streamOpen.value = false }

// 拉取事件 + 工单并打 kind 标记，供事件流筛选（随大屏 60s 轮询刷新）
async function loadStreamData() {
  try {
    const ev: any = await getEvents({ excludeHidden: true, size: 200 })
    liveEvents.value = (ev?.items || []).filter((e: any) => !e.archived).map((e: any) => ({ ...e, kind: 'event' }))
  } catch (e) {}
  try {
    const wo: any = await getWorkOrders({ pageSize: 100 })
    const list = wo?.items || wo?.list || (Array.isArray(wo) ? wo : [])
    // 工单列表接口不返回名称，从「最新工单动态」按编号匹配标题，避免展示工单 id
    const nameMap = new Map<string, string>()
    ;(data.value?.recentWorkOrders || []).forEach((r: any) => {
      if (r.workOrderNo) nameMap.set(String(r.workOrderNo), r.title)
      if (r.id != null) nameMap.set(String(r.id), r.title)
    })
    liveWorkOrders.value = list.map((w: any) => {
      const key = String(w.workOrderNo ?? w.id)
      return { ...w, kind: 'wo', title: w.title || (key && nameMap.get(key)) || '' }
    })
  } catch (e) {}
}

const streamItems = computed(() => {
  const cat = streamCategory.value
  const isWo = cat === 'woAll' || cat === 'woProc'
  const base = isWo ? liveWorkOrders.value : liveEvents.value
  if (cat === 'all' || cat === 'woAll') return base
  if (cat === 'today') {
    const t = new Date(); t.setHours(0, 0, 0, 0)
    return base.filter((e: any) => new Date(e.createdAt).getTime() >= t.getTime())
  }
  if (cat === 'pending') {
    // 待处置 = 仍需处理；已关闭/已忽略/已驳回 均属终态不处理，排除
    const done = new Set(['CLOSED', 'IGNORED', 'AUDIT_REJECTED'])
    return base.filter((e: any) => !done.has(e.currentStatus))
  }
  if (cat === 'closed') return base.filter((e: any) => e.currentStatus === 'CLOSED')
  if (cat === 'woProc') return base.filter((w: any) => w.status === 'PROCESSING' || w.status === 'WAITING_ACCEPT')
  return base
})

function streamColor(it: any) {
  if (it.kind === 'wo') {
    return it.status === 'PROCESSING' || it.status === 'WAITING_ACCEPT' ? '#0284c7'
      : it.status === 'COMPLETED' || it.status === 'CLOSED' ? '#059669' : '#7c3aed'
  }
  return urgencyColor(it.urgencyLevel)
}
function streamColorBg(it: any) {
  if (it.kind === 'wo') {
    const c = streamColor(it)
    return c === '#0284c7' ? 'rgba(2,132,199,0.12)' : c === '#059669' ? 'rgba(5,150,105,0.12)' : 'rgba(124,58,237,0.12)'
  }
  return urgencyBg(it.urgencyLevel)
}
const woIconMap: Record<string, string> = {
  WAITING_ACCEPT: 'fa-hourglass-half', PROCESSING: 'fa-spinner',
  WAITING_CLOSE_CONFIRM: 'fa-user-check', COMPLETED: 'fa-check-circle', CLOSED: 'fa-check-double'
}
function streamStatusIcon(it: any) {
  return it.kind === 'wo' ? (woIconMap[it.status] || 'fa-info-circle') : statusIcon(it.currentStatus)
}
function streamStatusText(it: any) {
  return it.kind === 'wo' ? workOrderStatusLabel(it.status) : eventStatusLabel(it.currentStatus)
}
function streamTitle(it: any) {
  return it.title || it.workOrderNo || '未命名'
}
function streamItemClick(it: any) {
  if (it.kind === 'event' && it.longitude && it.latitude) {
    selectedEvent.value = it
    mapInstance?.setCenter([it.longitude, it.latitude])
  }
}

let timer: number | undefined
let mapInstance: any = null
// 地图固定中心点（系统配置，默认拔蛟窝社区坐标）
let mapCenter: [number, number] = [113.939521, 22.971231]
// 记录已绘制的网格多边形与事件标记，重绘前先清除以免轮询重复堆叠
let gridOverlays: any[] = []
let eventMarkers: any[] = []

async function loadData() {
  try {
    data.value = await getBigScreenData() || {}
  } catch (e) {
  } finally {
    loading.value = false
  }
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false
  })
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = { GREEN: '一般', YELLOW: '较重', RED: '紧急' }
  return map[level] || level || '未知'
}

function eventStatusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核', IN_AUDIT: '审核中', AUDIT_APPROVED: '已通过', AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单', WAITING_LEADER_REVIEW: '组长审核', DISPATCHED_TO_WORK_ORDER: '已派单', CLOSED: '已关闭', IGNORED: '已忽略'
  }
  return map[status] || status || '未知'
}

function workOrderStatusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ACCEPT: '待接单', PROCESSING: '处理中',
    WAITING_CLOSE_CONFIRM: '待确认', COMPLETED: '已完成', CLOSED: '已关闭'
  }
  return map[status] || status || '未知'
}

// 红点仅标注未派单（待接单）与进行中（处理中）的工单
function isActiveOrder(status: string) {
  return status === 'WAITING_ACCEPT' || status === 'PROCESSING'
}

function formatTrendDate(date: string) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function formatTime(time: any) {
  if (!time) return ''
  try {
    const d = new Date(time)
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return time }
}

function getUrgencyWidth(count: number) {
  const max = Math.max(...(data.value.urgencyDist || []).map((i: any) => i.count || 0), 1)
  return Math.max(5, (count / max) * 100)
}

function getTypeWidth(count: number) {
  const max = Math.max(...(data.value.eventTypeDist || []).map((i: any) => i.count || 0), 1)
  return Math.max(5, (count / max) * 100)
}

function getTrendHeight(count: number) {
  const max = Math.max(...(data.value.weeklyTrend || []).map((i: any) => i.count || 0), 1)
  return Math.max(10, (count / max) * 100)
}

function getRankWidth(count: number) {
  const max = Math.max(...(data.value.gridRanking || []).map((i: any) => i.count || 0), 1)
  return Math.max(5, (count / max) * 100)
}

function goEventDetail(evt: any) {
  router.push('/events/' + (evt.id || evt.externalEventId))
}

// 全屏切换：对整个大屏容器启用浏览器全屏，切换后地图重新计算尺寸
async function toggleFullscreen() {
  try {
    if (!document.fullscreenElement) {
      await screenRef.value?.requestFullscreen()
    } else {
      await document.exitFullscreen()
    }
  } catch (e) {
    // 部分浏览器/iframe 环境不支持全屏 API，静默降级
  }
}

function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  setTimeout(() => mapInstance?.resize?.(), 100)
}

// 初始化监管地图底图（仅加载高德 API 和创建空地图实例）
async function initMapBase() {
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    // 读取系统配置的地图中心点（网格管理页可配置），未配置时用默认坐标
    try {
      const [lng, lat] = await Promise.all([
        http.get('/system/config/map.center.lng'),
        http.get('/system/config/map.center.lat'),
      ])
      if (lng && !isNaN(Number(lng))) mapCenter = [Number(lng), mapCenter[1]]
      if (lat && !isNaN(Number(lat))) mapCenter = [mapCenter[0], Number(lat)]
    } catch (e) { /* 配置接口失败时使用默认中心点 */ }
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker'] })
    mapInstance = new AMap.Map('bsMap', { zoom: 14, center: mapCenter, mapStyle: 'amap://styles/normal' })
  } catch (e) {
  }
}

// 回到固定中心位置（与系统配置的中心点一致，便于快速复位视角）
function mapReset() {
  if (!mapInstance) return
  mapInstance.setZoomAndCenter(14, mapCenter)
  mapInstance.setPitch(0)
  mapInstance.setRotation(0)
}

// 在已有地图实例上叠加业务数据（网格轮廓 + 事件点位）
async function overlayData() {
  if (!mapInstance) {
    return
  }

  let tree: any[] = []
  let activeEvents: any[] = []

  try {
    tree = await getGridTree() || []
  } catch (e) {
  }

  try {
    const result = await getEvents({ excludeHidden: true })
    activeEvents = (result.items || []).filter((e: any) => !e.archived)
  } catch (e) {
  }

  if (!tree.length && !activeEvents.length) {
    return
  }

  try {
    const map = mapInstance
    const AMap = (window as any).AMap

    // 重绘前先清除上一次叠加，避免 60s 轮询反复堆叠导致颜色叠实
    gridOverlays.forEach(o => o.setMap(null))
    gridOverlays = []
    eventMarkers.forEach(m => m.setMap(null))
    eventMarkers = []

    // 绘制网格轮廓（社区底图 + 大网格 + 小网格），纯展示不抢事件交互
    const drawPolygons = (nodes: any[]) => {
      for (const node of nodes) {
        if (node.roiJson) {
          try {
            const coords = JSON.parse(node.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const style = node.gridLevel === 1
                ? { fill: '#0284c7', fillOpacity: 0.08, stroke: '#0284c7', weight: 2 }
                : node.gridLevel === 2
                  ? { fill: '#f59e0b', fillOpacity: 0.35, stroke: '#ffffff', weight: 2 }
                  : { fill: '#10b981', fillOpacity: 0.30, stroke: '#ffffff', weight: 1 }
              const polygon = new AMap.Polygon({
                path: coords, fillColor: style.fill, fillOpacity: style.fillOpacity,
                strokeColor: style.stroke, strokeWeight: style.weight,
                zIndex: node.gridLevel === 1 ? 1 : 5, bubble: node.gridLevel === 1, map
              })
              gridOverlays.push(polygon)
            }
          } catch (e) {}
        }
        if (node.children) drawPolygons(node.children)
      }
    }

    if (tree.length) {
      drawPolygons(tree)
    }

    // 事件点位：悬停放大提示、点击弹出详情卡片
    for (const evt of activeEvents) {
      if (!evt.longitude || !evt.latitude) continue
      const color = evt.urgencyLevel === 'RED' ? '#FF4D4F' : evt.urgencyLevel === 'YELLOW' ? '#FAAD14' : '#52C41A'
      const dotHtml = (size: number, border: number, glow: number) =>
        `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:${border}px solid #fff;box-shadow:0 0 ${glow}px ${color};cursor:pointer;transition:all 0.2s;"></div>`
      const marker = new AMap.Marker({
        position: [evt.longitude, evt.latitude],
        zIndex: 10,
        content: dotHtml(12, 2, 6),
        offset: new AMap.Pixel(-6, -6),
        map
      })
      marker.on('mouseover', (e: any) => {
        const px = map.lngLatToContainer(e.lnglat)
        hoverInfo.visible = true
        hoverInfo.x = px.getX()
        hoverInfo.y = px.getY()
        hoverInfo.name = evt.title || '事件'
        marker.setContent(dotHtml(16, 3, 12))
        marker.setOffset(new AMap.Pixel(-8, -8))
      })
      marker.on('mousemove', (e: any) => {
        const px = map.lngLatToContainer(e.lnglat)
        hoverInfo.x = px.getX()
        hoverInfo.y = px.getY()
      })
      marker.on('mouseout', () => {
        hoverInfo.visible = false
        marker.setContent(dotHtml(12, 2, 6))
        marker.setOffset(new AMap.Pixel(-6, -6))
      })
      marker.on('click', () => {
        selectedEvent.value = evt
        map.setCenter([evt.longitude, evt.latitude])
      })
      eventMarkers.push(marker)
    }

    if (activeEvents.length) {
    }
  } catch (e) {
  }
}

// 完整初始化流程（兼容旧接口）：先加载底图，再叠加数据
async function initMap() {
  await initMapBase()
  await overlayData()
}

onMounted(async () => {
  updateTime()

  await initMapBase()

  await loadData()
  await overlayData()
  loadStreamData()

  document.addEventListener('fullscreenchange', onFullscreenChange)
  timer = window.setInterval(async () => {
    updateTime()
    await loadData()
    await overlayData()
    loadStreamData()
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
/* ============ 大屏容器：地图铺满整个主内容区 ============ */
.bs-screen {
  position: relative;
  margin: -24px; /* 抵消 main-content 的 padding，让地图成为真正主角 */
  height: calc(100vh - 56px);
  overflow: hidden;
  background: #eef3f8;
  color: #334155;
}
.bs-screen:fullscreen { height: 100vh; }
.bs-map { width: 100%; height: 100%; }

.bs-loading {
  position: absolute; inset: 0; z-index: 60;
  display: flex; align-items: center; justify-content: center;
  color: #0284c7; font-size: 16px; gap: 10px;
}

/* ============ 半透明悬浮面板通用样式（白色调） ============ */
.glass-panel {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(2, 132, 199, 0.12);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);
}
.chart-card { padding: 12px 16px; }
.chart-title {
  font-size: 13px; font-weight: 600; margin: 0 0 10px; color: #0284c7;
  display: flex; align-items: center; gap: 6px;
}
.panel-empty { font-size: 12px; color: #94a3b8; text-align: center; padding: 14px 0; margin: 0; }
.scroll-area { max-height: 190px; overflow-y: auto; }
.scroll-area::-webkit-scrollbar { width: 4px; }
.scroll-area::-webkit-scrollbar-thumb { background: rgba(2,132,199,0.25); border-radius: 2px; }

/* ============ 顶部悬浮栏 ============ */
.bs-topbar {
  position: absolute; top: 14px; left: 50%; transform: translateX(-50%);
  z-index: 20; display: flex; align-items: center; gap: 18px;
  padding: 10px 22px; white-space: nowrap;
}
.bs-topbar h1 {
  margin: 0; font-size: 19px; font-weight: 700; color: #0f172a; letter-spacing: 2px;
  display: flex; align-items: center; gap: 10px;
}
.bs-topbar h1 i { color: #0284c7; }
.bs-time { font-size: 13px; color: #64748b; font-variant-numeric: tabular-nums; }
.bs-fullscreen-btn {
  display: flex; align-items: center; gap: 6px;
  background: rgba(2, 132, 199, 0.10);
  border: 1px solid rgba(2, 132, 199, 0.30);
  border-radius: 8px; color: #0284c7; font-size: 13px;
  padding: 7px 14px; cursor: pointer; transition: all 0.2s;
}
.bs-fullscreen-btn:hover { background: rgba(2, 132, 199, 0.18); }

/* ============ KPI 悬浮指标条 ============ */
.bs-kpis {
  position: absolute; top: 78px; left: 50%; transform: translateX(-50%);
  z-index: 18; display: flex; gap: 12px; flex-wrap: wrap; justify-content: center;
  max-width: calc(100% - 640px);
}
.kpi {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 16px; min-width: 128px;
}
.kpi-icon {
  width: 38px; height: 38px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center; font-size: 17px; flex-shrink: 0;
}
.kpi-value { margin: 0; font-size: 21px; font-weight: 700; color: #0f172a; line-height: 1.1; }
.kpi-label { margin: 2px 0 0; font-size: 11px; color: #64748b; }

.kpi-blue { border-left: 3px solid #0284c7; }
.kpi-blue .kpi-icon { background: #e0f2fe; color: #0284c7; }
.kpi-green { border-left: 3px solid #059669; }
.kpi-green .kpi-icon { background: #d1fae5; color: #059669; }
.kpi-orange { border-left: 3px solid #d97706; }
.kpi-orange .kpi-icon { background: #fef3c7; color: #d97706; }
.kpi-purple { border-left: 3px solid #7c3aed; }
.kpi-purple .kpi-icon { background: #ede9fe; color: #7c3aed; }
.kpi-cyan { border-left: 3px solid #0891b2; }
.kpi-cyan .kpi-icon { background: #cffafe; color: #0891b2; }
.kpi-red { border-left: 3px solid #dc2626; }
.kpi-red .kpi-icon { background: #fee2e2; color: #dc2626; }

/* ============ 左右悬浮列 ============ */
.bs-col {
  position: absolute; top: 78px; bottom: 16px; z-index: 15;
  width: 300px; display: flex; flex-direction: column; gap: 12px;
  overflow-y: auto; padding-right: 2px;
}
.bs-col::-webkit-scrollbar { width: 0; }
.bs-left { left: 16px; }
.bs-right { right: 16px; }

/* ============ 紧急程度分布 ============ */
.urgency-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; font-size: 12px; }
.urgency-bar-row:last-child { margin-bottom: 0; }
.urgency-label { width: 34px; flex-shrink: 0; }
.urgency-green { color: #059669; }
.urgency-yellow { color: #d97706; }
.urgency-red { color: #dc2626; }
.urgency-bar-track { flex: 1; height: 9px; background: #e2e8f0; border-radius: 5px; overflow: hidden; }
.bar-green { background: linear-gradient(90deg, rgba(5,150,105,0.4), #059669); }
.bar-yellow { background: linear-gradient(90deg, rgba(217,119,6,0.4), #d97706); }
.bar-red { background: linear-gradient(90deg, rgba(220,38,38,0.4), #dc2626); }
.urgency-bar-fill { height: 100%; border-radius: 5px; transition: width 0.4s; }
.urgency-count { width: 30px; text-align: right; font-weight: 600; color: #0f172a; }

/* ============ 事件类型 TOP8 ============ */
.type-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; font-size: 12px; }
.type-bar-row:last-child { margin-bottom: 0; }
.type-rank {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  background: #f1f5f9; color: #64748b;
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.type-rank.top3 { background: #e0f2fe; color: #0284c7; font-weight: 700; }
.type-label { width: 74px; flex-shrink: 0; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-bar-track { flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.type-bar-fill {
  height: 100%; border-radius: 4px; transition: width 0.4s;
  background: linear-gradient(90deg, rgba(2,132,199,0.35), #0284c7);
}
.type-count { width: 26px; text-align: right; font-weight: 600; color: #0f172a; }

/* ============ 最新工单 ============ */
.wo-row {
  display: flex; align-items: center; gap: 8px; padding: 7px 0;
  border-bottom: 1px solid #f1f5f9; font-size: 11.5px;
}
.wo-row:last-child { border-bottom: none; }
.wo-no { color: #0284c7; font-variant-numeric: tabular-nums; flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.wo-dot { width: 6px; height: 6px; border-radius: 50%; background: #ef4444; flex-shrink: 0; box-shadow: 0 0 4px rgba(239, 68, 68, 0.8); }
.wo-status { border-radius: 4px; padding: 1px 6px; font-size: 10.5px; flex-shrink: 0; }
.status-waiting_accept { background: #fef3c7; color: #d97706; }
.status-processing { background: #e0f2fe; color: #0284c7; }
.status-waiting_close_confirm { background: #ede9fe; color: #7c3aed; }
.status-completed, .status-closed { background: #d1fae5; color: #059669; }
.wo-assignee { flex: 1; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.wo-time { color: #94a3b8; flex-shrink: 0; }

/* ============ 7 天趋势 ============ */
.trend-bars {
  display: flex; align-items: flex-end; gap: 10px;
  height: 110px; padding: 0 4px;
}
.trend-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.trend-bar {
  width: 70%; border-radius: 4px 4px 0 0; position: relative; min-height: 6px;
  background: linear-gradient(180deg, #0284c7, rgba(2,132,199,0.35));
}
.trend-value {
  position: absolute; top: -17px; left: 50%; transform: translateX(-50%);
  font-size: 10px; color: #0284c7; font-weight: 600;
}
.trend-date { font-size: 10px; color: #94a3b8; margin-top: 5px; }

/* ============ 网格事件排名 ============ */
.rank-row { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; font-size: 12px; }
.rank-row:last-child { margin-bottom: 0; }
.rank-num {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  background: #f1f5f9; color: #64748b;
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.rank-num.top3 { background: #fef3c7; color: #d97706; font-weight: 700; }
.rank-name { width: 84px; flex-shrink: 0; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rank-track { flex: 1; height: 8px; background: #e2e8f0; border-radius: 4px; overflow: hidden; }
.rank-fill {
  height: 100%; border-radius: 4px; transition: width 0.4s;
  background: linear-gradient(90deg, rgba(245,158,11,0.4), #f59e0b);
}
.rank-count { width: 26px; text-align: right; font-weight: 600; color: #0f172a; }

/* ============ 事件详情悬浮卡片 ============ */
.event-popup {
  position: absolute; z-index: 30; left: 50%; bottom: 26px; transform: translateX(-50%);
  width: 420px; max-width: calc(100% - 32px); padding: 14px 18px;
}
.popup-head { display: flex; align-items: center; gap: 8px; }
.popup-tag { border-radius: 4px; padding: 2px 8px; font-size: 11px; font-weight: 600; }
.tag-red { background: #fee2e2; color: #dc2626; }
.tag-orange { background: #fef3c7; color: #d97706; }
.tag-green { background: #d1fae5; color: #059669; }
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

/* ============ 悬停提示（浅色地图上保持深底白字，确保可读） ============ */
.hover-tip {
  position: absolute; z-index: 40; transform: translate(-50%, calc(-100% - 14px));
  background: rgba(15, 23, 42, 0.88); color: #fff; padding: 6px 12px; border-radius: 6px;
  font-size: 12px; font-weight: 600; pointer-events: none; white-space: nowrap;
  box-shadow: 0 4px 16px rgba(15,23,42,0.25);
}

/* ============ 顶部 KPI 可点击触发事件流 ============ */
.kpi { cursor: pointer; transition: box-shadow 0.2s, transform 0.2s; }
.kpi:hover { transform: translateY(-1px); }
.kpi.kpi-active { box-shadow: 0 0 0 2px rgba(2,132,199,0.55), 0 8px 24px rgba(15,23,42,0.12); }

/* ============ 实时事件流面板（悬浮于 KPI 下方） ============ */
.bs-stream {
  position: absolute; top: 152px; left: 50%; transform: translateX(-50%);
  z-index: 25; width: 360px; max-width: calc(100% - 32px);
  padding: 12px 14px; max-height: 62vh; display: flex; flex-direction: column;
}
.stream-head { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.stream-title { font-size: 13px; font-weight: 600; color: #0284c7; display: flex; align-items: center; gap: 6px; }
.stream-count { font-size: 11px; color: #94a3b8; }
.stream-close {
  margin-left: auto; border: none; background: none; color: #94a3b8;
  font-size: 18px; cursor: pointer; padding: 0 4px; line-height: 1;
}
.stream-close:hover { color: #0f172a; }
.stream-list { overflow-y: auto; max-height: calc(62vh - 48px); padding-right: 2px; }
.stream-list::-webkit-scrollbar { width: 4px; }
.stream-list::-webkit-scrollbar-thumb { background: rgba(2,132,199,0.25); border-radius: 2px; }

.st-item { display: flex; gap: 10px; padding: 9px 0; border-bottom: 1px solid #f1f5f9; }
.st-item:last-child { border-bottom: none; }
.st-item.clickable { cursor: pointer; }
.st-item.clickable:hover .st-title { color: #0284c7; }
.st-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; margin-top: 5px; }
.st-card { flex: 1; min-width: 0; }
.st-top { display: flex; align-items: center; gap: 8px; }
.st-tag { border-radius: 4px; padding: 1px 7px; font-size: 10.5px; font-weight: 600; flex-shrink: 0; }
.st-time { font-size: 11px; color: #94a3b8; margin-left: auto; }
.st-title { margin: 4px 0 2px; font-size: 13px; font-weight: 600; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.st-status { margin: 0; font-size: 11px; color: #64748b; display: flex; align-items: center; gap: 5px; }

.stream-fade-enter-active, .stream-fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.stream-fade-enter-from, .stream-fade-leave-to { opacity: 0; transform: translateX(-50%) translateY(-6px); }
</style>