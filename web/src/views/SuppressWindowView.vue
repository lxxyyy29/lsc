<template>
  <div class="suppress-page">
    <div class="page-head">
      <h2>时段抑制（夜间静默）</h2>
      <p class="sub">
        落在下列时段内上传的事件按所选动作被抑制（<b>只打标记、不删数据</b>），
        时段结束后（抑制截止时间到点）<b>自动恢复可见</b>，无需人工操作。
      </p>
    </div>

    <div class="toolbar">
      <label class="global-switch">
        <input type="checkbox" v-model="globalEnabled" @change="saveGlobalSwitch" />
        <span>总开关（关闭后所有规则立即失效，应急用）</span>
      </label>
      <div class="spacer"></div>
      <button class="btn btn-primary" @click="openCreate">+ 新增规则</button>
      <button class="btn btn-default" @click="fetchList">刷新</button>
    </div>

    <div v-if="loading" class="empty">加载中...</div>
    <div v-else-if="!list.length" class="empty">暂无规则，点击「新增规则」创建，例如 22:00–07:00 夜间静默</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>名称</th><th>时段</th><th>生效星期</th><th>动作</th>
          <th>限定来源</th><th>限定类型</th><th>豁免紧急等级</th><th>状态</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in list" :key="r.id">
          <td><b>{{ r.name }}</b><div class="muted">{{ r.remark || '' }}</div></td>
          <td>
            {{ r.startTime }} - {{ r.endTime }}
            <span v-if="isCrossDay(r)" class="tag tag-orange">跨天</span>
          </td>
          <td>{{ weekdayLabel(r.weekdays) }}</td>
          <td>
            <span v-for="a in (r.action || '').split(',')" :key="a" class="tag tag-blue">{{ actionLabel(a) }}</span>
          </td>
          <td class="muted">{{ r.scopeSource || '全部' }}</td>
          <td class="muted">{{ r.scopeType || '全部' }}</td>
          <td class="muted">{{ r.exemptUrgency || '无' }}</td>
          <td>
            <label class="switch">
              <input type="checkbox" :checked="r.enabled === 1" @change="toggleEnabled(r, $event)" />
              <span>{{ r.enabled === 1 ? '启用' : '停用' }}</span>
            </label>
          </td>
          <td>
            <div class="ops">
              <button class="btn btn-default sm" @click="openEdit(r)">编辑</button>
              <button class="btn btn-default sm" @click="openTest(r)">命中预演</button>
              <button class="btn btn-danger sm" @click="removeRule(r)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 新增/编辑 -->
    <div v-if="showForm" class="modal-overlay">
      <div class="modal-box">
        <h3>{{ form.id ? '编辑规则' : '新增规则' }}</h3>

        <div class="row">
          <label>规则名称 <span class="req">*</span></label>
          <input v-model="form.name" placeholder="如：夜间静默" />
        </div>

        <div class="row two">
          <div>
            <label>开始时间 <span class="req">*</span></label>
            <input type="time" v-model="form.startTime" />
          </div>
          <div>
            <label>结束时间 <span class="req">*</span></label>
            <input type="time" v-model="form.endTime" />
            <div class="hint">结束早于开始表示<b>跨天</b>（如 22:00 → 07:00）</div>
          </div>
        </div>

        <div class="row">
          <label>生效星期</label>
          <div class="checks">
            <label v-for="d in weekOptions" :key="d.value">
              <input type="checkbox" :value="d.value" v-model="form.weekdaysArr" /> {{ d.label }}
            </label>
          </div>
        </div>

        <div class="row">
          <label>抑制动作（可多选）</label>
          <div class="checks">
            <label><input type="checkbox" value="HIDE" v-model="form.actions" /> 不显示（列表/大屏/地图不可见）</label>
            <label><input type="checkbox" value="NO_DISPATCH" v-model="form.actions" /> 不自动派单</label>
            <label><input type="checkbox" value="NO_NOTIFY" v-model="form.actions" /> 不推送通知</label>
          </div>
          <div class="hint">建议默认只勾「不显示」，避免误伤处置时效</div>
        </div>

        <div class="row two">
          <div>
            <label>限定来源系统（逗号分隔，空=全部）</label>
            <input v-model="form.scopeSource" placeholder="如：THIRD_PARTY_DRONE,H5" />
          </div>
          <div>
            <label>限定事件类型（逗号分隔，空=全部）</label>
            <input v-model="form.scopeType" placeholder="如：市容环境,消防安全" />
          </div>
        </div>

        <div class="row">
          <label>豁免紧急等级（逗号分隔）</label>
          <input v-model="form.exemptUrgency" placeholder="RED" />
          <div class="hint">列表内的事件即使落在时段内也不被抑制，默认 RED（紧急）</div>
        </div>

        <div class="row">
          <label>备注</label>
          <input v-model="form.remark" placeholder="用途说明" />
        </div>

        <div class="modal-foot">
          <button class="btn btn-default" @click="showForm = false">取消</button>
          <button class="btn btn-primary" :disabled="saving" @click="submitForm">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 命中预演 -->
    <div v-if="showTest" class="modal-overlay">
      <div class="modal-box">
        <h3>命中预演 — {{ testRule?.name }}</h3>
        <div class="row two">
          <div>
            <label>事件时间</label>
            <input type="datetime-local" v-model="testForm.at" />
          </div>
          <div>
            <label>来源系统</label>
            <input v-model="testForm.sourceSystem" placeholder="如：THIRD_PARTY_DRONE" />
          </div>
        </div>
        <div class="row two">
          <div>
            <label>事件类型</label>
            <input v-model="testForm.eventType" placeholder="如：市容环境" />
          </div>
          <div>
            <label>紧急等级</label>
            <input v-model="testForm.urgency" placeholder="GREEN / YELLOW / RED" />
          </div>
        </div>
        <div v-if="testResult" class="result" :class="testResult.suppressed ? 'hit' : 'miss'">
          <div v-if="testResult.suppressed">
            <b>会被抑制</b>：命中规则「{{ testResult.ruleName }}」，抑制至 <b>{{ testResult.until }}</b>，
            动作：{{ (testResult.actions || []).map(actionLabel).join('、') }}
          </div>
          <div v-else><b>不会被抑制</b>（不在时段内 / 不在生效星期 / 命中豁免 / 规则未启用）</div>
        </div>
        <div class="modal-foot">
          <button class="btn btn-default" @click="showTest = false">关闭</button>
          <button class="btn btn-primary" @click="runTest">试算</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import http from '../api'

const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const globalEnabled = ref(true)

const showForm = ref(false)
const showTest = ref(false)
const testRule = ref<any>(null)
const testResult = ref<any>(null)
const testForm = reactive({ at: '', sourceSystem: '', eventType: '', urgency: '' })

const weekOptions = [
  { value: '1', label: '周一' }, { value: '2', label: '周二' }, { value: '3', label: '周三' },
  { value: '4', label: '周四' }, { value: '5', label: '周五' }, { value: '6', label: '周六' }, { value: '7', label: '周日' },
]

const emptyForm = () => ({
  id: null as number | null,
  name: '夜间静默',
  startTime: '22:00',
  endTime: '07:00',
  weekdaysArr: ['1', '2', '3', '4', '5', '6', '7'],
  actions: ['HIDE'] as string[],
  scopeSource: '',
  scopeType: '',
  exemptUrgency: 'RED',
  remark: '',
})
const form = ref(emptyForm())

function actionLabel(a: string) {
  return ({ HIDE: '不显示', NO_DISPATCH: '不派单', NO_NOTIFY: '不通知' } as Record<string, string>)[a] || a
}
function isCrossDay(r: any) {
  return r.startTime && r.endTime && r.startTime > r.endTime
}
function weekdayLabel(w: string) {
  if (!w) return '每天'
  const arr = String(w).split(',')
  if (arr.length === 7) return '每天'
  return arr.map(x => (weekOptions.find(d => d.value === x.trim())?.label || x)).join('、')
}

async function fetchList() {
  loading.value = true
  try {
    list.value = await http.get('/system/suppress-windows') || []
  } catch (e: any) {
    alert('加载失败：' + (e?.message || e))
  } finally {
    loading.value = false
  }
}

async function fetchGlobalSwitch() {
  try {
    const rows: any = await http.get('/system/configs')
    const item = (rows || []).find((r: any) => r.configKey === 'suppress.enabled')
    if (item) globalEnabled.value = String(item.configValue) !== '0'
  } catch (e) { /* 配置接口不可用时忽略，不影响规则管理 */ }
}

async function saveGlobalSwitch() {
  try {
    await http.put('/system/configs/suppress.enabled', { configValue: globalEnabled.value ? '1' : '0' })
    alert(globalEnabled.value ? '已启用时段抑制' : '已全局关闭时段抑制（所有规则立即失效）')
  } catch (e: any) {
    alert('保存失败：' + (e?.message || e))
  }
}

function openCreate() {
  form.value = emptyForm()
  showForm.value = true
}

function openEdit(r: any) {
  form.value = {
    id: r.id,
    name: r.name || '',
    startTime: r.startTime || '22:00',
    endTime: r.endTime || '07:00',
    weekdaysArr: String(r.weekdays || '1,2,3,4,5,6,7').split(',').map((s: string) => s.trim()),
    actions: String(r.action || 'HIDE').split(',').map((s: string) => s.trim()).filter(Boolean),
    scopeSource: r.scopeSource || '',
    scopeType: r.scopeType || '',
    exemptUrgency: r.exemptUrgency || '',
    remark: r.remark || '',
  }
  showForm.value = true
}

async function submitForm() {
  if (!form.value.name.trim()) { alert('请填写规则名称'); return }
  if (!form.value.startTime || !form.value.endTime) { alert('请填写开始与结束时间'); return }
  if (!form.value.actions.length) { alert('请至少选择一个抑制动作'); return }
  if (!form.value.weekdaysArr.length) { alert('请至少选择一个生效星期'); return }
  saving.value = true
  const payload = {
    name: form.value.name.trim(),
    startTime: form.value.startTime,
    endTime: form.value.endTime,
    weekdays: form.value.weekdaysArr.join(','),
    action: form.value.actions.join(','),
    scopeSource: form.value.scopeSource.trim(),
    scopeType: form.value.scopeType.trim(),
    exemptUrgency: form.value.exemptUrgency.trim(),
    remark: form.value.remark.trim(),
    enabled: 1,
  }
  try {
    if (form.value.id) await http.put(`/system/suppress-windows/${form.value.id}`, payload)
    else await http.post('/system/suppress-windows', payload)
    showForm.value = false
    await fetchList()
  } catch (e: any) {
    alert('保存失败：' + (e?.message || e))
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(r: any, ev: Event) {
  const checked = (ev.target as HTMLInputElement).checked
  try {
    await http.put(`/system/suppress-windows/${r.id}/enabled?enabled=${checked}`)
    r.enabled = checked ? 1 : 0
  } catch (e: any) {
    alert('操作失败：' + (e?.message || e))
    await fetchList()
  }
}

async function removeRule(r: any) {
  if (!confirm(`确定删除规则「${r.name}」吗？删除后该时段不再抑制。`)) return
  try {
    await http.delete(`/system/suppress-windows/${r.id}`)
    await fetchList()
  } catch (e: any) {
    alert('删除失败：' + (e?.message || e))
  }
}

function openTest(r: any) {
  testRule.value = r
  testResult.value = null
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  testForm.at = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
  testForm.sourceSystem = r.scopeSource || 'THIRD_PARTY_DRONE'
  testForm.eventType = r.scopeType || '市容环境'
  testForm.urgency = 'GREEN'
  showTest.value = true
  runTest()
}

async function runTest() {
  try {
    const q = new URLSearchParams()
    if (testForm.sourceSystem) q.set('sourceSystem', testForm.sourceSystem)
    if (testForm.eventType) q.set('eventType', testForm.eventType)
    if (testForm.urgency) q.set('urgency', testForm.urgency)
    if (testForm.at) q.set('at', testForm.at)
    testResult.value = await http.get(`/system/suppress-windows/test?${q.toString()}`)
  } catch (e: any) {
    alert('试算失败：' + (e?.message || e))
  }
}

onMounted(() => {
  fetchList()
  fetchGlobalSwitch()
})
</script>

<style scoped>
.suppress-page { padding: 16px 20px; }
.page-head h2 { margin: 0 0 6px; font-size: 18px; }
.page-head .sub { margin: 0 0 14px; color: #6b7280; font-size: 12px; line-height: 1.6; }
.toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.toolbar .spacer { flex: 1; }
.global-switch { font-size: 13px; color: #374151; display: flex; align-items: center; gap: 6px; }
.empty { padding: 30px; text-align: center; color: #9ca3af; background: #f9fafb; border-radius: 8px; }
.table { width: 100%; border-collapse: collapse; background: #fff; font-size: 13px; }
.table th, .table td { padding: 8px 10px; border-bottom: 1px solid #f1f5f9; text-align: left; vertical-align: middle; }
.table th { background: #f3f4f6; font-weight: 600; color: #374151; }
.muted { color: #9ca3af; font-size: 12px; }
.tag { display: inline-block; padding: 1px 6px; border-radius: 4px; font-size: 11px; margin-right: 4px; }
.tag-blue { background: #eff6ff; color: #1d4ed8; }
.tag-orange { background: #fff7ed; color: #c2410c; }
.ops { display: flex; gap: 6px; }
.btn { padding: 5px 12px; border-radius: 6px; border: 1px solid #d1d5db; background: #fff; cursor: pointer; font-size: 13px; }
.btn.sm { padding: 3px 9px; font-size: 12px; }
.btn-primary { background: #2563eb; border-color: #2563eb; color: #fff; }
.btn-danger { background: #fff; border-color: #fca5a5; color: #dc2626; }
.btn-default { background: #fff; }
.switch { display: flex; align-items: center; gap: 4px; font-size: 12px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, .35); display: flex; align-items: center; justify-content: center; z-index: 50; }
.modal-box { width: 620px; max-height: 88vh; overflow-y: auto; background: #fff; border-radius: 10px; padding: 18px 20px; }
.modal-box h3 { margin: 0 0 14px; font-size: 16px; }
.row { margin-bottom: 12px; }
.row.two { display: flex; gap: 12px; }
.row.two > div { flex: 1; }
.row label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 5px; color: #374151; }
.row input { width: 100%; padding: 7px 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 13px; box-sizing: border-box; }
.checks { display: flex; flex-wrap: wrap; gap: 12px; font-size: 13px; }
.checks label { font-weight: 400; display: flex; align-items: center; gap: 4px; margin: 0; }
.hint { color: #9ca3af; font-size: 11px; margin-top: 4px; }
.req { color: #ef4444; }
.modal-foot { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
.result { margin-top: 12px; padding: 10px 12px; border-radius: 8px; font-size: 13px; line-height: 1.7; }
.result.hit { background: #fff7ed; border: 1px solid #fdba74; color: #9a3412; }
.result.miss { background: #f0fdf4; border: 1px solid #86efac; color: #166534; }
</style>
