<template>
  <view class="page">
    <view class="header">
      <text class="header-title">志愿活动</text>
      <text class="header-sub">查看活动详情、参与报名</text>
    </view>

    <view v-if="isLoading" class="tip">加载中...</view>
    <view v-else-if="loadError" class="tip tip-error" @click="load">加载失败，点击重试</view>
    <view v-else-if="!activities.length" class="tip">暂无志愿活动</view>

    <view v-else class="list">
      <view v-for="item in activities" :key="item.id" class="card" @click="openDetail(item)">
        <view class="card-head">
          <text class="card-title">{{ item.title }}</text>
          <text v-if="item.signedUp" class="badge badge--ok">已报名</text>
        </view>
        <text class="card-desc">{{ item.description || '暂无活动说明' }}</text>
        <text class="card-meta">活动时间：{{ formatDate(item.activityDate) }}</text>
        <text class="card-meta">
          报名情况：{{ item.attendedCount ?? 0 }} / {{ item.maxParticipants ?? '不限' }}
        </text>
        <text class="card-more">查看详情 ›</text>
      </view>
    </view>

    <!-- 活动详情（缺陷 BUG-15：原来点开看不到详情） -->
    <view v-if="detail" class="sheet-mask" @click.self="detail = null">
      <view class="sheet">
        <view class="sheet-head">
          <text class="sheet-title">{{ detail.title }}</text>
          <text class="sheet-close" @click="detail = null">×</text>
        </view>
        <view class="sheet-body">
          <view class="row"><text class="row-label">活动时间</text><text class="row-value">{{ formatDate(detail.activityDate) }}</text></view>
          <view class="row"><text class="row-label">报名情况</text><text class="row-value">{{ detail.attendedCount ?? 0 }} / {{ detail.maxParticipants ?? '不限' }}</text></view>
          <view class="row"><text class="row-label">活动状态</text><text class="row-value">{{ statusText(detail.status) }}</text></view>
          <view class="row"><text class="row-label">发布人</text><text class="row-value">{{ detail.creatorName || '-' }}</text></view>
          <view class="row"><text class="row-label">我的状态</text><text class="row-value">{{ detail.checkedIn ? '已签到' : (detail.signedUp ? '已报名' : '未报名') }}</text></view>
          <view class="detail-block">
            <text class="row-label">活动详情</text>
            <text class="detail-text">{{ detail.description || '暂无活动说明' }}</text>
          </view>
        </view>

        <view class="sheet-actions">
          <button v-if="!detail.signedUp" class="btn btn-primary" :disabled="submitting" @click="signup(detail)">
            {{ submitting ? '提交中...' : '我要报名' }}
          </button>
          <button v-else class="btn btn-ghost" :disabled="submitting" @click="cancelSignup(detail)">
            {{ submitting ? '提交中...' : '取消报名' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getActivities,
  signupActivity,
  cancelActivitySignup,
  getResidentSession,
  type ActivityItem
} from '../../src/api/resident'

const activities = ref<ActivityItem[]>([])
const isLoading = ref(true)
const loadError = ref(false)
const submitting = ref(false)
const detail = ref<ActivityItem | null>(null)

function formatDate(value?: string | null) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

function statusText(status?: string | null) {
  const map: Record<string, string> = {
    ACTIVE: '进行中',
    ONGOING: '进行中',
    FINISHED: '已结束',
    CLOSED: '已结束',
    CANCELED: '已取消'
  }
  return map[String(status || '')] || (status || '-')
}

async function load() {
  isLoading.value = true
  loadError.value = false
  try {
    activities.value = await getActivities()
  } catch (e) {
    loadError.value = true
    activities.value = []
  } finally {
    isLoading.value = false
  }
}

/** 详情数据随列表返回，直接展示；报名后回写列表与当前详情，避免多请求 */
function openDetail(item: ActivityItem) {
  detail.value = item
}

async function signup(item: ActivityItem) {
  if (submitting.value) return
  submitting.value = true
  try {
    await signupActivity(item.id)
    item.signedUp = true
    item.attendedCount = (item.attendedCount ?? 0) + 1
    uni.showToast({ title: '报名成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '报名失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

async function cancelSignup(item: ActivityItem) {
  if (submitting.value) return
  submitting.value = true
  try {
    await cancelActivitySignup(item.id)
    item.signedUp = false
    item.attendedCount = Math.max(0, (item.attendedCount ?? 1) - 1)
    uni.showToast({ title: '已取消报名', icon: 'none' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '取消失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad(() => {
  // 未登录则回居民登录页，避免直接打接口拿 401
  if (!getResidentSession()) {
    uni.redirectTo({ url: '/pages/resident/login/index' })
    return
  }
  void load()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 120rpx;
  background: #f5f7fa;
}

.header {
  padding: 40rpx 8rpx 28rpx;
  background: linear-gradient(135deg, #3898fd, #2272d9);
  border-radius: 20rpx;
  margin-bottom: 20rpx;
}

.header-title {
  display: block;
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.header-sub {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.tip {
  padding: 100rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #9ca3af;
}

.tip-error {
  color: #ef4444;
}

.list {
  display: grid;
  gap: 16rpx;
}

.card {
  display: grid;
  gap: 8rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 2rpx 12rpx rgba(15, 23, 42, 0.06);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.card-title {
  flex: 1;
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}

.badge {
  flex-shrink: 0;
  font-size: 22rpx;
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
}

.badge--ok {
  color: #059669;
  background: rgba(5, 150, 105, 0.12);
}

.card-desc {
  font-size: 26rpx;
  color: #4b5563;
  line-height: 1.6;
}

.card-meta {
  font-size: 24rpx;
  color: #6b7280;
}

.card-more {
  font-size: 24rpx;
  color: #1890ff;
}

.sheet-mask {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 999;
}

.sheet {
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 28rpx 28rpx 40rpx;
  border-radius: 24rpx 24rpx 0 0;
  background: #fff;
}

.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #1f2937;
}

.sheet-close {
  font-size: 40rpx;
  line-height: 1;
  color: #9ca3af;
  padding: 0 8rpx;
}

.sheet-body {
  display: grid;
  gap: 14rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  font-size: 26rpx;
}

.row-label {
  color: #6b7280;
  flex-shrink: 0;
}

.row-value {
  color: #1f2937;
  text-align: right;
  word-break: break-all;
}

.detail-block {
  display: grid;
  gap: 8rpx;
  padding-top: 12rpx;
  border-top: 1px solid #f1f5f9;
}

.detail-text {
  font-size: 26rpx;
  color: #374151;
  line-height: 1.7;
}

.sheet-actions {
  margin-top: 24rpx;
}

.btn {
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 14rpx;
  font-size: 30rpx;
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #3898fd, #2272d9);
  color: #fff;
}

.btn-ghost {
  background: #fff;
  color: #6b7280;
  border: 1px solid #d1d5db;
}
</style>
