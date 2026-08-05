<template>
  <div class="page">
    <div class="header">
      <h2>📸 随手拍</h2>
      <p>发现身边问题，一键上报</p>
    </div>

    <div class="card">
      <h3>问题类型</h3>
      <div class="type-grid">
        <div v-for="t in types" :key="t.value" class="type-item"
             :class="{ active: form.type === t.value }" @click="form.type = t.value">
          <span class="type-icon">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <h3>问题描述</h3>
      <p class="selected-type">已选择：{{ selectedType.label }}</p>
      <textarea v-model="form.description" placeholder="请详细描述您发现的问题..." class="textarea" rows="3" />
    </div>

    <div class="card">
      <h3>现场照片<span v-if="uploading" class="uploading">（上传中...）</span></h3>
      <div class="photo-list">
        <div v-for="(p, idx) in photos" :key="idx" class="photo-item">
          <img v-if="p.url" :src="p.url" class="photo-img" />
          <div v-else class="photo-placeholder">📷 {{ idx + 1 }}</div>
          <span class="photo-del" @click="photos.splice(idx, 1)">×</span>
        </div>
        <div v-if="photos.length < 3" class="photo-add" @click="addPhoto">
          <span>+</span>
          <p>拍照/选图</p>
        </div>
      </div>
      <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif" style="display:none" @change="onFilesSelected" />
    </div>

    <div class="card">
      <h3>位置信息</h3>
      <p class="location">📍 {{ locationText }} <span class="relocate" @click="locate">重新定位</span></p>
    </div>

    <div class="card">
      <h3>联系方式（选填）</h3>
      <input v-model="form.contactName" placeholder="您的姓名" class="input" />
      <input v-model="form.contactPhone" placeholder="联系电话" class="input" style="margin-top:8px;" />
    </div>

    <button @click="handleSubmit" :disabled="loading" class="btn-submit">
      {{ loading ? '提交中...' : '提交上报' }}
    </button>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="success" class="success">{{ success }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { reportEvent, uploadMedia } from '../api'

const loading = ref(false)
const error = ref('')
const success = ref('')
const uploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
// 每张照片：{ file?: 待上传文件, url: 本地预览或已上传 URL, uploadedUrl?: 服务端 URL }
const photos = ref<{ file?: File; url: string; uploadedUrl?: string }[]>([])

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
  fileInput.value?.click()
}

async function onFilesSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  const remain = 3 - photos.value.length
  for (const file of files.slice(0, remain)) {
    const item = { file, url: URL.createObjectURL(file) } as { file?: File; url: string; uploadedUrl?: string }
    photos.value.push(item)
    uploading.value = true
    try {
      const res: any = await uploadMedia(file)
      item.uploadedUrl = res?.fileUrl
    } catch (err: any) {
      const idx = photos.value.indexOf(item)
      if (idx >= 0) photos.value.splice(idx, 1)
      error.value = typeof err === 'string' ? err : '照片上传失败'
    } finally {
      uploading.value = false
    }
  }
}

/** 定位：优先浏览器精确定位（需 HTTPS），失败时用高德 IP 定位兑底（城市级） */
function locate() {
  locationText.value = '定位中...'
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
}

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

onMounted(() => {
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
    error.value = e || '上报失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 12px;
  padding: 20px;
  color: #fff;
  margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card h3 { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #374151; }
.selected-type { margin-bottom: 8px; font-size: 13px; color: #1890ff; }
.type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.type-item {
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  padding: 10px 4px; border: 1px solid #e5e7eb; border-radius: 8px; cursor: pointer; font-size: 11px;
}
.type-item.active { border-color: #1890ff; background: #e6f4ff; }
.type-icon { font-size: 20px; }
.input, .textarea {
  width: 100%; padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; outline: none;
}
.input:focus, .textarea:focus { border-color: #1890ff; }
.textarea { resize: none; }
.photo-list { display: flex; gap: 8px; flex-wrap: wrap; }
.photo-item { position: relative; width: 80px; height: 80px; }
.photo-placeholder {
  width: 80px; height: 80px; background: #f3f4f6; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; font-size: 24px;
}
.photo-del {
  position: absolute; top: -6px; right: -6px; width: 20px; height: 20px;
  background: #ff4d4f; color: #fff; border-radius: 50%; display: flex;
  align-items: center; justify-content: center; font-size: 12px; cursor: pointer;
}
.photo-add {
  width: 80px; height: 80px; border: 1px dashed #d1d5db; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: #9ca3af; font-size: 12px;
}
.photo-add span { font-size: 24px; }
.location { font-size: 13px; color: #6b7280; padding: 8px; background: #f9fafb; border-radius: 6px; }
.relocate { color: #1890ff; margin-left: 8px; cursor: pointer; }
.photo-img { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; display: block; }
.uploading { font-size: 12px; color: #1890ff; font-weight: normal; }
.btn-submit {
  width: 100%; padding: 14px; background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  color: #fff; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px;
}
.btn-submit[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 13px; text-align: center; margin-top: 12px; }
.success { color: #52c41a; font-size: 13px; text-align: center; margin-top: 12px; }
</style>
