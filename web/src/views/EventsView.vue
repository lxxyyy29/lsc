<template>
  <div>
    <div class="page-header" style="display:flex;align-items:center;justify-content:space-between;">
      <div>
        <h1 class="page-title">事件闭环处置</h1>
        <p class="page-desc">未被网格员手机端处理过的事件：上报→审核→派单→网格员处置前的全流程跟进</p>
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
        <select v-model="filters.status" class="filter-select" @change="page = 1; loadData()">
          <option value="">全部状态</option>
          <option value="PENDING_AUDIT">待审核</option>
          <option value="IN_AUDIT">审核中</option>
          <option value="AUDIT_APPROVED">已通过</option>
          <option value="WAITING_DISPATCH">待派单</option>
          <option value="WAITING_LEADER_REVIEW">组长审核</option>
          <option value="DISPATCHED_TO_WORK_ORDER">已派单（待处理）</option>
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
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <button @click="loadData" class="filter-action"><i class="fas fa-search"></i> 查询</button>
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
        <div class="table-scroll" style="max-height:calc(100vh - 400px);min-height:240px;overflow-y:auto;">
        <table class="table">
          <thead>
            <tr>
              <th>事件编号</th>
              <th>标题</th>
              <th>状态</th>
              <th>紧急程度</th>
              <th>来源</th>
              <th>上报时间</th>
              <th style="width:320px;">操作</th>
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
                <span :class="['tag', e.status === 'CLOSED' ? 'tag-green' : (e.status === 'DISPATCHED_TO_WORK_ORDER' || e.status === 'WAITING_LEADER_REVIEW') ? 'tag-blue' : 'tag-orange']">
                  {{ statusLabel(e.status) }}
                </span>
              </td>
              <td>
                <span :class="['tag', e.urgencyLevel === 'RED' ? 'tag-red' : e.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']">
                  {{ e.urgencyLevel === 'RED' ? '紧急' : e.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
                </span>
              </td>
              <td style="font-size:12px;color:#6b7280;">{{ sourceLabel(e.sourceSystem || e.sourceType) }}</td>
              <td style="font-size:12px;color:#6b7280;">{{ formatTime(e.occurredAt || e.createdAt) }}</td>
              <td>
                <div style="display:flex;flex-wrap:wrap;gap:4px;">
                  <button @click="goDetail(e)" style="padding:3px 8px;border:1px solid #1890ff;border-radius:4px;background:#fff;color:#1890ff;font-size:12px;cursor:pointer;">详情</button>
                  <button v-if="['PENDING_AUDIT', 'IN_AUDIT'].includes(e.status)" @click="handleAudit(e, 'pass')" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#52c41a;color:#fff;font-size:12px;cursor:pointer;">通过</button>
                  <button v-if="['PENDING_AUDIT', 'IN_AUDIT'].includes(e.status)" @click="handleAudit(e, 'reject')" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#ff4d4f;color:#fff;font-size:12px;cursor:pointer;">驳回</button>
                  <button v-if="e.status === 'WAITING_DISPATCH'" @click="openDispatch(e)" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#1890ff;color:#fff;font-size:12px;cursor:pointer;">派单</button>
                  <button v-if="e.status === 'WAITING_LEADER_REVIEW'" @click="openLeaderDispatch(e)" type="button" style="padding:3px 8px;border:none;border-radius:4px;background:#722ed1;color:#fff;font-size:12px;cursor:pointer;">组长派单</button>
                  <button @click="toggleHidden(e)" type="button" style="padding:3px 8px;border:1px solid #d1d5db;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;color:#6b7280;">{{ e.hidden ? '显示' : '隐藏' }}</button>
                  <button @click="handleDelete(e)" type="button" style="padding:3px 8px;border:1px solid #ffccc7;border-radius:4px;background:#fff;font-size:12px;cursor:pointer;color:#ff4d4f;">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        </div>
        <div v-if="!displayList.length" class="empty-state">
          <i class="fas fa-inbox"></i>
          <p>暂无事件数据</p>
        </div>

        <!-- 分页：有数据即显示 -->
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

    <!-- 创建事件弹窗 -->
    <el-dialog
      v-model="showCreateModal"
      title="创建事件"
      width="760px"
      class="create-dialog ui-dialog"
      :close-on-click-modal="false"
      @close="showCreateModal = false"
    >
      <EventCreateView ref="createRef" embedded @cancel="showCreateModal = false" @created="onEventCreated" />
      <template #footer>
        <el-button @click="showCreateModal = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="onCreate">创建事件</el-button>
      </template>
    </el-dialog>

    <!-- 事件详情弹窗（纯只读，无任何功能按钮） -->
    <div v-if="detailEventId" class="modal-overlay" @click.self="detailEventId = null">
      <div class="modal-box" style="width:960px;max-width:96vw;max-height:92vh;overflow-y:auto;">
        <EventDetailView embedded :event-id="detailEventId" @close="detailEventId = null" />
      </div>
    </div>

    <!-- 审核确认弹窗 -->
    <div v-if="auditModal.visible" class="modal-overlay" style="z-index:10000;">
      <div class="modal-box" style="width:420px;">
        <h3 style="font-size:15px;font-weight:600;margin-bottom:4px;">{{ auditModal.action === 'pass' ? '审核通过' : '审核驳回' }}</h3>
        <p style="font-size:12px;color:#9ca3af;margin-bottom:14px;">{{ auditModal.action === 'pass' ? '通过后事件将进入后续派单处置流程' : '驳回后事件将进入异常工单' }}</p>
        <textarea v-model="auditModal.remark" rows="3" style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;resize:vertical;box-sizing:border-box;" :placeholder="auditModal.action === 'pass' ? '请输入通过备注（可选）' : '请输入驳回原因（必填）'"></textarea>
        <div style="display:flex;justify-content:flex-end;gap:8px;margin-top:16px;">
          <button @click="auditModal.visible = false" class="btn btn-default">取消</button>
          <button @click="confirmAudit" class="btn" :style="{ background: auditModal.action === 'pass' ? '#52c41a' : '#ff4d4f', color: '#fff' }">确认{{ auditModal.action === 'pass' ? '通过' : '驳回' }}</button>
        </div>
      </div>
    </div>

    <!-- 派单弹窗（普通派单） -->
    <div v-if="showDispatch" class="modal-overlay" style="z-index:10000;">
      <div class="modal-box" style="width:480px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">派发工单</h3>
        <div class="form-group">
          <label class="form-label">选择受派人员 <span class="required">*</span></label>
          <select v-model="dispatchForm.assigneeUserId" class="form-select">
            <option :value="null">请选择受派人员</option>
            <option v-for="u in workers" :key="u.id" :value="Number(u.id)">{{ u.realName || u.username }}{{ u.roleNames ? `（${u.roleNames}）` : '' }}</option>
          </select>
          <p v-if="!workers.length" style="font-size:12px;color:#dc2626;margin-top:6px;">⚠️ 暂无可用人员，请先添加系统用户</p>
        </div>
        <div class="form-group">
          <label class="form-label">备注</label>
          <textarea v-model="dispatchForm.remark" rows="2" placeholder="派单备注..." class="form-textarea"></textarea>
        </div>
        <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
          <button @click="showDispatch = false" class="btn btn-default">取消</button>
          <button @click="confirmDispatch" class="btn btn-primary" :disabled="!dispatchForm.assigneeUserId">确认派单</button>
        </div>
      </div>
    </div>

    <!-- 组长派单弹窗（WAITING_LEADER_REVIEW 状态专用） -->
    <div v-if="showLeaderDispatch" class="modal-overlay" style="z-index:10000;">
      <div class="modal-box" style="width:560px;">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">组长派单 · 二级派发</h3>
        <div v-if="leaderDispatchLoading" style="text-align:center;padding:40px;color:#9ca3af;">
          <i class="fas fa-spinner fa-spin" style="font-size:20px;"></i>
          <p style="margin-top:8px;font-size:13px;">加载派单信息...</p>
        </div>
        <template v-else-if="leaderDispatchData">
          <div class="info-card" style="background:#fffbe6;border:1px solid #ffe58f;border-radius:8px;padding:12px 14px;margin-bottom:14px;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
              <span class="tag tag-blue">组长审核</span>
              <span :class="['tag', leaderDispatchData.event?.urgency === 'RED' ? 'tag-red' : leaderDispatchData.event?.urgency === 'YELLOW' ? 'tag-orange' : 'tag-green']">
                {{ leaderDispatchData.event?.urgencyLabel || '一般' }}
              </span>
            </div>
            <div style="font-size:14px;font-weight:600;color:#374151;">{{ leaderDispatchData.event?.title }}</div>
            <div v-if="leaderDispatchData.event?.location" style="font-size:12px;color:#6b7280;margin-top:4px;">📍 {{ leaderDispatchData.event.location }}</div>
          </div>
          <div v-if="leaderDispatchData.leaderFound" style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px 14px;margin-bottom:14px;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
              <span style="font-size:12px;background:#1890ff;color:#fff;border-radius:4px;padding:2px 8px;">网格组长</span>
            </div>
            <div style="font-size:14px;font-weight:600;color:#1e40af;">
              {{ leaderDispatchData.leader?.name }}
              <span style="font-size:12px;color:#1e40af;background:#dbeafe;border-radius:4px;padding:2px 6px;margin-left:6px;">
                {{ leaderDispatchData.leader?.positionLabel || '网格组长' }}
              </span>
            </div>
            <div style="font-size:12px;color:#6b7280;margin-top:2px;">当前事件需由{{ leaderDispatchData.leader?.name }}组长派发下属网格员处理</div>
          </div>
          <div v-else style="background:#fff1f0;border:1px solid #ffa39e;border-radius:8px;padding:12px 14px;margin-bottom:14px;">
            <p style="font-size:13px;color:#cf1324;">⚠️ {{ leaderDispatchData.reason || '未找到网格组长' }}</p>
          </div>
          <div v-if="leaderDispatchData.leaderFound" class="form-group">
            <label class="form-label">选择下属网格员 <span class="required">*</span></label>
            <select v-model="leaderDispatchForm.assigneeUserId" class="form-select">
              <option :value="null">请选择下属网格员</option>
              <option v-for="s in leaderDispatchData.subordinates" :key="s.userId" :value="Number(s.userId)">
                {{ s.name }}（待办 {{ s.pendingCount || 0 }} 条）
              </option>
            </select>
            <p v-if="!leaderDispatchData.subordinates?.length" style="font-size:12px;color:#dc2626;margin-top:6px;">⚠️ 该网格暂无下属网格员，请先在组织管理中添加</p>
          </div>
          <div class="form-group">
            <label class="form-label">派单备注</label>
            <textarea v-model="leaderDispatchForm.remark" rows="2" placeholder="派单备注（选填）..." class="form-textarea"></textarea>
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
            <button @click="showLeaderDispatch = false" class="btn btn-default">取消</button>
            <button @click="confirmLeaderDispatch" class="btn btn-primary" :disabled="!leaderDispatchForm.assigneeUserId || !leaderDispatchData.leaderFound">确认派单</button>
          </div>
        </template>
        <template v-else>
          <p style="font-size:13px;color:#6b7280;text-align:center;padding:20px;">无法加载派单信息</p>
          <div style="display:flex;justify-content:flex-end;">
            <button @click="showLeaderDispatch = false" class="btn btn-default">关闭</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { getEventSectionEvents, auditEvent, setEventHidden, deleteEvents, getDictItems, dispatchEvent, getSystemUsers, getLeaderDispatchInfo, leaderDispatch } from '../api'
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

// 详情弹窗（纯只读）
const detailEventId = ref<string | number | null>(null)

async function onCreate() {
  createLoading.value = true
  try {
    await createRef.value?.submit()
  } finally {
    createLoading.value = false
  }
}

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

const dateRange = ref<[string, string] | null>(null)

interface DictItem {
  id: number
  dictCode: string
  itemValue: string
  itemLabel: string
  sortOrder: number
  status: string
  remark: string | null
}

const reportSourceOptions = ref<{ value: string; label: string }[]>([])

async function loadReportSources() {
  try {
    const items: DictItem[] = await getDictItems('event_report_source', true)
    if (Array.isArray(items) && items.length) {
      reportSourceOptions.value = items
        .filter((item: DictItem) => item.status === 'ACTIVE')
        .sort((a: DictItem, b: DictItem) => a.sortOrder - b.sortOrder)
        .map((item: DictItem) => ({ value: item.itemValue, label: item.itemLabel }))
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

const displayList = computed(() => list.value)

function goDetail(e: any) {
  detailEventId.value = e.id || e.externalEventId
}

async function toggleHidden(e: any) {
  const target = !e.hidden
  const ok = await confirmDialog({
    title: target ? '隐藏事件' : '显示事件',
    message: target
      ? '隐藏后，该事件将不在监管大屏、全域态势、GIS 网格等面板展示。确定隐藏？'
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

// 删除事件（软删除）：需填写删除原因，异常工单可查
async function handleDelete(e: any) {
  if (!e.id) { showMessage('该事件缺少主键 ID，无法删除'); return }
  const reason = await promptDialog({
    title: '删除事件',
    message: `确定删除事件「${e.title}」？删除后可在"异常工单"中查看，不可恢复。请填写删除原因：`,
    placeholder: '请输入删除原因（必填）',
    required: true,
    rows: 2,
  })
  if (!reason) return
  try {
    await deleteEvents([e.id], reason)
    showMessage('事件已删除', 'success')
    if (displayList.value.length <= 1 && page.value > 1) page.value--
    loadData()
  } catch (err: any) {
    showMessage(err?.message || '删除失败')
  }
}

// 审核确认弹窗（接入审核：通过/驳回）
const auditModal = reactive({ visible: false, id: 0, action: '', remark: '' })

function handleAudit(e: any, action: string) {
  auditModal.id = e.id
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
    showMessage(action === 'pass' ? '审核已通过' : '已驳回，事件进入异常工单', 'success')
    loadData()
  } catch (e: any) {
    showMessage(e?.message || '操作失败')
  }
}

// 派单弹窗（普通派单）
const showDispatch = ref(false)
const dispatchEventId = ref<number | null>(null)
const dispatchForm = ref({ assigneeUserId: null as number | null, remark: '' })
const workers = ref<any[]>([])

async function openDispatch(e: any) {
  dispatchEventId.value = e.id
  dispatchForm.value = { assigneeUserId: null, remark: '' }
  try {
    const users: any[] = await getSystemUsers()
    workers.value = (Array.isArray(users) ? users : []).filter((u: any) => u.status === 'ACTIVE')
  } catch (err) {
    workers.value = []
  }
  showDispatch.value = true
}

async function confirmDispatch() {
  if (!dispatchForm.value.assigneeUserId) { showMessage('请选择受派人员'); return }
  try {
    await dispatchEvent(dispatchEventId.value!, {
      assigneeUserId: dispatchForm.value.assigneeUserId,
      remark: dispatchForm.value.remark
    })
    showDispatch.value = false
    showMessage('派单成功', 'success')
    loadData()
  } catch (e: any) {
    showMessage(e?.message || '派单失败')
  }
}

// 组长派单弹窗（WAITING_LEADER_REVIEW 专用）
const showLeaderDispatch = ref(false)
const leaderDispatchEventId = ref<number | null>(null)
const leaderDispatchData = ref<any>(null)
const leaderDispatchForm = ref({ assigneeUserId: null as number | null, remark: '' })
const leaderDispatchLoading = ref(false)

async function openLeaderDispatch(e: any) {
  leaderDispatchEventId.value = e.id
  leaderDispatchForm.value = { assigneeUserId: null, remark: '' }
  leaderDispatchData.value = null
  leaderDispatchLoading.value = true
  showLeaderDispatch.value = true
  try {
    leaderDispatchData.value = await getLeaderDispatchInfo(e.id)
    const subs = leaderDispatchData.value?.subordinates || []
    if (subs.length) {
      leaderDispatchForm.value.assigneeUserId = Number(subs[0].userId)
    }
  } catch (err: any) {
    showMessage(err?.message || '加载派单信息失败')
    showLeaderDispatch.value = false
  } finally {
    leaderDispatchLoading.value = false
  }
}

async function confirmLeaderDispatch() {
  if (!leaderDispatchForm.value.assigneeUserId) { showMessage('请选择下属网格员'); return }
  try {
    await leaderDispatch(leaderDispatchEventId.value!, {
      assigneeUserId: leaderDispatchForm.value.assigneeUserId,
      remark: leaderDispatchForm.value.remark
    })
    showLeaderDispatch.value = false
    showMessage('组长派单成功', 'success')
    loadData()
  } catch (e: any) {
    showMessage(e?.message || '组长派单失败')
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    AUDIT_APPROVED: '已通过',
    AUDIT_REJECTED: '已驳回',
    WAITING_DISPATCH: '待派单',
    WAITING_LEADER_REVIEW: '组长审核',
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

function formatTime(value: any) {
  if (!value) return '-'
  const date = new Date(String(value).replace(' ', 'T'))
  if (isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const params: any = { page: page.value, size: pageSize }
    if (filters.status) params.status = filters.status
    if (filters.urgencyLevel) params.urgencyLevel = filters.urgencyLevel
    if (filters.sourceSystem) params.sourceSystem = filters.sourceSystem
    if (dateRange.value && dateRange.value.length === 2) {
      if (dateRange.value[0]) params.startDate = dateRange.value[0]
      if (dateRange.value[1]) params.endDate = dateRange.value[1]
    }
    const result = await getEventSectionEvents('closed-loop', params)
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
