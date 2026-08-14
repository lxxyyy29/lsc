<template>
  <view class="page">
    <view class="page-glow"></view>

    <!-- 加载中 -->
    <view v-if="loading" class="empty-card">
      <text class="empty-title">加载中...</text>
    </view>

    <!-- 指令详情 -->
    <view v-else-if="detail" class="content">
      <!-- 指令头卡 -->
      <view class="head-card">
        <view class="head-top">
          <view class="type-badge" :class="`type-badge--${detail.type}`">
            <text class="type-badge-text">{{ detail.type_name || detail.type }}</text>
          </view>
          <view class="status-badge" :class="`status-badge--${dispatchStatus}`">
            <text class="status-badge-text">{{ statusName(dispatchStatus) }}</text>
          </view>
        </view>
        <text class="head-title">{{ detail.title }}</text>
        <view class="head-meta">
          <text class="meta-item">编号：{{ detail.dispatch_no }}</text>
          <text class="meta-item">级别：{{ detail.level_name || detail.level }}</text>
          <text class="meta-item">目标：{{ detail.grid_name || '全域' }}</text>
          <text class="meta-item">发起：{{ detail.creator_name || '指挥中心' }}</text>
          <text class="meta-item">时间：{{ formatTime(detail.dispatch_time) }}</text>
          <text v-if="detail.completed_at" class="meta-item">完成：{{ formatTime(detail.completed_at) }}</text>
        </view>
      </view>

      <!-- 指令内容 -->
      <view class="block-card">
        <text class="block-title">📋 指令内容</text>
        <text class="block-content">{{ detail.content }}</text>
      </view>

      <!-- 视频会议 -->
      <view v-if="detail.meeting_url" class="block-card">
        <text class="block-title">🎥 视频会议</text>
        <text class="meeting-link" @click="copyMeeting">{{ detail.meeting_url }}</text>
        <text class="meeting-hint">点击复制会议链接，外接会议系统后自动拉起</text>
      </view>

      <!-- 我的回执 -->
      <view class="block-card">
        <text class="block-title">我的反馈</text>
        <view v-if="myStatus" class="my-receipt">
          <view class="my-receipt-status" :class="`rstatus--${myStatus}`">
            <text class="my-receipt-status-text">{{ receiptName(myStatus) }}</text>
          </view>
          <text v-if="myFeedback" class="my-receipt-feedback">已反馈：{{ myFeedback }}</text>
          <text class="my-receipt-time">
            {{ myReceivedAt ? `接收于 ${formatTime(myReceivedAt)}` : '尚未接收' }}
          </text>
        </view>
        <view v-else class="my-receipt">
          <text class="my-receipt-feedback">您尚未反馈该指令</text>
        </view>

        <!-- 反馈操作 -->
        <view v-if="dispatchStatus !== 'COMPLETED'" class="feedback-area">
          <text class="feedback-label">选择反馈状态</text>
          <view class="feedback-options">
            <view
              class="feedback-option"
              :class="{ 'feedback-option--active': feedbackForm.status === 'RESPONDING' }"
              @click="feedbackForm.status = 'RESPONDING'"
            >
              <text class="feedback-option-text">响应中</text>
              <text class="feedback-option-sub">正在赶往/处置</text>
            </view>
            <view
              class="feedback-option"
              :class="{ 'feedback-option--active': feedbackForm.status === 'COMPLETED' }"
              @click="feedbackForm.status = 'COMPLETED'"
            >
              <text class="feedback-option-text">已完成</text>
              <text class="feedback-option-sub">处置完毕</text>
            </view>
          </view>
          <textarea
            v-model="feedbackForm.feedback"
            class="feedback-input"
            placeholder="补充反馈内容（可选），如：已到达现场，正在组织人员转移…"
            placeholder-class="feedback-placeholder"
            maxlength="500"
          />
          <button class="submit-btn" :class="{ 'submit-btn--disabled': submitting }" :disabled="submitting" @click="submitFeedback">
            <text class="submit-btn-text">{{ submitting ? '提交中…' : '提交反馈' }}</text>
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
  getEmergencyDispatchDetail,
  feedbackEmergencyReceipt,
  type EmergencyDispatch,
} from '../../src/api/emergency'

const detail = ref<EmergencyDispatch | null>(null)
const loading = ref(true)
const submitting = ref(false)
const feedbackForm = reactive({ status: 'RESPONDING', feedback: '' })

const myStatus = computed(() => {
  const r: any = detail.value?.myReceipt
  return r?.status || detail.value?.my_status || ''
})
const myFeedback = computed(() => {
  const r: any = detail.value?.myReceipt
  return r?.feedback || detail.value?.my_feedback || ''
})
const myReceivedAt = computed(() => (detail.value?.myReceipt as any)?.received_at)
const dispatchStatus = computed(() => detail.value?.status || '')

const statusName = (s: string) =>
  ({ DISPATCHED: '已下达', RESPONDING: '响应中', COMPLETED: '已完成' } as Record<string, string>)[s] || s
const receiptName = (s: string) =>
  ({ PENDING: '未接收', RECEIVED: '已接收', RESPONDING: '响应中', COMPLETED: '已完成' } as Record<string, string>)[s] || s

function formatTime(t?: string | null) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function load(id: number) {
  loading.value = true
  try {
    detail.value = await getEmergencyDispatchDetail(id)
  } catch (e: any) {
    uni.showToast({ title: e?.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

async function submitFeedback() {
  if (!detail.value) return
  submitting.value = true
  try {
    const res: any = await feedbackEmergencyReceipt(detail.value.id, feedbackForm.status, feedbackForm.feedback.trim() || undefined)
    if (detail.value.myReceipt) {
      detail.value.myReceipt.status = res.status
      detail.value.myReceipt.feedback = res.feedback
    }
    uni.showToast({ title: '反馈成功', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '反馈失败', icon: 'none' })
  } finally {
    submitting.value = false
  }
}

function copyMeeting() {
  if (!detail.value?.meeting_url) return
  uni.setClipboardData({
    data: detail.value.meeting_url,
    success: () => uni.showToast({ title: '已复制会议链接', icon: 'none' }),
  })
}

onLoad((query: any) => {
  const id = Number(query?.id)
  if (id) load(id)
})
onMounted(() => {})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4fb 0%, #f6f9fc 100%);
  padding: 20rpx 24rpx 60rpx;
  position: relative;
  overflow: hidden;
}

.page-glow {
  position: absolute;
  top: -160rpx;
  right: -120rpx;
  width: 420rpx;
  height: 420rpx;
  background: radial-gradient(circle, rgba(220, 38, 38, 0.14) 0%, rgba(220, 38, 38, 0) 70%);
  border-radius: 50%;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx;
}

.empty-title { font-size: 30rpx; color: #334155; }

.content { display: flex; flex-direction: column; gap: 20rpx; }

.head-card {
  background: linear-gradient(135deg, #dc2626 0%, #ea580c 100%);
  border-radius: 24rpx;
  padding: 30rpx 30rpx 34rpx;
  box-shadow: 0 8rpx 24rpx rgba(220, 38, 38, 0.28);
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.head-top { display: flex; justify-content: space-between; align-items: center; }

.type-badge, .status-badge {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.22);
}

.type-badge-text, .status-badge-text {
  font-size: 22rpx;
  color: #fff;
  font-weight: 600;
}

.head-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  line-height: 1.4;
}

.head-meta { display: flex; flex-direction: column; gap: 6rpx; }

.meta-item {
  font-size: 23rpx;
  color: rgba(255, 255, 255, 0.92);
}

.block-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.block-title { font-size: 28rpx; font-weight: 700; color: #0f172a; }

.block-content {
  font-size: 26rpx;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
}

.meeting-link {
  font-size: 25rpx;
  color: #0284c7;
  word-break: break-all;
  text-decoration: underline;
}

.meeting-hint { font-size: 22rpx; color: #94a3b8; }

.my-receipt { display: flex; flex-direction: column; gap: 8rpx; }

.my-receipt-status {
  align-self: flex-start;
  padding: 6rpx 22rpx;
  border-radius: 999rpx;
}

.rstatus--RECEIVED { background: #fef3c7; }
.rstatus--RESPONDING { background: #e0f2fe; }
.rstatus--COMPLETED { background: #dcfce7; }

.my-receipt-status-text { font-size: 24rpx; font-weight: 600; }

.rstatus--RECEIVED .my-receipt-status-text { color: #b45309; }
.rstatus--RESPONDING .my-receipt-status-text { color: #0369a1; }
.rstatus--COMPLETED .my-receipt-status-text { color: #15803d; }

.my-receipt-feedback { font-size: 24rpx; color: #475569; }

.my-receipt-time { font-size: 22rpx; color: #94a3b8; }

.feedback-area {
  margin-top: 6rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.feedback-label { font-size: 24rpx; color: #64748b; }

.feedback-options { display: flex; gap: 16rpx; }

.feedback-option {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4rpx;
  padding: 18rpx 0;
  border-radius: 16rpx;
  border: 2rpx solid #e2e8f0;
  background: #f8fafc;
}

.feedback-option--active {
  border-color: #0284c7;
  background: #eff6ff;
}

.feedback-option-text { font-size: 26rpx; font-weight: 600; color: #334155; }
.feedback-option--active .feedback-option-text { color: #0369a1; }
.feedback-option-sub { font-size: 20rpx; color: #94a3b8; }

.feedback-input {
  width: 100%;
  min-height: 140rpx;
  padding: 18rpx 20rpx;
  border-radius: 16rpx;
  border: 1rpx solid #e2e8f0;
  background: #f8fafc;
  font-size: 25rpx;
  box-sizing: border-box;
}

.feedback-placeholder { color: #94a3b8; }

.submit-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border-radius: 16rpx;
  padding: 22rpx 0;
  display: flex;
  justify-content: center;
  border: none;
}

.submit-btn--disabled { opacity: 0.6; }

.submit-btn-text { font-size: 28rpx; color: #fff; font-weight: 600; }
</style>
