<template>
  <div class="amap-point-picker">
    <div class="amap-point-picker__trigger" @click="openPicker">
      <input
        type="text"
        class="amap-point-picker__input"
        :value="displayText"
        readonly
        :placeholder="placeholder"
      />
      <button v-if="hasValue" type="button" class="amap-point-picker__clear" @click.stop="clearValue">&times;</button>
      <button type="button" class="amap-point-picker__btn" @click.stop="openPicker">选择</button>
    </div>

    <Teleport to="body">
      <div v-if="pickerVisible" class="amap-picker-overlay" @click.self="cancelPicker">
        <div class="amap-picker-dialog">
          <div class="amap-picker-header">
            <h4>选择经纬度</h4>
            <button type="button" class="amap-picker-close" @click="cancelPicker">&times;</button>
          </div>
          <div class="amap-picker-search-wrap">
            <div class="amap-picker-search">
              <input
                v-model="searchText"
                type="text"
                placeholder="请输入地点"
                class="amap-picker-search__input"
                @keyup.enter="doSearch"
              />
              <button type="button" class="amap-picker-search__btn" :disabled="searching" @click="doSearch">搜索</button>
            </div>
            <div v-if="searchResults.length > 0" class="amap-picker-results">
              <div
                v-for="(poi, idx) in searchResults"
                :key="idx"
                class="amap-picker-results__item"
                @click="selectPlace(poi)"
              >
                <div class="amap-picker-results__name">{{ poi.name }}</div>
                <div v-if="poi.address" class="amap-picker-results__addr">{{ poi.address }}</div>
              </div>
            </div>
          </div>
          <div class="amap-picker-tip">点击地图选择位置</div>
          <div ref="pickerMapContainer" class="amap-picker-map"></div>
          <div v-if="tempLng !== null" class="amap-picker-coords">
            已选择：{{ tempLng }}, {{ tempLat }}
          </div>
          <div class="amap-picker-footer">
            <button type="button" class="amap-picker-footer__cancel" @click="cancelPicker">取消</button>
            <button type="button" class="amap-picker-footer__confirm" :disabled="tempLng === null" @click="confirmPicker">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

const props = defineProps<{
  longitude?: number | string | null
  latitude?: number | string | null
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:longitude': [value: number | null]
  'update:latitude': [value: number | null]
}>()

const pickerVisible = ref(false)
const searchText = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)
const tempLng = ref<number | null>(null)
const tempLat = ref<number | null>(null)
const pickerMapContainer = ref<HTMLDivElement>()

let pickerMap: any = null
let pickerMarker: any = null
let AMapLib: any = null
let placeSearch: any = null

const hasValue = computed(() => {
  const lng = Number(props.longitude)
  const lat = Number(props.latitude)
  return Number.isFinite(lng) && Number.isFinite(lat) && (lng !== 0 || lat !== 0)
})

const displayText = computed(() => {
  if (!hasValue.value) return ''
  return `${props.longitude}, ${props.latitude}`
})

function clearValue() {
  emit('update:longitude', null)
  emit('update:latitude', null)
}

function openPicker() {
  pickerVisible.value = true
  searchText.value = ''
  searchResults.value = []
  tempLng.value = hasValue.value ? Number(props.longitude) : null
  tempLat.value = hasValue.value ? Number(props.latitude) : null
  void nextTick(() => initPickerMap())
}

function cancelPicker() {
  destroyPickerMap()
  pickerVisible.value = false
}

function confirmPicker() {
  if (tempLng.value !== null && tempLat.value !== null) {
    emit('update:longitude', tempLng.value)
    emit('update:latitude', tempLat.value)
  }
  destroyPickerMap()
  pickerVisible.value = false
}

function setTempMarker(lng: number, lat: number) {
  tempLng.value = Math.round(lng * 1000000) / 1000000
  tempLat.value = Math.round(lat * 1000000) / 1000000

  if (!pickerMap || !AMapLib) return
  const position = new AMapLib.LngLat(lng, lat)

  if (pickerMarker) {
    pickerMarker.setPosition(position)
  } else {
    pickerMarker = new AMapLib.Marker({
      position,
      anchor: 'bottom-center'
    })
    pickerMap.add(pickerMarker)
  }
  pickerMap.setCenter(position)
}

async function doSearch() {
  if (!searchText.value.trim() || !placeSearch) return
  searching.value = true
  placeSearch.search(searchText.value.trim(), (status: string, result: any) => {
    searching.value = false
    if (status === 'complete' && result.poiList?.pois?.length > 0) {
      searchResults.value = result.poiList.pois
    } else {
      searchResults.value = []
    }
  })
}

function selectPlace(poi: any) {
  const lng = poi.location.getLng()
  const lat = poi.location.getLat()
  searchText.value = poi.name
  searchResults.value = []
  setTempMarker(lng, lat)
  pickerMap.setZoom(16)
}

async function initPickerMap() {
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.PlaceSearch']
  })

  if (!pickerMapContainer.value) return

  const center = hasValue.value
    ? [Number(props.longitude), Number(props.latitude)]
    : [113.866, 22.982]

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  pickerMap = new AMapLib.Map(pickerMapContainer.value, {
    zoom: hasValue.value ? 16 : 14,
    center,
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer]
  })

  placeSearch = new AMapLib.PlaceSearch({ city: '全国', pageSize: 10 })

  pickerMap.on('click', (e: any) => {
    setTempMarker(e.lnglat.getLng(), e.lnglat.getLat())
  })

  if (hasValue.value) {
    setTempMarker(Number(props.longitude), Number(props.latitude))
  }
}

function destroyPickerMap() {
  if (pickerMarker && pickerMap) {
    pickerMap.remove(pickerMarker)
    pickerMarker = null
  }
  if (pickerMap) {
    pickerMap.destroy()
    pickerMap = null
  }
  placeSearch = null
}

watch(pickerVisible, (v) => {
  if (!v) destroyPickerMap()
})
</script>

<style scoped>
.amap-point-picker {
  width: 100%;
}

.amap-point-picker__trigger {
  display: flex;
  align-items: center;
  gap: 0;
  width: 100%;
}

.amap-point-picker__input {
  flex: 1;
  min-height: 32px;
  box-sizing: border-box;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-right: none;
  border-radius: 3px 0 0 3px;
  background: #1a344b;
  color: #eaf5ff;
  font: inherit;
  padding: 0 12px;
  cursor: pointer;
}

.amap-point-picker__input::placeholder {
  color: rgba(200, 220, 240, 0.4);
}

.amap-point-picker__clear {
  min-height: 32px;
  padding: 0 8px;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-left: none;
  border-right: none;
  background: #1a344b;
  color: rgba(255, 100, 100, 0.8);
  font-size: 16px;
  cursor: pointer;
  transition: color 0.2s;
}

.amap-point-picker__clear:hover {
  color: #ff6464;
}

.amap-point-picker__btn {
  min-height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(64, 158, 255, 0.6);
  border-radius: 0 3px 3px 0;
  background: rgba(64, 158, 255, 0.2);
  color: #8dc5ff;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}

.amap-point-picker__btn:hover {
  background: rgba(64, 158, 255, 0.35);
}

.amap-picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
}

.amap-picker-dialog {
  width: 680px;
  max-width: 95vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  background: #0e2236;
  border: 1px solid rgba(103, 187, 246, 0.2);
  border-radius: 8px;
  overflow: hidden;
}

.amap-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(103, 187, 246, 0.12);
}

.amap-picker-header h4 {
  margin: 0;
  color: #eaf5ff;
  font-size: 16px;
}

.amap-picker-close {
  background: none;
  border: none;
  color: #8db0d0;
  font-size: 22px;
  cursor: pointer;
  transition: color 0.2s;
}

.amap-picker-close:hover {
  color: #fff;
}

.amap-picker-search-wrap {
  position: relative;
  padding: 12px 20px 0;
}

.amap-picker-search {
  display: flex;
  gap: 0;
}

.amap-picker-results {
  position: absolute;
  top: 100%;
  left: 20px;
  right: 20px;
  max-height: 220px;
  overflow-y: auto;
  background: rgba(10, 28, 46, 0.95);
  border: 1px solid rgba(103, 187, 246, 0.2);
  border-top: none;
  border-radius: 0 0 4px 4px;
  z-index: 10;
}

.amap-picker-results__item {
  padding: 10px 14px;
  cursor: pointer;
  border-bottom: 1px solid rgba(103, 187, 246, 0.08);
  transition: background 0.15s;
}

.amap-picker-results__item:hover {
  background: rgba(64, 158, 255, 0.15);
}

.amap-picker-results__item:last-child {
  border-bottom: none;
}

.amap-picker-results__name {
  color: #eaf5ff;
  font-size: 13px;
  font-weight: 500;
}

.amap-picker-results__addr {
  color: rgba(200, 220, 240, 0.6);
  font-size: 12px;
  margin-top: 2px;
}

.amap-picker-search__input {
  flex: 1;
  min-height: 34px;
  box-sizing: border-box;
  border: 1px solid rgba(75, 119, 159, 0.9);
  border-right: none;
  border-radius: 3px 0 0 3px;
  background: #1a344b;
  color: #eaf5ff;
  font: inherit;
  padding: 0 12px;
}

.amap-picker-search__input::placeholder {
  color: rgba(200, 220, 240, 0.4);
}

.amap-picker-search__btn {
  min-height: 34px;
  padding: 0 20px;
  border: none;
  border-radius: 0 3px 3px 0;
  background: #409eff;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.amap-picker-search__btn:hover {
  background: #66b1ff;
}

.amap-picker-tip {
  padding: 6px 20px 0;
  font-size: 12px;
  color: rgba(200, 220, 240, 0.5);
}

.amap-picker-map {
  height: 400px;
  margin: 8px 20px 0;
  border-radius: 4px;
  border: 1px solid rgba(103, 187, 246, 0.15);
  overflow: hidden;
}

.amap-picker-coords {
  padding: 8px 20px 0;
  font-size: 13px;
  color: #8dc5ff;
}

.amap-picker-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 20px;
}

.amap-picker-footer__cancel,
.amap-picker-footer__confirm {
  min-height: 32px;
  padding: 0 20px;
  border-radius: 3px;
  font-size: 13px;
  cursor: pointer;
}

.amap-picker-footer__cancel {
  border: 1px solid rgba(115, 142, 167, 0.75);
  background: rgba(255, 255, 255, 0.04);
  color: #d9e8f5;
}

.amap-picker-footer__confirm {
  border: none;
  background: #409eff;
  color: #fff;
}

.amap-picker-footer__confirm:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.amap-picker-footer__confirm:not(:disabled):hover {
  background: #66b1ff;
}
</style>
