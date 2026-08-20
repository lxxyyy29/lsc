<template>
  <!-- 全域态势感知大屏 v3：浅色高级科技风 · 差异化交互版 -->
  <div ref="screenRef" class="dash-screen" :class="{ 'panel-folded-l': !leftOpen, 'panel-folded-r': !rightOpen }">
    <!-- 背景装饰：浅色渐变 + 极细网格 + 柔光 -->
    <div class="bg-decor">
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
    </div>

    <!-- 地图主体 -->
    <div id="gisMap" class="dash-map" @mousemove="onMapMouseMove" @mouseleave="crosshair.visible = false"></div>

    <!-- 十字准星线（hover 网格时显示，差异化交互） -->
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
          <i class="fas fa-circle"></i>{{ isLive ? '实时数据' : '演示数据' }}
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

    <!-- ============ KPI 浮动横条（顶部栏下方居中） ============ -->
    <div class="kpi-strip">
      <div class="kpi-pill" v-for="k in kpiList" :key="k.key">
        <i :class="k.icon" :style="{ color: k.color }"></i>
        <div class="kpi-body">
          <b>{{ k.value }}<em v-if="k.unit">{{ k.unit }}</em></b>
          <span>{{ k.label }}</span>
        </div>
        <div class="kpi-spark" v-if="k.spark" :id="'spark-' + k.key"></div>
      </div>
    </div>

    <!-- ============ 地图控制按钮组（右下角，不挡面板） ============ -->
    <div class="map-controls">
      <button class="mc-btn" @click="mapZoom(1)" title="放大"><i class="fas fa-plus"></i></button>
      <button class="mc-btn" @click="mapZoom(-1)" title="缩小"><i class="fas fa-minus"></i></button>
      <button class="mc-btn" @click="mapReset" title="重置视角"><i class="fas fa-compass"></i></button>
      <button class="mc-btn" @click="flyRandom" title="巡检"><i class="fas fa-route"></i></button>
    </div>

    <!-- ============ 左侧标签页面板（可折叠） ============ -->
    <aside class="side-panel left" :class="{ folded: !leftOpen }">
      <button class="panel-fold" @click="leftOpen = !leftOpen">
        <i :class="leftOpen ? 'fas fa-chevron-left' : 'fas fa-chevron-right'"></i>
      </button>
      <template v-if="leftOpen">
        <div class="panel-content">
          <div class="tab-head">
            <button v-for="t in leftTabs" :key="t.key"
                    class="tab-btn" :class="{ active: leftTab === t.key }"
                    @click="leftTab = t.key">
              <i :class="t.icon"></i>{{ t.label }}
            </button>
          </div>
          <div class="tab-body">
          <!-- 三色分级 -->
          <div v-show="leftTab === 'ring'" class="tab-pane">
            <!-- 图层开关（收进面板，避免悬浮遮挡） -->
            <div class="panel-tools">
              <span class="pt-title"><i class="fas fa-layer-group"></i>图层</span>
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
            </div>

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

            <!-- 网格预警色图例（收进面板底部） -->
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
          <div v-show="leftTab === 'trend'" class="tab-pane">
            <div id="chartTrend" class="chart-trend"></div>
          </div>
          <!-- 人口排名 -->
          <div v-show="leftTab === 'rank'" class="tab-pane">
            <div id="chartRank" class="chart-rank"></div>
            <p v-if="!hasPopulation" class="panel-empty">暂无人口数据</p>
          </div>
        </div>
        </div>
      </template>
    </aside>

    <!-- ============ 右侧事件面板（可折叠） ============ -->
    <aside class="side-panel right" :class="{ folded: !rightOpen }">
      <button class="panel-fold" @click="rightOpen = !rightOpen">
        <i :class="rightOpen ? 'fas fa-chevron-right' : 'fas fa-chevron-left'"></i>
      </button>
      <template v-if="rightOpen">
        <div class="panel-content">
        <div class="panel-head-r">
          <span class="panel-title"><i class="fas fa-bolt"></i>最新事件</span>
          <span class="panel-count">{{ filteredEvents.length }}/{{ events.length }}</span>
        </div>
        <!-- 筛选 chips -->
        <div class="filter-chips">
          <button v-for="c in urgencyChips" :key="c.key"
                  class="chip" :class="{ active: eventFilter === c.key }"
                  :style="eventFilter === c.key ? { borderColor: c.color, color: c.color, background: c.bg } : {}"
                  @click="eventFilter = c.key">
            {{ c.label }}
          </button>
        </div>
        <!-- 搜索 -->
        <div class="search-box">
          <i class="fas fa-search"></i>
          <input v-model="eventSearch" placeholder="搜索事件标题..." />
        </div>
        <!-- 事件列表 -->
        <div class="events-list">
          <div v-for="evt in filteredEvents" :key="evt.id" class="event-item"
               :class="{ active: selectedEvent?.id === evt.id }" @click="focusEvent(evt)">
            <span class="evt-tag" :style="{ background: urgencyBg(evt.urgencyLevel), color: urgencyColor(evt.urgencyLevel) }">
              {{ urgencyText(evt.urgencyLevel) }}
            </span>
            <div class="event-text">
              <p class="event-title">{{ evt.title }}</p>
              <p class="event-meta">
                <i class="fas" :class="statusIcon(evt.currentStatus)"></i>
                {{ statusLabel(evt.currentStatus) }} · {{ formatTime(evt.createdAt) }}
              </p>
            </div>
            <i v-if="evt.longitude && evt.latitude" class="fas fa-crosshairs event-locate"></i>
          </div>
          <p v-if="!filteredEvents.length" class="panel-empty">无匹配事件</p>
        </div>
        </div>
      </template>
    </aside>

    <!-- ============ 底部状态栏（含迷你 sparkline） ============ -->
    <div class="status-bar">
      <div class="sb-left">
        <span class="sb-item"><i class="fas fa-broadcast-tower"></i> 系统正常</span>
        <span class="sb-divider"></span>
        <span class="sb-item"><i class="fas fa-database"></i> {{ isLive ? '实时' : '演示' }}模式</span>
        <span class="sb-divider"></span>
        <span class="sb-item"><i class="fas fa-server"></i> 5 接口在线</span>
      </div>
      <div class="sb-center">
        <span class="sb-label">今日事件</span>
        <b class="sb-num" :style="{ color: total > 10 ? '#ef4444' : '#0284c7' }">{{ total }}</b>
        <div class="sb-spark" id="spark-bottom"></div>
      </div>
      <div class="sb-right">
        <span class="sb-item urgent" v-if="(overview.eventRed || 0) > 0">
          <i class="fas fa-fire"></i> 紧急 {{ overview.eventRed }}
        </span>
        <span class="sb-item"><i class="fas fa-clock"></i> 更新于 {{ clock.time }}</span>
      </div>
    </div>

    <!-- ============ 网格详情抽屉（右侧滑入，区别于居中弹窗） ============ -->
    <transition name="drawer">
      <div v-if="selectedGrid" class="grid-drawer glass-panel">
        <button class="drawer-close" @click="selectedGrid = null"><i class="fas fa-times"></i></button>
        <div class="drawer-header">
          <div class="drawer-urgency" :style="{ background: urgencyColor(gridUrgencyOf(selectedGrid)) }" v-if="gridUrgencyOf(selectedGrid)"></div>
          <h3>{{ selectedGrid.gridName }}</h3>
          <span class="drawer-level">{{
            selectedGrid.gridLevel === 2 ? '二级 · 大网格' :
            selectedGrid.gridLevel === 3 ? '三级 · 小网格' : '一级 · 社区'
          }}</span>
        </div>
        <div class="drawer-body">
          <div class="drawer-row" v-if="gridUrgencyOf(selectedGrid)">
            <i class="fas fa-exclamation-triangle" :style="{ color: urgencyColor(gridUrgencyOf(selectedGrid)) }"></i>
            <span>预警级别</span>
            <b :style="{ color: urgencyColor(gridUrgencyOf(selectedGrid)) }">{{ urgencyText(gridUrgencyOf(selectedGrid)) }}</b>
          </div>
          <div class="drawer-row">
            <i class="fas fa-user-shield"></i><span>负责人</span><b>{{ selectedGrid.managerName || '-' }}</b>
          </div>
          <div class="drawer-row">
            <i class="fas fa-expand-arrows-alt"></i><span>面积</span><b>{{ selectedGrid.area || '-' }} km²</b>
          </div>
          <div class="drawer-row" v-if="selectedGrid.populationCount">
            <i class="fas fa-users"></i><span>人口</span><b>{{ selectedGrid.populationCount }} 人</b>
          </div>
          <div class="drawer-row">
            <i class="fas fa-circle" :style="{ color: selectedGrid.status === 'ACTIVE' ? '#16a34a' : '#dc2626', fontSize: '8px' }"></i>
            <span>状态</span><b>{{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}</b>
          </div>
          <div class="drawer-row" v-if="selectedGrid.children?.length">
            <i class="fas fa-sitemap"></i><span>子网格</span><b>{{ selectedGrid.children.length }} 个</b>
          </div>
        </div>
        <div class="drawer-events" v-if="gridEvents(selectedGrid).length">
          <div class="de-title"><i class="fas fa-bolt"></i> 网格内事件 ({{ gridEvents(selectedGrid).length }})</div>
          <div class="de-item" v-for="e in gridEvents(selectedGrid)" :key="e.id" @click="focusEvent(e)">
            <span class="de-tag" :style="{ background: urgencyColor(e.urgencyLevel) }"></span>
            <span class="de-text">{{ e.title }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- ============ 事件详情卡片（底部居中，紧凑） ============ -->
    <transition name="slideup">
      <div v-if="selectedEvent" class="event-card glass-panel">
        <button class="card-close" @click="selectedEvent = null"><i class="fas fa-times"></i></button>
        <div class="card-top">
          <span class="evt-tag" :style="{ background: urgencyBg(selectedEvent.urgencyLevel), color: urgencyColor(selectedEvent.urgencyLevel) }">
            {{ urgencyText(selectedEvent.urgencyLevel) }}
          </span>
          <span class="card-status">{{ statusLabel(selectedEvent.currentStatus) }}</span>
        </div>
        <p class="card-title">{{ selectedEvent.title }}</p>
        <div class="card-meta">
          <span v-if="selectedEvent.address"><i class="fas fa-map-marker-alt"></i> {{ selectedEvent.address }}</span>
          <span v-if="selectedEvent.createdAt"><i class="fas fa-clock"></i> {{ formatTime(selectedEvent.createdAt) }}</span>
        </div>
        <p v-if="selectedEvent.description" class="card-desc">{{ selectedEvent.description }}</p>
        <button class="card-detail-btn" @click="goEventDetail(selectedEvent)">
          查看详情 <i class="fas fa-arrow-right"></i>
        </button>
      </div>
    </transition>

    <!-- 悬停提示 -->
    <div v-if="hoverInfo.visible" class="hover-tip" :style="{ left: hoverInfo.x + 'px', top: hoverInfo.y + 'px' }">
      {{ hoverInfo.name }}
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
 *  所有接口失败或为空时自动回退到内置演示数据，保证大屏始终完整可用。
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

// ==================== 演示数据兜底 ====================
const MOCK_CENTER: [number, number] = [113.9395, 22.9712]

function rectRoi(cx: number, cy: number, w: number, h: number): number[][] {
  return [[cx - w, cy - h], [cx + w, cy - h], [cx + w, cy + h], [cx - w, cy + h], [cx - w, cy - h]]
}

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
        urgencyLevel: demoLv[idx - 1],
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
let chartSparkBottom: any = null
let heatLayer: any = null
let hoverId = 0
let clockTimer: number | undefined
let resizeHandler: (() => void) | null = null
let gridPolygonList: any[] = []
let eventMarkerList: any[] = []
let labelMarkerList: any[] = []

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
const urgencyChips = [
  { key: 'ALL', label: '全部', color: '#0284c7', bg: 'rgba(2,132,199,0.08)' },
  { key: 'RED', label: '紧急', color: '#ef4444', bg: 'rgba(239,68,68,0.08)' },
  { key: 'YELLOW', label: '重点', color: '#f59e0b', bg: 'rgba(245,158,11,0.08)' },
  { key: 'GREEN', label: '一般', color: '#22c55e', bg: 'rgba(34,197,94,0.08)' }
] as const

const ringFilter = ref('')
const layerState = reactive({ grids: true, events: true, heatmap: false, labels: false })
const crosshair = reactive({ visible: false, x: 0, y: 0 })

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
const eventTotalNum = useCount(() => overview.value.eventTotal || MOCK_OVERVIEW.eventTotal)
const redEventNum = useCount(() => overview.value.eventRed || MOCK_OVERVIEW.eventRed)
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
  for (const m of eventMarkerList) {
    if (layerState.events) m.show()
    else m.hide()
  }
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
    const AMap = await AMapLoader.load({ key: '5e00e01d2d2b6ca9e1eed533a15572e4', version: '2.0', plugins: ['AMap.Polygon', 'AMap.Marker', 'AMap.Text', 'AMap.HeatMap'] })
    mapInstance = new AMap.Map('gisMap', {
      zoom: 14, center: MOCK_CENTER,
      mapStyle: 'amap://styles/normal',
      features: ['bg', 'road', 'building']
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
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: st.hoverOpacity, strokeWeight: 2.5, zIndex: 20 })
                crosshair.visible = true
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km² · ${lvText}`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => {
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
              })
              polygon.on('mouseout', () => {
                if (hoverInfo.id !== myId) return
                polygon.setOptions({ fillOpacity: st.fillOpacity, strokeWeight: 1.5, zIndex: 5 })
                crosshair.visible = false
                hoverInfo.visible = false
              })
              polygon.on('click', () => {
                selectedGrid.value = grid
                if (grid.roiJson) {
                  const c = JSON.parse(grid.roiJson)
                  map.setBounds(new AMap.Bounds(c[0], c[2]))
                }
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
              polygon.on('mouseover', (e: any) => {
                polygon.setOptions({ fillOpacity: st.hoverOpacity, strokeWeight: 1.8, strokeOpacity: 1, zIndex: 20 })
                crosshair.visible = true
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.visible = true; hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
                hoverInfo.name = `${grid.gridName} · ${grid.area} km² · ${lvText}`
                hoverInfo.id = myId
              })
              polygon.on('mousemove', (e: any) => {
                const px = map.lngLatToContainer(e.lnglat)
                crosshair.x = px.getX(); crosshair.y = px.getY()
                hoverInfo.x = px.getX(); hoverInfo.y = px.getY()
              })
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

    // 事件标记：六边形脉冲（差异化：六边形 vs 圆形波纹）
    for (const evt of events.value) {
      if (!evt.longitude || !evt.latitude) continue
      const color = urgencyColor(evt.urgencyLevel)
      const marker = new AMap.Marker({
        position: [evt.longitude, evt.latitude], zIndex: 12,
        content: `<div class="evt-hex" style="--hc:${color}">
          <span class="hex-pulse"></span><span class="hex-pulse hp2"></span>
          <span class="hex-core"></span></div>`,
        offset: new AMap.Pixel(-10, -10), map, extData: evt
      })
      eventMarkerList.push(marker)
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
  if (!chartSparkBottom) chartSparkBottom = echarts.init(el)
  const t = mockTrend()
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
  const trend = mockTrend()
  renderTrend(trend.days, trend.values)
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
  } catch (e: any) { errors.push('概览: ' + (e?.message || e)) }

  try {
    const big = await getBigScreenData()
    if (big && typeof big === 'object') {
      overview.value = { ...overview.value, ...big }
      if (big.communityArea) areaRaw.value = Number(big.communityArea)
      anyLive = true
    }
  } catch (e) {}

  let stats: any = {}
  try {
    const s = await getGridStats()
    if (s) stats = s
    anyLive = true
  } catch (e: any) { errors.push('网格统计: ' + (e?.message || e)) }

  let tree: any[] = []
  try {
    const t = await getGridTree()
    if (Array.isArray(t) && t.length) tree = t
  } catch (e: any) { errors.push('网格树: ' + (e?.message || e)) }
  if (!tree.length) tree = buildMockTree()

  try {
    const r = await getEvents()
    if (r?.items?.length) { events.value = r.items; anyLive = true }
  } catch (e: any) { errors.push('事件: ' + (e?.message || e)) }
  if (!events.value.length) events.value = buildMockEvents()

  isLive.value = anyLive
  if (errors.length && !anyLive) loadError.value = errors.join('；')

  allGrids.value = []
  const flatten = (nodes: any[]) => {
    for (const n of nodes) { allGrids.value.push(n); if (n.children) flatten(n.children) }
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
  resizeHandler = () => { chartRing?.resize?.(); chartTrend?.resize?.(); chartRank?.resize?.(); chartSparkBottom?.resize?.() }
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
  ;[chartRing, chartTrend, chartRank, chartSparkBottom].forEach(c => { if (c) { c.dispose(); c = null } })
})

// 标签切换时重渲染图表
watch(leftTab, async (v) => {
  await nextTick()
  if (v === 'ring') { renderRing(); setTimeout(() => chartRing?.resize?.(), 50) }
  else if (v === 'trend') {
    const t = mockTrend(); renderTrend(t.days, t.values)
    setTimeout(() => chartTrend?.resize?.(), 50)
  }
  else if (v === 'rank' && hasPopulation.value) {
    const stats = await getGridStats().catch(() => null)
    if (stats?.populationRanking?.length) { renderRank(stats.populationRanking); setTimeout(() => chartRank?.resize?.(), 50) }
  }
})
</script>

<style scoped>
/* ================= 大屏容器 ================= */
.dash-screen {
  position: relative; margin: -24px; height: calc(100vh - 56px); overflow: hidden;
  background: linear-gradient(165deg, #eef4fa 0%, #f8fafc 45%, #eef4fa 100%);
  transition: padding 0.3s ease;
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
.crosshair-v, .crosshair-h {
  position: absolute; z-index: 6; pointer-events: none;
  transition: opacity 0.15s;
}
.crosshair-v {
  width: 1px; top: 48px; bottom: 36px;
  background: linear-gradient(180deg, transparent, rgba(2,132,199,0.35) 20%, rgba(2,132,199,0.35) 80%, transparent);
}
.crosshair-h {
  height: 1px; left: 0; right: 0;
  background: linear-gradient(90deg, transparent, rgba(2,132,199,0.35) 15%, rgba(2,132,199,0.35) 85%, transparent);
}

/* ================= 超窄顶部栏（48px） ================= */
.topbar {
  position: absolute; top: 0; left: 0; right: 0; height: 48px; z-index: 30;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; pointer-events: none;
  background: linear-gradient(180deg, rgba(255,255,255,0.92) 0%, rgba(255,255,255,0.4) 70%, transparent 100%);
  border-bottom: 1px solid rgba(2,132,199,0.08);
}
.topbar-brand { display: flex; align-items: center; gap: 8px; pointer-events: auto; }
.brand-dot {
  width: 8px; height: 8px; border-radius: 50%; animation: pulse 2s ease-in-out infinite;
}
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

/* ================= KPI 浮动横条 ================= */
.kpi-strip {
  position: absolute; top: 48px; left: 50%; transform: translateX(-50%); z-index: 45;
  display: flex; gap: 8px; pointer-events: auto;
  max-width: calc(100vw - 360px);
  flex-wrap: wrap;
  justify-content: center;
}
.kpi-pill {
  display: flex; align-items: center; gap: 8px;
  padding: 5px 12px 5px 9px; border-radius: 10px;
  background: rgba(255,255,255,0.94); backdrop-filter: blur(8px);
  border: 1px solid rgba(2,132,199,0.14);
  box-shadow: 0 4px 14px rgba(15,23,42,0.05);
  transition: transform 0.2s, box-shadow 0.2s;
}
.kpi-pill:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(2,132,199,0.12); }
.kpi-pill i { font-size: 16px; }
.kpi-body b { font-size: 16px; font-weight: 700; color: #0f172a; line-height: 1.1; display: block; }
.kpi-body b em { font-size: 10px; color: #64748b; font-style: normal; margin-left: 2px; font-weight: 400; }
.kpi-body span { font-size: 10px; color: #64748b; display: block; margin-top: 1px; }
.kpi-spark { width: 36px; height: 22px; }

/* ================= 地图控制按钮组 ================= */
.map-controls {
  position: absolute; right: 16px; bottom: 48px; z-index: 26;
  display: flex; flex-direction: column; gap: 6px; pointer-events: auto;
}
.mc-btn {
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(6px);
  border: 1px solid rgba(2,132,199,0.18); border-radius: 8px;
  color: #0284c7; font-size: 13px; cursor: pointer; transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(15,23,42,0.06);
}
.mc-btn:hover { background: #0284c7; color: #fff; transform: scale(1.05); }

/* ================= 面板内工具栏（图层开关） ================= */
.panel-tools {
  display: flex; align-items: center; flex-wrap: wrap; gap: 8px;
  padding: 8px 10px; margin: 0 0 8px; border-radius: 8px;
  background: rgba(241,245,249,0.7); border: 1px solid rgba(2,132,199,0.08);
}
.pt-title { font-size: 11px; font-weight: 600; color: #075985; display: inline-flex; align-items: center; gap: 4px; margin-right: 4px; }
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

/* ================= 网格预警图例（面板内） ================= */
.grid-legend {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px 10px;
  padding: 8px 10px; margin-top: 8px; border-radius: 8px;
  background: rgba(241,245,249,0.6); border: 1px solid rgba(2,132,199,0.08); font-size: 10px; color: #64748b;
}
.gl-title { display: inline-flex; align-items: center; gap: 3px; font-weight: 600; color: #075985; }
.gl-title i { color: #0284c7; font-size: 10px; }
.gl-item { display: inline-flex; align-items: center; gap: 3px; }
.gl-item i { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }

/* ================= 玻璃面板通用 ================= */
.glass-panel {
  background: rgba(255,255,255,0.94); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(2,132,199,0.12); border-radius: 12px;
  box-shadow: 0 8px 28px rgba(15,23,42,0.08); color: #334155;
}
.panel-empty { font-size: 12px; color: #94a3b8; text-align: center; padding: 18px 0; margin: 0; }

/* ================= 侧面板通用 ================= */
.side-panel {
  position: absolute; top: 96px; width: 300px; z-index: 20;
  display: flex; flex-direction: column;
  transition: width 0.3s ease, transform 0.3s ease;
}
.side-panel.left { left: 16px; }
.side-panel.right { right: 16px; }
.side-panel.folded { width: 0; }

/* 折叠按钮：靠内贴边，少伸出 */
.panel-fold {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 30;
  width: 20px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.96); border: 1px solid rgba(2,132,199,0.22); border-radius: 6px;
  color: #0284c7; cursor: pointer; font-size: 11px;
  box-shadow: 0 2px 10px rgba(15,23,42,0.08);
}
.side-panel.left .panel-fold { right: -6px; }
.side-panel.right .panel-fold { left: -6px; }

/* 面板容器：背景/圆角/阴影只作用于内容区，按钮伸出不被裁 */
.side-panel:not(.folded) .panel-content {
  background: rgba(255,255,255,0.94); backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(2,132,199,0.12); border-radius: 12px;
  box-shadow: 0 8px 28px rgba(15,23,42,0.08);
  overflow: hidden;
  display: flex; flex-direction: column;
}
/* 左侧面板：固定 480px，足够显示图表又不过高 */
.side-panel.left:not(.folded) { height: 480px; max-height: calc(100vh - 152px); }
/* 右侧面板：固定较小高度 */
.side-panel.right:not(.folded) { height: 460px; max-height: calc(100vh - 152px); }

/* ================= 左侧标签页 ================= */
.tab-head { display: flex; border-bottom: 1px solid rgba(2,132,199,0.10); flex-shrink: 0; }
.tab-btn {
  flex: 1; padding: 9px 0; border: none; background: none; cursor: pointer;
  font-size: 12px; color: #64748b; display: flex; align-items: center; justify-content: center; gap: 5px;
  transition: all 0.2s; border-bottom: 2px solid transparent;
}
.tab-btn i { font-size: 11px; }
.tab-btn:hover { color: #0284c7; }
.tab-btn.active { color: #0284c7; border-bottom-color: #0284c7; font-weight: 600; }
.tab-body { flex: 1; overflow-y: auto; padding: 12px 14px; min-height: 0; }
.tab-pane { height: 100%; display: flex; flex-direction: column; gap: 4px; }

/* ================= 环形图 + 图例 ================= */
.chart-ring { height: 170px; margin: 4px 0; }
.ring-legend { display: flex; flex-direction: column; gap: 9px; margin-top: 6px; }
.legend-row {
  display: flex; align-items: center; gap: 8px; font-size: 12px; cursor: pointer;
  padding: 3px 4px; border-radius: 6px; transition: background 0.15s;
}
.legend-row:hover { background: rgba(2,132,199,0.05); }
.legend-row.dim { opacity: 0.35; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.legend-name { width: 30px; color: #475569; flex-shrink: 0; font-size: 11px; }
.legend-track { flex: 1; height: 6px; background: #eef2f7; border-radius: 3px; overflow: hidden; }
.legend-track i { display: block; height: 100%; border-radius: 3px; transition: width 0.5s; }
.legend-num { width: 22px; text-align: right; font-weight: 600; color: #0f172a; font-size: 11px; }
.filter-hint {
  font-size: 11px; color: #0284c7; display: flex; align-items: center; gap: 6px;
  margin: 6px 0 0; padding: 4px 8px; background: rgba(2,132,199,0.06); border-radius: 6px;
}
.filter-hint button { border: none; background: none; color: #ef4444; cursor: pointer; font-size: 11px; margin-left: auto; }

/* ================= 趋势/排名 ================= */
.chart-trend { height: 260px; }
.chart-rank { height: 260px; }

/* ================= 右侧事件面板 ================= */
.panel-head-r { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px 8px; }
.panel-title { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: #075985; padding-left: 8px; border-left: 3px solid #0284c7; }
.panel-title i { color: #0284c7; }
.panel-count { font-size: 11px; color: #94a3b8; }

.filter-chips { display: flex; gap: 6px; padding: 0 14px 10px; flex-wrap: wrap; }
.chip {
  padding: 4px 12px; border-radius: 12px; border: 1px solid #e2e8f0; background: rgba(255,255,255,0.8);
  font-size: 11px; color: #64748b; cursor: pointer; transition: all 0.2s;
}
.chip:hover { border-color: rgba(2,132,199,0.4); }
.chip.active { font-weight: 600; }

.search-box { margin: 0 14px 10px; display: flex; align-items: center; gap: 8px; padding: 6px 12px; border-radius: 8px; background: #f1f5f9; border: 1px solid #e2e8f0; }
.search-box i { color: #94a3b8; font-size: 11px; }
.search-box input { border: none; background: none; outline: none; flex: 1; font-size: 12px; color: #334155; }

.events-list { flex: 1; overflow-y: auto; padding: 0 14px 10px; min-height: 0; }
.events-list::-webkit-scrollbar { width: 4px; }
.events-list::-webkit-scrollbar-thumb { background: rgba(2,132,199,0.25); border-radius: 2px; }
.event-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 6px; border-radius: 8px;
  cursor: pointer; transition: background 0.15s; border-bottom: 1px solid #f1f5f9;
}
.event-item:hover { background: rgba(2,132,199,0.05); }
.event-item.active { background: rgba(2,132,199,0.10); }
.evt-tag {
  flex-shrink: 0; font-size: 10px; font-weight: 600; padding: 2px 7px; border-radius: 4px; line-height: 1.3;
}
.event-text { flex: 1; min-width: 0; }
.event-title { margin: 0; font-size: 12px; color: #0f172a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.event-meta { margin: 2px 0 0; font-size: 10px; color: #94a3b8; display: flex; align-items: center; gap: 4px; }
.event-meta i { font-size: 9px; }
.event-locate { color: rgba(2,132,199,0.7); font-size: 11px; flex-shrink: 0; }

/* ================= 底部状态栏 ================= */
.status-bar {
  position: absolute; left: 0; right: 0; bottom: 0; height: 36px; z-index: 22;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 16px; pointer-events: none;
  background: rgba(255,255,255,0.92); backdrop-filter: blur(8px);
  border-top: 1px solid rgba(2,132,199,0.12);
}
.sb-left, .sb-right { display: flex; align-items: center; gap: 10px; pointer-events: auto; }
.sb-item { font-size: 11px; color: #64748b; display: inline-flex; align-items: center; gap: 4px; }
.sb-item i { color: #0284c7; font-size: 10px; }
.sb-item.urgent { color: #dc2626; }
.sb-item.urgent i { color: #ef4444; }
.sb-divider { width: 1px; height: 12px; background: #e2e8f0; }
.sb-center { display: flex; align-items: center; gap: 8px; pointer-events: auto; }
.sb-label { font-size: 11px; color: #64748b; }
.sb-num { font-size: 14px; font-weight: 700; font-variant-numeric: tabular-nums; }
.sb-spark { width: 60px; height: 20px; }

/* ================= 网格详情抽屉 ================= */
.grid-drawer {
  position: absolute; z-index: 42; top: 100px; right: 326px; width: 280px;
  padding: 14px 16px; animation: drawerIn 0.25s ease-out;
}
.side-panel.right.folded ~ .grid-drawer,
.dash-screen.panel-folded-r .grid-drawer { right: 32px; }
@keyframes drawerIn { from { opacity: 0; transform: translateX(20px); } to { opacity: 1; transform: translateX(0); } }
.drawer-close {
  position: absolute; top: 8px; right: 8px; border: none; background: none; color: #94a3b8;
  font-size: 14px; cursor: pointer; padding: 4px;
}
.drawer-close:hover { color: #0f172a; }
.drawer-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.drawer-urgency { width: 4px; height: 28px; border-radius: 2px; flex-shrink: 0; }
.drawer-header h3 { margin: 0; font-size: 15px; font-weight: 700; color: #0f172a; }
.drawer-level { font-size: 10px; color: #64748b; margin-left: auto; }
.drawer-body { display: flex; flex-direction: column; gap: 8px; }
.drawer-row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.drawer-row i { width: 16px; color: #0284c7; font-size: 11px; text-align: center; }
.drawer-row span { color: #64748b; width: 52px; flex-shrink: 0; }
.drawer-row b { color: #0f172a; font-weight: 600; }
.drawer-events { margin-top: 10px; border-top: 1px solid #f1f5f9; padding-top: 8px; }
.de-title { font-size: 11px; font-weight: 600; color: #075985; margin-bottom: 6px; display: flex; align-items: center; gap: 5px; }
.de-title i { color: #0284c7; }
.de-item { display: flex; align-items: center; gap: 6px; padding: 4px 0; cursor: pointer; font-size: 11px; }
.de-item:hover { color: #0284c7; }
.de-tag { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.de-text { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #475569; }

/* ================= 事件详情卡片 ================= */
.event-card {
  position: absolute; z-index: 40; left: 50%; bottom: 44px; transform: translateX(-50%);
  width: 400px; max-width: calc(100% - 32px); padding: 12px 16px;
}
.card-close {
  position: absolute; top: 8px; right: 8px; border: none; background: none; color: #94a3b8;
  font-size: 14px; cursor: pointer; padding: 4px;
}
.card-close:hover { color: #0f172a; }
.card-top { display: flex; align-items: center; gap: 8px; }
.card-status { font-size: 11px; color: #64748b; }
.card-title { margin: 8px 0 0; font-size: 14px; font-weight: 600; color: #0f172a; }
.card-meta { font-size: 11px; color: #475569; display: flex; gap: 12px; margin-top: 4px; }
.card-meta i { color: #0284c7; margin-right: 2px; }
.card-desc { margin: 8px 0 0; font-size: 11px; color: #64748b; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-detail-btn {
  margin-top: 10px; width: 100%; padding: 7px 0; border: none; border-radius: 8px;
  background: #0284c7; color: #fff; font-size: 12px; cursor: pointer; transition: background 0.2s;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.card-detail-btn:hover { background: #0369a1; }

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

/* ================= 过渡动画 ================= */
.drawer-enter-active, .drawer-leave-active { transition: opacity 0.25s, transform 0.25s; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; transform: translateX(20px); }
.slideup-enter-active, .slideup-leave-active { transition: opacity 0.25s, transform 0.25s; }
.slideup-enter-from, .slideup-leave-to { opacity: 0; transform: translate(-50%, 20px); }
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
</style>
