<template>
  <div class="register-page">
    <div class="header">
      <button @click="$router.push('/login')" class="back-btn">← 返回</button>
      <h2>注册账号</h2>
    </div>

    <div class="form">
      <div class="form-item">
        <label>账号</label>
        <input v-model="form.account" placeholder="4-20位账号" />
      </div>
      <div class="form-item">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="6-20位密码" />
      </div>
      <div class="form-item">
        <label>确认密码</label>
        <input v-model="form.confirmPassword" type="password" placeholder="再次输入密码" />
      </div>
      <div class="form-item">
        <label>真实姓名</label>
        <input v-model="form.realName" placeholder="请输入真实姓名" />
      </div>
      <div class="form-item">
        <label>联系电话</label>
        <input v-model="form.phone" placeholder="选填" />
      </div>

      <button @click="handleRegister" :disabled="loading" class="btn-submit">
        {{ loading ? '注册中...' : '注 册' }}
      </button>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="success" class="success">{{ success }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api'

const router = useRouter()
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
    setTimeout(() => router.push('/login'), 1500)
  } catch (e: any) {
    error.value = e || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { min-height: 100vh; background: #f5f7fa; }
.header {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  padding: 20px 16px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn { background: none; border: none; color: #fff; font-size: 16px; cursor: pointer; }
.header h2 { font-size: 18px; font-weight: 600; }
.form { padding: 24px 16px; }
.form-item { margin-bottom: 16px; }
.form-item label { display: block; font-size: 13px; color: #6b7280; margin-bottom: 6px; }
.form-item input {
  width: 100%; padding: 12px 14px; border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 15px; outline: none; background: #fff;
}
.form-item input:focus { border-color: #1890ff; }
.btn-submit {
  width: 100%; padding: 14px; background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff; border: none; border-radius: 8px; font-size: 16px; font-weight: 600; cursor: pointer; margin-top: 8px;
}
.btn-submit[disabled] { opacity: 0.6; }
.error { color: #ff4d4f; font-size: 13px; text-align: center; margin-top: 12px; }
.success { color: #52c41a; font-size: 13px; text-align: center; margin-top: 12px; }
</style>
