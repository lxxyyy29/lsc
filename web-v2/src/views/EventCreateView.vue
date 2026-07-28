<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">创建事件</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">上报新事件，启动闭环处置流程</p>

    <div class="card" style="max-width:700px;">
      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">事件标题 <span style="color:#ff4d4f;">*</span></label>
        <input v-model="form.title" placeholder="简要描述事件" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
        <div>
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">事件类型 <span style="color:#ff4d4f;">*</span></label>
          <select v-model="form.eventType" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
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
          <input v-model="form.occurredAt" type="datetime-local" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
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
          <input v-model="form.location" placeholder="点击地图选择位置或手动输入地址" style="flex:1;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
          <button @click="locateMe" type="button" style="padding:8px 12px;border:1px solid #1890ff;border-radius:6px;background:#fff;color:#1890ff;font-size:13px;cursor:pointer;white-space:nowrap;">
            <i class="fas fa-crosshairs"></i> 定位
          </button>
        </div>
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

      <div style="margin-bottom:16px;">
        <label style="display:block;font-size:13px;font-weight:600;margin-bottom:6px;">上报来源</label>
        <select v-model="form.reportSource" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option value="GRID_MEMBER">网格员上报</option>
          <option value="RESIDENT">居民上报</option>
          <option value="12345">12345转办</option>
          <option value="PROPERTY">物业上报</option>
          <option value="AI_CAMERA">AI监控抓拍</option>
        </select>
      </div>

      <div style="display:flex;gap:12px;justify-content:flex-end;">
        <button @click="$router.back()" style="padding:8px 20px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
        <button @click="submit" :disabled="loading" style="padding:8px 20px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
          {{ loading ? '提交中...' : '创建事件' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { createEvent, getGridTree } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'

const router = useRouter()
const loading = ref(false)
const grids = ref<any[]>([])
let mapInstance: any = null
let markerInstance: any = null

const form = ref({
  title: '',
  eventType: '',
  urgencyLevel: 'GREEN',
  occurredAt: new Date().toISOString().slice(0, 16),
  location: '',
  description: '',
  reportSource: 'GRID_MEMBER',
  gridId: null as number | null,
  longitude: null as number | null,
  latitude: null as number | null,
})

onMounted(async () => {
  try {
    grids.value = await getGridTree() || []
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
  const AMap = await AMapLoader.load({
    key: '5e00e01d2d2b6ca9e1eed533a15572e4',
    version: '2.0',
    plugins: ['AMap.Marker', 'AMap.Geocoder', 'AMap.Geolocation']
  })
  mapInstance = new AMap.Map('eventMap', {
    zoom: 15,
    center: [113.939521, 22.971231],
    mapStyle: 'amap://styles/normal'
  })

  // 点击地图事件
  mapInstance.on('click', (e: any) => {
    const lng = e.lnglat.getLng()
    const lat = e.lnglat.getLat()
    setMarker(lng, lat)
    reverseGeocode(lng, lat)
  })

  // 初始标记
  markerInstance = new AMap.Marker({
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
    markerInstance = new AMap.Marker({
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
      alert('定位失败，请手动在地图上选择位置')
    }
  })
}

async function submit() {
  if (!form.value.title || !form.value.eventType || !form.value.occurredAt || !form.value.location) {
    alert('请填写必填字段')
    return
  }
  loading.value = true
  try {
    const result = await createEvent({
      ...form.value,
      sourceType: 'MANUAL',
      sourceSystem: 'GRID_PLATFORM',
      externalEventId: 'EVT-' + Date.now(),
      evidenceReferences: [],
    })
    router.push(`/events/${result.id}`)
  } catch (e: any) {
    alert(e?.message || '创建失败')
  } finally {
    loading.value = false
  }
}
</script>
