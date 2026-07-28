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
      </div>
    </div>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;">
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

const timeRange = ref('week')
const reportData = ref<any>({})

async function loadData() {
  // 模拟数据，实际应从后端 API 获取
  reportData.value = {
    eventCount: 0,
    completedOrderCount: 0,
    patrolCount: 0,
    aiAlertCount: 0,
    completionRate: 0,
    eventTrend: '↑ 0%',
    overdueCount: 0,
    pendingAlertCount: 0,
    eventTypeStats: [],
    orderStatusStats: [],
    gridStats: []
  }
}

onMounted(() => {
  loadData()
})
</script>
