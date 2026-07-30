<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">GIS网格可视化</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">社区总面积 2.50 km²，精准划分三级网格单元（悬停查看面积，点击查看详情）</p>

    <div style="display:grid;grid-template-columns:1fr 320px;gap:16px;">
      <!-- 地图区域 -->
      <div class="card" style="position:relative;padding:0;overflow:hidden;">
        <div id="gisMapLarge" style="height:calc(100vh - 200px);border-radius:12px;overflow:hidden;"></div>

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

        <!-- 点击选中信息面板 -->
        <div v-if="selectedGrid" :style="{
          position: 'absolute', top: '16px', right: '16px', background: '#fff',
          borderRadius: '12px', padding: '16px 20px', minWidth: '240px', zIndex: 100,
          boxShadow: '0 8px 32px rgba(0,0,0,0.18)'
        }">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
            <span style="font-size:15px;font-weight:700;color:#111827;">{{ selectedGrid.gridName }}</span>
            <button @click="selectedGrid = null" style="border:none;background:none;cursor:pointer;color:#9ca3af;font-size:16px;padding:0 4px;">&times;</button>
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

        <!-- 图例 -->
        <div :style="{
          position: 'absolute', bottom: '16px', left: '16px', background: 'rgba(255,255,255,0.95)',
          borderRadius: '10px', padding: '12px 16px', zIndex: 100,
          boxShadow: '0 4px 16px rgba(0,0,0,0.12)', backdropFilter: 'blur(8px)'
        }">
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

      <!-- 右侧面积统计面板 -->
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 社区总览 -->
        <div class="card" style="padding:16px;">
          <h3 style="font-size:14px;font-weight:600;margin:0 0 12px;color:#111827;">
            <i class="fas fa-chart-pie" style="color:#0284c7;margin-right:6px;"></i>面积总览
          </h3>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
            <div style="background:#eff6ff;border-radius:8px;padding:10px;text-align:center;">
              <div style="font-size:20px;font-weight:700;color:#0284c7;">{{ communityArea }}</div>
              <div style="font-size:11px;color:#6b7280;margin-top:2px;">总面积 km²</div>
            </div>
            <div style="background:#f0fdf4;border-radius:8px;padding:10px;text-align:center;">
              <div style="font-size:20px;font-weight:700;color:#059669;">{{ totalGrids }}</div>
              <div style="font-size:11px;color:#6b7280;margin-top:2px;">网格总数</div>
            </div>
            <div style="background:#fffbeb;border-radius:8px;padding:10px;text-align:center;">
              <div style="font-size:20px;font-weight:700;color:#d97706;">{{ largeGridCount }}</div>
              <div style="font-size:11px;color:#6b7280;margin-top:2px;">大网格</div>
            </div>
            <div style="background:#f5f3ff;border-radius:8px;padding:10px;text-align:center;">
              <div style="font-size:20px;font-weight:700;color:#7c3aed;">{{ smallGridCount }}</div>
              <div style="font-size:11px;color:#6b7280;margin-top:2px;">小网格</div>
            </div>
          </div>
        </div>

        <!-- 大网格面积排行 -->
        <div class="card" style="padding:16px;flex:1;overflow-y:auto;">
          <h3 style="font-size:14px;font-weight:600;margin:0 0 12px;color:#111827;">
            <i class="fas fa-layer-group" style="color:#f59e0b;margin-right:6px;"></i>大网格面积排行
          </h3>
          <div style="display:flex;flex-direction:column;gap:8px;">
            <div v-for="(grid, idx) in largeGridsByArea" :key="grid.id"
              style="display:flex;align-items:center;gap:10px;padding:8px 10px;border-radius:8px;cursor:pointer;transition:background 0.15s;"
              :style="selectedGrid?.id === grid.id ? 'background:#eff6ff;' : 'background:#f9fafb;'"
              @click="selectGrid(grid)">
              <div style="width:24px;height:24px;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:#fff;flex-shrink:0;"
                :style="`background:${areaColor(idx)};`">
                {{ idx + 1 }}
              </div>
              <div style="flex:1;min-width:0;">
                <div style="font-size:13px;font-weight:600;color:#111827;">{{ grid.gridName }}</div>
                <div style="font-size:11px;color:#9ca3af;">{{ grid.gridCode }}</div>
              </div>
              <div style="text-align:right;flex-shrink:0;">
                <div style="font-size:14px;font-weight:700;color:#0284c7;">{{ grid.area }}</div>
                <div style="font-size:10px;color:#9ca3af;">km²</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 小网格列表 -->
        <div class="card" style="padding:16px;max-height:260px;overflow-y:auto;">
          <h3 style="font-size:14px;font-weight:600;margin:0 0 12px;color:#111827;">
            <i class="fas fa-th" style="color:#10b981;margin-right:6px;"></i>小网格列表
          </h3>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:6px;">
            <div v-for="grid in smallGrids" :key="grid.id"
              style="padding:6px 8px;background:#f0fdf4;border-radius:6px;cursor:pointer;font-size:11px;"
              :style="selectedGrid?.id === grid.id ? 'background:#d1fae5;' : 'background:#f0fdf4;'"
              @click="selectGrid(grid)">
              <div style="font-weight:600;color:#065f46;">{{ grid.gridName }}</div>
              <div style="color:#6b7280;">{{ grid.area }} km² · {{ grid.population }}人</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getGridTree, getEvents } from '../api'
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
const gridTree = ref<GridInfo[]>([])

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
      const lngs = coords.map((c: number[]) => c[0])
      const lats = coords.map((c: number[]) => c[1])
      const center = [(Math.min(...lngs) + Math.max(...lngs)) / 2, (Math.min(...lats) + Math.max(...lats)) / 2]
      mapInstance.setCenter(center)
      mapInstance.setZoom(15)
    }
  } catch (e) {}
}

onMounted(async () => {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Polygon', 'AMap.Marker']
  })
  mapInstance = new AMapLib.Map('gisMapLarge', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/normal' })
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
            new AMapLib.Polygon({
              path: coords, fillColor: '#0284c7', fillOpacity: 0.08,
              strokeColor: '#0284c7', strokeWeight: 3, strokeStyle: 'solid',
              zIndex: 1, bubble: true, map
            })
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
