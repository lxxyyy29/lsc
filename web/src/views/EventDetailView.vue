<template>
  <div>
    <div v-if="loading" style="text-align:center;padding:60px;color:#9ca3af;">
      <i class="fas fa-spinner fa-spin" style="font-size:24px;"></i>
      <p style="margin-top:12px;">加载中...</p>
    </div>

    <div v-else-if="event">
      <!-- 顶部操作栏 -->
      <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;">
        <div>
          <h2 style="font-size:20px;font-weight:600;">{{ event.title }}</h2>
          <div style="display:flex;align-items:center;gap:8px;margin-top:6px;">
            <span :class="['tag', event.urgencyLevel === 'RED' ? 'tag-red' : event.urgencyLevel === 'YELLOW' ? 'tag-orange' : 'tag-green']">
              {{ event.urgencyLevel === 'RED' ? '紧急' : event.urgencyLevel === 'YELLOW' ? '重点' : '一般' }}
            </span>
            <span class="tag tag-blue">{{ statusLabel(event.currentStatus) }}</span>
            <span v-if="event.archived" class="tag" style="background:#f3f4f6;color:#6b7280;border:1px solid #d1d5db;">已归档</span>
            <span style="font-size:12px;color:#9ca3af;">{{ event.eventCode }}</span>
          </div>
        </div>
        <div style="display:flex;gap:8px;">
          <button v-if="(event.currentStatus === 'CLOSED' || event.currentStatus === 'IGNORED') && !event.archived" @click="handleArchive" style="padding:8px 16px;border:1px solid #6b7280;border-radius:6px;background:#fff;color:#374151;font-size:13px;cursor:pointer;">归档</button>
          <button v-if="event.currentStatus === 'CLOSED'" @click="handleReopen" style="padding:8px 16px;border:1px solid #52c41a;border-radius:6px;background:#fff;color:#52c41a;font-size:13px;cursor:pointer;">重新打开</button>
          <button @click="handleBack" :title="embedded ? '关闭' : '返回'" style="width:32px;height:32px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:18px;line-height:1;color:#6b7280;cursor:pointer;display:flex;align-items:center;justify-content:center;">×</button>
        </div>
      </div>

      <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
        <!-- 左侧：基本信息 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">基本信息</h3>
          <div style="font-size:13px;color:#374151;line-height:2;">
            <div><span style="color:#9ca3af;">事件类型：</span>{{ getEventTypeName(event.eventType) }}</div>
            <div><span style="color:#9ca3af;">发生时间：</span>{{ event.occurredAt || '-' }}</div>
            <div><span style="color:#9ca3af;">事发地点：</span>{{ event.location || '-' }}</div>
            <div><span style="color:#9ca3af;">所属网格：</span>{{ event.gridName || '-' }}</div>
            <div><span style="color:#9ca3af;">上报人：</span>{{ getReportSourceName(event.reportSource) }}</div>
            <div><span style="color:#9ca3af;">来源系统：</span>{{ getSourceSystemName(event.sourceSystem) }}</div>
            <div v-if="event.description" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;"><span style="color:#9ca3af;">详细描述：</span><br/>{{ event.description }}</div>
            <!-- 现场照片（创建事件时上传的证据图片，image 预览模式：点击打开全屏预览浮层，不跳转链接） -->
            <div v-if="event.evidenceReferences && event.evidenceReferences.length" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;">
              <span style="color:#9ca3af;">现场照片（{{ event.evidenceReferences.length }}）：</span>
              <div style="display:flex;flex-wrap:wrap;gap:8px;margin-top:8px;">
                <img v-for="(url, idx) in event.evidenceReferences" :key="idx" :src="url" @click="previewIdx = Number(idx)" title="点击预览大图" style="width:72px;height:72px;object-fit:cover;border-radius:6px;border:1px solid #e5e7eb;cursor:zoom-in;" />
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：生命周期时间轴 -->
        <div class="card">
          <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">处置时间轴</h3>
          <div v-if="timeline.length" style="position:relative;padding-left:20px;">
            <div style="position:absolute;left:7px;top:0;bottom:0;width:2px;background:#e5e7eb;"></div>
            <div v-for="(item, idx) in timeline" :key="idx" style="position:relative;margin-bottom:16px;">
              <div style="position:absolute;left:-17px;top:4px;width:10px;height:10px;border-radius:50%;background:#1890ff;border:2px solid #fff;"></div>
              <div style="font-size:13px;font-weight:600;color:#374151;">{{ item.action }}</div>
              <div style="font-size:12px;color:#6b7280;margin-top:2px;">{{ item.remark }}</div>
              <div style="font-size:11px;color:#9ca3af;margin-top:2px;">{{ item.occurredAt }}</div>
            </div>
          </div>
          <p v-else style="font-size:12px;color:#9ca3af;text-align:center;padding:20px;">暂无操作记录</p>
        </div>
      </div>

      <!-- 派单弹窗（z-index 高于外层详情弹窗，避免嵌套时被遮挡） -->
      <div v-if="showDispatch" class="modal-overlay" style="z-index:10000;">
        <div class="modal-box">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">派发工单</h3>

          <!-- 智能推荐卡片 -->
          <div v-if="dispatchSuggestion" style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:12px 14px;margin-bottom:14px;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
              <span style="font-size:12px;background:#1890ff;color:#fff;border-radius:4px;padding:2px 8px;">⚡ 智能推荐</span>
              <span style="font-size:14px;font-weight:600;color:#1e40af;">
                {{ dispatchSuggestion.recommendedUserName || '暂无可用人员' }}
              </span>
              <span v-if="dispatchSuggestion.recommendedUserName" style="font-size:12px;color:#1e40af;background:#dbeafe;border-radius:4px;padding:2px 6px;">
                {{ dispatchSuggestion.roleLabel }}
              </span>
            </div>
            <p style="font-size:12px;color:#1e40af;line-height:1.6;">{{ dispatchSuggestion.reason }}</p>
            <p v-if="dispatchSuggestion.candidates?.length" style="font-size:12px;color:#6b7280;margin-top:4px;">
              候选（按待办量排序）：{{ dispatchSuggestion.candidates.map((c: any) => `${c.name}（待办${c.pendingCount}）`).join('、') }}
            </p>
          </div>

          <div class="form-group">
            <label class="form-label">选择受派人员 <span class="required">*</span></label>
            <select v-model="dispatchForm.assigneeUserId" class="form-select">
              <option :value="null">请选择受派人员</option>
              <option v-for="u in filteredWorkers" :key="u.id" :value="Number(u.id)">{{ u.realName || u.username }}{{ u.roleNames ? `（${u.roleNames}）` : '' }}</option>
            </select>
            <p style="font-size:12px;color:#1890ff;margin-top:6px;">
              ℹ️ 该事件为{{ recommendedRoleCode === 'EVENT_OPERATOR' ? '重点事件' : '简易事件' }}，建议派{{ recommendedRoleLabel }}处理
            </p>
            <p v-if="!filteredWorkers.length" style="font-size:12px;color:#dc2626;margin-top:6px;">
              ⚠️ 暂无可用人员，请先添加系统用户
            </p>
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea v-model="dispatchForm.remark" rows="2" placeholder="派单备注..." class="form-textarea"></textarea>
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
            <button @click="showDispatch = false" class="btn btn-default">取消</button>
            <button v-if="dispatchSuggestion?.recommendedUserId" @click="handleSmartDispatch" style="padding:8px 16px;border:none;border-radius:6px;background:#722ed1;color:#fff;font-size:13px;cursor:pointer;">⚡ 一键智能派单</button>
            <button @click="handleDispatch" class="btn btn-primary">手动确认派单</button>
          </div>
        </div>
      </div>

      <!-- 组长派单弹窗（WAITING_LEADER_REVIEW 状态专用） -->
      <div v-if="showLeaderDispatch" class="modal-overlay" style="z-index:10000;">
        <div class="modal-box" style="width:560px;">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">组长派单 · 二级派发</h3>

          <!-- 加载中 -->
          <div v-if="leaderDispatchLoading" style="text-align:center;padding:40px;color:#9ca3af;">
            <i class="fas fa-spinner fa-spin" style="font-size:20px;"></i>
            <p style="margin-top:8px;font-size:13px;">加载派单信息...</p>
          </div>

          <template v-else-if="leaderDispatchData">
            <!-- 事件信息卡片 -->
            <div class="info-card" style="background:#fffbe6;border:1px solid #ffe58f;border-radius:8px;padding:12px 14px;margin-bottom:14px;">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
                <span class="tag tag-blue">{{ leaderDispatchData.event?.statusLabel || '组长审核' }}</span>
                <span :class="['tag', leaderDispatchData.event?.urgency === 'RED' ? 'tag-red' : leaderDispatchData.event?.urgency === 'YELLOW' ? 'tag-orange' : 'tag-green']">
                  {{ leaderDispatchData.event?.urgencyLabel || '一般' }}
                </span>
              </div>
              <div style="font-size:14px;font-weight:600;color:#374151;">{{ leaderDispatchData.event?.title }}</div>
              <div v-if="leaderDispatchData.event?.location" style="font-size:12px;color:#6b7280;margin-top:4px;">📍 {{ leaderDispatchData.event.location }}</div>
            </div>

            <!-- 组长信息卡片 -->
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

            <!-- 下属网格员选择 -->
            <div v-if="leaderDispatchData.leaderFound" class="form-group">
              <label class="form-label">选择下属网格员 <span class="required">*</span></label>
              <select v-model="leaderDispatchForm.assigneeUserId" class="form-select">
                <option :value="null">请选择下属网格员</option>
                <option v-for="s in leaderDispatchData.subordinates" :key="s.userId" :value="Number(s.userId)">
                  {{ s.name }}（待办 {{ s.pendingCount || 0 }} 条）
                </option>
              </select>
              <p v-if="!leaderDispatchData.subordinates?.length" style="font-size:12px;color:#dc2626;margin-top:6px;">
                ⚠️ 该网格暂无下属网格员，请先在组织管理中添加
              </p>
            </div>

            <div class="form-group">
              <label class="form-label">派单备注</label>
              <textarea v-model="leaderDispatchForm.remark" rows="2" placeholder="派单备注（选填）..." class="form-textarea"></textarea>
            </div>

            <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
              <button @click="showLeaderDispatch = false" class="btn btn-default">取消</button>
              <button @click="handleLeaderDispatch" class="btn btn-primary" :disabled="!leaderDispatchForm.assigneeUserId">确认派单</button>
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

      <!-- 关闭弹窗 -->
      <div v-if="showClose" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:10000;">
        <div style="background:#fff;border-radius:12px;padding:24px;width:400px;max-width:90vw;">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">关闭事件</h3>
          <div style="margin-bottom:16px;">
            <label style="display:block;font-size:13px;font-weight:600;margin-bottom:4px;">关闭原因 <span style="color:#ff4d4f;">*</span></label>
            <textarea v-model="closeReason" rows="3" placeholder="请输入关闭原因..." style="width:100%;padding:8px 12px;border:1px solid #d1d5db;border-radius:6px;font-size:13px;"></textarea>
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;">
            <button @click="showClose = false" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">取消</button>
            <button @click="handleClose" style="padding:8px 16px;border:none;border-radius:6px;background:#ff4d4f;color:#fff;font-size:13px;cursor:pointer;">确认关闭</button>
          </div>
        </div>
      </div>
      <!-- 现场照片预览浮层（image 预览模式：全屏遮罩 + 大图 + 左右切换，点击遮罩或 × 关闭） -->
      <div v-if="previewIdx !== null && event.evidenceReferences?.length" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.85);display:flex;align-items:center;justify-content:center;z-index:10001;" @click.self="previewIdx = null">
        <button @click="previewIdx = null" title="关闭" style="position:absolute;top:20px;right:24px;width:36px;height:36px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">×</button>
        <button v-if="event.evidenceReferences.length > 1" @click="previewIdx = (previewIdx - 1 + event.evidenceReferences.length) % event.evidenceReferences.length" title="上一张" style="position:absolute;left:24px;top:50%;transform:translateY(-50%);width:40px;height:40px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">‹</button>
        <img :src="event.evidenceReferences[previewIdx]" style="max-width:80vw;max-height:85vh;object-fit:contain;border-radius:8px;" />
        <button v-if="event.evidenceReferences.length > 1" @click="previewIdx = (previewIdx + 1) % event.evidenceReferences.length" title="下一张" style="position:absolute;right:24px;top:50%;transform:translateY(-50%);width:40px;height:40px;border-radius:50%;border:none;background:rgba(255,255,255,0.2);color:#fff;font-size:20px;cursor:pointer;">›</button>
        <span style="position:absolute;bottom:24px;left:50%;transform:translateX(-50%);color:#fff;font-size:13px;">{{ previewIdx + 1 }} / {{ event.evidenceReferences.length }}</span>
      </div>
    </div>

    <div v-else style="text-align:center;padding:60px;color:#9ca3af;">事件不存在</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEventDetail, getEventTimeline, closeEvent, reopenEvent, dispatchEvent, getSystemUsers, archiveEvent, getDispatchSuggestion, smartDispatchEvent, getLeaderDispatchInfo, leaderDispatch } from '../api'
import { getEventTypeName, getReportSourceName, getSourceSystemName } from '../utils/eventTypes'
import { showMessage } from '../utils/message'
import { confirmDialog } from '../utils/dialog'

const route = useRoute()
const router = useRouter()
// embedded=true 时作为弹窗内嵌组件：事件 id 由 props 传入，操作变更后 emit 通知父组件刷新列表
const props = withDefaults(defineProps<{ embedded?: boolean; eventId?: string | number | null }>(), { embedded: false, eventId: null })
const emit = defineEmits<{ (e: 'close'): void; (e: 'changed'): void }>()
defineExpose({ triggerDispatch: () => { openDispatchDialog() } })
// 弹窗模式优先用 props 传入的 id，页面模式回退到路由参数
const eventId = computed(() => props.eventId ?? route.params.id)

const event = ref<any>(null)
const timeline = ref<any[]>([])
const loading = ref(true)
const showDispatch = ref(false)
const showLeaderDispatch = ref(false)
const showClose = ref(false)
const closeReason = ref('')
// 组长派单：存储派单信息（组长、下属、事件）
const leaderDispatchData = ref<any>(null)
const leaderDispatchForm = ref({ assigneeUserId: null as number | null, remark: '' })
const leaderDispatchLoading = ref(false)
// 现场照片预览：非 null 时打开全屏预览浮层，值为当前预览下标
const previewIdx = ref<number | null>(null)
const workers = ref<any[]>([])
const dispatchForm = ref({ assigneeUserId: null as number | null, remark: '' })
const dispatchSuggestion = ref<any>(null)

// 重点事件类型 → 推荐两委干部(EVENT_OPERATOR)；其余简易事件 → 网格员(H5_WORKER)
const SERIOUS_EVENT_TYPES = new Set([
  'COMPLAINT', 'FIRE', 'ILLEGAL_BUILDING', 'PUBLIC_SAFETY', 'SAFETY', 'SAFE',
  '民生诉求', '消防安全', '违建', '公共安全', '安全生产', '矛盾纠纷', '防汛防台风'
])

const recommendedRoleCode = computed(() => {
  const eventType = event.value?.eventType
  return eventType && SERIOUS_EVENT_TYPES.has(eventType) ? 'EVENT_OPERATOR' : 'H5_WORKER'
})

const recommendedRoleLabel = computed(() =>
  recommendedRoleCode.value === 'EVENT_OPERATOR' ? '两委干部' : '网格员'
)

const filteredWorkers = computed(() => {
  const role = recommendedRoleCode.value
  const matched = workers.value.filter((user: any) =>
    user.status === 'ACTIVE' && Array.isArray(user.roleCodes) && user.roleCodes.includes(role)
  )
  // 若推荐角色无人，回退到展示全部活跃人员
  return matched.length > 0 ? matched : workers.value.filter((user: any) => user.status === 'ACTIVE')
})

const selectedWorker = computed(() =>
  workers.value.find((worker) => worker.id === dispatchForm.value.assigneeUserId) || null
)

const selectedWorkerName = computed(() =>
  selectedWorker.value ? (selectedWorker.value.realName || selectedWorker.value.username) : ''
)

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

async function loadData() {
  loading.value = true
  try {
    // 支持数字 id 与字符串 externalEventId 两种参数（列表项 id 缺失时用外部事件ID跳转）
    const idParam = String(eventId.value || '')
    event.value = await getEventDetail(idParam)
    try {
      // timeline 接口仅支持数字 id，externalEventId 场景下容错为空
      timeline.value = await getEventTimeline(idParam) || []
    } catch (e) {
      timeline.value = []
    }
    try {
      const users = await getSystemUsers()
      workers.value = (Array.isArray(users) ? users : []).filter((user: any) => user.status === 'ACTIVE')
      // 默认选中推荐角色的第一个人员
      if (!dispatchForm.value.assigneeUserId && filteredWorkers.value.length) {
        dispatchForm.value.assigneeUserId = Number(filteredWorkers.value[0].id)
      }
    } catch (e) {
      console.error('加载人员失败:', e)
      workers.value = []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleBack() {
  if (props.embedded) {
    emit('close')
  } else {
    router.back()
  }
}

async function handleClose() {
  if (!closeReason.value.trim()) { showMessage('请输入关闭原因'); return }
  try {
    await closeEvent(Number(eventId.value), closeReason.value)
    showClose.value = false
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '操作失败') }
}

async function handleReopen() {
  try {
    await reopenEvent(Number(eventId.value))
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '操作失败') }
}

async function handleArchive() {
  if (!await confirmDialog({ message: '确认归档该事件？归档后将从活跃视图隐藏。', okText: '归档' })) return
  try {
    await archiveEvent(Number(eventId.value))
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '归档失败') }
}

async function handleDispatch() {
  if (!dispatchForm.value.assigneeUserId) { showMessage('请选择受派人员'); return }
  try {
    await dispatchEvent(Number(eventId.value), {
      assigneeUserId: dispatchForm.value.assigneeUserId,
      remark: dispatchForm.value.remark
    })
    showDispatch.value = false
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '派单失败') }
}

// 打开派单弹窗时加载智能推荐
watch(showDispatch, async (visible) => {
  if (!visible) return
  dispatchSuggestion.value = null
  try {
    dispatchSuggestion.value = await getDispatchSuggestion(Number(eventId.value))
    // 推荐人有且当前未选中时，默认选中推荐人
    if (dispatchSuggestion.value?.recommendedUserId && !dispatchForm.value.assigneeUserId) {
      dispatchForm.value.assigneeUserId = Number(dispatchSuggestion.value.recommendedUserId)
    }
  } catch (e) {
    dispatchSuggestion.value = null
  }
})

// 一键智能派单：按规则自动分配推荐人
async function handleSmartDispatch() {
  if (!await confirmDialog({ message: '确认按智能推荐自动派单？', okText: '确认派单' })) return
  try {
    await smartDispatchEvent(Number(eventId.value), dispatchForm.value.remark)
    showDispatch.value = false
    showMessage('智能派单成功')
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '智能派单失败') }
}

// 打开派单弹窗：根据状态选择组长派单或普通派单
function openDispatchDialog() {
  if (event.value?.currentStatus === 'WAITING_LEADER_REVIEW') {
    showLeaderDispatch.value = true
  } else {
    showDispatch.value = true
  }
}

// 组长派单：加载派单信息
watch(showLeaderDispatch, async (visible) => {
  if (!visible) return
  leaderDispatchData.value = null
  leaderDispatchForm.value = { assigneeUserId: null, remark: '' }
  leaderDispatchLoading.value = true
  try {
    leaderDispatchData.value = await getLeaderDispatchInfo(Number(eventId.value))
    const subs = leaderDispatchData.value?.subordinates || []
    if (subs.length) {
      leaderDispatchForm.value.assigneeUserId = Number(subs[0].userId)
    }
  } catch (e: any) {
    showMessage(e?.message || '加载派单信息失败')
    showLeaderDispatch.value = false
  } finally {
    leaderDispatchLoading.value = false
  }
})

// 执行组长派单
async function handleLeaderDispatch() {
  if (!leaderDispatchForm.value.assigneeUserId) { showMessage('请选择下属网格员'); return }
  try {
    await leaderDispatch(Number(eventId.value), {
      assigneeUserId: leaderDispatchForm.value.assigneeUserId,
      remark: leaderDispatchForm.value.remark
    })
    showLeaderDispatch.value = false
    showMessage('组长派单成功')
    emit('changed')
    loadData()
  } catch (e: any) { showMessage(e?.message || '组长派单失败') }
}

onMounted(loadData)
// 事件 id 变化时重新加载（页面模式：路由参数变化；弹窗模式：父组件切换了打开的事件）
watch(eventId, loadData)
</script>