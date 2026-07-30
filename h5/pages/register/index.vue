<template>
  <view class="register-page">
    <view class="register-overlay overlay-hex" :style="hexOverlayStyle"></view>
    <view class="register-overlay overlay-glow overlay-glow--top"></view>

    <view class="register-shell">
      <view class="brand-block">
        <image class="brand-shield" :src="loginShieldUrl" mode="widthFix" aria-hidden="true" />
        <text class="brand-title">居里智能低空巡检</text>
      </view>

      <view class="register-card">
        <view class="field-block">
          <text class="field-label">账号</text>
          <view class="field-shell">
            <AppIcon name="user" class="field-icon" size="24rpx" />
            <input v-model.trim="form.account" class="field-input" placeholder="请输入账号（4-20位）" placeholder-class="field-placeholder" />
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">密码</text>
          <view class="field-shell">
            <AppIcon name="lock" class="field-icon" size="24rpx" />
            <input v-model="form.password" class="field-input" :password="!showPassword" placeholder="请输入密码（6-20位）" placeholder-class="field-placeholder" />
            <AppIcon :name="showPassword ? 'eye-off' : 'eye'" class="field-action" size="24rpx" @click="showPassword = !showPassword" />
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">确认密码</text>
          <view class="field-shell">
            <AppIcon name="lock" class="field-icon" size="24rpx" />
            <input v-model="form.confirmPassword" class="field-input" :password="!showPassword" placeholder="请再次输入密码" placeholder-class="field-placeholder" />
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">真实姓名</text>
          <view class="field-shell">
            <AppIcon name="user" class="field-icon" size="24rpx" />
            <input v-model.trim="form.realName" class="field-input" placeholder="请输入真实姓名" placeholder-class="field-placeholder" />
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">联系电话</text>
          <view class="field-shell">
            <AppIcon name="phone" class="field-icon" size="24rpx" />
            <input v-model.trim="form.phone" class="field-input" type="tel" maxlength="11" placeholder="请输入联系电话" placeholder-class="field-placeholder" />
          </view>
        </view>

        <text v-if="errorMessage" class="register-error">{{ errorMessage }}</text>

        <button class="register-button" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '注册中...' : '注 册' }}
        </button>

        <view class="card-footer">
          <text class="login-link" @click="goLogin">已有账号？去登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import AppIcon from '../../src/components/AppIcon.vue'
import loginHexUrl from '../../src/assets/login-hex.svg'
import loginShieldUrl from '../../src/assets/login-shield.svg'
import { HttpResponseError } from '../../src/api/http'
import { registerH5 } from '../../src/api/auth'

interface RegisterForm {
  account: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
}

const form = reactive<RegisterForm>({
  account: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: ''
})
const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const MOBILE_PHONE_PATTERN = /^1[3-9]\d{9}$/

const hexOverlayStyle = computed(() => ({
  backgroundImage: `url(${loginHexUrl})`
}))

function goLogin() {
  uni.navigateTo({ url: '/pages/login/index' })
}

async function handleSubmit() {
  if (!form.account || !form.password || !form.confirmPassword || !form.realName) {
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
  if (form.phone && !MOBILE_PHONE_PATTERN.test(form.phone)) {
    errorMessage.value = '请输入正确的手机号'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await registerH5({
      account: form.account,
      password: form.password,
      realName: form.realName,
      phone: form.phone || undefined
    })
    uni.showToast({ title: '提交成功，等待审批', icon: 'success', duration: 3000 })
    setTimeout(() => goLogin(), 2500)
  } catch (e: any) {
    console.error('注册失败:', e)
    if (e instanceof HttpResponseError) {
      errorMessage.value = e.message || '注册失败'
    } else {
      errorMessage.value = '注册失败，请稍后重试'
    }
    uni.showToast({ title: errorMessage.value, icon: 'error', duration: 3000 })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40rpx;
  position: relative;
  overflow: hidden;
}
.register-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.overlay-hex {
  background-size: cover;
  background-position: center;
  opacity: 0.1;
}
.overlay-glow {
  background: radial-gradient(ellipse at top, rgba(87, 185, 255, 0.15) 0%, transparent 60%);
}
.register-shell {
  width: 100%;
  max-width: 600rpx;
  position: relative;
  z-index: 1;
}
.brand-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 40rpx;
}
.brand-shield {
  width: 120rpx;
  margin-bottom: 16rpx;
}
.brand-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #eaf5ff;
}
.register-card {
  background: rgba(6, 18, 31, 0.85);
  border: 1px solid rgba(87, 185, 255, 0.2);
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  backdrop-filter: blur(10px);
}
.field-block {
  margin-bottom: 24rpx;
}
.field-label {
  font-size: 24rpx;
  color: #8db0d0;
  margin-bottom: 8rpx;
  display: block;
}
.field-shell {
  display: flex;
  align-items: center;
  background: rgba(13, 25, 40, 0.8);
  border: 1px solid rgba(87, 185, 255, 0.15);
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
}
.field-icon {
  margin-right: 16rpx;
  color: #57b9ff;
}
.field-input {
  flex: 1;
  font-size: 28rpx;
  color: #eaf5ff;
}
.field-action {
  color: #57b9ff;
  margin-left: 16rpx;
}
.register-error {
  color: #ff4d4f;
  font-size: 24rpx;
  text-align: center;
  display: block;
  margin-bottom: 16rpx;
}
.register-button {
  width: 100%;
  padding: 20rpx;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
  border-radius: 12rpx;
  font-size: 30rpx;
  font-weight: 600;
  border: none;
  margin-top: 16rpx;
}
.register-button[disabled] {
  opacity: 0.6;
}
.card-footer {
  text-align: center;
  margin-top: 24rpx;
}
.login-link {
  font-size: 24rpx;
  color: #57b9ff;
}
</style>
