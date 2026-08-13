<template>
  <div>
    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:16px;">
      <div class="card card-border-blue" style="padding:14px;">
        <p class="stat-label">监测设备总数</p>
        <p class="stat-value" style="color:#1890ff;">{{ stats.devices || 0 }}</p>
      </div>
      <div class="card card-border-green" style="padding:14px;">
        <p class="stat-label">在线设备</p>
        <p class="stat-value" style="color:#16a34a;">{{ stats.online || 0 }}</p>
      </div>
      <div class="card card-border-purple" style="padding:14px;">
        <p class="stat-label">今日上报</p>
        <p class="stat-value" style="color:#722ed1;">{{ stats.todayReports || 0 }}</p>
      </div>
      <div class="card card-border-red" style="padding:14px;">
        <p class="stat-label">近7日超标</p>
        <p class="stat-value" style="color:#dc2626;">{{ stats.overThreshold || 0 }}</p>
      </div>
    </div>

    <!-- 工具栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;justify-content:space-between;flex-wrap:wrap;">
      <div style="display:flex;gap:12px;align-items:center;">
        <select v-model="filterSite" @change="handleFilterChange" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;max-width:220px;">
          <option value="">全部孳生地</option>
          <option v-for="s in siteOptions" :key="s.id" :value="s.id">{{ s.site_name }}</option>
        </select>
        <select v-model="filterMetric" @change="handleFilterChange" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option v-for="m in metricOptions" :key="m.value" :value="m.value">{{ m.label }}</option>
        </select>
        <select v-model="filterHours" @change="handleFilterChange" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option :value="24">近24小时</option>
          <option :value="72">近3天</option>
          <option :value="168">近7天</option>
        </select>
        <span style="font-size:12px;color:#6b7280;">设备 {{ deviceTotal }} 台 · 数据 {{ dataTotal }} 条</span>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <select v-model="simDays" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option :value="1">1天</option>
          <option :value="3">3天</option>
          <option :value="7">7天</option>
        </select>
        <button @click="handleSimulate" :disabled="simulating" style="padding:8px 16px;border:none;border-radius:6px;background:#722ed1;color:#fff;font-size:13px;cursor:pointer;">
          <i class="fas fa-sync"></i> {{ simulating ? '生成中...' : '一键生成演示数据' }}
        </button>
      </div>
    </div>

    <!-- 设备台账 + 趋势图 -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:16px;">
      <div class="card" style="padding:16px;">
        <p style="font-weight:600;font-size:14px;margin-bottom:12px;">监测设备台账</p>
        <table class="table">
          <thead>
            <tr>
              <th>设备 / 类型</th>
              <th>部署位置</th>
              <th>在线状态</th>
              <th>最新指标</th>
              <th>数值</th>
              <th>最后上报</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in devices" :key="d.id">
              <td>
                <div style="font-weight:500;font-size:13px;">{{ d.device_name || d.device_no }}</div>
                <div style="font-size:11px;color:#9ca3af;">{{ d.device_no }}</div>
                <div style="font-size:11px;color:#9ca3af;">{{ deviceTypeLabel(d.device_type) }}</div>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ d.site_name || '-' }}</td>
              <td>
                <span :class="['tag', d.status === 'ONLINE' ? 'tag-green' : 'tag-gray']">{{ d.status === 'ONLINE' ? '在线' : '离线' }}</span>
              </td>
              <td style="font-size:12px;">{{ metricLabel(d.last_metric) }}</td>
              <td style="font-size:12px;font-weight:600;">{{ d.last_metric_value != null ? d.last_metric_value : '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ formatTime(d.last_data_at || d.last_online_at) }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!devices.length" style="text-align:center;padding:30px;color:#9ca3af;">暂无设备,点击右上角"一键生成演示数据"自动注册</p>
      </div>
      <div class="card" style="padding:16px;">
        <p style="font-weight:600;font-size:14px;margin-bottom:12px;">监测趋势（{{ metricLabel(filterMetric) }}）<span v-if="selectedSiteName" style="font-weight:400;color:#6b7280;font-size:12px;"> · {{ selectedSiteName }}</span></p>
        <div ref="chartEl" style="width:100%;height:320px;"></div>
        <p v-if="!trendPoints.length" style="text-align:center;padding:30px;color:#9ca3af;">暂无监测数据,点击"一键生成演示数据"生成趋势</p>
      </div>
    </div>

    <!-- 数据明细 -->
    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th>采集时间</th>
            <th>设备编号</th>
            <th>孳生地</th>
            <th>指标</th>
            <th>数值</th>
            <th>阈值</th>
            <th>级别</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in deviceData" :key="d.id">
            <td style="font-size:12px;">{{ formatTime(d.collected_at) }}</td>
            <td style="font-size:12px;">{{ d.device_no }}</td>
            <td style="font-size:12px;">{{ d.site_name || '-' }}</td>
            <td style="font-size:12px;">{{ metricLabel(d.metric_type) }}</td>
            <td style="font-size:12px;font-weight:600;">{{ d.metric_value }}</td>
            <td style="font-size:12px;color:#9ca3af;">{{ d.threshold }}</td>
            <td><span :class="['tag', d.alarm_level === 'OVER' ? 'tag-red' : 'tag-green']">{{ d.alarm_level === 'OVER' ? '超标' : '正常' }}</span></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!deviceData.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无上报数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import {
  getMosquitoDevices, getMosquitoDeviceData, getMosquitoDeviceTrend,
  getMosquitoDeviceStatistics, simulateMosquitoDeviceData, getMosquitoSites
} from '../api'

const metricOptions = [
  { value: 'DENSITY', label: '成蚊密度（只/灯·夜）' },
  { value: 'CAPTURE', label: '捕获数（只）' },
  { value: 'BITE', label: '叮咬率（次/人·夜）' },
  { value: 'BREEDING', label: '孳生指数（%）' }
]
const deviceTypeMap: Record<string, string> = {
  MOSQUITO_TRAP: '智能捕蚊器', DENSITY_MONITOR: '密度监测仪', SENSOR: '传感器'
}

const stats = ref<Record<string, number>>({})
const devices = ref<any[]>([])
const deviceTotal = ref(0)
const deviceData = ref<any[]>([])
const dataTotal = ref(0)
const trendPoints = ref<any[]>([])
const siteOptions = ref<any[]>([])
const filterSite = ref<number | string>('')
const filterMetric = ref('DENSITY')
const filterHours = ref(72)
const simDays = ref(3)
const simulating = ref(false)
const chartEl = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const selectedSiteName = ref('')

function metricLabel(m?: string) {
  return metricOptions.find(x => x.value === m)?.label || m || '-'
}
function deviceTypeLabel(t?: string) {
  return deviceTypeMap[t || ''] || t || '-'
}
function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}
function formatChartTime(t?: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(5, 16)
}

async function loadStats() {
  stats.value = await getMosquitoDeviceStatistics()
}
async function loadDevices() {
  const r = await getMosquitoDevices({ page: 1, size: 100 })
  devices.value = r.items || []
  deviceTotal.value = r.total || 0
}
async function loadDeviceData() {
  const r = await getMosquitoDeviceData({
    siteId: filterSite.value ? Number(filterSite.value) : undefined,
    metricType: filterMetric.value, page: 1, size: 50
  })
  deviceData.value = r.items || []
  dataTotal.value = r.total || 0
}
async function loadTrend() {
  trendPoints.value = await getMosquitoDeviceTrend({
    siteId: filterSite.value ? Number(filterSite.value) : undefined,
    metricType: filterMetric.value, hours: filterHours.value
  })
  renderChart()
}
async function loadSiteOptions() {
  const r = await getMosquitoSites({ status: 'ACTIVE', page: 1, size: 100 })
  siteOptions.value = r.items || []
  const sel = siteOptions.value.find(s => s.id === filterSite.value)
  selectedSiteName.value = sel ? sel.site_name : ''
}

function renderChart() {
  if (!chartEl.value) return
  if (!chartInstance) chartInstance = echarts.init(chartEl.value)
  const points = trendPoints.value
  const times = points.map(p => formatChartTime(p.collected_at))
  const values = points.map(p => Number(p.metric_value))
  const threshold = points.length ? Number(points[0].threshold) : null
  const overIdx = new Set(points.map((p, i) => p.alarm_level === 'OVER' ? i : -1).filter(i => i >= 0))
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 24, bottom: 40 },
    xAxis: { type: 'category', data: times, axisLabel: { fontSize: 10, rotate: 30 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', data: values, smooth: true, symbolSize: 5,
      lineStyle: { width: 2, color: '#1890ff' },
      itemStyle: { color: (p: { dataIndex: number }) => overIdx.has(p.dataIndex) ? '#dc2626' : '#1890ff' },
      markLine: threshold ? {
        silent: true, symbol: 'none',
        label: { formatter: '阈值 ' + threshold, fontSize: 10 },
        lineStyle: { color: '#d97706', type: 'dashed' },
        data: [{ yAxis: threshold }]
      } : undefined
    }]
  }, true)
}

function handleFilterChange() {
  const sel = siteOptions.value.find(s => s.id === filterSite.value)
  selectedSiteName.value = sel ? sel.site_name : ''
  loadTrend()
  loadDeviceData()
}

async function handleSimulate() {
  simulating.value = true
  try {
    const r = await simulateMosquitoDeviceData(Number(simDays.value))
    await Promise.all([loadStats(), loadDevices(), loadTrend(), loadDeviceData(), loadSiteOptions()])
    alert(`演示数据生成完成: ${r.generated} 条(超标 ${r.overThreshold} 条),覆盖 ${r.sites} 处孳生地`)
  } catch (e: any) {
    alert('生成失败: ' + (e?.message || e))
  } finally {
    simulating.value = false
  }
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(async () => {
  await Promise.all([loadStats(), loadDevices(), loadDeviceData(), loadSiteOptions()])
  await loadTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>
