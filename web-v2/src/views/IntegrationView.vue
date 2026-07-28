<template>
  <div class="page-container">
    <div class="page-header">
      <h2>信息互通</h2>
      <p class="page-desc">外部系统数据对接管理，支持应急管理、卫生健康、民政、物业、12345政务热线等系统</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">对接系统数</div>
        <div class="stat-value">{{ statistics.totalSystems || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">启用同步</div>
        <div class="stat-value">{{ statistics.enabledSystems || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">累计同步次数</div>
        <div class="stat-value">{{ statistics.totalSyncs || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日同步记录</div>
        <div class="stat-value">{{ statistics.todaySuccessRecords || 0 }}</div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar">
      <input v-model="searchKey" placeholder="搜索系统名称..." class="search-input" @keyup.enter="loadSystems" />
      <button @click="loadSystems" class="btn btn-primary">查询</button>
      <button @click="showCreateDialog = true" class="btn btn-success">新增系统</button>
      <button @click="loadStatistics" class="btn">刷新统计</button>
    </div>

    <!-- 系统列表 -->
    <div class="card">
      <h3 class="card-title">外部系统配置</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>系统编码</th>
            <th>系统名称</th>
            <th>类型</th>
            <th>API地址</th>
            <th>同步</th>
            <th>最后同步</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in systems" :key="s.id">
            <td>{{ s.id }}</td>
            <td style="font-size:12px;font-family:monospace;">{{ s.systemCode }}</td>
            <td>{{ s.systemName }}</td>
            <td><span class="tag tag-blue">{{ s.systemType }}</span></td>
            <td style="font-size:11px;color:#6b7280;max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ s.apiBaseUrl || '-' }}</td>
            <td>
              <span :class="['tag', s.syncEnabled ? 'tag-green' : 'tag-gray']">
                {{ s.syncEnabled ? '启用' : '禁用' }}
              </span>
            </td>
            <td style="font-size:12px;">{{ s.lastSyncAt || '-' }}</td>
            <td>
              <span :class="['tag', statusClass(s.lastSyncStatus)]">{{ statusLabel(s.lastSyncStatus) }}</span>
            </td>
            <td>
              <button @click="triggerSync(s)" class="btn btn-sm btn-primary" :disabled="!s.syncEnabled">同步</button>
              <button @click="viewDetail(s)" class="btn btn-sm">详情</button>
              <button @click="deleteSystem(s)" class="btn btn-sm btn-danger">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!systems.length" class="empty-text">暂无外部系统配置</p>
    </div>

    <!-- 同步日志 -->
    <div class="card" style="margin-top:20px;">
      <h3 class="card-title">同步日志</h3>
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>系统</th>
            <th>类型</th>
            <th>动作</th>
            <th>总计</th>
            <th>成功</th>
            <th>失败</th>
            <th>状态</th>
            <th>开始时间</th>
            <th>耗时</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="log in syncLogs" :key="log.id">
            <td>{{ log.id }}</td>
            <td style="font-size:12px;font-family:monospace;">{{ log.systemCode }}</td>
            <td>{{ log.syncType }}</td>
            <td style="font-size:12px;">{{ log.syncAction }}</td>
            <td>{{ log.recordsTotal }}</td>
            <td style="color:#52c41a;">{{ log.recordsSuccess }}</td>
            <td style="color:#ef4444;">{{ log.recordsFailed }}</td>
            <td><span :class="['tag', log.status === 'SUCCESS' ? 'tag-green' : 'tag-red']">{{ log.status }}</span></td>
            <td style="font-size:12px;">{{ log.startedAt }}</td>
            <td style="font-size:12px;">{{ calcDuration(log) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!syncLogs.length" class="empty-text">暂无同步日志</p>
    </div>

    <!-- 新建对话框 -->
    <div v-if="showCreateDialog" class="dialog-overlay" @click.self="showCreateDialog = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>新增外部系统</h3>
          <button @click="showCreateDialog = false" class="btn-close">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <label>系统编码 <span class="required">*</span></label>
            <input v-model="form.systemCode" placeholder="如: EMERGENCY" />
          </div>
          <div class="form-row">
            <label>系统名称 <span class="required">*</span></label>
            <input v-model="form.systemName" placeholder="如: 应急管理平台" />
          </div>
          <div class="form-row">
            <label>系统类型</label>
            <select v-model="form.systemType">
              <option value="API">API</option>
              <option value="WEBHOOK">WEBHOOK</option>
              <option value="FTP">FTP</option>
              <option value="DB">DB</option>
            </select>
          </div>
          <div class="form-row">
            <label>API地址</label>
            <input v-model="form.apiBaseUrl" placeholder="https://example.com/api" />
          </div>
          <div class="form-row">
            <label>备注</label>
            <textarea v-model="form.remark" rows="2" placeholder="备注信息"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="showCreateDialog = false" class="btn">取消</button>
          <button @click="createSystem" class="btn btn-primary">创建</button>
        </div>
      </div>
    </div>

    <!-- 详情对话框 -->
    <div v-if="selectedSystem" class="dialog-overlay" @click.self="selectedSystem = null">
      <div class="dialog" style="width:600px;">
        <div class="dialog-header">
          <h3>系统详情</h3>
          <button @click="selectedSystem = null" class="btn-close">&times;</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item"><label>ID:</label><span>{{ selectedSystem.id }}</span></div>
            <div class="detail-item"><label>编码:</label><span>{{ selectedSystem.systemCode }}</span></div>
            <div class="detail-item"><label>名称:</label><span>{{ selectedSystem.systemName }}</span></div>
            <div class="detail-item"><label>类型:</label><span>{{ selectedSystem.systemType }}</span></div>
            <div class="detail-item"><label>API地址:</label><span>{{ selectedSystem.apiBaseUrl || '-' }}</span></div>
            <div class="detail-item"><label>同步:</label><span>{{ selectedSystem.syncEnabled ? '启用' : '禁用' }}</span></div>
            <div class="detail-item"><label>最后同步:</label><span>{{ selectedSystem.lastSyncAt || '-' }}</span></div>
            <div class="detail-item"><label>同步状态:</label><span>{{ selectedSystem.lastSyncStatus }}</span></div>
            <div class="detail-item"><label>同步消息:</label><span>{{ selectedSystem.lastSyncMessage || '-' }}</span></div>
            <div class="detail-item"><label>状态:</label><span>{{ selectedSystem.status }}</span></div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="selectedSystem = null" class="btn">关闭</button>
        </div>
      </div>
    </div>

    <!-- 同步结果 -->
    <div v-if="syncResult" class="dialog-overlay" @click.self="syncResult = null">
      <div class="dialog">
        <div class="dialog-header">
          <h3>同步结果</h3>
          <button @click="syncResult = null" class="btn-close">&times;</button>
        </div>
        <div class="dialog-body">
          <pre style="background:#f5f5f5;padding:12px;border-radius:6px;font-size:12px;overflow:auto;">{{ JSON.stringify(syncResult, null, 2) }}</pre>
        </div>
        <div class="dialog-footer">
          <button @click="syncResult = null" class="btn">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getExternalSystems,
  getExternalSystem,
  createExternalSystem,
  updateExternalSystem,
  deleteExternalSystem,
  triggerSystemSync,
  getSyncLogs,
  getIntegrationStatistics
} from '../api'

const systems = ref<any[]>([])
const syncLogs = ref<any[]>([])
const statistics = ref<any>({})
const searchKey = ref('')
const showCreateDialog = ref(false)
const selectedSystem = ref<any>(null)
const syncResult = ref<any>(null)

const form = ref({
  systemCode: '',
  systemName: '',
  systemType: 'API',
  apiBaseUrl: '',
  remark: ''
})

async function loadSystems() {
  try {
    const res: any = await getExternalSystems({ keyword: searchKey.value || undefined, pageSize: 50 })
    systems.value = res?.items || []
  } catch (e) { console.error('加载失败:', e) }
}

async function loadSyncLogs() {
  try {
    const res: any = await getSyncLogs({ pageSize: 20 })
    syncLogs.value = res?.items || []
  } catch (e) { console.error('加载日志失败:', e) }
}

async function loadStatistics() {
  try {
    const res: any = await getIntegrationStatistics()
    statistics.value = res || {}
  } catch (e) { console.error('加载统计失败:', e) }
}

async function createSystem() {
  if (!form.value.systemCode || !form.value.systemName) {
    alert('请填写系统编码和名称')
    return
  }
  try {
    await createExternalSystem(form.value)
    showCreateDialog.value = false
    form.value = { systemCode: '', systemName: '', systemType: 'API', apiBaseUrl: '', remark: '' }
    loadSystems()
  } catch (e: any) {
    alert(e.message || '创建失败')
  }
}

async function deleteSystem(s: any) {
  if (!confirm(`确定删除系统 ${s.systemName}？`)) return
  try {
    await deleteExternalSystem(s.id)
    loadSystems()
  } catch (e: any) {
    alert(e.message || '删除失败')
  }
}

async function triggerSync(s: any) {
  try {
    const res: any = await triggerSystemSync(s.id)
    syncResult.value = res
    loadSystems()
    loadSyncLogs()
  } catch (e: any) {
    alert(e.message || '同步失败')
  }
}

function viewDetail(s: any) {
  selectedSystem.value = s
}

function statusClass(status: string) {
  if (status === 'SUCCESS') return 'tag-green'
  if (status === 'FAILURE') return 'tag-red'
  return 'tag-gray'
}

function statusLabel(status: string) {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILURE') return '失败'
  if (status === 'PENDING') return '待同步'
  return status || '未知'
}

function calcDuration(log: any) {
  if (!log.startedAt || !log.finishedAt) return '-'
  const start = new Date(log.startedAt).getTime()
  const end = new Date(log.finishedAt).getTime()
  const sec = Math.round((end - start) / 1000)
  return sec < 60 ? `${sec}秒` : `${Math.round(sec / 60)}分${sec % 60}秒`
}

onMounted(() => {
  loadSystems()
  loadSyncLogs()
  loadStatistics()
})
</script>

<style scoped>
.page-container { padding: 20px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 20px; font-weight: 600; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #6b7280; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.stat-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1f2937; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; align-items: center; }
.search-input { padding: 6px 12px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13px; width: 200px; }
.btn { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; background: #fff; font-size: 13px; cursor: pointer; }
.btn-primary { background: #1890ff; color: #fff; border-color: #1890ff; }
.btn-success { background: #52c41a; color: #fff; border-color: #52c41a; }
.btn-danger { color: #ef4444; border-color: #ef4444; }
.btn-sm { padding: 3px 8px; font-size: 12px; }
.btn:hover { opacity: 0.85; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.card { background: #fff; border-radius: 8px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th, .data-table td { padding: 8px; text-align: left; border-bottom: 1px solid #f3f4f6; }
.data-table th { background: #f9fafb; font-weight: 500; color: #6b7280; }
.tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 11px; }
.tag-green { background: #f0fdf4; color: #16a34a; }
.tag-red { background: #fef2f2; color: #dc2626; }
.tag-blue { background: #eff6ff; color: #2563eb; }
.tag-gray { background: #f3f4f6; color: #6b7280; }
.empty-text { text-align: center; padding: 30px; color: #9ca3af; }
.dialog-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { width: 480px; max-height: 80vh; background: #fff; border-radius: 12px; overflow: hidden; display: flex; flex-direction: column; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #f3f4f6; }
.dialog-header h3 { font-size: 16px; font-weight: 600; }
.btn-close { border: none; background: none; font-size: 20px; cursor: pointer; color: #9ca3af; }
.dialog-body { padding: 20px; overflow-y: auto; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; padding: 12px 20px; border-top: 1px solid #f3f4f6; }
.form-row { margin-bottom: 12px; }
.form-row label { display: block; font-size: 13px; font-weight: 500; margin-bottom: 4px; }
.form-row input, .form-row select, .form-row textarea { width: 100%; padding: 6px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.required { color: #ef4444; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.detail-item { display: flex; gap: 8px; }
.detail-item label { color: #6b7280; font-size: 13px; min-width: 80px; }
.detail-item span { font-size: 13px; word-break: break-all; }
</style>
