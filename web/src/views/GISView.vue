<template>
  <div style="position:absolute;top:0;left:240px;right:0;bottom:0;overflow:hidden;">
    <!-- 全屏地图背景 -->
    <div id="gisMapLarge" style="position:absolute;inset:0;z-index:0;"></div>

    <!-- 顶部标题栏（悬浮） -->
    <div style="position:absolute;top:0;left:0;right:0;height:48px;z-index:30;display:flex;align-items:center;justify-content:space-between;padding:0 16px;background:linear-gradient(180deg, rgba(255,255,255,0.7) 0%, rgba(255,255,255,0.4) 70%, transparent 100%);pointer-events:none;">
      <div style="pointer-events:auto;">
        <h2 style="font-size:16px;font-weight:600;margin:0;color:#111827;display:inline-flex;align-items:center;gap:8px;">
          <i class="fas fa-map-marked-alt" style="color:#0284c7;"></i>
          GIS网格可视化
        </h2>
        <span style="font-size:12px;color:#6b7280;margin-left:12px;">社区总面积 {{ communityArea }} km² · 三级网格单元</span>
      </div>
    </div>

    <!-- 右侧统计面板组（悬浮） -->
    <div style="position:absolute;top:64px;right:16px;bottom:80px;z-index:15;display:flex;flex-direction:column;gap:10px;width:300px;pointer-events:auto;">
      <!-- 面积总览 KPI 卡片 -->
      <div class="glass-panel" style="padding:14px;">
        <div style="font-size:13px;font-weight:600;margin-bottom:10px;color:#0284c7;display:flex;align-items:center;gap:6px;">
          <i class="fas fa-chart-pie"></i>面积总览
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;">
          <div style="background:#eff6ff;border-radius:8px;padding:10px;text-align:center;">
            <div style="font-size:18px;font-weight:700;color:#0284c7;">{{ communityArea }}</div>
            <div style="font-size:11px;color:#6b7280;margin-top:2px;">总面积 km²</div>
          </div>
          <div style="background:#f0fdf4;border-radius:8px;padding:10px;text-align:center;">
            <div style="font-size:18px;font-weight:700;color:#059669;">{{ totalGrids }}</div>
            <div style="font-size:11px;color:#6b7280;margin-top:2px;">网格总数</div>
          </div>
          <div style="background:#fffbeb;border-radius:8px;padding:10px;text-align:center;">
            <div style="font-size:18px;font-weight:700;color:#d97706;">{{ largeGridCount }}</div>
            <div style="font-size:11px;color:#6b7280;margin-top:2px;">大网格</div>
          </div>
          <div style="background:#f5f3ff;border-radius:8px;padding:10px;text-align:center;">
            <div style="font-size:18px;font-weight:700;color:#7c3aed;">{{ smallGridCount }}</div>
            <div style="font-size:11px;color:#6b7280;margin-top:2px;">小网格</div>
          </div>
        </div>
      </div>

      <!-- 大网格面积排行 -->
      <div class="glass-panel" style="padding:14px;flex:1;overflow-y:auto;max-height:calc(100vh - 420px);">
        <div style="font-size:13px;font-weight:600;margin-bottom:10px;color:#f59e0b;display:flex;align-items:center;gap:6px;">
          <i class="fas fa-layer-group"></i>大网格排行
        </div>
        <div style="display:flex;flex-direction:column;gap:6px;">
          <div v-for="(grid, idx) in largeGridsByArea" :key="grid.id"
            style="display:flex;align-items:center;gap:8px;padding:8px;border-radius:8px;cursor:pointer;transition:all 0.2s;"
            :style="selectedGrid?.id === grid.id ? 'background:#eff6ff;' : 'background:rgba(249,250,251,0.8);'"
            @click="selectGrid(grid)">
            <div style="width:22px;height:22px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:#fff;flex-shrink:0;"
              :style="`background:${areaColor(idx)};`">
              {{ idx + 1 }}
            </div>
            <div style="flex:1;min-width:0;">
              <div style="font-size:12px;font-weight:600;color:#111827;">{{ grid.gridName }}</div>
              <div style="font-size:10px;color:#9ca3af;">{{ grid.gridCode }}</div>
            </div>
            <div style="text-align:right;flex-shrink:0;">
              <div style="font-size:13px;font-weight:700;color:#0284c7;">{{ grid.area }}</div>
              <div style="font-size:9px;color:#9ca3af;">km²</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 小网格列表 -->
      <div class="glass-panel" style="padding:14px;max-height:220px;overflow-y:auto;">
        <div style="font-size:13px;font-weight:600;margin-bottom:10px;color:#10b981;display:flex;align-items:center;gap:6px;">
          <i class="fas fa-th"></i>小网格列表
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;">
          <div v-for="grid in smallGrids" :key="grid.id"
            style="padding:6px 8px;border-radius:6px;cursor:pointer;font-size:11px;transition:all 0.2s;"
            :style="selectedGrid?.id === grid.id ? 'background:#d1fae5;' : 'background:rgba(240,253,244,0.8);'"
            @click="selectGrid(grid)">
            <div style="font-weight:600;color:#065f46;">{{ grid.gridName }}</div>
            <div style="color:#6b7280;">{{ grid.area }} km² · {{ grid.population }}人</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 地图加载失败提示 -->
    <div v-if="mapError" style="position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);text-align:center;padding:24px 32px;border-radius:12px;background:rgba(255,255,255,0.97);box-shadow:0 8px 32px rgba(0,0,0,0.15);z-index:200;">
      <div style="font-size:28px;margin-bottom:10px;">🗺️</div>
      <div style="font-size:14px;font-weight:600;color:#374151;margin-bottom:6px;">地图加载失败</div>
      <div style="font-size:12px;color:#6b7280;max-width:280px;line-height:1.7;">{{ mapError }}<br/>请在浏览器 F12 控制台查看具体报错（INVALID_USER_SCODE = 高德 key 未授权当前域名）</div>
    </div>

    <!-- 悬停提示框 -->
    <div v-if="hoverInfo.visible" :style="{
      position: 'absolute', left: hoverInfo.x + 'px', top: hoverInfo.y + 'px',
      transform: 'translate(-50%, calc(-100% - 14px))',
      background: 'rgba(17,24,39,0.92)', color: '#fff', padding: '8px 14px',
      borderRadius: '8px', fontSize: '13px', fontWeight: '600',
      pointerEvents: 'none', zIndex: 1000, whiteSpace: 'nowrap',
      boxShadow: '0 4px 16px rgba(0,0,0,0.3)'
    }">
      {{ hoverInfo.name }}
      <div v-if="hoverInfo.area" style="font-size:11px;font-weight:400;color:#93c5fd;margin-top:2px;">
        面积: {{ hoverInfo.area }} km²
      </div>
      <div style="position:absolute;left:50%;bottom:-6px;transform:translateX(-50%);width:0;height:0;border-left:6px solid transparent;border-right:6px solid transparent;border-top:6px solid rgba(17,24,39,0.92);"></div>
    </div>

    <!-- 点击选中信息面板（悬浮） - 调整位置避免与右侧面板重叠 -->
    <div v-if="selectedGrid" :style="{
      position: 'absolute',
      top: '64px',
      right: selectedGrid ? '324px' : '16px',
      width: selectedGrid ? '260px' : '280px',
      zIndex: 20,
      background: 'rgba(255, 255, 255, 0.7)',
      border: '1px solid rgba(2, 132, 199, 0.12)',
      borderRadius: '12px',
      padding: '14px 18px',
      boxShadow: '0 8px 24px rgba(15, 23, 42, 0.10)',
      transition: 'background 0.25s ease'
    }"
    @mouseenter="$event.target.style.background = 'rgba(255, 255, 255, 0.92)'"
    @mouseleave="$event.target.style.background = 'rgba(255, 255, 255, 0.7)'">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
        <span style="font-size:14px;font-weight:700;color:#111827;display:flex;align-items:center;gap:6px;">
          <i class="fas fa-map-pin" style="color:#0284c7;"></i>
          {{ selectedGrid.gridName }}
        </span>
        <button @click="selectedGrid = null" style="border:none;background:none;cursor:pointer;color:#9ca3af;font-size:16px;padding:0 4px;line-height:1;">&times;</button>
      </div>
      <div style="font-size:12px;color:#6b7280;line-height:2;">
        <div>网格编码：<strong style="color:#374151;">{{ selectedGrid.gridCode || '-' }}</strong></div>
        <div>层级：<strong style="color:#374151;">{{ levelText(selectedGrid.gridLevel) }}</strong></div>
        <div>面积：<strong style="color:#0284c7;font-size:14px;">{{ selectedGrid.area }} km²</strong></div>
        <div>人口：<strong style="color:#374151;">{{ selectedGrid.population?.toLocaleString() || '-' }} 人</strong></div>
        <div>楼栋：<strong style="color:#374151;">{{ selectedGrid.buildingCount || '-' }} 栋</strong></div>
        <div>状态：<strong :style="selectedGrid.status === 'ACTIVE' ? 'color:#52c41a;' : 'color:#f5222d;'">{{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}</strong></div>
      </div>
      <button @click="focusGrid(selectedGrid)" style="margin-top:12px;width:100%;padding:7px 0;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;font-weight:600;cursor:pointer;">
        聚焦到此网格
      </button>
    </div>

    <!-- 图层切换（左上角悬浮） -->
    <div class="layer-control">
      <div style="font-size:12px;font-weight:600;color:#374151;margin-bottom:8px;">图层控制</div>
      <div style="display:flex;flex-direction:column;gap:6px;">
        <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-size:12px;color:#374151;">
          <input type="checkbox" v-model="showHeatmap" style="accent-color:#ff4d4f;" />
          <span style="width:12px;height:12px;border-radius:50%;background:#ff4d4f;opacity:0.7;"></span>
          事件热力图
        </label>
        <label style="display:flex;align-items:center;gap:8px;cursor:pointer;font-size:12px;color:#374151;">
          <input type="checkbox" v-model="showTrajectories" style="accent-color:#0284c7;" />
          <span style="width:12px;height:12px;border-radius:50%;background:#0284c7;opacity:0.7;"></span>
          巡查轨迹
        </label>
        <!-- 轨迹时间范围：避免历史轨迹堆积重叠占满地图，默认只看近 7 天 -->
        <div v-if="showTrajectories" style="display:flex;gap:4px;margin:2px 0 0 20px;">
          <button v-for="opt in rangeOptions" :key="opt.value" @click="trajectoryRange = opt.value"
            style="padding:2px 7px;font-size:11px;border-radius:10px;border:1px solid #d1d5db;cursor:pointer;"
            :style="trajectoryRange === opt.value ? 'background:#0284c7;color:#fff;border-color:#0284c7;' : 'background:#fff;color:#6b7280;'">
            {{ opt.label }}
          </button>
        </div>
      </div>
    </div>

    <!-- 网格层级图例（左下角悬浮） -->
    <div class="gis-legend">
      <div style="font-size:12px;font-weight:600;color:#374151;margin-bottom:8px;">网格层级</div>
      <div style="display:flex;flex-direction:column;gap:6px;">
        <div style="display:flex;align-items:center;gap:8px;">
          <div style="width:14px;height:14px;border-radius:3px;background:#0284c7;opacity:0.6;"></div>
          <span style="font-size:12px;color:#6b7280;">社区边界</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px;">
          <div style="width:14px;height:14px;border-radius:3px;background:#f59e0b;opacity:0.7;"></div>
          <span style="font-size:12px;color:#6b7280;">大网格 (0.38-0.42 km²)</span>
        </div>
        <div style="display:flex;align-items:center;gap:8px;">
          <div style="width:14px;height:14px;border-radius:3px;background:#10b981;opacity:0.6;"></div>
          <span style="font-size:12px;color:#6b7280;">小网格 (0.19-0.21 km²)</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'

// 地图加载失败提示（白屏定位用）
const mapError = ref('')
import { getGridTree, getEvents, getEventHeatmap, getPatrolTrajectories } from '../api'
import http from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'

interface GridInfo {
  id: number
  gridName: string
  gridCode?: string
  gridLevel?: number
  area?: number
  population?: number
  buildingCount?: number
  status?: string
  roiJson?: string
  children?: GridInfo[]
}

const hoverInfo = reactive({ visible: false, x: 0, y: 0, name: '', area: '', id: 0 })
const selectedGrid = ref<GridInfo | null>(null)
let hoverId = 0
let mapInstance: any = null
let AMapLib: any = null
// 跟踪所有绘制的多边形覆盖物，用于自适应地图视野
const allOverlays: any[] = []
const gridTree = ref<GridInfo[]>([])

// 图层控制
const showHeatmap = ref(false)
const showTrajectories = ref(false)
let heatmapInstance: any = null
let trajectoryPolylines: any[] = []
let trajectoryMarkers: any[] = []

// 巡查轨迹时间范围（默认近 7 天，避免历史轨迹堆积重叠）
const trajectoryRange = ref<'today' | '7d' | '30d' | 'all'>('7d')
const rangeOptions = [
  { value: 'today' as const, label: '今天' },
  { value: '7d' as const, label: '近7天' },
  { value: '30d' as const, label: '近30天' },
  { value: 'all' as const, label: '全部' }
]

/** 根据所选范围计算起始时间；不传结束时间，上限自然取当前时刻 */
function trajectoryStartDate(): string | undefined {
  if (trajectoryRange.value === 'all') return undefined
  const days = trajectoryRange.value === 'today' ? 0 : trajectoryRange.value === '7d' ? 6 : 29
  const d = new Date()
  d.setDate(d.getDate() - days)
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} 00:00:00`
}

function clearTrajectories() {
  for (const p of trajectoryPolylines) p.setMap(null)
  trajectoryPolylines = []
  // 热力降级标记（_isHeatmapMarker）与轨迹标记共用数组，只清理轨迹类
  for (const m of trajectoryMarkers) {
    if (!m._isHeatmapMarker) m.setMap(null)
  }
  trajectoryMarkers = trajectoryMarkers.filter(m => m._isHeatmapMarker)
}

// ==================== 巡查轨迹（按时间范围拉取，切换范围时清除重绘） ====================
async function loadTrajectories() {
  if (!AMapLib || !mapInstance) return
  clearTrajectories()
  try {
    const startDate = trajectoryStartDate()
    const data: any = await getPatrolTrajectories(startDate ? { startDate } : {})
    if (!data || !data.length) return
    const map = mapInstance
    const colors = ['#0284c7', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']
    let colorIdx = 0
    for (const track of data) {
      const coords = track.coords
      if (!coords || coords.length < 2) continue
      const color = colors[colorIdx % colors.length]
      colorIdx++

      // 绘制轨迹线（更细更美观：带白色描边 + 半透明彩色线）
      // 底层白色描边（加粗）
      const outline = new AMapLib.Polyline({
        path: coords,
        strokeColor: '#ffffff',
        strokeWeight: 5,
        strokeOpacity: 0.9,
        strokeStyle: 'solid',
        bubble: true,
        zIndex: 10,
        map
      })
      trajectoryPolylines.push(outline)
      // 上层彩色细线
      const polyline = new AMapLib.Polyline({
        path: coords,
        strokeColor: color,
        strokeWeight: 2.5,
        strokeOpacity: 0.85,
        strokeStyle: 'solid',
        bubble: true,
        zIndex: 11,
        map
      })
      trajectoryPolylines.push(polyline)

      // 起点标记（绿色圆点）
      const startMarker = new AMapLib.CircleMarker({
        center: coords[0],
        radius: 5,
        fillColor: '#ffffff',
        fillOpacity: 1,
        strokeColor: '#52c41a',
        strokeWeight: 2,
        zIndex: 20,
        bubble: true,
        map
      })
      trajectoryMarkers.push(startMarker)

      // 终点标记（红色圆点）
      const endMarker = new AMapLib.CircleMarker({
        center: coords[coords.length - 1],
        radius: 5,
        fillColor: '#ffffff',
        fillOpacity: 1,
        strokeColor: '#ff4d4f',
        strokeWeight: 2,
        zIndex: 20,
        bubble: true,
        map
      })
      trajectoryMarkers.push(endMarker)

      // 轨迹中点标签（显示网格员姓名）
      const midIdx = Math.floor(coords.length / 2)
      const midPoint = coords[midIdx]
      const textLabel = new AMapLib.Text({
        text: track.userName,
        position: midPoint,
        offset: [0, -12],
        style: {
          'background-color': color,
          'color': '#ffffff',
          'font-size': '11px',
          'padding': '2px 6px',
          'border-radius': '10px',
          'border-width': 0,
          'white-space': 'nowrap',
          'opacity': 0.85
        },
        zIndex: 25,
        map
      })
      trajectoryMarkers.push(textLabel)
    }
    // 图层关闭时新绘制的元素保持隐藏
    if (!showTrajectories.value) {
      for (const p of trajectoryPolylines) p.hide()
      for (const m of trajectoryMarkers) {
        if (!m._isHeatmapMarker) m.hide()
      }
    }
  } catch (e) {
    console.warn('轨迹数据加载失败:', e)
  }
}

// 切换时间范围时重新拉取并重绘
watch(trajectoryRange, () => { loadTrajectories() })

const allGrids = computed(() => {
  const result: GridInfo[] = []
  const flatten = (nodes: GridInfo[]) => {
    for (const n of nodes) {
      result.push(n)
      if (n.children) flatten(n.children)
    }
  }
  flatten(gridTree.value)
  return result
})

const communityArea = computed(() => {
  const community = allGrids.value.find(g => g.gridLevel === 1)
  return community?.area?.toFixed(2) || '2.50'
})

const totalGrids = computed(() => allGrids.value.filter(g => g.gridLevel >= 2).length)
const largeGridCount = computed(() => allGrids.value.filter(g => g.gridLevel === 2).length)
const smallGridCount = computed(() => allGrids.value.filter(g => g.gridLevel === 3).length)

const largeGridsByArea = computed(() =>
  allGrids.value.filter(g => g.gridLevel === 2).sort((a, b) => (b.area || 0) - (a.area || 0))
)

const smallGrids = computed(() =>
  allGrids.value.filter(g => g.gridLevel === 3).sort((a, b) => (a.gridCode || '').localeCompare(b.gridCode || ''))
)

function areaColor(idx: number) {
  const colors = ['#0284c7', '#059669', '#d97706', '#dc2626', '#7c3aed', '#db2777']
  return colors[idx % colors.length]
}

function levelText(level?: number) {
  if (level === 1) return '社区'
  if (level === 2) return '大网格'
  if (level === 3) return '小网格'
  return '未知'
}

function selectGrid(grid: GridInfo) {
  selectedGrid.value = grid
  focusGrid(grid)
}

function focusGrid(grid: GridInfo) {
  if (!mapInstance || !grid.roiJson) return
  try {
    const coords = JSON.parse(grid.roiJson)
    if (Array.isArray(coords) && coords.length > 0) {
      // 临时创建一个多边形用于聚焦，setFitView 后立即销毁
      const tempPoly = new AMapLib.Polygon({ path: coords, map: mapInstance })
      mapInstance.setFitView([tempPoly], false, [60, 60, 60, 60])
      const currentZoom = mapInstance.getZoom()
      if (currentZoom > 18) mapInstance.setZoom(18)
      tempPoly.setMap(null)
    }
  } catch (e) {}
}

onMounted(async () => {
  try {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  // 读取系统配置的地图中心点（网格管理页可配置），未配置时用默认坐标
  let centerLng = 113.939521
  let centerLat = 22.971231
  try {
    const [lng, lat] = await Promise.all([
      http.get('/system/config/map.center.lng'),
      http.get('/system/config/map.center.lat'),
    ])
    if (lng && !isNaN(Number(lng))) centerLng = Number(lng)
    if (lat && !isNaN(Number(lat))) centerLat = Number(lat)
  } catch (e) { /* 配置接口失败时使用默认中心点 */ }
  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Polygon', 'AMap.Marker', 'AMap.Polyline', 'AMap.HeatMap', 'AMap.Circle']
  })
  mapInstance = new AMapLib.Map('gisMapLarge', { zoom: 14, center: [centerLng, centerLat], mapStyle: 'amap://styles/normal' })
  const map = mapInstance

  const tree = await getGridTree()
  gridTree.value = tree

  // 绘制社区边界 (level 1)
  const drawCommunity = (nodes: any[]) => {
    for (const node of nodes) {
      if (node.gridLevel === 1 && node.roiJson) {
        try {
          const coords = JSON.parse(node.roiJson)
          if (Array.isArray(coords) && coords.length >= 3) {
            const poly = new AMapLib.Polygon({
              path: coords, fillColor: '#0284c7', fillOpacity: 0.08,
              strokeColor: '#0284c7', strokeWeight: 3, strokeStyle: 'solid',
              zIndex: 1, bubble: true, map
            })
            allOverlays.push(poly)
          }
        } catch (e) {}
      }
      if (node.children) drawCommunity(node.children)
    }
  }
  drawCommunity(tree)

  // 绘制大网格 (level 2)
  const drawLargeGrids = (nodes: any[]) => {
    for (const grid of nodes) {
      if (grid.gridLevel === 2 && grid.roiJson) {
        drawGridPolygon(map, grid, '#f59e0b', '#ffffff', 2, 0.35)
      }
      if (grid.children) drawLargeGrids(grid.children)
    }
  }
  drawLargeGrids(tree)

  // 绘制小网格 (level 3)
  const drawSmallGrids = (nodes: any[]) => {
    for (const grid of nodes) {
      if (grid.gridLevel === 3 && grid.roiJson) {
        drawGridPolygon(map, grid, '#10b981', '#ffffff', 1, 0.30)
      }
      if (grid.children) drawSmallGrids(grid.children)
    }
  }
  drawSmallGrids(tree)

  // 自适应视野：让所有多边形（社区边界+大网格+小网格）完整显示在地图内
  if (allOverlays.length > 0) {
    setTimeout(() => {
      mapInstance.setFitView(allOverlays, false, [80, 80, 80, 80])
      // 限制缩放级别，避免视野过大或过挤
      const currentZoom = mapInstance.getZoom()
      if (currentZoom > 17) mapInstance.setZoom(17)
      if (currentZoom < 12) mapInstance.setZoom(12)
    }, 100)
  }

  // ==================== 热力图 ====================
  const loadHeatmap = async () => {
    try {
      const data: any = await getEventHeatmap({})
      if (!data || !data.length) return
      const heatPoints = data.map((d: any) => ({
        lng: Number(d.lng),
        lat: Number(d.lat),
        count: d.urgency_level === 'RED' ? 3 : d.urgency_level === 'YELLOW' ? 2 : 1
      }))
      if (AMapLib.HeatMap) {
        heatmapInstance = new AMapLib.HeatMap(map, {
          radius: 25,
          opacity: [0, 0.6],
          gradient: { 0.3: '#2764d6', 0.5: '#12b312', 0.7: '#ffff00', 0.9: '#ff8800', 1.0: '#ff0000' }
        })
        heatmapInstance.setDataSet({ data: heatPoints, max: 5 })
        if (!showHeatmap.value) heatmapInstance.hide()
      } else {
        // 降级：用圆形标记表示热力
        for (const p of heatPoints) {
          const weight = p.count
          const marker = new AMapLib.Circle({
            center: [p.lng, p.lat],
            radius: 30 + weight * 20,
            fillColor: weight >= 3 ? '#ff4d4f' : weight === 2 ? '#faad14' : '#1890ff',
            fillOpacity: 0.35,
            strokeColor: weight >= 3 ? '#ff4d4f' : weight === 2 ? '#faad14' : '#1890ff',
            strokeWeight: 1,
            strokeOpacity: 0.5,
            bubble: true,
            map
          })
          marker._isHeatmapMarker = true
          // 初始隐藏（showHeatmap 默认 false）
          if (!showHeatmap.value) marker.hide()
          trajectoryMarkers.push(marker)
        }
      }
    } catch (e) {
      console.warn('热力图数据加载失败:', e)
    }
  }

  // 初始加载数据（页面打开时即加载，此时已登录）
  await loadHeatmap()
  await loadTrajectories()

  // 图层开关监听（数据已加载，只需控制显隐）
  showHeatmap.value = false  // 确保初始隐藏
  showTrajectories.value = false

  // 用 watch 监听复选框变化
  watch(showHeatmap, (visible) => {
    if (heatmapInstance) {
      visible ? heatmapInstance.show() : heatmapInstance.hide()
    }
    for (const m of trajectoryMarkers) {
      if (m._isHeatmapMarker) {
        visible ? m.show() : m.hide()
      }
    }
  })

  watch(showTrajectories, (visible) => {
    for (const p of trajectoryPolylines) {
      visible ? p.show() : p.hide()
    }
    for (const m of trajectoryMarkers) {
      if (!m._isHeatmapMarker) {
        visible ? m.show() : m.hide()
      }
    }
  })
  } catch (e: any) {
    // 地图初始化失败（常见：高德 key 域名白名单未授权）——显示提示而非静默白屏
    console.error('[GIS] 地图初始化失败:', e)
    mapError.value = e?.message?.includes('INVALID_USER_SCODE') || String(e?.message || '').includes('key')
      ? '当前域名未在高德地图 key 的白名单中，地图无法加载。'
      : `地图初始化失败：${e?.message || '未知错误'}`
  }
})

function drawGridPolygon(map: any, grid: any, fillColor: string, strokeColor: string, strokeWeight: number, fillOpacity: number) {
  try {
    const coords = JSON.parse(grid.roiJson)
    if (!Array.isArray(coords) || coords.length < 3) return

    const myId = ++hoverId
    const polygon = new AMapLib.Polygon({
      path: coords, fillColor, fillOpacity, strokeColor, strokeWeight,
      strokeStyle: 'solid', zIndex: 5, bubble: false, map
    })
    allOverlays.push(polygon)

    polygon.on('mouseover', (e: any) => {
      polygon.setOptions({ fillOpacity: 0.6, strokeWeight: strokeWeight + 1, zIndex: 20 })
      const px = map.lngLatToContainer(e.lnglat)
      hoverInfo.visible = true
      hoverInfo.x = px.getX()
      hoverInfo.y = px.getY()
      hoverInfo.name = grid.gridName
      hoverInfo.area = grid.area
      hoverInfo.id = myId
    })

    polygon.on('mousemove', (e: any) => {
      const px = map.lngLatToContainer(e.lnglat)
      hoverInfo.x = px.getX()
      hoverInfo.y = px.getY()
    })

    polygon.on('mouseout', () => {
      if (hoverInfo.id !== myId) return
      polygon.setOptions({ fillOpacity, strokeWeight, zIndex: 5 })
      hoverInfo.visible = false
    })

    polygon.on('click', () => {
      selectedGrid.value = grid as GridInfo
    })
  } catch (e) {}
}
</script>

<style scoped>
/* ================= 玻璃面板通用（与首页统一） ================= */
.glass-panel {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(2, 132, 199, 0.12);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);
  transition: background 0.25s ease;
}
.glass-panel:hover,
.glass-panel:active {
  background: rgba(255, 255, 255, 0.92);
}

/* ================= 图层控制（左上角悬浮，z-index高于右侧面板） ================= */
.layer-control {
  position: absolute;
  top: 64px;
  left: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(2, 132, 199, 0.12);
  border-radius: 10px;
  padding: 12px 16px;
  z-index: 25;  /* 高于右侧面板(z-index:15) */
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);
  transition: background 0.25s ease;
}
.layer-control:hover,
.layer-control:active {
  background: rgba(255, 255, 255, 0.92);
}

/* ================= 网格层级图例（左下角悬浮，z-index高于右侧面板） ================= */
.gis-legend {
  position: absolute;
  bottom: 80px;
  left: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(2, 132, 199, 0.12);
  border-radius: 10px;
  padding: 12px 16px;
  z-index: 25;  /* 高于右侧面板(z-index:15) */
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);
  transition: background 0.25s ease;
}
.gis-legend:hover,
.gis-legend:active {
  background: rgba(255, 255, 255, 0.92);
}

/* ================= 滚动条美化 ================= */
.glass-panel::-webkit-scrollbar,
.gis-legend::-webkit-scrollbar,
.layer-control::-webkit-scrollbar {
  width: 4px;
}
.glass-panel::-webkit-scrollbar-thumb,
.gis-legend::-webkit-scrollbar-thumb,
.layer-control::-webkit-scrollbar-thumb {
  background: rgba(2, 132, 199, 0.25);
  border-radius: 2px;
}

/* ================= 选中网格信息面板（右上角悬浮） ================= */
.selected-grid-info {
  position: absolute;
  top: 64px;
  right: 320px;
  width: 260px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(2, 132, 199, 0.12);
  border-radius: 12px;
  padding: 14px 18px;
  z-index: 100;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.10);
  transition: background 0.25s ease;
}
.selected-grid-info:hover,
.selected-grid-info:active {
  background: rgba(255, 255, 255, 0.92);
}
</style>