<template>
  <!-- 大屏容器：地图为绝对主角，统计面板全部悬浮在地图上（负 margin 抵消主内容区 padding） -->
  <div ref="screenRef" class="dash-screen">
    <!-- 地图主体 -->
    <div id="gisMap" class="dash-map"></div>

    <!-- 错误提示（悬浮顶部中央） -->
    <div v-if="loadError" class="dash-error">
      <i class="fas fa-exclamation-circle"></i>
      <span>数据加载异常：{{ loadError }}</span>
    </div>

    <!-- 顶部悬浮栏：标题 + KPI + 全屏按钮 -->
    <div class="dash-top">
      <div class="dash-title-box">
        <h2><i class="fas fa-satellite-dish"></i> 全域态势看板</h2>
        <p>一屏观全域 · 以图管格 · 以格管人<span class="dash-time">{{ currentTime }}</span></p>
      </div>
      <div class="dash-kpis">
        <div class="kpi">
          <p class="kpi-num">{{ communityArea }}<span class="kpi-unit">km²</span></p>
          <p class="kpi-name"><i class="fas fa-map"></i> 社区面积</p>
        </div>
        <div class="kpi">
          <p class="kpi-num">{{ overview.largeGridCount || 6 }}</p>
          <p class="kpi-name"><i class="fas fa-layer-group"></i> 大网格</p>
        </div>
        <div class="kpi">
          <p class="kpi-num">{{ overview.smallGridCount || 12 }}</p>
          <p class="kpi-name"><i class="fas fa-th"></i> 小网格</p>
        </div>
        <div class="kpi">
          <p class="kpi-num">{{ overview.eventTotal || 0 }}</p>
          <p class="kpi-name"><i class="fas fa-exclamation-triangle"></i> 事件总数</p>
        </div>
        <div class="kpi kpi-danger">
          <p class="kpi-num">{{ overview.eventRed || 0 }}</p>
          <p class="kpi-name"><i class="fas fa-fire"></i> 紧急事件</p>
        </div>
      </div>
      <button class="dash-fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏看地图'">
        <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
        {{ isFullscreen ? '退出全屏' : '全屏' }}
      </button>
    </div>

    <!-- 左侧悬浮面板：三色分级 + 网格人口排名 -->
    <div class="dash-left">
      <div class="glass-panel">
        <h3><i class="fas fa-chart-pie"></i> 三色分级</h3>
        <div class="level-row" v-for="lv in levelRows" :key="lv.key">
          <span class="level-dot" :style="{ background: lv.color }"></span>
          <span class="level-name">{{ lv.name }}</span>
          <div class="level-track"><div class="level-fill" :style="{ width: lv.pct + '%', background: lv.color }"></div></div>
          <span class="level-count">{{ lv.count }}</span>
        </div>
      </div>
      <div class="glass-panel">
        <h3><i class="fas fa-users"></i> 网格人口排名</h3>
        <div id="chartPopulation" style="height:190px;"></div>
        <p v-if="!hasPopulation" class="panel-empty">暂无人口数据</p>
      </div>
    </div>

    <!-- 右侧悬浮面板：最新事件（点击可在地图上定位并查看详情） -->
    <div class="dash-right">
      <div class="glass-panel events-panel">
        <h3><i class="fas fa-bolt"></i> 最新事件 <span class="events-count">{{ events.length }}</span></h3>
        <div class="events-list">
          <div v-for="evt in events" :key="evt.id" class="event-item"
               :class="{ active: selectedEvent?.id === evt.id }" @click="focusEvent(evt)">
            <span class="tag" :class="evt.urgencyLevel === 'RED' ? 'tag-red' : evt.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green'">
              {{ evt.urgencyLevel === 'RED' ? '紧急' : evt.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
            </span>
            <div class="event-text">
              <p class="event-title">{{ evt.title }}</p>
              <p class="event-meta">{{ statusLabel(evt.currentStatus) }} · {{ formatTime(evt.createdAt) }}</p>
            </div>
            <i v-if="evt.longitude && evt.latitude" class="fas fa-crosshairs event-locate"></i>
          </div>
          <p v-if="!events.length" class="panel-empty">暂无事件</p>
        </div>
      </div>
    </div>

    <!-- 事件详情悬浮卡片（点击地图事件点 / 列表项后弹出） -->
    <div v-if="selectedEvent" class="event-popup glass-panel">
      <div class="popup-head">
        <span class="tag" :class="selectedEvent.urgencyLevel === 'RED' ? 'tag-red' : selectedEvent.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green'">
          {{ selectedEvent.urgencyLevel === 'RED' ? '紧急' : selectedEvent.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
        </span>
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

    <!-- 网格选中信息面板（点击网格弹出） -->
    <div v-if="selectedGrid" class="grid-popup glass-panel">
      <div class="popup-head">
        <span class="popup-grid-name">{{ selectedGrid.gridName }}</span>
        <button class="popup-close" @click="selectedGrid = null">&times;</button>
      </div>
      <div class="popup-meta">
        <div><i class="fas fa-sitemap"></i> {{ selectedGrid.gridLevel === 2 ? '二级网格（大网格）' : '三级网格（小网格）' }}</div>
        <div><i class="fas fa-user"></i> 负责人：{{ selectedGrid.managerName || '-' }}</div>
        <div>
          <i class="fas fa-circle" :style="{ color: selectedGrid.status === 'ACTIVE' ? '#52c41a' : '#f5222d', fontSize: '8px' }"></i>
          {{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}
          <template v-if="selectedGrid.area"> · {{ selectedGrid.area }} km²</template>
        </div>
      </div>
    </div>

    <!-- 悬停提示框 -->
    <div v-if="hoverInfo.visible" class="hover-tip" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
      {{ hoverInfo.name }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardOverview, getGridStats, getGridTree, getEvents } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import * as echarts from 'echarts'

const router = useRouter()

const overview = ref<any>({})
const events = ref<any[]>([])
const loadError = ref('')
const communityArea = ref('2.50')
const allGrids = ref<any[]>([])
const hasPopulation = ref(false)

// 保存实例引用以便销毁
let mapInstance: any = null
let chartInstance: any = null
let hoverId = 0
let clockTimer: number | undefined

const screenRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const currentTime = ref('')

const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '', id: 0 })
const selectedGrid = ref<any>(null)
const selectedEvent = ref<any>(null)

const total = computed(() => (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const pctOf = (n: number) => total.value ? (n / total.value * 100) : 0
const levelRows = computed(() => [
  { key: 'green', name: '一般', color: '#52C41A', count: overview.value.eventGreen || 0, pct: pctOf(overview.value.eventGreen || 0) },
  { key: 'yellow', name: '重点', color: '#FAAD14', count: overview.value.eventYellow || 0, pct: pctOf(overview.value.eventYellow || 0) },
  { key: 'red', name: '紧急', color: '#FF4D4F', count: overview.value.eventRed || 0, pct: pctOf(overview.value.eventRed || 0) }
])

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

// 点击事件（列表或地图标记）：弹出详情卡片并把地图视野移到事件点
function focusEvent(evt: any) {
  selectedEvent.value = evt
  if (mapInstance && evt.longitude && evt.latitude) {
    mapInstance.setCenter([evt.longitude, evt.latitude])
  }
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
  // 容器尺寸变化后让地图重新铺满
  setTimeout(() => mapInstance?.resize?.(), 100)
}

function tickClock() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  currentTime.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

onMounted(async () => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  document.addEventListener('fullscreenchange', onFullscreenChange)

  const errors: string[] = []

  try { overview.value = await getDashboardOverview() } catch (e: any) { errors.push('概览数据加载失败: ' + (e?.message || e)) }
  let stats: any = {}
  try { stats = await getGridStats() } catch (e: any) { errors.push('网格统计加载失败: ' + (e?.message || e)) }
  let tree: any = []
  try {
    tree = await getGridTree()
    // 提取所有网格计算面积
    const flatten = (nodes: any[]) => {
      for (const n of nodes) {
        allGrids.value.push(n)
        if (n.children) flatten(n.children)
      }
    }
    flatten(tree)
    const community = allGrids.value.find((g: any) => g.gridLevel === 1)
    if (community?.area) communityArea.value = community.area.toFixed(2)
  } catch (e: any) { errors.push('网格树加载失败: ' + (e?.message || e)) }
  try {
    const evtResult = await getEvents()
    events.value = evtResult.items || []
  } catch (e: any) {
    errors.push('事件列表加载失败: ' + (e?.message || e))
  }

  if (errors.length) {
    loadError.value = errors.join('；')
  }

  // Init map (only if tree data available and gisMap element exists)
  if (tree && tree.length > 0) {
    try {
      ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
      const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker'] })
      // 大屏深色底图，突出网格与事件点位
      mapInstance = new AMap.Map('gisMap', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/dark' })
      const map = mapInstance

      // 第一步：绘制社区底图（level 1）
      const drawCommunityOutline = (nodes: any[]) => {
        for (const node of nodes) {
          if (node.gridLevel === 1 && node.roiJson) {
            try {
              const coords = JSON.parse(node.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: '#38bdf8', fillOpacity: 0.05,
                  strokeColor: '#38bdf8', strokeWeight: 2, strokeStyle: 'solid', zIndex: 1, map
                })
                polygon.setOptions({ bubble: true })
              }
            } catch (e) {}
          }
          if (node.children) drawCommunityOutline(node.children)
        }
      }
      drawCommunityOutline(tree)

      // 第二步：绘制大网格（level 2）
      const drawLargeGrids = (nodes: any[]) => {
        for (const grid of nodes) {
          if (grid.gridLevel === 2 && grid.roiJson) {
            try {
              const coords = JSON.parse(grid.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const baseFillColor = '#f59e0b'
                const myId = ++hoverId
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: baseFillColor, fillOpacity: 0.30,
                  strokeColor: '#fbbf24', strokeWeight: 2, zIndex: 5, bubble: false, map
                })
                polygon.on('mouseover', (e: any) => {
                  polygon.setOptions({ fillOpacity: 0.55, zIndex: 20 })
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
                  polygon.setOptions({ fillOpacity: 0.30, zIndex: 5 })
                  hoverInfo.visible = false
                })
                polygon.on('click', () => { selectedGrid.value = grid })
              }
            } catch (e) {}
          }
          if (grid.children) drawLargeGrids(grid.children)
        }
      }
      drawLargeGrids(tree)

      // 第三步：绘制小网格（level 3）
      const drawSmallGrids = (nodes: any[]) => {
        for (const grid of nodes) {
          if (grid.gridLevel === 3 && grid.roiJson) {
            try {
              const coords = JSON.parse(grid.roiJson)
              if (Array.isArray(coords) && coords.length >= 3) {
                const baseFillColor = '#10b981'
                const myId = ++hoverId
                const polygon = new AMap.Polygon({
                  path: coords, fillColor: baseFillColor, fillOpacity: 0.26,
                  strokeColor: '#34d399', strokeWeight: 1, zIndex: 5, bubble: false, map
                })
                polygon.on('mouseover', (e: any) => {
                  polygon.setOptions({ fillOpacity: 0.5, zIndex: 20 })
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
                  polygon.setOptions({ fillOpacity: 0.26, zIndex: 5 })
                  hoverInfo.visible = false
                })
                polygon.on('click', () => { selectedGrid.value = grid })
              }
            } catch (e) {}
          }
          if (grid.children) drawSmallGrids(grid.children)
        }
      }
      drawSmallGrids(tree)

      // === 事件标记（只展示未归档的活跃事件）：悬停放大、点击弹出详情卡片 ===
      for (const evt of events.value.filter(e => !e.archived)) {
        if (!evt.longitude || !evt.latitude) continue
        const color = evt.urgencyLevel === 'RED' ? '#FF4D4F' : evt.urgencyLevel === 'YELLOW' ? '#FAAD14' : '#52C41A'
        const dotHtml = (size: number, border: number, glow: number) =>
          `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:${border}px solid #fff;box-shadow:0 0 ${glow}px ${color};cursor:pointer;transition:all 0.2s;"></div>`
        const marker = new AMap.Marker({
          position: [evt.longitude, evt.latitude],
          zIndex: 10,
          content: dotHtml(12, 2, 6),
          offset: new AMap.Pixel(-6, -6),
          map,
          extData: evt
        })
        marker.on('mouseover', (e: any) => {
          const px = map.lngLatToContainer(e.lnglat)
          hoverInfo.visible = true
          hoverInfo.x = px.getX()
          hoverInfo.y = px.getY()
          hoverInfo.name = evt.title || '事件'
          hoverInfo.id = ++hoverId
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
        // 地图可交互事件：点击点位弹出事件详情卡片
        marker.on('click', () => {
          selectedEvent.value = evt
        })
      }
    } catch (e: any) {
      if (!errors.length || !errors.some(msg => msg.includes('地图'))) {
        errors.push('地图初始化失败: ' + (e?.message || e))
      }
      loadError.value = errors.join('；')
    }
  }

  // Chart (only if stats available and chart element exists)
  const chartEl = document.getElementById('chartPopulation')
  if (stats && stats.populationRanking && stats.populationRanking.length && chartEl) {
    try {
      const ranking = stats.populationRanking || []
      hasPopulation.value = true
      chartInstance = echarts.init(chartEl)
      chartInstance.setOption({
        backgroundColor: 'transparent',
        grid: { left: 78, right: 30, top: 8, bottom: 8 },
        xAxis: {
          type: 'value',
          axisLabel: { color: 'rgba(255,255,255,0.55)', fontSize: 10 },
          splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } }
        },
        yAxis: {
          type: 'category', data: ranking.map((r: any) => r.gridName).reverse(),
          axisLabel: { color: 'rgba(255,255,255,0.85)', fontSize: 11 },
          axisLine: { lineStyle: { color: 'rgba(255,255,255,0.15)' } }
        },
        series: [{
          type: 'bar', barWidth: 12,
          data: ranking.map((r: any) => r.populationCount).reverse(),
          itemStyle: {
            borderRadius: [0, 6, 6, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: 'rgba(24,144,255,0.35)' },
              { offset: 1, color: '#1890FF' }
            ])
          },
          label: { show: true, position: 'right', color: 'rgba(255,255,255,0.85)', fontSize: 10 }
        }]
      })
    } catch (e: any) {
      errors.push('图表初始化失败: ' + (e?.message || e))
      loadError.value = errors.join('；')
    }
  }
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
/* ============ 大屏容器：地图铺满整个主内容区 ============ */
.dash-screen {
  position: relative;
  margin: -24px; /* 抵消 main-content 的 padding，让地图成为真正主角 */
  height: calc(100vh - 56px);
  overflow: hidden;
  background: #06121f;
}
.dash-screen:fullscreen { height: 100vh; }
.dash-map { width: 100%; height: 100%; }

/* ============ 玻璃拟态悬浮面板通用样式 ============ */
.glass-panel {
  background: rgba(8, 20, 38, 0.68);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 180, 255, 0.16);
  border-radius: 12px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
  color: #e2e8f0;
}
.glass-panel h3 {
  font-size: 13px; font-weight: 600; margin: 0 0 12px; color: #7dd3fc;
  display: flex; align-items: center; gap: 6px;
}
.panel-empty { font-size: 12px; color: rgba(255,255,255,0.4); text-align: center; padding: 16px 0; margin: 0; }

/* ============ 顶部悬浮栏 ============ */
.dash-top {
  position: absolute; top: 14px; left: 16px; right: 16px; z-index: 20;
  display: flex; align-items: center; gap: 14px; pointer-events: none;
}
.dash-title-box {
  pointer-events: auto;
  background: rgba(8, 20, 38, 0.68);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 180, 255, 0.16);
  border-radius: 12px; padding: 10px 18px; flex-shrink: 0;
}
.dash-title-box h2 {
  margin: 0; font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 1px;
  display: flex; align-items: center; gap: 8px;
}
.dash-title-box h2 i { color: #38bdf8; }
.dash-title-box p { margin: 2px 0 0; font-size: 11px; color: rgba(255,255,255,0.55); }
.dash-time { margin-left: 10px; color: #7dd3fc; font-variant-numeric: tabular-nums; }

.dash-kpis {
  pointer-events: auto;
  display: flex; gap: 10px; flex: 1; justify-content: center; flex-wrap: wrap;
}
.kpi {
  background: rgba(8, 20, 38, 0.68);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 180, 255, 0.16);
  border-radius: 10px; padding: 8px 16px; min-width: 108px; text-align: center;
}
.kpi-num { margin: 0; font-size: 22px; font-weight: 700; color: #fff; line-height: 1.2; }
.kpi-unit { font-size: 11px; color: rgba(255,255,255,0.5); margin-left: 3px; font-weight: 400; }
.kpi-name { margin: 2px 0 0; font-size: 11px; color: rgba(255,255,255,0.6); }
.kpi-name i { margin-right: 4px; color: #38bdf8; }
.kpi-danger .kpi-num { color: #ff7875; }
.kpi-danger .kpi-name i { color: #ff7875; }

.dash-fullscreen-btn {
  pointer-events: auto; flex-shrink: 0;
  display: flex; align-items: center; gap: 6px;
  background: rgba(24, 144, 255, 0.28);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(120, 180, 255, 0.4);
  border-radius: 10px; color: #fff; font-size: 13px;
  padding: 10px 16px; cursor: pointer; transition: all 0.2s;
}
.dash-fullscreen-btn:hover { background: rgba(24, 144, 255, 0.5); }

/* ============ 左侧悬浮面板 ============ */
.dash-left {
  position: absolute; left: 16px; top: 92px; bottom: 16px; z-index: 15;
  width: 280px; display: flex; flex-direction: column; gap: 12px;
}
.dash-left .glass-panel { padding: 14px 16px; }
.level-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; font-size: 12px; }
.level-row:last-child { margin-bottom: 0; }
.level-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.level-name { width: 30px; color: rgba(255,255,255,0.75); flex-shrink: 0; }
.level-track { flex: 1; height: 8px; background: rgba(255,255,255,0.08); border-radius: 4px; overflow: hidden; }
.level-fill { height: 100%; border-radius: 4px; transition: width 0.4s; }
.level-count { width: 28px; text-align: right; font-weight: 600; color: #fff; }

/* ============ 右侧悬浮面板：最新事件 ============ */
.dash-right {
  position: absolute; right: 16px; top: 92px; bottom: 16px; z-index: 15; width: 300px;
}
.events-panel { padding: 14px 16px; height: 100%; display: flex; flex-direction: column; }
.events-count {
  margin-left: 4px; background: rgba(24,144,255,0.3); color: #7dd3fc;
  font-size: 11px; border-radius: 8px; padding: 0 7px; line-height: 16px;
}
.events-list { flex: 1; overflow-y: auto; margin: 0 -6px; padding: 0 6px; }
.events-list::-webkit-scrollbar { width: 4px; }
.events-list::-webkit-scrollbar-thumb { background: rgba(120,180,255,0.25); border-radius: 2px; }
.event-item {
  display: flex; align-items: center; gap: 8px;
  padding: 9px 8px; border-radius: 8px; cursor: pointer; transition: background 0.15s;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.event-item:hover { background: rgba(120, 180, 255, 0.1); }
.event-item.active { background: rgba(24, 144, 255, 0.22); }
.event-text { flex: 1; min-width: 0; }
.event-title {
  margin: 0; font-size: 12.5px; color: #f1f5f9;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.event-meta { margin: 2px 0 0; font-size: 10.5px; color: rgba(255,255,255,0.45); }
.event-locate { color: rgba(125, 211, 252, 0.6); font-size: 11px; flex-shrink: 0; }

/* ============ 事件详情悬浮卡片 ============ */
.event-popup {
  position: absolute; z-index: 30; left: 50%; bottom: 26px; transform: translateX(-50%);
  width: 420px; max-width: calc(100% - 32px); padding: 14px 18px;
}
.popup-head { display: flex; align-items: center; gap: 8px; }
.popup-status { font-size: 11px; color: rgba(255,255,255,0.6); }
.popup-close {
  margin-left: auto; border: none; background: none; color: rgba(255,255,255,0.5);
  font-size: 18px; cursor: pointer; padding: 0 4px; line-height: 1;
}
.popup-close:hover { color: #fff; }
.popup-title { margin: 8px 0 8px; font-size: 15px; font-weight: 600; color: #fff; }
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

/* ============ 网格选中面板 ============ */
.grid-popup {
  position: absolute; z-index: 25; top: 92px; left: 50%; transform: translateX(-50%);
  padding: 12px 18px; min-width: 240px;
}
.popup-grid-name { font-size: 14px; font-weight: 700; color: #fff; }

/* ============ 悬停提示 ============ */
.hover-tip {
  position: absolute; z-index: 40; transform: translate(-50%, calc(-100% - 14px));
  background: rgba(8, 20, 38, 0.92); color: #fff; padding: 6px 12px; border-radius: 6px;
  font-size: 12px; font-weight: 600; pointer-events: none; white-space: nowrap;
  border: 1px solid rgba(120, 180, 255, 0.25); box-shadow: 0 4px 16px rgba(0,0,0,0.4);
}

/* ============ 错误提示 ============ */
.dash-error {
  position: absolute; z-index: 50; top: 86px; left: 50%; transform: translateX(-50%);
  background: rgba(120, 20, 20, 0.85); backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 120, 117, 0.4); border-radius: 8px;
  color: #ffd6d6; font-size: 12px; padding: 8px 16px; max-width: 70%;
  display: flex; align-items: center; gap: 8px;
}

/* 标签在深色底上的适配 */
.event-popup .tag, .event-item .tag { flex-shrink: 0; }
</style>
