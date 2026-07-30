<template>
  <div class="page">
    <div class="header">
      <h2>📋 我的上报</h2>
      <p>查看上报记录和处理进度</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-for="item in reports" :key="item.id" class="card">
        <div class="card-top">
          <span class="title">{{ item.title }}</span>
          <span :class="['status', statusClass(item.status)]">{{ statusLabel(item.status) }}</span>
        </div>
        <p class="desc">{{ item.description }}</p>
        <div class="card-bottom">
          <span class="time">{{ item.createdAt }}</span>
          <span class="code">查询码：{{ item.eventCode || item.id }}</span>
        </div>
        <!-- 办结后评价 -->
        <div v-if="item.status === 'CLOSED'" class="rating-section">
          <div v-if="item.rating" class="rating-display">
            <span class="stars">
              <span v-for="n in 5" :key="n" :class="['star', n <= item.rating ? 'star-active' : '']">★</span>
            </span>
            <span class="rating-text">{{ item.ratingText || '已评价' }}</span>
          </div>
          <div v-else class="rating-action">
            <span class="rating-label">请评价：</span>
            <span class="stars-input">
              <span v-for="n in 5" :key="n" class="star-input" @click="rateItem(item, n)"
                    :class="{ 'star-hover': hoverRating[item.id] >= n }"
                    @mouseenter="hoverRating[item.id] = n"
                    @mouseleave="hoverRating[item.id] = 0">★</span>
            </span>
          </div>
        </div>
      </div>
      <p v-if="!reports.length" class="empty">暂无上报记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyReports, rateEvent } from '../api'

const loading = ref(false)
const reports = ref<any[]>([])
const hoverRating = ref<Record<string, number>>({})

function statusLabel(status: string) {
  const map: any = {
    PENDING: '待处理',
    WAITING_DISPATCH: '待派单',
    DISPATCHED_TO_WORK_ORDER: '处理中',
    PROCESSING: '处理中',
    WAITING_CLOSE_CONFIRM: '待确认',
    CLOSED: '已办结',
    IGNORED: '已忽略',
    REJECTED: '已驳回'
  }
  return map[status] || status || '未知'
}
function statusClass(status: string) {
  if (status === 'CLOSED') return 'status-green'
  if (status === 'DISPATCHED_TO_WORK_ORDER' || status === 'PROCESSING' || status === 'WAITING_CLOSE_CONFIRM') return 'status-blue'
  if (status === 'REJECTED' || status === 'IGNORED') return 'status-red'
  return 'status-orange'
}

async function rateItem(item: any, rating: number) {
  try {
    await rateEvent(item.id, { rating, comment: '' })
    item.rating = rating
    item.ratingText = ['', '很不满意', '不满意', '一般', '满意', '非常满意'][rating]
  } catch (e) {
    console.error('评价失败:', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMyReports()
    reports.value = res?.items || res?.data?.items || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.loading { text-align: center; padding: 40px; color: #9ca3af; }
.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.title { font-size: 14px; font-weight: 600; flex: 1; }
.status { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.status-green { background: #f6ffed; color: #52c41a; }
.status-blue { background: #e6f7ff; color: #1890ff; }
.status-orange { background: #fff7e6; color: #fa8c16; }
.status-red { background: #fff1f0; color: #ff4d4f; }
.desc { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.card-bottom { display: flex; justify-content: space-between; font-size: 11px; color: #9ca3af; }
.empty { text-align: center; padding: 40px; color: #9ca3af; }
.rating-section { margin-top: 12px; padding-top: 12px; border-top: 1px solid #f3f4f6; }
.rating-display { display: flex; align-items: center; gap: 8px; }
.stars { display: flex; gap: 2px; }
.star { font-size: 16px; color: #d1d5db; }
.star-active { color: #faad14; }
.rating-text { font-size: 12px; color: #6b7280; }
.rating-action { display: flex; align-items: center; gap: 8px; }
.rating-label { font-size: 12px; color: #6b7280; }
.stars-input { display: flex; gap: 4px; }
.star-input { font-size: 24px; color: #d1d5db; cursor: pointer; transition: color 0.2s; }
.star-input.star-hover, .star-input:hover { color: #faad14; }
</style>
