<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo">🏘️</text>
      <text class="title">东莞杰瑞智慧网格治理平台</text>
      <text class="subtitle">网格员工作端</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <input v-model.trim="form.account" placeholder="请输入账号" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <view class="form-item">
        <input v-model="form.password" type="password" placeholder="请输入密码" placeholder-style="color:#9ca3af;font-size:30rpx;" />
      </view>
      <button class="btn-login" :disabled="submitting" @click="handleLogin" style="color:#ffffff;">
        {{ submitting ? '登录中...' : '登 录' }}
      </button>
      <text v-if="errorMessage" class="error">{{ errorMessage }}</text>

      <view class="footer-links">
        <text class="link" @click="goRegister">注册账号</text>
        <text class="link-divider">|</text>
        <text class="link" @click="goForgotPassword">忘记密码</text>
      </view>
    </view>

    <view class="login-help">
      <text>忘记密码可在线提交重置申请，管理员处理后告知新密码</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { HttpResponseError } from '../../src/api/http'
import { clearH5Session, loginH5 } from '../../src/api/auth'
import { hasMenuPermission } from '../../src/auth/permissions'
import { h5NavigationItems } from '../../src/navigation'
import { consumePendingRedirect, redirectToPath } from '../../src/uni/navigation'

const form = reactive({ account: '', password: '' })
const submitting = ref(false)
const errorMessage = ref('')

// 进入登录页即代表需要重新认证：清理残留会话，防止切换账号后旧用户信息串号
onShow(() => {
  clearH5Session()
})

async function handleLogin() {
  if (!form.account || !form.password) {
    errorMessage.value = '请输入账号和密码'
    return
  }
  errorMessage.value = ''
  submitting.value = true

  try {
    await loginH5({
      account: form.account,
      password: form.password
    })
    const fallback = h5NavigationItems.find((item) => hasMenuPermission(item.permission))?.to ?? '/workbench'
    const target = consumePendingRedirect() || fallback
    redirectToPath(target)
  } catch (error) {
    const msg = error instanceof HttpResponseError ? error.message : ''
    errorMessage.value = msg && /[一-龥]/.test(msg) ? msg : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}

function goForgotPassword() {
  uni.navigateTo({ url: '/pages/password-reset/index' })
}
</script>

<style>
/* 网格员端登录页 全局样式（浅色主题，与居民端一致，规避小程序原生组件兼容问题） */
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 48rpx;
  box-sizing: border-box;
}
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 80rpx;
  color: #fff;
}
.login-header .logo { font-size: 120rpx; margin-bottom: 32rpx; }
.login-header .title { font-size: 48rpx; font-weight: 600; margin-bottom: 16rpx; color: #fff; }
.login-header .subtitle { font-size: 28rpx; opacity: 0.85; color: #fff; }
.login-form {
  width: 100%;
  max-width: 720rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 64rpx 48rpx;
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
.btn-login {
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
.btn-login::after { border: none; }
.btn-login[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
.footer-links { text-align: center; margin-top: 32rpx; display: flex; align-items: center; justify-content: center; }
.link { color: #1890ff; font-size: 26rpx; }
.link-divider { color: #d1d5db; font-size: 24rpx; margin: 0 20rpx; }
.login-help { margin-top: 48rpx; color: rgba(255,255,255,0.7); font-size: 24rpx; text-align: center; }
</style>
