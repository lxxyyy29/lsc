<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">巡查打卡</view>
      <view class="hero-subtitle">记录巡查位置与现场情况</view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">所属网格</text>
        <picker :range="gridNameList" @change="onGridChange">
          <view class="picker-text">{{ selectedGridName || '请选择网格' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">当前位置</text>
        <!-- #ifdef MP-WEIXIN -->
        <!-- 地图可点击直接选点：此前地图无任何点击响应，用户反馈"点地图没反应" -->
        <map
          class="location-map"
          :latitude="mapLat"
          :longitude="mapLng"
          :scale="16"
          :markers="mapMarkers"
          @tap="chooseLocation"
        ></map>
        <view class="map-hint">点地图或下方「地图选点」可选取打卡位置</view>
        <!-- #endif -->
        <view class="location-text">{{ locationText }}</view>
        <view class="location-actions">
          <text class="link" @click="getLocation">定位</text>
          <text class="link link-divider">|</text>
          <text class="link" @click="chooseLocation">地图选点</text>
        </view>
      </view>

      <!-- 地址输入 + 关键词联想（历史地址 + 网格名） -->
      <view class="form-item">
        <text class="label">打卡地址</text>
        <view class="address-row">
          <input
            v-model="address"
            class="text-input"
            placeholder="请输入打卡地址"
            @input="onAddressInput"
            @focus="onAddressInput"
            @blur="closeSuggestions"
          />
          <!-- 输入框旁直接给一个选点入口：地图/输入框任一入口都能选点，避免"点不动"无路可走 -->
          <view class="address-pick" @click="chooseLocation">📍 选点</view>
        </view>
        <view v-if="suggestions.length" class="suggest-list">
          <view v-for="(s, idx) in suggestions" :key="idx" class="suggest-item" @mousedown.prevent="selectAddress(s)">
            {{ s }}
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">现场照片</text>
        <view class="photo-grid">
          <view v-for="(photo, idx) in photos" :key="idx" class="photo-item">
            <image :src="photo" mode="aspectFill" />
            <text class="photo-del" @click="removePhoto(idx)">×</text>
          </view>
          <view class="photo-add" @click="takePhoto" v-if="photos.length < 3">
            <text class="photo-add-icon">+</text>
            <text class="photo-add-text">拍照</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">巡查内容</text>
        <textarea v-model="content" class="textarea" placeholder="描述现场情况..." />
      </view>

      <view class="form-item">
        <text class="label">备注<text style="color:#5a7a9a;font-size:11px;">（非必填）</text></text>
        <textarea v-model="remark" class="textarea" rows="2" placeholder="选填备注..." />
      </view>
    </view>

    <view class="btn-submit" @click="handleSubmit">确认打卡</view>
    <GridWorkerTabBar current="/pages/patrol/checkin" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import GridWorkerTabBar from '../../src/components/GridWorkerTabBar.vue'
import { getGridTree, createPatrolRecord, getAddressSuggestions, GridTreeVo, PatrolRecord } from '../../src/api/community'
import { getH5Session } from '../../src/api/auth'
import { locateWithFallback } from '../../src/utils/geolocation'
import { enqueueOfflineTask, isNetworkError } from '../../src/utils/offlineQueue'
import { MP_ORIGIN } from '../../src/api/endpoints'

interface GridOption {
  label: string
  value: number
}

const gridTree = ref<GridTreeVo[]>([])
const gridOptions = ref<GridOption[]>([])
const selectedGridId = ref<number | null>(null)
const selectedGridName = ref('')
const locationText = ref('正在定位...')
const longitude = ref<number | null>(null)
const latitude = ref<number | null>(null)
const photos = ref<string[]>([])
const content = ref('')
// 打卡地址（手动输入 + 关键词联想）
const address = ref('')
const remark = ref('')
const suggestions = ref<string[]>([])
let suggestionTimer: any = null

// 小程序地图状态
const mapLat = ref(22.971231)
const mapLng = ref(113.939521)
const mapMarkers = ref<any[]>([])

function updateMapMarker(lat: number, lng: number) {
  mapLat.value = lat
  mapLng.value = lng
  mapMarkers.value = [{
    id: 1,
    latitude: lat,
    longitude: lng,
    iconPath: '../../static/map-marker.png',
    // 小地图（300rpx）内用迷你标记，避免大图标遮挡地名标注
    width: 16,
    height: 28,
    label: {
      content: '当前位置',
      color: '#ffffff',
      fontSize: 10,
      borderRadius: 8,
      bgColor: 'rgba(30, 64, 110, 0.85)',
      padding: 3,
      anchorX: -24,
      anchorY: 6
    }
  }]
}

const gridNameList = computed(() => gridOptions.value.map(g => g.label))

function flattenGrids(nodes: GridTreeVo[], prefix: string = '') {
  nodes.forEach(n => {
    const name = prefix ? `${prefix} > ${n.gridName}` : n.gridName
    gridOptions.value.push({ label: name, value: n.id })
    if (n.children) flattenGrids(n.children, name)
  })
}

function onGridChange(e: any) {
  const idx = Number(e.detail.value)
  const option = gridOptions.value[idx]
  if (option) {
    selectedGridId.value = option.value
    selectedGridName.value = option.label
  }
}

// ==================== 地址关键词联想（历史地址 + 网格名） ====================
function onAddressInput() {
  clearTimeout(suggestionTimer)
  const kw = address.value.trim()
  if (!kw) {
    suggestions.value = []
    return
  }
  suggestionTimer = setTimeout(async () => {
    try {
      suggestions.value = await getAddressSuggestions(kw) || []
    } catch (e) {
      suggestions.value = []
    }
  }, 200)
}

function selectAddress(s: string) {
  address.value = s
  suggestions.value = []
}

function closeSuggestions() {
  // 延迟关闭，保证点击联想项先触发 selectAddress
  setTimeout(() => { suggestions.value = [] }, 150)
}

function getLocation() {
  locationText.value = '定位中...'
  locateWithFallback().then((res) => {
    longitude.value = res.longitude
    latitude.value = res.latitude
    // #ifdef MP-WEIXIN
    updateMapMarker(res.latitude, res.longitude)
    // #endif
    locationText.value = res.precise
      ? `${res.sourceText}：经度 ${res.longitude.toFixed(6)}, 纬度 ${res.latitude.toFixed(6)}`
      : `${res.sourceText}：${res.longitude.toFixed(4)}, ${res.latitude.toFixed(4)}`
  }).catch(() => {
    locationText.value = '定位失败：未获得浏览器定位授权，且 IP 定位也失败，请检查定位权限后重试'
  })
}

/** 选点结果统一写入：经纬度 + 地图标记 + 打卡地址 */
function applyChosenLocation(res: any) {
  const lat = Number(res?.latitude)
  const lng = Number(res?.longitude)
  if (!lat || !lng) return
  longitude.value = Number(lng.toFixed(6))
  latitude.value = Number(lat.toFixed(6))
  // #ifdef MP-WEIXIN
  updateMapMarker(lat, lng)
  // #endif
  locationText.value = res.address || res.name || `${lat.toFixed(6)}, ${lng.toFixed(6)}`
  // 选点地址同步填入"打卡地址"输入框
  if (res.address || res.name) {
    address.value = res.address || res.name
    suggestions.value = []
  }
}

/**
 * 地图选点。
 * 历史问题：H5 端此函数是空实现、小程序端失败时只弹「取消选点」，
 * 用户反馈"点地图/点选点没反应"，无从判断原因。现在任何分支都有明确反馈。
 */
function chooseLocation() {
  // #ifdef MP-WEIXIN
  const wxRef = (globalThis as { wx?: { chooseLocation?: unknown } }).wx
  if (!wxRef || typeof wxRef.chooseLocation !== 'function') {
    uni.showModal({
      title: '地图选点不可用',
      content: '当前小程序未开通地理位置接口（个人主体小程序不开放该接口）。请改用「定位」，或直接在「打卡地址」里手动填写。',
      showCancel: false
    })
    return
  }
  uni.chooseLocation({
    success: (res: any) => applyChosenLocation(res),
    fail: (err: any) => {
      const msg = String(err?.errMsg || '')
      if (msg.includes('cancel')) {
        uni.showToast({ title: '已取消选点', icon: 'none' })
      } else {
        uni.showModal({
          title: '地图选点失败',
          content: '请确认已允许获取位置信息；若仍失败，可改用「定位」或手动填写打卡地址。',
          showCancel: false
        })
      }
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  // H5 网页端：uni.chooseLocation 依赖地图 key，能调则调，不能调给出明确提示（不再静默无反应）
  const uniRef = (globalThis as { uni?: { chooseLocation?: (options: any) => void } }).uni
  if (typeof uniRef?.chooseLocation === 'function') {
    uniRef.chooseLocation({
      success: (res: any) => applyChosenLocation(res),
      fail: () => uni.showToast({ title: '未选择位置', icon: 'none' })
    })
    return
  }
  uni.showModal({
    title: '地图选点不可用',
    content: '网页端未配置地图选点能力，请在小程序内使用，或直接手动填写「打卡地址」。',
    showCancel: false
  })
  // #endif
}

/** 媒体上传基址：小程序用绝对 HTTPS 域名，H5 用相对路径走代理 */
function resolveMediaBaseUrl(): string {
  // #ifdef MP-WEIXIN
  return MP_ORIGIN
  // #endif
  // #ifndef MP-WEIXIN
  return ''
  // #endif
}

function takePhoto() {
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res: any) => {
      const paths = res.tempFilePaths || []
      paths.forEach((p: string) => {
        // 上传文件
        uni.uploadFile({
          url: `${resolveMediaBaseUrl()}/api/media/upload`,
          filePath: p,
          name: 'file',
          formData: {
            businessType: 'PATROL'
          },
          header: {
            Authorization: `Bearer ${getH5Session()?.token || ''}`
          },
          success: (uploadRes: any) => {
            try {
              const data = JSON.parse(uploadRes.data)
              if (data.success && data.data?.fileUrl) {
                photos.value.push(data.data.fileUrl)
              }
            } catch (e) {
              console.error('解析上传响应失败', e)
            }
          },
          fail: () => {
            uni.showToast({ title: '上传失败', icon: 'none' })
          }
        })
      })
    },
    fail: () => {
      uni.showToast({ title: '请选择图片', icon: 'none' })
    }
  })
}

function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
}

async function handleSubmit() {
  if (!selectedGridId.value) {
    uni.showToast({ title: '请选择网格', icon: 'none' })
    return
  }
  if (!address.value.trim()) {
    uni.showToast({ title: '请输入打卡地址', icon: 'none' })
    return
  }

  const record: PatrolRecord = {
    gridId: selectedGridId.value!,
    longitude: longitude.value || undefined,
    latitude: latitude.value || undefined,
    address: address.value.trim(),
    content: content.value,
    remark: remark.value || undefined,
    photoUrls: photos.value,
    // 离线重试幂等键:同一次打卡重复提交只落一条记录
    clientRequestId: 'CKI-' + Date.now()
  }

  try {
    await createPatrolRecord(record)
    uni.showToast({ title: '打卡成功！', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/workbench/index' }), 1500)
  } catch (e: any) {
    if (isNetworkError(e)) {
      // 网络信号差:离线保存,恢复网络后自动同步
      enqueueOfflineTask('CHECKIN', record, `巡查打卡:${address.value.trim() || selectedGridId.value}`)
      uni.showModal({
        title: '已离线保存',
        content: '当前网络不可用,打卡记录已保存在本地,恢复网络后将自动上报。',
        showCancel: false,
        confirmText: '知道了'
      })
      setTimeout(() => uni.reLaunch({ url: '/pages/workbench/index' }), 500)
    } else {
      uni.showToast({ title: '打卡失败', icon: 'none' })
    }
  }
}

onMounted(async () => {
  try {
    gridTree.value = await getGridTree()
    flattenGrids(gridTree.value)
  } catch (e) { console.error(e) }
  getLocation()
})
</script>

<style scoped>
.container { padding: 20px 20px calc(208rpx + env(safe-area-inset-bottom)); background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.form-card { background: #0e233a; border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.form-item { margin-bottom: 16px; }
.label { display: block; font-size: 13px; color: #cfe5fb; margin-bottom: 8px; }
.picker-text { background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; font-size: 14px; }
.text-input { width: 100%; background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; font-size: 14px; box-sizing: border-box; }
.suggest-list { margin-top: 6px; background: #0a1d33; border-radius: 8px; overflow: hidden; }
.suggest-item { padding: 10px 12px; color: #cfe5fb; font-size: 13px; border-bottom: 1px solid #12263f; }
.suggest-item:active { background: #14304f; }
.location-text { font-size: 12px; color: #7ea4c8; margin-bottom: 4px; }
.location-map { width: 100%; height: 380rpx; border-radius: 12rpx; margin-bottom: 8rpx; }
.map-hint { font-size: 11px; color: #5a7a9a; margin-bottom: 6px; }
.location-actions { display: flex; align-items: center; gap: 12rpx; }
.address-row { display: flex; align-items: center; gap: 8px; }
.address-row .text-input { flex: 1; }
.address-pick { flex: none; padding: 10px 12px; background: #10344f; color: #57b9ff; border-radius: 8px; font-size: 13px; white-space: nowrap; }
.address-pick:active { background: #14304f; }
.link { font-size: 12px; color: #57b9ff; }
.link-divider { color: #3a5a7a; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.photo-item { width: 80px; height: 80px; position: relative; border-radius: 8px; overflow: hidden; }
.photo-item image { width: 100%; height: 100%; }
.photo-del { position: absolute; top: 0; right: 0; background: rgba(255,0,0,0.7); color: white; width: 20px; height: 20px; border-radius: 50%; text-align: center; line-height: 20px; font-size: 14px; }
.photo-add { width: 80px; height: 80px; border: 1px dashed #57b9ff; border-radius: 8px; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.photo-add-icon { font-size: 24px; color: #57b9ff; }
.photo-add-text { font-size: 11px; color: #7ea4c8; }
.textarea { width: 100%; background: #0a1d33; border-radius: 8px; padding: 10px; color: #eaf5ff; min-height: 80px; }
.btn-submit { background: linear-gradient(135deg, #57b9ff, #1e88e5); color: white; text-align: center; padding: 14px; border-radius: 12px; font-weight: bold; font-size: 16px; }
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
