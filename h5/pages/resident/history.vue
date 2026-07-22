<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">上报记录</view>
      <view class="hero-subtitle">我的居民上报历史</view>
    </view>
    <view v-if="reports.length" class="record-list">
      <view v-for="report in reports" :key="report.id" class="record-card">
        <view class="record-header">
          <text class="record-type">{{ typeLabel(report.reportType) }}</text>
          <text class="record-status" :class="`status--${report.status}`">{{ statusLabel(report.status) }}</text>
        </view>
        <text class="record-title">{{ report.title }}</text>
        <text v-if="report.queryCode" class="record-code">查询码: {{ report.queryCode }}</text>
      </view>
    </view>
    <view v-else class="empty-text">暂无上报记录</view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getResidentReports, ResidentReport } from '../../src/api/community'

const reports = ref<ResidentReport[]>([])

function typeLabel(type?: string) {
  const map: Record<string, string> = { COMPLAINT: '投诉', REPAIR: '报修', ACTIVITY: '活动', POLICY: '政策', HAZARD: '隐患' }
  return map[type || ''] || '-'
}

function statusLabel(status?: string) {
  const map: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
  return map[status || ''] || '-'
}

onMounted(async () => {
  try { reports.value = await getResidentReports() } catch (e) { console.error(e) }
})
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card { background: linear-gradient(135deg, #0a2a4a, #0d3866); border-radius: 16px; padding: 20px; margin-bottom: 16px; }
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; }
.record-list { display: flex; flex-direction: column; gap: 12px; }
.record-card { background: #0e233a; border-radius: 12px; padding: 16px; }
.record-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.record-type { font-size: 12px; color: #57b9ff; }
.record-status { font-size: 12px; }
.status--PENDING { color: #f0c060; }
.status--PROCESSING { color: #57b9ff; }
.status--RESOLVED { color: #8ce56d; }
.status--CLOSED { color: #7ea4c8; }
.record-title { font-size: 14px; color: #eaf5ff; }
.record-code { font-size: 12px; color: #7ea4c8; margin-top: 4px; display: block; }
.empty-text { text-align: center; color: #7ea4c8; padding: 40px; }
</style>
