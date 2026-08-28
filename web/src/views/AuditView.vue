<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">事件审核</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">网格员手机端已处理完成的事件，在此进行 PC 端审核：通过后进入已完成工单，驳回后由网格员重新处理</p>

    <!-- 筛选栏 -->
    <div class="card" style="margin-bottom:16px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <select v-model="filters.workOrderStatus" class="filter-select" @change="loadData">
          <option value="">全部工单状态</option>
          <option value="WAITING_VERIFY">待核实</option>
          <option value="WAITING_CLOSE_CONFIRM">待确认</option>
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

    <!-- 审核列表 -->
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
              <th>上报时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in audits" :key="a.id">
              <td style="font-size:12px;">{{ a.eventCode }}</td>
              <td style="font-size:13px;">{{ a.title }}</td>
              <td style="font-size:12px;">{{ a.workOrderNo || '-' }}</td>
              <td><span :class="['tag', a.workOrderStatus === 'WAITING_CLOSE_CONFIRM' ? 'tag-red' : 'tag-orange']">{{ workOrderStatusLabel(a.workOrderStatus) }}</span></td>
              <td style="font-size:12px;">{{ a.assigneeName || '-' }}</td>
              <td style="font-size:12px;">{{ formatTime(a.occurredAt || a.createdAt) }}</td>
              <td>
                <div style="display:flex;flex-wrap:wrap;gap:4px;">
                  <button @click="viewDetail(a)" style="padding:3px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;">详情</button>
                  <button @click="auditAction(a, 'pass')" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;">通过</button>
                  <button @click="auditAction(a, 'reject')" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:12px;cursor:pointer;">驳回</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!audits.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无待审核数据</p>
        <div v-if="audits.length" style="display:flex;justify-content:flex-end;align-items:center;gap:12px;margin-top:12px;font-size:12px;color:#6b7280;">
          <span>共 {{ totalAudits }} 条</span>
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

    <!-- 审核确认弹窗 -->
    <div v-if="auditModal.visible" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:2000;">
      <div style="width:420px;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);">
        <h3 style="font-size:15px;font-weight:600;margin-bottom:4px;">{{ auditModal.action === 'pass' ? '审核通过' : '审核驳回' }}</h3>
        <p style="font-size:12px;color:#9ca3af;margin-bottom:14px;">{{ auditModal.action === 'pass' ? '通过后该事件将进入已完成工单' : '驳回后工单将退回网格员重新处理' }}</p>
        <textarea v-model="auditModal.remark" rows="3" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;box-sizing:border-box;" :placeholder="auditModal.action === 'pass' ? '请输入通过备注（可选）' : '请输入驳回原因（必填）'"></textarea>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px;">
          <button @click="auditModal.visible = false" class="btn btn-default">取消</button>
          <button @click="confirmAudit" class="btn" :style="{ background: auditModal.action === 'pass' ? '#52c41a' : '#ff4d4f', color: '#fff' }">确认{{ auditModal.action === 'pass' ? '通过' : '驳回' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getEventSectionEvents, confirmCloseWorkOrder, rejectCloseWorkOrder } from '../api'
import EventDetailView from './EventDetailView.vue'
import { showMessage } from '../utils/message'

const loading = ref(false)
const audits = ref<any[]>([])
const filters = reactive({ workOrderStatus: '', urgencyLevel: '' })
const searchKey = ref('')
const totalAudits = ref(0)
const page = ref(1)
const pageSize = 10
const totalPages = ref(1)
const detailEventId = ref<string | number | null>(null)

function workOrderStatusLabel(status: string) {
  const map: Record<string, string> = {
    WAITING_VERIFY: '待核实',
    WAITING_CLOSE_CONFIRM: '待确认'
  }
  return map[status] || status || '未知'
}

function formatTime(value: any) {
  if (!value) return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function viewDetail(a: any) {
  detailEventId.value = a.id
}

// 审核确认弹窗状态
const auditModal = reactive({ visible: false, workOrderId: 0, action: '', remark: '' })

function auditAction(a: any, action: string) {
  auditModal.workOrderId = a.workOrderId
  auditModal.action = action
  auditModal.remark = ''
  auditModal.visible = true
}

async function confirmAudit() {
  const { workOrderId, action, remark } = auditModal
  if (!workOrderId) { showMessage('该工单缺少 ID，无法审核'); return }
  if (action === 'reject' && !remark.trim()) { showMessage('请填写驳回原因', 'warning'); return }
  try {
    if (action === 'pass') {
      await confirmCloseWorkOrder(workOrderId, remark.trim())
    } else {
      await rejectCloseWorkOrder(workOrderId, remark.trim())
    }
    auditModal.visible = false
    showMessage(action === 'pass' ? '审核通过，事件进入已完成工单' : '已驳回，工单退回网格员重新处理', 'success')
    await loadData()
  } catch (e: any) {
    showMessage(e.message || '审核操作失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.workOrderStatus) params.workOrderStatus = filters.workOrderStatus
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (searchKey.value.trim()) params.searchKey = searchKey.value.trim()
    const res: any = await getEventSectionEvents('audit', params)
    audits.value = res?.items || []
    totalAudits.value = res?.total || 0
    totalPages.value = Math.max(1, Math.ceil(totalAudits.value / pageSize))
  } catch (e) {
    console.error('加载审核列表失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>
