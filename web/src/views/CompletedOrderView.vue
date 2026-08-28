<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">已完成工单</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">已通过 PC 端审核、办结关闭的事件工单，仅作归档查阅</p>

    <!-- 筛选栏 -->
    <div class="card" style="margin-bottom:16px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <select v-model="filters.workOrderStatus" class="filter-select" @change="loadData">
          <option value="">全部工单状态</option>
          <option value="COMPLETED">已完成</option>
          <option value="CLOSED">已关闭</option>
        </select>
        <select v-model="filters.urgencyLevel" class="filter-select" @change="loadData">
          <option value="">全部紧急度</option>
          <option value="RED">🔴 紧急</option>
          <option value="YELLOW">🟡 重点</option>
          <option value="GREEN">🟢 一般</option>
        </select>
        <input v-model="searchKey" class="filter-input" placeholder="搜索事件编号/标题..." @keyup.enter="loadData" />
        <button @click="loadData" class="filter-action">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>
    </div>

    <!-- 已完成工单列表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:8px;font-size:13px;">加载中...</p>
      </div>
      <div v-else>
        <table class="table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>事件标题</th>
              <th>工单号</th>
              <th>工单状态</th>
              <th>受派人</th>
              <th>事件状态</th>
              <th>上报时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td style="font-size:12px;">{{ o.eventCode }}</td>
              <td style="font-size:13px;">{{ o.title }}</td>
              <td style="font-size:12px;">{{ o.workOrderNo || '-' }}</td>
              <td><span :class="['tag', o.workOrderStatus === 'CLOSED' ? 'tag-gray' : 'tag-green']">{{ workOrderStatusLabel(o.workOrderStatus) }}</span></td>
              <td style="font-size:12px;">{{ o.assigneeName || '-' }}</td>
              <td><span class="tag tag-blue">{{ statusLabel(o.status) }}</span></td>
              <td style="font-size:12px;">{{ formatTime(o.occurredAt || o.createdAt) }}</td>
              <td>
                <button @click="viewDetail(o)" style="padding:3px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!orders.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无已完成工单</p>
        <div v-if="orders.length" style="display:flex;justify-content:flex-end;align-items:center;gap:12px;margin-top:12px;font-size:12px;color:#6b7280;">
          <span>共 {{ totalOrders }} 条</span>
          <button :disabled="page <= 1" @click="page--; loadData()" style="padding:2px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">上一页</button>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <button :disabled="page >= totalPages" @click="page++; loadData()" style="padding:2px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">下一页</button>
        </div>
      </div>
    </div>

    <!-- 事件详情弹窗（纯只读） -->
    <div v-if="detailEventId" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;" @click.self="detailEventId = null">
      <div style="width:960px;max-width:96vw;max-height:92vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
        <EventDetailView embedded :event-id="detailEventId" @close="detailEventId = null" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getEventSectionEvents } from '../api'
import EventDetailView from './EventDetailView.vue'

const loading = ref(false)
const orders = ref<any[]>([])
const filters = reactive({ workOrderStatus: '', urgencyLevel: '' })
const searchKey = ref('')
const totalOrders = ref(0)
const page = ref(1)
const pageSize = 10
const totalPages = ref(1)
const detailEventId = ref<string | number | null>(null)

function workOrderStatusLabel(status: string) {
  const map: Record<string, string> = {
    COMPLETED: '已完成',
    CLOSED: '已关闭'
  }
  return map[status] || status || '未知'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '已通过',
    AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单',
    WAITING_LEADER_REVIEW: '组长审核',
    DISPATCHED_TO_WORK_ORDER: '已派单',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

function formatTime(value: any) {
  if (!value) return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function viewDetail(o: any) {
  detailEventId.value = o.id
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.workOrderStatus) params.workOrderStatus = filters.workOrderStatus
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (searchKey.value.trim()) params.searchKey = searchKey.value.trim()
    const res: any = await getEventSectionEvents('completed', params)
    orders.value = res?.items || []
    totalOrders.value = res?.total || 0
    totalPages.value = Math.max(1, Math.ceil(totalOrders.value / pageSize))
  } catch (e) {
    console.error('加载已完成工单失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
