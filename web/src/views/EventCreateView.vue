<template>
  <!-- embedded 弹窗模式：header / 表单滚动区 / footer 三段结构，按钮固定 footer 右侧 -->
  <div :style="embedded ? 'display:flex;flex-direction:column;height:100%;min-height:0;' : ''">
    <div style="flex-shrink:0;">
      <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">创建事件</h2>
      <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">上报新事件，启动闭环处置流程</p>
    </div>

    <div :style="embedded ? 'flex:1;overflow-y:auto;min-height:0;padding-right:4px;' : ''">
    <div class="card" :style="embedded ? 'padding:0;box-shadow:none;margin-bottom:0;' : 'max-width:700px;'">
      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">事件标题 <span style="color:#ff4d4f;">*</span></label>
        <input v-model="form.title" @input="errors.title = ''" placeholder="简要描述事件" :style="`width:100%;padding:8px 12px;border:1px solid ${errors.title ? '#ff4d4f' : '#d1d5db'};border-radius:6px;font-size:13px;line-height:normal;box-sizing:border-box;`" />
        <p v-if="errors.title" class="field-error">{{ errors.title }}</p>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
        <div>
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">事件类型 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="form.eventType" @change="errors.eventType = ''" :style="`width:100%;padding:8px 12px;border:1px solid ${errors.eventType ? '#ff4d4f' : '#d1d5db'};border-radius:6px;font-size:13px;`">
            <option value="">请选择</option>
            <option value="市容环境">市容环境</option>
            <option value="消防安全">消防安全</option>
            <option value="矛盾纠纷">矛盾纠纷</option>
            <option value="安全生产">安全生产</option>
            <option value="民生诉求">民生诉求</option>
            <option value="防汛防台风">防汛防台风</option>
            <option value="违建">违建</option>
            <option value="其他">其他</option>
          </select>
          <p v-if="errors.eventType" class="field-error">{{ errors.eventType }}</p>
        </div>
        <div>
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">紧急程度</label>
          <select v-model="form.urgencyLevel" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option value="GREEN">一般（绿）</option>
            <option value="YELLOW">重点（黄）</option>
            <option value="RED">紧急（红）</option>
          </select>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
        <div>
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">发生时间 <span style="color:#ff4d4f;">*</span></label>
          <input v-model="form.occurredAt" @input="errors.occurredAt = ''" type="datetime-local" :style="`width:100%;padding:8px 12px;border:1px solid ${errors.occurredAt ? '#ff4d4f' : '#d1d5db'};border-radius:6px;font-size:13px;`" />
          <p v-if="errors.occurredAt" class="field-error">{{ errors.occurredAt }}</p>
        </div>
        <div>
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">所属网格</label>
          <select v-model="form.gridId" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option :value="null">自动关联</option>
            <option v-for="g in grids" :key="g.id" :value="g.id">{{ g.gridName }}</option>
          </select>
        </div>
      </div>

      <!-- 事发地点 - 地图定位 -->
      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">事发地点 <span style="color:#ff4d4f;">*</span></label>
        <div style="display:flex;gap:8px;margin-bottom:8px;">
          <input v-model="form.location" @input="errors.location = ''" placeholder="点击地图选择位置或手动输入地址" :style="`flex:1;padding:8px 12px;border:1px solid ${errors.location ? '#ff4d4f' : '#d1d5db'};border-radius:6px;font-size:13px;line-height:normal;box-sizing:border-box;`" />
          <button @click="locateMe" type="button" style="padding:8px 12px;border:1px solid #1890ff;border-radius:6px;background:#fff;color:#1890ff;font-size:13px;cursor:pointer;white-space:nowrap;">
            <i class="fas fa-crosshairs"></i> 定位
          </button>
        </div>
        <p v-if="errors.location" class="field-error">{{ errors.location }}</p>
        <div id="eventMap" style="height:250px;border-radius:8px;border:1px solid #e5e7eb;overflow:hidden;"></div>
        <p style="font-size:11px;color:#9ca3af;margin-top:4px;">点击地图标记位置，或拖动标记调整</p>
        <div v-if="form.longitude && form.latitude" style="font-size:11px;color:#52c41a;margin-top:4px;">
          已定位：{{ form.longitude.toFixed(6) }}, {{ form.latitude.toFixed(6) }}
        </div>
      </div>

      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">详细描述</label>
        <textarea v-model="form.description" rows="4" placeholder="事件详细情况..." style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;"></textarea>
      </div>

      <!-- 现场照片：选填，最多 6 张，上传后随事件提交 -->
      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">现场照片 <span style="font-weight:400;font-size:12px;color:#9ca3af;">（选填，最多 6 张）</span></label>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <div v-for="(img, idx) in images" :key="idx" style="position:relative;width:72px;height:72px;">
            <img :src="img" style="width:100%;height:100%;object-fit:cover;border-radius:6px;border:1px solid #e5e7eb;" />
            <button @click="removeImage(idx)" style="position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;border:none;background:#ff4d4f;color:#fff;font-size:12px;line-height:1;cursor:pointer;" title="移除">×</button>
          </div>
          <label v-if="images.length < 6" style="width:72px;height:72px;border:1px dashed #d1d5db;border-radius:6px;display:flex;flex-direction:column;align-items:center;justify-content:center;cursor:pointer;color:#9ca3af;font-size:12px;gap:2px;">
            <span style="font-size:20px;line-height:1;">+</span>
            <span>{{ uploading ? '上传中...' : '上传' }}</span>
            <input type="file" accept="image/jpeg,image/png,image/webp,image/gif,image/bmp" multiple :disabled="uploading" style="display:none;" @change="onPickImages" />
          </label>
        </div>
      </div>

      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">上报来源 <span style="color:#ff4d4f;">*</span></label>
        <select v-model="form.reportSource" :style="`width:100%;padding:8px 12px;border:1px solid ${errors.reportSource ? '#ff4d4f' : '#d1d5db'};border-radius:6px;font-size:13px;`">
          <option value="">请选择</option>
          <option v-for="opt in reportSourceOptions" :key="opt.itemValue" :value="opt.itemValue">{{ opt.itemLabel }}</option>
        </select>
        <p v-if="errors.reportSource" class="field-error">{{ errors.reportSource }}</p>
      </div>

      <!-- 页面模式操作栏吸底：弹窗内滚动时始终保持在可视区右下角 -->
      <div v-if="!embedded" style="display:flex;gap:12px;justify-content:flex-end;position:sticky;bottom:0;background:#fff;padding-top:12px;margin-top:16px;border-top:1px solid #f3f4f6;">
        <button @click="handleCancel" style="padding:8px 20px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
        <button @click="submit" :disabled="loading" style="padding:8px 20px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
          {{ loading ? '提交中...' : '创建事件' }}
        </button>
      </div>
    </div>
    </div>

    <!-- footer：弹窗模式下按钮固定在底部右侧 -->
    <div v-if="embedded" style="flex-shrink:0;display:flex;gap:12px;justify-content:flex-end;padding-top:16px;margin-top:16px;border-top:1px solid #f3f4f6;">
      <button @click="handleCancel" style="padding:8px 20px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
      <button @click="submit" :disabled="loading" style="padding:8px 20px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
        {{ loading ? '提交中...' : '创建事件' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { createEvent, getGridTree, getDictItems, uploadEventImage } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import { locateWithFallback } from '../utils/geolocation'
import { showMessage } from '../utils/message'

const router = useRouter()
// embedded=true 时作为弹窗内嵌组件使用（取消/成功后 emit 给父组件，不跳路由）
const props = withDefaults(defineProps<{ embedded?: boolean }>(), { embedded: false })
const emit = defineEmits<{ (e: 'cancel'): void; (e: 'created', id: number): void }>()
const loading = ref(false)
const grids = ref<any[]>([])

// 上报来源字典驱动（event_report_source），接口不可用时兜底内置列表
const FALLBACK_REPORT_SOURCES = [
  { itemValue: 'GRID_MEMBER', itemLabel: '网格员上报' },
  { itemValue: 'RESIDENT', itemLabel: '居民上报' },
  { itemValue: '12345', itemLabel: '12345转办' },
  { itemValue: 'PROPERTY', itemLabel: '物业上报' },
  { itemValue: 'AI_CAMERA', itemLabel: '智能监控抓拍' },
]
const reportSourceOptions = ref<{ itemValue: string; itemLabel: string }[]>(FALLBACK_REPORT_SOURCES)

// 现场照片（选填）：选中即上传，成功后存 URL 随事件提交
const images = ref<string[]>([])
const uploading = ref(false)

async function onPickImages(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return
  const remain = 6 - images.value.length
  if (files.length > remain) showMessage(`最多上传 6 张，本次仅上传前 ${remain} 张`, 'warning')
  uploading.value = true
  try {
    for (const f of files.slice(0, remain)) {
      if (f.size > 10 * 1024 * 1024) {
        showMessage(`图片「${f.name}」超过 10MB，已跳过`, 'warning')
        continue
      }
      const res: any = await uploadEventImage(f)
      if (res?.fileUrl) images.value.push(res.fileUrl)
    }
  } catch (err: any) {
    showMessage(err?.message || '图片上传失败')
  } finally {
    uploading.value = false
  }
}

function removeImage(idx: number) {
  images.value.splice(idx, 1)
}
let mapInstance: any = null
let markerInstance: any = null
let AMapLib: any = null

// 必填项校验错误提示：提交时逐项标红并在字段下方展示 tips，输入后自动清除
const errors = reactive({ title: '', eventType: '', occurredAt: '', location: '', reportSource: '' })
function validateRequired(): boolean {
  errors.title = form.value.title.trim() ? '' : '请输入事件标题'
  errors.eventType = form.value.eventType ? '' : '请选择事件类型'
  errors.occurredAt = form.value.occurredAt ? '' : '请选择发生时间'
  errors.location = form.value.location.trim() ? '' : '请填写事发地点'
  errors.reportSource = form.value.reportSource ? '' : '请选择上报来源'
  const firstMsg = errors.title || errors.eventType || errors.occurredAt || errors.location || errors.reportSource
  if (firstMsg) { showMessage(firstMsg, 'warning'); return false }
  return true
}

const form = ref({
  title: '',
  eventType: '',
  urgencyLevel: 'GREEN',
  occurredAt: new Date().toISOString().slice(0, 16),
  location: '',
  description: '',
  reportSource: '',
  gridId: null as number | null,
  longitude: null as number | null,
  latitude: null as number | null,
})

onMounted(async () => {
  try {
    grids.value = await getGridTree() || []
  } catch (e) {}
  try {
    const items: any = await getDictItems('event_report_source', true)
    if (Array.isArray(items) && items.length) reportSourceOptions.value = items
  } catch (e) {}
  await initMap()
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})

async function initMap() {
  ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
  AMapLib = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Marker', 'AMap.Geocoder', 'AMap.Geolocation', 'AMap.CitySearch']
  })
  mapInstance = new AMapLib.Map('eventMap', {
    zoom: 15,
    center: [113.939521, 22.971231],
    mapStyle: 'amap://styles/normal',
    // 小尺寸选点地图用 2D 渲染，降低开销；弹窗内嵌入时容器尺寸由父级确定，延迟 resize 保证铺满
    viewMode: '2D',
    showIndoorMap: false
  })
  setTimeout(() => mapInstance?.resize(), 100)

  // 点击地图事件
  mapInstance.on('click', (e: any) => {
    const lng = e.lnglat.getLng()
    const lat = e.lnglat.getLat()
    setMarker(lng, lat)
    reverseGeocode(lng, lat)
  })

  // 初始标记
  markerInstance = new AMapLib.Marker({
    position: [113.939521, 22.971231],
    draggable: true,
    map: mapInstance
  })

  // 拖动标记结束事件
  markerInstance.on('dragend', (e: any) => {
    const lng = e.lnglat.getLng()
    const lat = e.lnglat.getLat()
    form.value.longitude = lng
    form.value.latitude = lat
    reverseGeocode(lng, lat)
  })
}

function setMarker(lng: number, lat: number) {
  form.value.longitude = lng
  form.value.latitude = lat
  if (markerInstance) {
    markerInstance.setPosition([lng, lat])
  } else {
    markerInstance = new AMapLib.Marker({
      position: [lng, lat],
      draggable: true,
      map: mapInstance
    })
    markerInstance.on('dragend', (e: any) => {
      const l = e.lnglat.getLng()
      const t = e.lnglat.getLat()
      form.value.longitude = l
      form.value.latitude = t
      reverseGeocode(l, t)
    })
  }
}

function reverseGeocode(lng: number, lat: number) {
  const geocoder = new (window as any).AMap.Geocoder({ city: '东莞' })
  geocoder.getAddress([lng, lat], (status: string, result: any) => {
    if (status === 'complete' && result.regeocode) {
      form.value.location = result.regeocode.formattedAddress
    }
  })
}

function locateMe() {
  // 三层定位策略：浏览器原生定位(HTTPS) → uni/其他 → 高德 IP 定位（与 H5 端共用工具）
  locateWithFallback().then((res) => {
    setMarker(res.longitude, res.latitude)
    mapInstance?.setCenter([res.longitude, res.latitude])
    if (!res.precise) mapInstance?.setZoom(12)
    reverseGeocode(res.longitude, res.latitude)
    if (!res.precise) {
      showMessage(`精确定位不可用，已定位到 ${res.sourceText}，可拖动地图标记修正`)
    }
  }).catch(() => {
    // 兼容旧逻辑：工具全部失败时再试一次 AMap.Geolocation 插件
    locateByAmapPlugin()
  })
}

function locateByAmapPlugin() {
  try {
    const geolocation = new (window as any).AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      zoomToAccuracy: true
    })
    geolocation.getCurrentPosition((status: string, result: any) => {
      if (status === 'complete') {
        const lng = result.position.lng
        const lat = result.position.lat
        setMarker(lng, lat)
        mapInstance.setCenter([lng, lat])
        reverseGeocode(lng, lat)
      } else {
        locateByIp()
      }
    })
  } catch {
    showMessage('定位失败，请手动在地图上选择位置')
  }
}

function locateByIp() {
  const citySearch = new (window as any).AMap.CitySearch()
  // AMap 2.0 的 CitySearch 实例方法是 getLocalCity（1.x 的 getLocalPosition 已不存在）
  citySearch.getLocalCity((status: string, result: any) => {
    if (status === 'complete' && result?.infocode === '10000' && result.rectangle) {
      const [sw, ne] = String(result.rectangle).split(';')
      const [lng1, lat1] = sw.split(',').map(Number)
      const [lng2, lat2] = ne.split(',').map(Number)
      const lng = (lng1 + lng2) / 2
      const lat = (lat1 + lat2) / 2
      setMarker(lng, lat)
      mapInstance.setCenter([lng, lat])
      mapInstance.setZoom(12)
      reverseGeocode(lng, lat)
      showMessage(`精确定位不可用，已定位到 ${result.city || '当前城市'} 大致位置，可拖动地图标记修正`)
    } else {
      showMessage('定位失败，请手动在地图上选择位置')
    }
  })
}

function handleCancel() {
  if (props.embedded) {
    emit('cancel')
  } else {
    router.back()
  }
}

async function submit() {
  if (!validateRequired()) return
  loading.value = true
  try {
    const result = await createEvent({
      ...form.value,
      sourceType: 'MANUAL',
      sourceSystem: 'GRID_PLATFORM',
      externalEventId: 'EVT-' + Date.now(),
      evidenceReferences: images.value,
    })
    if (props.embedded) {
      emit('created', result.id)
    } else {
      router.push(`/events/${result.id}`)
    }
  } catch (e: any) {
    showMessage(e?.message || '创建失败')
  } finally {
    loading.value = false
  }
}
</script>
