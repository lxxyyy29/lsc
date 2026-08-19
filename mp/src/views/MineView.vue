<template>
  <div class="page">
    <div class="header">
      <div class="avatar">{{ session?.userName?.slice(0, 1) || '用' }}</div>
      <div class="info">
        <h3>{{ session?.userName || '用户' }}</h3>
        <p>普通群众</p>
      </div>
    </div>

    <div class="stats">
      <div class="stat-item">
        <p class="num">{{ totalReports }}</p>
        <p class="label">总上报</p>
      </div>
      <div class="stat-item">
        <p class="num">{{ processingCount }}</p>
        <p class="label">处理中</p>
      </div>
      <div class="stat-item">
        <p class="num">{{ completedCount }}</p>
        <p class="label">已办结</p>
      </div>
    </div>

    <div class="menu">
      <router-link to="/history" class="menu-item">
        <i class="fas fa-list"></i>
        <span>我的上报</span>
        <span class="arrow">›</span>
      </router-link>
      <div class="menu-item" @click="showAbout = true">
        <i class="fas fa-info-circle"></i>
        <span>关于平台</span>
        <span class="arrow">›</span>
      </div>
      <div class="menu-item" @click="handleLogout">
        <i class="fas fa-sign-out-alt"></i>
        <span>退出登录</span>
        <span class="arrow">›</span>
      </div>
    </div>

    <!-- 关于弹窗 -->
    <div v-if="showAbout" class="mask" @click="showAbout = false">
      <div class="dialog" @click.stop>
        <h3>关于平台</h3>
        <p>东莞杰瑞智慧网格治理平台</p>
        <p>居民服务小程序 v1.0</p>
        <p>发现身边问题，一键上报，共建美好社区</p>
        <button @click="showAbout = false" class="btn-close">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSession, logout, getMyReports } from '../api'

const router = useRouter()
const session = ref(getSession())
const showAbout = ref(false)
const totalReports = ref(0)
const processingCount = ref(0)
const completedCount = ref(0)

function handleLogout() {
  logout()
  router.push('/login')
}

async function loadStats() {
  try {
    const res: any = await getMyReports()
    const items = res?.items || res?.data?.items || []
    totalReports.value = items.length
    processingCount.value = items.filter((i: any) => i.status === 'PROCESSING').length
    completedCount.value = items.filter((i: any) => i.status === 'CLOSED').length
  } catch (e) { console.error(e) }
}

onMounted(() => { loadStats() })
</script>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.header {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border-radius: 12px; padding: 24px 20px; color: #fff;
  display: flex; align-items: center; gap: 16px; margin-bottom: 16px;
}
.avatar {
  width: 56px; height: 56px; background: rgba(255,255,255,0.2); border-radius: 50%;
  display: flex; align-items: center; justify-content: center; font-size: 24px; font-weight: 600;
}
.info h3 { font-size: 18px; font-weight: 600; }
.info p { font-size: 12px; opacity: 0.8; margin-top: 4px; }
.stats {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px;
}
.stat-item {
  background: #fff; border-radius: 12px; padding: 16px; text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.stat-item .num { font-size: 24px; font-weight: 700; color: #1890ff; }
.stat-item .label { font-size: 12px; color: #6b7280; margin-top: 4px; }
.menu {
  background: #fff; border-radius: 12px; overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.menu-item {
  display: flex; align-items: center; gap: 12px; padding: 16px;
  border-bottom: 1px solid #f3f4f6; text-decoration: none; color: #374151; font-size: 14px;
}
.menu-item:last-child { border-bottom: none; }
.menu-item i { font-size: 18px; color: #1890ff; width: 24px; text-align: center; }
.menu-item span { flex: 1; }
.arrow { color: #d1d5db; font-size: 18px; }
.mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;
}
.dialog {
  background: #fff; border-radius: 16px; padding: 24px; width: 80%; max-width: 320px; text-align: center;
}
.dialog h3 { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.dialog p { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.btn-close {
  margin-top: 16px; padding: 8px 24px; background: #1890ff; color: #fff;
  border: none; border-radius: 6px; font-size: 14px; cursor: pointer;
}
</style>
