<template>
  <view class="page" :style="{ paddingTop: statusBarPadding }">
    <ResidentBackBar />
    <view class="header">
      <text class="header-title">⭐ 志愿服务积分</text>
      <text class="header-sub">参与志愿活动，积累服务积分</text>
    </view>

    <view class="points-overview">
      <view class="points-item">
        <text class="points-num">{{ points.totalPoints || 0 }}</text>
        <text class="points-label">累计积分</text>
      </view>
      <view class="points-item">
        <text class="points-num">{{ points.availablePoints || 0 }}</text>
        <text class="points-label">可用积分</text>
      </view>
    </view>

    <view class="section-title">积分明细</view>
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="log in points.logs || []" :key="log.id" class="card log-card">
        <view class="log-left">
          <text class="log-reason">{{ log.reason || '积分变动' }}</text>
          <text class="log-time">{{ log.createdAt }}</text>
        </view>
        <text :class="['log-points', log.points > 0 ? 'positive' : 'negative']">
          {{ log.points > 0 ? '+' : '' }}{{ log.points }}
        </text>
      </view>
      <text v-if="!points.logs || !points.logs.length" class="empty">暂无积分记录</text>
    </view>

    <view class="tips">
      <text class="tips-title">💡 如何获取积分？</text>
      <view class="tips-list">
        <text class="tips-item">· 报名志愿活动并在活动期内签到：+20 积分/次</text>
        <text class="tips-item">· 签到入口：便民服务 → 活动报名</text>
      </view>
    </view>

    <ResidentTabBar current="/pages/resident/points/index" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ResidentTabBar from '../../../src/components/ResidentTabBar.vue'
import { getMyPoints } from '../../../src/api/resident'
import { useStatusBar } from '../../../src/utils/useStatusBar'
import ResidentBackBar from '../../../src/components/ResidentBackBar.vue'

const { statusBarPadding } = useStatusBar()

const loading = ref(false)
const points = ref<any>({})

async function loadData() {
  loading.value = true
  try {
    // 后端返回 { account: { totalPoints, availablePoints }, logs }，展平后供模板使用
    const res: any = await getMyPoints()
    points.value = { ...(res?.account || {}), logs: res?.logs || [] }
  } catch (e) {
    console.error('加载失败:', e)
  } finally {
    loading.value = false
  }
}

onLoad(loadData)
</script>

<style scoped>
.page { padding: 32rpx; padding-bottom: 160rpx; background: #f5f7fa; min-height: 100vh; }
.header {
  background: linear-gradient(135deg, #faad14 0%, #d48806 100%);
  border-radius: 24rpx; padding: 40rpx; color: #fff; margin-bottom: 32rpx;
}
.header-title { font-size: 40rpx; font-weight: 600; display: block; margin-bottom: 8rpx; }
.header-sub { font-size: 26rpx; opacity: 0.8; }
.points-overview { display: flex; gap: 24rpx; margin-bottom: 40rpx; }
.points-item {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  background: #fff; border-radius: 24rpx; padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.points-num { font-size: 56rpx; font-weight: 700; color: #faad14; }
.points-label { font-size: 24rpx; color: #9ca3af; margin-top: 8rpx; }
.section-title { font-size: 28rpx; font-weight: 600; margin-bottom: 24rpx; color: #374151; }
.log-card {
  display: flex; justify-content: space-between; align-items: center;
  background: #fff; border-radius: 24rpx; padding: 28rpx 32rpx; margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.04);
}
.log-left { display: flex; flex-direction: column; }
.log-reason { font-size: 28rpx; color: #374151; }
.log-time { font-size: 22rpx; color: #9ca3af; margin-top: 8rpx; }
.log-points { font-size: 36rpx; font-weight: 700; }
.log-points.positive { color: #52c41a; }
.log-points.negative { color: #ff4d4f; }
.loading { text-align: center; padding: 40rpx; color: #9ca3af; }
.empty { text-align: center; padding: 40rpx; color: #9ca3af; font-size: 26rpx; display: block; }
.tips {
  margin-top: 40rpx; background: #fffbe6; border-radius: 24rpx; padding: 32rpx;
  border: 1px solid #ffe58f;
}
.tips-title { font-size: 28rpx; font-weight: 600; color: #d48806; display: block; margin-bottom: 20rpx; }
.tips-list { display: flex; flex-direction: column; gap: 12rpx; }
.tips-item { font-size: 26rpx; color: #ad6800; line-height: 1.6; }
</style>
