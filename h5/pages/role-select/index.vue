<template>
  <view class="login-page">
    <view class="login-header">
      <text class="logo">🏘️</text>
      <text class="title">东莞杰瑞智慧网格治理平台</text>
      <text class="subtitle">社区治理综合服务平台</text>
    </view>

    <view class="login-card">
      <!-- 手机号验证码登录（默认） -->
      <template v-if="mode === 'phone'">
        <view class="form-item">
          <input
            v-model.trim="form.phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-style="color:#9ca3af;font-size:30rpx;"
          />
        </view>
        <view class="form-item code-row">
          <input
            v-model.trim="form.code"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
            placeholder-style="color:#9ca3af;font-size:30rpx;"
          />
          <button class="btn-code" :disabled="countdown > 0 || !form.phone" @click="handleSendCode" style="color:#ffffff;">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </view>
        <button class="btn-login" :disabled="submitting" @click="handlePhoneLogin" style="color:#ffffff;">
          {{ submitting ? '登录中...' : '登 录' }}
        </button>
        <!-- #ifdef MP-WEIXIN -->
        <!-- 微信手机号一键登录（企业/组织认证主体可用）：授权后自动按手机号登录 -->
        <button class="btn-wechat" open-type="getPhoneNumber" @getphonenumber="handleWechatLogin" :disabled="submitting" style="color:#07c160;">
          <text>微信一键登录</text>
        </button>
        <!-- #endif -->
        <view class="test-hint">
          <text>测试验证码：123456</text>
        </view>
        <view class="switch-row">
          <text class="switch-link" @click="switchToPassword">使用账号密码登录</text>
        </view>
      </template>

      <!-- 账号密码登录 -->
      <template v-else>
        <view class="form-item">
          <input
            v-model.trim="form.account"
            placeholder="请输入账号或手机号"
            placeholder-style="color:#9ca3af;font-size:30rpx;"
          />
        </view>
        <view class="form-item">
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            placeholder-style="color:#9ca3af;font-size:30rpx;"
          />
        </view>
        <button class="btn-login" :disabled="submitting" @click="handlePasswordLogin" style="color:#ffffff;">
          {{ submitting ? '登录中...' : '登 录' }}
        </button>
        <view class="switch-row">
          <text class="switch-link" @click="switchToPhone">使用手机号验证码登录</text>
        </view>
      </template>

      <text v-if="errorMessage" class="error">{{ errorMessage }}</text>
    </view>

    <view class="register-links">
      <!-- 注册入口已移除：网格员由后台账号管理/组织人员添加，居民通过微信一键登录自动开通 -->
      <text class="register-hint">网格员由管理后台统一开通 · 居民可用微信一键登录</text>
    </view>

    <view class="login-help">
      <text>忘记密码请联系管理员重置</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { HttpResponseError } from '../../src/api/http'
import { loginH5, createH5SessionFromLoginResponse, persistH5Session } from '../../src/api/auth'
import { sendSmsCode, phoneLogin, login as residentLogin, persistResidentSession, wechatLogin } from '../../src/api/resident'

type LoginMode = 'phone' | 'password'

const mode = ref<LoginMode>('phone')
const submitting = ref(false)
const errorMessage = ref('')
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  phone: '',
  code: '',
  account: '',
  password: ''
})

const H5_ENTRY_CODES = ['menu:h5:workbench:view', 'menu:h5:workorder:list']

function isGridWorker(permissionCodes: string[]) {
  return (permissionCodes || []).some((code) => H5_ENTRY_CODES.includes(code))
}

function switchToPassword() {
  mode.value = 'password'
  errorMessage.value = ''
}
function switchToPhone() {
  mode.value = 'phone'
  errorMessage.value = ''
}

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function handleSendCode() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errorMessage.value = '请输入正确的手机号'
    return
  }
  errorMessage.value = ''
  try {
    await sendSmsCode(form.phone)
    startCountdown()
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } catch (e: any) {
    errorMessage.value = e?.message || '验证码发送失败'
  }
}

/** 微信手机号一键登录：getPhoneNumber 回调拿 code → 后端换手机号并按角色登录 */
async function handleWechatLogin(e: any) {
  // 用户拒绝授权时 detail 无 code
  if (!e?.detail?.code) {
    errorMessage.value = '已取消微信授权'
    return
  }
  errorMessage.value = ''
  submitting.value = true
  try {
    const session = await wechatLogin(e.detail.code)
    redirectByRole(session)
  } catch (err: any) {
    errorMessage.value = err?.message || '微信登录失败，请重试'
  } finally {
    submitting.value = false
  }
}

/** 手机号验证码登录：后端按角色决定 clientType，前端按权限码判断身份跳转 */
async function handlePhoneLogin() {
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errorMessage.value = '请输入正确的手机号'
    return
  }
  if (!form.code) {
    errorMessage.value = '请输入验证码'
    return
  }
  errorMessage.value = ''
  submitting.value = true
  try {
    const session = await phoneLogin(form.phone, form.code)
    redirectByRole(session)
  } catch (e: any) {
    errorMessage.value = e?.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

/** 账号密码登录：先试 H5（网格员），失败再试 WEB（居民） */
async function handlePasswordLogin() {
  if (!form.account || !form.password) {
    errorMessage.value = '请输入账号和密码'
    return
  }
  errorMessage.value = ''
  submitting.value = true
  try {
    // 第一次：H5 接口（网格员）。探测性请求，失败时静默（居民账号不允许 H5 登录属正常分支，不弹错）
    await loginH5({ account: form.account, password: form.password }, { silent: true })
    uni.reLaunch({ url: '/pages/workbench/index' })
  } catch {
    // 第二次：WEB 接口（居民）
    try {
      const session = await residentLogin(form.account, form.password)
      uni.reLaunch({ url: '/pages/resident/report/index' })
    } catch (e: any) {
      errorMessage.value = e?.message || '账号或密码错误'
    }
  } finally {
    submitting.value = false
  }
}

/** 按角色跳转：网格员 → 工作台；居民 → 随手拍 */
function redirectByRole(session: { permissionCodes: string[] }) {
  if (isGridWorker(session.permissionCodes)) {
    // 网格员：转换为 H5 session（token clientType=H5，可访问 /api/h5 接口）
    persistH5Session(createH5SessionFromLoginResponse(session as any))
    uni.reLaunch({ url: '/pages/workbench/index' })
  } else {
    // 居民：持久化 grid-mp-session 后进居民端
    persistResidentSession(session as any)
    uni.reLaunch({ url: '/pages/resident/report/index' })
  }
}

</script>

<style>
/* 小程序统一登录页 全局样式 */
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80rpx 48rpx 60rpx;
  box-sizing: border-box;
}
.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 64rpx;
}
.login-header .logo { font-size: 120rpx; margin-bottom: 28rpx; }
.login-header .title { font-size: 48rpx; font-weight: 600; margin-bottom: 14rpx; color: #fff; }
.login-header .subtitle { font-size: 28rpx; opacity: 0.85; color: #fff; }
.login-card {
  width: 100%;
  max-width: 720rpx;
  background: #fff;
  border-radius: 32rpx;
  padding: 56rpx 48rpx 40rpx;
  box-shadow: 0 16rpx 64rpx rgba(0,0,0,0.1);
}
.form-item { margin-bottom: 28rpx; }
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
.code-row { display: flex; gap: 20rpx; }
.code-row input { flex: 1; }
.btn-code {
  width: 220rpx;
  height: 90rpx;
  line-height: 90rpx;
  padding: 0;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #ffffff;
  border: none;
  border-radius: 12rpx;
  font-size: 28rpx;
  flex-shrink: 0;
}
.btn-code::after { border: none; }
.btn-code[disabled] { opacity: 0.6; }
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
  margin-top: 8rpx;
}
.btn-login::after { border: none; }
.btn-login[disabled] { opacity: 0.6; }
.btn-wechat {
  width: 100%;
  height: 90rpx;
  line-height: 90rpx;
  margin-top: 24rpx;
  border-radius: 12rpx;
  border: 2rpx solid #07c160;
  background: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
}
.btn-wechat::after { border: none; }
.btn-wechat[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 26rpx; text-align: center; margin-top: 20rpx; display: block; }
.test-hint { margin-top: 20rpx; text-align: center; font-size: 24rpx; color: #9ca3af; }
.switch-row { margin-top: 24rpx; text-align: center; }
.switch-link { color: #1890ff; font-size: 26rpx; }
.register-links {
  margin-top: 48rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
}
.register-hint { color: rgba(255,255,255,0.65); font-size: 26rpx; text-align: center; }
.login-help { margin-top: 40rpx; color: rgba(255,255,255,0.7); font-size: 24rpx; text-align: center; }
</style>
