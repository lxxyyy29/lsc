<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">工单中心</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">工单派发、流转追踪、办结确认</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">工单总数</p>
        <p class="stat-value">{{ totalOrders }}</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">待接单</p>
        <p class="stat-value">{{ statusCount('WAITING_ACCEPT') }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">处理中</p>
        <p class="stat-value">{{ statusCount('PROCESSING') }}</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">待确认</p>
        <p class="stat-value">{{ statusCount('WAITING_CLOSE_CONFIRM') }}</p>
      </div>
      <div class="card card-border-blue">
        <p class="stat-label">已完成</p>
        <p class="stat-value">{{ statusCount('COMPLETED') }}</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;gap:8px;align-items:center;">
        <select v-model="statusFilter" @change="loadData" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
          <option value="">全部状态</option>
          <option value="WAITING_ACCEPT">待接单</option>
          <option value="PROCESSING">处理中</option>
          <option value="WAITING_VERIFY">待核实</option>
          <option value="NEEDS_MORE_EVIDENCE">需补充</option>
          <option value="WAITING_CLOSE_CONFIRM">待确认</option>
          <option value="COMPLETED">已完成</option>
          <option value="CLOSED">已关闭</option>
        </select>
        <input v-model="searchKey" placeholder="搜索工单号/事件标题..." style="flex:1;padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;outline:none;" @keyup.enter="loadData" />
        <button @click="loadData" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>
    </div>

    <!-- 工单列表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:8px;font-size:13px;">加载中...</p>
      </div>
      <div v-else>
        <table class="table">
          <thead>
            <tr>
              <th>工单号</th>
              <th>事件标题</th>
              <th>受派人</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in filteredOrders" :key="o.id || o.workOrderId">
              <td style="font-size:12px;">{{ o.workOrderNo || o.id || '-' }}</td>
              <td>{{ o.eventTitle || o.title || '-' }}</td>
              <td style="font-size:12px;">{{ o.assigneeName || o.assignee || '-' }}</td>
              <td><span :class="['tag', workOrderStatusClass(o.status)]">{{ workOrderStatusLabel(o.status) }}</span></td>
              <td style="font-size:12px;">{{ o.createdAt || o.created_at || '-' }}</td>
              <td>
                <button @click="viewDetail(o)" style="padding:2px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:11px;cursor:pointer;">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredOrders.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无工单数据</p>
      </div>
    </div>

    <!-- 工单详情对话框 -->
    <div v-if="selectedOrder" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:560px;max-height:80vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 style="font-size:16px;font-weight:600;">工单详情</h3>
          <button @click="selectedOrder = null" style="border:none;background:none;font-size:18px;cursor:pointer;color:#9ca3af;">&times;</button>
        </div>
        <div style="display:grid;gap:12px;font-size:13px;">
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div><span style="color:#6b7280;">工单号：</span>{{ selectedOrder.workOrderNo || selectedOrder.id }}</div>
            <div><span style="color:#6b7280;">状态：</span><span :class="['tag', workOrderStatusClass(selectedOrder.status)]">{{ workOrderStatusLabel(selectedOrder.status) }}</span></div>
          </div>
          <div><span style="color:#6b7280;">事件标题：</span>{{ selectedOrder.eventTitle || selectedOrder.title }}</div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div><span style="color:#6b7280;">受派人：</span>{{ selectedOrder.assigneeName || selectedOrder.assignee }}</div>
            <div><span style="color:#6b7280;">派单人：</span>{{ selectedOrder.dispatcherName || selectedOrder.dispatcher }}</div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div><span style="color:#6b7280;">创建时间：</span>{{ selectedOrder.createdAt || selectedOrder.created_at }}</div>
            <div><span style="color:#6b7280;">更新时间：</span>{{ selectedOrder.updatedAt || selectedOrder.updated_at }}</div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
            <div><span style="color:#6b7280;">截止时间：</span>{{ selectedOrder.deadline || selectedOrder.deadlineAt || '-' }}</div>
            <div><span style="color:#6b7280;">完成时间：</span>{{ selectedOrder.completedAt || selectedOrder.completed_at || '-' }}</div>
          </div>
          <div><span style="color:#6b7280;">备注：</span>{{ selectedOrder.remark || '-' }}</div>
          <!-- 附件列表 -->
          <div v-if="attachments.length">
            <span style="color:#6b7280;">附件：</span>
            <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:4px;">
              <div v-for="att in attachments" :key="att.id" style="display:flex;align-items:center;gap:4px;padding:4px 8px;background:#f3f4f6;border-radius:4px;font-size:12px;">
                <a v-if="att.fileUrl" :href="att.fileUrl" target="_blank" style="color:#1890ff;text-decoration:none;">{{ att.fileName }}</a>
                <span v-else>{{ att.fileName }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 关闭确认操作区 -->
        <div v-if="selectedOrder.status === 'WAITING_CLOSE_CONFIRM'" style="margin-top:16px;padding:12px;background:#fef3c7;border-radius:8px;border:1px solid #fcd34d;">
          <p style="font-size:13px;color:#92400e;margin-bottom:8px;">处置已完成，请确认是否关闭工单。</p>
          <textarea v-model="closeRemark" placeholder="备注（可选）" rows="2" style="width:100%;padding:8px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;"></textarea>
        </div>

        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button @click="selectedOrder = null" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭</button>
          <button v-if="selectedOrder.status === 'WAITING_CLOSE_CONFIRM'" @click="rejectClose(selectedOrder)" style="padding:6px 16px;border:1px solid #ef4444;border-radius:6px;background:#fff;color:#ef4444;font-size:13px;cursor:pointer;">驳回</button>
          <button v-if="selectedOrder.status === 'WAITING_CLOSE_CONFIRM'" @click="confirmClose(selectedOrder)" style="padding:6px 16px;border:none;border-radius:6px;background:#52c41a;color:#fff;font-size:13px;cursor:pointer;">确认关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getWorkOrders, confirmCloseWorkOrder, rejectCloseWorkOrder } from '../api'

const loading = ref(false)
const orders = ref<any[]>([])
const statusFilter = ref('')
const searchKey = ref('')
const totalOrders = ref(0)
const selectedOrder = ref<any>(null)
const closeRemark = ref('')
const attachments = ref<any[]>([])

const filteredOrders = computed(() => {
  if (!searchKey.value) return orders.value
  const key = searchKey.value.toLowerCase()
  return orders.value.filter(o =>
    (o.workOrderNo || '').toLowerCase().includes(key) ||
    (o.eventTitle || o.title || '').toLowerCase().includes(key) ||
    (o.id || '').toString().includes(key)
  )
})

function statusCount(status: string) {
  return orders.value.filter(o => o.status === status).length
}

function workOrderStatusLabel(status: string) {
  const map: any = {
    'WAITING_ACCEPT': '待接单',
    'PROCESSING': '处理中',
    'WAITING_VERIFY': '待核实',
    'NEEDS_MORE_EVIDENCE': '需补充',
    'WAITING_CLOSE_CONFIRM': '待确认',
    'COMPLETED': '已完成',
    'CLOSED': '已关闭'
  }
  return map[status] || status || '未知'
}

function workOrderStatusClass(status: string) {
  if (status === 'COMPLETED') return 'tag-green'
  if (status === 'PROCESSING') return 'tag-blue'
  if (status === 'WAITING_ACCEPT') return 'tag-orange'
  if (status === 'WAITING_CLOSE_CONFIRM') return 'tag-red'
  if (status === 'CLOSED') return 'tag-gray'
  return 'tag-orange'
}

function viewDetail(order: any) {
  selectedOrder.value = order
  closeRemark.value = ''
  attachments.value = []
  // 加载附件
  if (order.sourceEventId) {
    http.get(`/events/${order.sourceEventId}/attachments`).then((res: any) => {
      attachments.value = res || []
    }).catch(() => {})
  }
}

async function confirmClose(order: any) {
  if (!order) return
  try {
    await confirmCloseWorkOrder(order.id, closeRemark.value)
    selectedOrder.value = null
    loadData()
  } catch (e: any) {
    alert(e.message || '确认关闭失败')
  }
}

async function rejectClose(order: any) {
  if (!order) return
  if (!closeRemark.value.trim()) {
    alert('驳回时必须填写原因')
    return
  }
  try {
    await rejectCloseWorkOrder(order.id, closeRemark.value)
    selectedOrder.value = null
    loadData()
  } catch (e: any) {
    alert(e.message || '驳回失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getWorkOrders({
      status: statusFilter.value || undefined,
      page: 1,
      pageSize: 50
    })
    if (res && res.items) {
      orders.value = res.items
      totalOrders.value = res.total || res.items.length
    } else if (Array.isArray(res)) {
      orders.value = res
      totalOrders.value = res.length
    }
  } catch (e) {
    console.error('加载工单失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
