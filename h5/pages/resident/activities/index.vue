<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">🤝 活动报名</text>
      <text class="header-sub">参与志愿活动，获取志愿服务积分</text>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="a in activities" :key="a.id" class="card">
        <view class="card-title">{{ a.title }}</view>
        <text class="desc">{{ a.description }}</text>
        <view class="card-meta">
          <text>📅 {{ a.activityDate }}</text>
          <text>👥 {{ a.attendedCount || 0 }}/{{ a.maxParticipants || '∞' }}</text>
        </view>
        <view class="card-action">
          <button v-if="!a.signedUp && a.status === 'PLANNED'" @click="signup(a.id)" class="btn-primary">立即报名</button>
          <button v-else-if="a.signedUp" @click="cancelSignup(a.id)" class="btn-default">取消报名</button>
          <text v-else class="tag tag-gray">已结束</text>
        </view>
      </view>
      <text v-if="!activities.length" class="empty">暂无可报名活动</text>
    </view>

    <ResidentTabBar current="/pages/resident/activities/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getResidentActivities, signupActivity, cancelActivitySignup } from '../../../src/api/resident'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'

const { statusBarPadding } = useStatusBar()

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
    uni.showToast({ title: '报名成功！', icon: 'success' })
    loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '报名失败', icon: 'none' })
  }
}

async function cancelSignup(id: number) {
  try {
    await cancelActivitySignup(id)
    uni.showToast({ title: '已取消报名', icon: 'none' })
    loadData()
  } catch (e: any) {
    uni.showToast({ title: e?.message || '取消失败', icon: 'none' })
  }
}

onLoad(loadData)
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.card {
  background: #fff; border-radius: 24rpx; padding: 32rpx; margin-bottom: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.card-title { font-size: 28rpx; font-weight: 600; margin-bottom: 12rpx; }
.desc { font-size: 26rpx; color: #6b7280; margin-bottom: 20rpx; display: block; line-height: 1.5; }
.card-meta { display: flex; justify-content: space-between; font-size: 24rpx; color: #9ca3af; margin-bottom: 24rpx; }
.card-action { display: flex; justify-content: flex-end; }
.btn-primary {
  padding: 16rpx 40rpx; background: #1890ff; color: #fff; border: none;
  border-radius: 12rpx; font-size: 26rpx; line-height: 1.6;
}
.btn-default {
  padding: 16rpx 40rpx; background: #f3f4f6; color: #6b7280; border: none;
  border-radius: 12rpx; font-size: 26rpx; line-height: 1.6;
}
.tag { font-size: 22rpx; padding: 8rpx 20rpx; border-radius: 8rpx; }
.tag-gray { background: #f3f4f6; color: #9ca3af; }
.loading { text-align: center; padding: 80rpx; color: #9ca3af; }
.empty { text-align: center; padding: 80rpx; color: #9ca3af; font-size: 26rpx; display: block; }
</style>
