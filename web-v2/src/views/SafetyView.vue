<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">安全风险防控</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">九小场所隐患监管、三色管控、整改跟踪</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <p class="stat-label">总检查数</p>
        <p class="stat-value">{{ stats.total || 0 }}</p>
      </div>
      <div class="card card-border-orange">
        <p class="stat-label">待整改</p>
        <p class="stat-value">{{ stats.pending || 0 }}</p>
      </div>
      <div class="card card-border-blue">
        <p class="stat-label">整改中</p>
        <p class="stat-value">{{ stats.inProgress || 0 }}</p>
      </div>
      <div class="card card-border-green">
        <p class="stat-label">已完成</p>
        <p class="stat-value">{{ stats.completed || 0 }}</p>
      </div>
      <div class="card card-border-red">
        <p class="stat-label">已超期</p>
        <p class="stat-value">{{ stats.overdue || 0 }}</p>
      </div>
    </div>

    <!-- 操作栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;">
      <button @click="showCreate = true" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-plus" style="margin-right:4px;"></i>新增检查
      </button>
      <button @click="markOverdue" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-exclamation" style="margin-right:4px;"></i>标记超期
      </button>
    </div>

    <!-- 检查记录表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="loadData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>场所</th>
              <th>消防风险</th>
              <th>安全状态</th>
              <th>检查日期</th>
              <th>整改状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in inspections" :key="item.id">
              <td>{{ item.merchantName || '场所#' + item.merchantId }}</td>
              <td>
                <span :class="['tag', item.fireRiskLevel === 'HIGH' ? 'tag-red' : item.fireRiskLevel === 'MEDIUM' ? 'tag-orange' : 'tag-green']">
                  {{ riskLabel(item.fireRiskLevel) }}
                </span>
              </td>
              <td>
                <span :class="['tag', item.safetyStatus === 'DANGER' ? 'tag-red' : item.safetyStatus === 'WARNING' ? 'tag-orange' : 'tag-green']">
                  {{ safetyLabel(item.safetyStatus) }}
                </span>
              </td>
              <td style="font-size:12px;">{{ item.inspectionDate }}</td>
              <td>
                <span :class="['tag', rectStatusClass(item.rectificationStatus)]">
                  {{ rectLabel(item.rectificationStatus) }}
                </span>
              </td>
              <td>
                <select v-if="item.rectificationStatus !== 'COMPLETED'" @change="updateStatus(item.id, ($event.target as HTMLSelectElement).value)" style="padding:4px 8px;border:1px solid #d1d5db;border-radius:4px;font-size:12px;">
                  <option value="">更改状态</option>
                  <option value="PENDING">待整改</option>
                  <option value="IN_PROGRESS">整改中</option>
                  <option value="COMPLETED">已完成</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!inspections.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无检查记录</p>
      </template>
    </div>

    <!-- 新增检查弹窗 -->
    <div v-if="showCreate" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="background:#fff;border-radius:12px;padding:24px;width:480px;max-width:90vw;max-height:80vh;overflow-y:auto;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">新增安全检查</h3>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">场所ID</label>
          <input v-model.number="form.merchantId" type="number" placeholder="输入场所ID" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px;">
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">消防风险</label>
            <select v-model="form.fireRiskLevel" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
              <option value="LOW">低风险</option>
              <option value="MEDIUM">中风险</option>
              <option value="HIGH">高风险</option>
            </select>
          </div>
          <div>
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">安全状态</label>
            <select v-model="form.safetyStatus" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
              <option value="NORMAL">正常</option>
              <option value="WARNING">警告</option>
              <option value="DANGER">危险</option>
            </select>
          </div>
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">发现隐患</label>
          <textarea v-model="form.hazardsFound" rows="2" placeholder="发现的隐患描述..." style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;"></textarea>
        </div>
        <div style="margin-bottom:12px;">
          <label style="display:flex;align-items:center;gap:6px;font-size:13px;">
            <input v-model="form.rectificationRequired" type="checkbox" />
            需要整改
          </label>
        </div>
        <div v-if="form.rectificationRequired" style="margin-bottom:12px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">整改截止日期</label>
          <input v-model="form.rectificationDeadline" type="date" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        </div>
        <div style="margin-bottom:16px;">
          <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">备注</label>
          <textarea v-model="form.remarks" rows="2" placeholder="检查备注..." style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;"></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;">
          <button @click="showCreate = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="submitInspection" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">提交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const inspections = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(true)
const error = ref('')
const showCreate = ref(false)

const form = ref({
  merchantId: null as number | null,
  fireRiskLevel: 'LOW',
  safetyStatus: 'NORMAL',
  hazardsFound: '',
  rectificationRequired: false,
  rectificationDeadline: '',
  remarks: '',
})

function riskLabel(level: string) {
  return level === 'HIGH' ? '高' : level === 'MEDIUM' ? '中' : '低'
}

function safetyLabel(status: string) {
  return status === 'DANGER' ? '危险' : status === 'WARNING' ? '警告' : '正常'
}

function rectLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待整改', IN_PROGRESS: '整改中', COMPLETED: '已完成', OVERDUE: '已超期' }
  return map[status] || status
}

function rectStatusClass(status: string) {
  if (status === 'COMPLETED') return 'tag-green'
  if (status === 'OVERDUE') return 'tag-red'
  if (status === 'IN_PROGRESS') return 'tag-blue'
  return 'tag-orange'
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [listRes, statsRes] = await Promise.all([
      http.get('/safety/inspections'),
      http.get('/safety/inspections/statistics')
    ])
    inspections.value = listRes || []
    stats.value = statsRes || {}
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function submitInspection() {
  if (!form.value.merchantId) { alert('请输入场所ID'); return }
  try {
    await http.post('/safety/inspections', form.value)
    showCreate.value = false
    alert('检查记录已提交')
    loadData()
  } catch (e: any) {
    alert(e?.message || '提交失败')
  }
}

async function updateStatus(id: number, status: string) {
  if (!status) return
  try {
    await http.put(`/safety/inspections/${id}/status`, { status })
    loadData()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

async function markOverdue() {
  try {
    const count = await http.post('/safety/inspections/mark-overdue')
    alert(`标记了 ${count} 个超期任务`)
    loadData()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

onMounted(loadData)
</script>
