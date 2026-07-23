<template>
  <section class="dashboard">
    <header class="dashboard__header">
      <h2>BI 态势看板</h2>
      <p class="dashboard__subtitle">拔蛟窝社区治理数据总览</p>
    </header>

    <!-- KPI 卡片 -->
    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--blue">📊</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.gridCount || 0 }}</span>
          <span class="kpi-label">网格数</span>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--green">🏪</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.merchantCount || 0 }}</span>
          <span class="kpi-label">场所台账</span>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--orange">🏠</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.rentalHouseCount || 0 }}</span>
          <span class="kpi-label">出租屋</span>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--purple">🏬</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.smallShopCount || 0 }}</span>
          <span class="kpi-label">三小场所</span>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--cyan">📋</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.otherPlaceCount || 0 }}</span>
          <span class="kpi-label">其他场所</span>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon kpi-icon--red">🚨</div>
        <div class="kpi-info">
          <span class="kpi-value">{{ overview.eventTotal || 0 }}</span>
          <span class="kpi-label">事件总数</span>
        </div>
      </div>
    </div>

    <!-- 三色分级 -->
    <div class="panel urgency-panel">
      <h3 class="panel__title">事件紧急程度分布</h3>
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

    <!-- 网格排名 -->
    <div class="panel" v-if="gridStats.populationRanking && gridStats.populationRanking.length">
      <h3 class="panel__title">各网格人口排名</h3>
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

    <!-- 高频问题 -->
    <div class="panel" v-if="gridStats.hotIssues && gridStats.hotIssues.length">
      <h3 class="panel__title">高频问题类型</h3>
      <div class="issue-list">
        <div v-for="(item, idx) in gridStats.hotIssues" :key="idx" class="issue-item">
          <span class="issue-type">{{ typeLabel(item.reportType) }}</span>
          <span class="issue-count">{{ item.count }} 次</span>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { getDashboardOverview, getGridStats } from '../../api/community'

const overview = ref<any>({})
const gridStats = ref<any>({})

const maxPopulation = computed(() => {
  const list = gridStats.value.populationRanking || []
  return Math.max(...list.map((i: any) => i.populationCount || 0), 1)
})

const totalEvents = computed(() => (overview.value.eventGreen || 0) + (overview.value.eventYellow || 0) + (overview.value.eventRed || 0))
const greenPercent = computed(() => totalEvents.value ? ((overview.value.eventGreen || 0) / totalEvents.value * 100) : 0)
const yellowPercent = computed(() => totalEvents.value ? ((overview.value.eventYellow || 0) / totalEvents.value * 100) : 0)
const redPercent = computed(() => totalEvents.value ? ((overview.value.eventRed || 0) / totalEvents.value * 100) : 0)

function typeLabel(type: string) {
  const map: Record<string, string> = { COMPLAINT: '投诉', REPAIR: '报修', ACTIVITY: '活动', POLICY: '政策', HAZARD: '隐患' }
  return map[type] || type
}

async function loadData() {
  try {
    overview.value = await getDashboardOverview()
    gridStats.value = await getGridStats()
  } catch (e) {
    console.error('加载看板数据失败', e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.dashboard { padding: 20px; height: 100%; overflow-y: auto; }
.dashboard__header h2 { margin: 0; font-size: 20px; color: var(--fg-text-primary); }
.dashboard__subtitle { margin: 4px 0 16px; color: var(--fg-text-secondary); font-size: 13px; }

.kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.kpi-card { background: var(--fg-bg-card); border: 1px solid var(--fg-border); border-radius: 12px; padding: 16px; display: flex; align-items: center; gap: 12px; }
.kpi-icon { font-size: 28px; width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; background: rgba(94, 162, 255, 0.1); }
.kpi-info { display: flex; flex-direction: column; }
.kpi-value { font-size: 24px; font-weight: bold; color: var(--fg-text-primary); }
.kpi-label { font-size: 12px; color: var(--fg-text-secondary); }

.panel { background: var(--fg-bg-card); border: 1px solid var(--fg-border); border-radius: 16px; padding: 16px; margin-bottom: 16px; }
.panel__title { margin: 0 0 12px; font-size: 14px; color: var(--fg-text-primary); }

.urgency-bars { display: flex; flex-direction: column; gap: 12px; }
.urgency-item { display: flex; align-items: center; gap: 8px; }
.urgency-dot { width: 10px; height: 10px; border-radius: 50%; }
.urgency-dot--green { background: #8ce56d; }
.urgency-dot--yellow { background: #f0c060; }
.urgency-dot--red { background: #ff6b6b; }
.urgency-label { width: 40px; font-size: 13px; color: var(--fg-text-secondary); }
.urgency-bar { flex: 1; height: 8px; background: rgba(255,255,255,0.06); border-radius: 4px; overflow: hidden; }
.urgency-fill { height: 100%; border-radius: 4px; transition: width 0.5s; }
.urgency-fill--green { background: #8ce56d; }
.urgency-fill--yellow { background: #f0c060; }
.urgency-fill--red { background: #ff6b6b; }
.urgency-count { width: 30px; text-align: right; font-size: 13px; color: var(--fg-text-primary); }

.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-item { display: flex; align-items: center; gap: 8px; }
.rank-num { width: 20px; height: 20px; border-radius: 50%; background: rgba(94, 162, 255, 0.15); color: var(--fg-text-secondary); font-size: 12px; display: flex; align-items: center; justify-content: center; }
.rank-num--top { background: rgba(94, 162, 255, 0.3); color: #57b9ff; }
.rank-name { width: 80px; font-size: 13px; color: var(--fg-text-primary); }
.rank-bar { flex: 1; height: 6px; background: rgba(255,255,255,0.06); border-radius: 3px; overflow: hidden; }
.rank-fill { height: 100%; background: linear-gradient(90deg, #57b9ff, #1e88e5); border-radius: 3px; }
.rank-value { width: 30px; text-align: right; font-size: 13px; color: var(--fg-text-primary); }

.issue-list { display: flex; flex-wrap: wrap; gap: 8px; }
.issue-item { background: rgba(94, 162, 255, 0.1); border-radius: 8px; padding: 8px 12px; display: flex; gap: 8px; align-items: center; }
.issue-type { font-size: 13px; color: var(--fg-text-primary); }
.issue-count { font-size: 12px; color: var(--fg-text-secondary); }
</style>
