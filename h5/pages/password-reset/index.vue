<template>
  <view class="reset-page">
    <view class="reset-header">
      <text class="title">忘记密码</text>
      <text class="subtitle">提交重置申请，管理员处理后请与其联系获取新密码</text>
    </view>

    <view class="reset-card">
      <view class="form-item">
        <input v-model.trim="form.account" placeholder="请输入账号" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <input v-model.trim="form.phone" type="number" maxlength="11" placeholder="请输入注册时的手机号" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>

      <button class="btn-primary" :disabled="submitting" @click="handleSubmit" style="color:#ffffff;">
        {{ submitting ? '提交中...' : '提交重置申请' }}
      </button>
      <button class="btn-plain" :disabled="submitting" @click="handleQuery">查询申请进度</button>

      <text v-if="errorMessage" class="msg msg-error">{{ errorMessage }}</text>
      <text v-if="successMessage" class="msg msg-success">{{ successMessage }}</text>

      <!-- 进度查询结果 -->
      <view v-if="queryResult" class="query-result">
        <view class="query-row">
          <text class="query-label">申请状态</text>
          <text :class="['query-status', 'status-' + (queryResult.status || '').toLowerCase()]">{{ statusText }}</text>
        </view>
        <view v-if="queryResult.createdAt" class="query-row">
          <text class="query-label">申请时间</text>
          <text class="query-value">{{ formatTime(queryResult.createdAt) }}</text>
        </view>
        <view v-if="queryResult.handledAt" class="query-row">
          <text class="query-label">处理时间</text>
          <text class="query-value">{{ formatTime(queryResult.handledAt) }}</text>
        </view>
        <view v-if="queryResult.status === 'APPROVED'" class="query-tip">
          密码已重置，请联系管理员获取新密码后登录
        </view>
        <view v-if="queryResult.status === 'REJECTED'" class="query-tip">
          申请被驳回{{ queryResult.remark ? '：' + queryResult.remark : '' }}，可重新提交
        </view>
      </view>
    </view>

    <view class="back-link" @click="goBack">返回登录</view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { HttpResponseError } from '../../src/api/http'
import { queryPasswordResetStatus, submitPasswordReset, type PasswordResetStatus } from '../../src/api/auth'

const form = reactive({ account: '', phone: '' })
const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const queryResult = ref<PasswordResetStatus | null>(null)

const statusText = computed(() => {
  switch (queryResult.value?.status) {
    case 'PENDING': return '处理中，请耐心等待'
    case 'APPROVED': return '已重置'
    case 'REJECTED': return '已驳回'
    default: return ''
  }
})

function validate(): boolean {
  errorMessage.value = ''
  successMessage.value = ''
  queryResult.value = null
  if (!form.account) { errorMessage.value = '请输入账号'; return false }
  if (!/^1\d{10}$/.test(form.phone)) { errorMessage.value = '请输入正确的 11 位手机号'; return false }
  return true
}

async function handleSubmit() {
  if (!validate()) return
  submitting.value = true
  try {
    await submitPasswordReset({ account: form.account, phone: form.phone })
    successMessage.value = '申请已提交，管理员处理后会通过您登记的联系方式告知新密码，也可随时查询进度'
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError && error.message ? error.message : '提交失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

async function handleQuery() {
  if (!validate()) return
  submitting.value = true
  try {
    const result = await queryPasswordResetStatus(form.account, form.phone)
    if (!result.found) {
      errorMessage.value = '未找到该账号的重置申请，请先提交申请'
    } else {
      queryResult.value = result
    }
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError && error.message ? error.message : '查询失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function formatTime(value?: string): string {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/login/index' })
  })
}
</script>

<style>
.reset-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 48rpx;
  box-sizing: border-box;
}
.reset-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 60rpx;
  color: #fff;
}
.reset-header .title { font-size: 44rpx; font-weight: 600; margin-bottom: 16rpx; color: #fff; }
.reset-header .subtitle { font-size: 26rpx; opacity: 0.85; color: #fff; text-align: center; line-height: 1.6; }
.reset-card {
  width: 100%;
  max-width: 720rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 56rpx 48rpx;
  box-shadow: 0 16rpx 64rpx rgba(0,0,0,0.1);
}
.form-item { margin-bottom: 32rpx; }
.form-item input {
  width: 100%;
  height: 90rpx;
  padding: 0 32rpx;
  border: 1px solid #d1d5db;
  border-radius: 12rpx;
  font-size: 30rpx;
  color: #111827;
  background: #fff;
  box-sizing: border-box;
}
.btn-primary {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  padding: 0;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #ffffff;
  border: none;
  border-radius: 12rpx;
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 8rpx;
}
.btn-primary::after, .btn-plain::after { border: none; }
.btn-primary[disabled], .btn-plain[disabled] { opacity: 0.6; }
.btn-plain {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  padding: 0;
  background: #fff;
  color: #1890ff;
  border: 1px solid #1890ff;
  border-radius: 12rpx;
  font-size: 30rpx;
  margin-top: 24rpx;
}
.msg { display: block; font-size: 26rpx; text-align: center; margin-top: 24rpx; line-height: 1.6; }
.msg-error { color: #ff4d4f; }
.msg-success { color: #52c41a; }
.query-result { margin-top: 32rpx; background: #f8fafc; border-radius: 12rpx; padding: 24rpx; }
.query-row { display: flex; justify-content: space-between; padding: 10rpx 0; }
.query-label { color: #6b7280; font-size: 26rpx; }
.query-value { color: #111827; font-size: 26rpx; }
.query-status { font-size: 26rpx; font-weight: 600; }
.status-pending { color: #fa8c16; }
.status-approved { color: #52c41a; }
.status-rejected { color: #ff4d4f; }
.query-tip { margin-top: 16rpx; font-size: 25rpx; color: #475569; line-height: 1.6; }
.back-link { margin-top: 48rpx; color: rgba(255,255,255,0.9); font-size: 28rpx; }
</style>
