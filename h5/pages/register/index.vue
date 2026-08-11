<template>
  <view class="register-page">
    <view class="register-header">
      <text class="logo">🏘️</text>
      <text class="title">注册账号</text>
      <text class="subtitle">选择您的身份完成注册</text>
    </view>

    <view class="register-form">
      <!-- 身份选择 -->
      <view class="form-item">
        <text class="label">身份</text>
        <view class="role-group">
          <view
            class="role-option"
            :class="{ active: form.role === 'RESIDENT' }"
            @click="form.role = 'RESIDENT'"
          >
            <text class="role-icon">📱</text>
            <text class="role-name">居民</text>
            <text class="role-desc">随手拍、社区服务，注册即开通</text>
          </view>
          <view
            class="role-option"
            :class="{ active: form.role === 'GRID_WORKER' }"
            @click="form.role = 'GRID_WORKER'"
          >
            <text class="role-icon">👷</text>
            <text class="role-name">网格员</text>
            <text class="role-desc">工单处理、巡查上报，需管理员审批</text>
          </view>
        </view>
      </view>

      <view class="form-item">
        <text class="label">账号</text>
        <input v-model.trim="form.account" placeholder="请输入账号（4-20位）" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <text class="label">密码</text>
        <input v-model="form.password" type="password" placeholder="请输入密码（6-20位）" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <text class="label">确认密码</text>
        <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input v-model.trim="form.realName" placeholder="请输入真实姓名" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <text class="label">手机号（登录凭据）</text>
        <input v-model.trim="form.phone" type="tel" maxlength="11" placeholder="请输入手机号" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>

      <text v-if="form.role === 'GRID_WORKER'" class="approve-hint">提交后需管理员审批，通过后方可登录</text>
      <text v-if="errorMessage" class="error">{{ errorMessage }}</text>

      <button class="btn-register" :disabled="submitting" @click="handleSubmit" style="color:#ffffff;">
        {{ submitting ? '提交中...' : '注 册' }}
      </button>

      <view class="footer-links">
        <text class="link" @click="goLogin">已有账号？去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { HttpResponseError } from '../../src/api/http'
import { registerH5 } from '../../src/api/auth'
import { register as residentRegister } from '../../src/api/resident'

type RoleType = 'RESIDENT' | 'GRID_WORKER'

const form = reactive({
  role: 'RESIDENT' as RoleType,
  account: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: ''
})
const submitting = ref(false)
const errorMessage = ref('')
const MOBILE_PHONE_PATTERN = /^1[3-9]\d{9}$/

function goLogin() {
  uni.reLaunch({ url: '/pages/role-select/index' })
}

async function handleSubmit() {
  if (!form.account || !form.password || !form.confirmPassword || !form.realName || !form.phone) {
    errorMessage.value = '请填写完整信息'
    return
  }
  if (form.account.length < 4) {
    errorMessage.value = '账号至少4位'
    return
  }
  if (form.password.length < 6) {
    errorMessage.value = '密码至少6位'
    return
  }
  if (form.password !== form.confirmPassword) {
    errorMessage.value = '两次密码不一致'
    return
  }
  if (!MOBILE_PHONE_PATTERN.test(form.phone)) {
    errorMessage.value = '请输入正确的手机号'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    if (form.role === 'RESIDENT') {
      // 居民：即时开通，直接写入数据库
      await residentRegister(form.account, form.password, form.realName, form.phone)
      uni.showToast({ title: '注册成功！', icon: 'success', duration: 2000 })
      setTimeout(() => goLogin(), 1200)
    } else {
      // 网格员：提交申请，待管理员审批通过后才可登录
      await registerH5({
        account: form.account,
        password: form.password,
        realName: form.realName,
        phone: form.phone
      })
      uni.showToast({ title: '提交成功，等待审批', icon: 'success', duration: 3000 })
      setTimeout(() => goLogin(), 2500)
    }
  } catch (e: any) {
    console.error('注册失败:', e)
    errorMessage.value = e instanceof HttpResponseError ? (e.message || '注册失败') : '注册失败，请稍后重试'
    uni.showToast({ title: errorMessage.value, icon: 'error', duration: 3000 })
  } finally {
    submitting.value = false
  }
}
</script>

<style>
/* 统一注册页 全局样式 */
.register-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 48rpx;
  box-sizing: border-box;
}
.register-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 48rpx;
}
.register-header .logo { font-size: 100rpx; margin-bottom: 24rpx; }
.register-header .title { font-size: 40rpx; font-weight: 600; margin-bottom: 12rpx; color: #fff; }
.register-header .subtitle { font-size: 26rpx; opacity: 0.85; color: #fff; }
.register-form {
  width: 100%;
  max-width: 720rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 48rpx 48rpx 56rpx;
  box-shadow: 0 16rpx 64rpx rgba(0,0,0,0.1);
}
.form-item { margin-bottom: 24rpx; }
.form-item .label { display: block; font-size: 26rpx; color: #374151; margin-bottom: 12rpx; font-weight: 500; }
.form-item input {
  width: 100%;
  height: 88rpx;
  padding: 0 32rpx;
  border: 1px solid #d1d5db;
  border-radius: 12rpx;
  font-size: 30rpx;
  color: #111827;
  background: #fff;
  box-sizing: border-box;
}
.role-group { display: flex; gap: 20rpx; }
.role-option {
  flex: 1;
  border: 2rpx solid #e5e7eb;
  border-radius: 16rpx;
  padding: 24rpx 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
}
.role-option.active {
  border-color: #1890ff;
  background: #e6f4ff;
}
.role-option .role-icon { font-size: 48rpx; margin-bottom: 8rpx; }
.role-option .role-name { font-size: 30rpx; font-weight: 600; color: #111827; margin-bottom: 6rpx; }
.role-option .role-desc { font-size: 20rpx; color: #6b7280; text-align: center; line-height: 1.4; }
.approve-hint {
  display: block;
  font-size: 24rpx;
  color: #fa8c16;
  margin-bottom: 12rpx;
}
.btn-register {
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
  margin-top: 16rpx;
}
.btn-register::after { border: none; }
.btn-register[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 20rpx; display: block; }
.footer-links { text-align: center; margin-top: 32rpx; }
.link { color: #1890ff; font-size: 26rpx; }
</style>
