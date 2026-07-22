<template>
  <PageContainer title="商户管理">
    <WebListPageTemplate filter-title="查询条件" table-title="商户列表" :table-meta="tableMeta">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="filters.keyword" aria-label="商户关键词" placeholder="请输入商户名称" clearable @change="resetAndReload" />
          </label>
          <label class="field-stack">
            <ElSelect v-model="filters.areaId" clearable placeholder="请选择所属片区" aria-label="所属片区筛选" @change="resetAndReload">
              <ElOption v-for="item in activeAreaOptions" :key="item.id" :label="item.areaName" :value="String(item.id)" />
            </ElSelect>
          </label>
          <label class="field-stack">
            <ElSelect v-model="filters.status" clearable placeholder="请选择商户状态" aria-label="商户状态" @change="resetAndReload">
              <ElOption v-for="item in statusOptions" :key="item" :label="statusLabel(item)" :value="item" />
            </ElSelect>
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" :disabled="loading" @click="resetAndReload">
              {{ loading ? '加载中...' : '刷新数据' }}
            </button>
            <button v-if="canCreate" type="button" class="action-button" @click="openCreateDialog">新增商户</button>
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="loading" class="panel empty-state">加载中...</div>
        <div v-else-if="items.length === 0" class="panel empty-state">暂无商户数据。</div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>商户名称</th>
              <th class="col-photo">商户照片</th>
              <th>经纬度</th>
              <th>所属片区</th>
              <th>法人信息</th>
              <th class="col-photo">法人照片</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id">
              <td>{{ item.merchantName }}</td>
              <td>
                <img v-if="item.merchantPhotoUrl" :src="toImageUrl(item.merchantPhotoUrl)" alt="商户照片" class="table-thumb table-thumb--clickable" @click="openThumbPreview(toImageUrl(item.merchantPhotoUrl))" />
                <span v-else>—</span>
              </td>
              <td>{{ formatCoordinate(item.longitude, item.latitude) }}</td>
              <td>{{ item.areaName || '—' }}</td>
              <td>{{ item.legalPersonName || '—' }} / {{ item.legalPersonPhone || '—' }}</td>
              <td>
                <img v-if="item.legalPersonPhotoUrl" :src="toImageUrl(item.legalPersonPhotoUrl)" alt="法人照片" class="table-thumb table-thumb--clickable" @click="openThumbPreview(toImageUrl(item.legalPersonPhotoUrl))" />
                <span v-else>—</span>
              </td>
              <td><StatusTag :status="item.status" /></td>
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
      subtitle="业务管理 / 商户档案"
      :model-value="formModel"
      :fields="dialogFields"
      :errors="errors"
      @close="closeDialog"
      @save="handleSave"
      @update:model-value="updateFormModel"
    >
      <template #field-location>
        <AMapPointPicker
          :longitude="formModel.longitude"
          :latitude="formModel.latitude"
          placeholder="点击选择商户位置"
          @update:longitude="(v) => { formModel.longitude = v == null ? '' : String(v) }"
          @update:latitude="(v) => { formModel.latitude = v == null ? '' : String(v) }"
        />
      </template>
    </BizEntityDialog>

    <ImagePreviewOverlay
      v-model="thumbPreviewVisible"
      :images="thumbPreviewImages"
      :index="0"
    />
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="业务管理 / 商户档案"
      :message="`确定删除商户「${deleteTarget?.merchantName ?? ''}」吗？`"
      description="删除后该商户将从列表中移除。"
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
  createBizMerchant,
  deleteBizMerchant,
  getBizMerchantDetail,
  listBizMerchantsPaged,
  updateBizMerchant,
  type AreaMatchMode,
  type BizMerchant,
  type BizMerchantSavePayload
} from '../../api/biz-merchant'
import { listBizAreaOptions, type BizAreaOption, type BizEntityStatus } from '../../api/biz-area'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import BizEntityDialog from '../../components/system/BizEntityDialog.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import AMapPointPicker from '../../components/biz/AMapPointPicker.vue'
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
const areaOptions = ref<BizAreaOption[]>([])
const errorMessage = ref('')
const deleteDialogOpen = ref(false)
const deleteTarget = ref<BizMerchant | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)
const toast = useToast()
const filters = reactive({
  keyword: '',
  areaId: '',
  status: '' as BizEntityStatus | ''
})
const statusOptions: BizEntityStatus[] = ['ACTIVE', 'DISABLED']
const errors = reactive(buildBaseErrors())
const formModel = reactive<Record<string, string | number | null | undefined>>({
  merchantName: '',
  merchantPhotoUrl: '',
  longitude: '',
  latitude: '',
  legalPersonName: '',
  legalPersonPhotoUrl: '',
  legalPersonPhone: '',
  areaId: '',
  areaMatchMode: 'MANUAL',
  remark: '',
  status: 'ACTIVE'
})

const { items, total, loading, currentPage, pageSize, changePage, resetAndReload } = usePagination<BizMerchant>({
  fetcher: (page: number, ps: number) => listBizMerchantsPaged(page, ps, {
    keyword: filters.keyword,
    areaId: filters.areaId ? Number(filters.areaId) : undefined,
    status: filters.status
  }),
  filters: () => filters
})

const canCreate = computed(() => hasPermission('button:biz:merchant:create'))
const canUpdate = computed(() => hasPermission('button:biz:merchant:update'))
const canDelete = computed(() => hasPermission('button:biz:merchant:delete'))
const dialogTitle = computed(() => (editingId.value ? '编辑商户' : '新增商户'))
const activeAreaOptions = computed(() => areaOptions.value)
const tableMeta = computed(() => `当前共 ${total.value} 个商户`)
const dialogFields = computed(() => [
  {
    key: 'merchantName',
    label: '商户名称',
    placeholder: '请输入商户名称'
  },
  {
    key: 'merchantPhotoUrl',
    label: '商户照片',
    type: 'image-upload' as const
  },
  {
    key: 'location',
    label: '商户位置（经纬度）',
    type: 'custom' as const,
    className: 'field-stack--full',
    hint: '点击「选择」在地图上选取商户位置'
  },
  {
    key: 'legalPersonName',
    label: '法人名称',
    placeholder: '请输入法人名称'
  },
  {
    key: 'legalPersonPhotoUrl',
    label: '法人照片',
    type: 'image-upload' as const
  },
  {
    key: 'legalPersonPhone',
    label: '法人电话',
    type: 'tel' as const,
    placeholder: '请输入法人电话'
  },
  {
    key: 'areaId',
    label: '所属片区',
    type: 'select' as const,
    options: activeAreaOptions.value.map((item) => ({ label: item.areaName, value: item.id })),
    placeholder: '请选择启用片区'
  },
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
  void loadAreaOptions()
  void resetAndReload()
})

async function loadAreaOptions() {
  try {
    areaOptions.value = await listBizAreaOptions()
  } catch {
    // area options are non-critical, ignore errors
  }
}

function statusLabel(status: BizEntityStatus) {
  return status === 'ACTIVE' ? '启用' : '停用'
}

function areaMatchModeLabel(mode?: AreaMatchMode) {
  if (mode === 'AUTO') {
    return '自动匹配'
  }
  return '人工指定'
}

function formatCoordinate(longitude?: number | null, latitude?: number | null) {
  if (longitude == null || latitude == null) {
    return '—'
  }
  return `${longitude}, ${latitude}`
}

function resetErrors() {
  Object.keys(errors).forEach((key) => {
    errors[key] = ''
  })
}

function resetForm() {
  editingId.value = null
  formModel.merchantName = ''
  formModel.merchantPhotoUrl = ''
  formModel.longitude = ''
  formModel.latitude = ''
  formModel.legalPersonName = ''
  formModel.legalPersonPhotoUrl = ''
  formModel.legalPersonPhone = ''
  formModel.areaId = ''
  formModel.areaMatchMode = 'MANUAL'
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
    const detail = await getBizMerchantDetail(id)
    editingId.value = detail.id
    formModel.merchantName = detail.merchantName
    formModel.merchantPhotoUrl = detail.merchantPhotoUrl || ''
    formModel.longitude = detail.longitude == null ? '' : String(detail.longitude)
    formModel.latitude = detail.latitude == null ? '' : String(detail.latitude)
    formModel.legalPersonName = detail.legalPersonName || ''
    formModel.legalPersonPhotoUrl = detail.legalPersonPhotoUrl || ''
    formModel.legalPersonPhone = detail.legalPersonPhone || ''
    formModel.areaId = detail.areaId == null ? '' : String(detail.areaId)
    formModel.areaMatchMode = detail.areaMatchMode || 'MANUAL'
    formModel.remark = detail.remark || ''
    formModel.status = detail.status
    dialogOpen.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '商户详情加载失败'
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
  const longitude = String(formModel.longitude || '').trim()
  const latitude = String(formModel.latitude || '').trim()

  if (!String(formModel.merchantName || '').trim()) {
    errors.merchantName = '请输入商户名称'
    valid = false
  }
  if (!isLikelyPhone(String(formModel.legalPersonPhone || ''))) {
    errors.legalPersonPhone = '法人电话格式不正确'
    valid = false
  }
  if ((longitude && !latitude) || (!longitude && latitude)) {
    errors.longitude = '经纬度必须成对填写'
    errors.latitude = '经纬度必须成对填写'
    valid = false
  }

  return valid
}

async function handleSave() {
  if (!validateForm()) {
    return
  }

  const payload: BizMerchantSavePayload = {
    merchantName: String(formModel.merchantName || ''),
    merchantPhotoUrl: String(formModel.merchantPhotoUrl || ''),
    longitude: String(formModel.longitude || '').trim() ? Number(formModel.longitude) : null,
    latitude: String(formModel.latitude || '').trim() ? Number(formModel.latitude) : null,
    legalPersonName: String(formModel.legalPersonName || ''),
    legalPersonPhotoUrl: String(formModel.legalPersonPhotoUrl || ''),
    legalPersonPhone: String(formModel.legalPersonPhone || ''),
    areaId: String(formModel.areaId || '').trim() ? Number(formModel.areaId) : null,
    areaMatchMode: 'MANUAL',
    remark: String(formModel.remark || ''),
    status: (formModel.status as BizEntityStatus) || 'ACTIVE'
  }

  try {
    if (editingId.value) {
      await updateBizMerchant(editingId.value, payload)
    } else {
      await createBizMerchant(payload)
    }
    closeDialog()
    await resetAndReload()
  } catch (error) {
    errorMessage.value = error instanceof HttpResponseError ? error.message : '商户保存失败'
    toast.error(errorMessage.value)
  }
}

function handleDelete(item: BizMerchant) {
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
    await deleteBizMerchant(deleteTarget.value.id)
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await resetAndReload()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '商户删除失败'
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
