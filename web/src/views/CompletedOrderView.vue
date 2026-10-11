<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">已完成工单</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">
      已通过 PC 端审核、办结关闭的事件工单，仅作归档查阅
      <span v-if="overallStats" style="margin-left:12px;color:#fa8c16;">
        评价统计：{{ overallStats.total ?? overallStats.count ?? 0 }} 条，平均 {{ overallStats.averageScore ?? overallStats.avgScore ?? '-' }} 分
      </span>
    </p>

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
                <div style="display:flex;gap:6px;">
                  <button @click="viewDetail(o)" style="padding:3px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;">详情</button>
                  <!-- 评价：缺陷 BUG-14「没有事件评价功能」，已关闭事件可评价/查看评价 -->
                  <button v-if="o.status === 'CLOSED'" @click="openRate(o)" style="padding:3px 8px;border:1px solid #fa8c16;border-radius:4px;background:#fff;color:#fa8c16;font-size:12px;cursor:pointer;">评价</button>
                </div>
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

    <!-- 事件评价弹窗（缺陷 BUG-14）：1-5 分评价；已有评价时展示评价内容与该事件评价统计 -->
    <div v-if="rateVisible" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1001;" @click.self="rateVisible = false">
      <div style="width:460px;max-width:94vw;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:4px;">事件评价</h3>
        <p style="font-size:12px;color:#6b7280;margin-bottom:14px;">{{ rateTarget?.title || '' }}</p>

        <div v-if="rateLoading" style="text-align:center;padding:24px;color:#9ca3af;">加载中...</div>
        <template v-else>
          <!-- 已有评价：可查询评价内容与统计 -->
          <div v-if="existingRating" style="padding:12px;border:1px solid #e5e7eb;border-radius:8px;background:#fafafa;">
            <div style="font-size:13px;">本次评分：<strong style="color:#fa8c16;">{{ existingRating.score }} 分</strong></div>
            <div v-if="existingRating.content" style="font-size:12px;color:#374151;margin-top:6px;">{{ existingRating.content }}</div>
            <div v-if="rateStats" style="font-size:12px;color:#6b7280;margin-top:6px;">
              评价统计：共 {{ rateStats.total ?? rateStats.count ?? '-' }} 条，平均 {{ rateStats.averageScore ?? rateStats.avgScore ?? '-' }} 分
            </div>
          </div>

          <!-- 未评价：填写评分 -->
          <div v-else>
            <div style="font-size:13px;margin-bottom:6px;">满意度评分（1-5 分）</div>
            <div style="display:flex;align-items:center;gap:4px;margin-bottom:12px;">
              <span
                v-for="n in 5"
                :key="n"
                @click="rateForm.score = n"
                :style="{ cursor:'pointer', fontSize:'24px', lineHeight:'1', color: n <= rateForm.score ? '#fa8c16' : '#e5e7eb' }"
              >★</span>
              <span style="font-size:12px;color:#6b7280;margin-left:8px;">{{ rateForm.score ? rateForm.score + ' 分' : '请选择' }}</span>
            </div>
            <textarea
              v-model="rateForm.content"
              rows="3"
              placeholder="补充评价内容（选填）"
              style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;box-sizing:border-box;"
            ></textarea>
          </div>
        </template>

        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px;">
          <button @click="rateVisible = false" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭</button>
          <button
            v-if="!existingRating && !rateLoading"
            @click="submitRate"
            :disabled="rateSubmitting"
            style="padding:6px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;"
          >{{ rateSubmitting ? '提交中...' : '提交评价' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getEventSectionEvents, submitRating, getEventRatings, getEventRatingStats, getOverallRatingStats } from '../api'
import EventDetailView from './EventDetailView.vue'
import { showMessage } from '../utils/message'

// ===== 事件评价（缺陷 BUG-14：已关闭事件增加评价功能） =====
const overallStats = ref<any>(null)
const rateVisible = ref(false)
const rateLoading = ref(false)
const rateSubmitting = ref(false)
const rateTarget = ref<any>(null)
const existingRating = ref<any>(null)
const rateStats = ref<any>(null)
const rateForm = reactive({ score: 0, content: '' })

/** 打开展评：已有评价则直接展示（可查询评价），否则进入填写态 */
async function openRate(o: any) {
  rateTarget.value = o
  rateForm.score = 0
  rateForm.content = ''
  existingRating.value = null
  rateStats.value = null
  rateVisible.value = true
  rateLoading.value = true
  try {
    const res: any = await getEventRatings(Number(o.id))
    const list: any[] = Array.isArray(res) ? res : (res?.items || (res ? [res] : []))
    existingRating.value = list.length ? list[0] : null
    if (existingRating.value) {
      try { rateStats.value = await getEventRatingStats(Number(o.id)) } catch (e) { /* 统计失败不影响查看评价 */ }
    }
  } catch (e) {
    // 无评价或接口异常：直接进入填写态
  } finally {
    rateLoading.value = false
  }
}

async function submitRate() {
  // 需求：评分超范围时提示"评分必须在1-5之间"
  const score = Number(rateForm.score)
  if (!Number.isFinite(score) || score < 1 || score > 5) {
    showMessage('评分必须在1-5之间', 'warning')
    return
  }
  rateSubmitting.value = true
  try {
    await submitRating({
      eventId: Number(rateTarget.value?.id),
      score,
      content: rateForm.content.trim() || undefined
    })
    showMessage('评价提交成功', 'success')
    rateVisible.value = false
    await loadData()
  } catch (e: any) {
    showMessage(e?.message || '评价提交失败')
  } finally {
    rateSubmitting.value = false
  }
}

async function loadOverallStats() {
  try {
    overallStats.value = await getOverallRatingStats()
  } catch (e) { /* 无统计数据时头部不显示 */ }
}

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
  loadOverallStats()
})
</script>
