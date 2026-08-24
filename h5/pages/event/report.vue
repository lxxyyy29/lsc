<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">事件上报</view>
      <view class="hero-subtitle">上报新事件，自动进入闭环处置流程</view>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">事件类型</text>
        <picker :range="typeNames" @change="onTypeChange">
          <view class="picker-text">{{ selectedTypeName || '请选择类型' }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">标题</text>
        <input v-model="title" class="input" placeholder="简要描述事件" placeholder-style="color:#5e7488;font-size:30rpx;" />
      </view>

      <view class="form-item">
        <text class="label">详细描述</text>
        <textarea v-model="content" class="textarea" placeholder="详细描述事件情况..." placeholder-style="color:#5e7488;font-size:30rpx;" />
      </view>

      <view class="form-item">
        <text class="label">事发地点</text>
        <view class="location-row">
          <input v-model="location" class="input" placeholder="输入地址后点「搜索」，或直接定位/地图选点" placeholder-style="color:#5e7488;font-size:30rpx;" />
          <view class="locate-btn" @click="searchAddress">
            <text class="locate-btn-text">搜索</text>
          </view>
          <view class="locate-btn locate-btn-secondary" @click="locateCurrent">
            <text class="locate-btn-text">定位</text>
          </view>
          <!-- #ifdef MP-WEIXIN -->
          <view class="locate-btn locate-btn-secondary" @click="chooseMapLocation">
            <text class="locate-btn-text">地图选点</text>
          </view>
          <!-- #endif -->
        </view>
        <view class="map-picker-wrap">
          <AMapPointPicker
            v-model:longitude="longitude"
            v-model:latitude="latitude"
            placeholder="在地图上选择事发位置（选填）"
            :use-current-location="false"
          />
        </view>
        <text v-if="locationHint" class="location-hint">{{ locationHint }}</text>
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
    </view>

    <view class="btn-submit" @click="handleSubmit">提交上报</view>

    <!-- 上报成功弹窗 -->
    <view v-if="showCodeDialog" class="mask" @click="showCodeDialog = false">
      <view class="dialog" @click.stop>
        <view class="dialog-title">上报成功！</view>
        <view class="dialog-text">事件编号：</view>
        <view class="dialog-code">{{ eventCode }}</view>
        <view class="dialog-text">事件已进入闭环处置流程，等待派单处理</view>
        <view class="dialog-btn" @click="goHistory">查看上报记录</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import AMapPointPicker from '../../src/components/AMapPointPicker.vue'
import { createEventForH5, EventCreatePayload } from '../../src/api/event'
import { locateWithFallback } from '../../src/utils/geolocation'
import { getH5Session } from '../../src/api/auth'
import { navigateToPath } from '../../src/uni/navigation'
import { enqueueOfflineTask, isNetworkError } from '../../src/utils/offlineQueue'

// 事件类型与 Web 端「创建事件」保持一致
const types = [
  { label: '市容环境', value: '市容环境' },
  { label: '消防安全', value: '消防安全' },
  { label: '矛盾纠纷', value: '矛盾纠纷' },
  { label: '安全生产', value: '安全生产' },
  { label: '民生诉求', value: '民生诉求' },
  { label: '防汛防台风', value: '防汛防台风' },
  { label: '违建', value: '违建' },
  { label: '其他', value: '其他' }
]
const typeNames = types.map(t => t.label)
const selectedType = ref('')
const selectedTypeName = ref('')
const title = ref('')
const content = ref('')
const location = ref('')
const longitude = ref<number | null>(null)
const latitude = ref<number | null>(null)
const locationHint = ref('')
const photos = ref<string[]>([])
const showCodeDialog = ref(false)
const eventCode = ref('')
const submitting = ref(false)

function onTypeChange(e: any) {
  const idx = e.detail.value
  selectedType.value = types[idx].value
  selectedTypeName.value = types[idx].label
}

function takePhoto() {
  uni.chooseImage({
    count: 3 - photos.value.length,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res: any) => { res.tempFilePaths.forEach((p: string) => photos.value.push(p)) },
    fail: () => { uni.showToast({ title: '请选择图片', icon: 'none' }) }
  })
}

function removePhoto(idx: number) { photos.value.splice(idx, 1) }

/** 媒体上传基址：小程序用绝对 HTTPS 域名，H5 用相对路径走代理 */
function resolveMediaBaseUrl(): string {
  // #ifdef MP-WEIXIN
  return 'https://drone.kfktec.cn:8443'
  // #endif
  // #ifndef MP-WEIXIN
  return ''
  // #endif
}

async function uploadPhoto(filePath: string): Promise<string | null> {
  const session = getH5Session()
  return await new Promise((resolve) => {
    uni.uploadFile({
      url: `${resolveMediaBaseUrl()}/api/media/upload`,
      filePath,
      name: 'file',
      formData: { businessType: 'EVENT' },
      header: { Authorization: `Bearer ${session?.token || ''}` },
      success: (res: any) => {
        try {
          const data = JSON.parse(res.data)
          resolve(data.success && data.data?.fileUrl ? data.data.fileUrl : null)
        } catch {
          resolve(null)
        }
      },
      fail: () => resolve(null)
    })
  })
}

/** 当前时间（本地时区，无时区后缀，与后端 LocalDateTime 解析兼容） */
function localNowIso(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 高德逆地理（仅 H5 平台，REST 接口无需安全密钥） */
async function reverseGeocode(lng: number, lat: number): Promise<string> {
  // #ifdef H5
  try {
    const res = await fetch(
      `https://restapi.amap.com/v3/geocode/regeo?location=${lng},${lat}&key=5e00e01d2d2b6ca9e1eed533a15572e4&extensions=base`
    )
    const data = await res.json()
    if (data?.status === '1' && data.regeocode?.formatted_address) {
      return data.regeocode.formatted_address as string
    }
  } catch { /* 逆地理失败不阻塞上报 */ }
  // #endif
  return ''
}

/** 高德地理编码：输入地址 → 经纬度（REST 接口，与逆地理同一 key） */
async function geocodeAddress(address: string): Promise<{ longitude: number; latitude: number } | null> {
  try {
    const res = await fetch(
      `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(address)}&key=5e00e01d2d2b6ca9e1eed533a15572e4`
    )
    const data = await res.json()
    const loc = data?.geocodes?.[0]?.location
    if (data?.status === '1' && loc) {
      const [lng, lat] = String(loc).split(',').map(Number)
      if (!isNaN(lng) && !isNaN(lat)) return { longitude: lng, latitude: lat }
    }
  } catch { /* 地理编码失败由调用方提示 */ }
  return null
}

/** 按输入地址搜索定位：地址 → 经纬度，并回填地点描述 */
async function searchAddress() {
  const addr = location.value.trim()
  if (!addr) {
    uni.showToast({ title: '请先输入地址', icon: 'none' })
    return
  }
  uni.showLoading({ title: '搜索定位中...' })
  try {
    const geo = await geocodeAddress(addr)
    if (geo) {
      longitude.value = Number(geo.longitude.toFixed(6))
      latitude.value = Number(geo.latitude.toFixed(6))
      locationHint.value = '已按输入地址定位，可在下方地图中微调'
    } else {
      uni.showToast({ title: '未找到该地址，请尝试更详细描述', icon: 'none' })
    }
  } finally {
    uni.hideLoading()
  }
}

/** 地图选点（小程序端）：微信原生地图选择位置，自动回填地址与经纬度 */
function chooseMapLocation() {
  uni.chooseLocation({
    success: (res: any) => {
      if (!res.latitude || !res.longitude) return
      longitude.value = Number(res.longitude.toFixed(6))
      latitude.value = Number(res.latitude.toFixed(6))
      if (res.address || res.name) location.value = res.address || res.name
      locationHint.value = '已在地图上选择位置'
    },
    fail: () => {
      uni.showToast({ title: '已取消选点', icon: 'none' })
    }
  })
}

/** 定位：三层定位工具获取坐标，H5 下逆地理自动填充地点描述 */
async function locateCurrent() {
  uni.showLoading({ title: '定位中...' })
  try {
    const res = await locateWithFallback()
    longitude.value = Number(res.longitude.toFixed(6))
    latitude.value = Number(res.latitude.toFixed(6))
    if (!location.value.trim()) {
      const address = await reverseGeocode(res.longitude, res.latitude)
      if (address) location.value = address
    }
    locationHint.value = res.precise
      ? `已定位（${res.sourceText}）`
      : `已定位（${res.sourceText}），请在地图上确认位置`
  } catch {
    uni.showToast({ title: '定位失败，请手动输入地点', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

// 地图选点后自动逆地理填充地点描述（未手动填写时）
watch([longitude, latitude], async ([lng, lat]) => {
  if (lng == null || lat == null || location.value.trim()) return
  const address = await reverseGeocode(lng, lat)
  if (address) location.value = address
})

async function handleSubmit() {
  if (submitting.value) return
  if (!selectedType.value) { uni.showToast({ title: '请选择事件类型', icon: 'none' }); return }
  if (!title.value) { uni.showToast({ title: '请输入标题', icon: 'none' }); return }
  if (!content.value) { uni.showToast({ title: '请填写详细描述', icon: 'none' }); return }
  if (!location.value.trim()) { uni.showToast({ title: '请填写事发地点', icon: 'none' }); return }

  // 先上传照片拿到可访问 URL，再随上报提交
  uni.showLoading({ title: '提交中...' })
  submitting.value = true
  const uploadedUrls: string[] = []
  for (const p of photos.value) {
    const url = await uploadPhoto(p)
    if (url) uploadedUrls.push(url)
  }
  uni.hideLoading()

  const payload: EventCreatePayload = {
    externalEventId: 'EVT-H5-' + Date.now(),
    sourceType: 'H5',
    sourceSystem: 'H5_APP',
    eventType: selectedType.value,
    title: title.value,
    description: content.value,
    occurredAt: localNowIso(),
    location: location.value.trim(),
    longitude: longitude.value,
    latitude: latitude.value,
    evidenceReferences: uploadedUrls
  }

  try {
    const event = await createEventForH5(payload)
    submitting.value = false
    eventCode.value = event.eventCode || ''
    showCodeDialog.value = true
  } catch (e: any) {
    submitting.value = false
    if (isNetworkError(e)) {
      // 网络信号差:离线保存,恢复网络后自动同步
      enqueueOfflineTask('EVENT', payload, `事件上报:${title.value}`)
      uni.showModal({
        title: '已离线保存',
        content: '当前网络不可用,上报内容已保存在本地,恢复网络后将自动上报。',
        showCancel: false,
        confirmText: '知道了'
      })
      resetForm()
    } else {
      uni.showToast({ title: e?.message || '提交失败', icon: 'none' })
    }
  }
}

function resetForm() {
  selectedType.value = ''
  selectedTypeName.value = ''
  title.value = ''
  content.value = ''
  location.value = ''
  locationHint.value = ''
  photos.value = []
}

function goHistory() {
  showCodeDialog.value = false
  navigateToPath('/pages/event/history')
}
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.form-card { background: #0e233a; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.form-item { margin-bottom: 16px; }
.label { font-size: 13px; color: #7ea4c8; display: block; margin-bottom: 8px; }
.picker-text, .input {
  background: #0a1a2e; border: 1px solid #1d3d5c; border-radius: 8px;
  padding: 10px 12px; font-size: 14px; color: #eaf5ff; width: 100%; box-sizing: border-box;
}
.picker-text { display: flex; align-items: center; min-height: 20px; }
.textarea {
  background: #0a1a2e; border: 1px solid #1d3d5c; border-radius: 8px;
  padding: 10px 12px; font-size: 14px; color: #eaf5ff; width: 100%; box-sizing: border-box;
  min-height: 100px;
}
.location-row { display: flex; gap: 8px; margin-bottom: 8px; }
.location-row .input { flex: 1; }
.locate-btn {
  display: flex; align-items: center; justify-content: center;
  padding: 0 16px; border-radius: 8px; background: #12539f; flex-shrink: 0;
}
.locate-btn-secondary { background: #0d3866; border: 1rpx solid #2a5f9e; }
.locate-btn-text { font-size: 14px; color: #eaf5ff; }
.map-picker-wrap { margin-top: 8px; }
.location-hint { display: block; font-size: 12px; color: #8ce56d; margin-top: 6px; }
.photo-grid { display: flex; gap: 10px; flex-wrap: wrap; }
.photo-item { width: 80px; height: 80px; position: relative; }
.photo-item image { width: 80px; height: 80px; border-radius: 8px; }
.photo-del {
  position: absolute; top: -6px; right: -6px; width: 20px; height: 20px;
  background: #ff4d4f; color: #fff; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 12px;
}
.photo-add {
  width: 80px; height: 80px; border: 1px dashed #2a5a8a; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #7ea4c8;
}
.photo-add-icon { font-size: 24px; }
.photo-add-text { font-size: 12px; }
.btn-submit {
  background: linear-gradient(135deg, #1890ff, #096dd9); border-radius: 12px;
  text-align: center; padding: 14px; color: #fff; font-size: 16px; font-weight: 600;
}
.mask {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.6);
  display: flex; align-items: center; justify-content: center; z-index: 999;
}
.dialog {
  width: 280px; background: #0e233a; border-radius: 16px; padding: 24px;
  text-align: center; border: 1px solid #1d3d5c;
}
.dialog-title { font-size: 18px; font-weight: bold; color: #eaf5ff; margin-bottom: 12px; }
.dialog-text { font-size: 13px; color: #7ea4c8; margin-bottom: 6px; }
.dialog-code { font-size: 20px; font-weight: bold; color: #57b9ff; margin-bottom: 12px; }
.dialog-btn {
  background: #1890ff; border-radius: 8px; padding: 10px; color: #fff;
  font-size: 14px; margin-top: 12px;
}
</style>

<style>
/* 小程序适配：scoped 样式对 input/textarea 原生组件不生效，用全局样式保证高度/行高/文字完整显示 */
.container .input {
  width: 100%;
  height: 88rpx;
  padding: 0 24rpx;
  border: 1px solid #1d3d5c;
  border-radius: 12rpx;
  font-size: 30rpx;
  line-height: 88rpx;
  color: #eaf5ff;
  background: #0a1a2e;
  box-sizing: border-box;
}
.container .textarea {
  width: 100%;
  min-height: 200rpx;
  padding: 20rpx 24rpx;
  border: 1px solid #1d3d5c;
  border-radius: 12rpx;
  font-size: 30rpx;
  line-height: 1.5;
  color: #eaf5ff;
  background: #0a1a2e;
  box-sizing: border-box;
}
.container .picker-text {
  display: flex;
  align-items: center;
  min-height: 88rpx;
  padding: 0 24rpx;
  border: 1px solid #1d3d5c;
  border-radius: 12rpx;
  font-size: 30rpx;
  color: #eaf5ff;
  background: #0a1a2e;
  box-sizing: border-box;
}
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
