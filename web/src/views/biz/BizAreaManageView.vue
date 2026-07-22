<template>
  <PageContainer title="片区管理">
    <WebListPageTemplate filter-title="查询条件" table-title="片区列表" :table-meta="tableMeta">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.keyword" aria-label="片区名称" placeholder="请输入片区名称" clearable @change="resetAndReload" />
          </label>
          <label class="field-stack">
            <ElSelect v-model="filters.status" clearable placeholder="请选择片区状态" aria-label="片区状态" @change="resetAndReload">
              <ElOption v-for="item in statusOptions" :key="item" :label="statusLabel(item)" :value="item" />
            </ElSelect>
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="resetAndReload">
              {{ loading ? '加载中...' : '刷新数据' }}
            </button>
            <button v-if="canCreate" type="button" class="action-button" @click="openCreateDialog">新增片区</button>
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无片区数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>片区名称</th>
              <th>负责人</th>
              <th>联系电话</th>
              <th>ROI 配置</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.areaName }}</td>
              <td>{{ item.principalName || '—' }}</td>
              <td>{{ item.principalPhone || '—' }}</td>
              <td>{{ formatRoiSummary(item.roiJson) }}</td>
              <td><StatusTag :status="item.status" /></td>
              <td>{{ item.updatedAt || '—' }}</td>
              <td>
                <div class="table-actions">
                  <button v-if="canUpdate" type="button" class="action-link" @click="openEditDialog(item.id)">编辑</button>
                  <button v-if="canDelete" type="button" class="action-link" @click="handleDelete(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination v-if="total > 0" :total="total" :current-page="currentPage" :page-size="pageSize" :disabled="loading" @change="changePage" />
      </template>
    </WebListPageTemplate>

    <BizEntityDialog
      :open="dialogOpen"
      :title="dialogTitle"
      subtitle="业务管理 / 片区档案"
      panel-class="system-dialog__panel--wide"
      :model-value="formModel"
      :fields="dialogFields"
      :errors="errors"
      @close="closeDialog"
      @save="handleSave"
      @update:model-value="updateFormModel"
    >
      <template #field-principalName>
        <ElSelect
          v-model="selectedOwnerUserId"
          filterable
          clearable
          :loading="systemUsersLoading"
          placeholder="请选择负责人"
          aria-label="负责人"
          :filter-method="handleOwnerUserFilter"
          @change="handleOwnerUserChange"
          @clear="clearOwnerUserSelection"
        >
          <ElOption
            v-for="user in filteredOwnerUsers"
            :key="user.id"
            :label="user.realName"
            :value="String(user.id)"
          >
            {{ formatOwnerUserLabel(user) }}
          </ElOption>
        </ElSelect>
      </template>
      <template #field-principalPhone>
        <input :value="String(formModel.principalPhone ?? '')" type="text" aria-label="负责人电话" placeholder="选择负责人后自动带出" readonly />
      </template>
      <template #field-roiJson>
        <AMapRoiDrawer :model-value="String(formModel.roiJson ?? '')" @update:model-value="updateRoiJson" />
      </template>
    </BizEntityDialog>
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="业务管理 / 片区档案"
      :message="`确定删除片区「${deleteTarget?.areaName ?? ''}」吗？`"
      description="删除后该片区将从列表中移除。"
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
import { HttpResponseError } from '../../api/http'
import {
  createBizArea,
  deleteBizArea,
  getBizAreaDetail,
  listBizAreasPaged,
  updateBizArea,
  type BizArea,
  type BizAreaSavePayload,
  type BizEntityStatus
} from '../../api/biz-area'
import { listSystemUsers, type SystemUser } from '../../api/system-user'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import BizEntityDialog from '../../components/system/BizEntityDialog.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import AMapRoiDrawer from '../../components/biz/AMapRoiDrawer.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { hasPermission } from '../../auth/permissions'
import { buildBaseErrors, isValidRoiPolygon, safeParseRoiJson } from './shared'
import { usePagination } from '../../composables/usePagination'

const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const systemUsers = ref<SystemUser[]>([])
const systemUsersLoading = ref(false)
const ownerUserSearch = ref('')
const selectedOwnerUserId = ref('')
const errorMessage = ref('')
const deleteDialogOpen = ref(false)
const deleteTarget = ref<BizArea | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)
const toast = useToast()
const filters = reactive({
  keyword: '',
  status: '' as BizEntityStatus | ''
})
const statusOptions: BizEntityStatus[] = ['ACTIVE', 'DISABLED']
const errors = reactive(buildBaseErrors())
const formModel = reactive<Record<string, string | number | null | undefined>>({
  areaName: '',
  principalName: '',
  principalPhone: '',
  roiJson: '',
  remark: '',
  status: 'ACTIVE'
})

const { items, total, loading, currentPage, pageSize, changePage, resetAndReload } = usePagination<BizArea>({
  fetcher: (page: number, ps: number) => listBizAreasPaged(page, ps, { keyword: filters.keyword, status: filters.status }),
  filters: () => filters
})

const activeSystemUsers = computed(() => systemUsers.value.filter((user) => user.status === 'ACTIVE'))
const filteredOwnerUsers = computed(() => {
  const keyword = ownerUserSearch.value.trim().toLowerCase()
  if (!keyword) {
    return activeSystemUsers.value
  }
  return activeSystemUsers.value.filter((user) => formatOwnerUserLabel(user).toLowerCase().includes(keyword))
})
const canCreate = computed(() => hasPermission('button:biz:area:create'))
const canUpdate = computed(() => hasPermission('button:biz:area:update'))
const canDelete = computed(() => hasPermission('button:biz:area:delete'))
const dialogTitle = computed(() => (editingId.value ? '编辑片区' : '新增片区'))
const tableMeta = computed(() => `当前共 ${total.value} 个片区`)
const dialogFields = computed(() => [
  {
    key: 'areaName',
    label: '片区名称',
    placeholder: '请输入片区名称'
  },
  {
    key: 'principalName',
    label: '负责人',
    type: 'custom' as const,
    placeholder: '请选择负责人'
  },
  {
    key: 'principalPhone',
    label: '负责人电话',
    type: 'custom' as const,
    hint: '手机号来自所选系统用户'
  },
  {
    key: 'status',
    label: '状态',
    type: 'select' as const,
    options: statusOptions.map((item) => ({
      label: statusLabel(item),
      value: item
    }))
  },
  {
    key: 'roiJson',
    label: 'ROI 区域',
    type: 'custom' as const,
    className: 'field-stack--full',
    hint: '点击「画多边形」在地图上绘制片区范围，至少 3 个顶点'
  },
  {
    key: 'remark',
    label: '备注',
    type: 'textarea' as const,
    rows: 4,
    className: 'field-stack--full',
    placeholder: '请输入片区备注'
  }
])

onMounted(() => {
  void resetAndReload()
})

function statusLabel(status: BizEntityStatus) {
  return status === 'ACTIVE' ? '启用' : '停用'
}

function formatOwnerUserLabel(user: SystemUser) {
  return [user.realName, user.username, user.phone].filter(Boolean).join(' / ')
}

function formatRoiSummary(value?: string) {
  if (!value?.trim()) {
    return '未配置'
  }
  const parsed = safeParseRoiJson(value)
  if (!parsed.valid || !Array.isArray(parsed.parsed)) {
    return '格式异常'
  }
  return `${parsed.parsed.length} 个点`
}

function resetErrors() {
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
}

function resetForm() {
  formModel.areaName = ''
  formModel.principalName = ''
  formModel.principalPhone = ''
  formModel.roiJson = ''
  formModel.remark = ''
  formModel.status = 'ACTIVE'
  ownerUserSearch.value = ''
  selectedOwnerUserId.value = ''
  editingId.value = null
  resetErrors()
}

function updateFormModel(value: Record<string, string | number | null | undefined>) {
  Object.assign(formModel, value)
}

function updateRoiJson(value: string) {
  formModel.roiJson = value
}

function clearOwnerUserSelection() {
  selectedOwnerUserId.value = ''
  formModel.principalName = ''
  formModel.principalPhone = ''
}

function handleOwnerUserFilter(value: string) {
  ownerUserSearch.value = value
}

function handleOwnerUserChange(value: string | number | boolean) {
  const selectedId = String(value || '')
  selectedOwnerUserId.value = selectedId
  if (!selectedId) {
    clearOwnerUserSelection()
    return
  }
  const user = activeSystemUsers.value.find((item) => String(item.id) === selectedId)
  if (!user) {
    return
  }
  formModel.principalName = user.realName
  formModel.principalPhone = user.phone || ''
  ownerUserSearch.value = ''
}

function syncSelectedOwnerUser() {
  const matchedUser = activeSystemUsers.value.find(
    (user) => user.realName === String(formModel.principalName || '') && (user.phone || '') === String(formModel.principalPhone || '')
  )
  selectedOwnerUserId.value = matchedUser ? String(matchedUser.id) : ''
}

async function loadSystemUsers() {
  systemUsersLoading.value = true
  try {
    systemUsers.value = await listSystemUsers()
  } catch {
    systemUsers.value = []
  } finally {
    systemUsersLoading.value = false
    syncSelectedOwnerUser()
  }
}

async function openCreateDialog() {
  resetForm()
  await loadSystemUsers()
  dialogOpen.value = true
}

async function openEditDialog(id: number) {
  resetForm()
  try {
    const detail = await getBizAreaDetail(id)
    editingId.value = detail.id
    formModel.areaName = detail.areaName
    formModel.principalName = detail.principalName || ''
    formModel.principalPhone = detail.principalPhone || ''
    formModel.roiJson = detail.roiJson || ''
    formModel.remark = detail.remark || ''
    formModel.status = detail.status
    await loadSystemUsers()
    dialogOpen.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '片区详情加载失败'
    toast.error(errorMessage.value)
  }
}

function closeDialog() {
  dialogOpen.value = false
  resetForm()
}

function validateForm() {
  resetErrors()
  let valid = true

  if (!String(formModel.areaName || '').trim()) {
    errors.areaName = '请输入片区名称'
    valid = false
  }

  if (!String(formModel.roiJson || '').trim()) {
    errors.roiJson = '请输入 ROI 区域配置'
    valid = false
  } else if (!isValidRoiPolygon(String(formModel.roiJson || ''))) {
    errors.roiJson = 'ROI 区域配置必须包含至少 3 个坐标点'
    valid = false
  }

  return valid
}

async function handleSave() {
  if (!validateForm()) {
    return
  }

  const payload: BizAreaSavePayload = {
    areaName: String(formModel.areaName || ''),
    principalName: String(formModel.principalName || ''),
    principalPhone: String(formModel.principalPhone || ''),
    roiJson: String(formModel.roiJson || ''),
    remark: String(formModel.remark || ''),
    status: (formModel.status as BizEntityStatus) || 'ACTIVE'
  }

  try {
    if (editingId.value) {
      await updateBizArea(editingId.value, payload)
    } else {
      await createBizArea(payload)
    }
    closeDialog()
    await resetAndReload()
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError ? error.message : '片区保存失败'
    toast.error(errorMessage.value)
  }
}

function handleDelete(item: BizArea) {
  deleteTarget.value = item
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
  deleteSaving.value = true
  deleteError.value = ''
  try {
    await deleteBizArea(deleteTarget.value.id)
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await resetAndReload()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '片区删除失败'
  } finally {
    deleteSaving.value = false
  }
}
</script>

<style scoped>
@import '../admin-shared.css';

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.empty-state,
.error-text {
  color: rgba(205, 222, 248, 0.88);
}

.error-text {
  color: #ffb4b4;
}

:deep(.el-select__placeholder) {
  color: rgba(255, 255, 255, 0.45);
}

:deep(.el-select__selected-item.el-select__placeholder),
:deep(.el-select__selected-item.el-select__placeholder span) {
  color: #fff;
}
</style>
