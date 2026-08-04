<template>
  <view class="login-page">
    <view class="login-overlay overlay-hex" :style="hexOverlayStyle"></view>
    <view class="login-overlay overlay-glow overlay-glow--top"></view>
    <view class="login-overlay overlay-glow overlay-glow--bottom"></view>

    <view class="login-shell">
      <view class="brand-block">
        <image class="brand-shield" :src="loginShieldUrl" mode="widthFix" aria-hidden="true" />
        <text class="brand-title">居里智能低空巡检</text>
      </view>

      <view class="login-card">
        <view class="field-block">
          <text class="field-label">账号</text>
          <view class="field-shell">
            <AppIcon name="user" class="field-icon" size="24rpx" />
            <input
              v-model.trim="form.account"
              class="field-input"
              placeholder="请输入账号"
              placeholder-class="field-placeholder"
            />
          </view>
        </view>

        <view class="field-block">
          <text class="field-label">密码</text>
          <view class="field-shell">
            <AppIcon name="lock" class="field-icon" size="24rpx" />
            <input
              v-model="form.password"
              class="field-input"
              :password="!showPassword"
              placeholder="请输入密码"
              placeholder-class="field-placeholder"
            />
            <AppIcon :name="showPassword ? 'eye-off' : 'eye'" class="field-action" size="24rpx" @click="showPassword = !showPassword" />
          </view>
        </view>

        <text v-if="errorMessage" class="login-error">{{ errorMessage }}</text>

        <button class="login-button" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '登录中...' : '登录' }}
        </button>

        <view class="card-footer">
          <label class="remember-row">
            <checkbox :checked="rememberAccount" color="#49b8ff" style="transform:scale(0.68)" @click="rememberAccount = !rememberAccount" />
            <text>记住账号</text>
          </label>
          <text class="register-link" @click="goRegister">注册账号</text>
        </view>
      </view>

      <view class="login-help">
        <text>忘记密码请联系管理员重置账号信息</text>
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
import { loginH5 } from '../../src/api/auth'
import { hasMenuPermission } from '../../src/auth/permissions'
import { h5NavigationItems } from '../../src/navigation'
import { consumePendingRedirect, redirectToPath, toPageUrl } from '../../src/uni/navigation'

interface LoginForm {
  account: string
  password: string
  captcha: string
}

const form = reactive<LoginForm>({
  account: '',
  password: '',
  captcha: ''
})
const submitting = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const rememberAccount = ref(false)
const fallbackRedirect = computed(() =>
  h5NavigationItems.find((item: (typeof h5NavigationItems)[number]) => hasMenuPermission(item.permission))?.to ?? '/workbench'
)
const hexOverlayStyle = computed(() => ({
  backgroundImage: `url(${loginHexUrl})`
}))

function goRegister() {
  uni.navigateTo({ url: '/pages/register/index' })
}

async function handleSubmit() {
  errorMessage.value = ''
  submitting.value = true

  try {
    await loginH5({
      account: form.account,
      password: form.password,
      captcha: form.captcha
    })
    const target = consumePendingRedirect() || fallbackRedirect.value
    redirectToPath(target)
  } catch (error) {
    const msg = error instanceof HttpResponseError ? error.message : ''
    errorMessage.value = msg && /[\u4e00-\u9fa5]/.test(msg) ? msg : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 0%, rgba(0, 95, 151, 0.18), transparent 44%),
    radial-gradient(circle at 0% 100%, rgba(0, 218, 248, 0.08), transparent 38%),
    #050b14;
}

.login-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.overlay-hex {
  opacity: 0.04;
  background-size: 30px 52px;
}

.overlay-glow {
  filter: blur(100rpx);
}

.overlay-glow--top {
  top: -180rpx;
  left: 50%;
  width: 480rpx;
  height: 480rpx;
  margin-left: -240rpx;
  background: rgba(0, 95, 151, 0.26);
}

.overlay-glow--bottom {
  left: -120rpx;
  bottom: -160rpx;
  width: 420rpx;
  height: 420rpx;
  border-radius: 50%;
  background: rgba(0, 218, 248, 0.08);
}

.login-shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  align-content: center;
  justify-items: center;
  padding: 56rpx 44rpx 160rpx;
}

.brand-block {
  display: grid;
  justify-items: center;
  gap: 14rpx;
  margin-top: 8rpx;
  margin-bottom: 48rpx;
}

.brand-shield {
  width: 112rpx;
  filter: drop-shadow(0 12rpx 24rpx rgba(137, 236, 255, 0.28));
}

.brand-title {
  color: #f8fbff;
  font-size: 50rpx;
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: 0.5rpx;
  text-shadow: 0 6rpx 16rpx rgba(0, 0, 0, 0.3);
}

.login-card {
  width: 100%;
  max-width: 598rpx;
  min-height: 538rpx;
  padding: 34rpx 32rpx 24rpx;
  border-radius: 12rpx;
  border: 1px solid rgba(183, 218, 244, 0.18);
  background: rgba(26, 38, 53, 0.84);
  backdrop-filter: blur(16rpx);
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.04);
  display: grid;
  align-content: start;
  gap: 18rpx;
}

.field-block {
  display: grid;
  gap: 8rpx;
}

.field-label,
.login-help,
.remember-row,
.fingerprint-text,
.field-placeholder {
  color: rgba(177, 188, 206, 0.88);
  font-size: 24rpx;
}

.field-shell {
  height: 78rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 0 18rpx;
  border-radius: 4rpx;
  border: 1px solid rgba(157, 188, 214, 0.16);
  background: rgba(7, 17, 29, 0.96);
}

.field-icon,
.field-action {
  color: #5f7187;
  flex-shrink: 0;
}

.field-icon {
  opacity: 0.95;
}

.field-action {
  min-width: 28rpx;
  justify-content: flex-end;
}

.field-input {
  flex: 1;
  color: #d7e3f5;
  font-size: 28rpx;
}

.login-error {
  padding: 18rpx 20rpx;
  border-radius: 8rpx;
  background: rgba(147, 0, 10, 0.32);
  color: #ffdad6;
  font-size: 26rpx;
}

.login-button {
  width: 100%;
  margin-top: 12rpx;
  height: 78rpx;
  line-height: 78rpx;
  border: none;
  border-radius: 12rpx;
  background: linear-gradient(90deg, #007aff 0%, #5ac8fa 100%);
  color: #003354;
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
  box-shadow: 0 4rpx 15rpx rgba(0, 122, 255, 0.3);
}

.login-button::after {
  border: none;
}

.login-button[disabled] {
  opacity: 0.7;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  margin-top: 0;
  padding: 0 2rpx;
}

.remember-row {
  display: flex;
  align-items: center;
  gap: 2rpx;
}

.fingerprint-text {
  letter-spacing: 0.5rpx;
}

.login-help {
  margin-top: 116rpx;
  text-align: center;
  line-height: 1.7;
}

.footer-icons {
  margin-top: 28rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.footer-icon {
  width: 46rpx;
  height: 46rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 999rpx;
  background: rgba(24, 35, 50, 0.94);
  border: 1px solid rgba(141, 144, 160, 0.12);
  color: rgba(195, 198, 214, 0.9);
  box-shadow: 0 6rpx 12rpx rgba(0, 0, 0, 0.18);
}
</style>
