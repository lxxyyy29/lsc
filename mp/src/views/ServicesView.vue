<template>
  <!-- 子路由（活动报名/便民报修/政策查询/积分明细） -->
  <router-view v-if="isChildRoute" />
  <div v-else class="page">
    <div class="header">
      <h2>🏠 便民服务</h2>
      <p>活动报名 / 便民报修 / 政策查询 / 志愿服务积分</p>
    </div>

    <div class="points-card" @click="goToPoints">
      <div class="points-info">
        <span class="points-label">我的志愿服务积分</span>
        <span class="points-value">{{ points.totalPoints || 0 }}</span>
      </div>
      <span class="points-arrow">›</span>
    </div>

    <div class="service-grid">
      <div class="service-item" @click="goToActivities">
        <span class="service-icon">🤝</span>
        <span class="service-name">活动报名</span>
      </div>
      <div class="service-item" @click="goToRepair">
        <span class="service-icon">🔧</span>
        <span class="service-name">便民报修</span>
      </div>
      <div class="service-item" @click="goToPolicies">
        <span class="service-icon">📋</span>
        <span class="service-name">政策查询</span>
      </div>
      <div class="service-item" @click="goToPoints">
        <span class="service-icon">⭐</span>
        <span class="service-name">积分明细</span>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h3>最新活动</h3>
        <span class="more" @click="goToActivities">查看全部 ›</span>
      </div>
      <div v-if="loading" class="loading">加载中...</div>
      <div v-else>
        <div v-for="a in activities.slice(0, 3)" :key="a.id" class="card activity-card" @click="goToActivities">
          <div class="card-title">{{ a.title }}</div>
          <div class="card-meta">
            <span>📅 {{ a.activityDate }}</span>
            <span class="tag tag-blue">{{ a.status === 'PLANNED' ? '报名中' : a.status }}</span>
          </div>
        </div>
        <p v-if="!activities.length" class="empty">暂无活动</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getResidentActivities, getMyPoints } from '../api'

const router = useRouter()
const route = useRoute()
// 进入子路由时隐藏服务首页内容，渲染对应子页面
const isChildRoute = computed(() => route.path !== '/services')
const loading = ref(false)
const activities = ref<any[]>([])
const points = ref<any>({})

async function loadData() {
  loading.value = true
  try {
    const [acts, pts]: any = await Promise.all([
      getResidentActivities(),
      getMyPoints().catch(() => ({}))
    ])
    activities.value = acts || []
    points.value = pts || {}
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

function goToActivities() { router.push('/services/activities') }
function goToRepair() { router.push('/services/repairs') }
function goToPolicies() { router.push('/services/policies') }
function goToPoints() { router.push('/services/points') }

onMounted(loadData)
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
  border-radius: 12px; padding: 20px; color: #fff; margin-bottom: 16px;
}
.header h2 { font-size: 20px; margin-bottom: 4px; }
.header p { font-size: 13px; opacity: 0.8; }
.points-card {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%);
  border-radius: 12px; padding: 16px 20px; margin-bottom: 16px; cursor: pointer;
}
.points-info { display: flex; flex-direction: column; }
.points-label { font-size: 12px; color: rgba(255,255,255,0.8); }
.points-value { font-size: 28px; font-weight: 700; color: #fff; }
.points-arrow { font-size: 28px; color: rgba(255,255,255,0.8); }
.service-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px;
}
.service-item {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  padding: 16px 8px; background: #fff; border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04); cursor: pointer;
}
.service-icon { font-size: 28px; }
.service-name { font-size: 12px; color: #374151; }
.section { margin-top: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-header h3 { font-size: 15px; font-weight: 600; }
.more { font-size: 12px; color: #722ed1; cursor: pointer; }
.card {
  background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 8px; }
.card-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #6b7280; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; }
.tag-blue { background: #e6f7ff; color: #1890ff; }
.loading { text-align: center; padding: 20px; color: #9ca3af; }
.empty { text-align: center; padding: 20px; color: #9ca3af; font-size: 13px; }
</style>
