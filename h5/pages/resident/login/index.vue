<template>
  <view class="login-page">
    <view class="login-header">
      <view class="logo">🏘️</view>
      <text class="title">拔蛟窝智慧网格</text>
      <text class="subtitle">居民服务</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <input v-model="form.account" placeholder="请输入账号" />
      </view>
      <view class="form-item">
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </view>
      <button class="btn-login" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登 录' }}
      </button>
      <text v-if="error" class="error">{{ error }}</text>

      <view class="footer-links">
        <text class="link" @click="goRegister">注册账号</text>
      </view>
    </view>

    <view class="quick-login">
      <text>测试账号：yonghu / 123456</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { login } from '../../../src/api/resident'

const loading = ref(false)
const error = ref('')
const form = reactive({ account: 'yonghu', password: '123456' })

async function handleLogin() {
  if (!form.account || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await login(form.account, form.password)
    uni.reLaunch({ url: '/pages/resident/report/index' })
  } catch (e: any) {
    error.value = e?.message || e || '登录失败'
  } finally {
    loading.value = false
  }
}

function goRegister() {
  uni.navigateTo({ url: '/pages/resident/register/index' })
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 48rpx;
}
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 80rpx;
  color: #fff;
}
.logo { font-size: 120rpx; margin-bottom: 32rpx; }
.title { font-size: 48rpx; font-weight: 600; margin-bottom: 16rpx; }
.subtitle { font-size: 28rpx; opacity: 0.8; }
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
  padding: 28rpx 32rpx;
  border: 1px solid #e5e7eb;
  border-radius: 16rpx;
  font-size: 30rpx;
  outline: none;
}
.btn-login {
  width: 100%;
  padding: 28rpx;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  margin-top: 16rpx;
  line-height: 1.4;
}
.btn-login[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
.footer-links { text-align: center; margin-top: 32rpx; }
.link { color: #1890ff; font-size: 26rpx; }
.quick-login { margin-top: 48rpx; color: rgba(255,255,255,0.7); font-size: 24rpx; text-align: center; }
</style>
