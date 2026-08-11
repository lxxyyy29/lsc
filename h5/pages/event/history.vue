<template>
  <view class="container">
    <view class="hero-card">
      <view class="hero-title">我的上报</view>
      <view class="hero-subtitle">我上报的事件与处理进度</view>
    </view>
    <view v-if="reports.length" class="record-list">
      <view v-for="report in reports" :key="report.id" class="record-card">
        <view class="record-header">
          <text class="record-code">{{ report.eventCode }}</text>
          <text class="record-status" :class="`status--${report.status}`">{{ statusLabel(report.status) }}</text>
        </view>
        <text class="record-title">{{ report.title }}</text>
        <text v-if="report.description" class="record-desc">{{ report.description }}</text>
        <text class="record-time">{{ formatTime(report.createdAt) }}</text>
      </view>
    </view>
    <view v-else-if="!loading" class="empty-text">
      <text>暂无上报记录</text>
      <text class="empty-hint">发现问题后可从工作台「事件上报」发起上报</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getMyReportedEvents, MyReportedEvent } from '../../src/api/event'
import { ensureAuthenticated } from '../../src/uni/navigation'

const reports = ref<MyReportedEvent[]>([])
const loading = ref(true)

function statusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_DISPATCH: '待派单',
    DISPATCHED_TO_WORK_ORDER: '处理中',
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '已通过',
    AUDIT_REJECTED: '已驳回',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

function formatTime(value?: string) {
  if (!value) return ''
  const normalized = value.replace('T', ' ')
  return normalized.length >= 16 ? normalized.slice(0, 16) : normalized
}

onShow(async () => {
  if (!ensureAuthenticated('/pages/event/history')) return
  loading.value = true
  try {
    reports.value = await getMyReportedEvents()
  } catch (e) {
    uni.showToast({ title: '加载失败', icon: 'none' })
    console.error(e)
  } finally {
    loading.value = false
  }
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
.record-code { font-size: 12px; color: #57b9ff; }
.record-status { font-size: 12px; }
.status--WAITING_DISPATCH { color: #f0c060; }
.status--DISPATCHED_TO_WORK_ORDER { color: #57b9ff; }
.status--PENDING_AUDIT { color: #f0c060; }
.status--IN_AUDIT { color: #f0c060; }
.status--AUDIT_APPROVED { color: #8ce56d; }
.status--AUDIT_REJECTED { color: #ff7a7a; }
.status--CLOSED { color: #7ea4c8; }
.status--IGNORED { color: #7ea4c8; }
.record-title { font-size: 14px; color: #eaf5ff; display: block; }
.record-desc {
  font-size: 12px; color: #7ea4c8; margin-top: 6px; display: -webkit-box;
  -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden;
}
.record-time { font-size: 12px; color: #4d7296; margin-top: 8px; display: block; }
.empty-text { text-align: center; color: #7ea4c8; padding: 48px 20px; display: grid; gap: 8px; }
.empty-hint { font-size: 12px; color: #4d7296; }
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
