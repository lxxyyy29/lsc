<template>
  <div class="page">
    <div class="header">
      <h2>⭐ 志愿服务积分</h2>
      <p>参与志愿活动，积累服务积分</p>
    </div>

    <div class="points-overview">
      <div class="points-item">
        <span class="points-num">{{ points.totalPoints || 0 }}</span>
        <span class="points-label">累计积分</span>
      </div>
      <div class="points-item">
        <span class="points-num">{{ points.availablePoints || 0 }}</span>
        <span class="points-label">可用积分</span>
      </div>
    </div>

    <div class="section-title">积分明细</div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-for="log in points.logs || []" :key="log.id" class="card log-card">
        <div class="log-left">
          <span class="log-reason">{{ log.reason || '积分变动' }}</span>
          <span class="log-time">{{ log.createdAt }}</span>
        </div>
        <span :class="['log-points', log.points > 0 ? 'positive' : 'negative']">
          {{ log.points > 0 ? '+' : '' }}{{ log.points }}
        </span>
      </div>
      <p v-if="!points.logs || !points.logs.length" class="empty">暂无积分记录</p>
    </div>

    <div class="tips">
      <div class="tips-title">💡 如何获取积分？</div>
      <ul>
        <li>参与志愿活动并完成签到：+20 积分/次</li>
        <li>处理报修工单：+10 积分/次</li>
        <li>参与矛盾调解：+15 积分/次</li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMyPoints } from '../api'

const loading = ref(false)
const points = ref<any>({})

async function loadData() {
  loading.value = true
  try {
    points.value = await getMyPoints() || {}
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.points-overview {
  display: flex; gap: 12px; margin-bottom: 20px;
}
.points-item {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  background: #fff; border-radius: 12px; padding: 16px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.points-num { font-size: 28px; font-weight: 700; color: #faad14; }
.points-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
.section-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; color: #374151; }
.log-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 12px; padding: 14px 16px; margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.log-left { display: flex; flex-direction: column; }
.log-reason { font-size: 14px; color: #374151; }
.log-time { font-size: 11px; color: #9ca3af; margin-top: 4px; }
.log-points { font-size: 18px; font-weight: 700; }
.log-points.positive { color: #52c41a; }
.log-points.negative { color: #ff4d4f; }
.loading { text-align: center; padding: 20px; color: #9ca3af; }
.empty { text-align: center; padding: 20px; color: #9ca3af; font-size: 13px; }
.tips {
  margin-top: 20px; background: #fffbe6; border-radius: 12px; padding: 16px;
  border: 1px solid #ffe58f;
}
.tips-title { font-size: 14px; font-weight: 600; color: #d48806; margin-bottom: 10px; }
.tips ul { padding-left: 18px; }
.tips li { font-size: 13px; color: #ad6800; margin-bottom: 6px; line-height: 1.6; }
</style>
