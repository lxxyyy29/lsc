<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">数据变更审计</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">查看所有数据变更历史，支持回滚到任意版本</p>

    <div class="card">
      <!-- 筛选栏 -->
      <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;">
        <select v-model="filters.tableName" @change="loadData" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;">
          <option value="">全部表</option>
          <option v-for="t in tables" :key="t" :value="t">{{ t }}</option>
        </select>
        <input v-model="filters.recordId" placeholder="记录ID" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;" />
        <button @click="loadData" style="padding:6px 14px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">查询</button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" style="text-align:center;padding:40px;color:#9ca3af;">
        <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
        <p style="margin-top:12px;font-size:13px;">加载中...</p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error" style="text-align:center;padding:40px;">
        <i class="fas fa-exclamation-circle" style="font-size:24px;color:#ff4d4f;"></i>
        <p style="margin-top:12px;font-size:13px;color:#ff4d4f;">{{ error }}</p>
        <button @click="loadData" style="margin-top:12px;padding:6px 16px;border:1px solid #d9d9d9;border-radius:4px;background:#fff;cursor:pointer;font-size:13px;">重试</button>
      </div>

      <!-- 数据表格 -->
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>表名</th>
              <th>记录ID</th>
              <th>操作</th>
              <th>变更字段</th>
              <th>操作人</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in items" :key="log.id">
              <td>{{ log.id }}</td>
              <td><span class="tag tag-blue">{{ log.tableName }}</span></td>
              <td style="font-size:12px;color:#6b7280;">{{ log.recordId }}</td>
              <td>
                <span :class="['tag', opClass(log.operationType)]">
                  {{ opLabel(log.operationType) }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ formatJson(log.changedFields) }}</td>
              <td>{{ log.operatorName || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ log.operationTime }}</td>
              <td>
                <button v-if="log.operationType !== 'CREATE' && log.oldValues" @click="handleRollback(log.id)" style="padding:4px 10px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:12px;cursor:pointer;">回滚</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!items.length" style="text-align:center;padding:40px;color:#9ca3af;">暂无审计记录</p>

        <!-- 分页 -->
        <div v-if="totalPages > 1" style="display:flex;align-items:center;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid #e5e7eb;">
          <span style="font-size:13px;color:#6b7280;">共 {{ total }} 条</span>
          <div style="display:flex;gap:6px;">
            <button @click="page = 1; loadData()" :disabled="page === 1" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">首页</button>
            <button @click="page--; loadData()" :disabled="page === 1" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">上一页</button>
            <span style="font-size:13px;color:#374151;margin:0 8px;">第 <strong>{{ page }}</strong> / {{ totalPages }} 页</span>
            <button @click="page++; loadData()" :disabled="page === totalPages" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">下一页</button>
            <button @click="page = totalPages; loadData()" :disabled="page === totalPages" style="padding:6px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:13px;cursor:pointer;">末页</button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import http from '../api'

const items = ref<any[]>([])
const tables = ref<string[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const filters = reactive({
  tableName: '',
  recordId: '',
})

function opLabel(type: string) {
  const map: Record<string, string> = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', ROLLBACK: '回滚' }
  return map[type] || type
}

function opClass(type: string) {
  if (type === 'CREATE') return 'tag-green'
  if (type === 'UPDATE') return 'tag-blue'
  if (type === 'DELETE') return 'tag-red'
  if (type === 'ROLLBACK') return 'tag-orange'
  return 'tag-blue'
}

function formatJson(json: string) {
  if (!json) return '-'
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.join(', ') : json
  } catch {
    return json
  }
}

async function loadTables() {
  try {
    tables.value = await http.get('/audit-logs/tables') || []
  } catch (e) {}
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.tableName) params.tableName = filters.tableName
    if (filters.recordId) params.recordId = filters.recordId
    const result = await http.get('/audit-logs', { params })
    items.value = result?.items || []
    total.value = result?.total || 0
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function handleRollback(id: number) {
  if (!confirm('确定要回滚到此版本吗？当前数据将被覆盖。')) return
  try {
    await http.post(`/audit-logs/rollback/${id}`)
    alert('回滚成功')
    loadData()
  } catch (e: any) {
    alert(e?.message || '回滚失败')
  }
}

onMounted(() => {
  loadTables()
  loadData()
})
</script>
