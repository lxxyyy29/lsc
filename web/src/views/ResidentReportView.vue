<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">居民上报记录</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">居民上报统一归口至事件闭环处理中心，本页展示 sourceSystem=PUBLIC_REPORT 的全部事件，处置派单请在事件中心操作</p>

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
          <thead><tr><th>标题</th><th>类型</th><th>上报时间</th><th>事件编号</th><th>状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.title }}</td>
              <td><span class="tag tag-blue">{{ getEventTypeName(item.eventType) }}</span></td>
              <td>{{ formatTime(item.occurredAt) }}</td>
              <td>
                <span style="font-size:12px;color:#6b7280;font-family:monospace;">{{ item.eventCode || '-' }}</span>
              </td>
              <td>
                <span :class="statusClass(item.currentStatus || item.status)">{{ statusLabel(item.currentStatus || item.status) }}</span>
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

    <!-- 详情弹窗 -->
    <div v-if="detailVisible" class="detail-modal-overlay" @click.self="closeDetail">
      <div class="detail-modal-box">
        <div class="detail-modal-header">
          <h3 style="margin:0;font-size:16px;font-weight:600;">上报详情</h3>
          <button @click="closeDetail" style="background:none;border:none;font-size:20px;cursor:pointer;color:#6b7280;padding:0;line-height:1;">&times;</button>
        </div>
        <div class="detail-modal-body" v-if="currentItem">
          <div class="detail-row"><span class="detail-label">标题</span><span class="detail-value">{{ currentItem.title }}</span></div>
          <div class="detail-row"><span class="detail-label">类型</span><span class="detail-value"><span class="tag tag-blue">{{ getEventTypeName(currentItem.eventType) }}</span></span></div>
          <div class="detail-row"><span class="detail-label">状态</span><span class="detail-value"><span :class="statusClass(currentItem.currentStatus || currentItem.status)">{{ statusLabel(currentItem.currentStatus || currentItem.status) }}</span></span></div>
          <div class="detail-row"><span class="detail-label">描述</span><span class="detail-value">{{ currentItem.description || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">事件编号</span><span class="detail-value" style="font-family:monospace;font-size:12px;">{{ currentItem.eventCode || '-' }}</span></div>
          <div class="detail-row"><span class="detail-label">上报位置</span><span class="detail-value">{{ currentItem.location || currentItem.area || '-' }}</span></div>
          <div class="detail-row" v-if="currentItem.longitude && currentItem.latitude"><span class="detail-label">坐标</span><span class="detail-value" style="font-family:monospace;font-size:12px;">{{ currentItem.longitude }}, {{ currentItem.latitude }}</span></div>
          <div class="detail-row"><span class="detail-label">上报时间</span><span class="detail-value">{{ formatTime(currentItem.occurredAt) }}</span></div>
          <div v-if="currentItem.urgencyLevel" class="detail-row"><span class="detail-label">紧急程度</span><span class="detail-value">{{ urgencyLabel(currentItem.urgencyLevel) }}</span></div>
          <div v-if="currentItem.evidenceReferences && currentItem.evidenceReferences.length" class="detail-row"><span class="detail-label">现场图片</span><span class="detail-value">
            <div v-for="(url, idx) in currentItem.evidenceReferences" :key="idx" style="margin-bottom:6px;">
              <a :href="url" target="_blank" style="color:#0284c7;font-size:12px;text-decoration:none;"><i class="fas fa-image"></i> 图片 {{ idx + 1 }}</a>
            </div>
          </span></div>
          <div class="detail-row"><span class="detail-label">关联事件</span><span class="detail-value"><a href="javascript:void(0)" @click="goEvent(currentItem); closeDetail()" style="color:#0284c7;text-decoration:none;"><i class="fas fa-link"></i> 查看事件详情（去事件中心派单/处置）</a></span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api'
import { getEventTypeName } from '../utils/eventTypes'
import { showMessage } from '../utils/message'

const router = useRouter()

// 状态 tab 使用事件状态枚举（EventStatus），与事件中心一致
const statusTabs = [
  { key: '', label: '全部' },
  { key: 'WAITING_DISPATCH', label: '待派发' },
  { key: 'DISPATCHED_TO_WORK_ORDER', label: '已派发' },
  { key: 'CLOSED', label: '已关闭' },
  { key: 'IGNORED', label: '已忽略' }
]
const activeStatus = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const detailVisible = ref(false)
const currentItem = ref<any>(null)

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '审核通过',
    AUDIT_REJECTED: '审核驳回',
    WAITING_DISPATCH: '待派发',
    DISPATCHED_TO_WORK_ORDER: '已派发',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[s] || s || '未知'
}

function statusClass(s: string) {
  if (s === 'CLOSED') return 'tag tag-green'
  if (s === 'IGNORED') return 'tag tag-red'
  if (s === 'AUDIT_REJECTED') return 'tag tag-red'
  if (s === 'DISPATCHED_TO_WORK_ORDER') return 'tag tag-blue'
  if (s === 'AUDIT_APPROVED') return 'tag tag-green'
  if (s === 'IN_AUDIT' || s === 'PENDING_AUDIT') return 'tag tag-orange'
  return 'tag tag-orange'
}

function urgencyLabel(u: string) {
  const map: Record<string, string> = { GREEN: '一般(绿)', YELLOW: '重点(黄)', RED: '紧急(红)' }
  return map[u] || u || '-'
}

function formatTime(t: string) {
  if (!t) return '-'
  try { return new Date(t).toLocaleString('zh-CN') } catch { return t }
}

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    // 改调 /events 接口，按 sourceSystem=PUBLIC_REPORT 过滤，includeArchived=true 显示所有状态（含已派发/已忽略）
    const params: any = {
      sourceSystem: 'PUBLIC_REPORT',
      includeArchived: true,
      page: 1,
      size: 100
    }
    if (activeStatus.value) params.status = activeStatus.value
    const res = await http.get('/events', { params })
    // /events 返回 { data: { items: [...], total } }
    const data: any = (res as any)?.data ?? res
    list.value = data?.items || []
  } catch(e: any) {
    error.value = e?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function viewDetail(item: any) {
  currentItem.value = item
  detailVisible.value = true
}

function closeDetail() {
  detailVisible.value = false
  currentItem.value = null
}

// 跳转到事件详情，事件 id 即为 item.id
function goEvent(item: any) {
  router.push('/events/' + item.id)
}

onMounted(fetchData)
</script>

<style scoped>
.detail-modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-modal-box {
  background: #fff;
  border-radius: 12px;
  width: 520px;
  max-width: 90vw;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1), 0 10px 10px -5px rgba(0,0,0,0.04);
}

.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-modal-body {
  padding: 20px;
}

.detail-row {
  display: flex;
  margin-bottom: 14px;
  line-height: 1.6;
}

.detail-label {
  width: 80px;
  flex-shrink: 0;
  color: #6b7280;
  font-size: 13px;
}

.detail-value {
  flex: 1;
  color: #1f2937;
  font-size: 13px;
  word-break: break-all;
}
</style>
