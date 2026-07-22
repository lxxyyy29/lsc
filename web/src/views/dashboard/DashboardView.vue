<template>
  <PageContainer title="首页">
    <section class="dashboard-board">
      <section class="dashboard-metrics panel" aria-label="驾驶舱指标概览">
        <article v-for="item in metrics" :key="item.label" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="dashboard-main-grid">
        <section class="dashboard-panel panel dashboard-panel--map">
          <header class="dashboard-panel__header">
            <h3>运营热力图</h3>
          </header>
          <div ref="mapContainer" class="heatmap-container"></div>
          <div v-if="!mapReady" class="heatmap-loading">地图加载中...</div>
        </section>

        <section class="dashboard-side-stack">
          <section class="dashboard-panel panel">
            <header class="dashboard-panel__header">
              <h3>最新事件</h3>
            </header>

            <div class="dashboard-list" aria-label="最新告警列表">
              <div class="dashboard-list__item" v-for="item in latestEvents" :key="item.title">
                <span>{{ item.title }}</span>
                <strong :class="['alert-tag', `alert-tag--${item.tone}`]">{{ item.status }}</strong>
              </div>
              <div v-if="!latestEvents.length" class="dashboard-list__item">
                <span>{{ loadingData ? '加载中...' : '暂无事件数据' }}</span>
              </div>
            </div>
          </section>
        </section>
      </section>

    </section>
  </PageContainer>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import PageContainer from '../../components/admin/PageContainer.vue'
import { listEventsPaged, getEventStatusLabel, type EventListItem } from '../../api/event'
import { listWorkOrdersPaged } from '../../api/workorder'
import { listDroneJobsPage, listDroneAiModelsPaged } from '../../api/drone'

const loadingData = ref(true)
const mapContainer = ref<HTMLElement>()
const mapReady = ref(false)
let mapInstance: any = null

const metrics = ref([
  { label: '待派单事件', value: '--' },
  { label: '待处理工单', value: '--' },
  { label: '巡检任务', value: '--' },
  { label: '算法模型', value: '--' }
])

const latestEvents = ref<Array<{ title: string; status: string; tone: string }>>([])

function statusTone(status: string): string {
  switch (status) {
    case 'PENDING_AUDIT':
    case 'AUDITING':
      return 'orange'
    case 'WAITING_DISPATCH':
      return 'blue'
    case 'PROCESSING':
      return 'blue'
    case 'COMPLETED':
    case 'CLOSED':
      return 'green'
    default:
      return 'blue'
  }
}

async function initHeatmap(events: EventListItem[]) {
  if (!mapContainer.value) return

  const points = events
    .filter((e) => e.longitude != null && e.latitude != null && e.longitude !== 0 && e.latitude !== 0)
    .map((e) => ({ lng: e.longitude!, lat: e.latitude!, title: e.title || e.eventCode || '未命名事件', status: e.currentStatus }))

  console.log('[Dashboard] 事件总数:', events.length, '有坐标的事件:', points.length)
  if (points.length) {
    console.log('[Dashboard] 坐标样例:', points.slice(0, 3).map((p) => `${p.lng},${p.lat}`))
  }

  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }

    const AMap = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.HeatMap']
    })

    // Find the densest area: grid-based density, center on highest concentration
    let center: [number, number] = [113.9917, 22.9648] // fallback: Changping
    if (points.length) {
      const gridSize = 0.005 // ~500m grid cells
      const density = new Map<string, { count: number; sumLng: number; sumLat: number }>()
      for (const p of points) {
        const key = `${Math.round(p.lng / gridSize)}_${Math.round(p.lat / gridSize)}`
        const cell = density.get(key) || { count: 0, sumLng: 0, sumLat: 0 }
        cell.count++
        cell.sumLng += p.lng
        cell.sumLat += p.lat
        density.set(key, cell)
      }
      let maxCell = { count: 0, sumLng: 0, sumLat: 0 }
      for (const cell of density.values()) {
        if (cell.count > maxCell.count) maxCell = cell
      }
      if (maxCell.count > 0) {
        center = [maxCell.sumLng / maxCell.count, maxCell.sumLat / maxCell.count]
      }
    }

    mapInstance = new AMap.Map(mapContainer.value, {
      zoom: 13,
      center,
      mapStyle: 'amap://styles/blue',
      viewMode: '2D'
    })

    mapReady.value = true

    if (points.length) {
      const heatmapData = points.map((p) => ({
        lng: p.lng,
        lat: p.lat,
        count: 1
      }))

      const heatmap = new AMap.HeatMap(mapInstance, {
        radius: 50,
        opacity: [0, 0.9],
        gradient: {
          0.2: '#0ff',
          0.4: '#0cf',
          0.6: '#09f',
          0.8: '#ff0',
          1.0: '#f00'
        }
      })

      heatmap.setDataSet({
        data: heatmapData,
        max: 3
      })

      // Add event markers
      const infoWindow = new AMap.InfoWindow({ offset: new AMap.Pixel(0, -30), closeWhenClickMap: true })
      for (const p of points) {
        const marker = new AMap.Marker({
          position: [p.lng, p.lat],
          anchor: 'center',
          content: `<div style="width:10px;height:10px;border-radius:50%;background:#73ebff;border:2px solid #fff;box-shadow:0 0 6px rgba(115,235,255,0.6);"></div>`
        })
        marker.on('click', () => {
          infoWindow.setContent(`<div style="padding:6px 10px;font-size:13px;color:#333;max-width:240px;"><strong>${p.title}</strong><br/><span style="color:#666;">${getEventStatusLabel(p.status)}</span></div>`)
          infoWindow.open(mapInstance, [p.lng, p.lat])
        })
        mapInstance.add(marker)
      }
    }
  } catch (error) {
    console.error('地图加载失败:', error)
  }
}

onMounted(async () => {
  loadingData.value = true

  // Fetch all stats in parallel
  const [eventsRes, workordersRes, jobsRes, modelsRes, allEventsRes, latestRes] = await Promise.allSettled([
    listEventsPaged(1, 1, { status: 'WAITING_DISPATCH' }),
    listWorkOrdersPaged(1, 1, 'PROCESSING'),
    listDroneJobsPage({ page: 1, pageSize: 1 }),
    listDroneAiModelsPaged(1, 1),
    listEventsPaged(1, 100, {}),
    listEventsPaged(1, 10, {})
  ])

  // Stats
  metrics.value[0].value = eventsRes.status === 'fulfilled' ? String(eventsRes.value.total) : '0'
  metrics.value[1].value = workordersRes.status === 'fulfilled' ? String(workordersRes.value.total) : '0'
  metrics.value[2].value = jobsRes.status === 'fulfilled' ? String(jobsRes.value.total ?? 0) : '0'
  metrics.value[3].value = modelsRes.status === 'fulfilled' ? String(modelsRes.value.total) : '0'

  // Latest events
  if (latestRes.status === 'fulfilled') {
    latestEvents.value = latestRes.value.items.map((item: EventListItem) => ({
      title: item.title || item.eventCode || '未命名事件',
      status: getEventStatusLabel(item.currentStatus),
      tone: statusTone(item.currentStatus)
    }))
  }

  loadingData.value = false

  // Init heatmap with all events
  const allEvents = allEventsRes.status === 'fulfilled' ? allEventsRes.value.items : []
  await initHeatmap(allEvents)
})

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
  }
})
</script>

<style scoped>
@import '../admin-shared.css';

.dashboard-board {
  display: grid;
  gap: 18px;
  height: calc(100vh - 122px);
  grid-template-rows: auto 1fr;
  overflow: hidden;
}

.dashboard-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.metric-card,
.dashboard-panel {
  border: 1px solid rgba(103, 187, 246, 0.14);
  background: rgba(8, 30, 50, 0.8);
  border-radius: 6px;
}

.metric-card {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.metric-card span,
.dashboard-list__item span {
  color: #8db0d0;
}

.metric-card strong,
.dashboard-panel h3,
.dashboard-list__item strong {
  color: #eaf5ff;
}

.metric-card strong {
  font-size: 32px;
  line-height: 1.1;
}

.dashboard-main-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1.15fr) minmax(360px, 0.85fr);
  min-height: 0;
}

.dashboard-side-stack {
  display: grid;
  gap: 18px;
  min-height: 0;
}

.dashboard-panel {
  padding: 20px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.dashboard-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-panel h3 {
  margin: 0;
  font-size: 24px;
}

/* 热力图 */
.dashboard-panel--map {
  position: relative;
}

.heatmap-container {
  width: 100%;
  flex: 1;
  min-height: 300px;
  margin-top: 16px;
  border-radius: 12px;
  overflow: hidden;
}

.heatmap-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #8db0d0;
  font-size: 14px;
}

.dashboard-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  flex: 1;
  overflow-y: auto;
  align-content: start;
}

.dashboard-list__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  background: rgba(8, 30, 50, 0.8);
}

.dashboard-list__item strong {
  text-align: right;
}

.alert-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  white-space: nowrap;
}

.alert-tag--red {
  color: #ffdbe0;
  background: rgba(255, 113, 134, 0.18);
}

.alert-tag--orange {
  color: #ffe3bd;
  background: rgba(247, 186, 90, 0.18);
}

.alert-tag--blue {
  color: #d7f4ff;
  background: rgba(115, 235, 255, 0.16);
}

.alert-tag--green {
  color: #d4f8e8;
  background: rgba(110, 230, 167, 0.16);
}

@media (max-width: 1280px) {
  .dashboard-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-metrics {
    grid-template-columns: 1fr;
  }

  .dashboard-panel {
    padding: 18px;
  }

  .dashboard-panel__header,
  .dashboard-list__item {
    flex-direction: column;
    align-items: flex-start;
  }

  .dashboard-list__item strong {
    text-align: left;
  }
}
</style>
