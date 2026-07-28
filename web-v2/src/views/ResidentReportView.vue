<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">居民上报管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">查看和处理居民随手拍上报的问题</p>

    <!-- 状态筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap;">
      <button v-for="s in statusTabs" :key="s.key"
        :style="activeStatus === s.key ? 'padding:6px 16px;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;cursor:pointer;' : 'padding:6px 16px;border:1px solid #e5e7eb;border-radius:6px;background:#fff;color:#374151;font-size:13px;cursor:pointer;'"
        @click="activeStatus = s.key; fetchData()">
        {{ s.label }}
      </button>
    </div>

    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="fetchData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead><tr><th>标题</th><th>类型</th><th>上报时间</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.title }}</td>
              <td><span class="tag tag-blue">{{ eventTypeLabel(item.eventType) }}</span></td>
              <td>{{ formatTime(item.createdAt || item.occurredAt) }}</td>
              <td>
                <span :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
              </td>
              <td>
                <button v-if="item.status === 'CLOSED' && !item.rating" disabled
                  style="padding:4px 12px;border:1px solid #e5e7eb;border-radius:4px;background:#f9fafb;color:#9ca3af;font-size:12px;cursor:default;">
                  待评价
                </button>
                <span v-else-if="item.rating" style="color:#faad14;font-size:12px;">
                  {{ '★'.repeat(item.rating) }}
                </span>
                <span v-else style="font-size:12px;color:#9ca3af;">-</span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'WAITING_DISPATCH', label: '待派单' },
  { key: 'DISPATCHED_TO_WORK_ORDER', label: '处理中' },
  { key: 'CLOSED', label: '已办结' },
  { key: 'IGNORED', label: '已忽略' }
]
const activeStatus = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')

const eventTypes: Record<string, string> = {
  ROAD: '道路损坏', LIGHT: '路灯故障', PIPE: '管道破损',
  ENV: '环境卫生', SAFE: '安全隐患', NOISE: '噪音扰民', OTHER: '其他'
}

function statusLabel(s: string) {
  return { WAITING_DISPATCH: '待派单', DISPATCHED_TO_WORK_ORDER: '处理中', CLOSED: '已办结', IGNORED: '已忽略' }[s] || s || '未知'
}

function statusClass(s: string) {
  if (s === 'CLOSED') return 'tag tag-green'
  if (s === 'DISPATCHED_TO_WORK_ORDER') return 'tag tag-blue'
  if (s === 'IGNORED') return 'tag tag-red'
  return 'tag tag-orange'
}

function eventTypeLabel(t: string) { return eventTypes[t] || t || '其他' }

function formatTime(t: string) {
  if (!t) return '-'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: 1, pageSize: 50 }
    if (activeStatus.value) params.status = activeStatus.value
    const res = await http.get('/community/resident-reports', { params })
    list.value = res?.items || res?.data?.items || res || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
