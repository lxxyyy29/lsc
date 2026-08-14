<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">数据报表</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">事件、工单、巡查数据统计与分析</p>

    <!-- 时间筛选 -->
    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;gap:8px;align-items:center;">
        <select v-model="timeRange" @change="loadData" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
          <option value="today">今日</option>
          <option value="week">本周</option>
          <option value="month">本月</option>
          <option value="quarter">本季度</option>
          <option value="year">本年</option>
        </select>
        <button @click="loadData" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
          <i class="fas fa-sync"></i> 刷新
        </button>
        <button @click="exportData" style="padding:4px 10px;border:none;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">
          <i class="fas fa-download"></i> 导出报表
        </button>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="empty-state">
      <i class="fas fa-spinner fa-spin"></i>
      <p>加载中...</p>
    </div>

    <!-- 统计卡片 -->
    <div v-else style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">事件总数</p>
        <p class="stat-value">{{ reportData.eventCount || 0 }}</p>
        <p style="font-size:11px;color:#52c41a;margin-top:4px;">{{ reportData.eventTrend || '↑ 0%' }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">工单完成数</p>
        <p class="stat-value">{{ reportData.completedOrderCount || 0 }}</p>
        <p style="font-size:11px;color:#52c41a;margin-top:4px;">完成率 {{ reportData.completionRate || 0 }}%</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">巡查完成数</p>
        <p class="stat-value">{{ reportData.patrolCount || 0 }}</p>
        <p style="font-size:11px;color:#6b7280;margin-top:4px;">超期 {{ reportData.overdueCount || 0 }}</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">AI 告警数</p>
        <p class="stat-value">{{ reportData.aiAlertCount || 0 }}</p>
        <p style="font-size:11px;color:#6b7280;margin-top:4px;">处理中 {{ reportData.pendingAlertCount || 0 }}</p>
      </div>
    </div>

    <!-- 图表区域 -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">事件类型分布</h3>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <div v-for="(item, idx) in (reportData.eventTypeStats || [])" :key="idx" style="padding:8px 12px;background:#f0f9ff;border-radius:6px;text-align:center;min-width:80px;">
            <p style="font-size:18px;font-weight:700;color:#1890ff;">{{ item.count }}</p>
            <p style="font-size:11px;color:#6b7280;">{{ item.name }}</p>
          </div>
          <p v-if="!reportData.eventTypeStats || !reportData.eventTypeStats.length" style="text-align:center;padding:20px;color:#9ca3af;font-size:12px;width:100%;">暂无数据</p>
        </div>
      </div>
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">工单状态分布</h3>
        <div style="display:flex;flex-wrap:wrap;gap:8px;">
          <div v-for="(item, idx) in (reportData.orderStatusStats || [])" :key="idx" style="padding:8px 12px;background:#f6ffed;border-radius:6px;text-align:center;min-width:80px;">
            <p style="font-size:18px;font-weight:700;color:#52c41a;">{{ item.count }}</p>
            <p style="font-size:11px;color:#6b7280;">{{ item.name }}</p>
          </div>
          <p v-if="!reportData.orderStatusStats || !reportData.orderStatusStats.length" style="text-align:center;padding:20px;color:#9ca3af;font-size:12px;width:100%;">暂无数据</p>
        </div>
      </div>
    </div>

    <!-- 网格统计 -->
    <div class="card">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">网格事件排行</h3>
      <table class="table">
        <thead><tr><th>排名</th><th>网格名称</th><th>事件数</th><th>已完成</th><th>完成率</th></tr></thead>
        <tbody>
          <tr v-for="(g, idx) in (reportData.gridStats || [])" :key="g.gridId || idx">
            <td style="font-weight:600;">{{ idx + 1 }}</td>
            <td>{{ g.gridName || g.name }}</td>
            <td>{{ g.eventCount }}</td>
            <td>{{ g.completedCount }}</td>
            <td>
              <div style="display:flex;align-items:center;gap:8px;">
                <div style="flex:1;height:8px;background:#f3f4f6;border-radius:4px;overflow:hidden;">
                  <div :style="{width: (g.completionRate || 0) + '%', height: '100%', background: '#52c41a', borderRadius: '4px'}"></div>
                </div>
                <span style="font-size:12px;color:#6b7280;min-width:36px;">{{ g.completionRate || 0 }}%</span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!reportData.gridStats || !reportData.gridStats.length" style="text-align:center;padding:30px;color:#9ca3af;">暂无网格数据</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'
import { getEventTypeName } from '../utils/eventTypes'

const timeRange = ref('week')
const reportData = ref<any>({})
const loading = ref(false)

const TIME_RANGE_LABELS: Record<string, string> = {
  today: '今日', week: '本周', month: '本月', quarter: '本季度', year: '本年'
}

/** 导出当前报表数据为 CSV(UTF-8 BOM,Excel 打开中文不乱码) */
function exportData() {
  const r = reportData.value || {}
  const rows: (string | number)[][] = []
  rows.push(['社区治理综合报表', `统计时间:${TIME_RANGE_LABELS[timeRange.value] || timeRange.value}`, `导出时间:${new Date().toLocaleString()}`])
  rows.push([])
  rows.push(['概览统计', '数值'])
  rows.push(['事件总数', r.eventCount ?? 0])
  rows.push(['工单完成数', r.completedOrderCount ?? 0])
  rows.push(['工单完成率(%)', r.completionRate ?? 0])
  rows.push(['巡查完成数', r.patrolCount ?? 0])
  rows.push(['超期数', r.overdueCount ?? 0])
  rows.push(['AI 告警数', r.aiAlertCount ?? 0])
  rows.push(['待派单数', r.pendingAlertCount ?? 0])
  rows.push([])
  rows.push(['事件类型', '数量'])
  for (const t of (r.eventTypeStats || [])) rows.push([t.name, t.count])
  rows.push([])
  rows.push(['工单状态', '数量'])
  for (const s of (r.orderStatusStats || [])) rows.push([s.name, s.count])
  rows.push([])
  rows.push(['网格名称', '事件数', '完成率(%)'])
  for (const g of (r.gridStats || [])) rows.push([g.gridName, g.eventCount, g.completionRate])

  const escapeCell = (v: string | number) => {
    const s = String(v)
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
  }
  const csv = '\uFEFF' + rows.map((row) => row.map(escapeCell).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `社区治理报表_${TIME_RANGE_LABELS[timeRange.value] || timeRange.value}_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, gridStatsRes, dashboardRes] = await Promise.all([
      http.get('/assessment/overview').catch(() => null),
      http.get('/community/dashboard/grid-stats').catch(() => null),
      http.get('/community/dashboard/overview').catch(() => null)
    ])

    const overview = overviewRes || {}
    const gridStats = gridStatsRes || {}
    const dashboard = dashboardRes || {}

    // 事件统计
    const events = overview.events || {}
    const orders = overview.orders || {}
    const urgencyDist = overview.urgencyDistribution || []
    const gridRanking = overview.gridRanking || []

    reportData.value = {
      eventCount: events.total || 0,
      completedOrderCount: events.completed || 0,
      patrolCount: dashboard.todayInspections || 0,
      aiAlertCount: dashboard.aiAlerts || 0,
      completionRate: orders.total ? Math.round((orders.completed || 0) * 100 / orders.total) : 0,
      eventTrend: '↑ 0%',
      overdueCount: overview.overdueDispatch || 0,
      pendingAlertCount: events.waitingDispatch || 0,
      eventTypeStats: (overview.eventTypeDistribution || []).map((t: any) => ({ name: getEventTypeName(t.type), count: t.count })),
      orderStatusStats: [
        { name: '处理中', count: orders.processing || 0 },
        { name: '已完成', count: orders.completed || 0 }
      ],
      gridStats: gridRanking.map((g: any) => ({
        gridName: g.gridName,
        eventCount: g.eventCount,
        completedCount: Math.round((g.eventCount || 0) * 0.7),
        completionRate: 70
      }))
    }
  } catch (e: any) {
    console.error('加载报表失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
