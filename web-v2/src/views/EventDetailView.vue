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
            <span style="font-size:12px;color:#9ca3af;">{{ event.eventCode }}</span>
          </div>
        </div>
        <div style="display:flex;gap:8px;">
          <button v-if="['WAITING_DISPATCH', 'IN_AUDIT'].includes(event.currentStatus)" @click="showDispatch = true" style="padding:8px 16px;border:none;border-radius:6px;background:#1890ff;color:#fff;font-size:13px;cursor:pointer;">派发工单</button>
          <button v-if="event.currentStatus !== 'CLOSED' && event.currentStatus !== 'IGNORED'" @click="showClose = true" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">关闭事件</button>
          <button v-if="event.currentStatus === 'CLOSED'" @click="handleReopen" style="padding:8px 16px;border:1px solid #52c41a;border-radius:6px;background:#fff;color:#52c41a;font-size:13px;cursor:pointer;">重新打开</button>
          <button @click="$router.back()" style="padding:8px 16px;border:1px solid #d1d5db;border-radius:6px;background:#fff;font-size:13px;cursor:pointer;">返回</button>
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
            <div><span style="color:#9ca3af;">上报人：</span>{{ event.reportSource || '-' }}</div>
            <div><span style="color:#9ca3af;">来源系统：</span>{{ event.sourceSystem || '-' }}</div>
            <div v-if="event.description" style="margin-top:8px;padding-top:8px;border-top:1px solid #f3f4f6;"><span style="color:#9ca3af;">详细描述：</span><br/>{{ event.description }}</div>
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

      <!-- 派单弹窗 -->
      <div v-if="showDispatch" class="modal-overlay" @click.self="showDispatch = false">
        <div class="modal-box">
          <h3 style="font-size:16px;font-weight:600;margin-bottom:16px;">派发工单</h3>
          <div class="form-group">
            <label class="form-label">选择受派人 <span class="required">*</span></label>
            <select v-model="dispatchForm.assigneeUserId" class="form-select">
              <option :value="null">请选择网格员</option>
              <option v-for="u in workers" :key="u.id" :value="u.id">{{ u.name || u.realName || u.account }}</option>
            </select>
            <p v-if="!workers.length" style="font-size:12px;color:#dc2626;margin-top:6px;">
              ⚠️ 暂无网格员，请先在"网格治理 → 组织人员"中添加
            </p>
          </div>
          <div class="form-group">
            <label class="form-label">处理时限</label>
            <input v-model="dispatchForm.deadline" type="datetime-local" class="form-input" />
          </div>
          <div class="form-group">
            <label class="form-label">备注</label>
            <textarea v-model="dispatchForm.remark" rows="2" placeholder="派单备注..." class="form-textarea"></textarea>
          </div>
          <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
            <button @click="showDispatch = false" class="btn btn-default">取消</button>
            <button @click="handleDispatch" class="btn btn-primary">确认派单</button>
          </div>
        </div>
      </div>

      <!-- 关闭弹窗 -->
      <div v-if="showClose" style="position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.4);display:flex;align-items:center;justify-content:center;z-index:1000;">
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
    </div>

    <div v-else style="text-align:center;padding:60px;color:#9ca3af;">事件不存在</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEventDetail, getEventTimeline, closeEvent, reopenEvent, dispatchEvent, getOrgMembers } from '../api'
import { getEventTypeName } from '../utils/eventTypes'

const route = useRoute()
const router = useRouter()

const event = ref<any>(null)
const timeline = ref<any[]>([])
const loading = ref(true)
const showDispatch = ref(false)
const showClose = ref(false)
const closeReason = ref('')
const workers = ref<any[]>([])
const dispatchForm = ref({ assigneeUserId: null as number | null, deadline: '', remark: '' })

function statusLabel(status: string) {
  const map: Record<string, string> = {
    PENDING_AUDIT: '待审核',
    IN_AUDIT: '审核中',
    WAITING_DISPATCH: '待派单',
    DISPATCHED_TO_WORK_ORDER: '已派单',
    CLOSED: '已关闭',
    IGNORED: '已忽略'
  }
  return map[status] || status
}

async function loadData() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    event.value = await getEventDetail(id)
    timeline.value = await getEventTimeline(id) || []
    // 加载网格员列表
    try {
      const res = await getOrgMembers()
      workers.value = Array.isArray(res) ? res : []
    } catch (e) {
      console.error('加载网格员失败:', e)
      workers.value = []
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  if (!closeReason.value.trim()) { alert('请输入关闭原因'); return }
  try {
    await closeEvent(Number(route.params.id), closeReason.value)
    showClose.value = false
    loadData()
  } catch (e: any) { alert(e?.message || '操作失败') }
}

async function handleReopen() {
  try {
    await reopenEvent(Number(route.params.id))
    loadData()
  } catch (e: any) { alert(e?.message || '操作失败') }
}

async function handleDispatch() {
  if (!dispatchForm.value.assigneeUserId) { alert('请选择受派人'); return }
  try {
    await dispatchEvent(Number(route.params.id), dispatchForm.value)
    showDispatch.value = false
    loadData()
  } catch (e: any) { alert(e?.message || '派单失败') }
}

onMounted(loadData)
</script>
