<template>
  <div class="page">
    <div class="header">
      <h2>🤝 活动报名</h2>
      <p>参与志愿活动，获取志愿服务积分</p>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else>
      <div v-for="a in activities" :key="a.id" class="card">
        <div class="card-title">{{ a.title }}</div>
        <p class="desc">{{ a.description }}</p>
        <div class="card-meta">
          <span>📅 {{ a.activityDate }}</span>
          <span>👥 {{ a.attendedCount || 0 }}/{{ a.maxParticipants || '∞' }}</span>
        </div>
        <div class="card-action">
          <button v-if="!a.signedUp && a.status === 'PLANNED'" @click="signup(a.id)" class="btn-primary">立即报名</button>
          <button v-else-if="a.signedUp" @click="cancelSignup(a.id)" class="btn-default">取消报名</button>
          <span v-else class="tag tag-gray">已结束</span>
        </div>
      </div>
      <p v-if="!activities.length" class="empty">暂无可报名活动</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getResidentActivities, signupActivity, cancelActivitySignup } from '../api'

const loading = ref(false)
const activities = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    activities.value = await getResidentActivities() || []
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

async function signup(id: number) {
  try {
    await signupActivity(id)
    alert('报名成功！')
    loadData()
  } catch (e: any) {
    alert('报名失败：' + (e?.message || '未知错误'))
  }
}

async function cancelSignup(id: number) {
  try {
    await cancelActivitySignup(id)
    alert('已取消报名')
    loadData()
  } catch (e: any) {
    alert('取消失败：' + (e?.message || '未知错误'))
  }
}

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.desc { font-size: 13px; color: #6b7280; margin-bottom: 10px; }
.card-meta { display: flex; justify-content: space-between; font-size: 12px; color: #9ca3af; margin-bottom: 12px; }
.card-action { display: flex; justify-content: flex-end; }
.btn-primary {
  padding: 8px 20px; background: #1890ff; color: #fff; border: none;
  border-radius: 6px; font-size: 13px; cursor: pointer;
}
.btn-default {
  padding: 8px 20px; background: #f3f4f6; color: #6b7280; border: none;
  border-radius: 6px; font-size: 13px; cursor: pointer;
}
.tag { font-size: 11px; padding: 4px 10px; border-radius: 4px; }
.tag-gray { background: #f3f4f6; color: #9ca3af; }
.loading { text-align: center; padding: 40px; color: #9ca3af; }
.empty { text-align: center; padding: 40px; color: #9ca3af; font-size: 13px; }
</style>
