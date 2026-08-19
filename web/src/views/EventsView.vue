<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">事件闭环处置</h1>
        <p class="page-desc">发现上报→智能派单→现场处置→复核核查→归档</p>
      </div>
      <div style="display:flex;gap:8px;">
        <button @click="show12345Dialog = true" class="btn btn-default">
          <i class="fas fa-phone-alt"></i>12345 登记
        </button>
        <button @click="showPropertyDialog = true" class="btn btn-default">
          <i class="fas fa-building"></i>物业上报
        </button>
        <button @click="showCreateModal = true" class="btn btn-primary">
          <i class="fas fa-plus"></i>创建事件
        </button>
      </div>
    </div>

    <!-- 12345 热线转办登记弹窗 -->
    <div v-if="show12345Dialog" class="modal-overlay" @click.self="show12345Dialog = false">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">12345 热线转办登记</h3>
        <div class="form-group">
          <label class="form-label">标题 <span class="required">*</span></label>
          <input v-model="form12345.title" class="form-input" placeholder="请输入标题" />
        </div>
        <div class="form-group">
          <label class="form-label">描述</label>
          <textarea v-model="form12345.description" class="form-textarea" rows="2" placeholder="请输入描述..."></textarea>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">事件类型</label>
            <select v-model="form12345.eventType" class="form-select">
              <option value="COMPLAINT">市民投诉</option>
              <option value="FIRE">消防安全</option>
              <option value="ILLEGAL_BUILDING">违章建筑</option>
              <option value="PUBLIC_SAFETY">公共安全</option>
              <option value="SAFETY">安全生产</option>
              <option value="ENVIRONMENT">市容环境</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">地点</label>
            <input v-model="form12345.location" class="form-input" placeholder="请输入地点" />
          </div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">来电人</label>
            <input v-model="form12345.reporterName" class="form-input" placeholder="请输入来电人" />
          </div>
          <div class="form-group">
            <label class="form-label">来电电话</label>
            <input v-model="form12345.reporterPhone" class="form-input" placeholder="请输入电话" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">转办单号</label>
          <input v-model="form12345.externalNo" class="form-input" placeholder="请输入 12345 转办单号" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="show12345Dialog = false" class="btn btn-default">取消</button>
          <button @click="submit12345" class="btn btn-primary">提交登记</button>
        </div>
      </div>
    </div>

    <!-- 物业上报登记弹窗 -->
    <div v-if="showPropertyDialog" class="modal-overlay" @click.self="showPropertyDialog = false">
      <div class="modal-box">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">物业上报登记</h3>
        <div class="form-group">
          <label class="form-label">标题 <span class="required">*</span></label>
          <input v-model="formProperty.title" class="form-input" placeholder="请输入标题" />
        </div>
        <div class="form-group">
          <label class="form-label">描述</label>
          <textarea v-model="formProperty.description" class="form-textarea" rows="2" placeholder="请输入描述..."></textarea>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <div class="form-group">
            <label class="form-label">事件类型</label>
            <select v-model="formProperty.eventType" class="form-select">
              <option value="COMPLAINT">市民投诉</option>
              <option value="FIRE">消防安全</option>
              <option value="ENVIRONMENT">市容环境</option>
              <option value="REPAIR">物业报修</option>
              <option value="OTHER">其他</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">物业/小区</label>
            <input v-model="formProperty.propertyName" class="form-input" placeholder="请输入物业或小区名称" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">地点</label>
          <input v-model="formProperty.location" class="form-input" placeholder="请输入具体地点" />
        </div>
        <div class="form-group">
          <label class="form-label">上报人</label>
          <input v-model="formProperty.reporterName" class="form-input" placeholder="请输入上报人" />
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px;">
          <button @click="showPropertyDialog = false" class="btn btn-default">取消</button>
          <button @click="submitProperty" class="btn btn-primary">提交登记</button>
        </div>
      </div>
    </div>

    <div class="card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <select v-model="filters.status" class="form-select" style="width:auto;">
          <option value="">全部状态</option>
          <option value="PENDING_AUDIT">待审核</option>
          <option value="IN_AUDIT">审核中</option>
          <option value="WAITING_DISPATCH">待派单</option>
          <option value="DISPATCHED_TO_WORK_ORDER">已派单</option>
          <option value="CLOSED">已关闭</option>
          <option value="IGNORED">已忽略</option>
        </select>
        <select v-model="filters.urgencyLevel" class="form-select" style="width:auto;">
          <option value="">全部紧急程度</option>
          <option value="GREEN">一般（绿）</option>
          <option value="YELLOW">重点（黄）</option>
          <option value="RED">紧急（红）</option>
        </select>
        <input v-model="filters.startDate" type="date" class="form-input" style="width:auto;" />
        <input v-model="filters.endDate" type="date" class="form-input" style="width:auto;" />
        <button @click="loadData" class="btn btn-primary">查询</button>
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

      <!-- 数据表格 -->
      <template v-else>
        <table class="table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>标题</th>
              <th>状态</th>
              <th>紧急程度</th>
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
              <td style="font-size:12px;color:#6b7280;">{{ e.occurredAt }}</td>
              <td>
                <button @click="goDetail(e)" style="padding:4px 10px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;margin-right:4px;">详情</button>
                <button v-if="e.currentStatus === 'PENDING_AUDIT'" @click="handleAudit(e.id, 'pass')" style="padding:4px 10px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;margin-right:4px;">通过</button>
                <button v-if="e.currentStatus === 'PENDING_AUDIT'" @click="handleAudit(e.id, 'reject')" style="padding:4px 10px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:12px;cursor:pointer;margin-right:4px;">驳回</button>
                <button v-if="['WAITING_DISPATCH', 'IN_AUDIT'].includes(e.currentStatus)" @click="goDetail(e)" style="padding:4px 10px;border:none;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">派单</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!displayList.length" class="empty-state">
          <i class="fas fa-inbox"></i>
          <p>暂无事件数据</p>
        </div>

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
    <!-- 创建事件弹窗（内嵌创建表单，提交后直接刷新列表） -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal-box" style="width:760px;max-width:94vw;max-height:90vh;overflow-y:auto;">
        <EventCreateView embedded @cancel="showCreateModal = false" @created="onEventCreated" />
      </div>
    </div>

    <!-- 事件详情弹窗（列表项点击直达，派单/关闭/归档操作后自动刷新列表） -->
    <div v-if="detailEventId" class="modal-overlay" @click.self="detailEventId = null">
      <div class="modal-box" style="width:960px;max-width:96vw;max-height:92vh;overflow-y:auto;">
        <EventDetailView embedded :event-id="detailEventId" @close="detailEventId = null" @changed="loadData" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { getEvents, auditEvent, importFrom12345, reportFromProperty } from '../api'
import EventCreateView from './EventCreateView.vue'
import EventDetailView from './EventDetailView.vue'

const list = ref<any[]>([])
const loading = ref(true)
const error = ref('')
const page = ref(1)
const pageSize = 20
const total = ref(0)
const totalPages = ref(0)

// 弹窗交互：创建事件与事件详情均不离开列表页
const showCreateModal = ref(false)
const detailEventId = ref<string | number | null>(null)

function onEventCreated() {
  showCreateModal.value = false
  page.value = 1
  loadData()
}

const filters = reactive({
  status: '',
  urgencyLevel: '',
  startDate: '',
  endDate: '',
})
const showArchived = ref(false)

// 多渠道登记弹窗
const show12345Dialog = ref(false)
const showPropertyDialog = ref(false)
const submitting12345 = ref(false)
const submittingProperty = ref(false)

const form12345 = reactive({
  title: '',
  description: '',
  eventType: 'COMPLAINT',
  location: '',
  reporterName: '',
  reporterPhone: '',
  externalNo: ''
})

const formProperty = reactive({
  title: '',
  description: '',
  eventType: 'COMPLAINT',
  location: '',
  propertyName: '',
  reporterName: ''
})

async function submit12345() {
  if (!form12345.title.trim()) { alert('请填写标题'); return }
  submitting12345.value = true
  try {
    await importFrom12345(form12345)
    show12345Dialog.value = false
    Object.assign(form12345, { title: '', description: '', eventType: 'COMPLAINT', location: '', reporterName: '', reporterPhone: '', externalNo: '' })
    loadData()
  } catch (e: any) {
    alert(e?.message || '登记失败')
  } finally {
    submitting12345.value = false
  }
}

async function submitProperty() {
  if (!formProperty.title.trim()) { alert('请填写标题'); return }
  submittingProperty.value = true
  try {
    await reportFromProperty(formProperty)
    showPropertyDialog.value = false
    Object.assign(formProperty, { title: '', description: '', eventType: 'COMPLAINT', location: '', propertyName: '', reporterName: '' })
    loadData()
  } catch (e: any) {
    alert(e?.message || '登记失败')
  } finally {
    submittingProperty.value = false
  }
}

// 前端过滤：默认隐藏已归档事件，保持办理界面清爽
const displayList = computed(() => {
  if (showArchived.value) return list.value
  return list.value.filter((e: any) => !e.archived)
})

function goDetail(e: any) {
  detailEventId.value = e.id || e.externalEventId
}

async function handleAudit(id: number, action: string) {
  const remark = action === 'pass' ? prompt('请输入通过备注（可选）') : prompt('请输入驳回原因')
  if (action === 'reject' && !remark) { alert('请填写驳回原因'); return }
  try {
    await auditEvent(id, action, remark || '')
    loadData()
  } catch (e: any) {
    alert(e?.message || '操作失败')
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

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.status) params.status = filters.status
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (filters.startDate) params.startDate = filters.startDate
    if (filters.endDate) params.endDate = filters.endDate
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

onMounted(loadData)
</script>
