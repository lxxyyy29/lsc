<template>
  <div>
    <h2 style="font-size:20px;font-weight:600;margin-bottom:4px;">数据变更审计</h2>
    <p style="font-size:13px;color:#6b7280;margin-bottom:20px;">查看所有数据变更历史，支持字段级变更详情查看和回滚</p>

    <div class="card">
      <!-- 筛选栏 -->
      <div class="filter-bar" style="margin-bottom:16px;">
        <select v-model="filters.tableName" class="filter-select" @change="page = 1; loadData()">
          <option value="">全部模块</option>
          <option v-for="t in tables" :key="t" :value="t">{{ moduleLabel(t) }}</option>
        </select>
        <select v-model="filters.operationType" class="filter-select" @change="page = 1; loadData()">
          <option value="">全部操作</option>
          <option value="CREATE">新增</option>
          <option value="UPDATE">修改</option>
          <option value="DELETE">删除</option>
          <option value="APPROVE">审批</option>
          <option value="ROLLBACK">回滚</option>
        </select>
        <input v-model="filters.recordId" class="filter-input" placeholder="记录ID" @keyup.enter="page = 1; loadData()" />
        <input v-model="filters.operatorName" class="filter-input" placeholder="操作人" @keyup.enter="page = 1; loadData()" />
        <input v-model="filters.startTime" type="datetime-local" class="filter-input" />
        <span style="color:#9ca3af;font-size:12px;">至</span>
        <input v-model="filters.endTime" type="datetime-local" class="filter-input" />
        <button @click="page = 1; loadData()" class="filter-action"><i class="fas fa-search"></i>查询</button>
        <button @click="resetFilters" class="filter-action ghost">重置</button>
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
              <th>模块</th>
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
              <td><span class="tag tag-blue" :title="log.tableName">{{ moduleLabel(log.tableName) }}</span></td>
              <td style="font-size:12px;color:#6b7280;">{{ log.recordId }}</td>
              <td>
                <span :class="['tag', opClass(log.operationType)]">
                  {{ opLabel(log.operationType) }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;max-width:150px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ formatJson(log.changedFields) }}</td>
              <td>{{ log.operatorName || '-' }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ formatTime(log.operationTime) }}</td>
              <td>
                <button @click="viewDiff(log)" style="padding:3px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:11px;cursor:pointer;margin-right:4px;">变更详情</button>
                <button v-if="log.operationType !== 'CREATE' && log.oldValues" @click="handleRollback(log)" style="padding:3px 8px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:11px;cursor:pointer;">回滚</button>
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

    <!-- 变更详情 Diff 对话框 -->
    <div v-if="diffData" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:640px;max-height:80vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 style="font-size:16px;font-weight:600;">字段变更详情</h3>
          <button @click="diffData = null" style="border:none;background:none;font-size:18px;cursor:pointer;color:#9ca3af;">&times;</button>
        </div>

        <!-- 基本信息 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:16px;font-size:13px;padding:12px;background:#f9fafb;border-radius:8px;">
          <div><span style="color:#6b7280;">模块：</span><strong>{{ moduleLabel(diffData.tableName) }}</strong></div>
          <div><span style="color:#6b7280;">记录ID：</span><strong>{{ diffData.recordId }}</strong></div>
          <div><span style="color:#6b7280;">操作类型：</span><span :class="['tag', opClass(diffData.operationType)]">{{ opLabel(diffData.operationType) }}</span></div>
          <div><span style="color:#6b7280;">操作人：</span><strong>{{ diffData.operatorName || '-' }}</strong></div>
          <div style="grid-column:1/-1;"><span style="color:#6b7280;">操作时间：</span><strong>{{ formatTime(diffData.operationTime) }}</strong></div>
        </div>

        <!-- Diff 表格 -->
        <div v-if="diffData.diffRows && diffData.diffRows.length" style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
          <table style="width:100%;border-collapse:collapse;font-size:13px;">
            <thead>
              <tr style="background:#f3f4f6;">
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;width:30%;">字段</th>
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;width:35%;">变更前</th>
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;width:35%;">变更后</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in diffData.diffRows" :key="idx" :style="row.changed ? 'background:#fff7e6;' : ''">
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;font-weight:500;">
                  {{ row.field }}
                  <span v-if="row.changed" style="margin-left:4px;color:#fa8c16;font-size:11px;">&#9654;</span>
                </td>
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;color:#ff4d4f;font-family:monospace;font-size:12px;">
                  <span v-if="row.changed" style="background:#fff1f0;padding:1px 4px;border-radius:3px;">{{ formatValue(row.oldValue) }}</span>
                  <span v-else>{{ formatValue(row.oldValue) }}</span>
                </td>
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;color:#52c41a;font-family:monospace;font-size:12px;">
                  <span v-if="row.changed" style="background:#f6ffed;padding:1px 4px;border-radius:3px;">{{ formatValue(row.newValue) }}</span>
                  <span v-else>{{ formatValue(row.newValue) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 无变更数据 -->
        <div v-else style="text-align:center;padding:30px;color:#9ca3af;font-size:13px;">
          <i class="fas fa-info-circle" style="font-size:20px;margin-bottom:8px;"></i>
          <p>此条审计日志未记录详细字段变更（可能由系统切面自动记录）</p>
        </div>

        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button @click="diffData = null" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭</button>
        </div>
      </div>
    </div>

    <!-- 回滚预览对话框 -->
    <div v-if="previewData" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;">
      <div style="width:560px;max-height:80vh;background:#fff;border-radius:12px;padding:24px;box-shadow:0 8px 32px rgba(0,0,0,0.12);overflow-y:auto;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
          <h3 style="font-size:16px;font-weight:600;color:#ff4d4f;">
            <i class="fas fa-exclamation-triangle" style="margin-right:6px;"></i>回滚确认
          </h3>
          <button @click="previewData = null" style="border:none;background:none;font-size:18px;cursor:pointer;color:#9ca3af;">&times;</button>
        </div>

        <div style="padding:12px;background:#fff7e6;border:1px solid #ffe58f;border-radius:8px;margin-bottom:16px;font-size:13px;">
          <strong>注意：</strong>回滚将把 <code style="background:#fff;padding:1px 4px;border-radius:3px;">{{ moduleLabel(previewData.tableName) }}</code> 中 ID 为 <strong>{{ previewData.recordId }}</strong> 的记录恢复到变更前状态。
        </div>

        <!-- 变更预览 -->
        <div v-if="previewData.previewRows && previewData.previewRows.length" style="border:1px solid #e5e7eb;border-radius:8px;overflow:hidden;">
          <table style="width:100%;border-collapse:collapse;font-size:13px;">
            <thead>
              <tr style="background:#f3f4f6;">
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;">字段</th>
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;">当前值</th>
                <th style="padding:8px 12px;text-align:left;border-bottom:1px solid #e5e7eb;">回滚后</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, idx) in previewData.previewRows" :key="idx" style="background:#fff1f0;">
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;font-weight:500;">{{ row.field }}</td>
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;color:#ff4d4f;font-family:monospace;font-size:12px;">{{ formatValue(row.currentValue) }}</td>
                <td style="padding:8px 12px;border-bottom:1px solid #f3f4f6;color:#52c41a;font-family:monospace;font-size:12px;">{{ formatValue(row.rollbackToValue) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else style="text-align:center;padding:20px;color:#9ca3af;font-size:13px;">
          无需要回滚的字段（当前值已与旧值一致）
        </div>

        <div style="margin-top:16px;display:flex;justify-content:flex-end;gap:8px;">
          <button @click="previewData = null" style="padding:6px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
          <button @click="confirmRollback" :disabled="!previewData.previewRows || !previewData.previewRows.length" style="padding:6px 16px;border:none;border-radius:6px;background:#ff4d4f;color:#fff;font-size:13px;cursor:pointer;">确认回滚</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import http from '../api'
import { getAuditLogs, getAuditLogDiff, previewAuditLogRollback, rollbackAuditLog } from '../api'
import { showMessage } from '../utils/message'

const items = ref<any[]>([])
const tables = ref<string[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const diffData = ref<any>(null)
const previewData = ref<any>(null)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const filters = reactive({
  tableName: '',
  operationType: '',
  recordId: '',
  operatorName: '',
  startTime: '',
  endTime: '',
})

function resetFilters() {
  filters.tableName = ''
  filters.operationType = ''
  filters.recordId = ''
  filters.operatorName = ''
  filters.startTime = ''
  filters.endTime = ''
  page.value = 1
  loadData()
}

function opLabel(type: string) {
  const map: Record<string, string> = { CREATE: '新增', UPDATE: '修改', DELETE: '删除', APPROVE: '审批', ROLLBACK: '回滚' }
  return map[type] || type
}

/** 审计切面按接口路径首段记录模块代号，这里映射为中文便于阅读 */
function moduleLabel(name: string) {
  const map: Record<string, string> = {
    auth: '登录认证', h5: '移动端', community: '社区基础', events: '事件工单', party: '党建治理',
    messaging: '消息', resident: '居民服务', emergency: '应急指挥', notifications: '通知',
    activities: '志愿活动', trend_alerts: '趋势预警', media: '媒体资源', registration: '注册',
    work_orders: '工单', upload: '文件上传', repairs: '报修', integrations: '外部集成',
    video: '视频', dispatch_rules: '派单规则', vehicle_tracks: '车辆轨迹', processes: '流程',
    test: '测试', grids: '网格', assessments: '考核研判', audits: '审核', drones: '无人机',
  }
  return map[name] || name
}

function opClass(type: string) {
  if (type === 'CREATE') return 'tag-green'
  if (type === 'UPDATE') return 'tag-blue'
  if (type === 'DELETE') return 'tag-red'
  if (type === 'APPROVE') return 'tag-green'
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

function formatTime(time: any) {
  if (!time) return '-'
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return time
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return time
  }
}

function formatValue(val: any) {
  if (val === null || val === undefined) return '(空)'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
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
    if (filters.operationType) params.operationType = filters.operationType
    if (filters.recordId) params.recordId = filters.recordId
    if (filters.startTime) params.startTime = filters.startTime
    if (filters.endTime) params.endTime = filters.endTime
    const result = await getAuditLogs(params)
    items.value = result?.items || []
    total.value = result?.total || 0
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function viewDiff(log: any) {
  try {
    diffData.value = await getAuditLogDiff(log.id)
  } catch (e: any) {
    // 如果接口不可用，直接用本地数据构造
    diffData.value = {
      id: log.id,
      tableName: log.tableName,
      recordId: log.recordId,
      operationType: log.operationType,
      operatorName: log.operatorName,
      operationTime: log.operationTime,
      remark: log.remark,
      diffRows: parseLocalDiff(log)
    }
  }
}

function parseLocalDiff(log: any): any[] {
  try {
    const fields = log.changedFields ? JSON.parse(log.changedFields) : []
    const oldMap = log.oldValues ? JSON.parse(log.oldValues) : {}
    const newMap = log.newValues ? JSON.parse(log.newValues) : {}
    if (!Array.isArray(fields)) return []
    return fields.map((f: string) => ({
      field: f,
      oldValue: oldMap[f],
      newValue: newMap[f],
      changed: !Object.is(oldMap[f], newMap[f])
    }))
  } catch {
    return []
  }
}

async function handleRollback(log: any) {
  try {
    previewData.value = await previewAuditLogRollback(log.id)
    previewData.value._logId = log.id
  } catch (e: any) {
    // 如果预览接口不可用，直接确认
    if (confirm(`确定要将 ${log.tableName} 表中 ID=${log.recordId} 的记录回滚到变更前状态吗？`)) {
      await doRollback(log.id)
    }
  }
}

async function confirmRollback() {
  if (!previewData.value?._logId) return
  const id = previewData.value._logId
  previewData.value = null
  await doRollback(id)
}

async function doRollback(id: number) {
  try {
    await rollbackAuditLog(id)
    showMessage('回滚成功')
    loadData()
  } catch (e: any) {
    showMessage(e?.message || '回滚失败')
  }
}

onMounted(() => {
  loadTables()
  loadData()
})
</script>
