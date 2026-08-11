<template>
  <view class="map-container">
    <!-- #ifdef MP-WEIXIN -->
    <!-- 小程序版：原生 map 组件 + markers（事件点） -->
    <map
      id="mpMapContainer"
      class="mp-map"
      :latitude="mpCenterLat"
      :longitude="mpCenterLng"
      :scale="mpScale"
      :markers="mpMarkers"
      @markertap="onMpMarkerTap"
    ></map>
    <!-- #endif -->

    <!-- #ifndef MP-WEIXIN -->
    <!-- 高德地图容器 -->
    <view id="h5MapContainer" class="map"></view>
    <!-- #endif -->

    <!-- 顶部信息栏 -->
    <view class="map-header">
      <view class="header-left">
        <text class="header-title">移动 GIS</text>
        <text class="header-sub">{{ myGridName || '全网格' }}</text>
      </view>
      <text class="header-count">{{ eventPoints.length }} 个事件</text>
    </view>

    <!-- 定位按钮 -->
    <view class="locate-btn" @click="locateMe">
      <text class="locate-icon">📍</text>
    </view>

    <!-- #ifndef MP-WEIXIN -->
    <!-- 缩放控制（小程序 map 自带缩放） -->
    <view class="zoom-controls">
      <view class="zoom-btn" @click="zoomIn">+</view>
      <view class="zoom-btn" @click="zoomOut">-</view>
    </view>
    <!-- #endif -->

    <!-- 地图加载失败提示 -->
    <view v-if="loadError" class="map-error">
      <text>地图加载失败，请检查网络后重试</text>
    </view>

    <!-- 底部事件/网格信息 -->
    <view v-if="selectedGrid" class="event-detail">
      <view class="event-header">
        <text class="event-title">{{ selectedGrid.gridName }}</text>
        <text class="event-close" @click="selectedGrid = null">×</text>
      </view>
      <view class="event-info">
        <text class="event-type">{{ levelText(selectedGrid.gridLevel) }}</text>
        <text class="event-status status-closed">{{ selectedGrid.status === 'ACTIVE' ? '启用中' : '已停用' }}</text>
      </view>
      <text class="event-address">编码：{{ selectedGrid.gridCode || '-' }} ｜ 面积：{{ selectedGrid.area || '-' }} km²</text>
      <text class="event-time">人口：{{ selectedGrid.population || '-' }} 人 ｜ 楼栋：{{ selectedGrid.buildingCount || '-' }} 栋</text>
      <view class="grid-focus-btn" @click="focusGrid(selectedGrid)">聚焦到此网格</view>
    </view>
    <GridWorkerTabBar current="/pages/map/index" />
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { locateWithFallback } from '../../src/utils/geolocation'
import { getH5Session } from '../../src/api/auth'

const AMAP_KEY = '5e00e01d2d2b6ca9e1eed533a15572e4'
const AMAP_SECURITY_CODE = '0a57a5453a660300283bebf7323d8bce'
const DEFAULT_CENTER: [number, number] = [113.939521, 22.971231]
const DEFAULT_ZOOM = 14

// ─── 小程序原生 map 状态 ───
const mpCenterLat = ref(22.971231)
const mpCenterLng = ref(113.939521)
const mpScale = ref(13)
const mpMarkers = ref<any[]>([])

function onMpMarkerTap(e: any) {
  const id = e?.detail?.markerId ?? e?.markerId
  const p = eventPoints.value.find((item: any) => item.id === id || item.eventId === id)
  if (p) selectedEventDetail(p)
}

function refreshMpMarkers() {
  mpMarkers.value = eventPoints.value
    .filter((p: any) => typeof p.lat === 'number' && typeof p.lng === 'number')
    .map((p: any, idx: number) => ({
      id: p.id ?? p.eventId ?? idx,
      latitude: p.lat,
      longitude: p.lng,
      title: p.title || '事件点位',
      iconPath: '../../static/map-marker.png',
      width: 28,
      height: 36,
      callout: {
        content: p.title || '事件点位',
        color: '#ffffff',
        fontSize: 12,
        borderRadius: 6,
        bgColor: '#1890ff',
        padding: 6,
        display: 'BYCLICK'
      }
    }))
}

interface GridNode {
  id: number
  gridName: string
  gridCode?: string
  gridLevel?: number
  area?: number
  population?: number
  buildingCount?: number
  status?: string
  roiJson?: string
  children?: GridNode[]
}

const eventPoints = ref<any[]>([])
const selectedGrid = ref<GridNode | null>(null)
const loadError = ref(false)
const viewMode = ref<'mine' | 'all'>('all')
const hasMyGrid = ref(false)
const myGridName = ref('')

let mapInstance: any = null
let AMapRef: any = null
let mapMarkers: any[] = []
let locateMarker: any = null
let myGridPolygons: any[] = []
let communityBounds: any = null
let myGridBounds: any = null

function getToken() {
  return getH5Session()?.token || ''
}

function loadAMapScript(): Promise<any> {
  const w = globalThis as any
  if (w.AMap) return Promise.resolve(w.AMap)
  w._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.onload = () => (w.AMap ? resolve(w.AMap) : reject(new Error('AMap 加载失败')))
    script.onerror = () => reject(new Error('AMap 脚本加载失败'))
    document.head.appendChild(script)
  })
}

function parseRoi(roiJson?: string): number[][] | null {
  if (!roiJson) return null
  try {
    const coords = JSON.parse(roiJson)
    return Array.isArray(coords) && coords.length >= 3 ? coords : null
  } catch { return null }
}

function boundsOfCoords(coords: number[][]) {
  const lngs = coords.map(c => c[0])
  const lats = coords.map(c => c[1])
  return {
    minLng: Math.min(...lngs), maxLng: Math.max(...lngs),
    minLat: Math.min(...lats), maxLat: Math.max(...lats)
  }
}

// 按 roiJson 计算网格视野并居中
function focusGrid(grid: GridNode) {
  if (!mapInstance || !AMapRef) return
  const coords = parseRoi(grid.roiJson)
  if (!coords) return
  const b = boundsOfCoords(coords)
  const bounds = new AMapRef.Bounds([b.minLng, b.minLat], [b.maxLng, b.maxLat])
  mapInstance.setBounds(bounds)
}

// 绘制三级网格边界
function drawGridPolygons(tree: GridNode[], myGridIds: Set<number>) {
  if (!mapInstance || !AMapRef) return
  const draw = (nodes: GridNode[]) => {
    for (const node of nodes) {
      const coords = parseRoi(node.roiJson)
      if (coords) {
        let fillColor = '#0284c7', strokeColor = '#0284c7', fillOpacity = 0.06, strokeWeight = 2
        if (node.gridLevel === 2) { fillColor = '#f59e0b'; strokeColor = '#f59e0b'; fillOpacity = 0.18 }
        if (node.gridLevel === 3) { fillColor = '#10b981'; strokeColor = '#10b981'; fillOpacity = 0.15 }
        const isMine = myGridIds.has(node.id)
        const polygon = new AMapRef.Polygon({
          path: coords,
          fillColor,
          fillOpacity: isMine ? Math.max(fillOpacity, 0.3) : fillOpacity,
          strokeColor: isMine ? '#ff4d4f' : strokeColor,
          strokeWeight: isMine ? 4 : strokeWeight,
          strokeStyle: 'solid',
          zIndex: isMine ? 12 : (node.gridLevel === 1 ? 1 : 5),
          bubble: true,
          map: mapInstance
        })
        polygon.on('click', () => { selectedGrid.value = node })
        if (isMine) {
          myGridPolygons.push(polygon)
          if (!myGridBounds) myGridBounds = boundsOfCoords(coords)
        }
        if (node.gridLevel === 1) communityBounds = boundsOfCoords(coords)
      }
      if (node.children) draw(node.children)
    }
  }
  draw(tree)
}

async function initMap() {
  try {
    AMapRef = await loadAMapScript()
    mapInstance = new AMapRef.Map('h5MapContainer', {
      zoom: DEFAULT_ZOOM,
      center: DEFAULT_CENTER,
      mapStyle: 'amap://styles/normal'
    })
    mapInstance.on('click', () => { selectedGrid.value = null })

    // 并行加载：网格树 + 我的网格
    const [treeRes, myRes]: any[] = await Promise.all([
      uni.request({ url: '/api/community/grids/h5/tree', method: 'GET', header: { Authorization: `Bearer ${getToken()}` } }),
      uni.request({ url: '/api/community/grids/h5/my-grid', method: 'GET', header: { Authorization: `Bearer ${getToken()}` } })
    ])

    const tree: GridNode[] = (treeRes.data && treeRes.data.code === 'OK') ? (treeRes.data.data || []) : []
    const myGrids: GridNode[] = (myRes.data && myRes.data.code === 'OK') ? (myRes.data.data || []) : []

    const myGridIds = new Set(myGrids.map(g => g.id))
    drawGridPolygons(tree, myGridIds)

    if (myGrids.length > 0) {
      hasMyGrid.value = true
      myGridName.value = myGrids.length === 1 ? myGrids[0].gridName : `我的网格（${myGrids.length}）`
      viewMode.value = 'mine'
      // 网格员打开地图先定位到自己负责的网格范围
      focusGrid(myGrids[0])
    } else if (communityBounds) {
      fitCommunity()
    }
  } catch (e) {
    console.error('地图初始化失败:', e)
    loadError.value = true
  }
}

function fitCommunity() {
  if (!mapInstance || !AMapRef || !communityBounds) return
  const b = communityBounds
  mapInstance.setBounds(new AMapRef.Bounds([b.minLng, b.minLat], [b.maxLng, b.maxLat]))
}

function switchView(mode: 'mine' | 'all') {
  if (mode === 'mine' && !hasMyGrid.value) {
    uni.showToast({ title: '当前账号未分配网格', icon: 'none' })
    return
  }
  viewMode.value = mode
  selectedGrid.value = null
  if (mode === 'all') {
    fitCommunity()
  } else if (myGridBounds) {
    const b = myGridBounds
    mapInstance.setBounds(new AMapRef.Bounds([b.minLng, b.minLat], [b.maxLng, b.maxLat]))
  }
}

function levelText(level?: number) {
  if (level === 1) return '社区'
  if (level === 2) return '大网格'
  if (level === 3) return '小网格'
  return '网格'
}

// 按事件状态生成彩色标记图标（data URI，避免依赖图片文件）
function markerIcon(status: string) {
  let color = '#1890ff'
  if (status === 'CLOSED') color = '#28a745'
  else if (status === 'PENDING_AUDIT') color = '#ff5252'
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="28" height="38" viewBox="0 0 28 38"><path d="M14 1C7.4 1 2 6.4 2 13c0 9 12 24 12 24s12-15 12-24c0-6.6-5.4-12-12-12z" fill="${color}"/><circle cx="14" cy="13" r="5" fill="#fff"/></svg>`
  return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
}

function renderMarkers() {
  if (!mapInstance || !AMapRef) return
  mapMarkers.forEach(m => mapInstance.remove(m))
  mapMarkers = eventPoints.value
    .filter(p => typeof p.lat === 'number' && typeof p.lng === 'number')
    .map(p => {
      const marker = new AMapRef.Marker({
        position: [p.lng, p.lat],
        icon: markerIcon(p.status),
        offset: new AMapRef.Pixel(-14, -38),
        title: p.title || ''
      })
      marker.on('click', () => {
        selectedGrid.value = null
        selectedEventDetail(p)
      })
      return marker
    })
  mapInstance.add(mapMarkers)
}

// 事件点位详情（复用底部面板结构，转换为网格信息格式）
function selectedEventDetail(p: any) {
  selectedGrid.value = {
    id: p.id,
    gridName: p.title || '事件点位',
    gridCode: p.eventType || p.event_type || '',
    gridLevel: undefined,
    status: p.status,
    area: undefined,
    population: undefined,
    buildingCount: undefined
  } as any
}

// 加载事件点位
async function loadEvents() {
  try {
    const res: any = await uni.request({
      url: '/api/events/h5/map-points',
      method: 'GET',
      header: { Authorization: `Bearer ${getToken()}` }
    })
    if (res.data && res.data.code === 'OK') {
      eventPoints.value = res.data.data || []
      // #ifdef MP-WEIXIN
      refreshMpMarkers()
      // #endif
      // #ifndef MP-WEIXIN
      renderMarkers()
      // #endif
    }
  } catch (e) {
    console.error('加载事件失败:', e)
  }
}

function zoomIn() { if (mapInstance) mapInstance.setZoom(Math.min(18, mapInstance.getZoom() + 1)) }
function zoomOut() { if (mapInstance) mapInstance.setZoom(Math.max(5, mapInstance.getZoom() - 1)) }

// 定位到当前位置（精确定位失败时回退 IP 大致定位）
function locateMe() {
  locateWithFallback().then((res) => {
    // #ifdef MP-WEIXIN
    mpCenterLat.value = res.latitude
    mpCenterLng.value = res.longitude
    mpScale.value = res.precise ? 16 : 12
    // #endif
    // #ifndef MP-WEIXIN
    const pos: [number, number] = [res.longitude, res.latitude]
    if (mapInstance) {
      mapInstance.setZoomAndCenter(res.precise ? 16 : 12, pos)
      if (AMapRef) {
        if (locateMarker) mapInstance.remove(locateMarker)
        locateMarker = new AMapRef.CircleMarker({
          center: pos,
          radius: 8,
          fillColor: '#1890ff',
          fillOpacity: 0.9,
          strokeColor: '#ffffff',
          strokeWeight: 2
        })
        mapInstance.add(locateMarker)
      }
    }
    // #endif
    if (!res.precise) {
      uni.showToast({ title: res.sourceText, icon: 'none' })
    }
  }).catch(() => {
    uni.showToast({ title: '定位失败', icon: 'none' })
  })
}

onMounted(async () => {
  // #ifndef MP-WEIXIN
  await initMap()
  // #endif
  loadEvents()
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style lang="scss" scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100vh;
}

.map {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
}

.mp-map {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
}

.map-header {
  position: absolute;
  top: 80rpx;
  left: 30rpx;
  right: 30rpx;
  background: rgba(6, 18, 31, 0.85);
  border-radius: 16rpx;
  padding: 20rpx 30rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  backdrop-filter: blur(10rpx);
  z-index: 10;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.header-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #ffffff;
}

.header-sub {
  font-size: 22rpx;
  color: #57b9ff;
}

.header-count {
  font-size: 24rpx;
  color: #8db0d0;
}

.view-switch {
  position: absolute;
  top: 210rpx;
  left: 30rpx;
  display: flex;
  background: rgba(6, 18, 31, 0.9);
  border-radius: 12rpx;
  overflow: hidden;
  z-index: 10;
}

.switch-item {
  padding: 14rpx 28rpx;
  font-size: 24rpx;
  color: #8db0d0;

  &.active {
    background: #1890ff;
    color: #ffffff;
    font-weight: 600;
  }

  &.disabled {
    opacity: 0.45;
  }
}

.map-error {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  text-align: center;
  color: #666;
  font-size: 26rpx;
  z-index: 10;
}

.locate-btn {
  position: absolute;
  bottom: 200rpx;
  right: 30rpx;
  width: 80rpx;
  height: 80rpx;
  background: rgba(6, 18, 31, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.3);
  z-index: 10;
}

.locate-icon {
  font-size: 36rpx;
}

.zoom-controls {
  position: absolute;
  bottom: 300rpx;
  right: 30rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  z-index: 10;
}

.zoom-btn {
  width: 64rpx;
  height: 64rpx;
  background: rgba(6, 18, 31, 0.9);
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 600;
}

.event-detail {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(6, 18, 31, 0.95);
  border-radius: 24rpx 24rpx 0 0;
  padding: 30rpx;
  backdrop-filter: blur(10rpx);
  z-index: 10;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.event-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #ffffff;
  flex: 1;
}

.event-close {
  font-size: 40rpx;
  color: #8db0d0;
  padding: 0 10rpx;
}

.event-info {
  display: flex;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.event-type {
  font-size: 22rpx;
  color: #57b9ff;
  background: rgba(87, 185, 255, 0.15);
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.event-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
}

.status-closed { color: #28a745; background: rgba(40, 167, 69, 0.15); }

.event-address {
  display: block;
  font-size: 24rpx;
  color: #8db0d0;
  margin-bottom: 8rpx;
}

.event-time {
  display: block;
  font-size: 22rpx;
  color: #6c757d;
}

.grid-focus-btn {
  margin-top: 16rpx;
  background: #1890ff;
  color: #ffffff;
  text-align: center;
  padding: 16rpx 0;
  border-radius: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
}
</style>
