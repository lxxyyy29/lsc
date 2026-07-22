<template>
  <SystemDialog
    :open="open"
    :title="currentView === 'list' ? '航线管理' : (selectedWayline?.name || selectedWayline?.file_name || '算法绑定配置')"
    subtitle="航线算法绑定"
    panel-class="system-dialog__panel--wide"
    @close="handleClose"
  >
    <!-- ══ 页面 1：航线列表 ══ -->
    <template v-if="currentView === 'list'">
      <div v-if="waylineLoading" class="empty-state">加载中...</div>
      <template v-else>
        <div v-if="waylines.length === 0" class="empty-state">暂无航线数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>航线名称</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="wl in waylines" :key="String(wl.id)">
              <td>{{ wl.name || wl.file_name || '--' }}</td>
              <td>{{ formatDroneTimestamp(wl.updateTime ?? wl.update_time) }}</td>
              <td>
                <button type="button" class="action-link" @click="enterBindView(wl)">
                  绑定算法
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </template>

    <!-- ══ 页面 2：算法绑定配置 ══ -->
    <template v-else-if="currentView === 'bind'">
      <!-- 顶部面包屑 / 返回导航 -->
      <div class="bind-view__nav">
        <button type="button" class="bind-view__back" @click="exitBindView">
          ← 返回列表
        </button>
        <span class="bind-view__subtitle">
          为「{{ selectedWayline?.name || selectedWayline?.file_name }}」配置AI算法绑定
        </span>
      </div>

      <div v-if="existingLoading" class="empty-state">查询已绑定算法...</div>

      <template v-else>
        <!-- 已绑定算法区块 -->
        <div class="section-block">
          <div class="section-block__header">
            <span class="section-block__label">
              已绑定算法{{ existingBindings.length ? ` (${existingBindings.length})` : '' }}
            </span>
            <button type="button" class="action-button action-button--secondary bind-add-btn" @click="addNewBinding">
              + 添加绑定
            </button>
          </div>
          <table v-if="existingBindings.length" class="data-table existing-table">
            <thead>
              <tr>
                <th>算法名称</th>
                <th>起始航点</th>
                <th>结束航点</th>
                <th>频率(秒)</th>
                <th>违章区域</th>
                <th style="width: 50px"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(eb, idx) in existingBindings" :key="'e-' + idx">
                <td>
                  <span>{{ eb.displayName }}</span>
                  <span v-if="!eb.isLocal" class="unlinked-tag">未关联</span>
                </td>
                <td>{{ eb.startPointIndex ?? '--' }}</td>
                <td>{{ eb.endPointIndex ?? '--' }}</td>
                <td>{{ eb.intervalSecond ?? '--' }}</td>
                <td>
                  <span class="area-summary">{{ areaNamesOf(eb.violationAreaIds) || '—' }}</span>
                </td>
                <td>
                  <button
                    type="button"
                    class="bind-row__remove bind-row__remove--sm"
                    aria-label="移除此绑定"
                    @click="removeExisting(idx)"
                  >✕</button>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty-state empty-state--sm">该航线暂无已绑定的AI算法。</div>
        </div>

        <!-- 新增绑定区块 -->
        <div v-if="newBindingRows.length > 0" class="section-block">
          <div class="section-block__header">
            <span class="section-block__label">新增绑定</span>
          </div>
          <div
            v-for="(binding, idx) in newBindingRows"
            :key="'n-' + idx"
            class="bind-row"
          >
            <label class="field-stack">
              <span>AI算法</span>
              <select v-model="binding.label" aria-label="选择AI算法">
                <option value="" disabled hidden>请选择模型</option>
                <option v-for="m in qwenModels" :key="m.id" :value="m.label">
                  {{ m.name }}（{{ m.label }}）
                </option>
              </select>
            </label>
            <label class="field-stack">
              <span>起始航点索引</span>
              <input
                v-model.number="binding.startPointIndex"
                type="number"
                min="0"
                placeholder="如 0"
                aria-label="起始航点索引"
              />
            </label>
            <label class="field-stack">
              <span>结束航点索引</span>
              <input
                v-model.number="binding.endPointIndex"
                type="number"
                min="1"
                placeholder="如 10"
                aria-label="结束航点索引"
              />
            </label>
            <label class="field-stack">
              <span>推送频率（秒）</span>
              <input
                v-model.number="binding.intervalSecond"
                type="number"
                min="1"
                placeholder="如 10"
                aria-label="推送频率"
              />
            </label>
            <label class="field-stack">
              <span>违章区域</span>
              <ElSelect
                v-model="binding.violationAreaIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="选择违章区域（可多选）"
                aria-label="选择违章区域（可多选）"
                :teleported="true"
                :max-collapse-tags="3"
                popper-class="violation-area-dropdown"
              >
                <ElOption
                  v-for="area in violationAreas"
                  :key="area.id"
                  :label="area.areaName"
                  :value="area.id"
                />
              </ElSelect>
            </label>
            <button
              type="button"
              class="bind-row__remove"
              aria-label="删除此绑定"
              @click="removeNewBinding(idx)"
            >✕</button>
          </div>
        </div>

        <p class="bind-warning">⚠ 确认绑定将覆盖该航线所有已有算法绑定</p>
      </template>

      <!-- 底部操作栏 -->
      <div class="bind-view__footer">
        <div class="bind-view__actions">
          <button type="button" class="action-button action-button--secondary" @click="exitBindView">
            返回
          </button>
          <button
            type="button"
            class="action-button"
            :disabled="!canSubmit || bindLoading"
            @click="handleBind(String(selectedWayline?.id))"
          >
            {{ bindLoading ? '提交中...' : '确认绑定' }}
          </button>
        </div>
      </div>
    </template>
  </SystemDialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElSelect, ElOption } from 'element-plus'
import SystemDialog from '../system/SystemDialog.vue'
import { useToast } from '../../composables/useToast'
import {
  formatDroneTimestamp,
  listDroneWaylines,
  listQwenModelsEnabled,
  getWaylineAiBindingDetail,
  bindQwenToWayline,
  type DroneWayline,
  type QwenAlgorithmModel,
  type QwenBindingRequest
} from '../../api/drone'
import { listViolationAreas, type ViolationArea } from '../../api/violation-area'

const props = defineProps<{
  open: boolean
  dockSn: string
}>()

const emit = defineEmits<{
  close: []
}>()

const toast = useToast()

// ── 页面状态 ──
const currentView = ref<'list' | 'bind'>('list')
const selectedWayline = ref<DroneWayline | null>(null)

// ── 航线列表 ──
const waylines = ref<DroneWayline[]>([])
const waylineLoading = ref(false)

async function loadWaylines() {
  waylineLoading.value = true
  try {
    waylines.value = await listDroneWaylines()
  } catch (error) {
    const msg = error instanceof Error ? error.message : '航线加载失败'
    toast.error(msg)
    waylines.value = []
  } finally {
    waylineLoading.value = false
  }
}

// ── AI 模型列表 ──
const qwenModels = ref<QwenAlgorithmModel[]>([])
const qwenLoading = ref(false)

async function loadQwenModels() {
  if (qwenModels.value.length > 0) return
  qwenLoading.value = true
  try {
    qwenModels.value = await listQwenModelsEnabled()
  } catch {
    qwenModels.value = []
  } finally {
    qwenLoading.value = false
  }
}

// ── 绑定状态 ──
interface ExistingBinding {
  label: string
  displayName: string
  isLocal: boolean
  startPointIndex: number | null
  endPointIndex: number | null
  intervalSecond: number
  violationAreaIds: number[]
}

interface NewBindingRow {
  label: string
  startPointIndex: number | null
  endPointIndex: number | null
  intervalSecond: number
  violationAreaIds: number[]
}

const existingBindings = ref<ExistingBinding[]>([])
const existingLoading = ref(false)
const newBindingRows = ref<NewBindingRow[]>([])
const bindLoading = ref(false)

// ── 违章区域列表(全量缓存,供多选用) ──
const violationAreas = ref<ViolationArea[]>([])

async function loadViolationAreas() {
  if (violationAreas.value.length > 0) return
  try {
    violationAreas.value = await listViolationAreas({ status: 'ACTIVE' })
  } catch {
    violationAreas.value = []
  }
}

function areaNamesOf(ids: number[] | undefined): string {
  if (!ids || ids.length === 0) return ''
  const nameById = new Map(violationAreas.value.map((a) => [a.id, a.areaName]))
  return ids.map((id) => nameById.get(id) ?? `#${id}`).join('、')
}

function resetBindState() {
  existingBindings.value = []
  newBindingRows.value = []
}

// ── 进入/退出绑定页 ──
async function enterBindView(wl: DroneWayline) {
  resetBindState()
  selectedWayline.value = wl
  currentView.value = 'bind'
  await Promise.all([loadQwenModels(), loadViolationAreas()])
  await loadExistingBindings(String(wl.id))
}

function exitBindView() {
  resetBindState()
  selectedWayline.value = null
  currentView.value = 'list'
}

function handleClose() {
  exitBindView()
  emit('close')
}

// ── 绑定操作 ──
function addNewBinding() {
  newBindingRows.value.push({ label: '', startPointIndex: null, endPointIndex: null, intervalSecond: 10, violationAreaIds: [] })
}

function removeNewBinding(idx: number) {
  newBindingRows.value.splice(idx, 1)
}

function removeExisting(idx: number) {
  existingBindings.value.splice(idx, 1)
}

async function loadExistingBindings(flyLineId: string) {
  existingLoading.value = true
  try {
    const bindings = await getWaylineAiBindingDetail(flyLineId)
    const labelToName: Record<string, string> = {}
    for (const m of qwenModels.value) {
      labelToName[m.label] = m.name
    }
    existingBindings.value = bindings
      .filter((b) => {
        const modelNo = b.model_no ?? b.modelNo
        return modelNo === 'QWENMODEL'
      })
      .map((b) => {
        const label = String(b.label ?? '')
        const localName = labelToName[label]
        const upstreamName = String(b.model_name ?? b.modelName ?? '')
        const rawAreaIds = b.violationAreaIds
        const areaIds: number[] = Array.isArray(rawAreaIds)
          ? rawAreaIds
              .map((id) => (typeof id === 'number' ? id : Number(id)))
              .filter((n) => Number.isFinite(n))
          : []
        return {
          label,
          displayName: localName ?? upstreamName ?? label,
          isLocal: !!localName,
          startPointIndex: b.start_point_index ?? b.startPointIndex ?? null,
          endPointIndex: b.end_point_index ?? b.endPointIndex ?? null,
          intervalSecond: b.push_frequency ?? b.pushFrequency ?? 10,
          violationAreaIds: areaIds
        } as ExistingBinding
      })
  } catch {
    existingBindings.value = []
  } finally {
    existingLoading.value = false
  }
}

const canSubmit = computed(() => {
  // 允许提交空列表（清除该航线所有算法绑定）
  // 仅校验新增行：已填写的必须完整有效
  return newBindingRows.value.every(
    (r) =>
      r.label.trim() &&
      r.startPointIndex != null &&
      r.endPointIndex != null &&
      r.endPointIndex > r.startPointIndex
  )
})

async function handleBind(flyLineId: string) {
  bindLoading.value = true
  try {
    const payload: QwenBindingRequest[] = [
      ...existingBindings.value.map((e) => ({
        label: e.label,
        startPointIndex: e.startPointIndex!,
        endPointIndex: e.endPointIndex!,
        intervalSecond: e.intervalSecond ?? 10,
        violationAreaIds: e.violationAreaIds ?? []
      })),
      ...newBindingRows.value
        .filter((r) => r.label.trim() && r.startPointIndex != null && r.endPointIndex != null)
        .map((r) => ({
          label: r.label.trim(),
          startPointIndex: r.startPointIndex!,
          endPointIndex: r.endPointIndex!,
          intervalSecond: r.intervalSecond ?? 10,
          violationAreaIds: r.violationAreaIds ?? []
        }))
    ]
    await bindQwenToWayline(flyLineId, payload)
    toast.success('绑定成功！')
    await loadExistingBindings(flyLineId)
    newBindingRows.value = []
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '绑定失败，请重试')
  } finally {
    bindLoading.value = false
  }
}

// ── 生命周期 ──
watch(
  () => props.open,
  (isOpen) => {
    if (isOpen) {
      void loadWaylines()
      void loadQwenModels()
    } else {
      exitBindView()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
@import '../../views/admin-shared.css';

/* ── 状态文案 ── */
.empty-state {
  color: var(--fg-text-secondary, rgba(205, 222, 248, 0.78));
  padding: 24px 0;
  font-size: 13px;
}

.empty-state--sm {
  padding: 8px 0;
}

.area-summary {
  color: rgba(205, 222, 248, 0.85);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-all;
}

.field-hint {
  font-size: 11px;
  color: rgba(141, 197, 255, 0.55);
  margin-top: 2px;
}

.error-text {
  color: #ffb4b4;
  font-size: 13px;
  margin: 4px 0;
}

.success-text {
  color: #6ee6a7;
  font-size: 13px;
}

/* ── 绑定页顶部导航 ── */
.bind-view__nav {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(64, 158, 255, 0.12);
  margin-bottom: 20px;
}

.bind-view__back {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border: 1px solid rgba(64, 158, 255, 0.25);
  border-radius: 6px;
  background: transparent;
  color: #8dc5ff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.bind-view__back:hover {
  background: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.5);
  color: #b8d9ff;
}

.bind-view__subtitle {
  font-size: 13px;
  color: rgba(205, 222, 248, 0.6);
}

/* ── 内容区块 ── */
.section-block {
  margin-bottom: 20px;
}

.section-block__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.section-block__label {
  font-size: 12px;
  color: rgba(205, 222, 248, 0.6);
  font-weight: 500;
  letter-spacing: 0.03em;
  text-transform: uppercase;
}

.bind-add-btn {
  padding: 5px 12px !important;
  font-size: 12px !important;
}

/* ── 已绑定表格 ── */
.existing-table {
  font-size: 12px;
}

.existing-table th,
.existing-table td {
  padding: 8px 12px;
}

.unlinked-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  color: rgba(255, 180, 100, 0.9);
  background: rgba(255, 180, 100, 0.12);
  border: 1px solid rgba(255, 180, 100, 0.2);
  vertical-align: middle;
}

/* ── 每条新增绑定行 ── */
.bind-row {
  display: grid;
  grid-template-columns: 2fr 0.6fr 0.6fr 0.6fr 1.5fr auto;
  gap: 12px;
  align-items: end;
  padding: 14px 16px;
  border: 1px solid rgba(103, 187, 246, 0.14);
  border-radius: 12px;
  background: rgba(8, 30, 50, 0.5);
  margin-bottom: 10px;
}

.bind-row .field-stack {
  gap: 6px;
}

.bind-row .field-stack span {
  font-size: 12px;
}

.bind-row .field-stack input,
.bind-row .field-stack select {
  padding: 8px 12px;
  font-size: 13px;
}

.bind-row__remove {
  align-self: end;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 120, 117, 0.3);
  border-radius: 8px;
  background: transparent;
  color: #ff7875;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.bind-row__remove--sm {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  font-size: 11px;
}

.bind-row__remove:hover {
  background: rgba(255, 120, 117, 0.15);
  border-color: #ff7875;
}

/* ── 警告提示 ── */
.bind-warning {
  margin: 8px 0 4px;
  font-size: 12px;
  color: rgba(255, 200, 100, 0.7);
}

/* ── 绑定页底部操作栏 ── */
.bind-view__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(64, 158, 255, 0.12);
  margin-top: 16px;
}

.bind-view__actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-left: auto;
}

@media (max-width: 720px) {
  .bind-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>

<style>
/* 违章区域下拉菜单深色样式 (全局,因为 teleported) */
.violation-area-dropdown {
  background: rgba(8, 30, 50, 0.98) !important;
  border: 1px solid rgba(64, 158, 255, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5) !important;
}

.violation-area-dropdown .el-select-dropdown__item {
  color: rgba(205, 222, 248, 0.9) !important;
  background: transparent !important;
}

.violation-area-dropdown .el-select-dropdown__item:hover {
  background: rgba(64, 158, 255, 0.15) !important;
}

.violation-area-dropdown .el-select-dropdown__item.is-selected {
  color: #73ebff !important;
  background: rgba(64, 158, 255, 0.2) !important;
}

.violation-area-dropdown .el-select-dropdown__item.is-hovering {
  background: rgba(64, 158, 255, 0.15) !important;
}

/* 违章区域选中标签深色样式 */
.bind-row .field-stack .el-select .el-tag {
  background: rgba(64, 158, 255, 0.2) !important;
  border-color: rgba(64, 158, 255, 0.4) !important;
  color: #73ebff !important;
}

.bind-row .field-stack .el-select .el-tag .el-tag__close {
  color: rgba(115, 235, 255, 0.8) !important;
  background: transparent !important;
}

.bind-row .field-stack .el-select .el-tag .el-tag__close:hover {
  background: rgba(64, 158, 255, 0.3) !important;
  color: #73ebff !important;
}
</style>
