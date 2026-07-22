<template>
  <div class="amap-roi-drawer">
    <div class="amap-roi-toolbar">
      <button type="button" class="amap-roi-btn" :class="{ active: drawing }" @click="startDraw">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5" />
        </svg>
        {{ hasPolygon ? '重新绘制' : '画多边形' }}
      </button>
      <button type="button" class="amap-roi-btn amap-roi-btn--danger" :disabled="!hasPolygon && !drawing" @click="clearPolygon">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
        </svg>
        清除
      </button>
      <span v-if="pointCount > 0" class="amap-roi-info">{{ pointCount }} 个顶点</span>
      <span v-if="drawing" class="amap-roi-hint">点击地图添加顶点，双击完成绘制</span>
    </div>
    <div ref="mapContainer" class="amap-roi-map">
      <div class="amap-search-box">
        <input
          v-model="searchText"
          type="text"
          class="amap-search-input"
          placeholder="搜索地点..."
          @input="handleSearchInput"
          @keydown.enter.prevent="handleSearchEnter"
        />
        <div v-if="searchResults.length" class="amap-search-dropdown">
          <div
            v-for="(item, index) in searchResults"
            :key="index"
            class="amap-search-item"
            @click="selectSearchResult(item)"
          >
            <span class="amap-search-name">{{ item.name }}</span>
            <span class="amap-search-addr">{{ item.district }}{{ item.address }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

interface RoiPoint {
  lng: number
  lat: number
}

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const mapContainer = ref<HTMLDivElement>()
const drawing = ref(false)
const hasPolygon = ref(false)
const pointCount = ref(0)
const searchText = ref('')
const searchResults = ref<any[]>([])

let mapInstance: any = null
let mouseTool: any = null
let currentPolygon: any = null
let polygonEditor: any = null
let AMapLib: any = null
let suppressWatch = false
let autoComplete: any = null
let searchDebounce: ReturnType<typeof setTimeout> | null = null

function parseRoiPoints(json: string): RoiPoint[] {
  if (!json?.trim()) return []
  try {
    const parsed = JSON.parse(json)
    if (
      Array.isArray(parsed) &&
      parsed.length >= 3 &&
      parsed.every((p: any) => p && typeof p.lng === 'number' && typeof p.lat === 'number')
    ) {
      return parsed
    }
  } catch {
    // ignore
  }
  return []
}

function pointsToJson(points: RoiPoint[]): string {
  if (points.length === 0) return ''
  return JSON.stringify(points)
}

function getPolygonPoints(polygon: any): RoiPoint[] {
  const path = polygon.getPath()
  if (!path || path.length === 0) return []
  return path.map((p: any) => ({
    lng: Math.round(p.getLng() * 1000000) / 1000000,
    lat: Math.round(p.getLat() * 1000000) / 1000000
  }))
}

function emitPoints() {
  if (!currentPolygon) {
    hasPolygon.value = false
    pointCount.value = 0
    suppressWatch = true
    emit('update:modelValue', '')
    return
  }
  const points = getPolygonPoints(currentPolygon)
  hasPolygon.value = points.length >= 3
  pointCount.value = points.length
  suppressWatch = true
  emit('update:modelValue', pointsToJson(points))
}

function removeCurrentPolygon() {
  if (polygonEditor) {
    polygonEditor.close()
    polygonEditor = null
  }
  if (currentPolygon && mapInstance) {
    mapInstance.remove(currentPolygon)
    currentPolygon = null
  }
  hasPolygon.value = false
  pointCount.value = 0
}

function showPolygon(points: RoiPoint[]) {
  if (!mapInstance || !AMapLib || points.length < 3) return
  removeCurrentPolygon()

  const path = points.map((p) => new AMapLib.LngLat(p.lng, p.lat))
  currentPolygon = new AMapLib.Polygon({
    path,
    strokeColor: '#409eff',
    strokeOpacity: 0.9,
    strokeWeight: 2,
    fillColor: '#409eff',
    fillOpacity: 0.25,
    strokeStyle: 'solid',
    cursor: 'pointer'
  })
  mapInstance.add(currentPolygon)

  polygonEditor = new AMapLib.PolygonEditor(mapInstance, currentPolygon)
  polygonEditor.open()
  polygonEditor.on('adjust', () => emitPoints())
  polygonEditor.on('addnode', () => emitPoints())
  polygonEditor.on('removenode', () => emitPoints())
  polygonEditor.on('end', () => emitPoints())

  hasPolygon.value = true
  pointCount.value = points.length
  mapInstance.setFitView([currentPolygon], false, [60, 60, 60, 60])
}

function startDraw() {
  if (!mouseTool) return
  // Clear existing polygon first
  removeCurrentPolygon()
  suppressWatch = true
  emit('update:modelValue', '')

  drawing.value = true
  mouseTool.polygon({
    strokeColor: '#409eff',
    strokeOpacity: 0.9,
    strokeWeight: 2,
    fillColor: '#409eff',
    fillOpacity: 0.25,
    strokeStyle: 'solid'
  })
}

function clearPolygon() {
  if (drawing.value && mouseTool) {
    mouseTool.close(true)
    drawing.value = false
  }
  removeCurrentPolygon()
  suppressWatch = true
  emit('update:modelValue', '')
}

function handleSearchInput() {
  if (searchDebounce) clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    const keyword = searchText.value.trim()
    if (!keyword || !autoComplete) {
      searchResults.value = []
      return
    }
    autoComplete.search(keyword, (status: string, result: any) => {
      if (status === 'complete' && result.tips) {
        searchResults.value = result.tips.filter((t: any) => t.location)
      } else {
        searchResults.value = []
      }
    })
  }, 300)
}

function handleSearchEnter() {
  if (searchResults.value.length > 0) {
    selectSearchResult(searchResults.value[0])
  }
}

function selectSearchResult(item: any) {
  if (!mapInstance || !item.location) return
  mapInstance.setZoomAndCenter(16, [item.location.lng, item.location.lat])
  searchText.value = item.name
  searchResults.value = []
}

async function initMap() {
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.MouseTool', 'AMap.PolygonEditor', 'AMap.AutoComplete']
  })

  if (!mapContainer.value) return

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  mapInstance = new AMapLib.Map(mapContainer.value, {
    zoom: 16,
    center: [113.866, 22.982],
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer]
  })

  mouseTool = new AMapLib.MouseTool(mapInstance)
  autoComplete = new AMapLib.AutoComplete({ city: '' })
  mouseTool.on('draw', (event: any) => {
    drawing.value = false
    const drawnOverlay = event.obj

    // Extract points from the drawn polygon
    const points = getPolygonPoints(drawnOverlay)

    // Remove the MouseTool-drawn overlay and close the tool
    mapInstance.remove(drawnOverlay)
    mouseTool.close(true)

    if (points.length >= 3) {
      // Show a clean static polygon
      showPolygon(points)
      suppressWatch = true
      emit('update:modelValue', pointsToJson(points))
    }
  })

  // Render existing polygon if modelValue provided
  const existingPoints = parseRoiPoints(props.modelValue)
  if (existingPoints.length >= 3) {
    showPolygon(existingPoints)
  }
}

watch(
  () => props.modelValue,
  (newVal) => {
    if (suppressWatch) {
      suppressWatch = false
      return
    }
    const points = parseRoiPoints(newVal)
    if (points.length >= 3) {
      showPolygon(points)
    } else if (!newVal?.trim()) {
      removeCurrentPolygon()
    }
  }
)

onMounted(() => {
  void initMap()
})

onBeforeUnmount(() => {
  if (searchDebounce) clearTimeout(searchDebounce)
  autoComplete = null
  if (polygonEditor) {
    polygonEditor.close()
    polygonEditor = null
  }
  if (mouseTool) {
    mouseTool.close(true)
    mouseTool = null
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.amap-roi-drawer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.amap-roi-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.amap-roi-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 1px solid rgba(64, 158, 255, 0.4);
  border-radius: 6px;
  background: rgba(64, 158, 255, 0.1);
  color: rgba(200, 220, 240, 0.9);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.amap-roi-btn:hover:not(:disabled) {
  background: rgba(64, 158, 255, 0.2);
  border-color: rgba(64, 158, 255, 0.6);
  color: #fff;
}

.amap-roi-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.amap-roi-btn.active {
  background: rgba(64, 158, 255, 0.3);
  border-color: #409eff;
  color: #fff;
}

.amap-roi-btn--danger {
  border-color: rgba(255, 100, 100, 0.4);
  background: rgba(255, 100, 100, 0.08);
}

.amap-roi-btn--danger:hover:not(:disabled) {
  background: rgba(255, 100, 100, 0.18);
  border-color: rgba(255, 100, 100, 0.6);
}

.amap-roi-info {
  font-size: 12px;
  color: rgba(64, 158, 255, 0.8);
  margin-left: 4px;
}

.amap-roi-hint {
  font-size: 12px;
  color: rgba(255, 200, 50, 0.85);
  animation: blink-hint 1.2s ease-in-out infinite;
}

@keyframes blink-hint {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.amap-roi-map {
  position: relative;
  width: 100%;
  height: 400px;
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  overflow: hidden;
}

.amap-search-box {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  width: 280px;
}

.amap-search-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(64, 158, 255, 0.4);
  border-radius: 6px;
  background: rgba(5, 11, 20, 0.88);
  color: #d7e3f5;
  font-size: 13px;
  outline: none;
  backdrop-filter: blur(8px);
  box-sizing: border-box;
}

.amap-search-input:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.amap-search-input::placeholder {
  color: rgba(177, 188, 206, 0.5);
}

.amap-search-dropdown {
  margin-top: 4px;
  max-height: 240px;
  overflow-y: auto;
  border-radius: 6px;
  border: 1px solid rgba(64, 158, 255, 0.25);
  background: rgba(5, 11, 20, 0.92);
  backdrop-filter: blur(8px);
}

.amap-search-item {
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid rgba(64, 158, 255, 0.08);
  transition: background 0.15s;
}

.amap-search-item:last-child {
  border-bottom: none;
}

.amap-search-item:hover {
  background: rgba(64, 158, 255, 0.15);
}

.amap-search-name {
  display: block;
  font-size: 13px;
  color: #d7e3f5;
}

.amap-search-addr {
  display: block;
  font-size: 11px;
  color: rgba(177, 188, 206, 0.6);
  margin-top: 2px;
}
</style>
