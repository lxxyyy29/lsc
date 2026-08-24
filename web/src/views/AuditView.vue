<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">异常工单</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">事件审核：审核员对待审核事件进行通过 / 驳回，通过后进入待派单闭环处置</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-orange">
        <p class="stat-value">{{ stats.pending }}</p>
        <p class="stat-label">待审核</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-value">{{ stats.approved }}</p>
        <p class="stat-label">已通过</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-value">{{ stats.rejected }}</p>
        <p class="stat-label">已驳回</p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="card" style="margin-bottom:16px;">
      <div class="filter-bar" style="margin-bottom:0;">
        <select v-model="statusFilter" class="filter-select" @change="loadData">
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
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
              <th>审核状态</th>
              <th>审核人</th>
              <th>审核时间</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in audits" :key="a.id">
              <td style="font-size:12px;">{{ a.event_code }}</td>
              <td style="font-size:13px;">{{ a.title }}</td>
              <td><span :class="['tag', auditStatusClass(a.status)]">{{ auditStatusLabel(a.status) }}</span></td>
              <td style="font-size:12px;">{{ a.auditor_name || '-' }}</td>
              <td style="font-size:12px;">{{ formatTime(a.audit_time) }}</td>
              <td style="font-size:12px;">{{ formatTime(a.created_at) }}</td>
              <td>
                <button @click="viewDetail(a)" style="padding:2px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:11px;cursor:pointer;margin-right:4px;">详情</button>
                <button v-if="canAudit(a.status)" @click="auditAction(a, 'pass')" style="padding:2px 8px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:11px;cursor:pointer;margin-right:4px;">通过</button>
                <button v-if="canAudit(a.status)" @click="auditAction(a, 'reject')" style="padding:2px 8px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:11px;cursor:pointer;">驳回</button>
                <button v-if="a.status === 'AUDIT_REJECTED'" @click="auditAction(a, 'pass')" style="padding:2px 8px;border:1px solid #52c41a;border-radius:4px;background:#fff;color:#52c41a;font-size:11px;cursor:pointer;">重新通过</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!audits.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无审核数据</p>
        <div v-if="audits.length" style="display:flex;justify-content:flex-end;align-items:center;gap:12px;margin-top:12px;font-size:12px;color:#6b7280;">
          <span>共 {{ totalAudits }} 条</span>
          <button :disabled="page <= 1" @click="page--; loadData()" style="padding:2px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">上一页</button>
          <span>第 {{ page }} 页</span>
          <button :disabled="audits.length < pageSize" @click="page++; loadData()" style="padding:2px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">下一页</button>
        </div>
      </div>
    </div>

    <!-- 审核详情对话框 -->
    <div v-if="selectedAudit" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:560px;max-height:80vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 style="font-size:16px;font-weight:600;">审核详情</h3>
          <button @click="selectedAudit = null" style="border:none;background:none;font-size:18px;cursor:pointer;color:#9ca3af;">&times;</button>
        </div>
        <div style="display:grid;gap:12px;font-size:13px;">
          <div><span style="color:#6b7280;">事件编号：</span>{{ selectedAudit.event_code }}</div>
          <div><span style="color:#6b7280;">事件标题：</span>{{ selectedAudit.title }}</div>
          <div><span style="color:#6b7280;">审核状态：</span><span :class="['tag', auditStatusClass(selectedAudit.status)]">{{ auditStatusLabel(selectedAudit.status) }}</span></div>
          <div><span style="color:#6b7280;">审核人：</span>{{ selectedAudit.auditor_name || '-' }}</div>
          <div><span style="color:#6b7280;">审核意见：</span>{{ selectedAudit.audit_remark || '-' }}</div>
          <div><span style="color:#6b7280;">创建时间：</span>{{ formatTime(selectedAudit.created_at) }}</div>
          <div><span style="color:#6b7280;">更新时间：</span>{{ formatTime(selectedAudit.updated_at) }}</div>
        </div>
        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button @click="selectedAudit = null" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭</button>
        </div>
      </div>
    </div>

    <!-- 审核确认弹窗（取消仅关闭弹窗，确认才执行审核） -->
    <div v-if="auditModal.visible" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:2000;">
      <div style="width:420px;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);">
        <h3 style="font-size:15px;font-weight:600;margin-bottom:4px;">{{ auditModal.action === 'pass' ? '审核通过' : '审核驳回' }}</h3>
        <p style="font-size:12px;color:#9ca3af;margin-bottom:14px;">{{ auditModal.action === 'pass' ? '通过后事件将进入后续处置流程' : '驳回后事件将退回上报人' }}</p>
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
import { getAudits, auditEvent } from '../api'
import { showMessage } from '../utils/message'

const loading = ref(false)
const audits = ref<any[]>([])
const stats = ref({ pending: 0, approved: 0, rejected: 0 })
const statusFilter = ref('')
const searchKey = ref('')
const totalAudits = ref(0)
const page = ref(1)
const pageSize = 10
const selectedAudit = ref<any>(null)

/** 事件原始状态 → 审核归一化状态（待审核/已通过/已驳回） */
function auditStatusLabel(status: string) {
  if (status === 'PENDING_AUDIT' || status === 'IN_AUDIT') return '待审核'
  if (status === 'AUDIT_REJECTED') return '已驳回'
  return '已通过'
}

function auditStatusClass(status: string) {
  if (status === 'PENDING_AUDIT' || status === 'IN_AUDIT') return 'tag-orange'
  if (status === 'AUDIT_REJECTED') return 'tag-red'
  return 'tag-green'
}

function canAudit(status: string) {
  return status === 'PENDING_AUDIT' || status === 'IN_AUDIT'
}

function formatTime(value: any) {
  if (!value) return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function viewDetail(audit: any) {
  selectedAudit.value = audit
}

// 审核确认弹窗状态：取消仅关闭弹窗，不执行任何审核操作
const auditModal = reactive({ visible: false, id: 0, action: '', remark: '' })

function auditAction(audit: any, action: string) {
  auditModal.id = audit.id
  auditModal.action = action
  auditModal.remark = ''
  auditModal.visible = true
}

async function confirmAudit() {
  const { id, action, remark } = auditModal
  if (action === 'reject' && !remark.trim()) { showMessage('请填写驳回原因', 'warning'); return }
  try {
    await auditEvent(id, action, remark.trim() || undefined)
    auditModal.visible = false
    showMessage(action === 'pass' ? '已通过，事件进入待派单' : '已驳回', 'success')
    await loadData()
  } catch (e: any) {
    showMessage(e.message || '审核操作失败')
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, pageSize }
    if (statusFilter.value) params.status = statusFilter.value
    if (searchKey.value.trim()) params.searchKey = searchKey.value.trim()
    const res: any = await getAudits(params)
    audits.value = res?.items || []
    totalAudits.value = res?.total || 0
    if (res?.stats) stats.value = res.stats
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