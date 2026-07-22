<template>
  <view class="map-picker">
    <!-- Trigger: show current value or placeholder -->
    <view class="map-picker__trigger" @click="openPicker">
      <text v-if="hasValue" class="map-picker__value">{{ displayText }}</text>
      <text v-else class="map-picker__placeholder">{{ placeholder }}</text>
      <view class="map-picker__actions">
        <text v-if="hasValue" class="map-picker__clear" @click.stop="clearValue">&times;</text>
        <text class="map-picker__btn">选择</text>
      </view>
    </view>

    <!-- Full-screen map picker overlay -->
    <view v-if="pickerVisible" class="picker-overlay">
      <!-- Header -->
      <view class="picker-header">
        <text class="picker-header__cancel" @click="cancelPicker">取消</text>
        <text class="picker-header__title">选择位置</text>
        <text
          class="picker-header__confirm"
          :class="{ 'picker-header__confirm--disabled': tempLng === null }"
          @click="confirmPicker"
        >确定</text>
      </view>

      <!-- Search bar -->
      <view class="picker-search">
        <input
          class="picker-search__input"
          v-model="searchText"
          placeholder="搜索地点"
          placeholder-class="picker-search__placeholder"
          confirm-type="search"
          @confirm="doSearch"
        />
        <text class="picker-search__btn" @click="doSearch">搜索</text>
      </view>

      <!-- Search results dropdown -->
      <scroll-view v-if="searchResults.length > 0" class="picker-results" scroll-y>
        <view
          v-for="(poi, idx) in searchResults"
          :key="idx"
          class="picker-results__item"
          @click="selectPlace(poi)"
        >
          <text class="picker-results__name">{{ poi.name }}</text>
          <text v-if="poi.address" class="picker-results__addr">{{ poi.address }}</text>
        </view>
      </scroll-view>

      <!-- Map container — must be a real <div> for AMap SDK -->
      <view class="picker-map-wrap">
        <div class="picker-map" :id="mapElId"></div>
      </view>

      <!-- Coordinate display -->
      <view v-if="tempLng !== null" class="picker-coords">
        <text class="picker-coords__text">经度: {{ tempLng }}  纬度: {{ tempLat }}</text>
      </view>
      <view v-else class="picker-coords">
        <text class="picker-coords__hint">点击地图选择位置</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, nextTick, onMounted } from 'vue'
// #ifndef MP-WEIXIN
import AMapLoader from '@amap/amap-jsapi-loader'
// #endif

const props = defineProps<{
  longitude?: number | string | null
  latitude?: number | string | null
  placeholder?: string
  /** When true, auto-locate via GPS on mount and use as initial map center */
  useCurrentLocation?: boolean
}>()

const emit = defineEmits<{
  'update:longitude': [value: number | null]
  'update:latitude': [value: number | null]
}>()

const mapElId = 'h5-amap-picker-' + Math.random().toString(36).slice(2, 8)

const pickerVisible = ref(false)
const searchText = ref('')
const searchResults = ref<any[]>([])
const tempLng = ref<number | null>(null)
const tempLat = ref<number | null>(null)

// GPS-derived center used when useCurrentLocation=true and no value is set yet
const gpsLng = ref<number>(113.866)
const gpsLat = ref<number>(22.982)

let pickerMap: any = null
let pickerMarker: any = null
let AMapLib: any = null
let placeSearch: any = null

// Auto-locate via GPS when useCurrentLocation prop is set
onMounted(() => {
  if (!props.useCurrentLocation) return
  uni.getLocation({
    type: 'gcj02',
    success(res) {
      gpsLng.value = res.longitude
      gpsLat.value = res.latitude
    },
    fail() {
      // Keep default center [113.866, 22.982] on failure
    }
  })
})

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
  // #ifdef MP-WEIXIN
  uni.chooseLocation({
    latitude: hasValue.value ? Number(props.latitude) : undefined,
    longitude: hasValue.value ? Number(props.longitude) : undefined,
    success: (res: any) => {
      if (res.latitude && res.longitude) {
        emit('update:longitude', Math.round(res.longitude * 1000000) / 1000000)
        emit('update:latitude', Math.round(res.latitude * 1000000) / 1000000)
      }
    }
  })
  return
  // #endif
  pickerVisible.value = true
  searchText.value = ''
  searchResults.value = []
  tempLng.value = hasValue.value ? Number(props.longitude) : null
  tempLat.value = hasValue.value ? Number(props.latitude) : null
  // uni-app needs extra time after v-if toggle for DOM to be ready
  void nextTick(() => {
    setTimeout(() => initPickerMap(), 100)
  })
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
  placeSearch.search(searchText.value.trim(), (status: string, result: any) => {
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
  // #ifdef MP-WEIXIN
  return
  // #endif
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: '0a57a5453a660300283bebf7323d8bce'
  }

  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.PlaceSearch']
  })

  const container = document.getElementById(mapElId)
  if (!container) return

  const center = hasValue.value
    ? [Number(props.longitude), Number(props.latitude)]
    : props.useCurrentLocation
      ? [gpsLng.value, gpsLat.value]
      : [113.866, 22.982]

  const satelliteLayer = new AMapLib.TileLayer.Satellite()
  const roadNetLayer = new AMapLib.TileLayer.RoadNet()

  pickerMap = new AMapLib.Map(container, {
    zoom: hasValue.value ? 16 : 14,
    center,
    viewMode: '2D',
    layers: [satelliteLayer, roadNetLayer],
    resizeEnable: false
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
</script>

<style scoped>
/* ── Trigger ── */
.map-picker {
  width: 100%;
}

.map-picker__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18rpx 20rpx;
  border-radius: 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
  min-height: 80rpx;
}

.map-picker__value {
  font-size: 30rpx;
  color: #eef6ff;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-picker__placeholder {
  font-size: 30rpx;
  color: #5e7488;
  flex: 1;
}

.map-picker__actions {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-shrink: 0;
  margin-left: 12rpx;
}

.map-picker__clear {
  font-size: 40rpx;
  color: rgba(239, 68, 68, 0.7);
  line-height: 1;
  padding: 0 8rpx;
}

.map-picker__btn {
  font-size: 28rpx;
  color: #5ea2ff;
  font-weight: 600;
}

/* ── Full-screen overlay ── */
.picker-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  background: #060f18;
}

/* ── Header ── */
.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;
  background: rgba(6, 15, 24, 0.96);
  border-bottom: 1px solid rgba(125, 163, 220, 0.1);
  flex-shrink: 0;
}

.picker-header__cancel {
  font-size: 32rpx;
  color: #8ba1b4;
  min-width: 80rpx;
}

.picker-header__title {
  font-size: 34rpx;
  font-weight: 700;
  color: #f3f8ff;
}

.picker-header__confirm {
  font-size: 32rpx;
  color: #5ea2ff;
  font-weight: 600;
  min-width: 80rpx;
  text-align: right;
}

.picker-header__confirm--disabled {
  color: #5e7488;
  opacity: 0.5;
}

/* ── Search ── */
.picker-search {
  display: flex;
  gap: 0;
  padding: 16rpx 24rpx;
  background: rgba(6, 15, 24, 0.96);
  flex-shrink: 0;
}

.picker-search__input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
  border-radius: 12rpx 0 0 12rpx;
  background: rgba(20, 40, 65, 0.8);
  border: 1px solid rgba(125, 163, 220, 0.14);
  border-right: none;
  font-size: 30rpx;
  color: #eef6ff;
}

.picker-search__placeholder {
  color: #5e7488;
}

.picker-search__btn {
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 28rpx;
  border-radius: 0 12rpx 12rpx 0;
  background: linear-gradient(135deg, #3898fd, #2272d9);
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
  flex-shrink: 0;
}

/* ── Search results ── */
.picker-results {
  position: absolute;
  top: calc(24rpx + 28rpx + 28rpx + 16rpx + 72rpx + 16rpx);
  left: 24rpx;
  right: 24rpx;
  max-height: 400rpx;
  background: rgba(10, 28, 46, 0.97);
  border: 1px solid rgba(103, 187, 246, 0.2);
  border-radius: 12rpx;
  z-index: 10;
  overflow: hidden;
}

.picker-results__item {
  padding: 20rpx 24rpx;
  border-bottom: 1px solid rgba(103, 187, 246, 0.08);
}

.picker-results__item:last-child {
  border-bottom: none;
}

.picker-results__name {
  font-size: 30rpx;
  color: #eef6ff;
  font-weight: 500;
}

.picker-results__addr {
  font-size: 26rpx;
  color: rgba(200, 220, 240, 0.5);
  margin-top: 6rpx;
}

/* ── Map ── */
.picker-map-wrap {
  flex: 1;
  padding: 0 24rpx;
  min-height: 0;
}

.picker-map {
  width: 100%;
  height: 100%;
  border-radius: 12rpx;
  overflow: hidden;
  border: 1px solid rgba(125, 163, 220, 0.12);
}

/* ── Coordinates ── */
.picker-coords {
  padding: 20rpx 24rpx;
  flex-shrink: 0;
  text-align: center;
  background: rgba(6, 15, 24, 0.96);
  border-top: 1px solid rgba(125, 163, 220, 0.08);
}

.picker-coords__text {
  font-size: 28rpx;
  color: #5ea2ff;
  font-weight: 500;
}

.picker-coords__hint {
  font-size: 28rpx;
  color: #5e7488;
}
</style>