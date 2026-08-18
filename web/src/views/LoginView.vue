<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- Logo 区域 -->
      <div class="login-header">
        <div class="logo-wrapper">
          <div class="logo-icon">
            <i class="fas fa-city"></i>
          </div>
        </div>
        <h1 class="login-title">拔蛟窝智慧网格治理平台</h1>
        <p class="login-subtitle">Grid Community Governance Platform</p>
      </div>

      <!-- 登录表单 -->
      <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-user"></i>
            <span>账号</span>
          </label>
          <input
            v-model="form.account"
            type="text"
            placeholder="请输入账号"
            class="form-input"
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-lock"></i>
            <span>密码</span>
          </label>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            class="form-input"
            autocomplete="current-password"
          />
        </div>

        <button type="submit" class="login-btn" :disabled="loading">
          <span v-if="!loading" class="btn-content">
            <span>登 录</span>
            <i class="fas fa-arrow-right"></i>
          </span>
          <span v-else class="btn-loading">
            <i class="fas fa-spinner fa-spin"></i>
            <span>登录中...</span>
          </span>
        </button>

        <p v-if="error" class="error-msg">
          <i class="fas fa-exclamation-circle"></i>
          {{ error }}
        </p>
      </form>

      <!-- 注册表单（普通管理员，需超级管理员审批后才能登录） -->
      <form v-else @submit.prevent="handleRegister" class="login-form">
        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-user"></i>
            <span>账号</span>
          </label>
          <input v-model="regForm.account" type="text" placeholder="请输入登录账号" class="form-input" autocomplete="username" />
        </div>

        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-id-card"></i>
            <span>姓名</span>
          </label>
          <input v-model="regForm.realName" type="text" placeholder="请输入真实姓名" class="form-input" />
        </div>

        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-mobile-alt"></i>
            <span>手机号</span>
          </label>
          <input v-model="regForm.phone" type="text" placeholder="请输入手机号" class="form-input" maxlength="11" />
        </div>

        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-lock"></i>
            <span>密码</span>
          </label>
          <input v-model="regForm.password" type="password" placeholder="请输入密码（至少6位）" class="form-input" autocomplete="new-password" />
        </div>

        <div class="form-group">
          <label class="form-label">
            <i class="fas fa-lock"></i>
            <span>确认密码</span>
          </label>
          <input v-model="regForm.confirm" type="password" placeholder="请再次输入密码" class="form-input" autocomplete="new-password" />
        </div>

        <button type="submit" class="login-btn" :disabled="regLoading">
          <span v-if="!regLoading" class="btn-content">
            <span>提交注册</span>
            <i class="fas fa-arrow-right"></i>
          </span>
          <span v-else class="btn-loading">
            <i class="fas fa-spinner fa-spin"></i>
            <span>提交中...</span>
          </span>
        </button>

        <p v-if="regMsg" :class="regOk ? 'success-msg' : 'error-msg'">
          <i :class="regOk ? 'fas fa-check-circle' : 'fas fa-exclamation-circle'"></i>
          {{ regMsg }}
        </p>
      </form>

      <!-- 登录/注册切换 -->
      <div class="mode-switch">
        <template v-if="mode === 'login'">
          还没有账号？<a href="javascript:;" @click="switchMode('register')">注册普通管理员</a>
        </template>
        <template v-else>
          已有账号？<a href="javascript:;" @click="switchMode('login')">返回登录</a>
        </template>
      </div>

      <!-- 底部信息 -->
      <div class="login-footer">
        <div class="quick-login">
          <span class="quick-label">快速登录:</span>
          <button @click="quickLogin('admin', 'admin123')" class="quick-btn">管理员</button>
          <button @click="quickLogin('grid01', '123456')" class="quick-btn">网格员</button>
        </div>
        <p class="copyright">© 2026 拔蛟窝社区综合治理办公室</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, registerAdmin } from '../api'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const form = reactive({ account: '', password: '' })

// 登录/注册双模式
const mode = ref<'login' | 'register'>('login')
const regLoading = ref(false)
const regMsg = ref('')
const regOk = ref(false)
const regForm = reactive({ account: '', realName: '', phone: '', password: '', confirm: '' })

function switchMode(m: 'login' | 'register') {
  mode.value = m
  error.value = ''
  regMsg.value = ''
  regOk.value = false
}

const emit = defineEmits(['success'])

async function handleLogin() {
  if (!form.account.trim()) {
    error.value = '请输入账号'
    return
  }
  if (!form.password.trim()) {
    error.value = '请输入密码'
    return
  }

  loading.value = true
  error.value = ''
  try {
    await login(form.account, form.password)
    emit('success')
    router.push('/')
  } catch (e: any) {
    error.value = e?.message || e || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}

function quickLogin(account: string, password: string) {
  form.account = account
  form.password = password
  handleLogin()
}

async function handleRegister() {
  regMsg.value = ''
  regOk.value = false
  if (!regForm.account.trim()) { regMsg.value = '请输入账号'; return }
  if (!regForm.realName.trim()) { regMsg.value = '请输入姓名'; return }
  if (!/^1[3-9]\d{9}$/.test(regForm.phone)) { regMsg.value = '请输入正确的手机号'; return }
  if (regForm.password.length < 6) { regMsg.value = '密码至少 6 位'; return }
  if (regForm.password !== regForm.confirm) { regMsg.value = '两次输入的密码不一致'; return }

  regLoading.value = true
  try {
    await registerAdmin({
      account: regForm.account.trim(),
      password: regForm.password,
      realName: regForm.realName.trim(),
      phone: regForm.phone
    })
    regOk.value = true
    regMsg.value = '注册申请已提交，请等待超级管理员审批后再登录'
    // 回填账号密码方便审批通过后直接登录
    form.account = regForm.account.trim()
    form.password = ''
  } catch (e: any) {
    regMsg.value = e?.message || '注册失败，请稍后重试'
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
/* === 全屏登录页 === */
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* === 背景装饰 === */
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #0c4a6e 0%, #0369a1 40%, #0284c7 100%);
  z-index: 0;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}
.bg-circle-1 {
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, #fff 0%, transparent 70%);
  top: -200px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}
.bg-circle-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #fff 0%, transparent 70%);
  bottom: -150px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}
.bg-circle-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #fff 0%, transparent 70%);
  top: 50%;
  left: 60%;
  animation: float 6s ease-in-out infinite 2s;
}

@keyframes float {
  0%, 100% { transform: translateY(0px) scale(1); }
  50% { transform: translateY(-20px) scale(1.05); }
}

/* === 登录卡片 === */
.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  max-width: 90vw;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 20px;
  padding: 48px 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* === Logo 区域 === */
.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(2, 132, 199, 0.3);
}

.logo-icon i {
  font-size: 28px;
  color: #fff;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 6px;
  letter-spacing: 0.5px;
}

.login-subtitle {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 2px;
}

/* === 登录表单 === */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.form-label i {
  font-size: 14px;
  color: #94a3b8;
  width: 16px;
  text-align: center;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
  background: #f8fafc;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #0284c7;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.1);
}

.form-input::placeholder {
  color: #cbd5e1;
}

/* === 登录按钮 === */
.login-btn {
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px rgba(2, 132, 199, 0.3);
  margin-top: 4px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(2, 132, 199, 0.4);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-content i {
  font-size: 13px;
  transition: transform 0.2s;
}

.login-btn:hover .btn-content i {
  transform: translateX(3px);
}

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

/* === 错误/成功提示 === */
.error-msg {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #ef4444;
  font-size: 13px;
  margin: 0;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

.error-msg i {
  font-size: 14px;
}

.success-msg {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #059669;
  font-size: 13px;
  margin: 0;
  padding: 8px 12px;
  background: #ecfdf5;
  border-radius: 8px;
  border: 1px solid #a7f3d0;
}

.success-msg i {
  font-size: 14px;
}

/* === 登录/注册切换 === */
.mode-switch {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #64748b;
}

.mode-switch a {
  color: #0284c7;
  font-weight: 500;
  text-decoration: none;
}

.mode-switch a:hover {
  text-decoration: underline;
}

/* === 底部 === */
.login-footer {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.quick-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.quick-label {
  font-size: 12px;
  color: #94a3b8;
}

.quick-btn {
  padding: 4px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-btn:hover {
  border-color: #0284c7;
  color: #0284c7;
  background: #eff6ff;
}

.copyright {
  font-size: 11px;
  color: #cbd5e1;
  margin: 0;
}

/* === 响应式 === */
@media (max-width: 480px) {
  .login-card {
    padding: 36px 24px 24px;
    border-radius: 16px;
  }
  .login-title {
    font-size: 18px;
  }
}
</style>
