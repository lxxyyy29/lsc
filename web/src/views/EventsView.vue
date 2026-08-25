<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">事件闭环处置</h1>
        <p class="page-desc">发现上报→智能派单→现场处置→复核核查→归档</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="showCreateModal = true" class="btn btn-primary">
          <i class="fas fa-plus"></i>创建事件
        </button>
      </div>
    </div>

    <div class="card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <select v-model="filters.status" class="filter-select">
          <option value="">全部状态</option>
          <option value="PENDING_AUDIT">待审核</option>
          <option value="IN_AUDIT">审核中</option>
          <option value="AUDIT_REJECTED">已驳回</option>
          <option value="WAITING_DISPATCH">待派单</option>
          <option value="DISPATCHED_TO_WORK_ORDER">已派单</option>
          <option value="CLOSED">已关闭</option>
          <option value="IGNORED">已忽略</option>
        </select>
        <select v-model="filters.urgencyLevel" class="filter-select">
          <option value="">全部紧急程度</option>
          <option value="GREEN">一般（绿）</option>
          <option value="YELLOW">重点（黄）</option>
          <option value="RED">紧急（红）</option>
        </select>
        <select v-model="filters.sourceSystem" class="filter-select" @change="page = 1; loadData()">
          <option value="">全部来源</option>
          <option v-for="opt in reportSourceOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <!-- 日期范围：Element Plus daterange（点查询生效，clearable 自带清空） -->
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <button @click="loadData" class="filter-action"><i class="fas fa-search"></i> 查询</button>
        <label style="display:flex;align-items:center;gap:4px;font-size:13px;color:#6b7280;margin-left:8px;white-space:nowrap;">
          <input type="checkbox" v-model="showArchived" @change="loadData" /> 显示已归档
        </label>
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

      <!-- 数据表格（表体固定高度滚动，表头吸顶，撑满页面可视区） -->
      <template v-else>
        <div class="table-scroll" style="max-height:calc(100vh - 330px);min-height:240px;overflow-y:auto;">
        <table class="table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>标题</th>
              <th>状态</th>
              <th>紧急程度</th>
              <th>来源</th>
              <th>上报时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="e in displayList" :key="e.id">
              <td style="font-size:12px;color:#6b7280;">{{ e.eventCode }}</td>
              <td>
                <div style="display:flex;align-items:center;gap:6px;">
                  <span>{{ e.title }}</span>
                  <span v-if="e.archived" class="tag" style="background:#f3f4f6;color:#6b7280;border:1px solid #d1d5db;font-size:11px;">已归档</span>
                  <span v-if="e.hidden" class="tag" style="background:#f3f4f6;color:#6b7280;border:1px solid #d1d5db;font-size:11px;">已隐藏</span>
                </div>
              </td>
              <td>
                <span :class="['tag', e.currentStatus === 'CLOSED' ? 'tag-green' : e.currentStatus === 'DISPATCHED_TO_WORK_ORDER' ? 'tag-blue' : 'tag-orange']">
                  {{ statusLabel(e.currentStatus) }}
                </span>
              </td>
              <td>
                <span :class="['tag', e.urgencyLevel === 'RED' ? 'tag-red' : e.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']">
                  {{ e.urgencyLevel === 'RED' ? '紧急' : e.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ sourceLabel(e.sourceSystem || e.sourceType) }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ e.occurredAt }}</td>
              <td>
                <button @click="goDetail(e)" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;margin-right:4px;">详情</button>
                <button v-if="['PENDING_AUDIT', 'WAITING_DISPATCH', 'IN_AUDIT'].includes(e.currentStatus)" @click="goDetail(e)" style="padding:4px 10px;border:none;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;margin-right:4px;">操作</button>
                <button @click="toggleHidden(e)" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;margin-left:4px;color:#6b7280;">{{ e.hidden ? '显示' : '隐藏' }}</button>
                <button @click="handleDelete(e)" style="padding:4px 10px;border:1px solid #ffccc7;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;margin-left:4px;color:#ff4d4f;">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        </div>
        <div v-if="!displayList.length" class="empty-state">
          <i class="fas fa-inbox"></i>
          <p>暂无事件数据</p>
        </div>

        <!-- 分页：有数据即显示，单页也展示"共 X 条 第 1/1 页" -->
        <div v-if="total > 0" style="display:flex;align-items:center;justify-content:space-between;margin-top:16px;padding-top:16px;border-top:1px solid #e5e7eb;">
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
    <!-- 创建事件弹窗：使用 Element Plus el-dialog（下拉浮层 z-index 由 EP 统一管理，解决原生遮罩压住下拉的问题） -->
    <el-dialog
      v-model="showCreateModal"
      title="创建事件"
      width="760px"
      class="create-dialog"
      :close-on-click-modal="false"
      @close="showCreateModal = false"
    >
      <EventCreateView ref="createRef" embedded @cancel="showCreateModal = false" @created="onEventCreated" />
      <template #footer>
        <el-button @click="showCreateModal = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onCreate">创建事件</el-button>
      </template>
    </el-dialog>

    <!-- 事件详情弹窗（列表项点击直达，派单/关闭/归档操作后自动刷新列表；通过/驳回按钮在弹窗右下角） -->
    <div v-if="detailEventId" class="modal-overlay" @click.self="detailEventId = null">
      <div class="modal-box" style="width:960px;max-width:96vw;max-height:92vh;overflow-y:auto;">
        <EventDetailView embedded :event-id="detailEventId" @close="detailEventId = null" @changed="loadData" />
        <!-- 审核操作：仅待审核事件显示，位于弹窗右下角 -->
        <div v-if="detailEvent?.currentStatus === 'PENDING_AUDIT'" style="display:flex;justify-content:flex-end;gap:8px;padding-top:14px;border-top:1px solid #e5e7eb;margin-top:14px;position:sticky;bottom:0;background:#fff;">
          <button @click="handleAudit(detailEvent.id, 'reject')" style="padding:8px 24px;border:none;border-radius:6px;background:#ff4d4f;color:#fff;font-size:13px;cursor:pointer;">驳回</button>
          <button @click="handleAudit(detailEvent.id, 'pass')" style="padding:8px 24px;border:none;border-radius:6px;background:#52c41a;color:#fff;font-size:13px;cursor:pointer;">通过</button>
        </div>
      </div>
    </div>

    <!-- 审核确认弹窗（替代浏览器原生 prompt：取消仅关闭弹窗，不执行任何审核操作） -->
    <div v-if="auditModal.visible" class="modal-overlay" style="z-index:10000;">
      <div class="modal-box" style="width:420px;">
        <h3 style="font-size:15px;font-weight:600;margin-bottom:4px;">{{ auditModal.action === 'pass' ? '审核通过' : '审核驳回' }}</h3>
        <p style="font-size:12px;color:#9ca3af;margin-bottom:14px;">{{ auditModal.action === 'pass' ? '通过后事件将进入后续处置流程' : '驳回后事件将退回上报人' }}</p>
        <textarea v-model="auditModal.remark" rows="3" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;box-sizing:border-box;" :placeholder="auditModal.action === 'pass' ? '请输入通过备注（可选）' : '请输入驳回原因（必填）'"></textarea>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px;">
          <button @click="auditModal.visible = false" class="btn btn-default">取消</button>
          <button @click="confirmAudit" class="btn" :style="{ background: auditModal.action === 'pass' ? '#52c41a' : '#ff4d4f', color: '#fff' }">确认{{ auditModal.action === 'pass' ? '通过' : '驳回' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { getEvents, auditEvent, setEventHidden, deleteEvents, getDictItems } from '../api'
import EventCreateView from './EventCreateView.vue'
import EventDetailView from './EventDetailView.vue'
import { showMessage } from '../utils/message'
import { confirmDialog, promptDialog } from '../utils/dialog'

const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = ref(0)

// 弹窗交互：创建事件与事件详情均不离开列表页
const showCreateModal = ref(false)
const createRef = ref<InstanceType<typeof EventCreateView> | null>(null)
const createLoading = ref(false)

// 调用内嵌创建表单的 submit，并由父级管理弹窗 footer 的 loading 态
async function onCreate() {
  createLoading.value = true
  try {
    await createRef.value?.submit()
  } finally {
    createLoading.value = false
  }
}
const detailEventId = ref<string | number | null>(null)
const detailEvent = ref<any>(null)

function onEventCreated() {
  showCreateModal.value = false
  page.value = 1
  loadData()
}

const filters = reactive({
  status: '',
  urgencyLevel: '',
  sourceSystem: '',
  startDate: '',
  endDate: '',
})

// 日期范围筛选：[开始, 结束]，value 为 YYYY-MM-DD 字符串
const dateRange = ref<[string, string] | null>(null)
const showArchived = ref(false)

// 字典数据接口返回格式（与 src/api/index.ts 的 DictItem 一致）
interface DictItem {
  id: number
  dictCode: string
  itemValue: string
  itemLabel: string
  sortOrder: number
  status: string
  remark: string | null
}

// 事件来源系统选项（从字典接口动态获取）
const reportSourceOptions = ref<{ value: string; label: string }[]>([])

async function loadReportSources() {
  try {
    const items: DictItem[] = await getDictItems('event_report_source', true)
    if (Array.isArray(items) && items.length) {
      reportSourceOptions.value = items
        .filter((item: DictItem) => item.status === 'ACTIVE')
        .sort((a: DictItem, b: DictItem) => a.sortOrder - b.sortOrder)
        .map((item: DictItem) => ({
          value: item.itemValue,
          label: item.itemLabel
        }))
    }
  } catch (e) {
    reportSourceOptions.value = [
      { value: 'GRID_MEMBER', label: '网格员上报' },
      { value: 'RESIDENT', label: '居民上报' },
      { value: '12345', label: '12345转办' },
      { value: 'PROPERTY', label: '物业上报' },
      { value: 'AI_CAMERA', label: '智能监控抓拍' },
    ]
  }
}

// 前端过滤：默认隐藏已归档事件，保持办理界面清爽
const displayList = computed(() => {
  if (showArchived.value) return list.value
  return list.value.filter((e: any) => !e.archived)
})

function goDetail(e: any) {
  detailEvent.value = e
  detailEventId.value = e.id || e.externalEventId
}

// 展示隐藏切换：隐藏后监管大屏/GIS 等面板不再展示，仅本闭环列表可见
async function toggleHidden(e: any) {
  const target = !e.hidden
  const ok = await confirmDialog({
    title: target ? '隐藏事件' : '显示事件',
    message: target
      ? '隐藏后，该事件将不在监管大屏、全域态势、GIS 网格等面板展示，仅在事件闭环中可见。确定隐藏？'
      : '恢复后，该事件将重新在监管大屏、GIS 等面板展示。确定显示？',
    okText: target ? '隐藏' : '显示',
  })
  if (!ok) return
  try {
    await setEventHidden(e.id, target)
    showMessage(target ? '已隐藏，大屏/GIS 面板不再展示' : '已恢复展示', 'success')
    loadData()
  } catch (err: any) {
    showMessage(err?.message || '操作失败')
  }
}

// 删除事件：级联删除关联工单/审核记录/附件，不可恢复，需二次确认
async function handleDelete(e: any) {
  if (!e.id) { showMessage('该事件缺少主键 ID，无法删除'); return }
  const reason = await promptDialog({
    title: '删除事件',
    message: `确定删除事件「${e.title}」？删除后将同时清除其关联工单、审核记录与附件，不可恢复。请填写删除原因：`,
    placeholder: '请输入删除原因（必填）',
    required: true,
    rows: 2,
  })
  if (!reason) return
  try {
    await deleteEvents([e.id], reason)
    showMessage('事件已删除', 'success')
    // 当前页删空时回退一页，避免停留在空列表
    if (displayList.value.length <= 1 && page.value > 1) page.value--
    loadData()
  } catch (err: any) {
    showMessage(err?.message || '删除失败')
  }
}

// 审核确认弹窗状态：取消仅关闭弹窗，确认才执行审核
const auditModal = reactive({ visible: false, id: 0, action: '', remark: '' })

function handleAudit(id: number, action: string) {
  auditModal.id = id
  auditModal.action = action
  auditModal.remark = ''
  auditModal.visible = true
}

async function confirmAudit() {
  const { id, action, remark } = auditModal
  if (action === 'reject' && !remark.trim()) { showMessage('请填写驳回原因', 'warning'); return }
  try {
    await auditEvent(id, action, remark.trim())
    auditModal.visible = false
    showMessage(action === 'pass' ? '审核已通过' : '已驳回该事件', 'success')
    loadData()
  } catch (e: any) {
    showMessage(e?.message || '操作失败')
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '已通过',
    AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单',
    DISPATCHED_TO_WORK_ORDER: '已派单',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

function sourceLabel(source: string) {
  const found = reportSourceOptions.value.find(opt => opt.value === source)
  return found ? found.label : (source || '-')
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.status) params.status = filters.status
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (filters.sourceSystem) params.sourceSystem = filters.sourceSystem
    if (showArchived.value) params.includeArchived = true
    if (dateRange.value && dateRange.value.length === 2) {
      if (dateRange.value[0]) params.startDate = dateRange.value[0]
      if (dateRange.value[1]) params.endDate = dateRange.value[1]
    }
    const result = await getEvents(params)
    list.value = result?.items || result?.list || []
    total.value = result?.total || list.value.length
    totalPages.value = Math.max(1, Math.ceil(total.value / pageSize))
  } catch (e: any) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => filters.status, () => { page.value = 1; loadData() })
watch(() => filters.urgencyLevel, () => { page.value = 1; loadData() })

onMounted(async () => {
  await Promise.all([loadData(), loadReportSources()])
})
</script>

<style>
.el-dialog.create-dialog { display: flex; flex-direction: column; height: 77vh; max-height: 77vh; }
.el-dialog.create-dialog .el-dialog__header { flex-shrink: 0; }
.el-dialog.create-dialog .el-dialog__body { flex: 1 1 auto; overflow-y: auto; padding-top: 12px; }
.el-dialog.create-dialog .el-dialog__footer { flex-shrink: 0; padding: 12px 20px; text-align: right; }
</style>