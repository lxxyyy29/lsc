<template>
  <div>
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
      <div>
        <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">趋势预判 / 反复投诉预警</h2>
        <p style="font-size:13px;color:#6b7280;">自动扫描：同一网格同类型事件 7 天内 ≥3 起、同一地点 7 天内 ≥2 次反复上报 → 自动预警并通知责任人</p>
      </div>
      <button @click="handleScan" :disabled="scanning" style="padding:8px 16px;border:none;border-radius:6px;background:#722ed1;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-sync" :class="{ 'fa-spin': scanning }"></i> {{ scanning ? '扫描中...' : '立即扫描' }}
      </button>
    </div>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;">
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">待处理预警</p>
            <p class="stat-value" style="color:#dc2626;">{{ stats.open || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fef2f2;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-bell" style="color:#dc2626;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">紧急预警</p>
            <p class="stat-value" style="color:#ea580c;">{{ stats.urgent || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff7ed;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-exclamation-triangle" style="color:#ea580c;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">类型频发</p>
            <p class="stat-value" style="color:#1890ff;">{{ stats.byType || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#e6f4ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-chart-line" style="color:#1890ff;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-purple">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">反复投诉</p>
            <p class="stat-value" style="color:#722ed1;">{{ stats.byAddress || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#f9f0ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-redo-alt" style="color:#722ed1;font-size:18px;"></i></div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;">
      <select v-model="filterStatus" @change="loadAlerts" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
        <option value="">全部状态</option>
        <option value="OPEN">待处理</option>
        <option value="HANDLED">已处理</option>
      </select>
      <select v-model="filterDimension" @change="loadAlerts" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
        <option value="">全部维度</option>
        <option value="EVENT_TYPE">类型频发</option>
        <option value="ADDRESS">反复投诉</option>
      </select>
      <span style="font-size:12px;color:#6b7280;">共 {{ total }} 条</span>
    </div>

    <div class="card">
      <table class="table">
        <thead>
          <tr>
            <th>级别</th>
            <th>维度</th>
            <th>预警内容</th>
            <th>频次</th>
            <th>统计窗口</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in alerts" :key="a.id">
            <td><span :class="['tag', a.level === 'URGENT' ? 'tag-red' : 'tag-orange']">{{ a.level === 'URGENT' ? '紧急' : '一般' }}</span></td>
            <td><span :class="['tag', a.dimension === 'ADDRESS' ? 'tag-purple' : 'tag-blue']">{{ a.dimension === 'ADDRESS' ? '反复投诉' : '类型频发' }}</span></td>
            <td style="max-width:360px;">
              <div style="font-weight:500;font-size:13px;">{{ a.title }}</div>
              <div style="font-size:12px;color:#6b7280;margin-top:2px;">{{ a.content }}</div>
              <div style="font-size:11px;color:#9ca3af;margin-top:2px;">{{ a.alert_no }} · {{ formatTime(a.created_at) }}</div>
            </td>
            <td style="text-align:center;">
              <span style="font-size:16px;font-weight:600;color:#dc2626;">{{ a.alert_count }}</span>
              <span style="font-size:11px;color:#9ca3af;">/{{ a.threshold }}</span>
            </td>
            <td style="font-size:12px;color:#6b7280;">
              <div>{{ formatTime(a.window_start) }}</div>
              <div>~ {{ formatTime(a.window_end) }}</div>
            </td>
            <td>
              <span :class="['tag', a.status === 'OPEN' ? 'tag-red' : 'tag-green']">{{ a.status === 'OPEN' ? '待处理' : '已处理' }}</span>
              <div v-if="a.status === 'HANDLED'" style="font-size:11px;color:#9ca3af;margin-top:2px;">{{ a.handler_name }} · {{ formatTime(a.handled_at) }}</div>
            </td>
            <td>
              <button v-if="a.status === 'OPEN'" @click="openHandle(a)" style="padding:4px 10px;font-size:12px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;cursor:pointer;">处理</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!alerts.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无预警，可点击右上角「立即扫描」触发检测</p>
    </div>

    <!-- 处理弹窗 -->
    <div v-if="showHandle" class="modal-overlay" @click.self="showHandle = false">
      <div class="modal-box" style="width:440px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">处理预警</h3>
        <p style="font-size:13px;color:#6b7280;margin-bottom:12px;background:#f9fafb;padding:10px 12px;border-radius:6px;">{{ current?.title }}</p>
        <div class="form-group">
          <label class="form-label">处理说明</label>
          <textarea v-model="handleRemark" class="form-input" rows="3" placeholder="如：已安排网格员现场核实，持续跟进中..."></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showHandle = false" class="btn btn-default">取消</button>
          <button @click="handleSubmit" class="btn btn-primary">确认处理</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTrendAlerts, getTrendAlertStatistics, scanTrendAlerts, handleTrendAlert } from '../api'

const alerts = ref<any[]>([])
const stats = ref<any>({})
const total = ref(0)
const filterStatus = ref('')
const filterDimension = ref('')
const scanning = ref(false)
const showHandle = ref(false)
const current = ref<any>(null)
const handleRemark = ref('')

function formatTime(t?: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function loadAlerts() {
  try {
    const res = await getTrendAlerts({ status: filterStatus.value, dimension: filterDimension.value, page: 1, size: 100 })
    alerts.value = res?.items || []
    total.value = res?.total || 0
    stats.value = await getTrendAlertStatistics() || {}
  } catch (e: any) {
    alert(e?.message || '加载失败')
  }
}

async function handleScan() {
  scanning.value = true
  try {
    const created = await scanTrendAlerts()
    alert(`扫描完成，新生成 ${created || 0} 条预警`)
    await loadAlerts()
  } catch (e: any) {
    alert(e?.message || '扫描失败')
  } finally {
    scanning.value = false
  }
}

function openHandle(a: any) {
  current.value = a
  handleRemark.value = ''
  showHandle.value = true
}

async function handleSubmit() {
  try {
    await handleTrendAlert(current.value.id, handleRemark.value)
    showHandle.value = false
    await loadAlerts()
  } catch (e: any) {
    alert(e?.message || '处理失败')
  }
}

onMounted(loadAlerts)
</script>
