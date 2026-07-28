<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">停车管理</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">车位资源、违停预警、车流监测</p>

    <!-- 车位统计 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-parking" style="font-size:20px;color:#1890FF;"></i>
          <div>
            <p class="stat-label">总车位</p>
            <p class="stat-value">{{ stats.total || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-green">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-check-circle" style="font-size:20px;color:#52C41A;"></i>
          <div>
            <p class="stat-label">空闲</p>
            <p class="stat-value">{{ stats.free || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-car" style="font-size:20px;color:#FAAD14;"></i>
          <div>
            <p class="stat-label">已占用</p>
            <p class="stat-value">{{ stats.occupied || 0 }}</p>
          </div>
        </div>
      </div>
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;gap:12px;">
          <i class="fas fa-fire" style="font-size:20px;color:#FF4D4F;"></i>
          <div>
            <p class="stat-label">消防通道</p>
            <p class="stat-value">{{ stats.fireLane || 0 }}</p>
          </div>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
      <!-- 车位列表 -->
      <div class="card">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
          <h3 style="font-size:14px;font-weight:600;">车位列表</h3>
          <select v-model="spaceFilter" @change="loadSpaces" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
            <option value="">全部</option>
            <option value="FREE">空闲</option>
            <option value="OCCUPIED">已占用</option>
          </select>
        </div>
        <table class="table">
          <thead><tr><th>编号</th><th>类型</th><th>位置</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="s in spaces" :key="s.id">
              <td style="font-size:12px;">{{ s.space_code }}</td>
              <td><span class="tag" :class="spaceTypeClass(s.space_type)">{{ spaceTypeLabel(s.space_type) }}</span></td>
              <td style="font-size:12px;">{{ s.address || '-' }}</td>
              <td><span class="tag" :class="s.status === 'FREE' ? 'tag-green' : 'tag-orange'">{{ s.status === 'FREE' ? '空闲' : '占用' }}</span></td>
            </tr>
          </tbody>
        </table>
        <p v-if="!spaces.length" style="font-size:12px;color:#9ca3af;text-align:center;padding:20px;">暂无车位</p>
      </div>

      <!-- 违停统计 -->
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">违停统计</h3>
        <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-bottom:16px;">
          <div style="text-align:center;padding:12px;background:#fff7e6;border-radius:8px;">
            <p style="font-size:20px;font-weight:800;color:#faad14;">{{ violationStats.pending || 0 }}</p>
            <p style="font-size:11px;color:#6b7280;">待处理</p>
          </div>
          <div style="text-align:center;padding:12px;background:#e6f4ff;border-radius:8px;">
            <p style="font-size:20px;font-weight:80px;color:#1890ff;">{{ violationStats.dispatched || 0 }}</p>
            <p style="font-size:11px;color:#6b7280;">已派单</p>
          </div>
          <div style="text-align:center;padding:12px;background:#f6ffed;border-radius:8px;">
            <p style="font-size:20px;font-weight:800;color:#52c41a;">{{ violationStats.closed || 0 }}</p>
            <p style="font-size:11px;color:#6b7280;">已处理</p>
          </div>
          <div style="text-align:center;padding:12px;background:#fff1f0;border-radius:8px;">
            <p style="font-size:20px;font-weight:800;color:#ff4d4f;">{{ violationStats.total || 0 }}</p>
            <p style="font-size:11px;color:#6b7280;">总计</p>
          </div>
        </div>
        <div v-for="item in violationStats.byType" :key="item.type" style="display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #f3f4f6;">
          <span style="font-size:12px;">{{ violationTypeLabel(item.type) }}</span>
          <span style="font-size:12px;font-weight:600;">{{ item.count }}</span>
        </div>
      </div>
    </div>

    <!-- 违停记录 -->
    <div class="card">
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;">
        <h3 style="font-size:14px;font-weight:600;">违停预警列表</h3>
        <select v-model="violationFilter" @change="loadViolations" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
          <option value="">全部状态</option>
          <option value="PENDING">待处理</option>
          <option value="DISPATCHED">已派单</option>
          <option value="PROCESSING">处理中</option>
          <option value="CLOSED">已关闭</option>
        </select>
      </div>
      <table class="table">
        <thead><tr><th>时间</th><th>位置</th><th>类型</th><th>车牌</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="v in violations" :key="v.id">
            <td style="font-size:12px;">{{ formatTime(v.occurred_at) }}</td>
            <td style="font-size:12px;">{{ v.address || '-' }}</td>
            <td><span class="tag" :class="violationTypeClass(v.violation_type)">{{ violationTypeLabel(v.violation_type) }}</span></td>
            <td style="font-size:12px;">{{ v.vehicle_plate || '-' }}</td>
            <td><span class="tag" :class="statusClass(v.status)">{{ statusLabel(v.status) }}</span></td>
            <td>
              <button v-if="v.status === 'PENDING'" @click="handleDispatch(v)" style="padding:4px 10px;font-size:12px;border:none;border-radius:4px;background:#1890ff;color:#fff;cursor:pointer;">派单</button>
              <button v-if="v.status === 'DISPATCHED'" @click="handleClose(v.id)" style="padding:4px 10px;font-size:12px;border:none;border-radius:4px;background:#52c41a;color:#fff;cursor:pointer;">结案</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!violations.length" style="font-size:12px;color:#9ca3af;text-align:center;padding:20px;">暂无违停记录</p>
    </div>

    <!-- 派单弹窗 -->
    <div v-if="showDispatch" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:24px;width:400px;max-width:90vw;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">派单处理</h3>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">选择网格员</label>
          <select v-model="dispatchForm.dispatcherId" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
            <option :value="null">请选择</option>
            <option v-for="u in workers" :key="u.id" :value="u.id">{{ u.realName }} ({{ u.username }})</option>
          </select>
        </div>
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">备注</label>
          <textarea v-model="dispatchForm.remark" rows="2" placeholder="处理备注..." style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;"></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showDispatch = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="submitDispatch" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">确认派单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const stats = ref<any>({})
const spaces = ref<any[]>([])
const violations = ref<any[]>([])
const violationStats = ref<any>({})
const workers = ref<any[]>([])
const spaceFilter = ref('')
const violationFilter = ref('')
const showDispatch = ref(false)
const currentViolation = ref<any>(null)
const dispatchForm = ref({ dispatcherId: null as number | null, remark: '' })

function spaceTypeLabel(type: string) {
  const map: Record<string, string> = { NORMAL: '普通', DISABLED: '无障碍', CHARGING: '充电', FIRE_LANE: '消防通道' }
  return map[type] || type
}
function spaceTypeClass(type: string) {
  if (type === 'FIRE_LANE') return 'tag-red'
  if (type === 'CHARGING') return 'tag-blue'
  if (type === 'DISABLED') return 'tag-green'
  return 'tag-blue'
}
function violationTypeLabel(type: string) {
  const map: Record<string, string> = { ILLEGAL_PARKING: '违停', FIRE_LANE: '消防通道', OCCUPYING: '霸占车位', OVERSTAY: '超时停放' }
  return map[type] || type
}
function violationTypeClass(type: string) {
  if (type === 'FIRE_LANE') return 'tag-red'
  if (type === 'ILLEGAL_PARKING') return 'tag-orange'
  return 'tag-blue'
}
function statusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待处理', DISPATCHED: '已派单', PROCESSING: '处理中', CLOSED: '已关闭' }
  return map[status] || status
}
function statusClass(status: string) {
  if (status === 'CLOSED') return 'tag-green'
  if (status === 'DISPATCHED') return 'tag-blue'
  if (status === 'PROCESSING') return 'tag-orange'
  return 'tag-red'
}
function formatTime(ts: string) {
  if (!ts) return '-'
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

async function loadStats() {
  try { stats.value = await http.get('/parking/statistics') || {} } catch (e) {}
}
async function loadSpaces() {
  try {
    const params: any = {}
    if (spaceFilter.value) params.status = spaceFilter.value
    spaces.value = await http.get('/parking/spaces', { params }) || []
  } catch (e) {}
}
async function loadViolations() {
  try {
    const params: any = {}
    if (violationFilter.value) params.status = violationFilter.value
    violations.value = await http.get('/parking/violations', { params }) || []
  } catch (e) {}
}
async function loadViolationStats() {
  try { violationStats.value = await http.get('/parking/violation-stats') || {} } catch (e) {}
}
async function loadWorkers() {
  try { workers.value = await http.get('/community/org-members') || [] } catch (e) {}
}

function handleDispatch(v: any) {
  currentViolation.value = v
  showDispatch.value = true
}

async function submitDispatch() {
  if (!dispatchForm.value.dispatcherId) { alert('请选择网格员'); return }
  try {
    await http.post(`/parking/violations/${currentViolation.value.id}/dispatch`, dispatchForm.value)
    showDispatch.value = false
    loadViolations()
    loadViolationStats()
  } catch (e: any) { alert(e?.message || '派单失败') }
}

async function handleClose(id: number) {
  try {
    await http.put(`/parking/violations/${id}/status`, { status: 'CLOSED' })
    loadViolations()
    loadViolationStats()
  } catch (e: any) { alert(e?.message || '操作失败') }
}

onMounted(() => {
  loadStats()
  loadSpaces()
  loadViolations()
  loadViolationStats()
  loadWorkers()
})
</script>
