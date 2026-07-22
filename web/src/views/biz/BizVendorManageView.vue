<template>
  <PageContainer title="流动摊贩管理">
    <WebListPageTemplate filter-title="查询条件" table-title="流动摊贩列表" :table-meta="tableMeta">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.keyword" aria-label="摊贩关键词" placeholder="请输入摊贩名称" clearable @change="resetAndReload" />
          </label>
          <label class="field-stack">
            <ElSelect v-model="filters.status" clearable placeholder="请选择摊贩状态" aria-label="摊贩状态" @change="resetAndReload">
              <ElOption v-for="item in statusOptions" :key="item" :label="statusLabel(item)" :value="item" />
            </ElSelect>
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="resetAndReload">
              {{ loading ? '加载中...' : '刷新数据' }}
            </button>
            <button v-if="canCreate" type="button" class="action-button" @click="openCreateDialog">新增摊贩</button>
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无摊贩数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>摊贩名称</th>
              <th class="col-photo">摊贩照片</th>
              <th>法人名称</th>
              <th class="col-photo">法人照片</th>
              <th>法人电话</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.vendorName }}</td>
              <td>
                <img v-if="item.vendorPhotoUrl" :src="toImageUrl(item.vendorPhotoUrl)" alt="摊贩照片" class="table-thumb table-thumb--clickable" @click="openThumbPreview(toImageUrl(item.vendorPhotoUrl))" />
                <span v-else>—</span>
              </td>
              <td>{{ item.legalPersonName || '—' }}</td>
              <td>
                <img v-if="item.legalPersonPhotoUrl" :src="toImageUrl(item.legalPersonPhotoUrl)" alt="法人照片" class="table-thumb table-thumb--clickable" @click="openThumbPreview(toImageUrl(item.legalPersonPhotoUrl))" />
                <span v-else>—</span>
              </td>
              <td>{{ item.legalPersonPhone || '—' }}</td>
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
      subtitle="业务管理 / 流动摊贩档案"
      :model-value="formModel"
      :fields="dialogFields"
      :errors="errors"
      @close="closeDialog"
      @save="handleSave"
      @update:model-value="updateFormModel"
    />

    <ImagePreviewOverlay
      v-model="thumbPreviewVisible"
      :images="thumbPreviewImages"
      :index="0"
    />
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="业务管理 / 流动摊贩档案"
      :message="`确定删除摊贩「${deleteTarget?.vendorName ?? ''}」吗？`"
      description="删除后该摊贩将从列表中移除。"
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
  createBizVendor,
  deleteBizVendor,
  getBizVendorDetail,
  listBizVendorsPaged,
  updateBizVendor,
  type BizVendor,
  type BizVendorSavePayload
} from '../../api/biz-vendor'
import type { BizEntityStatus } from '../../api/biz-area'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import BizEntityDialog from '../../components/system/BizEntityDialog.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import { hasPermission } from '../../auth/permissions'
import { buildBaseErrors, isLikelyPhone } from './shared'
import { fetchAccessPrefix, toImageUrl } from '../../api/upload'
import { usePagination } from '../../composables/usePagination'
import ImagePreviewOverlay from '../../components/ImagePreviewOverlay.vue'

const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const thumbPreviewVisible = ref(false)
const thumbPreviewImages = ref<string[]>([])

function openThumbPreview(url: string) {
  thumbPreviewImages.value = [url]
  thumbPreviewVisible.value = true
}
const errorMessage = ref('')
const deleteDialogOpen = ref(false)
const deleteTarget = ref<BizVendor | null>(null)
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
  vendorName: '',
  vendorPhotoUrl: '',
  legalPersonName: '',
  legalPersonPhotoUrl: '',
  legalPersonPhone: '',
  remark: '',
  status: 'ACTIVE'
})

const { items, total, loading, currentPage, pageSize, changePage, resetAndReload } = usePagination<BizVendor>({
  fetcher: (page: number, ps: number) => listBizVendorsPaged(page, ps, { keyword: filters.keyword, status: filters.status }),
  filters: () => filters
})

const canCreate = computed(() => hasPermission('button:biz:vendor:create'))
const canUpdate = computed(() => hasPermission('button:biz:vendor:update'))
const canDelete = computed(() => hasPermission('button:biz:vendor:delete'))
const dialogTitle = computed(() => (editingId.value ? '编辑流动摊贩' : '新增流动摊贩'))
const tableMeta = computed(() => `当前共 ${total.value} 个摊贩`)
const dialogFields = computed(() => [
  { key: 'vendorName', label: '摊贩名称', placeholder: '请输入摊贩名称' },
  { key: 'vendorPhotoUrl', label: '摊贩照片', type: 'image-upload' as const },
  { key: 'legalPersonName', label: '法人名称', placeholder: '请输入法人名称' },
  { key: 'legalPersonPhotoUrl', label: '法人照片', type: 'image-upload' as const },
  { key: 'legalPersonPhone', label: '法人电话', type: 'tel' as const, placeholder: '请输入法人电话' },
  {
    key: 'status',
    label: '状态',
    type: 'select' as const,
    options: statusOptions.map((item) => ({ label: statusLabel(item), value: item }))
  },
  {
    key: 'remark',
    label: '备注',
    type: 'textarea' as const,
    rows: 4,
    className: 'field-stack--full',
    placeholder: '请输入备注'
  }
])

onMounted(() => {
  void fetchAccessPrefix()
  void resetAndReload()
})

function statusLabel(status: BizEntityStatus) {
  return status === 'ACTIVE' ? '启用' : '停用'
}

function resetErrors() {
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
}

function resetForm() {
  editingId.value = null
  formModel.vendorName = ''
  formModel.vendorPhotoUrl = ''
  formModel.legalPersonName = ''
  formModel.legalPersonPhotoUrl = ''
  formModel.legalPersonPhone = ''
  formModel.remark = ''
  formModel.status = 'ACTIVE'
  resetErrors()
}

function updateFormModel(value: Record<string, string | number | null | undefined>) {
  Object.assign(formModel, value)
}

function openCreateDialog() {
  resetForm()
  dialogOpen.value = true
}

async function openEditDialog(id: number) {
  resetForm()
  try {
    const detail = await getBizVendorDetail(id)
    editingId.value = detail.id
    formModel.vendorName = detail.vendorName
    formModel.vendorPhotoUrl = detail.vendorPhotoUrl || ''
    formModel.legalPersonName = detail.legalPersonName || ''
    formModel.legalPersonPhotoUrl = detail.legalPersonPhotoUrl || ''
    formModel.legalPersonPhone = detail.legalPersonPhone || ''
    formModel.remark = detail.remark || ''
    formModel.status = detail.status
    dialogOpen.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '摊贩详情加载失败'
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

  if (!String(formModel.vendorName || '').trim()) {
    errors.vendorName = '请输入摊贩名称'
    valid = false
  }
  if (!isLikelyPhone(String(formModel.legalPersonPhone || ''))) {
    errors.legalPersonPhone = '法人电话格式不正确'
    valid = false
  }

  return valid
}

async function handleSave() {
  if (!validateForm()) {
    return
  }

  const payload: BizVendorSavePayload = {
    vendorName: String(formModel.vendorName || ''),
    vendorPhotoUrl: String(formModel.vendorPhotoUrl || ''),
    legalPersonName: String(formModel.legalPersonName || ''),
    legalPersonPhotoUrl: String(formModel.legalPersonPhotoUrl || ''),
    legalPersonPhone: String(formModel.legalPersonPhone || ''),
    remark: String(formModel.remark || ''),
    status: (formModel.status as BizEntityStatus) || 'ACTIVE'
  }

  try {
    if (editingId.value) {
      await updateBizVendor(editingId.value, payload)
    } else {
      await createBizVendor(payload)
    }
    closeDialog()
    await resetAndReload()
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError ? error.message : '摊贩保存失败'
    toast.error(errorMessage.value)
  }
}

function handleDelete(item: BizVendor) {
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
    await deleteBizVendor(deleteTarget.value.id)
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await resetAndReload()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '摊贩删除失败'
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

.table-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  vertical-align: middle;
  background: rgba(0, 0, 0, 0.2);
}

.table-thumb--clickable {
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}

.table-thumb--clickable:hover {
  opacity: 0.85;
  transform: scale(1.08);
}

.col-photo {
  width: 80px;
}
</style>
