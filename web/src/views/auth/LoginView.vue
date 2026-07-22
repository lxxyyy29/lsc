<template>
  <section class="login-view">
    <div class="login-view__backdrop"></div>

    <div class="login-frame" aria-label="Web 登录页">
      <section class="login-frame__hero" aria-label="平台介绍">
        <div class="login-frame__hero-panel">
          <h1>居里智能低空巡检综合监管平台</h1>
        </div>
      </section>

      <section class="login-frame__form-panel">
        <div class="login-card">
          <h2>欢迎登录</h2>
          <form class="login-form" @submit.prevent="handleSubmit">
            <label class="field-stack">
              <span class="field-stack__label">账号</span>
              <input
                v-model.trim="form.account"
                aria-label="账号"
                type="text"
                autocomplete="username"
                placeholder="请输入账号"
                :disabled="submitting"
              />
            </label>

            <label class="field-stack">
              <span class="field-stack__label">密码</span>
              <input
                v-model="form.password"
                aria-label="密码"
                type="password"
                autocomplete="current-password"
                placeholder="请输入密码"
                :disabled="submitting"
              />
            </label>

            <p v-if="errorMessage" class="login-form__error" role="alert">{{ errorMessage }}</p>

            <button type="submit" class="login-form__submit" :disabled="submitting">
              {{ submitting ? '登录中...' : '登录平台' }}
            </button>
          </form>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loginWeb } from '../../api/auth'
import { getFirstDynamicRoute, registerDynamicRoutes } from '../../router/dynamic-routes'

const router = useRouter()
const route = useRoute()

const form = reactive({
  account: '',
  password: ''
})

const submitting = ref(false)
const errorMessage = ref('')

const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : ''))

async function handleSubmit() {
  submitting.value = true
  errorMessage.value = ''

  try {
    const session = await loginWeb({
      account: form.account,
      password: form.password
    })

    if (session.menuTree?.length) {
      registerDynamicRoutes(router, session.menuTree)
    }

    await router.replace(redirect.value || getFirstDynamicRoute(session.menuTree ?? []) || '/dashboard')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-view {
  position: relative;
  height: 100vh;
  overflow: hidden;
  padding: 32px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 10% 0%, rgba(74, 156, 255, 0.16), transparent 18%),
    radial-gradient(circle at 90% 8%, rgba(42, 227, 255, 0.12), transparent 20%),
    linear-gradient(180deg, #030913 0%, #06121f 32%, #06101a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-view__backdrop {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(97, 188, 245, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(97, 188, 245, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(180deg, rgba(255, 255, 255, 0.65), transparent 82%);
}

.login-frame {
  position: relative;
  z-index: 1;
  min-height: 600px;
  width: min(1200px, 100%);
  display: grid;
  grid-template-columns: 1fr 1fr;
  border-radius: 32px;
  overflow: hidden;
  border: 1px solid rgba(103, 187, 246, 0.18);
  box-shadow: 0 28px 60px rgba(0, 0, 0, 0.42);
}

.login-frame__hero,
.login-frame__form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-frame__hero {
  position: relative;
  padding: 40px;
  background:
    radial-gradient(circle at 22% 22%, rgba(88, 191, 255, 0.16), transparent 24%),
    linear-gradient(180deg, rgba(7, 27, 46, 0.98), rgba(4, 14, 24, 0.99));
}

.login-frame__hero::after {
  content: '';
  position: absolute;
  right: -50px;
  top: 50%;
  transform: translateY(-50%);
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(88, 191, 255, 0.24), rgba(15, 48, 78, 0.08) 52%, transparent 74%);
  border: 1px solid rgba(113, 197, 255, 0.16);
  pointer-events: none;
}

.login-frame__hero-panel {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #eaf5ff;
}

.login-frame__hero-panel h1 {
  margin: 0;
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.3;
}

.login-frame__form-panel {
  padding: 40px;
  background: linear-gradient(180deg, rgba(7, 22, 38, 0.98), rgba(4, 13, 22, 0.99));
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  border-radius: 24px;
  background: rgba(10, 31, 50, 0.92);
  border: 1px solid rgba(110, 194, 255, 0.16);
  box-shadow: 0 28px 60px rgba(0, 0, 0, 0.32);
  color: #eaf5ff;
}

.login-card h2 {
  margin: 0 0 24px 0;
  font-size: 32px;
  line-height: 1.2;
}

.login-form {
  display: grid;
  gap: 20px;
}

.field-stack {
  display: grid;
  gap: 8px;
}

.field-stack__label {
  color: #cfe5fb;
  font-size: 13px;
}

.field-stack input {
  min-height: 52px;
  border: 1px solid rgba(110, 194, 255, 0.14);
  border-radius: 16px;
  padding: 0 16px;
  font: inherit;
  color: #eaf5ff;
  background: rgba(6, 24, 39, 0.98);
}

.field-stack input::placeholder {
  color: #7ea4c8;
}

.field-stack input:focus {
  outline: 2px solid rgba(87, 185, 255, 0.34);
  outline-offset: 1px;
}

.login-form__error {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid rgba(255, 113, 134, 0.26);
  border-radius: 14px;
  color: #ffd9df;
  background: rgba(95, 18, 32, 0.56);
}

.login-form__submit {
  min-height: 54px;
  border: 0;
  border-radius: 16px;
  font: inherit;
  font-size: 16px;
  font-weight: 700;
  color: #04111d;
  background: linear-gradient(135deg, #73ebff 0%, #57b9ff 100%);
  cursor: pointer;
  margin-top: 10px;
}

.login-form__submit:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

@media (max-width: 900px) {
  .login-frame {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .login-view {
    padding: 16px;
  }
  
  .login-frame__hero,
  .login-frame__form-panel {
    padding: 24px;
  }

  .login-frame__hero::after {
    display: none;
  }
}
</style>
