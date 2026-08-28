<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">考核研判</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">网格员绩效考核</p>

    <!-- 加载状态 -->
    <div v-if="loading" style="text-align:center;padding:60px;color:#9ca3af;">
      <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
      <p style="margin-top:12px;">加载中...</p>
    </div>

    <template v-else>
      <!-- 操作栏：导出 -->
      <div style="display:flex;justify-content:flex-end;gap:8px;margin-bottom:16px;">
        <button @click="loadData" class="btn btn-default">
          <i class="fas fa-sync"></i> 刷新
        </button>
        <button @click="exportData" class="btn btn-primary">
          <i class="fas fa-download"></i> 导出
        </button>
      </div>

      <!-- 网格组长研判：下属网格员工单绩效汇总反应组长 -->
      <div class="card" style="margin-bottom:16px;">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">
          网格组长研判
          <span style="font-size:12px;font-weight:400;color:#6b7280;">（共 {{ leaders.length }} 人 · 以下属网格员绩效汇总）</span>
        </h3>
        <table class="table">
          <thead>
            <tr>
              <th>排名</th>
              <th>姓名</th>
              <th>职位</th>
              <th>所属网格</th>
              <th>下属网格员</th>
              <th>总工单</th>
              <th>已完成</th>
              <th>处理中</th>
              <th>办结率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(l, idx) in leaders" :key="l.leaderOrgMemberId">
              <td><span :style="{fontWeight:'700',color: idx < 3 ? '#ff4d4f' : '#9ca3af'}">{{ idx + 1 }}</span></td>
              <td>{{ l.leaderName || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ l.position || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ l.gridName || '未分配网格' }}</td>
              <td>{{ l.workerCount || 0 }}</td>
              <td>{{ l.totalOrders || 0 }}</td>
              <td>{{ l.completedOrders || 0 }}</td>
              <td>{{ l.processingOrders || 0 }}</td>
              <td>
                <span :style="{fontWeight:'600',color: (l.completionRate || 0) >= 80 ? '#52c41a' : (l.completionRate || 0) >= 50 ? '#faad14' : '#ff4d4f'}">
                  {{ l.completionRate || 0 }}%
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!leaders.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </div>

      <!-- 网格员绩效考核表 -->
      <div class="card">
        <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">
          网格员绩效考核
          <span style="font-size:12px;font-weight:400;color:#6b7280;">（共 {{ workers.length }} 人）</span>
        </h3>
        <table class="table">
          <thead>
            <tr>
              <th>排名</th>
              <th>姓名</th>
              <th>所属网格</th>
              <th>总工单</th>
              <th>已完成</th>
              <th>处理中</th>
              <th>办结率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(w, idx) in workers" :key="w.userId">
              <td><span :style="{fontWeight:'700',color: idx < 3 ? '#ff4d4f' : '#9ca3af'}">{{ idx + 1 }}</span></td>
              <td>{{ w.realName || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ w.gridName || '未分配网格' }}</td>
              <td>{{ w.totalOrders || 0 }}</td>
              <td>{{ w.completedOrders || 0 }}</td>
              <td>{{ w.processingOrders || 0 }}</td>
              <td>
                <span :style="{fontWeight:'600',color: (w.completionRate || 0) >= 80 ? '#52c41a' : (w.completionRate || 0) >= 50 ? '#faad14' : '#ff4d4f'}">
                  {{ w.completionRate || 0 }}%
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!workers.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无数据</p>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import http from '../api'

const loading = ref(true)
const leaders = ref<any[]>([])
const workers = ref<any[]>([])

async function loadData() {
  loading.value = true
  try {
    const [leaderRes, workerRes]: any[] = await Promise.all([
      http.get('/assessment/leader-performance'),
      http.get('/assessment/worker-performance'),
    ])
    leaders.value = Array.isArray(leaderRes) ? leaderRes : []
    workers.value = Array.isArray(workerRes) ? workerRes : []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

/** 导出考核研判为 CSV(UTF-8 BOM,Excel 打开中文不乱码)：组长研判 + 网格员绩效 */
function exportData() {
  const rows: (string | number)[][] = []
  rows.push(['考核研判', `导出时间:${new Date().toLocaleString()}`])
  rows.push([])
  rows.push(['网格组长研判（下属网格员绩效汇总）'])
  rows.push(['排名', '姓名', '职位', '所属网格', '下属网格员', '总工单', '已完成', '处理中', '办结率(%)'])
  leaders.value.forEach((l, idx) => {
    rows.push([
      idx + 1,
      l.leaderName || '-',
      l.position || '-',
      l.gridName || '未分配网格',
      l.workerCount || 0,
      l.totalOrders || 0,
      l.completedOrders || 0,
      l.processingOrders || 0,
      l.completionRate || 0
    ])
  })
  rows.push([])
  rows.push(['网格员绩效考核'])
  rows.push(['排名', '姓名', '所属网格', '总工单', '已完成', '处理中', '办结率(%)'])
  workers.value.forEach((w, idx) => {
    rows.push([
      idx + 1,
      w.realName || '-',
      w.gridName || '未分配网格',
      w.totalOrders || 0,
      w.completedOrders || 0,
      w.processingOrders || 0,
      w.completionRate || 0
    ])
  })

  const escapeCell = (v: string | number) => {
    const s = String(v)
    return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
  }
  const csv = '\uFEFF' + rows.map((row) => row.map(escapeCell).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `网格员绩效考核_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(loadData)
</script>
