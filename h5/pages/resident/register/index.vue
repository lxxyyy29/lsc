<template>
  <view class="register-page">
    <view class="header">
      <text class="back-btn" @click="goBack">← 返回</text>
      <text class="header-title">注册账号</text>
    </view>

    <view class="form">
      <view class="form-item">
        <text class="label">账号</text>
        <input v-model="form.account" placeholder="4-20位账号" />
      </view>
      <view class="form-item">
        <text class="label">密码</text>
        <input v-model="form.password" type="password" placeholder="6-20位密码" />
      </view>
      <view class="form-item">
        <text class="label">确认密码</text>
        <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
      </view>
      <view class="form-item">
        <text class="label">真实姓名</text>
        <input v-model="form.realName" placeholder="请输入真实姓名" />
      </view>
      <view class="form-item">
        <text class="label">联系电话</text>
        <input v-model="form.phone" placeholder="选填" />
      </view>

      <button class="btn-submit" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注 册' }}
      </button>
      <text v-if="error" class="error">{{ error }}</text>
      <text v-if="success" class="success">{{ success }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { register } from '../../../src/api/resident'

const loading = ref(false)
const error = ref('')
const success = ref('')
const form = reactive({ account: '', password: '', confirmPassword: '', realName: '', phone: '' })

async function handleRegister() {
  if (!form.account || !form.password || !form.confirmPassword || !form.realName) {
    error.value = '请填写完整信息'
    return
  }
  if (form.account.length < 4) { error.value = '账号至少4位'; return }
  if (form.password.length < 6) { error.value = '密码至少6位'; return }
  if (form.password !== form.confirmPassword) { error.value = '两次密码不一致'; return }

  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await register(form.account, form.password, form.realName, form.phone)
    success.value = '注册成功！即将跳转登录...'
    setTimeout(() => uni.navigateBack({ delta: 1 }), 1500)
  } catch (e: any) {
    error.value = e?.message || e || '注册失败'
  } finally {
    loading.value = false
  }
}

function goBack() {
  uni.navigateBack({ delta: 1 })
}
</script>

<style scoped>
.register-page { min-height: 100vh; background: #f5f7fa; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 40rpx 32rpx;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.back-btn { font-size: 32rpx; }
.header-title { font-size: 36rpx; font-weight: 600; }
.form { padding: 48rpx 32rpx; }
.form-item { margin-bottom: 32rpx; }
.form-item .label { display: block; font-size: 26rpx; color: #6b7280; margin-bottom: 12rpx; }
.form-item input {
  width: 100%; padding: 24rpx 28rpx; border: 1px solid #e5e7eb; border-radius: 16rpx;
  font-size: 30rpx; outline: none; background: #fff;
}
.btn-submit {
  width: 100%; padding: 28rpx; background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff; border: none; border-radius: 16rpx; font-size: 32rpx; font-weight: 600; margin-top: 16rpx;
  line-height: 1.4;
}
.btn-submit[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
.success { color: #52c41a; font-size: 26rpx; text-align: center; margin-top: 24rpx; display: block; }
</style>
