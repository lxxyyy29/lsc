<script setup lang="ts">
import { onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

export interface MapMarker {
  id: string | number
  longitude: number
  latitude: number
  title?: string
  label?: string
  color?: string
  icon?: string
  data?: unknown
}

const props = withDefaults(defineProps<{
  center?: [number, number]
  zoom?: number
  markers?: MapMarker[]
  polygons?: Array<{ path: Array<[number, number]>; color?: string; data?: unknown }>
  heatmap?: Array<[number, number, number]>
  showToolbar?: boolean
}>(), {
  center: () => [113.746262, 23.046237],
  zoom: 15,
  markers: () => [],
  polygons: () => [],
  heatmap: () => [],
  showToolbar: true
})

const emit = defineEmits<{
  (e: 'marker-click', marker: MapMarker): void
  (e: 'polygon-click', polygon: unknown): void
  (e: 'map-click', lnglat: { lng: number; lat: number }): void
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const mapInstance = shallowRef<any>(null)
const mapMarkers = shallowRef<any[]>([])
const mapPolygons = shallowRef<any[]>([])
const isReady = ref(false)
const errorMessage = ref('')

const amapKey = import.meta.env.VITE_AMAP_KEY || ''

async function initMap() {
  if (!containerRef.value) return

  try {
    const AMap = await AMapLoader.load({
      key: amapKey,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.MapType', 'AMap.HawkEye']
    })

    const map = new AMap.Map(containerRef.value, {
      zoom: props.zoom,
      center: props.center,
      mapStyle: 'amap://styles/darkblue',
      viewMode: '2D'
    })

    mapInstance.value = map

    if (props.showToolbar) {
      map.addControl(new AMap.Scale())
      map.addControl(new AMap.ToolBar({
        position: 'LB'
      }))
    }

    map.on('click', (event: any) => {
      emit('map-click', { lng: event.lnglat.getLng(), lat: event.lnglat.getLat() })
    })

    isReady.value = true
    renderMarkers()
    renderPolygons()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '地图加载失败'
    console.error('AMap init error:', error)
  }
}

function renderMarkers() {
  if (!mapInstance.value) return

  mapMarkers.value.forEach((m) => mapInstance.value.remove(m))
  mapMarkers.value = []

  if (!props.markers.length) return

  const AMap = (window as any).AMap
  if (!AMap) return

  props.markers.forEach((marker) => {
    const content = document.createElement('div')
    content.className = 'v2-map-marker'
    content.style.backgroundColor = marker.color || '#5ea2ff'
    content.title = marker.title || marker.label || ''
    content.innerHTML = `<span>${marker.label || ''}</span>`

    const mapMarker = new AMap.Marker({
      position: [marker.longitude, marker.latitude],
      content,
      offset: new AMap.Pixel(-10, -10),
      extData: marker
    })

    mapMarker.on('click', () => emit('marker-click', marker))
    mapMarkers.value.push(mapMarker)
  })

  mapInstance.value.add(mapMarkers.value)
}

function renderPolygons() {
  if (!mapInstance.value) return

  mapPolygons.value.forEach((p) => mapInstance.value.remove(p))
  mapPolygons.value = []

  if (!props.polygons.length) return

  const AMap = (window as any).AMap
  if (!AMap) return

  props.polygons.forEach((polygon) => {
    const mapPolygon = new AMap.Polygon({
      path: polygon.path,
      strokeColor: polygon.color || '#5ea2ff',
      strokeWeight: 2,
      fillColor: polygon.color || '#5ea2ff',
      fillOpacity: 0.18,
      extData: polygon.data
    })

    mapPolygon.on('click', () => emit('polygon-click', polygon.data))
    mapPolygons.value.push(mapPolygon)
  })

  mapInstance.value.add(mapPolygons.value)
}

function fitView() {
  if (!mapInstance.value) return
  const all: any[] = [...mapMarkers.value, ...mapPolygons.value]
  if (all.length) {
    mapInstance.value.setFitView(all)
  }
}

watch(() => props.markers, renderMarkers, { deep: true })
watch(() => props.polygons, renderPolygons, { deep: true })

onMounted(initMap)
onUnmounted(() => {
  mapInstance.value?.destroy?.()
})

defineExpose({
  mapInstance,
  fitView
})
</script>

<template>
  <div class="map-container">
    <div ref="containerRef" class="map-container__map" />
    <div v-if="!isReady && errorMessage" class="map-container__error">
      <p>{{ errorMessage }}</p>
      <p class="map-container__hint">请配置 VITE_AMAP_KEY 环境变量</p>
    </div>
    <div v-if="!isReady && !errorMessage" class="map-container__loading">地图加载中...</div>
  </div>
</template>

<style scoped>
.map-container {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 320px;
  border-radius: var(--v2-radius-lg);
  overflow: hidden;
  background: var(--v2-bg-card-strong);
}

.map-container__map {
  width: 100%;
  height: 100%;
}

.map-container__loading,
.map-container__error {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--v2-text-secondary);
  font-size: 14px;
}

.map-container__error {
  color: var(--v2-danger);
}

.map-container__hint {
  color: var(--v2-text-tertiary);
  font-size: 12px;
  margin-top: 8px;
}
</style>

<style>
.v2-map-marker {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
}
</style>
