<template>
  <view class="resident-report-page">
    <view class="header">
      <text class="header-title">📸 随手拍</text>
      <text class="header-sub">发现身边问题，一键上报</text>
    </view>

    <view class="card">
      <text class="card-title">问题类型</text>
      <view class="type-grid">
        <view v-for="t in types" :key="t.value" class="type-item"
              :class="{ active: form.type === t.value }" @click="form.type = t.value">
          <text class="type-icon">{{ t.icon }}</text>
          <text class="type-label">{{ t.label }}</text>
        </view>
      </view>
    </view>

    <view class="card">
      <text class="card-title">问题描述</text>
      <text class="selected-type">已选择：{{ selectedType.label }}</text>
      <textarea v-model="form.description" placeholder="请详细描述您发现的问题..." class="form-textarea" placeholder-style="color:#999;font-size:30rpx;" />
    </view>

    <view class="card">
      <view class="card-title-row">
        <text class="card-title">现场照片</text>
        <text v-if="uploading" class="uploading">（上传中...）</text>
      </view>
      <view class="photo-list">
        <view v-for="(p, idx) in photos" :key="idx" class="photo-item">
          <image v-if="p.url" :src="p.url" class="photo-img" mode="aspectFill" />
          <view v-else class="photo-placeholder">📷 {{ idx + 1 }}</view>
          <view class="photo-del" @click="removePhoto(idx)">×</view>
        </view>
        <view v-if="photos.length < 3" class="photo-add" @click="addPhoto">
          <text class="photo-add-icon">+</text>
          <text>拍照/选图</text>
        </view>
      </view>
      <!-- H5 选图入口 -->
      <!-- #ifndef MP-WEIXIN -->
      <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif" style="display:none" @change="onFilesSelected" />
      <!-- #endif -->
    </view>

    <view class="card">
      <text class="card-title">位置信息</text>
      <view class="location">
        <text class="location-text">{{ locationText }}</text>
        <text class="relocate" @click="locate">重新定位</text>
      </view>
    </view>

    <view class="card">
      <text class="card-title">联系方式（选填）</text>
      <!-- 纯原生无样式input测试 -->
      <input placeholder="测试输入框1" style="width:100%;height:80rpx;border:1px solid red;color:#000;background:#fff;font-size:28rpx;box-sizing:border-box;padding:0 20rpx;" />
      <view style="height:20rpx;"></view>
      <input placeholder="测试输入框2" />
      <view style="height:20rpx;"></view>
      <input v-model="form.contactName" placeholder="您的姓名" class="form-input" placeholder-style="color:#999;font-size:30rpx;" />
      <input v-model="form.contactPhone" placeholder="联系电话" class="form-input" placeholder-style="color:#999;font-size:30rpx;" style="margin-top:20rpx;" />
    </view>

    <button class="btn-submit" :disabled="loading" @click="handleSubmit">
      {{ loading ? '提交中...' : '提交上报' }}
    </button>
    <text v-if="error" class="error">{{ error }}</text>
    <text v-if="success" class="success">{{ success }}</text>

    <ResidentTabBar current="/pages/resident/report/index" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref, reactive } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { reportEvent, uploadMedia } from '../../../src/api/resident'

const loading = ref(false)
const error = ref('')
const success = ref('')
const uploading = ref(false)
// 每张照片：{ url: 本地预览或已上传 URL, uploadedUrl?: 服务端 URL }
const photos = ref<{ url: string; uploadedUrl?: string }[]>([])

// 定位状态
const locationText = ref('定位中...')
const latitude = ref<number | null>(null)
const longitude = ref<number | null>(null)

const AMAP_KEY = '5e00e01d2d2b6ca9e1eed533a15572e4'
const AMAP_SECURITY_CODE = '0a57a5453a660300283bebf7323d8bce'

const types = [
  { value: 'ROAD', label: '道路损坏', icon: '🛣️' },
  { value: 'LIGHT', label: '路灯故障', icon: '💡' },
  { value: 'PIPE', label: '管道破损', icon: '🔧' },
  { value: 'ENV', label: '环境卫生', icon: '🗑️' },
  { value: 'SAFE', label: '安全隐患', icon: '⚠️' },
  { value: 'NOISE', label: '噪音扰民', icon: '🔊' },
  { value: 'OTHER', label: '其他问题', icon: '📝' },
]

const form = reactive({
  type: 'ROAD',
  description: '',
  contactName: '',
  contactPhone: ''
})

const selectedType = computed(() => types.find(t => t.value === form.type) || types[0])

function addPhoto() {
  if (photos.value.length >= 3 || uploading.value) return
  // #ifdef MP-WEIXIN
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['album', 'camera'],
    success: (res) => {
      res.tempFilePaths.forEach((path) => uploadByPath(path))
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  fileInput.value?.click()
  // #endif
}

// #ifndef MP-WEIXIN
const fileInput = ref<HTMLInputElement | null>(null)

async function onFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  const remain = 3 - photos.value.length
  for (const file of files.slice(0, remain)) {
    const item = { url: URL.createObjectURL(file) }
    photos.value.push(item)
    uploading.value = true
    try {
      const res: any = await uploadMedia(file)
      item.uploadedUrl = res?.fileUrl
    } catch (err: any) {
      const idx = photos.value.indexOf(item)
      if (idx >= 0) photos.value.splice(idx, 1)
      error.value = typeof err === 'string' ? err : (err?.message || '照片上传失败')
    } finally {
      uploading.value = false
    }
  }
}
// #endif

// #ifdef MP-WEIXIN
async function uploadByPath(filePath: string) {
  if (photos.value.length >= 3 || uploading.value) return
  const item = { url: filePath }
  photos.value.push(item)
  uploading.value = true
  try {
    const res: any = await uploadMedia(filePath)
    item.uploadedUrl = res?.fileUrl
  } catch (err: any) {
    const idx = photos.value.indexOf(item)
    if (idx >= 0) photos.value.splice(idx, 1)
    error.value = err?.message || '照片上传失败'
  } finally {
    uploading.value = false
  }
}
// #endif

function removePhoto(idx: number) {
  photos.value.splice(idx, 1)
}

/** 定位：H5 优先浏览器精确定位（需 HTTPS），失败时用高德 IP 定位兑底；小程序用 uni.getLocation */
function locate() {
  locationText.value = '定位中...'
  // #ifdef MP-WEIXIN
  uni.getLocation({
    type: 'gcj02',
    success: (pos) => {
      latitude.value = pos.latitude
      longitude.value = pos.longitude
      locationText.value = `${pos.latitude.toFixed(6)}, ${pos.longitude.toFixed(6)}（精确定位）`
    },
    fail: () => {
      locationText.value = '定位失败，请检查手机定位权限后重试'
    }
  })
  // #endif
  // #ifndef MP-WEIXIN
  if (!('geolocation' in navigator)) {
    amapIpLocate()
    return
  }
  navigator.geolocation.getCurrentPosition(
    pos => {
      latitude.value = pos.coords.latitude
      longitude.value = pos.coords.longitude
      locationText.value = `${pos.coords.latitude.toFixed(6)}, ${pos.coords.longitude.toFixed(6)}（精确定位）`
    },
    () => amapIpLocate(),
    { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
  )
  // #endif
}

// #ifndef MP-WEIXIN
function loadAMap(): Promise<any> {
  const w = window as any
  if (w.AMap) return Promise.resolve(w.AMap)
  w._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY_CODE }
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}&plugin=AMap.CitySearch`
    script.onload = () => (w.AMap ? resolve(w.AMap) : reject(new Error('AMap 加载失败')))
    script.onerror = () => reject(new Error('AMap 脚本加载失败'))
    document.head.appendChild(script)
  })
}

async function amapIpLocate() {
  try {
    const AMap = await loadAMap()
    const citySearch = new AMap.CitySearch()
    citySearch.getLocalPosition((status: string, result: any) => {
      if (status === 'complete' && result?.bounds) {
        const bounds = result.bounds
        const lat = (bounds.getSouthWest().getLat() + bounds.getNorthEast().getLat()) / 2
        const lng = (bounds.getSouthWest().getLng() + bounds.getNorthEast().getLng()) / 2
        latitude.value = lat
        longitude.value = lng
        locationText.value = `${result.city || '当前位置'}（大致定位，精确位置需 HTTPS 环境）`
      } else {
        locationText.value = '定位失败：当前为 HTTP 环境，浏览器禁止精确定位；建议部署 HTTPS 后重试'
      }
    })
  } catch {
    locationText.value = '定位失败：当前为 HTTP 环境，浏览器禁止精确定位；建议部署 HTTPS 后重试'
  }
}
// #endif

onLoad(() => {
  locate()
})

async function handleSubmit() {
  if (!form.description.trim()) { error.value = '请填写问题描述'; return }
  if (uploading.value) { error.value = '照片上传中，请稍候'; return }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const photoUrls = photos.value.map(p => p.uploadedUrl).filter(Boolean) as string[]
    const result: any = await reportEvent({
      title: selectedType.value.label,
      description: form.description,
      type: form.type,
      contactName: form.contactName,
      contactPhone: form.contactPhone,
      photos: photoUrls,
      latitude: latitude.value,
      longitude: longitude.value
    })
    success.value = '上报成功！查询码：' + (result?.eventCode || result?.id || '')
    // 重置表单
    form.description = ''
    form.contactName = ''
    form.contactPhone = ''
    photos.value = []
  } catch (e: any) {
    error.value = e?.message || e || '上报失败'
  } finally {
    loading.value = false
  }
}
</script>

<style>
/* 居民端-随手拍页面 全局样式（小程序scoped样式不生效，使用全局class前缀） */
.resident-report-page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.resident-report-page .header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 24rpx;
  padding: 40rpx;
  color: #fff;
  margin-bottom: 32rpx;
}
.resident-report-page .header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; color: #fff; }
.resident-report-page .header-sub { font-size: 26rpx; opacity: 0.8; color: #fff; }
.resident-report-page .card {
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.resident-report-page .card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; color: #374151; display: block; }
.resident-report-page .card-title-row { display: flex; align-items: center; margin-bottom: 24rpx; }
.resident-report-page .card-title-row .card-title { margin-bottom: 0; margin-right: 16rpx; }
.resident-report-page .selected-type { margin-bottom: 16rpx; font-size: 26rpx; color: #1890ff; display: block; }
.resident-report-page .type-grid { display: flex; flex-wrap: wrap; margin: -8rpx; }
.resident-report-page .type-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: calc(25% - 16rpx);
  margin: 8rpx;
  padding: 20rpx 8rpx; border: 1px solid #d1d5db; border-radius: 12rpx;
  box-sizing: border-box;
  background: #fff;
  color: #111827;
}
.resident-report-page .type-item.active { border-color: #1890ff; background: #e6f4ff; color: #000; }
.resident-report-page .type-icon { font-size: 48rpx; line-height: 1; margin-bottom: 8rpx; }
.resident-report-page .type-label { font-size: 22rpx; color: #111827; line-height: 1.2; text-align: center; }
.resident-report-page .form-input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  border: 1px solid #d9d9d9;
  border-radius: 8rpx;
  font-size: 30rpx;
  color: #000000;
  background: #ffffff;
  box-sizing: border-box;
}
.resident-report-page .form-textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 20rpx 24rpx;
  border: 1px solid #d9d9d9;
  border-radius: 8rpx;
  font-size: 30rpx;
  color: #000000;
  background: #ffffff;
  box-sizing: border-box;
  line-height: 1.5;
}
.resident-report-page .photo-list { display: flex; gap: 16rpx; flex-wrap: wrap; }
.resident-report-page .photo-item { position: relative; width: 160rpx; height: 160rpx; }
.resident-report-page .photo-placeholder {
  width: 160rpx; height: 160rpx; background: #f3f4f6; border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center; font-size: 48rpx;
  color: #9ca3af;
}
.resident-report-page .photo-del {
  position: absolute; top: -12rpx; right: -12rpx; width: 40rpx; height: 40rpx;
  background: #ff4d4f; color: #fff; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 24rpx;
}
.resident-report-page .photo-add {
  width: 160rpx; height: 160rpx; border: 1px dashed #d1d5db; border-radius: 16rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #9ca3af; font-size: 24rpx;
}
.resident-report-page .photo-add-icon { font-size: 48rpx; color: #9ca3af; }
.resident-report-page .location { font-size: 26rpx; color: #6b7280; padding: 16rpx; background: #f9fafb; border-radius: 12rpx; display: flex; justify-content: space-between; align-items: center; }
.resident-report-page .location-text { flex: 1; color: #6b7280; }
.resident-report-page .relocate { color: #1890ff; margin-left: 16rpx; }
.resident-report-page .photo-img { width: 160rpx; height: 160rpx; border-radius: 16rpx; display: block; }
.resident-report-page .uploading { font-size: 24rpx; color: #1890ff; font-weight: normal; }
.resident-report-page .btn-submit {
  width: 100%; height: 90rpx; line-height: 90rpx; padding: 0; background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  color: #fff; border: none; border-radius: 16rpx; font-size: 32rpx; font-weight: 600; margin-top: 16rpx;
}
.resident-report-page .btn-submit[disabled] { opacity: 0.6; }
.resident-report-page .error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
.resident-report-page .success { color: #52c41a; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
</style>
