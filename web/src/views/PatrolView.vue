<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">网格巡查任务</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">常态化巡查清单：人居环境、消防、出租屋、矛盾排查等</p>

    <!-- 统计卡片 -->
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin-bottom:20px;">
      <div class="card card-border-blue">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">总任务数</p>
            <p class="stat-value">{{ stats.total || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#e6f4ff;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-clipboard-list" style="color:#1890FF;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-orange">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">待巡查</p>
            <p class="stat-value">{{ stats.pending || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff7e6;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-clock" style="color:#FAAD14;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-green">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">已完成</p>
            <p class="stat-value">{{ stats.completed || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#f6ffed;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-check-circle" style="color:#52C41A;font-size:18px;"></i></div>
        </div>
      </div>
      <div class="card card-border-red">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div>
            <p class="stat-label">已超期</p>
            <p class="stat-value">{{ stats.overdue || 0 }}</p>
          </div>
          <div style="width:40px;height:40px;background:#fff1f0;border-radius:8px;display:flex;align-items:center;justify-content:center;"><i class="fas fa-exclamation-triangle" style="color:#FF4D4F;font-size:18px;"></i></div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center;">
      <button @click="generateTasks" :disabled="loading" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-sync" style="margin-right:4px;"></i>生成本周任务
      </button>
      <button @click="markOverdue" :disabled="loading" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">
        <i class="fas fa-exclamation" style="margin-right:4px;"></i>标记超期
      </button>
      <button @click="remindUpcoming" :disabled="loading" style="padding:8px 16px;border:1px solid #f59e0b;border-radius:6px;background:#fff7ed;color:#b45309;font-size:13px;cursor:pointer;">
        <i class="fas fa-bell" style="margin-right:4px;"></i>到期未巡提醒
      </button>
      <select v-model="filterStatus" @change="loadTasks" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
        <option value="">全部状态</option>
        <option value="PENDING">待巡查</option>
        <option value="COMPLETED">已完成</option>
        <option value="OVERDUE">已超期</option>
      </select>
    </div>

    <!-- 任务列表 -->
    <div class="card">
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="loadTasks" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>任务名称</th>
              <th>计划日期</th>
              <th>状态</th>
              <th>完成时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in filteredTasks" :key="t.id">
              <td>{{ t.taskName || '-' }}</td>
              <td>{{ t.plannedDate || '-' }}</td>
              <td>
                <span :class="['tag', t.status === 'COMPLETED' ? 'tag-green' : t.status === 'OVERDUE' ? 'tag-red' : 'tag-orange']">
                  {{ statusLabel(t.status) }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ t.completedAt || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-if="!filteredTasks.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无巡查任务</p>
      </template>
    </div>

    <!-- 巡查记录 -->
    <div class="card" style="margin-top:20px;">
      <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">巡查记录</h3>
      <table class="table">
        <thead><tr><th>网格</th><th>巡查员</th><th>类型</th><th>内容</th><th>时间</th></tr></thead>
        <tbody>
          <tr v-for="r in records" :key="r.id">
            <td>{{ r.gridName || '-' }}</td>
            <td>{{ r.userName || '-' }}</td>
            <td><span class="tag tag-blue">{{ r.patrolType || '日常' }}</span></td>
            <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ r.content || '-' }}</td>
            <td style="font-size:12px;color:#6b7280;">{{ r.createdAt || '-' }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!records.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无巡查记录</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAllPatrolTasks, getPatrolTaskStatistics, generatePatrolTasks, markOverduePatrolTasks, getPatrolRecords, remindUpcomingPatrolTasks } from '../api'

const tasks = ref<any[]>([])
const records = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(true)
const error = ref('')
const filterStatus = ref('')

const filteredTasks = computed(() => {
  if (!filterStatus.value) return tasks.value
  return tasks.value.filter(t => t.status === filterStatus.value)
})

function statusLabel(status: string) {
  const map: Record<string, string> = { PENDING: '待巡查', COMPLETED: '已完成', OVERDUE: '已超期' }
  return map[status] || status
}

async function loadTasks() {
  loading.value = true
  error.value = ''
  try {
    const [tasksRes, statsRes, recordsRes] = await Promise.all([
      getAllPatrolTasks(),
      getPatrolTaskStatistics(),
      getPatrolRecords()
    ])
    tasks.value = tasksRes || []
    stats.value = statsRes || {}
    records.value = recordsRes || []
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function generateTasks() {
  try {
    const count = await generatePatrolTasks()
    if (count > 0) {
      alert(`成功生成 ${count} 个巡查任务`)
    } else {
      alert('本周巡查任务已存在，无需重复生成')
    }
    loadTasks()
  } catch (e: any) {
    alert(e?.message || '生成失败')
  }
}

async function markOverdue() {
  try {
    const count = await markOverduePatrolTasks()
    alert(`标记了 ${count} 个超期任务`)
    loadTasks()
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

async function remindUpcoming() {
  try {
    const count = await remindUpcomingPatrolTasks()
    alert(`已发送 ${count} 条到期未巡提醒`)
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

onMounted(loadTasks)
</script>
