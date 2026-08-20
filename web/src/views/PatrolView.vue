<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">网格巡查任务</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">常态化巡查清单：人居环境、消防、出租屋、矛盾排查等</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">总任务数</p>
            <p class="stat-value">{{ stats.total || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#e6f4ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-clipboard-list" style="color:#1890FF;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">待巡查</p>
            <p class="stat-value">{{ stats.pending || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff7e6;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-clock" style="color:#FAAD14;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-green">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">已完成</p>
            <p class="stat-value">{{ stats.completed || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#f6ffed;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-check-circle" style="color:#52C41A;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">已超期</p>
            <p class="stat-value">{{ stats.overdue || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff1f0;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-exclamation-triangle" style="color:#FF4D4F;font-size:18px;"></i></div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="filter-bar">
      <button @click="generateTasks" :disabled="loading" class="filter-action">
        <i class="fas fa-sync"></i>生成本周任务
      </button>
      <button @click="markOverdue" :disabled="loading" class="filter-action ghost">
        <i class="fas fa-exclamation"></i>标记超期
      </button>
      <button @click="remindUpcoming" :disabled="loading" class="filter-action warn">
        <i class="fas fa-bell"></i>到期未巡提醒
      </button>
      <select v-model="filterStatus" class="filter-select" @change="loadTasks">
        <option value="">全部状态</option>
        <option value="PENDING">待巡查</option>
        <option value="COMPLETED">已完成</option>
        <option value="OVERDUE">已超期</option>
      </select>
    </div>

    <!-- 任务列表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="loadTasks" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>计划日期</th>
              <th>状态</th>
              <th>完成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in filteredTasks" :key="t.id">
              <td>{{ t.taskName || '-' }}</td>
              <td>{{ t.plannedDate || '-' }}</td>
              <td>
                <span :class="['tag', t.status === 'COMPLETED' ? 'tag-green' : t.status === 'OVERDUE' ? 'tag-red' : 'tag-orange']">
                  {{ statusLabel(t.status) }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ t.completedAt || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredTasks.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无巡查任务</p>
      </template>
    </div>

    <!-- 巡查轨迹地图：比表格直观，选人+时间段直接看巡查路线 -->
    <div class="card" style="margin-top:20px;">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;flex-wrap:wrap;gap:8px;">
        <h3 style="font-size:14px;font-weight:600;margin:0;">
          <i class="fas fa-route" style="color:#1890ff;margin-right:6px;"></i>巡查轨迹地图
        </h3>
        <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
          <select v-model="trackUser" class="filter-select" style="width:140px;">
            <option value="">全部网格员</option>
            <option v-for="u in userList" :key="u.id" :value="u.id">{{ u.name }}</option>
          </select>
          <div style="display:flex;gap:4px;">
            <button v-for="opt in rangeOptions" :key="opt.value" @click="trackRange = opt.value"
              style="height:32px;padding:0 12px;font-size:12px;border-radius:16px;border:1px solid #d1d5db;cursor:pointer;"
              :style="trackRange === opt.value ? 'background:#0284c7;color:#fff;border-color:#0284c7;' : 'background:#fff;color:#6b7280;'">
              {{ opt.label }}
            </button>
          </div>
        </div>
      </div>
      <div style="position:relative;">
        <div id="patrolTrackMap" style="height:380px;border-radius:10px;"></div>
        <div v-if="hoverInfo.visible" :style="{
          position:'absolute', left:hoverInfo.x+'px', top:hoverInfo.y+'px', transform:'translate(-50%, calc(-100% - 12px))',
          background:'rgba(17,24,39,0.92)', color:'#fff', padding:'8px 12px', borderRadius:'8px',
          fontSize:'12px', pointerEvents:'none', zIndex:50, maxWidth:'260px', boxShadow:'0 4px 16px rgba(0,0,0,0.3)'
        }">
          <div style="font-weight:600;margin-bottom:2px;">{{ hoverInfo.title }}</div>
          <div style="color:#cbd5e1;line-height:1.6;">{{ hoverInfo.content }}</div>
          <div style="color:#93c5fd;margin-top:2px;">{{ hoverInfo.time }}</div>
        </div>
        <p v-if="!trackRecords.length" style="position:absolute;top:12px;left:50%;transform:translateX(-50%);background:rgba(255,255,255,0.9);padding:4px 12px;border-radius:12px;font-size:12px;color:#9ca3af;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
          当前筛选条件下暂无带定位的打卡记录
        </p>
      </div>
      <p style="font-size:12px;color:#9ca3af;margin:8px 0 0;">绿点为起点、红点为终点；悬停圆点可查看该次打卡的内容与时间，点击下表记录可在地图上定位</p>
    </div>

    <!-- 巡查记录 -->
    <div class="card" style="margin-top:20px;">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">巡查记录</h3>
      <table class="table">
        <thead><tr><th>网格</th><th>巡查员</th><th>类型</th><th>内容</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="r in filteredRecords" :key="r.id" @click="locateRecord(r)"
            style="cursor:pointer;" title="点击在轨迹地图上定位">
            <td>{{ r.gridName || '-' }}</td>
            <td>{{ r.userName || '-' }}</td>
            <td><span class="tag tag-blue">{{ r.patrolType || '日常' }}</span></td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ r.content || '-' }}</td>
            <td style="font-size:12px;color:#6b7280;">{{ r.createdAt || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!filteredRecords.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无巡查记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { getAllPatrolTasks, getPatrolTaskStatistics, generatePatrolTasks, markOverduePatrolTasks, getPatrolRecords, remindUpcomingPatrolTasks } from '../api'
import AMapLoader from '@amap/amap-jsapi-loader'
import { showMessage } from '../utils/message'

const tasks = ref<any[]>([])
const records = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(true)
const error = ref('')
const filterStatus = ref('')

// ============ 巡查轨迹地图 ============
const trackUser = ref('')
const trackRange = ref<'today' | '7d' | '30d' | 'all'>('7d')
const rangeOptions = [
  { value: 'today' as const, label: '今天' },
  { value: '7d' as const, label: '近7天' },
  { value: '30d' as const, label: '近30天' },
  { value: 'all' as const, label: '全部' }
]
const hoverInfo = reactive({ visible: false, x: 0, y: 0, title: '', content: '', time: '' })
let AMapLib: any = null
let trackMap: any = null
let trackOverlays: any[] = []

const filteredTasks = computed(() => {
  if (!filterStatus.value) return tasks.value
  return tasks.value.filter(t => t.status === filterStatus.value)
})

// 有坐标的打卡记录（轨迹数据源）
const geoRecords = computed(() => records.value.filter(r => r.longitude && r.latitude))

// 网格员下拉（从打卡记录中提取去重）
const userList = computed(() => {
  const map = new Map<number, string>()
  for (const r of geoRecords.value) {
    if (r.userId && !map.has(r.userId)) map.set(r.userId, r.userName || `用户${r.userId}`)
  }
  return Array.from(map.entries()).map(([id, name]) => ({ id, name }))
})

// 按人 + 时间范围筛选并按时间正序（连线顺序）
const trackRecords = computed(() => {
  let list = geoRecords.value
  if (trackUser.value) list = list.filter(r => r.userId === Number(trackUser.value))
  if (trackRange.value !== 'all') {
    const days = trackRange.value === 'today' ? 0 : trackRange.value === '7d' ? 6 : 29
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    start.setDate(start.getDate() - days)
    list = list.filter(r => new Date(r.createdAt).getTime() >= start.getTime())
  }
  return [...list].sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
})

// 表格与地图同步筛选，避免表格列出一大堆与地图对不上
const filteredRecords = computed(() => {
  let list = records.value
  if (trackUser.value) list = list.filter(r => r.userId === Number(trackUser.value))
  if (trackRange.value !== 'all') {
    const days = trackRange.value === 'today' ? 0 : trackRange.value === '7d' ? 6 : 29
    const start = new Date()
    start.setHours(0, 0, 0, 0)
    start.setDate(start.getDate() - days)
    list = list.filter(r => new Date(r.createdAt).getTime() >= start.getTime())
  }
  return list
})

function clearTrackOverlays() {
  for (const o of trackOverlays) o.setMap(null)
  trackOverlays = []
}

/** 绘制当前筛选结果的轨迹：连线 + 逐打卡点圆点（悬停看内容） */
function drawTrack() {
  if (!AMapLib || !trackMap) return
  clearTrackOverlays()
  hoverInfo.visible = false
  const list = trackRecords.value
  if (!list.length) return

  const path = list.map(r => [Number(r.longitude), Number(r.latitude)])
  // 多点才连线（同一天多次打卡在同一点时只有圆点）
  if (path.length >= 2) {
    trackOverlays.push(new AMapLib.Polyline({
      path, strokeColor: '#1890ff', strokeWeight: 3, strokeOpacity: 0.8,
      strokeStyle: 'solid', zIndex: 10, map: trackMap
    }))
  }
  list.forEach((r, idx) => {
    const isStart = idx === 0
    const isEnd = idx === list.length - 1
    const strokeColor = isStart ? '#52c41a' : isEnd && !isStart ? '#ff4d4f' : '#1890ff'
    const circle = new AMapLib.CircleMarker({
      center: [Number(r.longitude), Number(r.latitude)],
      radius: isStart || isEnd ? 6 : 5,
      fillColor: '#ffffff', fillOpacity: 1,
      strokeColor, strokeWeight: 2.5, zIndex: 20, cursor: 'pointer', map: trackMap
    })
    circle.on('mouseover', (e: any) => {
      const px = trackMap.lngLatToContainer(e.lnglat)
      hoverInfo.visible = true
      hoverInfo.x = px.getX()
      hoverInfo.y = px.getY()
      hoverInfo.title = `${r.userName || '-'} · ${r.gridName || '-'}`
      hoverInfo.content = r.content || '（无内容）'
      hoverInfo.time = r.createdAt || ''
    })
    circle.on('mouseout', () => { hoverInfo.visible = false })
    trackOverlays.push(circle)
  })
  // 视野自适应到轨迹范围
  trackMap.setFitView(trackOverlays, false, [60, 60, 60, 60])
}

/** 点击表格行：地图定位到该条打卡 */
function locateRecord(r: any) {
  if (!trackMap || !r.longitude || !r.latitude) return
  trackMap.setCenter([Number(r.longitude), Number(r.latitude)])
  trackMap.setZoom(17)
}

async function initTrackMap() {
  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    AMapLib = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.Polyline', 'AMap.CircleMarker']
    })
    trackMap = new AMapLib.Map('patrolTrackMap', { zoom: 14, center: [113.939521, 22.971231], mapStyle: 'amap://styles/normal' })
    drawTrack()
  } catch (e) {
    console.warn('轨迹地图初始化失败:', e)
  }
}

watch([trackUser, trackRange], () => { drawTrack() })

function statusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待巡查', COMPLETED: '已完成', OVERDUE: '已超期' }
  return map[status] || status
}

async function loadTasks() {
  loading.value = true
  error.value = ''
  try {
    const [tasksRes, statsRes, recordsRes] = await Promise.all([
      getAllPatrolTasks(),
      getPatrolTaskStatistics(),
      getPatrolRecords()
    ])
    tasks.value = tasksRes || []
    stats.value = statsRes || {}
    records.value = recordsRes || []
    // 首次加载完成后初始化轨迹地图（重绘依赖 records，由 watch 无法触发首次绘制）
    if (!trackMap) initTrackMap()
    else drawTrack()
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function generateTasks() {
  try {
    const count = await generatePatrolTasks()
    if (count > 0) {
      showMessage(`成功生成 ${count} 个巡查任务`)
    } else {
      showMessage('本周巡查任务已存在，无需重复生成')
    }
    loadTasks()
  } catch (e: any) {
    showMessage(e?.message || '生成失败')
  }
}

async function markOverdue() {
  try {
    const count = await markOverduePatrolTasks()
    showMessage(`标记了 ${count} 个超期任务`)
    loadTasks()
  } catch (e: any) {
    showMessage(e?.message || '操作失败')
  }
}

async function remindUpcoming() {
  try {
    const count = await remindUpcomingPatrolTasks()
    showMessage(`已发送 ${count} 条到期未巡提醒`)
  } catch (e: any) {
    showMessage(e?.message || '操作失败')
  }
}

onMounted(loadTasks)
</script>
