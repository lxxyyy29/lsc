<template>
  <section class="big-screen">
    <header class="big-screen__header">
      <h1>拔蛟窝社区综合监管总览</h1>
      <div class="big-screen__time">{{ currentTime }}</div>
    </header>

    <div class="big-screen__body">
      <!-- 左侧面板 -->
      <aside class="big-screen__left">
        <div class="panel panel--glow">
          <h3 class="panel__title">网格统计</h3>
          <div class="stat-list">
            <div class="stat-item">
              <span class="stat-label">网格总数</span>
              <span class="stat-value stat-value--blue">{{ overview.gridCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">实有人口</span>
              <span class="stat-value stat-value--green">{{ overview.populationTotal || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">房屋/出租屋</span>
              <span class="stat-value stat-value--orange">{{ overview.buildingCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">场所资源</span>
              <span class="stat-value stat-value--purple">{{ overview.placeCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">组织力量</span>
              <span class="stat-value stat-value--cyan">{{ overview.orgMemberCount || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="panel panel--glow">
          <h3 class="panel__title">三色分级</h3>
          <div class="urgency-bars">
            <div class="urgency-item">
              <span class="urgency-dot urgency-dot--green"></span>
              <span class="urgency-label">一般</span>
              <div class="urgency-bar">
                <div class="urgency-fill urgency-fill--green" :style="{ width: greenPercent + '%' }"></div>
              </div>
              <span class="urgency-count">{{ overview.eventGreen || 0 }}</span>
            </div>
            <div class="urgency-item">
              <span class="urgency-dot urgency-dot--yellow"></span>
              <span class="urgency-label">重点</span>
              <div class="urgency-bar">
                <div class="urgency-fill urgency-fill--yellow" :style="{ width: yellowPercent + '%' }"></div>
              </div>
              <span class="urgency-count">{{ overview.eventYellow || 0 }}</span>
            </div>
            <div class="urgency-item">
              <span class="urgency-dot urgency-dot--red"></span>
              <span class="urgency-label">紧急</span>
              <div class="urgency-bar">
                <div class="urgency-fill urgency-fill--red" :style="{ width: redPercent + '%' }"></div>
              </div>
              <span class="urgency-count">{{ overview.eventRed || 0 }}</span>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间地图 -->
      <main class="big-screen__center">
        <div class="panel panel--map">
          <div ref="mapRef" class="big-screen__map"></div>
        </div>
      </main>

      <!-- 右侧面板 -->
      <aside class="big-screen__right">
        <div class="panel panel--glow">
          <h3 class="panel__title">事件处理</h3>
          <div class="stat-list">
            <div class="stat-item">
              <span class="stat-label">事件总数</span>
              <span class="stat-value stat-value--blue">{{ overview.eventTotal || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待处理</span>
              <span class="stat-value stat-value--orange">{{ overview.eventPending || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">巡查次数</span>
              <span class="stat-value stat-value--green">{{ overview.patrolCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">居民上报</span>
              <span class="stat-value stat-value--purple">{{ overview.reportCount || 0 }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待处理上报</span>
              <span class="stat-value stat-value--red">{{ overview.reportPending || 0 }}</span>
            </div>
          </div>
        </div>

        <div class="panel panel--glow">
          <h3 class="panel__title">网格人口排名</h3>
          <div class="rank-list">
            <div v-for="(item, idx) in gridStats.populationRanking" :key="idx" class="rank-item">
              <span class="rank-num" :class="{ 'rank-num--top': idx < 3 }">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.gridName }}</span>
              <div class="rank-bar">
                <div class="rank-fill" :style="{ width: (item.populationCount / maxPopulation * 100) + '%' }"></div>
              </div>
              <span class="rank-value">{{ item.populationCount }}</span>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue'
import { getDashboardOverview, getGridStats } from './api/community'
import AMapLoader from '@amap/amap-jsapi-loader'

const overview = ref<any>({})
const gridStats = ref<any>({})
const currentTime = ref('')
const mapRef = ref<HTMLDivElement | null>(null)
let mapInstance: any = null
let timer: any = null

const maxPopulation = computed(() => {
  const list = gridStats.value.populationRanking || []
  return Math.max(...list.map((i: any) => i.populationCount || 0), 1)
})

const totalEvents = computed(() => (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const greenPercent = computed(() => totalEvents.value ? ((overview.value.eventGreen || 0) / totalEvents.value * 100) : 0)
const yellowPercent = computed(() => totalEvents.value ? ((overview.value.eventYellow || 0) / totalEvents.value * 100) : 0)
const redPercent = computed(() => totalEvents.value ? ((overview.value.eventRed || 0) / totalEvents.value * 100) : 0)

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

async function loadData() {
  try {
    overview.value = await getDashboardOverview()
    gridStats.value = await getGridStats()
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await loadData()

  try {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: '0a57a5453a660300283bebf7323d8bce' }
    const AMap = await AMapLoader.load({
      key: '5e00e01d2d2b6ca9e1eed533a15572e4',
      version: '2.0',
      plugins: ['AMap.DistrictSearch', 'AMap.HeatMap'],
    })

    if (mapRef.value) {
      mapInstance = new AMap.Map(mapRef.value, {
        zoom: 14,
        center: [113.939521, 22.971231],
        mapStyle: 'amap://styles/dark',
      })
    }
  } catch (e) {
    console.error('地图加载失败', e)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (mapInstance) mapInstance.destroy()
})
</script>

<style scoped>
.big-screen {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #030913;
  color: #eaf5ff;
  overflow: hidden;
}

.big-screen__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: linear-gradient(90deg, #0a1d33 0%, #0d3866 50%, #0a1d33 100%);
  border-bottom: 1px solid rgba(87, 185, 255, 0.2);
}

.big-screen__header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(90deg, #57b9ff, #1e88e5);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.big-screen__time {
  font-size: 18px;
  color: #7ea4c8;
  font-family: monospace;
}

.big-screen__body {
  display: grid;
  grid-template-columns: 320px 1fr 320px;
  gap: 16px;
  flex: 1;
  padding: 16px;
  min-height: 0;
}

.big-screen__left,
.big-screen__right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.big-screen__center {
  min-height: 0;
}

.panel {
  background: rgba(14, 35, 58, 0.92);
  border: 1px solid rgba(110, 194, 255, 0.14);
  border-radius: 16px;
  padding: 16px;
}

.panel--glow {
  box-shadow: 0 0 20px rgba(87, 185, 255, 0.08);
}

.panel--map {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.panel__title {
  margin: 0 0 12px;
  font-size: 14px;
  color: #cfe5fb;
  border-left: 3px solid #57b9ff;
  padding-left: 8px;
}

.big-screen__map {
  width: 100%;
  height: 100%;
  min-height: 400px;
}

.stat-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid rgba(110, 194, 255, 0.08);
}

.stat-label {
  font-size: 13px;
  color: #7ea4c8;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
}

.stat-value--blue { color: #57b9ff; }
.stat-value--green { color: #8ce56d; }
.stat-value--orange { color: #f0c060; }
.stat-value--purple { color: #c792ea; }
.stat-value--cyan { color: #4dd0e1; }
.stat-value--red { color: #ff8080; }

.urgency-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.urgency-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.urgency-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.urgency-dot--green { background: #8ce56d; }
.urgency-dot--yellow { background: #f0c060; }
.urgency-dot--red { background: #ff6b6b; }

.urgency-label {
  width: 40px;
  font-size: 13px;
  color: #cfe5fb;
}

.urgency-bar {
  flex: 1;
  height: 10px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 5px;
  overflow: hidden;
}

.urgency-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s;
}

.urgency-fill--green { background: linear-gradient(90deg, #8ce56d, #4caf50); }
.urgency-fill--yellow { background: linear-gradient(90deg, #f0c060, #ff9800); }
.urgency-fill--red { background: linear-gradient(90deg, #ff6b6b, #f44336); }

.urgency-count {
  width: 30px;
  text-align: right;
  font-size: 14px;
  font-weight: 700;
  color: #eaf5ff;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rank-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(94, 162, 255, 0.15);
  color: #7ea4c8;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-num--top {
  background: rgba(94, 162, 255, 0.3);
  color: #57b9ff;
}

.rank-name {
  width: 60px;
  font-size: 12px;
  color: #cfe5fb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.rank-fill {
  height: 100%;
  background: linear-gradient(90deg, #57b9ff, #1e88e5);
  border-radius: 3px;
}

.rank-value {
  width: 25px;
  text-align: right;
  font-size: 12px;
  color: #eaf5ff;
}
</style>
