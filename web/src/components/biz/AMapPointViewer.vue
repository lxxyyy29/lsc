<template>
  <div class="amap-point-viewer">
    <div ref="mapContainer" class="amap-point-viewer__map"></div>
    <div v-if="!mapReady" class="amap-point-viewer__loading">地图加载中...</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

export interface AreaOverlay {
  areaName: string
  roiJson: string
}

const props = defineProps<{
  longitude: number
  latitude: number
  label?: string
  zoom?: number
  areas?: AreaOverlay[]
}>()

const mapContainer = ref<HTMLDivElement>()
const mapReady = ref(false)

let mapInstance: any = null
let marker: any = null
let infoWindow: any = null
let AMapLib: any = null
let areaPolygons: any[] = []
let areaLabels: any[] = []

function hasValidCoords(): boolean {
  return Number.isFinite(props.longitude) && Number.isFinite(props.latitude) && props.longitude !== 0 && props.latitude !== 0
}

function updateMarker() {
  if (!mapInstance || !AMapLib || !hasValidCoords()) return

  const position = new AMapLib.LngLat(props.longitude, props.latitude)

  if (marker) {
    marker.setPosition(position)
  } else {
    marker = new AMapLib.Marker({
      position,
      anchor: 'bottom-center'
    })
    mapInstance.add(marker)
  }

  const labelText = props.label || `${props.longitude}, ${props.latitude}`

  if (infoWindow) {
    infoWindow.setContent(`<div class="amap-info-label">经纬度：${labelText}</div>`)
    infoWindow.open(mapInstance, position)
  } else {
    infoWindow = new AMapLib.InfoWindow({
      content: `<div class="amap-info-label">经纬度：${labelText}</div>`,
      offset: new AMapLib.Pixel(0, -30),
      closeWhenClickMap: false,
      isCustom: false
    })
    infoWindow.open(mapInstance, position)
  }

  mapInstance.setCenter(position)
}

function clearAreaOverlays() {
  for (const polygon of areaPolygons) {
    mapInstance?.remove(polygon)
  }
  for (const label of areaLabels) {
    mapInstance?.remove(label)
  }
  areaPolygons = []
  areaLabels = []
}

function renderAreaOverlays() {
  if (!mapInstance || !AMapLib || !props.areas?.length) return

  clearAreaOverlays()

  for (const area of props.areas) {
    if (!area.roiJson) continue

    let points: Array<{ lng: number; lat: number }>
    try {
      points = JSON.parse(area.roiJson)
    } catch {
      continue
    }
    if (!Array.isArray(points) || points.length < 3) continue

    const path = points.map((p) => new AMapLib.LngLat(p.lng, p.lat))

    const polygon = new AMapLib.Polygon({
      path,
      strokeColor: '#409eff',
      strokeOpacity: 0.9,
      strokeWeight: 2,
      fillColor: '#409eff',
      fillOpacity: 0.15,
      strokeStyle: 'solid'
    })
    mapInstance.add(polygon)
    areaPolygons.push(polygon)

    // Calculate polygon center for label placement
    let sumLng = 0
    let sumLat = 0
    for (const p of points) {
      sumLng += p.lng
      sumLat += p.lat
    }
    const centerLng = sumLng / points.length
    const centerLat = sumLat / points.length

    const textMarker = new AMapLib.Text({
      text: area.areaName,
      position: new AMapLib.LngLat(centerLng, centerLat),
      anchor: 'center',
      style: {
        'background-color': 'rgba(64, 158, 255, 0.85)',
        'border': 'none',
        'border-radius': '4px',
        'color': '#fff',
        'font-size': '13px',
        'font-weight': '600',
        'padding': '4px 10px',
        'white-space': 'nowrap'
      }
    })
    mapInstance.add(textMarker)
    areaLabels.push(textMarker)
  }
}

async function initMap() {
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: []
  })

  if (!mapContainer.value) return

  const center = hasValidCoords()
    ? [props.longitude, props.latitude]
    : [113.866, 22.982]

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  mapInstance = new AMapLib.Map(mapContainer.value, {
    zoom: props.zoom ?? 15,
    center,
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer]
  })

  mapReady.value = true

  if (hasValidCoords()) {
    updateMarker()
  }

  renderAreaOverlays()
}

watch(
  () => [props.longitude, props.latitude, props.label],
  () => {
    if (mapReady.value) {
      updateMarker()
    }
  }
)

watch(
  () => props.areas,
  () => {
    if (mapReady.value) {
      renderAreaOverlays()
    }
  }
)

onMounted(() => {
  void initMap()
})

onBeforeUnmount(() => {
  clearAreaOverlays()
  if (infoWindow) {
    infoWindow.close()
    infoWindow = null
  }
  if (marker && mapInstance) {
    mapInstance.remove(marker)
    marker = null
  }
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
.amap-point-viewer {
  position: relative;
  width: 100%;
  height: 100%;
}

.amap-point-viewer__map {
  width: 100%;
  height: 100%;
}

.amap-point-viewer__loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(17, 34, 51, 0.85);
  color: #8db0d0;
  font-size: 14px;
  pointer-events: none;
}
</style>

<style>
.amap-info-label {
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  color: #333;
}
</style>
