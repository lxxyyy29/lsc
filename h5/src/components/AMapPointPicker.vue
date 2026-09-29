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

      <!-- 加载失败提示：地图不可用时给出明确原因，避免"打开一片空白"无从判断 -->
      <view v-if="mapError" class="picker-error">
        <text class="picker-error__text">{{ mapError }}</text>
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

/** 地图初始化失败原因（网络不可达 / key 限制 / 图层异常）。非空时浮层内显示提示，而不是空白一片 */
const mapError = ref('')

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
  // 地图可能未渲染成功（此时 mapError 有值），但通过搜索结果取点依然可用，不能因此报错
  if (pickerMap) pickerMap.setZoom(16)
}

/** 页面上是否已有正在加载/已加载的高德脚本（H5 里 geolocation.ts、map/index.vue 都用 <script> 方式引入） */
function findAmapScriptTag(): HTMLScriptElement | null {
  if (typeof document === 'undefined') return null
  const scripts = Array.from(document.getElementsByTagName('script'))
  return scripts.find(s => !!s.src && s.src.indexOf('webapi.amap.com') !== -1) || null
}

/** 轮询等待 window.AMap 就绪（用于等"正在加载中"的脚本，避免自己再加载一份） */
function waitForAmapReady(timeoutMs: number): Promise<any | null> {
  return new Promise(resolve => {
    const start = Date.now()
    const tick = () => {
      const w = window as any
      if (w.AMap) {
        resolve(w.AMap)
        return
      }
      if (Date.now() - start > timeoutMs) {
        resolve(null)
        return
      }
      setTimeout(tick, 150)
    }
    tick()
  })
}

/**
 * 获取 AMap 实例。
 *
 * 高德 JS API 2.0 禁止「多种 API 加载方式混用」：同一页面里既用 <script> 标签引入，
 * 又用 AMapLoader 二次加载，高德会直接抛出该错误并拒绝渲染 —— 表现就是浮层一片空白。
 * 本页（巡查打卡）进页面时 geolocation.ts 已用 <script> 方式加载过高德，
 * 所以这里必须优先复用 window.AMap（含等待在途脚本），只有在整页都没加载过时才自己加载。
 */
async function resolveAmapLib(): Promise<any> {
  const w = window as any
  if (w.AMap) return w.AMap

  if (findAmapScriptTag()) {
    const ready = await waitForAmapReady(6000)
    if (ready) return ready
  }

  return AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.PlaceSearch', 'AMap.TileLayer']
  })
}

/** 复用已有 AMap 实例时，补齐本组件需要的插件（它可能只带了 CitySearch） */
function ensurePlaceSearchPlugin(AMap: any): Promise<void> {
  return new Promise(resolve => {
    try {
      if (!AMap || typeof AMap.plugin !== 'function') {
        resolve()
        return
      }
      AMap.plugin(['AMap.PlaceSearch', 'AMap.TileLayer'], () => resolve())
      // 兜底：个别情况下回调不触发，2s 后继续，避免整段逻辑卡死
      setTimeout(resolve, 2000)
    } catch (e) {
      resolve()
    }
  })
}

async function initPickerMap() {
  // #ifdef MP-WEIXIN
  return
  // #endif
  mapError.value = ''
  try {
    // 仅在页面尚未配置时设置：与其它页面重复或冲突配置安全密钥同样会触发高德"混用"校验
    const amapWindow = window as any
    if (!amapWindow._AMapSecurityConfig) {
      amapWindow._AMapSecurityConfig = {
        securityJsCode: '0a57a5453a660300283bebf7323d8bce'
      }
    }

    // 复用页面已加载的高德实例（见 resolveAmapLib 注释：二次加载会被高德判为"混用"而拒绝渲染）
    AMapLib = await resolveAmapLib()
    // 复用来的实例可能只带了 CitySearch，补齐本组件要用的插件
    await ensurePlaceSearchPlugin(AMapLib)

    // 搜索与地图渲染解耦：先建好搜索，即使地图没渲染出来也能靠搜索结果取点
    placeSearch = new AMapLib.PlaceSearch({ city: '全国', pageSize: 10 })

    const container = document.getElementById(mapElId)
    if (!container) {
      mapError.value = '地图容器未就绪，请关闭后重试；也可直接手动填写打卡地址'
      return
    }

    const center: [number, number] = hasValue.value
      ? [Number(props.longitude), Number(props.latitude)]
      : props.useCurrentLocation
        ? [gpsLng.value, gpsLat.value]
        : [113.866, 22.982]

    // 卫星底图按需叠加：图层类不可用时退回默认矢量底图，
    // 绝不因为"底图类型"这种次要问题导致整张地图起不来
    const layers: any[] = []
    const satelliteCls = AMapLib.TileLayer?.Satellite
    const roadNetCls = AMapLib.TileLayer?.RoadNet
    if (typeof satelliteCls === 'function' && typeof roadNetCls === 'function') {
      try {
        layers.push(new satelliteCls(), new roadNetCls())
      } catch (e) {
        layers.length = 0
      }
    }

    pickerMap = new AMapLib.Map(container, {
      zoom: hasValue.value ? 16 : 14,
      center,
      viewMode: '2D',
      ...(layers.length ? { layers } : {}),
      resizeEnable: true
    })

    pickerMap.on('click', (e: any) => {
      setTempMarker(e.lnglat.getLng(), e.lnglat.getLat())
    })

    if (hasValue.value) {
      setTempMarker(Number(props.longitude), Number(props.latitude))
    }

    // 浮层由 v-if 渲染，创建瞬间容器尺寸可能还没稳定，补一次 resize 避免空白/半张地图
    setTimeout(() => {
      try {
        pickerMap?.resize()
      } catch (e) {
        /* 忽略：resize 失败不影响已渲染内容 */
      }
    }, 300)
  } catch (e: any) {
    const detail = (e && (e.message || e.info || e)) || '未知错误'
    mapError.value = `地图加载失败：${detail}。可用上方搜索框搜地点，或直接手动填写打卡地址。`
    console.error('[AMapPointPicker] 地图初始化失败', e)
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

/**
 * 暴露给父页面：父页面的「地图选点」入口可直接复用本组件的选点能力
 * （小程序端→微信原生选点；网页端→高德全屏选点浮层），避免各页面重复实现一套。
 */
defineExpose({ openPicker })
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

/* ── 地图加载失败提示 ── */
.picker-error {
  margin: 0 24rpx 16rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.35);
  flex-shrink: 0;
}

.picker-error__text {
  font-size: 26rpx;
  color: #fca5a5;
  line-height: 1.5;
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