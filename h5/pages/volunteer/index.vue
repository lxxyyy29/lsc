<template>
  <view class="container">
    <!-- 积分概览 -->
    <view class="hero-card">
      <view class="hero-title">志愿服务</view>
      <view class="hero-subtitle">参与志愿活动，共建美好社区</view>
      <view class="points-row">
        <view class="points-item">
          <text class="points-value">{{ points?.totalPoints ?? 0 }}</text>
          <text class="points-label">累计积分</text>
        </view>
        <view class="points-item">
          <text class="points-value">{{ points?.availablePoints ?? 0 }}</text>
          <text class="points-label">可用积分</text>
        </view>
      </view>
    </view>

    <!-- 活动列表 -->
    <view class="section-title">活动报名</view>
    <view v-if="loading" class="empty-text">加载中...</view>
    <view v-else-if="activities.length" class="activity-list">
      <view v-for="a in activities" :key="a.id" class="activity-card">
        <view class="activity-header">
          <text class="activity-title">{{ a.title }}</text>
          <text class="activity-status" :class="`status--${a.status}`">{{ statusLabel(a.status) }}</text>
        </view>
        <text v-if="a.description" class="activity-desc">{{ a.description }}</text>
        <view class="activity-meta">
          <text>📅 {{ a.activity_date }}</text>
          <text>👥 {{ a.signedUpCount || 0 }}/{{ a.max_participants || '不限' }}</text>
        </view>
        <view class="activity-action">
          <template v-if="a.signedUp">
            <button v-if="!a.checkedIn" class="btn-signup" @click="checkin(a.id)">签到 +20积分</button>
            <text v-else class="tag-checked">已签到 ✓</text>
            <button v-if="!a.checkedIn" class="btn-cancel" @click="cancelSignup(a.id)">取消报名</button>
          </template>
          <button v-else-if="a.status === 'PLANNED' || a.status === 'ONGOING'" class="btn-signup" @click="signup(a.id)">立即报名</button>
          <text v-else class="tag-closed">已结束</text>
        </view>
      </view>
    </view>
    <view v-else class="empty-text">暂无可报名活动</view>

    <!-- 积分流水 -->
    <view v-if="logs.length" class="section-title">积分明细</view>
    <view v-if="logs.length" class="log-list">
      <view v-for="(log, index) in logs" :key="index" class="log-row">
        <view class="log-left">
          <text class="log-reason">{{ log.reason || log.sourceType || '积分变动' }}</text>
          <text class="log-time">{{ log.createdAt }}</text>
        </view>
        <text class="log-points" :class="log.points >= 0 ? 'log-points--plus' : 'log-points--minus'">
          {{ log.points >= 0 ? '+' : '' }}{{ log.points }}
        </text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  getVolunteerActivities,
  getMyVolunteerPoints,
  signupVolunteerActivity,
  checkinVolunteerActivity,
  cancelVolunteerActivitySignup,
  type VolunteerActivity,
  type VolunteerPointsResponse
} from '../../src/api/volunteer'
import { ensureAuthenticated } from '../../src/uni/navigation'

const loading = ref(false)
const activities = ref<VolunteerActivity[]>([])
const points = ref<VolunteerPointsResponse['account'] | null>(null)
const logs = ref<VolunteerPointsResponse['logs']>([])

function statusLabel(status: string) {
  const map: Record<string, string> = { PLANNED: '报名中', ONGOING: '进行中', COMPLETED: '已结束', CANCELLED: '已取消' }
  return map[status] || status
}

async function loadActivities() {
  loading.value = true
  try {
    activities.value = await getVolunteerActivities() || []
  } catch (e) {
    console.error('加载活动失败:', e)
  } finally {
    loading.value = false
  }
}

async function loadPoints() {
  try {
    const data = await getMyVolunteerPoints()
    points.value = data?.account || null
    logs.value = data?.logs || []
  } catch (e) {
    console.error('加载积分失败:', e)
  }
}

function showToast(title: string) {
  const uniRef = typeof uni !== 'undefined' ? uni : (globalThis as any).uni
  uniRef?.showToast?.({ title, icon: 'none', duration: 2000 })
}

async function signup(id: number) {
  try {
    await signupVolunteerActivity(id)
    showToast('报名成功')
    loadActivities()
  } catch (e: any) {
    showToast('报名失败：' + (e?.message || '未知错误'))
  }
}

async function cancelSignup(id: number) {
  try {
    await cancelVolunteerActivitySignup(id)
    showToast('已取消报名')
    loadActivities()
  } catch (e: any) {
    showToast('取消失败：' + (e?.message || '未知错误'))
  }
}

async function checkin(id: number) {
  try {
    await checkinVolunteerActivity(id)
    showToast('签到成功，+20积分')
    loadActivities()
    loadPoints()
  } catch (e: any) {
    showToast('签到失败：' + (e?.message || '未知错误'))
  }
}

onShow(async () => {
  if (!ensureAuthenticated('/volunteer')) return
  loadActivities()
  loadPoints()
})
</script>

<style scoped>
.container { padding: 20px; background: #030913; min-height: 100vh; }
.hero-card {
  background: linear-gradient(135deg, #0a2a4a, #0d3866);
  border-radius: 16px; padding: 20px; margin-bottom: 20px;
}
.hero-title { font-size: 22px; font-weight: bold; color: #eaf5ff; }
.hero-subtitle { font-size: 13px; color: #7ea4c8; margin-top: 4px; margin-bottom: 16px; }
.points-row { display: flex; gap: 16px; }
.points-item { flex: 1; background: rgba(255,255,255,0.06); border-radius: 12px; padding: 12px; text-align: center; }
.points-value { display: block; font-size: 26px; font-weight: bold; color: #57b9ff; }
.points-label { font-size: 12px; color: #7ea4c8; margin-top: 4px; display: block; }
.section-title { font-size: 16px; font-weight: bold; color: #eaf5ff; margin-bottom: 12px; }
.activity-list { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
.activity-card { background: #0e233a; border-radius: 12px; padding: 16px; }
.activity-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.activity-title { font-size: 15px; font-weight: 600; color: #eaf5ff; flex: 1; margin-right: 8px; }
.activity-status { font-size: 12px; flex-shrink: 0; }
.status--PLANNED { color: #8ce56d; }
.status--ONGOING { color: #57b9ff; }
.status--COMPLETED, .status--CANCELLED { color: #7ea4c8; }
.activity-desc { font-size: 13px; color: #7ea4c8; margin-bottom: 10px; display: block; }
.activity-meta { display: flex; justify-content: space-between; font-size: 12px; color: #7ea4c8; margin-bottom: 12px; }
.activity-action { display: flex; justify-content: flex-end; }
.btn-signup {
  padding: 8px 22px; background: #1890ff; color: #fff; border: none;
  border-radius: 8px; font-size: 13px;
}
.btn-cancel {
  padding: 8px 22px; background: #1b3350; color: #7ea4c8; border: 1px solid #2c4a6e;
  border-radius: 8px; font-size: 13px;
}
.tag-closed { font-size: 12px; color: #7ea4c8; padding: 8px 0; }
.tag-checked { font-size: 12px; color: #67e8a9; font-weight: 600; padding: 8px 0; }
.log-list { background: #0e233a; border-radius: 12px; overflow: hidden; }
.log-row { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid rgba(255,255,255,0.05); }
.log-row:last-child { border-bottom: none; }
.log-reason { font-size: 13px; color: #eaf5ff; display: block; }
.log-time { font-size: 11px; color: #7ea4c8; margin-top: 4px; display: block; }
.log-points { font-size: 15px; font-weight: 600; }
.log-points--plus { color: #8ce56d; }
.log-points--minus { color: #f0c060; }
.empty-text { text-align: center; color: #7ea4c8; padding: 40px; font-size: 13px; }
</style>

<style>
/* 网格员端深色主题：页面根背景与容器一致，避免滑动露出浅色 page 背景 */
page {
  background: #081421;
}
</style>
