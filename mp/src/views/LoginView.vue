<template>
  <div class="login-page">
    <div class="login-header">
      <div class="logo">🏘️</div>
      <h1>东莞杰瑞智慧网格治理平台</h1>
      <p>居民服务小程序</p>
    </div>

    <div class="login-form">
      <div class="form-item">
        <input v-model="form.account" placeholder="请输入账号" />
      </div>
      <div class="form-item">
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>
      <button @click="handleLogin" :disabled="loading" class="btn-login">
        {{ loading ? '登录中...' : '登 录' }}
      </button>
      <p v-if="error" class="error">{{ error }}</p>

      <div class="footer-links">
        <router-link to="/register">注册账号</router-link>
      </div>
    </div>

    <div class="quick-login">
      <p>测试账号：yonghu / 123456</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
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
    router.push('/report')
  } catch (e: any) {
    error.value = e || '登录失败'
  } finally {
    loading.value = false
  }
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
  padding: 40px 24px;
}
.login-header {
  text-align: center;
  margin-bottom: 40px;
  color: #fff;
}
.logo { font-size: 60px; margin-bottom: 16px; }
.login-header h1 { font-size: 24px; font-weight: 600; margin-bottom: 8px; }
.login-header p { font-size: 14px; opacity: 0.8; }
.login-form {
  width: 100%;
  max-width: 360px;
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}
.form-item { margin-bottom: 16px; }
.form-item input {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
}
.form-item input:focus { border-color: #1890ff; }
.btn-login {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
.btn-login[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 13px; text-align: center; margin-top: 12px; }
.footer-links { text-align: center; margin-top: 16px; }
.footer-links a { color: #1890ff; font-size: 13px; text-decoration: none; }
.quick-login { margin-top: 24px; color: rgba(255,255,255,0.7); font-size: 12px; text-align: center; }
</style>
