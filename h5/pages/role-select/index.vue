<template>
  <view class="login-page">
    <!-- 背景装饰光斑（纯 CSS，不依赖图片资源） -->
    <view class="bg-blob bg-blob-1"></view>
    <view class="bg-blob bg-blob-2"></view>

    <view class="login-header">
      <!-- 用 CSS 绘制「网格」徽标，替代原先的 emoji（emoji 在真机上观感差） -->
      <view class="logo-badge">
        <view class="grid-icon">
          <view class="grid-row">
            <view class="grid-cell"></view>
            <view class="grid-cell"></view>
          </view>
          <view class="grid-row">
            <view class="grid-cell"></view>
            <view class="grid-cell"></view>
          </view>
        </view>
      </view>
      <text class="title">东莞杰瑞智慧网格治理平台</text>
      <text class="subtitle">社区治理综合服务平台</text>
    </view>

    <view class="login-card">
      <!-- 手机号验证码登录（默认） -->
      <template v-if="mode === 'phone'">
        <view class="form-item" :class="{ 'field-active': activeField === 'phone' }">
          <input
            v-model.trim="form.phone"
            type="number"
            maxlength="11"
            placeholder="请输入手机号"
            placeholder-style="color:#9aa4b2;font-size:30rpx;"
            @focus="onFocus('phone')"
            @blur="onBlur"
          />
        </view>
        <view class="form-item code-row" :class="{ 'field-active': activeField === 'code' }">
          <input
            v-model.trim="form.code"
            type="number"
            maxlength="6"
            placeholder="请输入验证码"
            placeholder-style="color:#9aa4b2;font-size:30rpx;"
            @focus="onFocus('code')"
            @blur="onBlur"
          />
          <button class="btn-code" :disabled="countdown > 0 || !form.phone" hover-class="btn-hover" @click="handleSendCode">
            {{ countdown > 0 ? countdown + 's' : '获取验证码' }}
          </button>
        </view>
        <button class="btn-login" :disabled="submitting" hover-class="btn-hover" @click="handlePhoneLogin">
          {{ submitting ? '登录中...' : '登 录' }}
        </button>
        <!-- #ifdef MP-WEIXIN -->
        <!-- 微信手机号一键登录（企业/组织认证主体可用）：授权后自动按手机号登录 -->
        <button class="btn-wechat" open-type="getPhoneNumber" @getphonenumber="handleWechatLogin" :disabled="submitting" hover-class="btn-hover">
          <view class="wx-mark">
            <view class="wx-dot"></view>
            <view class="wx-dot"></view>
          </view>
          <text class="btn-wechat-text">微信一键登录</text>
        </button>
        <!-- #endif -->
        <view class="switch-row">
          <text class="switch-link" @click="switchToPassword">使用账号密码登录</text>
        </view>
      </template>

      <!-- 账号密码登录 -->
      <template v-else>
        <view class="form-item" :class="{ 'field-active': activeField === 'account' }">
          <input
            v-model.trim="form.account"
            placeholder="请输入账号或手机号"
            placeholder-style="color:#9aa4b2;font-size:30rpx;"
            @focus="onFocus('account')"
            @blur="onBlur"
          />
        </view>
        <view class="form-item" :class="{ 'field-active': activeField === 'password' }">
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            placeholder-style="color:#9aa4b2;font-size:30rpx;"
            @focus="onFocus('password')"
            @blur="onBlur"
          />
        </view>
        <button class="btn-login" :disabled="submitting" hover-class="btn-hover" @click="handlePasswordLogin">
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
type FieldName = 'phone' | 'code' | 'account' | 'password'

const mode = ref<LoginMode>('phone')
const submitting = ref(false)
const errorMessage = ref('')
const countdown = ref(0)
/** 当前聚焦的输入框，仅用于高亮边框（纯样式，不参与业务） */
const activeField = ref<FieldName | ''>('')
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

function onFocus(field: FieldName) {
  activeField.value = field
}
function onBlur() {
  activeField.value = ''
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
    // 「微信一键登录」定位为居民入口：不按权限码分流。
    // 若按权限码判断，手机号恰好也是网格员/组长/管理员账号时会被分到工作台，居民会「误入」管理端。
    persistResidentSession(session as any)
    uni.reLaunch({ url: '/pages/resident/report/index' })
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
/* ============ 页面容器与背景 ============ */
.login-page {
  position: relative;
  min-height: 100vh;
  background: linear-gradient(165deg, #2b8cff 0%, #1670e8 42%, #0b4fae 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 48rpx 64rpx;
  box-sizing: border-box;
  overflow: hidden;
}

/* 背景光斑：增加纵向层次，避免大面积纯色 */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}
.bg-blob-1 {
  width: 520rpx;
  height: 520rpx;
  top: -180rpx;
  right: -160rpx;
  background: rgba(255, 255, 255, 0.10);
}
.bg-blob-2 {
  width: 620rpx;
  height: 620rpx;
  bottom: -260rpx;
  left: -220rpx;
  background: rgba(255, 255, 255, 0.07);
}

/* ============ 头部品牌区 ============ */
.login-header {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 56rpx;
}
.logo-badge {
  width: 132rpx;
  height: 132rpx;
  border-radius: 40rpx;
  background: rgba(255, 255, 255, 0.16);
  border: 2rpx solid rgba(255, 255, 255, 0.38);
  box-shadow: 0 16rpx 40rpx rgba(0, 30, 80, 0.16);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32rpx;
}
.grid-icon {
  width: 68rpx;
  height: 68rpx;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.grid-row {
  display: flex;
  justify-content: space-between;
}
.grid-cell {
  width: 30rpx;
  height: 30rpx;
  border-radius: 8rpx;
  background: #ffffff;
}
.login-header .title {
  font-size: 46rpx;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 2rpx;
  margin-bottom: 16rpx;
  text-align: center;
}
.login-header .subtitle {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.78);
  letter-spacing: 6rpx;
}

/* ============ 登录卡片 ============ */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 720rpx;
  background: #ffffff;
  border-radius: 40rpx;
  padding: 56rpx 48rpx 44rpx;
  box-shadow: 0 24rpx 72rpx rgba(4, 32, 78, 0.22);
  box-sizing: border-box;
}

/* ============ 输入区 ============ */
.form-item {
  margin-bottom: 24rpx;
}
.form-item input {
  width: 100%;
  height: 96rpx;
  padding: 0 32rpx;
  border: 2rpx solid #e8ecf1;
  border-radius: 16rpx;
  font-size: 30rpx;
  color: #111827;
  background: #f7f9fc;
  box-sizing: border-box;
  transition: border-color 0.2s, background-color 0.2s;
}
/* 聚焦高亮：由外层 view 的 class 驱动，兼容小程序 */
.form-item.field-active input {
  border-color: #1670e8;
  background: #ffffff;
}

/* 验证码一行：输入框 + 次要按钮 */
.code-row {
  display: flex;
  align-items: center;
}
.code-row input {
  flex: 1;
}
.btn-code {
  width: 220rpx;
  height: 96rpx;
  line-height: 96rpx;
  padding: 0;
  margin-left: 20rpx;
  flex-shrink: 0;
  background: #eaf3ff;
  color: #1670e8;
  border: none;
  border-radius: 16rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.btn-code::after { border: none; }
.btn-code[disabled] {
  background: #f2f4f7;
  color: #a8b0bb;
}

/* ============ 主按钮 ============ */
.btn-login {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  padding: 0;
  margin-top: 8rpx;
  background: linear-gradient(135deg, #2b8cff 0%, #0e5fd8 100%);
  color: #ffffff;
  border: none;
  border-radius: 16rpx;
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 4rpx;
  box-shadow: 0 14rpx 30rpx rgba(14, 95, 216, 0.26);
}
.btn-login::after { border: none; }
.btn-login[disabled] {
  opacity: 0.55;
  box-shadow: none;
}

/* ============ 微信登录按钮 ============ */
.btn-wechat {
  width: 100%;
  height: 96rpx;
  line-height: 1;
  padding: 0;
  margin-top: 24rpx;
  border: 2rpx solid #07c160;
  border-radius: 16rpx;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-wechat::after { border: none; }
.btn-wechat[disabled] { opacity: 0.55; }
.btn-wechat-text {
  font-size: 32rpx;
  font-weight: 600;
  color: #07c160;
}
/* 极简微信气泡标记（纯 CSS，无图片依赖） */
.wx-mark {
  width: 40rpx;
  height: 34rpx;
  border-radius: 50%;
  background: #07c160;
  margin-right: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.wx-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #ffffff;
  margin: 0 3rpx;
}

/* 按钮按下反馈 */
.btn-hover {
  opacity: 0.88;
}

/* ============ 辅助信息 ============ */
.error {
  display: block;
  margin-top: 22rpx;
  padding: 16rpx 20rpx;
  border-radius: 12rpx;
  background: #fff1f0;
  color: #e5484d;
  font-size: 25rpx;
  line-height: 1.5;
  text-align: center;
}
.switch-row {
  margin-top: 36rpx;
  text-align: center;
}
.switch-link {
  color: #1670e8;
  font-size: 27rpx;
  font-weight: 500;
}

/* ============ 底部说明 ============ */
.register-links {
  position: relative;
  z-index: 1;
  margin-top: 52rpx;
  padding: 0 24rpx;
  text-align: center;
}
.register-hint {
  color: rgba(255, 255, 255, 0.72);
  font-size: 25rpx;
  line-height: 1.6;
}
.login-help {
  position: relative;
  z-index: 1;
  margin-top: 26rpx;
  color: rgba(255, 255, 255, 0.55);
  font-size: 23rpx;
  text-align: center;
}
</style>
