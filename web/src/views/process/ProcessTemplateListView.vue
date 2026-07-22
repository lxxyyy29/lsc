<template>
  <PageContainer title="流程配置">
    <div class="process-config-page">
      <header class="process-header">
        <div class="process-header__kicker">
          <span class="process-header__icon">⚙</span>
          <span>流程配置</span>
        </div>
        <h3 class="process-header__title">流程配置</h3>
      </header>

      <div class="process-toolbar">
        <div class="toolbar-search">
          <label class="field-stack search-input-wrapper">
            <ElInput v-model="keywordInput" placeholder="请输入流程配置名称" clearable @keydown.enter="applyKeyword" />
          </label>
          <button class="action-button" @click="applyKeyword">
            查询
          </button>
          <button class="action-button action-button--secondary" @click="resetKeyword">
            重置
          </button>
        </div>
        <button class="action-button" @click="openCreateModal">
          + 新建
        </button>
      </div>

      <p v-if="infoMessage" class="process-message">{{ infoMessage }}</p>

      <div v-if="loading" class="process-empty-state">加载中...</div>
      <div v-else-if="items.length" class="process-grid">
        <div class="process-card" v-for="template in items" :key="template.id">
          <div class="process-card__header">
            <div class="process-card__title">
              <span class="process-card__icon">👥</span>
              <h4>{{ template.templateName }}</h4>
            </div>
            <span class="status-badge" :class="{ 'status-badge--active': template.status === 'ACTIVE' }">
              {{ template.status === 'ACTIVE' ? '启用中' : '未启用' }}
            </span>
          </div>
          <div class="process-card__meta">
            <span>{{ template.eventType || '通用流程' }}</span>
            <span>{{ template.versionLabel }}</span>
            <span>{{ template.nodes.length }} 个节点</span>
          </div>
          <div class="process-card__time">
            {{ formatDisplayTime(template.updatedAt || template.createdAt) || '暂无时间信息' }}
          </div>
          <div class="process-card__actions">
            <button class="action-btn" @click="openEditPage(template.id)">修改</button>
            <button class="action-btn action-btn--primary" @click="openReadonlyPage(template.id)">查看</button>
            <button class="action-btn action-btn--danger" @click="handleDelete(template)">删除</button>
          </div>
        </div>
      </div>
      <ListPagination v-if="total > 0" :total="total" :current-page="currentPage" :page-size="pageSize" :disabled="loading" @change="changePage" />
      <div v-if="!loading && !items.length" class="process-empty-state">暂无流程模板数据。</div>
    </div>

    <div class="modal-overlay" v-if="isModalOpen" @click.self="closeModal">
      <div class="modal-content">
        <header class="modal-header">
          <h3>{{ modalTitle }}</h3>
          <button class="modal-close" @click="closeModal">×</button>
        </header>

        <div class="modal-body">
          <div class="form-group row-group">
            <label>流程名称</label>
            <input v-model="draft.templateName" type="text" class="form-input" placeholder="请输入流程名称" :disabled="isReadonly" />
          </div>

          <div class="form-group row-group">
            <label>启用状态</label>
            <div class="node-select-wrapper">
              <select v-model="draft.enabled" class="node-select" :disabled="isReadonly">
                <option :value="true">启用</option>
                <option :value="false">停用</option>
              </select>
            </div>
          </div>

          <div class="node-list">
            <div class="node-item" v-for="(node, index) in draft.nodes" :key="index">
              <div class="node-header">
                <span>{{ buildNodeName(index) }}</span>
                <button v-if="!isReadonly" class="node-remove" :disabled="draft.nodes.length === 1" @click="removeNode(index)">×</button>
              </div>
              <div class="node-body">
                <label>审批人：</label>
                <div class="node-select-wrapper node-assignee-select-wrapper">
                  <ElSelect
                    v-model="node.assigneeUserId"
                    class="node-assignee-select"
                    popper-class="node-assignee-select-popper"
                    :disabled="isReadonly"
                    placeholder="请选择审批人"
                    @change="syncNodeAssignee(index)"
                  >
                    <ElOption v-for="user in getNodeAssigneeOptions(node)" :key="user.id" :label="user.realName" :value="user.id">
                      {{ formatAssigneeOption(user) }}
                    </ElOption>
                  </ElSelect>
                </div>
              </div>
              <div class="node-caption" v-if="node.assigneeName">已选择：{{ node.assigneeName }}</div>
              <div class="node-arrow" v-if="index < draft.nodes.length - 1">↓</div>
            </div>

            <button v-if="!isReadonly" class="add-node-btn" @click="addNode">+ 添加审批节点</button>
          </div>

        </div>

        <footer class="modal-footer">
          <button class="action-button action-button--secondary" @click="closeModal">取消</button>
          <button v-if="!isReadonly" class="action-button" :disabled="creating" @click="saveTemplate">
            {{ creating ? '提交中...' : modalConfirmText }}
          </button>
        </footer>
      </div>
    </div>
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="流程配置"
      :message="`确定删除流程模板「${deleteTarget?.templateName ?? ''}」吗？`"
      description="删除后该流程模板将从列表中移除。"
      :error="deleteError"
      :loading="deleteSaving"
      @close="closeDeleteDialog"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElOption, ElSelect } from 'element-plus'
import { useToast } from '../../composables/useToast'
import { createProcessTemplate, deleteProcessTemplate, getProcessTemplate, listProcessTemplatesPaged, updateProcessTemplate, type ProcessTemplate } from '../../api/process'
import { listSystemUsers, type SystemUser } from '../../api/system-user'
import PageContainer from '../../components/admin/PageContainer.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import { usePagination } from '../../composables/usePagination'

interface DraftNode {
  assigneeUserId: number | null
  assigneeName: string
}

interface DraftTemplate {
  templateName: string
  enabled: boolean
  eventType?: string
  version?: number
  nodes: DraftNode[]
}

interface AssigneeOption {
  id: number
  realName: string
  username?: string
  phone?: string
}

const modalMode = ref<'create' | 'edit' | 'view'>('create')
const editingTemplateId = ref<number | null>(null)
const users = ref<SystemUser[]>([])
const creating = ref(false)
const infoMessage = ref('')
const modalErrorMessage = ref('')
const toast = useToast()
const keywordInput = ref('')
const isModalOpen = ref(false)
const deleteDialogOpen = ref(false)
const deleteTarget = ref<ProcessTemplate | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)
const draft = reactive<DraftTemplate>(createDefaultDraft())

const filters = ref({ keyword: '' })

const { items, total, loading, errorMessage, currentPage, pageSize, changePage, resetAndReload } = usePagination<ProcessTemplate>({
  fetcher: (page, ps, f) => listProcessTemplatesPaged(page, ps, (f as { keyword: string }).keyword || undefined),
  filters
})

const activeUsers = computed(() => users.value.filter((user) => user.status === 'ACTIVE'))
const isReadonly = computed(() => modalMode.value === 'view')
const modalTitle = computed(() => {
  if (modalMode.value === 'edit') return '修改流程'
  if (modalMode.value === 'view') return '查看流程'
  return '新建流程'
})
const modalConfirmText = computed(() => (modalMode.value === 'edit' ? '保存' : '确定'))

function createDefaultDraft(): DraftTemplate {
  return {
    templateName: '',
    enabled: true,
    eventType: '',
    version: 1,
    nodes: [{ assigneeUserId: null, assigneeName: '' }]
  }
}

function resetDraft() {
  const next = createDefaultDraft()
  draft.templateName = next.templateName
  draft.enabled = next.enabled
  draft.eventType = next.eventType
  draft.version = next.version
  draft.nodes.splice(0, draft.nodes.length, ...next.nodes)
}

function formatDisplayTime(value?: string) {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 16)
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

function buildNodeName(index: number) {
  return `审批节点${index + 1}`
}

function formatAssigneeOption(user: AssigneeOption) {
  return [user.realName, user.username, user.phone].filter(Boolean).join(' / ')
}

function getNodeAssigneeOptions(node: DraftNode): AssigneeOption[] {
  const selectedId = Number(node.assigneeUserId)
  const selectedUser = users.value.find((user) => user.id === selectedId)
  if (selectedUser && !activeUsers.value.some((user) => user.id === selectedUser.id)) {
    return [selectedUser, ...activeUsers.value]
  }
  if (!selectedUser && selectedId > 0 && node.assigneeName.trim()) {
    return [{ id: selectedId, realName: node.assigneeName }, ...activeUsers.value]
  }
  return activeUsers.value
}

function applyKeyword() {
  filters.value = { keyword: keywordInput.value.trim() }
  void resetAndReload()
}

function resetKeyword() {
  keywordInput.value = ''
  filters.value = { keyword: '' }
  void resetAndReload()
}

async function ensureUsersLoaded() {
  if (users.value.length) {
    return
  }
  users.value = await listSystemUsers()
}

async function openCreateModal() {
  modalMode.value = 'create'
  editingTemplateId.value = null
  modalErrorMessage.value = ''
  infoMessage.value = ''
  resetDraft()
  isModalOpen.value = true
  try {
    await ensureUsersLoaded()
  } catch (error) {
    modalErrorMessage.value = error instanceof Error ? error.message : '审批人列表加载失败'
    toast.error(modalErrorMessage.value)
  }
}

async function openEditModal(templateId: number, mode: 'edit' | 'view') {
  modalMode.value = mode
  editingTemplateId.value = templateId
  modalErrorMessage.value = ''
  infoMessage.value = ''
  resetDraft()
  isModalOpen.value = true

  try {
    await ensureUsersLoaded()
    const template = await getProcessTemplate(templateId)
    draft.templateName = template.templateName
    draft.enabled = template.enabled
    draft.eventType = template.eventType
    draft.version = template.version
    draft.nodes.splice(
      0,
      draft.nodes.length,
      ...template.nodes.map((node) => ({
        assigneeUserId: node.assigneeUserId ?? 0,
        assigneeName: node.assigneeName
      }))
    )
  } catch (error) {
    modalErrorMessage.value = error instanceof Error ? error.message : '流程模板加载失败'
    toast.error(modalErrorMessage.value)
  }
}

function closeModal() {
  isModalOpen.value = false
  modalMode.value = 'create'
  editingTemplateId.value = null
  modalErrorMessage.value = ''
  resetDraft()
}

function addNode() {
  draft.nodes.push({ assigneeUserId: null, assigneeName: '' })
}

function removeNode(index: number) {
  if (draft.nodes.length === 1) {
    return
  }
  draft.nodes.splice(index, 1)
}

function syncNodeAssignee(index: number) {
  const node = draft.nodes[index]
  const user = activeUsers.value.find((item) => item.id === Number(node.assigneeUserId))
  node.assigneeName = user?.realName ?? ''
}

function validateDraft() {
  if (!draft.templateName.trim()) {
    return '请输入流程名称'
  }
  if (!draft.nodes.length) {
    return '请至少保留一个审批节点'
  }
  if (!activeUsers.value.length) {
    return '暂无可用审批人'
  }
  for (let index = 0; index < draft.nodes.length; index += 1) {
    const node = draft.nodes[index]
    if (!node.assigneeUserId || !node.assigneeName.trim()) {
      return `请选择${buildNodeName(index)}的审批人`
    }
  }
  return ''
}

async function saveTemplate() {
  modalErrorMessage.value = validateDraft()
  if (modalErrorMessage.value) {
    return
  }

  creating.value = true
  try {
    if (modalMode.value === 'edit' && editingTemplateId.value != null) {
      await updateProcessTemplate(editingTemplateId.value, {
        templateName: draft.templateName,
        eventType: draft.eventType,
        version: draft.version,
        enabled: draft.enabled,
        nodes: draft.nodes.map((node, index) => ({
          assigneeUserId: node.assigneeUserId ?? 0,
          assigneeName: node.assigneeName,
          nodeName: buildNodeName(index)
        }))
      })
      infoMessage.value = '流程模板已更新。'
    } else {
      await createProcessTemplate({
        templateName: draft.templateName,
        eventType: draft.eventType,
        version: draft.version,
        enabled: draft.enabled,
        nodes: draft.nodes.map((node, index) => ({
          assigneeUserId: node.assigneeUserId ?? 0,
          assigneeName: node.assigneeName,
          nodeName: buildNodeName(index)
        }))
      })
      infoMessage.value = '流程模板已创建。'
    }
    closeModal()
    await resetAndReload()
  } catch (error) {
    modalErrorMessage.value = error instanceof Error ? error.message : modalMode.value === 'edit' ? '流程模板更新失败' : '流程模板创建失败'
    toast.error(modalErrorMessage.value)
  } finally {
    creating.value = false
  }
}

function openEditPage(id: number) {
  void openEditModal(id, 'edit')
}

function openReadonlyPage(id: number) {
  void openEditModal(id, 'view')
}

function handleDelete(template: ProcessTemplate) {
  deleteTarget.value = template
  deleteError.value = ''
  deleteDialogOpen.value = true
}

function closeDeleteDialog() {
  if (deleteSaving.value) return
  deleteDialogOpen.value = false
  deleteTarget.value = null
  deleteError.value = ''
}

async function confirmDelete() {
  if (!deleteTarget.value) return
  infoMessage.value = ''
  deleteSaving.value = true
  deleteError.value = ''
  try {
    await deleteProcessTemplate(deleteTarget.value.id)
    infoMessage.value = '流程模板已删除。'
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await resetAndReload()
  } catch (error) {
    // errorMessage is managed by usePagination; show modal error for delete failures
    modalErrorMessage.value = error instanceof Error ? error.message : '流程模板删除失败'
    deleteError.value = modalErrorMessage.value
  } finally {
    deleteSaving.value = false
  }
}

onMounted(async () => {
  await resetAndReload()
})
</script>

<style scoped>
@import '../admin-shared.css';

.process-config-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: calc(100vh - 120px);
}

.process-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.process-header__kicker {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8db0d0;
  font-size: 13px;
}

.process-header__icon {
  font-size: 14px;
}

.process-header__title {
  margin: 0;
  color: #eaf5ff;
  font-size: 18px;
  font-weight: 600;
}

.process-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  gap: 16px;
  flex-wrap: wrap;
}

.toolbar-search {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input-wrapper {
  width: 280px;
}

.process-message,
.process-empty-state {
  margin: 0;
  color: rgba(205, 222, 248, 0.78);
}

.process-message--error {
  color: #ffb4b4;
}

.process-message--compact {
  margin-top: 8px;
}

.process-empty-state {
  margin-top: 16px;
}

.process-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-top: 12px;
}

.process-card {
  background: rgba(15, 30, 45, 0.6);
  border: 1px solid rgba(103, 187, 246, 0.15);
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.process-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.process-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-card__icon {
  color: #5b9bd5;
  font-size: 16px;
}

.process-card__title h4 {
  margin: 0;
  color: #eaf5ff;
  font-size: 15px;
  font-weight: 500;
}

.process-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #8db0d0;
  font-size: 12px;
}

.status-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(100, 100, 100, 0.2);
  color: #aaa;
  border: 1px solid rgba(100, 100, 100, 0.3);
}

.status-badge--active {
  background: rgba(60, 180, 80, 0.15);
  color: #67c23a;
  border-color: rgba(103, 194, 58, 0.3);
}

.process-card__time {
  font-size: 12px;
  color: #6688aa;
}

.process-card__actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.action-btn {
  flex: 1;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  background: rgba(10, 26, 45, 0.72);
  border: 1px solid rgba(125, 163, 220, 0.32);
  color: rgba(238, 245, 255, 0.9);
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
  filter: none;
}

.action-btn--primary {
  color: #73c7ff;
  border-color: rgba(64, 158, 255, 0.45);
  background: rgba(64, 158, 255, 0.12);
}

.action-btn--danger {
  color: #ff9d9d;
  border-color: rgba(255, 120, 117, 0.42);
  background: rgba(255, 120, 117, 0.12);
}

.action-btn:hover:not(:disabled) {
  border-color: rgba(64, 158, 255, 0.55);
  color: #fff;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  width: 540px;
  max-width: calc(100vw - 32px);
  background: #1a2b3c;
  border-radius: 8px;
  border: 1px solid rgba(103, 187, 246, 0.3);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(103, 187, 246, 0.15);
}

.modal-header h3 {
  margin: 0;
  color: #eaf5ff;
  font-size: 16px;
  font-weight: 600;
}

.modal-close {
  background: transparent;
  border: none;
  color: #8db0d0;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: #fff;
}

.modal-body {
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group.row-group {
  flex-direction: row;
  align-items: center;
}

.form-group label {
  color: #8db0d0;
  font-size: 13px;
  width: 96px;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  background: rgba(10, 24, 40, 0.6);
  border: 1px solid rgba(103, 187, 246, 0.2);
  border-radius: 4px;
  color: #eaf5ff;
  padding: 8px 12px;
  font-size: 13px;
  outline: none;
}

.node-list {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 12px;
}

.node-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.node-header {
  width: 80%;
  max-width: 320px;
  background: #6a8bad;
  color: #fff;
  padding: 8px 16px;
  border-radius: 6px 6px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
}

.node-remove {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 16px;
}

.node-remove:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.node-body {
  width: 80%;
  max-width: 320px;
  background: rgba(40, 60, 80, 0.8);
  border: 1px solid rgba(106, 139, 173, 0.3);
  border-top: none;
  border-radius: 0 0 6px 6px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-body label {
  color: #eaf5ff;
  font-size: 13px;
  white-space: nowrap;
}

.node-select-wrapper {
  flex: 1;
  position: relative;
}

.node-select {
  width: 100%;
  background: #1e3046;
  border: 1px solid #3c526d;
  color: #eaf5ff;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
  appearance: none;
  outline: none;
}

.node-select-wrapper::after {
  content: '▼';
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #8db0d0;
  font-size: 10px;
  pointer-events: none;
}

.node-assignee-select-wrapper::after {
  display: none;
}

.node-assignee-select {
  width: 100%;
  --el-component-size: 34px;
  --el-input-height: 34px;
}

.node-assignee-select :deep(.el-select__wrapper) {
  min-height: 34px;
  background: #1e3046;
  border: 1px solid #3c526d;
  box-shadow: none;
}

.node-assignee-select :deep(.el-select__selected-item),
.node-assignee-select :deep(.el-select__selected-item.el-select__placeholder),
.node-assignee-select :deep(.el-select__selected-item.el-select__placeholder span) {
  color: #eaf5ff;
}

.node-assignee-select :deep(.el-select__placeholder) {
  color: rgba(234, 245, 255, 0.55);
}

.node-caption {
  width: 80%;
  max-width: 320px;
  margin-top: 8px;
  color: #8db0d0;
  font-size: 12px;
}

.node-arrow {
  color: #6a8bad;
  margin: 8px 0;
  font-size: 16px;
}

.add-node-btn {
  margin-top: 16px;
  background: transparent;
  border: 1px dashed rgba(103, 187, 246, 0.4);
  color: #8db0d0;
  padding: 8px 32px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.add-node-btn:hover {
  border-color: rgba(103, 187, 246, 0.8);
  color: #eaf5ff;
}

.modal-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid rgba(103, 187, 246, 0.15);
}

</style>
