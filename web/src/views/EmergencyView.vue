<template>
  <div class="emergency-page">
    <!-- 顶部统计卡 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">指令总数</div>
      </div>
      <div class="stat-card dispatched">
        <div class="stat-num">{{ stats.dispatched }}</div>
        <div class="stat-label">已下达</div>
      </div>
      <div class="stat-card responding">
        <div class="stat-num">{{ stats.responding }}</div>
        <div class="stat-label">响应中</div>
      </div>
      <div class="stat-card completed">
        <div class="stat-num">{{ stats.completed }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="filters">
        <select v-model="filterStatus" @change="loadList(1)" class="filter-select">
          <option value="">全部状态</option>
          <option value="DISPATCHED">已下达</option>
          <option value="RESPONDING">响应中</option>
          <option value="COMPLETED">已完成</option>
        </select>
        <select v-model="filterLevel" @change="loadList(1)" class="filter-select">
          <option value="">全部级别</option>
          <option value="COMMUNITY">社区级</option>
          <option value="GRID">大网格级</option>
          <option value="SUB_GRID">小网格级</option>
        </select>
        <button class="btn-refresh" @click="loadList(1)">刷新</button>
      </div>
      <button class="btn-launch" @click="openCreate">
        <span class="btn-launch-icon">⚡</span> 一键发起应急调度
      </button>
    </div>

    <!-- 指令列表 -->
    <div class="list-card">
      <table class="data-table">
        <thead>
          <tr>
            <th>指令编号</th>
            <th>标题</th>
            <th>类型</th>
            <th>级别</th>
            <th>目标</th>
            <th>状态</th>
            <th>接收/响应</th>
            <th>发起人</th>
            <th>下达时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="empty-row"><td colspan="10">加载中...</td></tr>
          <tr v-else-if="items.length === 0" class="empty-row">
            <td colspan="10">暂无应急调度指令，点击右上角「一键发起应急调度」创建</td>
          </tr>
          <tr v-for="row in items" v-else :key="row.id">
            <td class="mono">{{ row.dispatch_no }}</td>
            <td class="title-cell">{{ row.title }}</td>
            <td><span class="type-tag" :class="'type-' + row.type">{{ row.type_name }}</span></td>
            <td><span class="level-tag" :class="'level-' + row.level">{{ row.level_name }}</span></td>
            <td>{{ row.grid_name || '全域' }}</td>
            <td><span class="status-tag" :class="'status-' + row.status">{{ statusName(row.status) }}</span></td>
            <td class="mono">{{ row.responded_count }}/{{ row.receiver_count }}</td>
            <td>{{ row.creator_name }}</td>
            <td class="mono">{{ fmtTime(row.dispatch_time) }}</td>
            <td>
              <button class="btn-detail" @click="openDetail(row)">详情</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="pager" v-if="total > pageSize">
        <button :disabled="page <= 1" @click="loadList(page - 1)">上一页</button>
        <span>第 {{ page }} / {{ Math.ceil(total / pageSize) }} 页（共 {{ total }} 条）</span>
        <button :disabled="page >= Math.ceil(total / pageSize)" @click="loadList(page + 1)">下一页</button>
      </div>
    </div>

    <!-- 发起应急调度弹窗 -->
    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal create-modal">
        <div class="modal-header">
          <span>⚡ 一键发起多方联合调度</span>
          <button class="modal-close" @click="showCreate = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <label>事件类型 <em>*</em></label>
            <div class="type-options">
              <label v-for="(name, code) in metaTypes" :key="code" class="type-option"
                     :class="{ selected: form.type === code }">
                <input type="radio" :value="code" v-model="form.type" />
                <span>{{ name }}</span>
              </label>
            </div>
          </div>
          <div class="form-row">
            <label>调度级别 <em>*</em></label>
            <div class="type-options">
              <label v-for="(name, code) in metaLevels" :key="code" class="type-option"
                     :class="{ selected: form.level === code }">
                <input type="radio" :value="code" v-model="form.level" />
                <span>{{ name }}</span>
              </label>
            </div>
            <div class="level-hint">{{ levelHint }}</div>
          </div>
          <div class="form-row" v-if="form.level !== 'COMMUNITY'">
            <label>目标网格 <em>*</em></label>
            <select v-model="form.gridId" class="form-input">
              <option :value="null" disabled>请选择目标网格</option>
              <optgroup v-for="g in gridGroups" :key="g.id" :label="g.gridName">
                <option v-for="child in g.children || []" :key="child.id" :value="child.id">
                  {{ child.gridName }}
                </option>
              </optgroup>
            </select>
          </div>
          <div class="form-row">
            <label>指令标题 <em>*</em></label>
            <input v-model="form.title" class="form-input" maxlength="100"
                   placeholder="如：台风“格美”防御应急调度" />
          </div>
          <div class="form-row">
            <label>关联事件编号</label>
            <input v-model="form.eventCode" class="form-input" placeholder="选填：关联的突发事件编号" />
          </div>
          <div class="form-row">
            <label>指令内容 <em>*</em></label>
            <textarea v-model="form.content" class="form-textarea" rows="4"
                      placeholder="描述紧急状况与处置要求，如：请各网格员立即排查低洼易涝点，组织人员转移…"></textarea>
          </div>
          <div class="form-row">
            <label>附带视频点位</label>
            <div class="camera-options">
              <label v-for="cam in cameras" :key="cam.id" class="camera-option"
                     :class="{ selected: form.videoCameraIds.includes(String(cam.id)) }">
                <input type="checkbox" :value="String(cam.id)" v-model="form.videoCameraIds" />
                <span>{{ cam.camera_name }}<i v-if="cam.status !== 'ACTIVE'" class="cam-offline">（离线）</i></span>
              </label>
              <span v-if="cameras.length === 0" class="hint-text">暂无视频点位</span>
            </div>
          </div>
          <div class="form-row">
            <label>视频会议</label>
            <div class="meeting-row">
              <input v-model="form.meetingUrl" class="form-input" placeholder="选填：视频会议链接（外接会议系统）" />
              <span class="hint-text">外接会议系统后，发起时自动拉起视频会议</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showCreate = false">取消</button>
          <button class="btn-confirm" :disabled="creating" @click="submitCreate">
            {{ creating ? '发起中…' : '一键发起调度' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 指令详情抽屉 -->
    <div v-if="showDetail" class="drawer-mask" @click.self="showDetail = false">
      <div class="drawer">
        <div class="drawer-header">
          <span>指令详情</span>
          <button class="modal-close" @click="showDetail = false">×</button>
        </div>
        <div class="drawer-body" v-if="detail">
          <div class="detail-block">
            <div class="detail-title">
              {{ detail.title }}
              <span class="status-tag" :class="'status-' + detail.status">{{ statusName(detail.status) }}</span>
            </div>
            <div class="detail-meta">
              <span class="mono">{{ detail.dispatch_no }}</span>
              <span class="type-tag" :class="'type-' + detail.type">{{ detail.type_name }}</span>
              <span class="level-tag" :class="'level-' + detail.level">{{ detail.level_name }}</span>
              <span>目标：{{ detail.grid_name || '全域' }}</span>
              <span>发起人：{{ detail.creator_name }}</span>
              <span>下达：{{ fmtTime(detail.dispatch_time) }}</span>
              <span v-if="detail.event_code">关联事件：{{ detail.event_code }}</span>
              <span v-if="detail.completed_at">完成：{{ fmtTime(detail.completed_at) }}</span>
            </div>
            <div class="detail-content">{{ detail.content }}</div>
            <div v-if="detail.meeting_url" class="meeting-link">
              🎥 视频会议：<a :href="detail.meeting_url" target="_blank" rel="noopener">{{ detail.meeting_url }}</a>
            </div>
            <div v-if="cameraNames.length" class="meeting-link">
              📹 附带视频点位：{{ cameraNames.join('、') }}
              <button class="btn-goto-video" @click="$router.push('/video')">前往视频轮巡</button>
            </div>
          </div>

          <div class="receipt-block">
            <div class="receipt-title">接收回执（{{ detail.receipts?.length || 0 }} 人）</div>
            <div v-if="!detail.receipts || detail.receipts.length === 0" class="hint-text">暂无接收人</div>
            <div v-for="r in detail.receipts" :key="r.id" class="receipt-row">
              <div class="receipt-user">
                <span class="receipt-avatar">{{ (r.user_name || '?').slice(-1) }}</span>
                <span class="receipt-name">{{ r.user_name || `用户${r.user_id}` }}</span>
              </div>
              <span class="receipt-status" :class="'rstatus-' + r.status">{{ receiptName(r.status) }}</span>
              <span class="receipt-time">{{ r.received_at ? fmtTime(r.received_at) : '—' }}</span>
              <span class="receipt-feedback">{{ r.feedback || '' }}</span>
            </div>
          </div>
        </div>
        <div class="drawer-footer" v-if="detail && detail.status !== 'COMPLETED'">
          <button class="btn-complete" @click="doComplete">✔ 完成指令（指挥端）</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getEmergencyMeta,
  getEmergencyDispatches,
  getEmergencyDispatchDetail,
  createEmergencyDispatch,
  completeEmergencyDispatch,
  getVideoCameras,
  getGridTree,
} from '../api/index'

const items = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const filterStatus = ref('')
const filterLevel = ref('')
const stats = reactive({ total: 0, dispatched: 0, responding: 0, completed: 0 })

const metaTypes = ref<Record<string, string>>({})
const metaLevels = ref<Record<string, string>>({})
const gridGroups = ref<any[]>([])
const cameras = ref<any[]>([])

const showCreate = ref(false)
const creating = ref(false)
const form = reactive({
  type: 'RAIN',
  level: 'COMMUNITY',
  gridId: null as number | null,
  title: '',
  eventCode: '',
  content: '',
  videoCameraIds: [] as string[],
  meetingUrl: '',
})

const showDetail = ref(false)
const detail = ref<any>(null)

const statusName = (s: string) =>
  ({ DISPATCHED: '已下达', RESPONDING: '响应中', COMPLETED: '已完成' } as Record<string, string>)[s] || s
const receiptName = (s: string) =>
  ({ PENDING: '未接收', RECEIVED: '已接收', RESPONDING: '响应中', COMPLETED: '已完成' } as Record<string, string>)[s] || s

const levelHint = computed(() => {
  const map: Record<string, string> = {
    COMMUNITY: '推送至全社区在册网格成员（两委干部/网格长/网格员）',
    GRID: '推送至所选大网格及其下辖小网格的网格成员',
    SUB_GRID: '推送至所选小网格的网格成员',
  }
  return map[form.level] || ''
})

const cameraNames = computed(() => {
  if (!detail.value?.video_camera_ids) return []
  const ids = String(detail.value.video_camera_ids).split(',').filter(Boolean)
  return ids.map((id: string) => {
    const cam = cameras.value.find((c) => String(c.id) === id)
    return cam ? cam.camera_name : id
  })
})

function fmtTime(t?: string | null) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}

async function loadList(p = 1) {
  page.value = p
  loading.value = true
  try {
    const res: any = await getEmergencyDispatches({
      status: filterStatus.value || undefined,
      level: filterLevel.value || undefined,
      page: p,
      size: pageSize,
    })
    items.value = res.items || []
    total.value = res.total || 0
    stats.total = total.value
    // 分状态统计（各取第一页 total）
    const byStatus: Record<string, number> = { DISPATCHED: 0, RESPONDING: 0, COMPLETED: 0 }
    for (const st of ['DISPATCHED', 'RESPONDING', 'COMPLETED']) {
      const r: any = await getEmergencyDispatches({ status: st, page: 1, size: 1 })
      byStatus[st] = r.total || 0
    }
    stats.dispatched = byStatus.DISPATCHED
    stats.responding = byStatus.RESPONDING
    stats.completed = byStatus.COMPLETED
  } catch (e) {
    console.error('加载指令列表失败', e)
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  try {
    const res: any = await getEmergencyMeta()
    metaTypes.value = res.types || {}
    metaLevels.value = res.levels || {}
  } catch (e) {
    console.error('加载字典失败', e)
  }
}

async function loadGrids() {
  try {
    const res: any = await getGridTree()
    // GridTreeVo 树：根节点为社区（level 1），其 children 为大网格（level 2），大网格 children 为小网格
    const groups: any[] = []
    for (const root of res || []) {
      for (const g of root.children || []) {
        groups.push({
          id: g.id,
          gridName: g.gridName,
          children: (g.children || []).map((c: any) => ({ id: c.id, gridName: c.gridName })),
        })
      }
    }
    gridGroups.value = groups
  } catch (e) {
    console.error('加载网格失败', e)
  }
}

async function loadCameras() {
  try {
    const res: any = await getVideoCameras({ page: 1, size: 100 })
    cameras.value = res.items || []
  } catch (e) {
    console.error('加载视频点位失败', e)
  }
}

function openCreate() {
  form.type = 'RAIN'
  form.level = 'COMMUNITY'
  form.gridId = null
  form.title = ''
  form.eventCode = ''
  form.content = ''
  form.videoCameraIds = []
  form.meetingUrl = ''
  showCreate.value = true
}

async function submitCreate() {
  if (!form.title.trim() || !form.content.trim()) {
    alert('请填写指令标题与指令内容')
    return
  }
  if (form.level !== 'COMMUNITY' && !form.gridId) {
    alert('请选择目标网格')
    return
  }
  creating.value = true
  try {
    const res: any = await createEmergencyDispatch({
      title: form.title.trim(),
      type: form.type,
      level: form.level,
      gridId: form.level === 'COMMUNITY' ? null : form.gridId,
      content: form.content.trim(),
      eventCode: form.eventCode.trim() || null,
      videoCameraIds: form.videoCameraIds.join(','),
      meetingUrl: form.meetingUrl.trim() || null,
    })
    showCreate.value = false
    alert(`指令已下达（编号 ${res.dispatchNo}），已推送 ${res.receiverCount} 名接收人`)
    loadList(1)
  } catch (e: any) {
    alert(e?.message || '发起失败')
  } finally {
    creating.value = false
  }
}

async function openDetail(row: any) {
  try {
    detail.value = await getEmergencyDispatchDetail(row.id)
    showDetail.value = true
  } catch (e) {
    console.error('加载详情失败', e)
  }
}

async function doComplete() {
  if (!detail.value) return
  if (!confirm('确认完成该应急调度指令？')) return
  try {
    await completeEmergencyDispatch(detail.value.id)
    showDetail.value = false
    loadList(1)
  } catch (e: any) {
    alert(e?.message || '操作失败')
  }
}

onMounted(() => {
  loadList()
  loadMeta()
  loadGrids()
  loadCameras()
})
</script>

<style scoped>
.emergency-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px 20px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border-left: 4px solid #64748b;
}

.stat-card.dispatched { border-left-color: #f59e0b; }
.stat-card.responding { border-left-color: #0284c7; }
.stat-card.completed { border-left-color: #16a34a; }

.stat-num { font-size: 26px; font-weight: 700; color: #0f172a; }

.stat-label { font-size: 12px; color: #64748b; margin-top: 2px; }

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.filters { display: flex; gap: 8px; align-items: center; }

.filter-select {
  padding: 7px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  background: #fff;
  outline: none;
}

.btn-refresh {
  padding: 7px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.btn-launch {
  padding: 9px 18px;
  background: linear-gradient(135deg, #dc2626, #ea580c);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.35);
  transition: all 0.2s;
}

.btn-launch:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(220, 38, 38, 0.45); }

.btn-launch-icon { margin-right: 2px; }

.list-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  text-align: left;
  padding: 11px 12px;
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
  white-space: nowrap;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
}

.data-table tr:hover td { background: #f8fafc; }

.empty-row td { text-align: center; color: #94a3b8; padding: 28px 12px; }

.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }

.title-cell { font-weight: 600; color: #0f172a; max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.type-tag, .level-tag, .status-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  white-space: nowrap;
}

.type-RAIN { background: #e0f2fe; color: #0369a1; }
.type-FIRE { background: #fee2e2; color: #b91c1c; }
.type-MASS { background: #fef3c7; color: #b45309; }
.type-OTHER { background: #f1f5f9; color: #475569; }

.level-COMMUNITY { background: #ede9fe; color: #6d28d9; }
.level-GRID { background: #e0e7ff; color: #4338ca; }
.level-SUB_GRID { background: #dbeafe; color: #1d4ed8; }

.status-DISPATCHED { background: #fef3c7; color: #b45309; }
.status-RESPONDING { background: #e0f2fe; color: #0369a1; }
.status-COMPLETED { background: #dcfce7; color: #15803d; }

.btn-detail {
  padding: 4px 12px;
  border: 1px solid #0284c7;
  border-radius: 6px;
  background: #fff;
  color: #0284c7;
  font-size: 12px;
  cursor: pointer;
}

.btn-detail:hover { background: #eff6ff; }

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 14px;
  padding: 12px;
  font-size: 13px;
  color: #475569;
}

.pager button {
  padding: 5px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
}

.pager button:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-mask, .drawer-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal {
  background: #fff;
  border-radius: 14px;
  width: 640px;
  max-width: 92vw;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
}

.modal-header, .drawer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid #e2e8f0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.modal-close {
  border: none;
  background: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
}

.modal-body {
  padding: 16px 18px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row label { display: block; font-size: 13px; color: #475569; margin-bottom: 6px; font-weight: 600; }
.form-row label em { color: #dc2626; font-style: normal; }

.type-options { display: flex; gap: 8px; flex-wrap: wrap; }

.type-option {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #334155;
}

.type-option.selected { border-color: #0284c7; background: #eff6ff; color: #0369a1; font-weight: 600; }
.type-option input { display: none; }

.level-hint { font-size: 12px; color: #94a3b8; margin-top: 6px; }

.form-input, .form-textarea {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  outline: none;
  box-sizing: border-box;
  font-family: inherit;
}

.form-input:focus, .form-textarea:focus { border-color: #0284c7; }

.camera-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 120px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.camera-option {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  color: #334155;
}

.camera-option.selected { border-color: #0284c7; background: #eff6ff; color: #0369a1; }
.camera-option input { display: none; }
.cam-offline { color: #dc2626; font-style: normal; }

.meeting-row { display: flex; align-items: center; gap: 10px; }

.hint-text { font-size: 12px; color: #94a3b8; white-space: nowrap; }

.modal-footer, .drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 13px 18px;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel {
  padding: 8px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.btn-confirm {
  padding: 8px 20px;
  background: #dc2626;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }

.drawer {
  position: fixed;
  right: 0;
  top: 0;
  bottom: 0;
  width: 560px;
  max-width: 92vw;
  background: #fff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.15);
}

.drawer-body { flex: 1; overflow-y: auto; padding: 16px 18px; display: flex; flex-direction: column; gap: 16px; }

.detail-block, .receipt-block {
  background: #f8fafc;
  border-radius: 10px;
  padding: 14px 16px;
}

.detail-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
  align-items: center;
}

.detail-content {
  font-size: 13px;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  background: #fff;
  border-radius: 8px;
  padding: 10px 12px;
}

.meeting-link { font-size: 13px; color: #475569; margin-top: 10px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.meeting-link a { color: #0284c7; word-break: break-all; }

.btn-goto-video {
  padding: 4px 12px;
  border: 1px solid #0284c7;
  border-radius: 6px;
  background: #fff;
  color: #0284c7;
  font-size: 12px;
  cursor: pointer;
}

.receipt-title { font-size: 14px; font-weight: 600; color: #0f172a; margin-bottom: 10px; }

.receipt-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 6px;
}

.receipt-user { display: flex; align-items: center; gap: 8px; min-width: 110px; }

.receipt-avatar {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #0284c7;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.receipt-name { font-size: 13px; color: #334155; font-weight: 500; }

.receipt-status { font-size: 12px; padding: 2px 8px; border-radius: 8px; white-space: nowrap; }

.rstatus-PENDING { background: #f1f5f9; color: #64748b; }
.rstatus-RECEIVED { background: #fef3c7; color: #b45309; }
.rstatus-RESPONDING { background: #e0f2fe; color: #0369a1; }
.rstatus-COMPLETED { background: #dcfce7; color: #15803d; }

.receipt-time { font-size: 12px; color: #94a3b8; min-width: 110px; }
.receipt-feedback { font-size: 12px; color: #475569; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.btn-complete {
  padding: 9px 20px;
  background: #16a34a;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-complete:hover { background: #15803d; }
</style>
