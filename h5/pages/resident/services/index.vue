<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <view class="header">
      <text class="header-title">🏠 便民服务</text>
      <text class="header-sub">活动报名 / 便民报修 / 政策查询 / 志愿服务积分</text>
    </view>

    <view class="points-card" @click="goToPoints">
      <view class="points-info">
        <text class="points-label">我的志愿服务积分</text>
        <text class="points-value">{{ points.totalPoints || 0 }}</text>
      </view>
      <text class="points-arrow">›</text>
    </view>

    <view class="service-grid">
      <view class="service-item" @click="goToActivities">
        <text class="service-icon">🤝</text>
        <text class="service-name">活动报名</text>
      </view>
      <view class="service-item" @click="goToRepair">
        <text class="service-icon">🔧</text>
        <text class="service-name">便民报修</text>
      </view>
      <view class="service-item" @click="goToPolicies">
        <text class="service-icon">📋</text>
        <text class="service-name">政策查询</text>
      </view>
      <view class="service-item" @click="goToPoints">
        <text class="service-icon">⭐</text>
        <text class="service-name">积分明细</text>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">最新活动</text>
        <text class="more" @click="goToActivities">查看全部 ›</text>
      </view>
      <view v-if="loading" class="loading">加载中...</view>
      <view v-else>
        <view v-for="a in activities.slice(0, 3)" :key="a.id" class="card activity-card" @click="goToActivities">
          <view class="card-title">{{ a.title }}</view>
          <view class="card-meta">
            <text>📅 {{ a.activityDate }}</text>
            <text class="tag tag-blue">{{ a.status === 'PLANNED' ? '报名中' : a.status }}</text>
          </view>
        </view>
        <text v-if="!activities.length" class="empty">暂无活动</text>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/services/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getResidentActivities, getMyPoints } from '../../../src/api/resident'

const { statusBarPadding } = useStatusBar()

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

function goToActivities() { uni.navigateTo({ url: '/pages/resident/activities/index' }) }
function goToRepair() { uni.navigateTo({ url: '/pages/resident/repairs/index' }) }
function goToPolicies() { uni.navigateTo({ url: '/pages/resident/policies/index' }) }
function goToPoints() { uni.navigateTo({ url: '/pages/resident/points/index' }) }

onLoad(loadData)
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.points-card {
  display: flex; align-items: center; justify-content: space-between;
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%);
  border-radius: 24rpx; padding: 32rpx 40rpx; margin-bottom: 32rpx;
}
.points-info { display: flex; flex-direction: column; }
.points-label { font-size: 24rpx; color: rgba(255,255,255,0.8); }
.points-value { font-size: 56rpx; font-weight: 700; color: #fff; }
.points-arrow { font-size: 56rpx; color: rgba(255,255,255,0.8); }
.service-grid { display: flex; flex-wrap: wrap; gap: 24rpx; margin-bottom: 40rpx; }
.service-item {
  display: flex; flex-direction: column; align-items: center; gap: 12rpx;
  padding: 32rpx 16rpx; background: #fff; border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04); width: calc((100% - 72rpx) / 4);
  box-sizing: border-box;
}
.service-icon { font-size: 56rpx; }
.service-name { font-size: 24rpx; color: #374151; }
.section { margin-top: 8rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24rpx; }
.section-title { font-size: 30rpx; font-weight: 600; }
.more { font-size: 24rpx; color: #722ed1; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; }
.card-meta { display: flex; justify-content: space-between; align-items: center; font-size: 24rpx; color: #6b7280; }
.tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; }
.tag-blue { background: #e6f7ff; color: #1890ff; }
.loading { text-align: center; padding: 40rpx; color: #9ca3af; }
.empty { text-align: center; padding: 40rpx; color: #9ca3af; font-size: 26rpx; display: block; }
</style>
