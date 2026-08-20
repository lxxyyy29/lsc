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
        <button class="bs-fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏看地图'">
          <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
          {{ isFullscreen ? '退出全屏' : '全屏' }}
        </button>
      </div>

      <!-- KPI 悬浮指标条 -->
      <div class="bs-kpis">
        <div class="kpi glass-panel kpi-blue">
          <div class="kpi-icon"><i class="fas fa-exclamation-circle"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventTotal || 0 }}</p>
            <p class="kpi-label">事件总数</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-green">
          <div class="kpi-icon"><i class="fas fa-calendar-day"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventToday || 0 }}</p>
            <p class="kpi-label">今日新增</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-orange">
          <div class="kpi-icon"><i class="fas fa-clock"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventPending || 0 }}</p>
            <p class="kpi-label">待处置</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-purple">
          <div class="kpi-icon"><i class="fas fa-check-circle"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.eventClosed || 0 }}</p>
            <p class="kpi-label">已处置</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-cyan">
          <div class="kpi-icon"><i class="fas fa-clipboard-list"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.workOrderTotal || 0 }}</p>
            <p class="kpi-label">工单总数</p>
          </div>
        </div>
        <div class="kpi glass-panel kpi-red">
          <div class="kpi-icon"><i class="fas fa-user-clock"></i></div>
          <div>
            <p class="kpi-value">{{ data.kpis?.workOrderProcessing || 0 }}</p>
            <p class="kpi-label">处置中</p>
          </div>
        </div>
      </div>

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
              <span class="wo-no">{{ item.workOrderNo }}</span>
              <span class="wo-status" :class="'status-' + (item.status || '').toLowerCase()">{{ workOrderStatusLabel(item.status) }}</span>
              <span class="wo-assignee">{{ item.assigneeName || '未派单' }}</span>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBigScreenData, getGridTree, getEvents } from '../api'
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

let timer: number | undefined
let mapInstance: any = null

async function loadData() {
  try {
    data.value = await getBigScreenData() || {}
  } catch (e) {
    console.error('加载大屏数据失败:', e)
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
    WAITING_DISPATCH: '待派单', DISPATCHED_TO_WORK_ORDER: '已派单', CLOSED: '已关闭', IGNORED: '已忽略'
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

// 初始化监管地图：深色底图 + 社区/网格轮廓 + 可交互事件点位
async function initMap() {
  let tree: any[] = []
  let activeEvents: any[] = []
  try {
    tree = await getGridTree() || []
  } catch (e) { /* 网格树加载失败不阻断大屏 */ }
  try {
    const result = await getEvents()
    activeEvents = (result.items || []).filter((e: any) => !e.archived)
  } catch (e) { /* 事件加载失败不阻断大屏 */ }
  if (!tree.length) return

  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker'] })
    mapInstance = new AMap.Map('bsMap', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/dark' })
    const map = mapInstance

    // 绘制网格轮廓（社区底图 + 大网格 + 小网格），纯展示不抢事件交互
    const drawPolygons = (nodes: any[]) => {
      for (const node of nodes) {
        if (node.roiJson) {
          try {
            const coords = JSON.parse(node.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const style = node.gridLevel === 1
                ? { fill: '#38bdf8', fillOpacity: 0.05, stroke: '#38bdf8', weight: 2 }
                : node.gridLevel === 2
                  ? { fill: '#f59e0b', fillOpacity: 0.28, stroke: '#fbbf24', weight: 2 }
                  : { fill: '#10b981', fillOpacity: 0.24, stroke: '#34d399', weight: 1 }
              new AMap.Polygon({
                path: coords, fillColor: style.fill, fillOpacity: style.fillOpacity,
                strokeColor: style.stroke, strokeWeight: style.weight,
                zIndex: node.gridLevel === 1 ? 1 : 5, bubble: node.gridLevel === 1, map
              })
            }
          } catch (e) {}
        }
        if (node.children) drawPolygons(node.children)
      }
    }
    drawPolygons(tree)

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
    }
  } catch (e) {
    console.error('监管地图初始化失败:', e)
  }
}

onMounted(async () => {
  updateTime()
  await loadData()
  await initMap()
  document.addEventListener('fullscreenchange', onFullscreenChange)
  timer = window.setInterval(() => {
    updateTime()
    loadData()  // 每 60 秒刷新数据
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
  background: #06121f;
  color: #e2e8f0;
}
.bs-screen:fullscreen { height: 100vh; }
.bs-map { width: 100%; height: 100%; }

.bs-loading {
  position: absolute; inset: 0; z-index: 60;
  display: flex; align-items: center; justify-content: center;
  color: #7dd3fc; font-size: 16px; gap: 10px;
}

/* ============ 玻璃拟态面板通用样式 ============ */
.glass-panel {
  background: rgba(8, 20, 38, 0.68);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 180, 255, 0.16);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
}
.chart-card { padding: 12px 16px; }
.chart-title {
  font-size: 13px; font-weight: 600; margin: 0 0 10px; color: #7dd3fc;
  display: flex; align-items: center; gap: 6px;
}
.panel-empty { font-size: 12px; color: rgba(255,255,255,0.4); text-align: center; padding: 14px 0; margin: 0; }
.scroll-area { max-height: 190px; overflow-y: auto; }
.scroll-area::-webkit-scrollbar { width: 4px; }
.scroll-area::-webkit-scrollbar-thumb { background: rgba(120,180,255,0.25); border-radius: 2px; }

/* ============ 顶部悬浮栏 ============ */
.bs-topbar {
  position: absolute; top: 14px; left: 50%; transform: translateX(-50%);
  z-index: 20; display: flex; align-items: center; gap: 18px;
  padding: 10px 22px; white-space: nowrap;
}
.bs-topbar h1 {
  margin: 0; font-size: 19px; font-weight: 700; color: #fff; letter-spacing: 2px;
  display: flex; align-items: center; gap: 10px;
}
.bs-topbar h1 i { color: #38bdf8; }
.bs-time { font-size: 13px; color: rgba(255,255,255,0.6); font-variant-numeric: tabular-nums; }
.bs-fullscreen-btn {
  display: flex; align-items: center; gap: 6px;
  background: rgba(24, 144, 255, 0.28);
  border: 1px solid rgba(120, 180, 255, 0.4);
  border-radius: 8px; color: #fff; font-size: 13px;
  padding: 7px 14px; cursor: pointer; transition: all 0.2s;
}
.bs-fullscreen-btn:hover { background: rgba(24, 144, 255, 0.5); }

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
.kpi-value { margin: 0; font-size: 21px; font-weight: 700; color: #fff; line-height: 1.1; }
.kpi-label { margin: 2px 0 0; font-size: 11px; color: rgba(255,255,255,0.6); }

.kpi-blue { border-left: 3px solid #38bdf8; }
.kpi-blue .kpi-icon { background: rgba(56,189,248,0.18); color: #38bdf8; }
.kpi-green { border-left: 3px solid #34d399; }
.kpi-green .kpi-icon { background: rgba(52,211,153,0.18); color: #34d399; }
.kpi-orange { border-left: 3px solid #fbbf24; }
.kpi-orange .kpi-icon { background: rgba(251,191,36,0.18); color: #fbbf24; }
.kpi-purple { border-left: 3px solid #a78bfa; }
.kpi-purple .kpi-icon { background: rgba(167,139,250,0.18); color: #a78bfa; }
.kpi-cyan { border-left: 3px solid #22d3ee; }
.kpi-cyan .kpi-icon { background: rgba(34,211,238,0.18); color: #22d3ee; }
.kpi-red { border-left: 3px solid #f87171; }
.kpi-red .kpi-icon { background: rgba(248,113,113,0.18); color: #f87171; }

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
.urgency-green { color: #34d399; }
.urgency-yellow { color: #fbbf24; }
.urgency-red { color: #f87171; }
.urgency-bar-track { flex: 1; height: 9px; background: rgba(255,255,255,0.08); border-radius: 5px; overflow: hidden; }
.bar-green { background: linear-gradient(90deg, rgba(52,211,153,0.4), #34d399); }
.bar-yellow { background: linear-gradient(90deg, rgba(251,191,36,0.4), #fbbf24); }
.bar-red { background: linear-gradient(90deg, rgba(248,113,113,0.4), #f87171); }
.urgency-bar-fill { height: 100%; border-radius: 5px; transition: width 0.4s; }
.urgency-count { width: 30px; text-align: right; font-weight: 600; color: #fff; }

/* ============ 事件类型 TOP8 ============ */
.type-bar-row { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; font-size: 12px; }
.type-bar-row:last-child { margin-bottom: 0; }
.type-rank {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.6);
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.type-rank.top3 { background: rgba(24,144,255,0.35); color: #7dd3fc; font-weight: 700; }
.type-label { width: 74px; flex-shrink: 0; color: rgba(255,255,255,0.8); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.type-bar-track { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; }
.type-bar-fill {
  height: 100%; border-radius: 4px; transition: width 0.4s;
  background: linear-gradient(90deg, rgba(24,144,255,0.35), #1890ff);
}
.type-count { width: 26px; text-align: right; font-weight: 600; color: #fff; }

/* ============ 最新工单 ============ */
.wo-row {
  display: flex; align-items: center; gap: 8px; padding: 7px 0;
  border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 11.5px;
}
.wo-row:last-child { border-bottom: none; }
.wo-no { color: #7dd3fc; font-variant-numeric: tabular-nums; }
.wo-status { border-radius: 4px; padding: 1px 6px; font-size: 10.5px; flex-shrink: 0; }
.status-waiting_accept { background: rgba(251,191,36,0.18); color: #fbbf24; }
.status-processing { background: rgba(24,144,255,0.22); color: #7dd3fc; }
.status-waiting_close_confirm { background: rgba(167,139,250,0.2); color: #a78bfa; }
.status-completed, .status-closed { background: rgba(52,211,153,0.18); color: #34d399; }
.wo-assignee { flex: 1; color: rgba(255,255,255,0.75); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.wo-time { color: rgba(255,255,255,0.45); flex-shrink: 0; }

/* ============ 7 天趋势 ============ */
.trend-bars {
  display: flex; align-items: flex-end; gap: 10px;
  height: 110px; padding: 0 4px;
}
.trend-bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.trend-bar {
  width: 70%; border-radius: 4px 4px 0 0; position: relative; min-height: 6px;
  background: linear-gradient(180deg, #38bdf8, rgba(24,144,255,0.35));
}
.trend-value {
  position: absolute; top: -17px; left: 50%; transform: translateX(-50%);
  font-size: 10px; color: #7dd3fc; font-weight: 600;
}
.trend-date { font-size: 10px; color: rgba(255,255,255,0.5); margin-top: 5px; }

/* ============ 网格事件排名 ============ */
.rank-row { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; font-size: 12px; }
.rank-row:last-child { margin-bottom: 0; }
.rank-num {
  width: 18px; height: 18px; border-radius: 4px; flex-shrink: 0;
  background: rgba(255,255,255,0.1); color: rgba(255,255,255,0.6);
  font-size: 11px; display: flex; align-items: center; justify-content: center;
}
.rank-num.top3 { background: rgba(251,191,36,0.3); color: #fbbf24; font-weight: 700; }
.rank-name { width: 84px; flex-shrink: 0; color: rgba(255,255,255,0.85); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rank-track { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; }
.rank-fill {
  height: 100%; border-radius: 4px; transition: width 0.4s;
  background: linear-gradient(90deg, rgba(251,191,36,0.4), #fbbf24);
}
.rank-count { width: 26px; text-align: right; font-weight: 600; color: #fff; }

/* ============ 事件详情悬浮卡片 ============ */
.event-popup {
  position: absolute; z-index: 30; left: 50%; bottom: 26px; transform: translateX(-50%);
  width: 420px; max-width: calc(100% - 32px); padding: 14px 18px;
}
.popup-head { display: flex; align-items: center; gap: 8px; }
.popup-tag { border-radius: 4px; padding: 2px 8px; font-size: 11px; font-weight: 600; }
.tag-red { background: rgba(248,113,113,0.2); color: #f87171; }
.tag-orange { background: rgba(251,191,36,0.2); color: #fbbf24; }
.tag-green { background: rgba(52,211,153,0.2); color: #34d399; }
.popup-status { font-size: 11px; color: rgba(255,255,255,0.6); }
.popup-close {
  margin-left: auto; border: none; background: none; color: rgba(255,255,255,0.5);
  font-size: 18px; cursor: pointer; padding: 0 4px; line-height: 1;
}
.popup-close:hover { color: #fff; }
.popup-title { margin: 8px 0; font-size: 15px; font-weight: 600; color: #fff; }
.popup-meta { font-size: 12px; color: rgba(255,255,255,0.65); display: flex; flex-direction: column; gap: 4px; }
.popup-meta i { width: 14px; color: #7dd3fc; }
.popup-desc {
  margin: 8px 0 0; font-size: 12px; color: rgba(255,255,255,0.55); line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.popup-detail-btn {
  margin-top: 12px; width: 100%; padding: 8px 0; border: none; border-radius: 8px;
  background: #1890ff; color: #fff; font-size: 13px; cursor: pointer; transition: background 0.2s;
}
.popup-detail-btn:hover { background: #40a9ff; }

/* ============ 悬停提示 ============ */
.hover-tip {
  position: absolute; z-index: 40; transform: translate(-50%, calc(-100% - 14px));
  background: rgba(8, 20, 38, 0.92); color: #fff; padding: 6px 12px; border-radius: 6px;
  font-size: 12px; font-weight: 600; pointer-events: none; white-space: nowrap;
  border: 1px solid rgba(120, 180, 255, 0.25); box-shadow: 0 4px 16px rgba(0,0,0,0.4);
}
</style>
