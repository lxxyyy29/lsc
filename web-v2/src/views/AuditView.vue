<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">审核中心</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">事件审核、流程节点审批、转工单</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">审核总数</p>
        <p class="stat-value">{{ totalAudits }}</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">待审核</p>
        <p class="statusCount('PENDING')"></p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">已通过</p>
        <p class="statusCount('APPROVED')"></p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">已驳回</p>
        <p class="statusCount('REJECTED')"></p>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="card" style="margin-bottom:16px;">
      <div style="display:flex;gap:8px;align-items:center;">
        <select v-model="statusFilter" @change="loadData" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
          <option value="">全部状态</option>
          <option value="PENDING">待审核</option>
          <option value="APPROVED">已通过</option>
          <option value="REJECTED">已驳回</option>
          <option value="WAITING_DISPATCH">待派单</option>
        </select>
        <input v-model="searchKey" placeholder="搜索事件编号/标题..." style="flex:1;padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;outline:none;" @keyup.enter="loadData" />
        <button @click="loadData" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;">
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
              <th>流程模板</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in filteredAudits" :key="a.id || a.eventId">
              <td style="font-size:12px;">{{ a.eventCode || a.event_code || '-' }}</td>
              <td>{{ a.eventTitle || a.title || '-' }}</td>
              <td><span :class="['tag', auditStatusClass(a.status)]">{{ auditStatusLabel(a.status) }}</span></td>
              <td style="font-size:12px;">{{ a.templateName || a.processTemplateName || '-' }}</td>
              <td style="font-size:12px;">{{ a.createdAt || a.created_at || '-' }}</td>
              <td>
                <button @click="viewDetail(a)" style="padding:2px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:11px;cursor:pointer;">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredAudits.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无审核数据</p>
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
          <div><span style="color:#6b7280;">事件编号：</span>{{ selectedAudit.eventCode || selectedAudit.event_code }}</div>
          <div><span style="color:#6b7280;">事件标题：</span>{{ selectedAudit.eventTitle || selectedAudit.title }}</div>
          <div><span style="color:#6b7280;">审核状态：</span><span :class="['tag', auditStatusClass(selectedAudit.status)]">{{ auditStatusLabel(selectedAudit.status) }}</span></div>
          <div><span style="color:#6b7280;">流程模板：</span>{{ selectedAudit.templateName || selectedAudit.processTemplateName }}</div>
          <div><span style="color:#6b7280;">创建时间：</span>{{ selectedAudit.createdAt || selectedAudit.created_at }}</div>
        </div>
        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button @click="selectedAudit = null" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getEvents } from '../api'

const loading = ref(false)
const audits = ref<any[]>([])
const statusFilter = ref('')
const searchKey = ref('')
const totalAudits = ref(0)
const selectedAudit = ref<any>(null)

const filteredAudits = computed(() => {
  if (!searchKey.value) return audits.value
  const key = searchKey.value.toLowerCase()
  return audits.value.filter(a =>
    (a.eventCode || a.event_code || '').toLowerCase().includes(key) ||
    (a.eventTitle || a.title || '').toLowerCase().includes(key)
  )
})

function statusCount(status: string) {
  return audits.value.filter(a => a.status === status).length
}

function auditStatusLabel(status: string) {
  const map: any = {
    'PENDING': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'WAITING_DISPATCH': '待派单',
    'IN_PROGRESS': '审核中'
  }
  return map[status] || status || '未知'
}

function auditStatusClass(status: string) {
  if (status === 'APPROVED') return 'tag-green'
  if (status === 'PENDING' || status === 'IN_PROGRESS') return 'tag-orange'
  if (status === 'REJECTED') return 'tag-red'
  if (status === 'WAITING_DISPATCH') return 'tag-blue'
  return 'tag-orange'
}

function viewDetail(audit: any) {
  selectedAudit.value = audit
}

async function loadData() {
  loading.value = true
  try {
    // 审核中心展示事件列表中需要审核/已审核的事件
    const params: any = { page: 1, pageSize: 50 }
    if (statusFilter.value) params.status = statusFilter.value
    const res: any = await getEvents(params)
    if (res && res.items) {
      audits.value = res.items
      totalAudits.value = res.total || res.items.length
    } else if (Array.isArray(res)) {
      audits.value = res
      totalAudits.value = res.length
    }
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
