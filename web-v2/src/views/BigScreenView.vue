<template>
  <div class="big-screen">
    <!-- 顶部标题栏 -->
    <div class="bs-header">
      <h1><i class="fas fa-chart-line"></i> 网格社区综合监管大屏</h1>
      <div class="bs-time">{{ currentTime }}</div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="bs-loading">
      <i class="fas fa-spinner fa-spin"></i> 数据加载中...
    </div>

    <template v-else>
      <!-- 核心指标卡片 -->
      <div class="bs-kpi-row">
        <div class="bs-kpi-card kpi-blue">
          <div class="kpi-icon"><i class="fas fa-exclamation-circle"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.eventTotal || 0 }}</p>
            <p class="kpi-label">事件总数</p>
          </div>
        </div>
        <div class="bs-kpi-card kpi-green">
          <div class="kpi-icon"><i class="fas fa-calendar-day"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.eventToday || 0 }}</p>
            <p class="kpi-label">今日新增</p>
          </div>
        </div>
        <div class="bs-kpi-card kpi-orange">
          <div class="kpi-icon"><i class="fas fa-clock"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.eventPending || 0 }}</p>
            <p class="kpi-label">待处置</p>
          </div>
        </div>
        <div class="bs-kpi-card kpi-purple">
          <div class="kpi-icon"><i class="fas fa-check-circle"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.eventClosed || 0 }}</p>
            <p class="kpi-label">已处置</p>
          </div>
        </div>
        <div class="bs-kpi-card kpi-cyan">
          <div class="kpi-icon"><i class="fas fa-clipboard-list"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.workOrderTotal || 0 }}</p>
            <p class="kpi-label">工单总数</p>
          </div>
        </div>
        <div class="bs-kpi-card kpi-red">
          <div class="kpi-icon"><i class="fas fa-user-clock"></i></div>
          <div class="kpi-info">
            <p class="kpi-value">{{ data.kpis?.workOrderProcessing || 0 }}</p>
            <p class="kpi-label">处置中</p>
          </div>
        </div>
      </div>

      <!-- 图表行 -->
      <div class="bs-chart-row">
        <!-- 紧急程度分布 -->
        <div class="bs-chart-card">
          <h3 class="chart-title">紧急程度分布</h3>
          <div class="chart-content">
            <div v-for="item in data.urgencyDist" :key="item.level" class="urgency-bar-row">
              <span class="urgency-label" :class="'urgency-' + (item.level || '').toLowerCase()">
                {{ urgencyLabel(item.level) }}
              </span>
              <div class="urgency-bar-track">
                <div class="urgency-bar-fill" :class="'bar-' + (item.level || '').toLowerCase()"
                     :style="{ width: getUrgencyWidth(item.count) + '%' }"></div>
              </div>
              <span class="urgency-count">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <!-- 事件类型分布 -->
        <div class="bs-chart-card">
          <h3 class="chart-title">事件类型 TOP8</h3>
          <div class="chart-content">
            <div v-for="(item, idx) in data.eventTypeDist" :key="item.type" class="type-bar-row">
              <span class="type-rank" :class="{ 'top3': idx < 3 }">{{ idx + 1 }}</span>
              <span class="type-label">{{ item.type || '未知' }}</span>
              <div class="type-bar-track">
                <div class="type-bar-fill" :style="{ width: getTypeWidth(item.count) + '%' }"></div>
              </div>
              <span class="type-count">{{ item.count }}</span>
            </div>
            <p v-if="!data.eventTypeDist?.length" style="color:#999;text-align:center;padding:20px;">暂无数据</p>
          </div>
        </div>

        <!-- 近 7 天趋势 -->
        <div class="bs-chart-card">
          <h3 class="chart-title">近 7 天事件趋势</h3>
          <div class="chart-content trend-chart">
            <div class="trend-bars">
              <div v-for="item in data.weeklyTrend" :key="item.date" class="trend-bar-col">
                <div class="trend-bar" :style="{ height: getTrendHeight(item.count) + '%' }">
                  <span class="trend-value">{{ item.count }}</span>
                </div>
                <span class="trend-date">{{ formatTrendDate(item.date) }}</span>
              </div>
            </div>
            <p v-if="!data.weeklyTrend?.length" style="color:#999;text-align:center;padding:20px;">暂无数据</p>
          </div>
        </div>
      </div>

      <!-- 底部行 -->
      <div class="bs-bottom-row">
        <!-- 网格排名 -->
        <div class="bs-chart-card">
          <h3 class="chart-title">各网格事件排名</h3>
          <div class="chart-content">
            <div v-for="(item, idx) in data.gridRanking" :key="item.gridName" class="rank-row">
              <span class="rank-num" :class="{ 'top3': idx < 3 }">{{ idx + 1 }}</span>
              <span class="rank-name">{{ item.gridName || '未知' }}</span>
              <span class="rank-count">{{ item.count }}</span>
            </div>
            <p v-if="!data.gridRanking?.length" style="color:#999;text-align:center;padding:20px;">暂无数据</p>
          </div>
        </div>

        <!-- 最新工单 -->
        <div class="bs-chart-card">
          <h3 class="chart-title">最新工单动态</h3>
          <div class="chart-content">
            <div v-for="item in data.recentWorkOrders" :key="item.workOrderNo" class="wo-row">
              <span class="wo-no">{{ item.workOrderNo }}</span>
              <span class="wo-status" :class="'status-' + (item.status || '').toLowerCase()">{{ workOrderStatusLabel(item.status) }}</span>
              <span class="wo-assignee">{{ item.assigneeName || '未派单' }}</span>
              <span class="wo-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <p v-if="!data.recentWorkOrders?.length" style="color:#999;text-align:center;padding:20px;">暂无工单</p>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getBigScreenData } from '../api'

const loading = ref(true)
const data = ref<any>({})
const currentTime = ref('')
let timer: number

async function loadData() {
  try {
    data.value = await getBigScreenData() || {}
  } catch (e) {
    console.error('加载大屏数据失败:', e)
  } finally {
    loading.value = false
  }
}

function updateTime() {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false
  })
}

function urgencyLabel(level: string) {
  const map: Record<string, string> = { GREEN: '一般', YELLOW: '较重', RED: '紧急' }
  return map[level] || level || '未知'
}

function workOrderStatusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_ACCEPT: '待接单', PROCESSING: '处理中',
    WAITING_CLOSE_CONFIRM: '待确认', COMPLETED: '已完成', CLOSED: '已关闭'
  }
  return map[status] || status || '未知'
}

function formatTrendDate(date: string) {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

function formatTime(time: any) {
  if (!time) return ''
  try {
    const d = new Date(time)
    return `${d.getMonth() + 1}/${d.getDate()} ${d.getHours()}:${String(d.getMinutes()).padStart(2, '0')}`
  } catch { return time }
}

function getUrgencyWidth(count: number) {
  const max = Math.max(...(data.value.urgencyDist || []).map((i: any) => i.count || 0), 1)
  return Math.max(5, (count / max) * 100)
}

function getTypeWidth(count: number) {
  const max = Math.max(...(data.value.eventTypeDist || []).map((i: any) => i.count || 0), 1)
  return Math.max(5, (count / max) * 100)
}

function getTrendHeight(count: number) {
  const max = Math.max(...(data.value.weeklyTrend || []).map((i: any) => i.count || 0), 1)
  return Math.max(10, (count / max) * 100)
}

onMounted(() => {
  loadData()
  updateTime()
  timer = window.setInterval(() => {
    updateTime()
    loadData()  // 每 60 秒刷新数据
  }, 60000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.big-screen {
  padding: 20px;
  background: #0a1628;
  min-height: calc(100vh - 60px);
  color: #e0e6ed;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.bs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #1e3a5f;
}

.bs-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.bs-header h1 i {
  color: #00d4ff;
  margin-right: 10px;
}

.bs-time {
  font-size: 14px;
  color: #8899aa;
}

.bs-loading {
  text-align: center;
  padding: 60px;
  color: #8899aa;
  font-size: 16px;
}

/* KPI 卡片 */
.bs-kpi-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.bs-kpi-card {
  background: linear-gradient(135deg, #1a2d4a 0%, #0f2137 100%);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #1e3a5f;
}

.kpi-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.kpi-blue .kpi-icon { background: rgba(0, 123, 255, 0.2); color: #007bff; }
.kpi-green .kpi-icon { background: rgba(40, 167, 69, 0.2); color: #28a745; }
.kpi-orange .kpi-icon { background: rgba(255, 152, 0, 0.2); color: #ff9800; }
.kpi-purple .kpi-icon { background: rgba(156, 39, 176, 0.2); color: #9c27b0; }
.kpi-cyan .kpi-icon { background: rgba(0, 212, 255, 0.2); color: #00d4ff; }
.kpi-red .kpi-icon { background: rgba(255, 82, 82, 0.2); color: #ff5252; }

.kpi-value {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0;
  line-height: 1;
}

.kpi-label {
  font-size: 12px;
  color: #8899aa;
  margin: 4px 0 0;
}

/* 图表卡片 */
.bs-chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.bs-bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.bs-chart-card {
  background: linear-gradient(135deg, #1a2d4a 0%, #0f2137 100%);
  border-radius: 12px;
  border: 1px solid #1e3a5f;
  padding: 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #1e3a5f;
}

.chart-content {
  min-height: 200px;
}

/* 紧急程度条形图 */
.urgency-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.urgency-label {
  width: 40px;
  font-size: 12px;
  text-align: center;
  padding: 2px 0;
  border-radius: 4px;
}

.urgency-green { background: rgba(40, 167, 69, 0.2); color: #28a745; }
.urgency-yellow { background: rgba(255, 193, 7, 0.2); color: #ffc107; }
.urgency-red { background: rgba(255, 82, 82, 0.2); color: #ff5252; }

.urgency-bar-track {
  flex: 1;
  height: 20px;
  background: #0a1628;
  border-radius: 4px;
  overflow: hidden;
}

.urgency-bar-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s;
}

.bar-green { background: linear-gradient(90deg, #28a745, #5dd879); }
.bar-yellow { background: linear-gradient(90deg, #ffc107, #ffe083); }
.bar-red { background: linear-gradient(90deg, #ff5252, #ff8a80); }

.urgency-count {
  width: 40px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
}

/* 事件类型条形图 */
.type-bar-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.type-rank {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  background: #1e3a5f;
  color: #8899aa;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-rank.top3 {
  background: #00d4ff;
  color: #0a1628;
  font-weight: 700;
}

.type-label {
  width: 80px;
  font-size: 12px;
  color: #ccc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-bar-track {
  flex: 1;
  height: 18px;
  background: #0a1628;
  border-radius: 4px;
  overflow: hidden;
}

.type-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #007bff, #00d4ff);
  border-radius: 4px;
  transition: width 0.5s;
}

.type-count {
  width: 36px;
  text-align: right;
  font-size: 13px;
  font-weight: 600;
}

/* 趋势图 */
.trend-chart {
  display: flex;
  align-items: flex-end;
}

.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  width: 100%;
  height: 180px;
  padding-top: 10px;
}

.trend-bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.trend-bar {
  width: 100%;
  max-width: 40px;
  background: linear-gradient(180deg, #00d4ff, #007bff);
  border-radius: 4px 4px 0 0;
  position: relative;
  transition: height 0.5s;
  min-height: 20px;
}

.trend-value {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 11px;
  color: #00d4ff;
  font-weight: 600;
}

.trend-date {
  margin-top: 6px;
  font-size: 11px;
  color: #8899aa;
}

/* 排名 */
.rank-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #1a2d4a;
}

.rank-num {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: #1e3a5f;
  color: #8899aa;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-num.top3 {
  background: #ff9800;
  color: #fff;
  font-weight: 700;
}

.rank-name {
  flex: 1;
  font-size: 13px;
}

.rank-count {
  font-size: 14px;
  font-weight: 600;
  color: #00d4ff;
}

/* 工单动态 */
.wo-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #1a2d4a;
  font-size: 12px;
}

.wo-no {
  width: 100px;
  color: #8899aa;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wo-status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}

.status-waiting_accept { background: rgba(255, 152, 0, 0.2); color: #ff9800; }
.status-processing { background: rgba(0, 123, 255, 0.2); color: #007bff; }
.status-completed { background: rgba(40, 167, 69, 0.2); color: #28a745; }
.status-closed { background: rgba(108, 117, 125, 0.2); color: #6c757d; }
.status-waiting_close_confirm { background: rgba(156, 39, 176, 0.2); color: #9c27b0; }

.wo-assignee {
  flex: 1;
  color: #ccc;
}

.wo-time {
  color: #8899aa;
  font-size: 11px;
}
</style>
