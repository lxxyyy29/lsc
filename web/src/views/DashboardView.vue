<template>
  <!-- 全域态势感知大屏 v4：沉浸式指挥中心 HUD · 浅色科技风 -->
  <div ref="screenRef" class="dash-screen">
    <!-- 背景装饰：浅色渐变 + 极细网格 + 柔光 -->
    <div class="bg-decor">
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
    </div>

    <!-- 地图主体 -->
    <div id="gisMap" class="dash-map" @mousemove="onMapMouseMove" @mouseleave="crosshair.visible = false"></div>

    <!-- 十字准星线（hover 网格时显示） -->
    <div v-if="crosshair.visible" class="crosshair-v" :style="{ left: crosshair.x + 'px' }"></div>
    <div v-if="crosshair.visible" class="crosshair-h" :style="{ top: crosshair.y + 'px' }"></div>

    <!-- ============ 超窄顶部状态栏（48px） ============ -->
    <header class="topbar">
      <div class="topbar-brand">
        <span class="brand-dot" :class="isLive ? 'live' : 'demo'"></span>
        <i class="fas fa-satellite-dish"></i>
        <span class="brand-text">智慧网格 · 全域态势感知一张图</span>
      </div>
      <div class="topbar-sub">一屏观全域 · 以图管格 · 以格管人</div>
      <div class="topbar-right">
        <span class="tb-status" :class="isLive ? 'live' : 'demo'">
          <i class="fas fa-circle"></i>{{ isLive ? '实时数据' : '数据异常' }}
        </span>
        <div class="tb-clock">
          <span class="tb-time">{{ clock.time }}</span>
          <span class="tb-date">{{ clock.date }} {{ week }}</span>
        </div>
        <button class="tb-btn" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
          <i :class="isFullscreen ? 'fas fa-compress' : 'fas fa-expand'"></i>
        </button>
      </div>
    </header>

    <!-- ============ 左侧 Dock 工具栏（图标导航，点击展开内容面板） ============ -->
    <div class="left-dock">
      <div class="dock-logo"><i class="fas fa-th-large"></i></div>
      <button v-for="t in leftTabs" :key="t.key" class="dock-btn"
              :class="{ active: !layerPanel && leftTab === t.key, quiet: !leftOpen }"
              @click="toggleDock(t.key)">
        <i :class="t.icon"></i><span>{{ t.label }}</span>
      </button>
      <div class="dock-divider"></div>
      <button class="dock-btn" :class="{ active: layerPanel }" @click="toggleLayerDock">
        <i class="fas fa-layer-group"></i><span>图层</span>
      </button>
    </div>

    <!-- ============ 左侧内容面板（dom 常驻，class 控制显隐，图表不丢失） ============ -->
    <aside class="left-widget glass-panel" :class="{ open: leftOpen }">
      <div class="widget-head">
        <span class="widget-title">
          <i class="fas" :class="layerPanel ? 'fa-layer-group' : (leftTabs.find(t => t.key === leftTab)?.icon || 'fa-chart-pie')"></i>
          {{ layerPanel ? '图层控制' : (leftTabs.find(t => t.key === leftTab)?.label || '数据总览') }}
        </span>
        <button class="widget-fold" @click="leftOpen = !leftOpen"><i class="fas fa-times"></i></button>
      </div>
      <div class="tab-body">
        <!-- 图层面板 -->
        <div v-if="layerPanel" class="tab-pane">
          <div class="panel-tools">
            <span class="pt-title"><i class="fas fa-layer-group"></i>地图图层</span>
            <label class="pt-row">
              <input type="checkbox" v-model="layerState.grids" @change="toggleLayers" />
              <span class="pt-switch"><i></i></span>
              <span class="pt-label">网格</span>
            </label>
            <label class="pt-row">
              <input type="checkbox" v-model="layerState.events" @change="toggleLayers" />
              <span class="pt-switch"><i></i></span>
              <span class="pt-label">事件点</span>
            </label>
            <label class="pt-row">
              <input type="checkbox" v-model="layerState.heatmap" @change="toggleLayers" />
              <span class="pt-switch"><i></i></span>
              <span class="pt-label">热力图</span>
            </label>
            <label class="pt-row">
              <input type="checkbox" v-model="layerState.labels" @change="toggleLabels" />
              <span class="pt-switch"><i></i></span>
              <span class="pt-label">标注</span>
            </label>
            <label class="pt-row">
              <input type="checkbox" v-model="layerState.satellite" @change="toggleLayers" />
              <span class="pt-switch"><i></i></span>
              <span class="pt-label">卫星影像</span>
            </label>
          </div>
          <div class="grid-legend">
            <span class="gl-title"><i class="fas fa-shield-alt"></i>网格预警</span>
            <span class="gl-item"><i style="background:#0284c7"></i>平稳</span>
            <span class="gl-item"><i style="background:#22c55e"></i>一般</span>
            <span class="gl-item"><i style="background:#f59e0b"></i>重点</span>
            <span class="gl-item"><i style="background:#ef4444"></i>紧急</span>
          </div>
        </div>
        <!-- 分级概览 -->
        <div v-show="!layerPanel && leftTab === 'ring'" class="tab-pane">
          <div id="chartRing" class="chart-ring"></div>
          <div class="ring-legend">
            <div class="legend-row" v-for="lv in levelRows" :key="lv.key"
                 :class="{ dim: ringFilter && ringFilter !== lv.key }"
                 @click="toggleRingFilter(lv.key)">
              <span class="legend-dot" :style="{ background: lv.color, boxShadow: `0 0 6px ${lv.color}` }"></span>
              <span class="legend-name">{{ lv.name }}</span>
              <div class="legend-track"><i :style="{ width: lv.pct + '%', background: lv.color }"></i></div>
              <span class="legend-num">{{ lv.count }}</span>
            </div>
          </div>
          <div class="grid-legend">
            <span class="gl-title"><i class="fas fa-shield-alt"></i>网格预警</span>
            <span class="gl-item"><i style="background:#0284c7"></i>平稳</span>
            <span class="gl-item"><i style="background:#22c55e"></i>一般</span>
            <span class="gl-item"><i style="background:#f59e0b"></i>重点</span>
            <span class="gl-item"><i style="background:#ef4444"></i>紧急</span>
          </div>
          <p class="filter-hint" v-if="ringFilter">
            <i class="fas fa-filter"></i> 已筛选「{{ levelRows.find(l => l.key === ringFilter)?.name }}」
            <button @click="ringFilter = ''">清除</button>
          </p>
        </div>
        <!-- 7 日趋势 -->
        <div v-show="!layerPanel && leftTab === 'trend'" class="tab-pane">
          <div class="pane-sub"><i class="fas fa-chart-line"></i> 近 7 日事件趋势</div>
          <div id="chartTrend" class="chart-trend"></div>
        </div>
        <!-- 人口排名 -->
        <div v-show="!layerPanel && leftTab === 'rank'" class="tab-pane">
          <div class="pane-sub"><i class="fas fa-users"></i> 网格人口 TOP8</div>
          <div id="chartRank" class="chart-rank"></div>
          <p v-if="!hasPopulation" class="panel-empty">暂无人口数据</p>
        </div>
      </div>
    </aside>

    <!-- ============ 右侧实时事件时间轴面板（垂直时间线） ============ -->
    <aside class="timeline-panel glass-panel" :class="{ open: rightOpen }">
      <div class="panel-head-r">
        <span class="panel-title"><i class="fas fa-bolt"></i>实时事件流</span>
        <span class="panel-count">{{ filteredEvents.length }}/{{ events.length }}</span>
      </div>
      <div class="filter-chips">
        <button v-for="c in urgencyChips" :key="c.key"
                class="chip" :class="{ active: eventFilter === c.key }"
                :style="eventFilter === c.key ? { borderColor: c.color, color: c.color, background: c.bg } : {}"
                @click="eventFilter = c.key">
          {{ c.label }}
        </button>
      </div>
      <div class="search-box" style="margin-top:6px;">
        <i class="fas fa-map-marker-alt"></i>
        <input v-model="gridSearch" placeholder="搜索网格定位（如：第一网格一区）..." @keyup.enter="searchGrid" />
      </div>
      <div class="search-box">
        <i class="fas fa-search"></i>
        <input v-model="eventSearch" placeholder="搜索事件标题..." />
      </div>
      <div class="timeline-list">
        <div v-for="evt in filteredEvents" :key="evt.id" class="tl-item"
             :class="{ active: selectedEvent?.id === evt.id }" @click="focusEvent(evt)">
          <div class="tl-axis">
            <span class="tl-dot" :style="{ background: urgencyColor(evt.urgencyLevel), boxShadow: `0 0 0 3px ${urgencyBg(evt.urgencyLevel)}` }"></span>
            <span class="tl-line"></span>
          </div>
          <div class="tl-card" :style="{ borderLeftColor: urgencyColor(evt.urgencyLevel) }">
            <div class="tl-top">
              <span class="evt-tag" :style="{ background: urgencyBg(evt.urgencyLevel), color: urgencyColor(evt.urgencyLevel) }">
                {{ urgencyText(evt.urgencyLevel) }}
              </span>
              <span class="tl-time"><i class="far fa-clock"></i>{{ formatTime(evt.createdAt).slice(11) }}</span>
            </div>
            <p class="event-title">{{ evt.title }}</p>
            <p class="event-meta">
              <i class="fas" :class="statusIcon(evt.currentStatus)"></i>
              {{ statusLabel(evt.currentStatus) }}
            </p>
          </div>
        </div>
        <p v-if="!filteredEvents.length" class="panel-empty">无匹配事件</p>
      </div>
      <div class="tl-foot">
        <button class="tl-fold" @click="rightOpen = !rightOpen">
          <i :class="rightOpen ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
          <span v-if="rightOpen">收起面板</span>
        </button>
      </div>
    </aside>

    <!-- ============ 底部 Dock（系统状态 + KPI + 滚动动态） ============ -->
    <div class="dock-bar">
      <div class="dock-left">
        <span class="dk-status" :class="isLive ? 'live' : 'demo'">
          <i class="fas fa-circle"></i>{{ isLive ? '实时' : '异常' }}
        </span>
        <span class="dk-sys"><i class="fas fa-broadcast-tower"></i>系统正常</span>
        <span class="dk-sys"><i class="fas fa-server"></i>5 接口在线</span>
        <span class="dk-sys urgent" v-if="(overview.eventRed || 0) > 0">
          <i class="fas fa-fire"></i>紧急 {{ overview.eventRed }}
        </span>
        <div class="dock-spark">
          <span class="ds-label">今日事件</span>
          <b class="ds-num" :style="{ color: total > 10 ? '#ef4444' : '#0284c7' }">{{ total }}</b>
          <div class="ds-chart" id="spark-bottom"></div>
        </div>
      </div>
      <div class="dock-kpis">
        <div class="dk-pill" v-for="k in kpiList" :key="k.key">
          <i :class="k.icon" :style="{ color: k.color }"></i>
          <b>{{ k.value }}<em v-if="k.unit">{{ k.unit }}</em></b>
          <span>{{ k.label }}</span>
        </div>
      </div>
      <div class="dock-ticker">
        <span class="ticker-label"><i class="fas fa-bullhorn"></i>动态</span>
        <div class="ticker-view">
          <div class="ticker-run">
            <span v-for="e in events" :key="'a' + e.id" class="ticker-item">
              <i class="fas fa-circle" :style="{ color: urgencyColor(e.urgencyLevel) }"></i>{{ e.title }}
            </span>
            <span v-for="e in events" :key="'b' + e.id" class="ticker-item">
              <i class="fas fa-circle" :style="{ color: urgencyColor(e.urgencyLevel) }"></i>{{ e.title }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============ 地图控制按钮组（右下角，避开 Dock） ============ -->
    <div class="map-controls">
      <button class="mc-btn" @click="mapZoom(1)" title="放大"><i class="fas fa-plus"></i></button>
      <button class="mc-btn" @click="mapZoom(-1)" title="缩小"><i class="fas fa-minus"></i></button>
      <button class="mc-btn" :class="{ active: is3D }" @click="toggleView3D" :title="is3D ? '切换 2D' : '切换 3D'"><i class="fas fa-cube"></i></button>
      <button class="mc-btn" @click="mapReset" title="重置视角"><i class="fas fa-compass"></i></button>
      <button class="mc-btn" @click="flyRandom" title="巡检"><i class="fas fa-route"></i></button>
    </div>

    <!-- ============ 网格详情 HUD 弹窗（地图中央） ============
         glass-panel 自身开关由 selectedGrid 控制(v-if),display 时按下面规则定位:
           · 实时事件流展开  → right: 366px (16 距右 + 330 宽 + 20 间距)
           · 实时事件流收起  → right:  80px (16 距右 +  44 宽 + 20 间距,与 tl-fold 保持 20px)
    -->
    <transition name="hud">
      <div v-if="selectedGrid" class="grid-hud glass-panel"
           :class="{ 'is-open': rightOpen, 'is-folded': !rightOpen }">
        <button class="hud-close" @click="selectedGrid = null"><i class="fas fa-times"></i></button>
        <div class="hud-head">
          <div class="hud-urgency" v-if="gridUrgencyOf(selectedGrid)" :style="{ background: urgencyColor(gridUrgencyOf(selectedGrid)) }"></div>
          <h3>{{ selectedGrid.gridName }}</h3>
          <span class="hud-level">{{
            selectedGrid.gridLevel === 2 ? '二级 · 大网格' :
            selectedGrid.gridLevel === 3 ? '三级 · 小网格' : '一级 · 社区'
          }}</span>
        </div>
        <div class="hud-grid">
          <div class="hud-cell" v-if="gridUrgencyOf(selectedGrid)">
            <i class="fas fa-exclamation-triangle" :style="{ color: urgencyColor(gridUrgencyOf(selectedGrid)) }"></i>
            <span>预警级别</span>
            <b :style="{ color: urgencyColor(gridUrgencyOf(selectedGrid)) }">{{ urgencyText(gridUrgencyOf(selectedGrid)) }}</b>
          </div>
          <div class="hud-cell">
            <i class="fas fa-user-shield"></i><span>负责人</span><b>{{ selectedGrid.managerName || '-' }}</b>
          </div>
          <div class="hud-cell">
            <i class="fas fa-expand-arrows-alt"></i><span>面积</span><b>{{ selectedGrid.area || '-' }} km²</b>
          </div>
          <div class="hud-cell" v-if="selectedGrid.populationCount">
            <i class="fas fa-users"></i><span>人口</span><b>{{ selectedGrid.populationCount }} 人</b>
          </div>
          <div class="hud-cell">
            <i class="fas fa-circle" :style="{ color: selectedGrid.status === 'ACTIVE' ? '#16a34a' : '#dc2626', fontSize: '8px' }"></i>
            <span>状态</span><b>{{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}</b>
          </div>
          <div class="hud-cell" v-if="selectedGrid.children?.length">
            <i class="fas fa-sitemap"></i><span>子网格</span><b>{{ selectedGrid.children.length }} 个</b>
          </div>
        </div>
        <div class="hud-events" v-if="gridEvents(selectedGrid).length">
          <div class="he-title"><i class="fas fa-bolt"></i> 网格内事件 ({{ gridEvents(selectedGrid).length }})</div>
          <div class="he-item" v-for="e in gridEvents(selectedGrid)" :key="e.id" @click="focusEvent(e)">
            <span class="he-tag" :style="{ background: urgencyColor(e.urgencyLevel) }"></span>
            <span class="he-text">{{ e.title }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- ============ 事件详情气泡（右下角） ============ -->
    <transition name="pop">
      <div v-if="selectedEvent" class="event-pop glass-panel">
        <button class="pop-close" @click="selectedEvent = null"><i class="fas fa-times"></i></button>
        <div class="pop-top">
          <span class="evt-tag" :style="{ background: urgencyBg(selectedEvent.urgencyLevel), color: urgencyColor(selectedEvent.urgencyLevel) }">
            {{ urgencyText(selectedEvent.urgencyLevel) }}
          </span>
          <span class="pop-status">{{ statusLabel(selectedEvent.currentStatus) }}</span>
        </div>
        <p class="pop-title">{{ selectedEvent.title }}</p>
        <div class="pop-meta">
          <span v-if="selectedEvent.address"><i class="fas fa-map-marker-alt"></i> {{ selectedEvent.address }}</span>
          <span v-if="selectedEvent.createdAt"><i class="fas fa-clock"></i> {{ formatTime(selectedEvent.createdAt) }}</span>
        </div>
        <p v-if="selectedEvent.description" class="pop-desc">{{ selectedEvent.description }}</p>
        <button class="pop-btn" @click="goEventDetail(selectedEvent)">
          查看详情 <i class="fas fa-arrow-right"></i>
        </button>
      </div>
    </transition>

    <!-- 悬停提示 -->
    <div v-if="hoverInfo.visible" class="hover-tip" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
      {{ hoverInfo.name }}
    </div>

    <!-- 所有数据接口失败：居中紧凑错误卡片，逐条列出具体失败接口与原因（不再展示演示数据） -->
    <div v-if="fatalErrors.length" class="dash-fatal">
      <div class="dash-fatal-box">
        <div class="dash-fatal-head">
          <i class="fas fa-exclamation-circle"></i>
          <div>
            <h2>数据加载失败</h2>
            <p>以下 {{ fatalErrors.length }} 个接口不可用，请检查后端服务后重试</p>
          </div>
        </div>
        <ul class="dash-fatal-list">
          <li v-for="(msg, idx) in fatalErrors" :key="idx">{{ msg }}</li>
        </ul>
        <button @click="reload"><i class="fas fa-redo"></i> 重试</button>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="loadError" class="dash-error">
      <i class="fas fa-exclamation-circle"></i>
      <span>数据加载异常：{{ loadError }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, reactive, watchEffect, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardOverview, getGridStats, getGridTree, getEvents, getBigScreenData } from '../api'
import http from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import * as echarts from 'echarts'

const router = useRouter()

/* ================================================================
 * 数据说明（预留接口）：
 *  - 概览 KPI：GET /community/dashboard/overview
 *  - 大屏聚合数据（可选）：GET /community/dashboard/big-screen
 *  - 网格统计（人口排名）：GET /community/dashboard/grid-stats
 *  - 网格树（地图多边形）：GET /community/grids/tree
 *  - 事件列表：GET /events?page=1&size=20
 *  - 近 7 日趋势：可用 GET /events/statistics 或 big-screen 提供
  *  任一接口成功即展示真实数据；全部失败时展示全屏错误提示与重试按钮，不再使用内置演示数据。
 * ================================================================ */

// ==================== 网格预警色（大网格只描边/淡填充，小网格实填充） ====================
// 大网格：仅作为背景参考层，几乎透明，避免染透小网格
const GRID_STYLE: Record<string, { fill: string; fillOpacity: number; hoverOpacity: number; stroke: string }> = {
  RED:    { fill: '#ef4444', fillOpacity: 0.03, hoverOpacity: 0.08, stroke: '#ef4444' },
  YELLOW: { fill: '#f59e0b', fillOpacity: 0.03, hoverOpacity: 0.08, stroke: '#f59e0b' },
  GREEN:  { fill: '#22c55e', fillOpacity: 0.02, hoverOpacity: 0.06, stroke: '#22c55e' },
  NONE:   { fill: '#0284c7', fillOpacity: 0.02, hoverOpacity: 0.06, stroke: '#0284c7' }
}
// 小网格：前景层，颜色实，确保盖住大网格底色且自身颜色一致
const GRID_STYLE_SMALL: Record<string, { fill: string; fillOpacity: number; hoverOpacity: number; stroke: string }> = {
  RED:    { fill: '#ef4444', fillOpacity: 0.20, hoverOpacity: 0.32, stroke: '#ef4444' },
  YELLOW: { fill: '#f59e0b', fillOpacity: 0.20, hoverOpacity: 0.32, stroke: '#f59e0b' },
  GREEN:  { fill: '#22c55e', fillOpacity: 0.16, hoverOpacity: 0.26, stroke: '#22c55e' },
  NONE:   { fill: '#0284c7', fillOpacity: 0.22, hoverOpacity: 0.32, stroke: '#0284c7' }
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

/** 依据网格内事件计算紧急度 */
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

/** 网格预警级别 */
function gridUrgencyOf(grid: any): string {
  if (!grid) return ''
  if (grid.urgencyLevel) return grid.urgencyLevel
  try {
    const coords = JSON.parse(grid.roiJson)
    if (Array.isArray(coords) && coords.length >= 3) return computeGridUrgency(coords, events.value)
  } catch (e) {}
  return ''
}

/** 网格内的事件列表 */
function gridEvents(grid: any): any[] {
  if (!grid?.roiJson) return []
  try {
    const coords = JSON.parse(grid.roiJson)
    if (!Array.isArray(coords) || coords.length < 3) return []
    return events.value.filter((e: any) =>
      e.longitude != null && e.latitude != null && pointInPolygon([e.longitude, e.latitude], coords))
  } catch (e) { return [] }
}

// ==================== 地图默认中心点（社区位置） ====================
const MOCK_CENTER: [number, number] = [113.9395, 22.9712]

// 近 7 日趋势：优先用后端聚合（big-screen weeklyTrend），其次按已加载事件创建时间统计；无数据返回 null（图表留空，不再用假数据填充）
function realTrend(): { days: string[]; values: number[] } | null {
  const fmt = (d: Date) => `${d.getMonth() + 1}/${d.getDate()}`
  const wt = overview.value?.weeklyTrend
  if (Array.isArray(wt) && wt.length) {
    return { days: wt.map((i: any) => fmt(new Date(i.date))), values: wt.map((i: any) => Number(i.count) || 0) }
  }
  if (!events.value.length) return null
  const buckets: Record<string, number> = {}
  const days: string[] = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    days.push(fmt(d)); buckets[fmt(d)] = 0
  }
  for (const e of events.value) {
    const k = e.createdAt ? fmt(new Date(e.createdAt)) : ''
    if (k in buckets) buckets[k]++
  }
  return { days, values: days.map(d => buckets[d]) }
}

// ==================== 状态 ====================
const overview = ref<any>({})
const events = ref<any[]>([])
const loadError = ref('')
const areaRaw = ref(0)
const allGrids = ref<any[]>([])
const hasPopulation = ref(false)
const isLive = ref(false)
// 所有数据接口失败时逐条展示具体失败接口与原因（不再回退演示数据）
const fatalErrors = ref<string[]>([])

let mapInstance: any = null
let chartRing: any = null
let chartTrend: any = null
let chartRank: any = null
let chartSparkBottom: any = null
let heatLayer: any = null
let hoverId = 0
let clockTimer: number | undefined
let resizeHandler: (() => void) | null = null
let gridPolygonList: any[] = []
// 网格ID -> 多边形 映射（搜索定位/高亮用）
const gridPolygonMap: Record<number, any> = {}
let labelMarkerList: any[] = []
let evtLabelsLayer: any = null      // 非紧急事件高性能图层（LabelsLayer）
let redPulseMarkers: any[] = []     // 红色紧急事件保留 DOM 脉冲动画
let clusterMarkerList: any[] = []   // 缩小级别聚合气泡
let gridBoundsMap = new Map<string, any>()  // 网格 id → 全顶点 Bounds

const screenRef = ref<HTMLElement | null>(null)
const isFullscreen = ref(false)
const clock = reactive({ time: '--:--:--', date: '---- -- --' })
const week = ref('')

const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '', id: 0 })
const selectedGrid = ref<any>(null)
const selectedEvent = ref<any>(null)

// ==================== 差异化交互状态 ====================
const leftOpen = ref(true)
const rightOpen = ref(true)
const leftTab = ref<'ring' | 'trend' | 'rank'>('ring')
const leftTabs = [
  { key: 'ring', label: '分级', icon: 'fas fa-chart-pie' },
  { key: 'trend', label: '趋势', icon: 'fas fa-chart-line' },
  { key: 'rank', label: '人口', icon: 'fas fa-users' }
] as const

const eventFilter = ref<'ALL' | 'RED' | 'YELLOW' | 'GREEN'>('ALL')
const eventSearch = ref('')
const gridSearch = ref('')
let highlightedGridId: number | null = null
const urgencyChips = [
  { key: 'ALL', label: '全部', color: '#0284c7', bg: 'rgba(2,132,199,0.08)' },
  { key: 'RED', label: '紧急', color: '#ef4444', bg: 'rgba(239,68,68,0.08)' },
  { key: 'YELLOW', label: '重点', color: '#f59e0b', bg: 'rgba(245,158,11,0.08)' },
  { key: 'GREEN', label: '一般', color: '#22c55e', bg: 'rgba(34,197,94,0.08)' }
] as const

const ringFilter = ref('')
const layerState = reactive({ grids: true, events: true, heatmap: false, labels: false, satellite: false })
const crosshair = reactive({ visible: false, x: 0, y: 0 })
const is3D = ref(true) // 3D 视角开关

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

const largeGridNum = useCount(() => overview.value.largeGridCount || 0)
const smallGridNum = useCount(() => overview.value.smallGridCount || 0)
const eventTotalNum = useCount(() => overview.value.eventTotal || 0)
const redEventNum = useCount(() => overview.value.eventRed || 0)
const fmtArea = computed(() => (Number(areaRaw.value) || 0).toFixed(2))

const kpiList = computed(() => [
  { key: 'area', icon: 'fas fa-map', color: '#0284c7', label: '社区面积', value: fmtArea.value, unit: 'km²', spark: false },
  { key: 'large', icon: 'fas fa-layer-group', color: '#0284c7', label: '大网格', value: largeGridNum.value, unit: '', spark: false },
  { key: 'small', icon: 'fas fa-th', color: '#0284c7', label: '小网格', value: smallGridNum.value, unit: '', spark: false },
  { key: 'total', icon: 'fas fa-exclamation-triangle', color: '#0284c7', label: '事件总数', value: eventTotalNum.value, unit: '', spark: true },
  { key: 'red', icon: 'fas fa-fire', color: '#ef4444', label: '紧急事件', value: redEventNum.value, unit: '', spark: false }
])

// ==================== 三色分级 ====================
const total = computed(() =>
  (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const pctOf = (n: number) => total.value ? (n / total.value * 100) : 0
const levelRows = computed(() => [
  { key: 'green', name: '一般', color: '#22c55e', count: overview.value.eventGreen || 0, pct: pctOf(overview.value.eventGreen || 0) },
  { key: 'yellow', name: '重点', color: '#f59e0b', count: overview.value.eventYellow || 0, pct: pctOf(overview.value.eventYellow || 0) },
  { key: 'red', name: '紧急', color: '#ef4444', count: overview.value.eventRed || 0, pct: pctOf(overview.value.eventRed || 0) }
])

// ==================== 事件筛选 ====================
const filteredEvents = computed(() => {
  let list = events.value
  if (eventFilter.value !== 'ALL') {
    list = list.filter((e: any) => e.urgencyLevel === eventFilter.value)
  }
  if (eventSearch.value.trim()) {
    const kw = eventSearch.value.trim().toLowerCase()
    list = list.filter((e: any) => (e.title || '').toLowerCase().includes(kw))
  }
  return list
})

// ==================== 工具函数 ====================
function urgencyText(lv: string) {
  return lv === 'RED' ? '紧急' : lv === 'YELLOW' ? '重点' : '一般'
}
function urgencyColor(lv: string) {
  return lv === 'RED' ? '#ef4444' : lv === 'YELLOW' ? '#f59e0b' : '#22c55e'
}
function urgencyBg(lv: string) {
  return lv === 'RED' ? 'rgba(239,68,68,0.12)' : lv === 'YELLOW' ? 'rgba(245,158,11,0.12)' : 'rgba(34,197,94,0.12)'
}

/** requestAnimationFrame 节流：高频 mousemove 每帧最多执行一次 */
function rafThrottle<T extends (...args: any[]) => void>(fn: T): T {
  let pending = false
  let lastArgs: any[] = []
  return ((...args: any[]) => {
    lastArgs = args
    if (pending) return
    pending = true
    requestAnimationFrame(() => { pending = false; fn(...lastArgs) })
  }) as T
}

/** 六边形事件图标（SVG data URI，供 LabelsLayer 批量渲染） */
function hexIconUri(color: string): string {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 22 22">` +
    `<polygon points="11,1.8 19,6.4 19,15.6 11,20.2 3,15.6 3,6.4" fill="${color}" fill-opacity="0.25"/>` +
    `<circle cx="11" cy="11" r="4.2" fill="${color}" stroke="#ffffff" stroke-width="1.6"/></svg>`
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}
const HEX_ICON: Record<string, string> = {
  RED: hexIconUri('#ef4444'), YELLOW: hexIconUri('#f59e0b'), GREEN: hexIconUri('#22c55e')
}

/** 缩放级别聚合阈值：低于此值显示聚合气泡 */
const CLUSTER_ZOOM = 13.5

/** 由多边形全顶点计算包围盒（修复非矩形网格点击视野偏移） */
function boundsOfPath(AMap: any, coords: number[][]): any {
  let minLng = Infinity, minLat = Infinity, maxLng = -Infinity, maxLat = -Infinity
  for (const [lng, lat] of coords) {
    if (lng < minLng) minLng = lng
    if (lat < minLat) minLat = lat
    if (lng > maxLng) maxLng = lng
    if (lat > maxLat) maxLat = lat
  }
  return new AMap.Bounds([minLng, minLat], [maxLng, maxLat])
}

/** hover 坐标更新统一节流入口（网格/事件点共用） */
const throttledHoverMove = rafThrottle((e: any) => {
  if (!mapInstance) return
  const px = mapInstance.lngLatToContainer(e.lnglat)
  crosshair.x = px.getX(); crosshair.y = px.getY()
  hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
})
function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核', IN_AUDIT: '审核中', AUDIT_APPROVED: '已通过', AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单', DISPATCHED_TO_WORK_ORDER: '已派单', CLOSED: '已关闭', IGNORED: '已忽略'
  }
  return map[status] || status || '未知'
}
function statusIcon(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: 'fa-hourglass-half', IN_AUDIT: 'fa-spinner', AUDIT_APPROVED: 'fa-check-circle',
    AUDIT_REJECTED: 'fa-times-circle', WAITING_DISPATCH: 'fa-paper-plane',
    DISPATCHED_TO_WORK_ORDER: 'fa-tools', CLOSED: 'fa-check-double', IGNORED: 'fa-eye-slash'
  }
  return map[status] || 'fa-info-circle'
}
function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 16)
}
function goEventDetail(evt: any) {
  router.push('/events/' + (evt.id || evt.externalEventId))
}
// 搜索网格并定位：按网格名称匹配，放大居中 + 高亮该网格边界
function searchGrid() {
  const kw = gridSearch.value.trim()
  if (!kw || !mapInstance) return
  const match = allGrids.value.find((g: any) => (g.gridName || '').includes(kw))
  if (!match) {
    crosshair.visible = true
    crosshair.x = 16; crosshair.y = 60
    hoverInfo.visible = true; hoverInfo.x = 16; hoverInfo.y = 60
    hoverInfo.name = `未找到网格「${kw}」，请尝试输入网格名称关键字`
    setTimeout(() => { hoverInfo.visible = false }, 2500)
    return
  }
  // 还原上一次高亮
  if (highlightedGridId != null && gridPolygonMap[highlightedGridId]) {
    const prev = gridPolygonMap[highlightedGridId]
    prev.setOptions({ fillOpacity: prev.__origFillOpacity ?? 0, strokeWeight: prev.__origStrokeWeight ?? 1.5, zIndex: 5 })
  }
  highlightedGridId = Number(match.id)
  let coords: number[][] = []
  try { coords = JSON.parse(match.roiJson || '[]') } catch (e) {}
  if (coords.length >= 3) {
    let cx = 0, cy = 0
    for (const [x, y] of coords) { cx += x; cy += y }
    cx /= coords.length; cy /= coords.length
    const poly = gridPolygonMap[highlightedGridId]
    if (poly) {
      poly.__origFillOpacity = poly.getOptions().fillOpacity ?? 0
      poly.__origStrokeWeight = poly.getOptions().strokeWeight ?? 1.5
      poly.setOptions({ fillOpacity: 0.35, strokeWeight: 4, strokeColor: '#f59e0b', zIndex: 30 })
    }
    mapInstance.setZoomAndCenter(16, [cx, cy])
  } else {
    mapInstance.setZoom(15)
  }
  crosshair.visible = true
  const px = mapInstance.lngLatToContainer(coords.length >= 3 ? [coords[0][0], coords[0][1]] : [0, 0])
  if (px) { crosshair.x = px.getX(); crosshair.y = px.getY() }
  hoverInfo.visible = true; hoverInfo.x = 16; hoverInfo.y = 60
  hoverInfo.name = `已定位：${match.gridName}${match.area ? `（${match.area} km²）` : ''}`
  setTimeout(() => { hoverInfo.visible = false }, 3000)
}

function focusEvent(evt: any) {
  selectedEvent.value = evt
  if (mapInstance && evt.longitude && evt.latitude) {
    mapInstance.setZoomAndCenter(15, [evt.longitude, evt.latitude])
  }
}

// ==================== 环形筛选 ====================
function toggleRingFilter(key: string) {
  ringFilter.value = ringFilter.value === key ? '' : key
  renderRing()
}

// ==================== 全屏 ====================
async function toggleFullscreen() {
  try {
    if (!document.fullscreenElement) await screenRef.value?.requestFullscreen()
    else await document.exitFullscreen()
  } catch (e) {}
}
function onFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
  setTimeout(() => {
    mapInstance?.resize?.()
    chartRing?.resize?.(); chartTrend?.resize?.(); chartRank?.resize?.(); chartSparkBottom?.resize?.()
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

// ==================== 地图控制 ====================
function mapZoom(delta: number) {
  if (!mapInstance) return
  mapInstance.setZoom(mapInstance.getZoom() + delta)
}
function mapReset() {
  if (!mapInstance) return
  mapInstance.setZoomAndCenter(14, MOCK_CENTER)
  mapInstance.setPitch(45)
  mapInstance.setRotation(-20)
}
function toggleView3D() {
  if (!mapInstance) return
  is3D.value = !is3D.value
  mapInstance.setPitch(is3D.value ? 45 : 0)
  mapInstance.setRotation(0)
}
function flyRandom() {
  if (!mapInstance || !events.value.length) return
  const evt = events.value[Math.floor(Math.random() * events.value.length)]
  if (evt.longitude && evt.latitude) {
    mapInstance.setZoomAndCenter(16, [evt.longitude, evt.latitude])
    selectedEvent.value = evt
  }
}

// ==================== 十字准星 ====================
function onMapMouseMove(e: MouseEvent) {
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  crosshair.x = e.clientX - rect.left
  crosshair.y = e.clientY - rect.top
}

// ==================== 图层切换 ====================
function toggleLayers() {
  if (!mapInstance) return
  for (const p of gridPolygonList) {
    if (layerState.grids) p.show()
    else p.hide()
  }
  // 事件层统一调度：缩小显示聚合气泡、放大展开单点
  syncClusterVisibility()
  // 热力图
  if (layerState.heatmap) {
    const AMap = (window as any).AMap
    const HeatMapCls = AMap?.HeatMap || AMap?.Heatmap
    if (!HeatMapCls) {
      console.warn('[热力图] AMap.HeatMap 插件未加载')
      layerState.heatmap = false
      return
    }
    if (!heatLayer) {
      heatLayer = new HeatMapCls(mapInstance, {
        radius: 38,
        opacity: [0.2, 0.75],
        gradient: { 0.15: '#0284c7', 0.45: '#facc15', 0.7: '#f97316', 1: '#dc2626' }
      })
    }
    const data = events.value
      .filter((e: any) => e.longitude != null && e.latitude != null)
      .map((e: any) => ({
        lng: +e.longitude,
        lat: +e.latitude,
        count: e.urgencyLevel === 'RED' ? 25 : e.urgencyLevel === 'YELLOW' ? 18 : 10
      }))
    if (data.length) {
      heatLayer.setDataSet({ data, max: 80 })
      heatLayer.show()
    } else {
      heatLayer.hide()
    }
  } else if (heatLayer) {
    heatLayer.hide()
  }
  // 卫星影像图层
  toggleSatellite()
}

let satelliteLayer: any = null
let roadNetLayer: any = null
/** 卫星影像层：卫星底图 + 路网标注叠加，关闭后恢复矢量底图 */
function toggleSatellite() {
  if (!mapInstance) return
  const AMap = (window as any).AMap
  if (layerState.satellite) {
    if (!satelliteLayer) {
      satelliteLayer = new AMap.TileLayer.Satellite()
      roadNetLayer = new AMap.TileLayer.RoadNet()
    }
    mapInstance.add([satelliteLayer, roadNetLayer])
    // 隐藏矢量建筑层，避免白灰色建筑块叠加遮挡卫星影像
    mapInstance.setFeatures(['bg', 'road'])
  } else if (satelliteLayer) {
    mapInstance.remove([satelliteLayer, roadNetLayer])
    // 恢复 3D 立体建筑
    mapInstance.setFeatures(['bg', 'road', 'building', 'building3D'])
  }
}

/** 重建缩放级别聚合气泡（按约 2km 网格单元分组） */
function rebuildClusters(AMap: any) {
  for (const m of clusterMarkerList) m.setMap(null)
  clusterMarkerList = []
  if (!mapInstance) return
  const groups = new Map<string, { x: number; y: number; n: number; red: boolean }>()
  for (const evt of events.value) {
    if (!evt.longitude || !evt.latitude) continue
    const key = `${Math.round(+evt.longitude / 0.02)}_${Math.round(+evt.latitude / 0.02)}`
    const g = groups.get(key)
    if (g) { g.x += +evt.longitude; g.y += +evt.latitude; g.n += 1; if (evt.urgencyLevel === 'RED') g.red = true }
    else groups.set(key, { x: +evt.longitude, y: +evt.latitude, n: 1, red: evt.urgencyLevel === 'RED' })
  }
  for (const g of groups.values()) {
    const m = new AMap.Marker({
      position: [g.x / g.n, g.y / g.n], zIndex: 14, bubble: true,
      content: `<div class="evt-cluster${g.red ? ' hot' : ''}">${g.n}</div>`,
      offset: new AMap.Pixel(-18, -18), map: mapInstance
    })
    m.on('click', () => { mapInstance.setZoomAndCenter(CLUSTER_ZOOM + 0.6, m.getPosition()) })
    clusterMarkerList.push(m)
  }
}

/** 按缩放级别与图层开关同步事件单点/聚合气泡的可见性 */
function syncClusterVisibility() {
  if (!mapInstance) return
  const clustered = mapInstance.getZoom() < CLUSTER_ZOOM
  const showPoints = layerState.events && !clustered
  if (evtLabelsLayer) { showPoints ? evtLabelsLayer.show() : evtLabelsLayer.hide() }
  for (const m of redPulseMarkers) { showPoints ? m.show() : m.hide() }
  for (const m of clusterMarkerList) { (layerState.events && clustered) ? m.show() : m.hide() }
}
function toggleLabels() {
  if (!mapInstance) return
  for (const m of labelMarkerList) {
    if (layerState.labels) m.show()
    else m.hide()
  }
}

// ==================== 地图初始化 ====================
async function initMap(tree: any[]) {
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    // 读取系统配置的地图中心点（网格管理页可配置），未配置时用默认坐标
    let center: [number, number] = MOCK_CENTER
    try {
      const [lng, lat] = await Promise.all([
        http.get('/system/config/map.center.lng'),
        http.get('/system/config/map.center.lat'),
      ])
      if (lng && !isNaN(Number(lng)) && lat && !isNaN(Number(lat))) center = [Number(lng), Number(lat)]
    } catch (e) { /* 配置接口失败时使用默认中心点 */ }
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker', 'AMap.Text', 'AMap.HeatMap', 'AMap.LabelsLayer', 'AMap.TileLayer'] })
    mapInstance = new AMap.Map('gisMap', {
      zoom: 14, center,
      viewMode: '3D',           // 3D 视图模式
      pitch: 45,                // 俯仰角 45°
      rotation: 0,              // 正向朝上，避免旋转造成方位辨识困难
      mapStyle: 'amap://styles/normal',
      features: ['bg', 'road', 'building', 'building3D'], // 保留 3D 立体建筑，仅隐藏 POI 文字标注降噪
      expandZoomRange: true,
      zooms: [3, 20]
    })
    const map = mapInstance

    // 社区轮廓（level 1）：虚线描边，不填充（差异化：虚线 vs 当前实线）
    const drawCommunity = (nodes: any[]) => {
      for (const node of nodes) {
        if (node.gridLevel === 1 && node.roiJson) {
          try {
            const coords = JSON.parse(node.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const p = new AMap.Polygon({
                path: coords, fillColor: '#0284c7', fillOpacity: 0,
                strokeColor: '#0284c7', strokeWeight: 1.5, strokeStyle: 'dashed',
                strokeDasharray: [8, 6], strokeOpacity: 0.7, zIndex: 1, bubble: true, map
              })
              gridPolygonList.push(p)
              gridPolygonMap[Number(node.id)] = p
            }
          } catch (e) {}
        }
        if (node.children) drawCommunity(node.children)
      }
    }

    // 大网格（level 2）：渐变透色填充 + 实色描边（差异化：实色边 vs 白边）
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
                strokeColor: st.stroke, strokeWeight: 1.5, strokeOpacity: 0.85,
                zIndex: 5, bubble: false, map
              })
              gridPolygonList.push(polygon)
              gridPolygonMap[Number(grid.id)] = polygon
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: st.hoverOpacity, strokeWeight: 2.5, zIndex: 20 })
                crosshair.visible = true
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km² · ${lvText}`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => throttledHoverMove(e))
              polygon.on('mouseout', () => {
                if (hoverInfo.id !== myId) return
                polygon.setOptions({ fillOpacity: st.fillOpacity, strokeWeight: 1.5, zIndex: 5 })
                crosshair.visible = false
                hoverInfo.visible = false
              })
              polygon.on('click', () => {
                selectedGrid.value = grid
                map.setBounds(boundsOfPath(AMap, coords))
              })

              // 标注（默认隐藏）
              if (grid.gridName) {
                let cx = 0, cy = 0
                for (const [x, y] of coords) { cx += x; cy += y }
                cx /= coords.length; cy /= coords.length
                const label = new AMap.Text({
                  position: [cx, cy], text: grid.gridName, anchor: 'center',
                  style: { 'background': 'rgba(255,255,255,0.9)', 'border': '1px solid rgba(2,132,199,0.3)',
                    'border-radius': '4px', 'padding': '2px 6px', 'font-size': '11px', 'color': '#075985' },
                  map
                })
                label.hide()
                labelMarkerList.push(label)
              }
            }
          } catch (e) {}
        }
        if (grid.children) drawLarge(grid.children)
      }
    }

    // 小网格（level 3）：点线描边 + 按紧急度透色填充
    const drawSmall = (nodes: any[]) => {
      for (const grid of nodes) {
        if (grid.gridLevel === 3 && grid.roiJson) {
          try {
            const coords = JSON.parse(grid.roiJson)
            if (Array.isArray(coords) && coords.length >= 3) {
              const lv = grid.urgencyLevel || computeGridUrgency(coords, events.value)
              const st = GRID_STYLE_SMALL[lv] || GRID_STYLE_SMALL.NONE
              const lvText = lv ? urgencyText(lv) : '平稳'
              const myId = ++hoverId
              const polygon = new AMap.Polygon({
                path: coords, fillColor: st.fill, fillOpacity: st.fillOpacity,
                strokeColor: st.stroke, strokeWeight: 1.2, strokeStyle: 'dotted',
                strokeOpacity: 0.85, zIndex: 5, bubble: false, map
              })
              gridPolygonList.push(polygon)
              gridPolygonMap[Number(grid.id)] = polygon
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: st.hoverOpacity, strokeWeight: 1.8, strokeOpacity: 1, zIndex: 20 })
                crosshair.visible = true
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km² · ${lvText}`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => throttledHoverMove(e))
              polygon.on('mouseout', () => {
                if (hoverInfo.id !== myId) return
                polygon.setOptions({ fillOpacity: st.fillOpacity, strokeWeight: 1.2, strokeOpacity: 0.85, zIndex: 5 })
                crosshair.visible = false
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

    // 事件标记：红色紧急保留 DOM 六边形脉冲动画（数量少）；其余走 LabelsLayer GPU 批量渲染
    evtLabelsLayer = new AMap.LabelsLayer({ zooms: [3, 20], zIndex: 12, collision: false, animation: false })
    map.add(evtLabelsLayer)
    const plainMarkers: any[] = []
    for (const evt of events.value) {
      if (!evt.longitude || !evt.latitude) continue
      if (evt.urgencyLevel === 'RED') {
        const marker = new AMap.Marker({
          position: [evt.longitude, evt.latitude], zIndex: 13,
          content: `<div class="evt-hex" style="--hc:${urgencyColor('RED')}">
            <span class="hex-pulse"></span><span class="hex-pulse hp2"></span>
            <span class="hex-core"></span></div>`,
          offset: new AMap.Pixel(-10, -10), map, extData: evt
        })
        redPulseMarkers.push(marker)
        marker.on('mouseover', (e: any) => {
          hoverInfo.visible = true; throttledHoverMove(e)
          hoverInfo.name = evt.title || '事件'; hoverInfo.id = ++hoverId
        })
        marker.on('mousemove', (e: any) => throttledHoverMove(e))
        marker.on('mouseout', () => { hoverInfo.visible = false })
        marker.on('click', () => { selectedEvent.value = evt })
      } else {
        const lm = new AMap.LabelMarker({
          position: [evt.longitude, evt.latitude], zIndex: 12, extData: evt,
          icon: { image: HEX_ICON[evt.urgencyLevel] || HEX_ICON.GREEN, size: [22, 22], anchor: 'center' }
        })
        lm.on('mouseover', (e: any) => {
          hoverInfo.visible = true; throttledHoverMove(e)
          hoverInfo.name = evt.title || '事件'; hoverInfo.id = ++hoverId
        })
        lm.on('mousemove', (e: any) => throttledHoverMove(e))
        lm.on('mouseout', () => { hoverInfo.visible = false })
        lm.on('click', () => { selectedEvent.value = evt })
        plainMarkers.push(lm)
      }
    }
    evtLabelsLayer.add(plainMarkers)

    // 缩放级别聚合：缩小显示数量气泡、放大展开单点
    rebuildClusters(AMap)
    map.on('zoomchange', syncClusterVisibility)
    syncClusterVisibility()
  } catch (e: any) {
    loadError.value = '地图初始化失败: ' + (e?.message || e)
  }
}

// ==================== 图表 ====================
function renderRing() {
  const el = document.getElementById('chartRing')
  if (!el) return
  if (!chartRing) chartRing = echarts.init(el)
  const filterKey = ringFilter.value
  const data = levelRows.value.map((lv: any) => ({
    value: lv.count, name: lv.name, itemStyle: {
      color: lv.color,
      opacity: filterKey && filterKey !== lv.key ? 0.2 : 1
    }
  }))
  chartRing.setOption({
    backgroundColor: 'transparent',
    tooltip: { show: false },
    graphic: [
      { type: 'text', left: 'center', top: '32%', style: { text: '事件总数', fill: '#94a3b8', fontSize: 11, textAlign: 'center' } },
      { type: 'text', left: 'center', top: '42%', style: { text: String(total.value), fill: '#0284c7', fontSize: 24, fontWeight: 700, textAlign: 'center' } }
    ],
    series: [{
      type: 'pie', radius: ['52%', '74%'], center: ['50%', '50%'],
      avoidLabelOverlap: false, silent: true,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false }, data
    }]
  })
}

function renderTrend(days: string[], values: number[]) {
  const el = document.getElementById('chartTrend')
  if (!el || el.offsetWidth === 0 || el.offsetHeight === 0) return
  if (!chartTrend) chartTrend = echarts.init(el)
  const maxIdx = values.indexOf(Math.max(...values))
  chartTrend.setOption({
    backgroundColor: 'transparent',
    grid: { left: 8, right: 14, top: 16, bottom: 6, containLabel: true },
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e2e8f0', textStyle: { color: '#334155', fontSize: 12 } },
    xAxis: {
      type: 'category', data: days, boundaryGap: false,
      axisLine: { lineStyle: { color: '#cbd5e1' } }, axisTick: { show: false },
      axisLabel: { color: '#64748b', fontSize: 10 }
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { lineStyle: { color: '#eef2f7' } } },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 5,
      lineStyle: { color: '#0284c7', width: 2.5, shadowColor: 'rgba(2,132,199,0.3)', shadowBlur: 6 },
      itemStyle: { color: '#0284c7', borderColor: '#fff', borderWidth: 1.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(2,132,199,0.22)' }, { offset: 1, color: 'rgba(2,132,199,0.01)' }]) },
      markPoint: {
        data: [{ coord: [maxIdx, values[maxIdx]], value: values[maxIdx] }],
        symbolSize: 36, itemStyle: { color: '#0284c7' },
        label: { color: '#fff', fontSize: 10 }
      }
    }]
  })
}

function renderRank(ranking: any[]) {
  const el = document.getElementById('chartRank')
  if (!el || el.offsetWidth === 0 || el.offsetHeight === 0) return
  if (!chartRank) chartRank = echarts.init(el)
  const top = ranking.slice(0, 8).reverse()
  chartRank.setOption({
    backgroundColor: 'transparent',
    grid: { left: 70, right: 34, top: 8, bottom: 6 },
    xAxis: { type: 'value', axisLabel: { color: '#94a3b8', fontSize: 10 }, splitLine: { lineStyle: { color: '#eef2f7' } } },
    yAxis: {
      type: 'category', data: top.map((r: any) => r.gridName),
      axisLabel: { color: '#475569', fontSize: 11 }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false }
    },
    series: [{
      type: 'bar', barWidth: 10, data: top.map((r: any) => r.populationCount),
      itemStyle: { borderRadius: [0, 5, 5, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: 'rgba(2,132,199,0.2)' }, { offset: 1, color: '#0284c7' }]) },
      label: { show: true, position: 'right', color: '#334155', fontSize: 10 },
      animationDelay: (idx: number) => idx * 80
    }]
  })
}

function renderSparkBottom() {
  const el = document.getElementById('spark-bottom')
  if (!el) return
  const t = realTrend()
  if (!t) return
  if (!chartSparkBottom) chartSparkBottom = echarts.init(el)
  chartSparkBottom.setOption({
    backgroundColor: 'transparent',
    grid: { left: 0, right: 0, top: 2, bottom: 0 },
    xAxis: { type: 'category', show: false, data: t.days, boundaryGap: false },
    yAxis: { type: 'value', show: false, min: 0, max: Math.max(...t.values) + 2 },
    series: [{
      type: 'line', data: t.values, smooth: true, symbol: 'none',
      lineStyle: { color: '#0284c7', width: 1.5 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(2,132,199,0.25)' }, { offset: 1, color: 'rgba(2,132,199,0)' }]) }
    }]
  })
}

function renderCharts(stats: any) {
  renderRing()
  const trend = realTrend()
  if (trend) renderTrend(trend.days, trend.values)
  if (stats?.populationRanking?.length) {
    hasPopulation.value = true
    renderRank(stats.populationRanking)
  }
  renderSparkBottom()
}

// ==================== 数据加载 ====================
async function loadData() {
  const errors: string[] = []
  let anyLive = false

  try {
    const data = await getDashboardOverview()
    if (data && Object.keys(data).length) {
      overview.value = data
      if (data.communityArea) areaRaw.value = Number(data.communityArea)
      anyLive = true
    }
  } catch (e: any) { errors.push('概览统计接口 /community/dashboard/overview：' + (e?.message || e)) }

  try {
    const big = await getBigScreenData()
    if (big && typeof big === 'object') {
      overview.value = { ...overview.value, ...big }
      if (big.communityArea) areaRaw.value = Number(big.communityArea)
      anyLive = true
    }
  } catch (e: any) { errors.push('大屏聚合接口 /community/dashboard/big-screen：' + (e?.message || e)) }

  let stats: any = {}
  try {
    const s = await getGridStats()
    if (s) stats = s
    anyLive = true
  } catch (e: any) { errors.push('网格统计接口 /community/dashboard/grid-stats：' + (e?.message || e)) }

  let tree: any[] = []
  try {
    const t = await getGridTree()
    if (Array.isArray(t) && t.length) tree = t
  } catch (e: any) { errors.push('网格树接口 /community/grids/tree：' + (e?.message || e)) }

  try {
    const r = await getEvents({ excludeHidden: true })
    if (r?.items?.length) { events.value = r.items; anyLive = true }
  } catch (e: any) { errors.push('事件列表接口 /events：' + (e?.message || e)) }

  isLive.value = anyLive
  if (!anyLive) {
    // 所有接口均失败/为空：逐条展示具体错误，不再用演示数据兜底
    fatalErrors.value = errors.length ? errors : ['所有数据接口返回空数据']
    loadError.value = ''
  } else {
    fatalErrors.value = []
    loadError.value = errors.length ? errors.join('；') : ''
  }

  allGrids.value = []
  const flatten = (nodes: any[]) => {
    for (const n of nodes) { allGrids.value.push(n); if (n.children) flatten(n.children) }
  }
  flatten(tree)
  const community = allGrids.value.find((g: any) => g.gridLevel === 1)
  if (community?.area) areaRaw.value = Number(community.area)

  return { tree, stats }
}

// 重试：重新加载全部数据并重建地图与图表
async function reload() {
  fatalErrors.value = []
  loadError.value = ''
  const { tree, stats } = await loadData()
  if (fatalErrors.value.length) return
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
  await initMap(tree)
  renderCharts(stats)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  tickClock()
  clockTimer = window.setInterval(tickClock, 1000)
  document.addEventListener('fullscreenchange', onFullscreenChange)
  resizeHandler = () => { chartRing?.resize?.(); chartTrend?.resize?.(); chartRank?.resize?.(); chartSparkBottom?.resize?.() }
  window.addEventListener('resize', resizeHandler)

  const { tree, stats } = await loadData()
  if (fatalErrors.value.length) return
  await initMap(tree)
  renderCharts(stats)
})

onUnmounted(() => {
  if (clockTimer) window.clearInterval(clockTimer)
  document.removeEventListener('fullscreenchange', onFullscreenChange)
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  if (mapInstance) { mapInstance.destroy(); mapInstance = null }
  ;[chartRing, chartTrend, chartRank, chartSparkBottom].forEach(c => { if (c) { c.dispose(); c = null } })
})

// 标签切换时重渲染图表
watch(leftTab, async (v) => {
  await nextTick()
  if (v === 'ring') { renderRing(); setTimeout(() => chartRing?.resize?.(), 50) }
  else if (v === 'trend') {
    const t = realTrend(); if (t) renderTrend(t.days, t.values)
    setTimeout(() => chartTrend?.resize?.(), 50)
  }
  else if (v === 'rank' && hasPopulation.value) {
    const stats = await getGridStats().catch(() => null)
    if (stats?.populationRanking?.length) { renderRank(stats.populationRanking); setTimeout(() => chartRank?.resize?.(), 50) }
  }
})

// ==================== v4 差异化交互：左侧 Dock 工具栏 ====================
const layerPanel = ref(false)

function toggleDock(key: 'ring' | 'trend' | 'rank') {
  if (leftOpen.value && !layerPanel.value && leftTab.value === key) {
    leftOpen.value = false
    return
  }
  leftOpen.value = true
  layerPanel.value = false
  leftTab.value = key
}
function toggleLayerDock() {
  if (leftOpen.value && layerPanel.value) {
    leftOpen.value = false
    return
  }
  layerPanel.value = true
  leftOpen.value = true
}
</script>
<style scoped>
/* ================= 大屏容器 ================= */
.dash-screen {
  position: relative; margin: -24px; height: calc(100vh - 56px); overflow: hidden;
  background: linear-gradient(165deg, #eef4fa 0%, #f8fafc 45%, #eef4fa 100%);
}
.dash-screen:fullscreen { height: 100vh; }
.dash-map { position: absolute; inset: 0; z-index: 0; width: 100%; height: 100%; }

/* ================= 背景装饰 ================= */
.bg-decor { position: absolute; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(2,132,199,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(2,132,199,0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  -webkit-mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.5), transparent 80%);
  mask-image: radial-gradient(ellipse at center, rgba(0,0,0,0.5), transparent 80%);
}
.bg-glow { position: absolute; border-radius: 50%; filter: blur(90px); }
.bg-glow-1 { width: 520px; height: 520px; background: rgba(2,132,199,0.08); top: -160px; left: 32%; }
.bg-glow-2 { width: 460px; height: 460px; background: rgba(56,189,248,0.08); bottom: -150px; right: 6%; }

/* ================= 十字准星 ================= */
.crosshair-v, .crosshair-h { position: absolute; z-index: 6; pointer-events: none; }
.crosshair-v {
  width: 1px; top: 48px; bottom: 64px;
  background: linear-gradient(180deg, transparent, rgba(2,132,199,0.35) 20%, rgba(2,132,199,0.35) 80%, transparent);
}
.crosshair-h {
  height: 1px; left: 0; right: 0;
  background: linear-gradient(90deg, transparent, rgba(2,132,199,0.35) 15%, rgba(2,132,199,0.35) 85%, transparent);
}

/* ================= 顶部栏（48px） ================= */
.topbar {
  position: absolute; top: 0; left: 0; right: 0; height: 48px; z-index: 30;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; pointer-events: none;
  background: linear-gradient(180deg, rgba(255,255,255,0.92) 0%, rgba(255,255,255,0.4) 70%, transparent 100%);
  border-bottom: 1px solid rgba(2,132,199,0.08);
}
.topbar-brand { display: flex; align-items: center; gap: 8px; pointer-events: auto; }
.brand-dot { width: 8px; height: 8px; border-radius: 50%; animation: pulse 2s ease-in-out infinite; }
.brand-dot.live { background: #22c55e; box-shadow: 0 0 8px rgba(34,197,94,0.6); }
.brand-dot.demo { background: #f59e0b; box-shadow: 0 0 8px rgba(245,158,11,0.6); }
@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
.topbar-brand i { color: #0284c7; font-size: 16px; }
.brand-text { font-size: 15px; font-weight: 700; color: #0f172a; letter-spacing: 1px; }
.topbar-sub { font-size: 11px; color: #64748b; letter-spacing: 2px; pointer-events: auto; }
.topbar-right { display: flex; align-items: center; gap: 12px; pointer-events: auto; }
.tb-status {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 11px; padding: 3px 10px; border-radius: 12px;
  background: rgba(255,255,255,0.85); border: 1px solid rgba(2,132,199,0.16); color: #64748b;
}
.tb-status i { font-size: 7px; }
.tb-status.live i { color: #22c55e; }
.tb-status.demo i { color: #f59e0b; }
.tb-clock { text-align: right; }
.tb-time { font-size: 16px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; line-height: 1; }
.tb-date { font-size: 10px; color: #64748b; display: block; margin-top: 2px; }
.tb-btn {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.88); border: 1px solid rgba(2,132,199,0.20); border-radius: 8px;
  color: #0284c7; font-size: 13px; cursor: pointer; transition: all 0.2s;
}
.tb-btn:hover { background: #0284c7; color: #fff; }

/* ================= 玻璃面板通用 ================= */
.glass-panel {
  background: rgba(255,255,255,0.92); backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
  border: 1px solid rgba(2,132,199,0.12); border-radius: 16px;
  box-shadow: 0 10px 32px rgba(15,23,42,0.09); color: #334155;
}
.panel-empty { font-size: 12px; color: #94a3b8; text-align: center; padding: 18px 0; margin: 0; }

/* ================= 左侧 Dock 工具栏 ================= */
.left-dock {
  position: absolute; left: 16px; top: 96px; z-index: 24;
  display: flex; flex-direction: column; gap: 4px; padding: 8px 6px;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(2,132,199,0.14); border-radius: 14px;
  box-shadow: 0 8px 26px rgba(15,23,42,0.08); pointer-events: auto;
}
.dock-logo {
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #0284c7, #0ea5e9); border-radius: 10px; color: #fff; font-size: 15px;
  box-shadow: 0 4px 12px rgba(2,132,199,0.35); margin-bottom: 2px;
}
.dock-btn {
  width: 40px; padding: 6px 0 5px; border: none; border-radius: 9px; background: none;
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  font-size: 10px; color: #64748b; cursor: pointer; transition: all 0.18s;
}
.dock-btn i { font-size: 14px; }
.dock-btn:hover { background: rgba(2,132,199,0.07); color: #0284c7; }
.dock-btn.active {
  background: rgba(2,132,199,0.12); color: #0284c7; font-weight: 600;
  box-shadow: inset 0 0 0 1px rgba(2,132,199,0.22);
}
.dock-btn.quiet { opacity: 0.45; }
.dock-btn.quiet.active { opacity: 1; }
.dock-divider { height: 1px; background: rgba(2,132,199,0.12); margin: 2px 2px; }

/* ================= 左侧内容面板 ================= */
.left-widget {
  position: absolute; left: 76px; top: 96px; width: 302px; max-height: calc(100vh - 176px); z-index: 24;
  display: flex; flex-direction: column; overflow: hidden;
  opacity: 0; transform: translateX(-18px); pointer-events: none;
  transition: opacity 0.28s ease, transform 0.28s ease;
}
.left-widget.open { opacity: 1; transform: translateX(0); pointer-events: auto; }
.widget-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px 10px; border-bottom: 1px solid rgba(2,132,199,0.10); flex-shrink: 0;
}
.widget-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #075985; }
.widget-title i { color: #0284c7; }
.widget-fold {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
  border: none; background: rgba(2,132,199,0.06); border-radius: 7px;
  color: #94a3b8; font-size: 12px; cursor: pointer; transition: all 0.18s;
}
.widget-fold:hover { background: rgba(2,132,199,0.14); color: #0284c7; }
.tab-body { flex: 0 1 auto; overflow-y: auto; padding: 12px 14px; min-height: 0; }
.tab-body::-webkit-scrollbar { width: 4px; }
.tab-body::-webkit-scrollbar-thumb { background: rgba(2,132,199,0.22); border-radius: 2px; }
.tab-pane { display: flex; flex-direction: column; gap: 8px; }
.pane-sub {
  font-size: 11px; color: #64748b; display: flex; align-items: center; gap: 5px;
  padding: 6px 10px; border-radius: 8px; background: rgba(241,245,249,0.7);
}
.pane-sub i { color: #0284c7; }

/* 图层开关 */
.panel-tools {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px 14px;
  padding: 10px 12px; border-radius: 12px;
  background: rgba(241,245,249,0.75); border: 1px solid rgba(2,132,199,0.08);
}
.pt-title { font-size: 11px; font-weight: 600; color: #075985; display: inline-flex; align-items: center; gap: 4px; width: 100%; margin-bottom: 2px; }
.pt-title i { color: #0284c7; font-size: 10px; }
.pt-row { display: inline-flex; align-items: center; gap: 5px; cursor: pointer; font-size: 11px; color: #475569; }
.pt-row input { display: none; }
.pt-switch {
  width: 26px; height: 13px; border-radius: 7px; background: #cbd5e1; position: relative; transition: background 0.2s; flex-shrink: 0;
}
.pt-switch i { position: absolute; top: 1px; left: 1px; width: 11px; height: 11px; border-radius: 50%; background: #fff; transition: left 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.2); }
.pt-row input:checked ~ .pt-switch { background: #0284c7; }
.pt-row input:checked ~ .pt-switch i { left: 14px; }
.pt-label { white-space: nowrap; }

/* 网格预警图例 */
.grid-legend {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px;
  padding: 8px 10px; border-radius: 10px;
  background: rgba(241,245,249,0.6); border: 1px solid rgba(2,132,199,0.08); font-size: 10px; color: #64748b;
}
.gl-title { display: inline-flex; align-items: center; gap: 3px; font-weight: 600; color: #075985; }
.gl-title i { color: #0284c7; font-size: 10px; }
.gl-item { display: inline-flex; align-items: center; gap: 3px; }
.gl-item i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

/* 环形图 + 图例 */
.chart-ring { height: 168px; margin: 2px 0; }
.ring-legend { display: flex; flex-direction: column; gap: 9px; }
.legend-row {
  display: flex; align-items: center; gap: 8px; font-size: 12px; cursor: pointer;
  padding: 3px 4px; border-radius: 8px; transition: background 0.15s;
}
.legend-row:hover { background: rgba(2,132,199,0.05); }
.legend-row.dim { opacity: 0.35; }
.legend-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
.legend-name { width: 30px; color: #475569; flex-shrink: 0; font-size: 11px; }
.legend-track { flex: 1; height: 6px; background: #eef2f7; border-radius: 3px; overflow: hidden; }
.legend-track i { display: block; height: 100%; border-radius: 3px; transition: width 0.5s; }
.legend-num { width: 22px; text-align: right; font-weight: 600; color: #0f172a; font-size: 11px; }
.filter-hint {
  font-size: 11px; color: #0284c7; display: flex; align-items: center; gap: 6px;
  padding: 4px 8px; background: rgba(2,132,199,0.06); border-radius: 8px;
}
.filter-hint button { border: none; background: none; color: #ef4444; cursor: pointer; font-size: 11px; margin-left: auto; }

/* 趋势/排名 */
.chart-trend { height: 300px; }
.chart-rank { height: 300px; }

/* ================= 右侧时间轴面板 ================= */
.timeline-panel {
  position: absolute; right: 16px; top: 96px; width: 330px; height: 460px; max-height: calc(100vh - 176px); z-index: 24;
  display: flex; flex-direction: column; overflow: hidden;
  opacity: 0; transform: translateX(18px); pointer-events: none;
  transition: opacity 0.28s ease, transform 0.28s ease;
}
.timeline-panel.open { opacity: 1; transform: translateX(0); pointer-events: auto; }
/* 收起时收缩为悬浮圆钮，保留展开入口 */
.timeline-panel:not(.open) {
  width: 44px; height: 44px; opacity: 1; transform: none; pointer-events: auto;
  justify-content: center; align-items: center; overflow: visible;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(2,132,199,0.18); border-radius: 12px;
  box-shadow: 0 6px 18px rgba(15,23,42,0.10);
}
.timeline-panel:not(.open) .panel-head-r,
.timeline-panel:not(.open) .filter-chips,
.timeline-panel:not(.open) .search-box,
.timeline-panel:not(.open) .timeline-list { display: none; }
.timeline-panel:not(.open) .tl-foot { padding: 0; }
.timeline-panel:not(.open) .tl-fold {
  border: none; background: none; padding: 0; width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  color: #0284c7; font-size: 12px;
}
.timeline-panel:not(.open) .tl-fold:hover { background: rgba(2,132,199,0.08); color: #0284c7; }
.panel-head-r { display: flex; align-items: center; justify-content: space-between; padding: 13px 14px 8px; flex-shrink: 0; }
.panel-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #075985; padding-left: 8px; border-left: 3px solid #0284c7; }
.panel-title i { color: #0284c7; }
.panel-count { font-size: 11px; color: #94a3b8; }
.filter-chips { display: flex; gap: 6px; padding: 0 14px 10px; flex-wrap: wrap; flex-shrink: 0; }
.chip {
  padding: 4px 12px; border-radius: 12px; border: 1px solid #e2e8f0; background: rgba(255,255,255,0.8);
  font-size: 11px; color: #64748b; cursor: pointer; transition: all 0.2s;
}
.chip:hover { border-color: rgba(2,132,199,0.4); }
.chip.active { font-weight: 600; }
.search-box { margin: 0 14px 10px; display: flex; align-items: center; gap: 8px; padding: 6px 12px; border-radius: 10px; background: #f1f5f9; border: 1px solid #e2e8f0; flex-shrink: 0; }
.search-box i { color: #94a3b8; font-size: 11px; }
.search-box input { border: none; background: none; outline: none; flex: 1; font-size: 12px; color: #334155; }

/* 垂直时间轴 */
.timeline-list { flex: 1; overflow-y: auto; padding: 2px 14px 8px; min-height: 0; }
.timeline-list::-webkit-scrollbar { width: 4px; }
.timeline-list::-webkit-scrollbar-thumb { background: rgba(2,132,199,0.25); border-radius: 2px; }
.tl-item { display: flex; gap: 10px; }
.tl-axis { display: flex; flex-direction: column; align-items: center; width: 14px; flex-shrink: 0; padding-top: 4px; }
.tl-dot {
  width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; z-index: 1;
  transition: transform 0.18s;
}
.tl-item:hover .tl-dot { transform: scale(1.35); }
.tl-line { flex: 1; width: 2px; margin: 3px 0 -3px; background: linear-gradient(180deg, rgba(2,132,199,0.22), rgba(2,132,199,0.05)); }
.tl-item:last-child .tl-line { display: none; }
.tl-card {
  flex: 1; min-width: 0; margin-bottom: 10px; padding: 8px 10px; border-radius: 10px;
  background: rgba(255,255,255,0.75); border: 1px solid #eef2f7; border-left-width: 3px;
  cursor: pointer; transition: all 0.18s;
}
.tl-card:hover { background: rgba(2,132,199,0.05); transform: translateX(-2px); box-shadow: 0 4px 14px rgba(2,132,199,0.08); }
.tl-item.active .tl-card { background: rgba(2,132,199,0.08); border-color: rgba(2,132,199,0.25); }
.tl-top { display: flex; align-items: center; justify-content: space-between; gap: 6px; margin-bottom: 3px; }
.evt-tag { flex-shrink: 0; font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: 4px; line-height: 1.3; }
.tl-time { font-size: 10px; color: #94a3b8; display: inline-flex; align-items: center; gap: 3px; white-space: nowrap; }
.event-title { margin: 0; font-size: 12px; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event-meta { margin: 2px 0 0; font-size: 10px; color: #94a3b8; display: flex; align-items: center; gap: 4px; }
.event-meta i { font-size: 9px; }
.tl-foot { flex-shrink: 0; padding: 8px 14px 12px; text-align: center; }
.tl-fold {
  border: 1px solid rgba(2,132,199,0.18); background: rgba(255,255,255,0.7); color: #0284c7;
  font-size: 11px; padding: 5px 16px; border-radius: 8px; cursor: pointer; transition: all 0.2s;
}
.tl-fold:hover { background: #0284c7; color: #fff; }

/* ================= 底部 Dock ================= */
.dock-bar {
  position: absolute; left: 0; right: 0; bottom: 0; height: 64px; z-index: 26;
  display: flex; align-items: center; gap: 14px; padding: 0 14px;
  background: rgba(255,255,255,0.9); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border-top: 1px solid rgba(2,132,199,0.12);
  box-shadow: 0 -6px 24px rgba(15,23,42,0.05); pointer-events: none;
}
.dock-left { display: flex; align-items: center; gap: 10px; pointer-events: auto; flex-shrink: 0; }
.dk-status {
  display: inline-flex; align-items: center; gap: 5px; font-size: 11px; font-weight: 600;
  padding: 5px 12px; border-radius: 12px; background: rgba(241,245,249,0.8);
  border: 1px solid rgba(2,132,199,0.14); color: #64748b;
}
.dk-status i { font-size: 7px; }
.dk-status.live i { color: #22c55e; }
.dk-status.demo i { color: #f59e0b; }
.dk-sys { font-size: 11px; color: #64748b; display: inline-flex; align-items: center; gap: 4px; }
.dk-sys i { color: #0284c7; font-size: 10px; }
.dk-sys.urgent { color: #dc2626; }
.dk-sys.urgent i { color: #ef4444; }
.dock-spark { display: flex; align-items: center; gap: 6px; padding-left: 10px; border-left: 1px solid #e2e8f0; }
.ds-label { font-size: 11px; color: #64748b; }
.ds-num { font-size: 15px; font-weight: 700; font-variant-numeric: tabular-nums; }
.ds-chart { width: 60px; height: 20px; }

.dock-kpis { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; min-width: 0; pointer-events: auto; }
.dk-pill {
  display: flex; align-items: center; gap: 7px; padding: 6px 12px; border-radius: 11px;
  background: rgba(241,245,249,0.75); border: 1px solid rgba(2,132,199,0.10);
  transition: transform 0.18s, box-shadow 0.18s; white-space: nowrap;
}
.dk-pill:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(2,132,199,0.12); }
.dk-pill i { font-size: 13px; }
.dk-pill b { font-size: 15px; font-weight: 700; color: #0f172a; font-variant-numeric: tabular-nums; }
.dk-pill b em { font-size: 9px; color: #64748b; font-style: normal; margin-left: 2px; font-weight: 400; }
.dk-pill span { font-size: 10px; color: #64748b; }

.dock-ticker { width: 30%; display: flex; align-items: center; gap: 8px; pointer-events: auto; min-width: 0; }
.ticker-label {
  font-size: 11px; font-weight: 600; color: #075985; display: inline-flex; align-items: center; gap: 4px; flex-shrink: 0;
}
.ticker-label i { color: #0284c7; }
.ticker-view { flex: 1; overflow: hidden; -webkit-mask-image: linear-gradient(90deg, transparent, #000 4%, #000 96%, transparent); mask-image: linear-gradient(90deg, transparent, #000 4%, #000 96%, transparent); }
.ticker-run { display: inline-flex; gap: 34px; white-space: nowrap; animation: tickerMove 42s linear infinite; }
.ticker-run:hover { animation-play-state: paused; }
@keyframes tickerMove { from { transform: translateX(0); } to { transform: translateX(-50%); } }
.ticker-item { font-size: 11px; color: #64748b; display: inline-flex; align-items: center; gap: 5px; }
.ticker-item i { font-size: 7px; flex-shrink: 0; }

/* ================= 地图控制按钮组 ================= */
.map-controls {
  position: absolute; right: 16px; bottom: 80px; z-index: 22;
  display: flex; flex-direction: column; gap: 6px; pointer-events: auto;
}
.mc-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(6px); -webkit-backdrop-filter: blur(6px);
  border: 1px solid rgba(2,132,199,0.18); border-radius: 9px;
  color: #0284c7; font-size: 13px; cursor: pointer; transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(15,23,42,0.06);
}
.mc-btn:hover { background: #0284c7; color: #fff; transform: scale(1.05); }
.mc-btn.active { background: #0284c7; color: #fff; border-color: #0284c7; box-shadow: 0 2px 12px rgba(2,132,199,0.35); }

/* ================= 网格详情 HUD 弹窗（地图上方，与事件流顶部对齐） =================
   定位策略：glass-panel 自身开关（selectedGrid v-if）打开时，按事件流开关自动让位：
     · .is-open   → 事件流展开  → right: 366px (16 距右 + 330 宽 + 20 间距)
     · .is-folded → 事件流收起  → right:  80px (16 距右 +  44 宽 + 20 间距，与 tl-fold 保持 20px)
*/
.grid-hud {
  position: absolute; z-index: 42; top: 96px; right: 366px;
  width: 320px; max-width: calc(100% - 40px); padding: 16px 18px;
  transition: right 0.28s ease, opacity 0.28s ease, transform 0.28s ease;
}
.grid-hud.is-folded { right: 80px; }
.hud-close {
  position: absolute; top: 10px; right: 10px; border: none; background: none; color: #94a3b8;
  font-size: 14px; cursor: pointer; padding: 4px;
}
.hud-close:hover { color: #0f172a; }
.hud-head { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.hud-urgency { width: 4px; height: 28px; border-radius: 2px; flex-shrink: 0; }
.hud-head h3 { margin: 0; font-size: 16px; font-weight: 700; color: #0f172a; }
.hud-level { font-size: 11px; color: #94a3b8; padding: 2px 8px; border-radius: 4px; background: rgba(2,132,199,0.08); }
.hud-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.hud-cell {
  display: flex; flex-direction: column; gap: 3px; padding: 9px 11px; border-radius: 10px;
  background: rgba(241,245,249,0.7); border: 1px solid rgba(2,132,199,0.08);
}
.hud-cell i { font-size: 12px; color: #0284c7; }
.hud-cell span { font-size: 10px; color: #94a3b8; }
.hud-cell b { font-size: 12px; color: #0f172a; font-weight: 600; }
.hud-events { margin-top: 12px; border-top: 1px solid #f1f5f9; padding-top: 8px; }
.he-title { font-size: 11px; font-weight: 600; color: #075985; margin-bottom: 6px; display: flex; align-items: center; gap: 5px; }
.he-title i { color: #0284c7; }
.he-item { display: flex; align-items: center; gap: 6px; padding: 4px 0; cursor: pointer; font-size: 11px; }
.he-item:hover { color: #0284c7; }
.he-tag { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.he-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #475569; }

/* ================= 事件详情气泡（右下角） ================= */
.event-pop {
  position: absolute; z-index: 40; right: 20px; bottom: 80px; width: 380px;
  max-width: calc(100% - 32px); padding: 14px 16px;
}
.pop-close {
  position: absolute; top: 10px; right: 10px; border: none; background: none; color: #94a3b8;
  font-size: 14px; cursor: pointer; padding: 4px;
}
.pop-close:hover { color: #0f172a; }
.pop-top { display: flex; align-items: center; gap: 8px; }
.pop-status { font-size: 11px; color: #64748b; }
.pop-title { margin: 8px 0 0; font-size: 14px; font-weight: 600; color: #0f172a; }
.pop-meta { font-size: 11px; color: #475569; display: flex; gap: 12px; margin-top: 4px; flex-wrap: wrap; }
.pop-meta i { color: #0284c7; margin-right: 2px; }
.pop-desc { margin: 8px 0 0; font-size: 11px; color: #64748b; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.pop-btn {
  margin-top: 10px; width: 100%; padding: 7px 0; border: none; border-radius: 9px;
  background: #0284c7; color: #fff; font-size: 12px; cursor: pointer; transition: background 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.pop-btn:hover { background: #0369a1; }

/* ================= 悬停提示 ================= */
.hover-tip {
  position: absolute; z-index: 45; transform: translate(-50%, calc(-100% - 12px));
  background: rgba(15,23,42,0.88); color: #fff; padding: 5px 11px; border-radius: 6px;
  font-size: 11px; font-weight: 600; pointer-events: none; white-space: nowrap;
  box-shadow: 0 4px 16px rgba(15,23,42,0.2);
}

/* ================= 错误提示 ================= */
.dash-error {
  position: absolute; z-index: 50; top: 56px; left: 50%; transform: translateX(-50%);
  background: rgba(254,226,226,0.94); backdrop-filter: blur(8px);
  border: 1px solid rgba(220,38,38,0.25); border-radius: 8px;
  color: #b91c1c; font-size: 12px; padding: 8px 16px; max-width: 70%;
  display: flex; align-items: center; gap: 8px;
}

/* 接口全部失败：居中紧凑错误卡片，逐条列出具体失败接口 */
.dash-fatal {
  position: absolute; inset: 0; z-index: 60;
  display: flex; align-items: center; justify-content: center;
  background: rgba(248,250,252,0.88); backdrop-filter: blur(6px);
}
.dash-fatal-box {
  width: 400px; max-width: 88vw; background: #fff; border-radius: 12px;
  border: 1px solid rgba(220,38,38,0.18); box-shadow: 0 12px 32px rgba(0,0,0,0.08);
  padding: 20px 22px;
}
.dash-fatal-head { display: flex; align-items: flex-start; gap: 10px; }
.dash-fatal-head > i { font-size: 26px; color: #dc2626; margin-top: 2px; }
.dash-fatal-head h2 { font-size: 15px; font-weight: 600; color: #1f2937; }
.dash-fatal-head p { font-size: 12px; color: #6b7280; margin-top: 3px; }
.dash-fatal-list {
  margin: 12px 0 0; padding: 10px 12px; list-style: none;
  background: #fef2f2; border-radius: 8px; max-height: 148px; overflow-y: auto;
}
.dash-fatal-list li {
  font-size: 12px; color: #b91c1c; line-height: 1.6; word-break: break-all;
  padding-left: 14px; position: relative; margin-bottom: 4px;
}
.dash-fatal-list li::before { content: ''; position: absolute; left: 2px; top: 7px; width: 5px; height: 5px; border-radius: 50%; background: #dc2626; }
.dash-fatal-list li:last-child { margin-bottom: 0; }
.dash-fatal-box button {
  margin-top: 14px; width: 100%; padding: 8px 0; border: none; border-radius: 6px;
  background: #0284c7; color: #fff; font-size: 13px; cursor: pointer;
}
.dash-fatal-box button:hover { background: #0369a1; }

/* ================= 过渡动画 ================= */
.hud-enter-active, .hud-leave-active { transition: opacity 0.25s, transform 0.25s, right 0.28s ease; }
.hud-enter-from, .hud-leave-to { opacity: 0; transform: translateY(10px) scale(0.96); }
.pop-enter-active, .pop-leave-active { transition: opacity 0.25s, transform 0.25s; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(16px); }
</style>

<style>
/* 六边形事件标记（非 scoped，marker content 动态注入） */
.evt-hex { position: relative; width: 20px; height: 20px; cursor: pointer; }
.hex-core {
  position: absolute; left: 50%; top: 50%; width: 10px; height: 10px;
  margin: -5px 0 0 -5px; z-index: 2;
  background: var(--hc, #0284c7);
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 1px 5px rgba(15,23,42,0.3);
}
.hex-pulse {
  position: absolute; left: 50%; top: 50%; width: 10px; height: 10px;
  margin: -5px 0 0 -5px; border-radius: 2px;
  background: var(--hc, #0284c7); opacity: 0.35;
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
  animation: hexPing 1.8s ease-out infinite;
}
.hex-pulse.hp2 { animation-delay: 0.6s; }
@keyframes hexPing {
  0% { transform: scale(1); opacity: 0.40; }
  100% { transform: scale(3.5); opacity: 0; }
}

/* 缩放级别聚合气泡 */
.evt-cluster {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(2,132,199,0.85); color: #fff; font-size: 13px; font-weight: 700;
  border: 2px solid rgba(255,255,255,0.9);
  box-shadow: 0 2px 10px rgba(2,132,199,0.45); cursor: pointer;
}
.evt-cluster.hot { background: rgba(239,68,68,0.88); box-shadow: 0 2px 12px rgba(239,68,68,0.5); }
</style>
