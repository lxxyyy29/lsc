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
        <map
          class="location-map"
          :latitude="mapLat"
          :longitude="mapLng"
          :scale="16"
          :markers="mapMarkers"
        ></map>
        <!-- #endif -->
        <view class="location-text">{{ locationText }}</view>
        <view class="location-actions">
          <text class="link" @click="getLocation">定位</text>
          <text class="link link-divider">|</text>
          <text class="link" @click="chooseLocation">地图选点</text>
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
    </view>

    <view class="btn-submit" @click="handleSubmit">确认打卡</view>
    <GridWorkerTabBar current="/pages/patrol/checkin" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import GridWorkerTabBar from '../../src/components/GridWorkerTabBar.vue'
import { getGridTree, createPatrolRecord, GridTreeVo, PatrolRecord } from '../../src/api/community'
import { getH5Session } from '../../src/api/auth'
import { locateWithFallback } from '../../src/utils/geolocation'

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
    width: 28,
    height: 36
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

/** 地图选点：微信原生地图，返回地址名称（小程序端使用） */
function chooseLocation() {
  // #ifdef MP-WEIXIN
  uni.chooseLocation({
    success: (res: any) => {
      if (!res.latitude || !res.longitude) return
      longitude.value = Number(res.longitude.toFixed(6))
      latitude.value = Number(res.latitude.toFixed(6))
      updateMapMarker(Number(res.latitude), Number(res.longitude))
      locationText.value = res.address || res.name || `${res.latitude.toFixed(6)}, ${res.longitude.toFixed(6)}`
    },
    fail: () => {
      uni.showToast({ title: '取消选点', icon: 'none' })
    }
  })
  // #endif
}

/** 媒体上传基址：小程序用绝对 HTTPS 域名，H5 用相对路径走代理 */
function resolveMediaBaseUrl(): string {
  // #ifdef MP-WEIXIN
  return 'https://drone.kfktec.cn:8443'
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
  if (!content.value) {
    uni.showToast({ title: '请填写巡查内容', icon: 'none' })
    return
  }

  const record: PatrolRecord = {
    gridId: selectedGridId.value!,
    longitude: longitude.value || undefined,
    latitude: latitude.value || undefined,
    address: locationText.value,
    content: content.value,
    photoUrls: photos.value
  }

  try {
    await createPatrolRecord(record)
    uni.showToast({ title: '打卡成功！', icon: 'success' })
    setTimeout(() => uni.reLaunch({ url: '/pages/workbench/index' }), 1500)
  } catch (e) {
    uni.showToast({ title: '打卡失败', icon: 'none' })
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
.location-text { font-size: 12px; color: #7ea4c8; margin-bottom: 4px; }
.location-map { width: 100%; height: 300rpx; border-radius: 12rpx; margin-bottom: 8rpx; }
.location-actions { display: flex; align-items: center; gap: 12rpx; }
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
