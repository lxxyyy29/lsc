<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">事件闭环处置</h2>
        <p style="font-size:13px;color:#6b7280;">发现上报→智能派单→现场处置→复核核查→归档</p>
      </div>
      <button @click="$router.push('/events/create')" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-plus" style="margin-right:4px;"></i>创建事件
      </button>
    </div>

    <div class="card">
      <!-- 筛选栏 -->
      <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;">
        <select v-model="filters.status" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option value="">全部状态</option>
          <option value="PENDING_AUDIT">待审核</option>
          <option value="IN_AUDIT">审核中</option>
          <option value="WAITING_DISPATCH">待派单</option>
          <option value="DISPATCHED_TO_WORK_ORDER">已派单</option>
          <option value="CLOSED">已关闭</option>
          <option value="IGNORED">已忽略</option>
        </select>
        <select v-model="filters.urgencyLevel" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option value="">全部紧急程度</option>
          <option value="GREEN">一般（绿）</option>
          <option value="YELLOW">重点（黄）</option>
          <option value="RED">紧急（红）</option>
        </select>
        <input v-model="filters.startDate" type="date" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        <input v-model="filters.endDate" type="date" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        <button @click="loadData" style="padding:6px 14px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">查询</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="loadData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>

      <!-- 数据表格 -->
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>标题</th>
              <th>状态</th>
              <th>紧急程度</th>
              <th>上报时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in list" :key="e.id">
              <td style="font-size:12px;color:#6b7280;">{{ e.eventCode }}</td>
              <td>{{ e.title }}</td>
              <td>
                <span :class="['tag', e.currentStatus === 'CLOSED' ? 'tag-green' : e.currentStatus === 'DISPATCHED_TO_WORK_ORDER' ? 'tag-blue' : 'tag-orange']">
                  {{ statusLabel(e.currentStatus) }}
                </span>
              </td>
              <td>
                <span :class="['tag', e.urgencyLevel === 'RED' ? 'tag-red' : e.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']">
                  {{ e.urgencyLevel === 'RED' ? '紧急' : e.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ e.occurredAt }}</td>
              <td>
                <button @click="goDetail(e)" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;margin-right:4px;">详情</button>
                <button v-if="e.currentStatus === 'PENDING_AUDIT'" @click="handleAudit(e.id, 'pass')" style="padding:4px 10px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;margin-right:4px;">通过</button>
                <button v-if="e.currentStatus === 'PENDING_AUDIT'" @click="handleAudit(e.id, 'reject')" style="padding:4px 10px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:12px;cursor:pointer;margin-right:4px;">驳回</button>
                <button v-if="['WAITING_DISPATCH', 'IN_AUDIT'].includes(e.currentStatus)" @click="goDetail(e)" style="padding:4px 10px;border:none;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">派单</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!list.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无事件</p>

        <!-- 分页 -->
        <div v-if="totalPages > 1" style="display:flex;align-items:center;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid #e5e7eb;">
          <span style="font-size:13px;color:#6b7280;">共 {{ total }} 条</span>
          <div style="display:flex;gap:6px;">
            <button @click="page = 1" :disabled="page === 1" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">首页</button>
            <button @click="page--" :disabled="page === 1" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">上一页</button>
            <span style="font-size:13px;color:#374151;margin:0 8px;">第 <strong>{{ page }}</strong> / {{ totalPages }} 页</span>
            <button @click="page++" :disabled="page === totalPages" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">下一页</button>
            <button @click="page = totalPages" :disabled="page === totalPages" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">末页</button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getEvents, auditEvent } from '../api'

const router = useRouter()

const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = ref(0)

const filters = reactive({
  status: '',
  urgencyLevel: '',
  startDate: '',
  endDate: '',
})

function goDetail(e: any) {
  const id = e.id || e.externalEventId
  router.push(`/events/${id}`)
}

async function handleAudit(id: number, action: string) {
  const remark = action === 'pass' ? prompt('请输入通过备注（可选）') : prompt('请输入驳回原因')
  if (action === 'reject' && !remark) { alert('请填写驳回原因'); return }
  try {
    await auditEvent(id, action, remark || '')
    loadData()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    WAITING_DISPATCH: '待派单',
    DISPATCHED_TO_WORK_ORDER: '已派单',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.status) params.status = filters.status
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (filters.startDate) params.startDate = filters.startDate
    if (filters.endDate) params.endDate = filters.endDate
    const result = await getEvents(params)
    list.value = result?.items || result?.list || []
    total.value = result?.total || list.value.length
    totalPages.value = Math.max(1, Math.ceil(total.value / pageSize))
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => filters.status, () => { page.value = 1; loadData() })
watch(() => filters.urgencyLevel, () => { page.value = 1; loadData() })

onMounted(loadData)
</script>
