<template>
  <!-- 页面模式：独立页面；embedded 模式：作为 el-dialog 内容体（标题/底部由父级提供） -->
  <!-- overflow-x:hidden 兜底 el-row gutter 负 margin (-16px) 与现场照片 72px × N 横向溢出 -->
  <div :style="(embedded ? 'padding:0;' : '') + 'overflow-x:hidden;'">
    <!-- 页面模式：标题与说明（弹窗模式由父级 el-dialog 标题提供） -->
    <div v-if="!embedded">
      <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">创建事件</h2>
      <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">上报新事件，启动闭环处置流程</p>
    </div>

    <div class="card" :style="embedded ? 'padding:0;box-shadow:none;max-width:none;' : 'max-width:700px;'">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="事件标题" prop="title" required>
          <el-input v-model="form.title" placeholder="简要描述事件" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="事件类型" prop="eventType" required>
              <el-select v-model="form.eventType" placeholder="请选择" style="width:100%;">
                <el-option v-for="opt in eventTypeOptions" :key="opt.itemValue" :value="opt.itemValue" :label="opt.itemLabel" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度">
              <el-select v-model="form.urgencyLevel" style="width:100%;">
                <el-option label="一般（绿）" value="GREEN" />
                <el-option label="重点（黄）" value="YELLOW" />
                <el-option label="紧急（红）" value="RED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发生时间" prop="occurredAt" required>
              <el-date-picker
                v-model="form.occurredAt"
                type="datetime"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm"
                placeholder="选择发生时间"
                style="width:100%;"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属网格">
              <el-select v-model="form.gridId" placeholder="自动关联" clearable style="width:100%;">
                <el-option v-for="g in grids" :key="g.id" :value="Number(g.id)" :label="g.gridName" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 事发地点 - 地图定位（内含地图复合控件，el-form-item 仅承载校验） -->
        <el-form-item label="事发地点" prop="location" required>
          <div style="display:flex;gap:8px;margin-bottom:8px;">
            <el-input v-model="form.location" placeholder="点击地图选择位置或手动输入地址" style="flex:1;" />
            <el-button @click="locateMe" type="default" plain style="white-space:nowrap;height:42px;">
              <i class="fas fa-crosshairs"></i> 定位
            </el-button>
          </div>
        </el-form-item>
        <div style="margin-top:0;margin-bottom:16px;">
          <div style="position:relative;">
            <div id="eventMap" style="height:250px;border-radius:8px;border:1px solid #e5e7eb;overflow:hidden;"></div>
            <button type="button" @click="backToCenter" title="回到社区中心点" style="position:absolute;top:8px;right:8px;padding:4px 10px;border:1px solid #1890ff;border-radius:6px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;box-shadow:0 1px 4px rgba(0,0,0,0.2);z-index:10;"><i class="fas fa-home"></i> 回到中心</button>
          </div>
          <p style="font-size:11px;color:#9ca3af;margin-top:4px;">点击地图标记位置，或拖动标记调整</p>
          <div v-if="form.longitude && form.latitude" style="font-size:11px;color:#52c41a;margin-top:4px;">
            已定位：{{ form.longitude.toFixed(6) }}, {{ form.latitude.toFixed(6) }}
          </div>
        </div>

        <el-form-item label="详细描述">
          <el-input v-model="form.description" type="textarea" :autosize="{ minRows: 4, maxRows: 8 }" placeholder="事件详细情况..." style="width:100%;" />
        </el-form-item>

        <!-- 现场照片：选填，最多 6 张，上传后随事件提交（复合控件，不走 el-form-item） -->
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;color:#374151;line-height:1.4;margin-bottom:6px;">
            现场照片 <span style="font-weight:400;font-size:12px;color:#9ca3af;">（选填，最多 6 张）</span>
          </label>
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

        <el-form-item label="上报来源" prop="reportSource" required>
          <el-select v-model="form.reportSource" placeholder="请选择" clearable style="width:100%;">
            <el-option v-for="opt in reportSourceOptions" :key="opt.itemValue" :value="opt.itemValue" :label="opt.itemLabel" />
          </el-select>
        </el-form-item>

        <!-- 发起人信息：电话必填，姓名选填 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="发起人姓名">
              <el-input v-model="form.reporterName" placeholder="请输入发起人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发起人电话" prop="reporterPhone" required>
              <el-input v-model="form.reporterPhone" maxlength="11" placeholder="请输入发起人联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 页面模式操作栏（弹窗模式的取消/创建按钮由父级 el-dialog 的 #footer 提供） -->
      <div v-if="!embedded" style="display:flex;gap:12px;justify-content:flex-end;position:sticky;bottom:0;background:#fff;padding-top:12px;margin-top:16px;border-top:1px solid #f3f4f6;">
        <el-button @click="handleCancel">取消</el-button>
        <el-button @click="submit" type="primary" :loading="loading">创建事件</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { createEvent, getGridTree, getDictItems, uploadEventImage } from '../api'
import http from '../api'
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

// 事件类型字典驱动（event_type），接口不可用时兜底内置列表
const FALLBACK_EVENT_TYPES = [
  { itemValue: '市容环境', itemLabel: '市容环境' },
  { itemValue: '消防安全', itemLabel: '消防安全' },
  { itemValue: '矛盾纠纷', itemLabel: '矛盾纠纷' },
  { itemValue: '安全生产', itemLabel: '安全生产' },
  { itemValue: '民生诉求', itemLabel: '民生诉求' },
  { itemValue: '防汛防台风', itemLabel: '防汛防台风' },
  { itemValue: '违建', itemLabel: '违建' },
  { itemValue: '其他', itemLabel: '其他' },
]
const eventTypeOptions = ref<{ itemValue: string; itemLabel: string }[]>(FALLBACK_EVENT_TYPES)

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
// 地图初始中心点：优先读系统配置，失败时用默认坐标（拔蛟窝社区）
let mapInitCenter: [number, number] = [113.939521, 22.971231]

const formRef = ref<FormInstance>()
const rules: FormRules = {
  title: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  occurredAt: [{ required: true, message: '请选择发生时间', trigger: 'change' }],
  location: [{ required: true, message: '请填写事发地点', trigger: 'blur' }],
  reportSource: [{ required: true, message: '请选择上报来源', trigger: 'change' }],
  reporterPhone: [
    { required: true, message: '请输入发起人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的11位手机号', trigger: 'blur' },
  ],
}

const form = ref({
  title: '',
  eventType: '',
  urgencyLevel: 'GREEN',
  occurredAt: new Date().toISOString().slice(0, 16),
  location: '',
  description: '',
  reportSource: '',
  reporterName: '',
  reporterPhone: '',
  gridId: null as number | null,
  longitude: null as number | null,
  latitude: null as number | null,
})

/** 将网格树（社区/大网格/小网格嵌套）展平为单层选项，标签带层级路径，供「所属网格」单选下拉使用 */
function flattenGridTree(nodes: any[], prefix = ''): any[] {
  const out: any[] = []
  for (const n of nodes || []) {
    const label = prefix ? `${prefix} / ${n.gridName}` : String(n.gridName || '')
    out.push({ id: n.id, gridName: label })
    if (Array.isArray(n.children) && n.children.length) {
      out.push(...flattenGridTree(n.children, label))
    }
  }
  return out
}

onMounted(async () => {
  try {
    const tree: any[] = (await getGridTree()) || []
    grids.value = flattenGridTree(tree)
  } catch (e) {}
  try {
    const items: any = await getDictItems('event_report_source', true)
    if (Array.isArray(items) && items.length) reportSourceOptions.value = items
  } catch (e) {}
  try {
    const items: any = await getDictItems('event_type', true)
    if (Array.isArray(items) && items.length) eventTypeOptions.value = items
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
  // 读取系统配置的地图中心点（与网格管理页一致），失败时保持默认坐标
  try {
    const [lng, lat] = await Promise.all([
      http.get('/system/config/map.center.lng'),
      http.get('/system/config/map.center.lat'),
    ])
    const nLng = Number(lng), nLat = Number(lat)
    if (!isNaN(nLng) && !isNaN(nLat)) mapInitCenter = [nLng, nLat]
  } catch (e) { /* 接口失败保持默认值 */ }
  mapInstance = new AMapLib.Map('eventMap', {
    zoom: 15,
    center: mapInitCenter,
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
    position: mapInitCenter,
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

// 快速回到地图中心点（系统配置的社区位置）
function backToCenter() {
  if (!mapInstance) return
  mapInstance.setZoomAndCenter(15, mapInitCenter)
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

function resetForm() {
  form.value = {
    title: '',
    eventType: '',
    urgencyLevel: 'GREEN',
    occurredAt: new Date().toISOString().slice(0, 16),
    location: '',
    description: '',
    reportSource: '',
    reporterName: '',
    reporterPhone: '',
    gridId: null as number | null,
    longitude: null as number | null,
    latitude: null as number | null,
  }
  images.value = []
  formRef.value?.clearValidate()
  if (markerInstance && mapInstance) {
    markerInstance.setPosition(mapInitCenter)
    mapInstance.setCenter(mapInitCenter)
    mapInstance.setZoom(15)
  }
}

function handleCancel() {
  resetForm()
  if (props.embedded) {
    emit('cancel')
  } else {
    router.back()
  }
}

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    // 表单里的「上报来源」对应后端的 source_system：
    // 事件列表「来源」列与来源筛选取的都该字段，字典 event_report_source 的码值即其值域。
    // 此前 sourceSystem 被写死为 GRID_PLATFORM，导致用户选择的来源被丢弃、列表恒显示「平台录入」。
    const { reportSource, ...baseFields } = form.value
    const result = await createEvent({
      ...baseFields,
      sourceType: 'MANUAL',
      sourceSystem: reportSource || 'GRID_PLATFORM',
      externalEventId: 'EVT-' + Date.now(),
      evidenceReferences: images.value,
    })
    resetForm()
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

// 暴露 submit 给父级 el-dialog 的 #footer 按钮调用（loading 由父级管理）
defineExpose({ submit })
</script>

<style scoped>
.form-hint { color: #9ca3af; font-weight: 400; font-size: 12px; margin-bottom: 4px; }
</style>