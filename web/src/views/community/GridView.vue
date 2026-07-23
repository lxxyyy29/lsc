<template>
  <section class="grid-view">
    <header class="grid-view__header">
      <h2>GIS 网格可视化</h2>
      <p class="grid-view__subtitle">拔蛟窝社区 — 6大网格 → 12小网格（当前：{{ gridCount }}个网格）</p>
    </header>

    <div class="grid-view__body">
      <aside class="grid-view__sidebar">
        <div class="panel">
          <h3 class="panel__title">网格列表</h3>
          <el-tree
            v-if="tree.length"
            :data="tree"
            :props="{ label: 'gridName', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="grid-tree__node">
                <el-tag size="small" :type="levelTagType(data.gridLevel)">
                  {{ levelLabel(data.gridLevel) }}
                </el-tag>
                <span class="grid-tree__name">{{ node.label }}</span>
                <span class="grid-tree__meta">👥{{ data.population || 0 }}</span>
              </span>
            </template>
          </el-tree>
          <el-empty v-else description="暂无网格数据" :image-size="80" />
        </div>

        <div class="panel">
          <h3 class="panel__title">图例</h3>
          <div class="legend">
            <div class="legend__item"><span class="legend__dot legend__dot--1"></span>社区</div>
            <div class="legend__item"><span class="legend__dot legend__dot--2"></span>大网格</div>
            <div class="legend__item"><span class="legend__dot legend__dot--3"></span>小网格</div>
            <div class="legend__item"><span class="legend__dot legend__dot--event"></span>事件</div>
          </div>
        </div>

        <div v-if="selectedGrid" class="panel panel--detail">
          <h3 class="panel__title">网格详情</h3>
          <dl class="detail-list">
            <dt>名称</dt><dd>{{ selectedGrid.gridName }}</dd>
            <dt>编码</dt><dd>{{ selectedGrid.gridCode }}</dd>
            <dt>层级</dt><dd>{{ levelLabel(selectedGrid.gridLevel) }}</dd>
            <dt>面积</dt><dd>{{ selectedGrid.area || '-' }} km²</dd>
            <dt>人口</dt><dd>{{ selectedGrid.population || 0 }} 人</dd>
            <dt>楼栋</dt><dd>{{ selectedGrid.buildingCount || 0 }} 栋</dd>
          </dl>
        </div>
      </aside>

      <main class="grid-view__map-container">
        <div ref="mapRef" class="grid-view__map"></div>
      </main>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { getGridTree, GridTreeVo } from '../../api/community'
import { http } from '../../api/http'
import AMapLoader from '@amap/amap-jsapi-loader'

interface EventItem {
  id: number
  title: string
  longitude: number
  latitude: number
  urgencyLevel: string
  status: string
}

const tree = ref<GridTreeVo[]>([])
const selectedGrid = ref<GridTreeVo | null>(null)
const mapRef = ref<HTMLDivElement | null>(null)
const events = ref<EventItem[]>([])

let mapInstance: any = null
const polygons: any[] = []
const eventMarkers: any[] = []

const gridCount = computed(() => {
  let count = 0
  const walk = (nodes: GridTreeVo[]) => {
    for (const n of nodes) {
      count++
      if (n.children) walk(n.children)
    }
  }
  walk(tree.value)
  return count
})

function levelLabel(level?: number) {
  return level === 1 ? '社区' : level === 2 ? '大网格' : level === 3 ? '小网格' : '-'
}

function levelTagType(level?: number) {
  return level === 1 ? 'primary' : level === 2 ? 'success' : 'warning'
}

function levelColor(level?: number): { fill: string; stroke: string } {
  switch (level) {
    case 1: return { fill: 'rgba(94,162,255,0.12)', stroke: '#5ea2ff' }
    case 2: return { fill: 'rgba(82,196,26,0.15)', stroke: '#73d13d' }
    case 3: return { fill: 'rgba(250,173,20,0.18)', stroke: '#ffc53d' }
    default: return { fill: 'rgba(140,140,140,0.12)', stroke: '#8c8c8c' }
  }
}

function handleNodeClick(data: GridTreeVo) {
  selectedGrid.value = data
  if (data.roiJson && mapInstance) {
    try {
      const coords = JSON.parse(data.roiJson)
      if (Array.isArray(coords) && coords.length >= 3) {
        mapInstance.setFitView(null, false, [60, 60, 60, 60])
        mapInstance.setCenter(coords[0])
      }
    } catch (e) { /* ignore */ }
  }
}

function urgencyColor(level?: string): string {
  switch (level) {
    case 'RED': return '#ff4d4f'
    case 'YELLOW': return '#faad14'
    case 'GREEN': return '#52c41a'
    default: return '#8c8c8c'
  }
}

function drawEventMarkers() {
  if (!mapInstance) return
  // Clear old event markers
  for (const m of eventMarkers) {
    mapInstance.remove(m)
  }
  eventMarkers.length = 0

  for (const evt of events.value) {
    if (!evt.longitude || !evt.latitude) continue
    const color = urgencyColor(evt.urgencyLevel)
    const marker = new (window as any).AMap.Marker({
      position: [evt.longitude, evt.latitude],
      content: `<div style="width:14px;height:14px;border-radius:50%;background:${color};border:2px solid #fff;box-shadow:0 0 6px ${color};"></div>`,
      offset: new (window as any).AMap.Pixel(-7, -7),
      extData: evt,
    })
    marker.on('click', () => {
      const info = new (window as any).AMap.InfoWindow({
        content: `<div style="padding:8px 12px;background:#132a45;color:#eef5ff;border:1px solid rgba(125,163,220,0.18);border-radius:8px;">
          <strong>${evt.title}</strong><br/>
          <span style="color:${color};">● ${evt.urgencyLevel === 'RED' ? '紧急' : evt.urgencyLevel === 'YELLOW' ? '重点' : '一般'}</span>
        </div>`,
        offset: new (window as any).AMap.Pixel(0, -10),
      })
      info.open(mapInstance, [evt.longitude, evt.latitude])
    })
    mapInstance.add(marker)
    eventMarkers.push(marker)
  }
}

function clearMapFeatures() {
  // Remove old polygons and labels
  for (const p of polygons) { mapInstance.remove(p) }
  for (const m of eventMarkers) { mapInstance.remove(m) }
  polygons.length = 0
  eventMarkers.length = 0
}

function drawAllGrids() {
  if (!mapInstance) return
  clearMapFeatures()

  const walk = (nodes: GridTreeVo[]) => {
    for (const grid of nodes) {
      if (grid.roiJson) {
        try {
          const coords = JSON.parse(grid.roiJson)
          if (Array.isArray(coords) && coords.length >= 3) {
            const colors = levelColor(grid.gridLevel)
            const polygon = new (window as any).AMap.Polygon({
              path: coords,
              fillColor: colors.fill,
              fillOpacity: 1,
              strokeColor: colors.stroke,
              strokeWeight: grid.gridLevel === 1 ? 2 : 1,
              strokeStyle: grid.gridLevel === 1 ? 'solid' : 'dashed',
              extData: grid,
            })
            polygon.on('click', () => {
              selectedGrid.value = grid
            })
            polygon.on('mouseover', () => {
              polygon.setOptions({ fillColor: colors.stroke, fillOpacity: 0.3 })
            })
            polygon.on('mouseout', () => {
              polygon.setOptions({ fillColor: colors.fill, fillOpacity: 1 })
            })
            mapInstance.add(polygon)
            polygons.push(polygon)

            // Add label at center
            const center = coords[0]
            const label = new (window as any).AMap.Text({
              text: grid.gridName,
              position: center,
              style: {
                'background-color': 'rgba(14,35,58,0.85)',
                'border': '1px solid rgba(125,163,220,0.3)',
                'color': '#eef5ff',
                'font-size': '12px',
                'padding': '2px 8px',
                'border-radius': '4px',
              },
              extData: grid,
            })
            label.on('click', () => { selectedGrid.value = grid })
            mapInstance.add(label)
          }
        } catch (e) {
          console.warn('绘制网格失败:', grid.gridName, e)
        }
      }
      if (grid.children) walk(grid.children)
    }
  }

  walk(tree.value)

  if (polygons.length > 0) {
    mapInstance.setFitView(polygons, false, [40, 40, 40, 40])
  }
}

async function loadTree() {
  try {
    tree.value = await getGridTree()
  } catch (e) {
    console.error('加载网格树失败', e)
  }
}

async function loadEvents() {
  try {
    const result = await http.get<{ items: EventItem[] }, { items: EventItem[] }>(
      '/events', { params: { page: 1, size: 500 } }
    )
    events.value = result.items.filter(e => e.longitude && e.latitude)
  } catch (e) {
    console.error('加载事件失败', e)
  }
}

onMounted(async () => {
  await Promise.all([loadTree(), loadEvents()])

  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.MouseTool', 'AMap.Polygon', 'AMap.Text', 'AMap.Marker', 'AMap.InfoWindow'],
    })

    if (mapRef.value) {
      mapInstance = new AMap.Map(mapRef.value, {
        zoom: 14,
        center: [113.939521, 22.971231],
        mapStyle: 'amap://styles/dark',
      })

      drawAllGrids()
      drawEventMarkers()
    }
  } catch (e) {
    console.error('地图加载失败', e)
  }
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
  }
})
</script>

<style scoped>
.grid-view { display: flex; flex-direction: column; height: 100%; gap: 16px; }
.grid-view__header h2 { margin: 0; font-size: 20px; color: var(--fg-text-primary); }
.grid-view__subtitle { margin: 4px 0 0; color: var(--fg-text-secondary); font-size: 13px; }
.grid-view__body { display: grid; grid-template-columns: 320px 1fr; gap: 16px; flex: 1; min-height: 0; }
.grid-view__sidebar { display: flex; flex-direction: column; gap: 16px; overflow-y: auto; }
.panel { background: var(--fg-bg-card); border: 1px solid var(--fg-border); border-radius: var(--fg-radius-lg); padding: 16px; }
.panel--detail { flex-shrink: 0; }
.panel__title { margin: 0 0 12px; font-size: 14px; color: var(--fg-text-primary); }
:deep(.el-tree) { background: transparent; color: var(--fg-text-primary); }
:deep(.el-tree-node__content) { background: transparent; }
:deep(.el-tree-node__content:hover) { background: rgba(94, 162, 255, 0.12); }
:deep(.el-tree-node.is-current > .el-tree-node__content) { background: rgba(94, 162, 255, 0.2); }
:deep(.el-tree-node__label) { color: var(--fg-text-primary); }
:deep(.el-tree-node__expand-icon) { color: var(--fg-text-secondary); }
.grid-tree__node { display: flex; align-items: center; gap: 8px; width: 100%; }
.grid-tree__name { flex: 1; color: var(--fg-text-primary); }
.grid-tree__meta { color: var(--fg-text-secondary); font-size: 12px; }
:deep(.el-tag) { border: none; }
.legend { display: flex; flex-direction: column; gap: 8px; }
.legend__item { display: flex; align-items: center; gap: 8px; color: var(--fg-text-secondary); font-size: 13px; }
.legend__dot { width: 12px; height: 12px; border-radius: 3px; display: inline-block; }
.legend__dot--1 { background: rgba(94,162,255,0.3); border: 2px solid #5ea2ff; }
.legend__dot--2 { background: rgba(82,196,26,0.3); border: 2px dashed #52c41a; }
.legend__dot--3 { background: rgba(250,173,20,0.3); border: 2px dashed #faad14; }
.legend__dot--event { background: #ff4d4f; border-radius: 50%; }
.detail-list { display: grid; grid-template-columns: auto 1fr; gap: 8px 16px; margin: 0; }
.detail-list dt { color: #7ea4c8; font-size: 13px; }
.detail-list dd { margin: 0; color: #eaf5ff; font-size: 13px; }
.grid-view__map-container { border-radius: 16px; overflow: hidden; border: 1px solid rgba(110, 194, 255, 0.14); }
.grid-view__map { width: 100%; height: 100%; min-height: 500px; }
</style>
