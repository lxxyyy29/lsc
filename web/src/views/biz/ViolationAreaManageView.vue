<template>
  <PageContainer title="违章区域管理">
    <WebListPageTemplate filter-title="查询条件" table-title="违章区域列表" :table-meta="tableMeta">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.keyword" aria-label="区域名称" placeholder="请输入区域名称" clearable @change="resetAndReload" />
          </label>
          <label class="field-stack">
            <ElSelect v-model="filters.status" clearable placeholder="请选择区域状态" aria-label="区域状态" @change="resetAndReload">
              <ElOption v-for="item in statusOptions" :key="item" :label="statusLabel(item)" :value="item" />
            </ElSelect>
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="resetAndReload">
              {{ loading ? '加载中...' : '刷新数据' }}
            </button>
            <button v-if="canCreate" type="button" class="action-button" @click="openCreateDialog">新增违章区域</button>
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无违章区域数据。</div>
        <template v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>区域名称</th>
                <th>类型</th>
                <th>ROI 配置</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.id">
                <td>{{ item.areaName }}</td>
                <td>{{ VIOLATION_AREA_TYPE_LABEL_MAP[item.areaType ?? ''] || item.areaType || '—' }}</td>
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
      </template>
    </WebListPageTemplate>

    <BizEntityDialog
      :open="dialogOpen"
      :title="dialogTitle"
      subtitle="业务管理 / 违章区域"
      panel-class="system-dialog__panel--wide"
      :model-value="formModel"
      :fields="dialogFields"
      :errors="errors"
      @close="closeDialog"
      @save="handleSave"
      @update:model-value="updateFormModel"
    >
      <template #field-roiJson>
        <AMapRoiDrawer :model-value="String(formModel.roiJson ?? '')" @update:model-value="updateRoiJson" />
      </template>
    </BizEntityDialog>
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="业务管理 / 违章区域"
      :message="`确定删除违章区域「${deleteTarget?.areaName ?? ''}」吗？`"
      description="删除后该违章区域将从列表中移除。"
      :error="deleteError"
      :loading="deleteSaving"
      @close="closeDeleteDialog"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '../../composables/useToast'
import { HttpResponseError } from '../../api/http'
import {
  createViolationArea,
  deleteViolationArea,
  getViolationAreaDetail,
  listViolationAreasPaged,
  updateViolationArea,
  VIOLATION_AREA_TYPE_LABEL_MAP,
  VIOLATION_AREA_TYPE_OPTIONS,
  type ViolationArea,
  type ViolationAreaSavePayload,
  type ViolationAreaStatus
} from '../../api/violation-area'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import BizEntityDialog from '../../components/system/BizEntityDialog.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import AMapRoiDrawer from '../../components/biz/AMapRoiDrawer.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { hasPermission } from '../../auth/permissions'
import { isValidRoiPolygon, safeParseRoiJson } from './shared'
import { usePagination } from '../../composables/usePagination'

const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const errorMessage = ref('')
const deleteDialogOpen = ref(false)
const deleteTarget = ref<ViolationArea | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)
const toast = useToast()
const filters = reactive({
  keyword: '',
  status: '' as ViolationAreaStatus | ''
})
const statusOptions: ViolationAreaStatus[] = ['ACTIVE', 'DISABLED']
const errors = reactive<Record<string, string>>({
  areaName: '',
  areaType: '',
  roiJson: '',
  remark: ''
})
const formModel = reactive<Record<string, string | number | null | undefined>>({
  areaName: '',
  areaType: 'ILLEGAL_STALL',
  roiJson: '',
  remark: '',
  status: 'ACTIVE'
})

const { items, total, loading, currentPage, pageSize, changePage, resetAndReload } = usePagination<ViolationArea>({
  fetcher: (page: number, ps: number) => listViolationAreasPaged(page, ps, { keyword: filters.keyword, status: filters.status }),
  filters: () => filters
})

const canCreate = computed(() => hasPermission('button:biz:violation-area:create'))
const canUpdate = computed(() => hasPermission('button:biz:violation-area:update'))
const canDelete = computed(() => hasPermission('button:biz:violation-area:delete'))
const dialogTitle = computed(() => (editingId.value ? '编辑违章区域' : '新增违章区域'))
const tableMeta = computed(() => `当前共 ${total.value} 个违章区域`)
const dialogFields = computed(() => [
  {
    key: 'areaName',
    label: '区域名称',
    placeholder: '请输入违章区域名称'
  },
  {
    key: 'areaType',
    label: '类型',
    type: 'select' as const,
    options: VIOLATION_AREA_TYPE_OPTIONS
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
    hint: '点击「画多边形」在地图上绘制违章区域范围，至少 3 个顶点'
  },
  {
    key: 'remark',
    label: '备注',
    type: 'textarea' as const,
    rows: 4,
    className: 'field-stack--full',
    placeholder: '请输入备注信息'
  }
])

onMounted(() => {
  void resetAndReload()
})

function statusLabel(status: ViolationAreaStatus) {
  return status === 'ACTIVE' ? '启用' : '停用'
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
  formModel.areaType = '违规摆摊'
  formModel.roiJson = ''
  formModel.remark = ''
  formModel.status = 'ACTIVE'
  editingId.value = null
  resetErrors()
}

function updateFormModel(value: Record<string, string | number | null | undefined>) {
  Object.assign(formModel, value)
}

function updateRoiJson(value: string) {
  formModel.roiJson = value
}

function openCreateDialog() {
  resetForm()
  dialogOpen.value = true
}

async function openEditDialog(id: number) {
  resetForm()
  try {
    const detail = await getViolationAreaDetail(id)
    editingId.value = detail.id
    formModel.areaName = detail.areaName
    formModel.areaType = detail.areaType || ''
    formModel.roiJson = detail.roiJson || ''
    formModel.remark = detail.remark || ''
    formModel.status = detail.status
    dialogOpen.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '详情加载失败'
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
    errors.areaName = '请输入区域名称'
    valid = false
  }

  if (!String(formModel.roiJson || '').trim()) {
    errors.roiJson = '请绘制 ROI 区域'
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

  const payload: ViolationAreaSavePayload = {
    areaName: String(formModel.areaName || ''),
    areaType: String(formModel.areaType || ''),
    roiJson: String(formModel.roiJson || ''),
    remark: String(formModel.remark || ''),
    status: (formModel.status as ViolationAreaStatus) || 'ACTIVE'
  }

  try {
    if (editingId.value) {
      await updateViolationArea(editingId.value, payload)
    } else {
      await createViolationArea(payload)
    }
    closeDialog()
    await resetAndReload()
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError ? error.message : '保存失败'
    toast.error(errorMessage.value)
  }
}

function handleDelete(item: ViolationArea) {
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
    await deleteViolationArea(deleteTarget.value.id)
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await resetAndReload()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '删除失败'
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
</style>
