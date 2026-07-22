<template>
  <PageContainer title="AI算法模型">
    <WebListPageTemplate filter-title="查询条件" table-title="AI模型列表" :table-meta="tableMeta">
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="keyword" aria-label="AI模型关键词" placeholder="模型名称 / 检测标签" clearable />
          </label>
        </QueryPanel>
        <div class="toolbar-row">
          <div class="action-row">
            <button type="button" class="action-button" @click="openCreateDialog">新增模型</button>
            <button type="button" class="action-button action-button--secondary" @click="refreshData" :disabled="pagination.loading.value">刷新</button>
          </div>
        </div>
      </template>

      <template #table>
        <div v-if="pagination.loading.value" class="panel empty-state">加载中...</div>
        <table v-else-if="filteredItems.length" class="data-table">
          <thead>
            <tr>
              <th>模型名称</th>
              <th>检测标签</th>
              <th>推送频率（秒）</th>
              <th>状态</th>
              <th>备注</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredItems" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.label || '--' }}</td>
              <td>{{ item.intervalSecond ?? '--' }}</td>
              <td>
                <span :class="item.status === 0 ? 'status-tag status-tag--on' : 'status-tag status-tag--off'">
                  {{ item.status === 0 ? '启用' : '未启用' }}
                </span>
              </td>
              <td>{{ item.description || '--' }}</td>
              <td>{{ formatDroneTimestamp(item.createTime) }}</td>
              <td>
                <div class="table-actions">
                  <button type="button" class="action-link" @click="openEditDialog(item)">编辑</button>
                  <button type="button" class="action-link action-link--danger" @click="openDeleteConfirm(item)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <ListPagination
          v-if="pagination.total.value > 0"
          :total="pagination.total.value"
          :current-page="pagination.currentPage.value"
          :page-size="pagination.pageSize"
          :disabled="pagination.loading.value"
          @change="pagination.changePage"
        />
        <div v-else-if="!pagination.loading.value" class="panel empty-state">暂无AI模型数据。</div>
      </template>
    </WebListPageTemplate>
  </PageContainer>

  <!-- 新增 / 编辑弹窗 -->
  <SystemDialog :open="formDialogOpen" :title="editingItem ? '编辑模型' : '新增模型'" subtitle="AI算法模型" @close="closeFormDialog">
    <div class="dialog-body">
      <label class="field-stack">
        <span>模型名称 <em class="required">*</em></span>
        <input v-model="form.name" placeholder="请输入模型名称" aria-label="模型名称" />
      </label>
      <label class="field-stack">
        <span>检测标签 <em class="required">*</em></span>
        <input v-model="form.label" placeholder="多个标签用逗号分隔" aria-label="检测标签" />
        <span class="field-hint">多个标签用英文逗号分隔，如：汽车,行人</span>
      </label>
      <label class="field-stack">
        <span>推送频率（秒）</span>
        <input v-model.number="form.intervalSecond" type="number" min="1" placeholder="默认 10" aria-label="推送频率" />
      </label>
      <label class="field-stack">
        <span>备注</span>
        <input v-model="form.description" placeholder="选填" aria-label="备注" />
      </label>
    </div>
    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="closeFormDialog">取消</button>
      <button
        type="button"
        class="action-button"
        :disabled="!form.name.trim() || !form.label.trim() || formLoading"
        @click="handleFormSubmit"
      >
        {{ formLoading ? '保存中...' : '确认保存' }}
      </button>
    </template>
  </SystemDialog>

  <!-- 删除确认弹窗 -->
  <SystemDialog :open="deleteDialogOpen" title="确认删除" subtitle="AI算法模型" @close="closeDeleteDialog">
    <p class="confirm-text">确定要删除模型「{{ deletingItem?.name }}」吗？此操作不可撤销。</p>
    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="closeDeleteDialog">取消</button>
      <button type="button" class="action-button action-button--danger" :disabled="deleteLoading" @click="handleDelete">
        {{ deleteLoading ? '删除中...' : '确认删除' }}
      </button>
    </template>
  </SystemDialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from '../../composables/useToast'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'
import ListPagination from '../../components/admin/ListPagination.vue'
import SystemDialog from '../../components/system/SystemDialog.vue'
import {
  formatDroneTimestamp,
  listQwenModelsPaged,
  createQwenModel,
  updateQwenModel,
  deleteQwenModel,
  type QwenAlgorithmModel
} from '../../api/drone'
import { usePagination } from '../../composables/usePagination'

const errorMessage = ref('')
const toast = useToast()
const keyword = ref('')

const pagination = usePagination<QwenAlgorithmModel, Record<string, never>>({
  pageSize: 10,
  filters: ref({} as Record<string, never>),
  fetcher: async (page, pageSize) => {
    return await listQwenModelsPaged(page, pageSize)
  }
})

const filteredItems = computed(() => {
  let items = pagination.items.value
  if (keyword.value.trim()) {
    const kw = keyword.value.trim().toLowerCase()
    items = items.filter(
      (item) =>
        item.name.toLowerCase().includes(kw) ||
        (item.label ?? '').toLowerCase().includes(kw)
    )
  }
  return items
})

const tableMeta = computed(() => `当前共 ${pagination.total.value} 个模型`)

async function refreshData() {
  errorMessage.value = ''
  try {
    await pagination.loadPage()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'AI模型加载失败'
    toast.error(errorMessage.value)
  }
}

// ── Form dialog ──
const formDialogOpen = ref(false)
const editingItem = ref<QwenAlgorithmModel | null>(null)
const formLoading = ref(false)
const formError = ref('')

const form = reactive({
  name: '',
  label: '',
  intervalSecond: 10,
  description: ''
})

function openCreateDialog() {
  editingItem.value = null
  form.name = ''
  form.label = ''
  form.intervalSecond = 10
  form.description = ''
  formError.value = ''
  formDialogOpen.value = true
}

function openEditDialog(item: QwenAlgorithmModel) {
  editingItem.value = item
  form.name = item.name
  form.label = item.label
  form.intervalSecond = item.intervalSecond ?? 10
  form.description = item.description ?? ''
  formError.value = ''
  formDialogOpen.value = true
}

function closeFormDialog() {
  formDialogOpen.value = false
  formError.value = ''
}

async function handleFormSubmit() {
  if (!form.name.trim() || !form.label.trim()) return
  formLoading.value = true
  formError.value = ''
  try {
    if (editingItem.value) {
      await updateQwenModel(editingItem.value.id, {
        name: form.name.trim(),
        label: form.label.trim(),
        intervalSecond: form.intervalSecond ?? 10,
        description: form.description.trim() || null
      })
    } else {
      await createQwenModel({
        name: form.name.trim(),
        label: form.label.trim(),
        intervalSecond: form.intervalSecond ?? 10,
        description: form.description.trim() || undefined
      })
    }
    formDialogOpen.value = false
    await refreshData()
  } catch (error) {
    formError.value = error instanceof Error ? error.message : '保存失败，请重试'
  } finally {
    formLoading.value = false
  }
}

// ── Delete dialog ──
const deleteDialogOpen = ref(false)
const deletingItem = ref<QwenAlgorithmModel | null>(null)
const deleteLoading = ref(false)
const deleteError = ref('')

function openDeleteConfirm(item: QwenAlgorithmModel) {
  deletingItem.value = item
  deleteError.value = ''
  deleteDialogOpen.value = true
}

function closeDeleteDialog() {
  deleteDialogOpen.value = false
  deleteError.value = ''
}

async function handleDelete() {
  if (!deletingItem.value) return
  deleteLoading.value = true
  deleteError.value = ''
  try {
    await deleteQwenModel(deletingItem.value.id)
    deleteDialogOpen.value = false
    await refreshData()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '删除失败，请重试'
  } finally {
    deleteLoading.value = false
  }
}

onMounted(() => {
  void refreshData()
})
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

.action-row {
  display: flex;
  gap: 8px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag--on {
  color: #6ee6a7;
  background: rgba(110, 230, 167, 0.1);
  border: 1px solid rgba(110, 230, 167, 0.25);
}

.status-tag--off {
  color: rgba(205, 222, 248, 0.5);
  background: rgba(205, 222, 248, 0.06);
  border: 1px solid rgba(205, 222, 248, 0.12);
}

.table-actions {
  display: flex;
  gap: 8px;
}

.dialog-body {
  display: grid;
  gap: 16px;
}

.field-hint {
  font-size: 11px;
  color: rgba(205, 222, 248, 0.45);
  margin-top: 4px;
}

.required {
  color: #ff7875;
  font-style: normal;
  margin-left: 2px;
}

.confirm-text {
  margin: 0;
  color: var(--fg-text-primary, #eef5ff);
  font-size: 14px;
  line-height: 1.6;
}

.empty-state,
.error-text {
  margin-top: 16px;
}

.error-text {
  color: #ffb4b4;
  margin: 8px 0 0;
}
</style>
