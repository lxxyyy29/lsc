<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">居民上报记录</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">居民上报统一归口至事件闭环处理中心，本页仅作记录查看，处置派单请在事件中心操作</p>

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
          <thead><tr><th>标题</th><th>类型</th><th>上报人</th><th>上报时间</th><th>关联事件</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.title }}</td>
              <td><span class="tag tag-blue">{{ getEventTypeName(item.reportType) }}</span></td>
              <td>{{ item.residentName || '-' }}</td>
              <td>{{ formatTime(item.createdAt) }}</td>
              <td>
                <a v-if="item.eventId" href="javascript:void(0)" @click="goEvent(item)"
                   style="color:#0284c7;font-size:12px;text-decoration:none;">
                  <i class="fas fa-link"></i> 查看事件
                </a>
                <span v-else style="color:#9ca3af;font-size:12px;">历史记录</span>
              </td>
              <td>
                <span :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span>
              </td>
              <td>
                <button @click="viewDetail(item)"
                  style="padding:4px 12px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;color:#374151;">
                  详情
                </button>
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
import { useRouter } from 'vue-router'
import http from '../api'
import { getEventTypeName } from '../utils/eventTypes'

const router = useRouter()

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待处理' },
  { key: 'PROCESSING', label: '处理中' },
  { key: 'HANDLED', label: '已处理' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'IGNORED', label: '已忽略' }
]
const activeStatus = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')

function statusLabel(s: string) {
  return { PENDING: '待处理', PROCESSING: '处理中', HANDLED: '已处理', COMPLETED: '已完成', IGNORED: '已忽略' }[s] || s || '未知'
}

function statusClass(s: string) {
  if (s === 'HANDLED' || s === 'COMPLETED') return 'tag tag-green'
  if (s === 'PROCESSING') return 'tag tag-blue'
  if (s === 'IGNORED') return 'tag tag-red'
  return 'tag tag-orange'
}

function formatTime(t: string) {
  if (!t) return '-'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = {}
    if (activeStatus.value) params.status = activeStatus.value
    const res = await http.get('/community/resident-reports', { params })
    // 后端返回的是数组（非分页结构），兼容多种解包层级
    list.value = Array.isArray(res) ? res : (res?.items || res?.data?.items || [])
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function viewDetail(item: any) {
  alert(`标题：${item.title}\n描述：${item.content || '-'}\n类型：${getEventTypeName(item.reportType)}\n状态：${statusLabel(item.status)}\n上报人：${item.residentName || '-'}（${item.residentPhone || '-'}）\n所属网格：${item.gridName || '-'}\n上报时间：${formatTime(item.createdAt)}${item.handleResult ? '\n处理结果：' + item.handleResult : ''}`)
}

// 跳转到归口生成的事件详情，处置派单在事件中心完成
function goEvent(item: any) {
  router.push('/events/' + item.eventId)
}

onMounted(fetchData)
</script>
