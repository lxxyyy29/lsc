<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">便民报修管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">查看和处理居民小程序端提交的报修申请</p>

    <!-- 状态筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:12px;flex-wrap:wrap;">
      <button v-for="s in statusTabs" :key="s.key"
        :style="activeStatus === s.key ? 'padding:6px 16px;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;cursor:pointer;' : 'padding:6px 16px;border:1px solid #e5e7eb;border-radius:6px;background:#fff;color:#374151;font-size:13px;cursor:pointer;'"
        @click="activeStatus = s.key; fetchData()">
        {{ s.label }}
      </button>
    </div>

    <!-- 类型 + 关键词筛选 -->
    <div style="display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
      <select v-model="activeType" @change="fetchData()"
        style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;outline:none;background:#fff;color:#374151;">
        <option value="">全部类型</option>
        <option v-for="t in typeOptions" :key="t.key" :value="t.key">{{ t.label }}</option>
      </select>
      <div style="display:flex;align-items:center;gap:6px;flex:1;max-width:360px;">
        <input v-model="keyword" placeholder="搜索标题/上报人..." @keyup.enter="fetchData()"
          style="flex:1;padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;outline:none;" />
        <button @click="fetchData()"
          style="padding:6px 14px;border:none;border-radius:6px;background:#0284c7;color:#fff;font-size:13px;cursor:pointer;">
          <i class="fas fa-search"></i> 搜索
        </button>
      </div>
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
          <thead><tr><th>标题</th><th>类型</th><th>上报人</th><th>联系电话</th><th>地址</th><th>上报时间</th><th>状态</th><th>处理结果</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td style="max-width:180px;">{{ item.title }}</td>
              <td><span class="tag tag-blue">{{ typeLabel(item.repairType) }}</span></td>
              <td>{{ item.reporterName || '-' }}</td>
              <td>{{ item.reporterPhone || '-' }}</td>
              <td style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ item.address || '-' }}</td>
              <td>{{ formatTime(item.createdAt) }}</td>
              <td><span :class="statusClass(item.status)">{{ statusLabel(item.status) }}</span></td>
              <td style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" :title="item.handleResult || ''">{{ item.handleResult || '-' }}</td>
              <td>
                <div style="display:flex;gap:6px;flex-wrap:wrap;">
                  <button @click="viewDetail(item)"
                    style="padding:4px 12px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;color:#374151;">
                    详情
                  </button>
                  <button v-if="item.status === 'PENDING'" @click="handleAccept(item)"
                    style="padding:4px 12px;border:none;border-radius:4px;background:#0284c7;color:#fff;font-size:12px;cursor:pointer;">
                    受理
                  </button>
                  <button v-if="item.status === 'PENDING'" @click="handleReject(item)"
                    style="padding:4px 12px;border:1px solid #ff4d4f;border-radius:4px;background:#fff;color:#ff4d4f;font-size:12px;cursor:pointer;">
                    驳回
                  </button>
                  <button v-if="item.status === 'ASSIGNED' || item.status === 'PROCESSING'" @click="handleComplete(item)"
                    style="padding:4px 12px;border:none;border-radius:4px;background:#059669;color:#fff;font-size:12px;cursor:pointer;">
                    完成
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无报修数据</p>
        <p v-if="total > list.length" style="text-align:center;padding:12px;color:#9ca3af;font-size:12px;">共 {{ total }} 条，仅显示前 {{ list.length }} 条</p>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const statusTabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待处理' },
  { key: 'ASSIGNED', label: '已派单' },
  { key: 'PROCESSING', label: '处理中' },
  { key: 'COMPLETED', label: '已完成' },
  { key: 'REJECTED', label: '已驳回' }
]

const typeOptions = [
  { key: 'WATER', label: '水电' },
  { key: 'ELEVATOR', label: '电梯' },
  { key: 'DOOR', label: '门禁' },
  { key: 'PIPE', label: '管道' },
  { key: 'ROOF', label: '屋顶' },
  { key: 'OTHER', label: '其他' }
]

const activeStatus = ref('')
const activeType = ref('')
const keyword = ref('')
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')

function statusLabel(s: string) {
  return { PENDING: '待处理', ASSIGNED: '已派单', PROCESSING: '处理中', COMPLETED: '已完成', REJECTED: '已驳回' }[s] || s || '未知'
}

function statusClass(s: string) {
  if (s === 'COMPLETED') return 'tag tag-green'
  if (s === 'ASSIGNED' || s === 'PROCESSING') return 'tag tag-blue'
  if (s === 'REJECTED') return 'tag tag-red'
  return 'tag tag-orange'
}

function typeLabel(t: string) {
  return typeOptions.find(o => o.key === t)?.label || t || '-'
}

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
    if (activeType.value) params.type = activeType.value
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    const res: any = await http.get('/repairs', { params })
    list.value = res?.items || []
    total.value = res?.total || 0
  } catch (e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function viewDetail(item: any) {
  alert(
    `报修单号：${item.id}\n标题：${item.title}\n类型：${typeLabel(item.repairType)}\n状态：${statusLabel(item.status)}\n` +
    `上报人：${item.reporterName || '-'}（${item.reporterPhone || '-'}）\n地址：${item.address || '-'}\n` +
    `描述：${item.description || '-'}\n上报时间：${formatTime(item.createdAt)}\n` +
    `处理人：${item.handlerName || '-'}\n处理时间：${formatTime(item.handledAt)}\n处理结果：${item.handleResult || '-'}`
  )
}

async function updateStatus(id: number, status: string, handleResult?: string) {
  try {
    await http.put(`/repairs/${id}/status`, { status, handleResult })
    alert('操作成功')
    fetchData()
  } catch (e: any) {
    alert(e?.message || '操作失败，请稍后重试')
  }
}

function handleAccept(item: any) {
  updateStatus(item.id, 'ASSIGNED')
}

function handleReject(item: any) {
  const reason = prompt(`驳回报修"${item.title}"，请填写驳回原因：`)
  if (reason === null) return
  if (!reason.trim()) { alert('请填写驳回原因'); return }
  updateStatus(item.id, 'REJECTED', reason.trim())
}

function handleComplete(item: any) {
  const result = prompt(`完成报修"${item.title}"，请填写处理结果：`)
  if (result === null) return
  if (!result.trim()) { alert('请填写处理结果'); return }
  updateStatus(item.id, 'COMPLETED', result.trim())
}

onMounted(fetchData)
</script>
