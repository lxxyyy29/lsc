<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import PageContainer from '../../components/admin/PageContainer.vue'
import QueryPanel from '../../components/admin/QueryPanel.vue'
import WebListPageTemplate from '../../templates/WebListPageTemplate.vue'

type SystemConfigStatus = '已接入' | '调试中' | '已停用'
type SystemConfigRecord = {
  id: number
  name: string
  category: string
  endpoint: string
  status: SystemConfigStatus
  alertPolicy: string
  description: string
}

type SystemConfigSavePayload = Omit<SystemConfigRecord, 'id'> & { id?: number }

const mockSystemConfigStore = ref<SystemConfigRecord[]>([
  {
    id: 1,
    name: '无人机平台',
    category: '设备接入',
    endpoint: 'https://api-uav.example.com',
    status: '已接入',
    alertPolicy: '设备离线告警',
    description: '维护飞行任务、设备心跳与回传鉴权配置。'
  },
  {
    id: 2,
    name: '视频算法平台',
    category: '算法接入',
    endpoint: 'https://api-ai.example.com',
    status: '已接入',
    alertPolicy: '识别异常告警',
    description: '同步识别结果推送地址、回调密钥与设备数据源。'
  },
  {
    id: 3,
    name: '消息通知',
    category: '消息接入',
    endpoint: 'https://api-notify.example.com',
    status: '已接入',
    alertPolicy: '审核与催办通知',
    description: '告警、催办与审核结果通知统一下发。'
  },
  {
    id: 4,
    name: '媒体存储服务',
    category: '媒体接入',
    endpoint: 'https://api-media.example.com',
    status: '调试中',
    alertPolicy: '回传失败提醒',
    description: '图片视频回传、预览、归档留口。'
  }
])

async function listSystemConfigs() {
  return [...mockSystemConfigStore.value]
}

async function saveSystemConfig(payload: SystemConfigSavePayload) {
  if (payload.id) {
    mockSystemConfigStore.value = mockSystemConfigStore.value.map((item) =>
      item.id === payload.id ? { ...item, ...payload, id: payload.id } : item
    )
    return
  }

  const nextId = Math.max(0, ...mockSystemConfigStore.value.map((item) => item.id)) + 1
  mockSystemConfigStore.value = [...mockSystemConfigStore.value, { ...payload, id: nextId }]
}

async function updateSystemConfigStatus(id: number, status: SystemConfigStatus) {
  mockSystemConfigStore.value = mockSystemConfigStore.value.map((item) => (item.id === id ? { ...item, status } : item))
}

const records = ref<SystemConfigRecord[]>([])
const keyword = ref('')
const selectedCategory = ref('')
const selectedStatus = ref<SystemConfigStatus | ''>('')
const selectedAlertPolicy = ref('')
const dialogOpen = ref(false)
const editingRecord = ref<SystemConfigRecord | null>(null)

const form = reactive<SystemConfigSavePayload>({
  name: '',
  category: '设备接入',
  endpoint: '',
  status: '已接入',
  alertPolicy: '',
  description: ''
})

async function loadData() {
  records.value = await listSystemConfigs()
}

void loadData()

const categoryOptions = computed(() => [...new Set(records.value.map((item) => item.category))])
const statusOptions: SystemConfigStatus[] = ['已接入', '调试中', '已停用']
const alertPolicyOptions = computed(() => [...new Set(records.value.map((item) => item.alertPolicy))])

const filteredRecords = computed(() =>
  records.value.filter((item) => {
    const search = keyword.value.trim()
    const matchesKeyword = !search || item.name.includes(search) || item.endpoint.includes(search)
    const matchesCategory = !selectedCategory.value || item.category === selectedCategory.value
    const matchesStatus = !selectedStatus.value || item.status === selectedStatus.value
    const matchesAlertPolicy = !selectedAlertPolicy.value || item.alertPolicy === selectedAlertPolicy.value
    return matchesKeyword && matchesCategory && matchesStatus && matchesAlertPolicy
  })
)

const summaryCards = computed(() => {
  const connectedCount = records.value.filter((item) => item.status === '已接入').length
  const tuningCount = records.value.filter((item) => item.status === '调试中').length
  return [
    { label: '接入配置', value: `${records.value.length} 项`, hint: '当前页面使用本地 mock 数据，可平滑切换为真实接口。' },
    { label: '已接入', value: `${connectedCount} 项`, hint: '已接入平台可参与业务链路联动。' },
    { label: '调试中', value: `${tuningCount} 项`, hint: '待联调完成后可切换为正式接入。' },
    { label: '通知策略', value: `${alertPolicyOptions.value.length} 类`, hint: '按设备、算法与消息场景分组配置。' }
  ]
})

const dialogTitle = computed(() => (editingRecord.value ? '编辑系统配置' : '新增系统配置'))

function resetFilters() {
  keyword.value = ''
  selectedCategory.value = ''
  selectedStatus.value = ''
  selectedAlertPolicy.value = ''
}

function openCreateDialog() {
  editingRecord.value = null
  form.id = undefined
  form.name = ''
  form.category = categoryOptions.value[0] ?? '设备接入'
  form.endpoint = ''
  form.status = '已接入'
  form.alertPolicy = ''
  form.description = ''
  dialogOpen.value = true
}

function openEditDialog(record: SystemConfigRecord) {
  editingRecord.value = record
  form.id = record.id
  form.name = record.name
  form.category = record.category
  form.endpoint = record.endpoint
  form.status = record.status
  form.alertPolicy = record.alertPolicy
  form.description = record.description
  dialogOpen.value = true
}

function closeDialog() {
  dialogOpen.value = false
}

async function handleSave() {
  await saveSystemConfig({
    id: form.id,
    name: form.name.trim(),
    category: form.category.trim(),
    endpoint: form.endpoint.trim(),
    status: form.status,
    alertPolicy: form.alertPolicy.trim(),
    description: form.description.trim()
  })
  dialogOpen.value = false
  await loadData()
}

async function toggleStatus(record: SystemConfigRecord) {
  await updateSystemConfigStatus(record.id, record.status === '已停用' ? '已接入' : '已停用')
  await loadData()
}
</script>

<template>
  <PageContainer title="系统配置">
    <WebListPageTemplate
      eyebrow="系统管理 / 系统配置"
      title="系统配置"
      badge="配置治理"
      filter-eyebrow="配置查询"
      filter-title="查询条件"
      table-eyebrow="配置列表"
      table-title="系统配置列表"
      :table-meta="`当前共 ${filteredRecords.length} 项配置`"
    >
      <template #filters>
        <QueryPanel>
          <label class="field-stack">
            <ElInput v-model="keyword" aria-label="系统配置关键词" placeholder="配置名称 / 接入地址" clearable />
          </label>
          <label class="field-stack">
            <ElSelect v-model="selectedCategory" clearable placeholder="请选择配置分组" aria-label="配置分组">
              <ElOption v-for="item in categoryOptions" :key="item" :label="item" :value="item" />
            </ElSelect>
          </label>
          <label class="field-stack">
            <ElSelect v-model="selectedStatus" clearable placeholder="请选择连接状态" aria-label="连接状态">
              <ElOption v-for="item in statusOptions" :key="item" :label="item" :value="item" />
            </ElSelect>
          </label>
          <label class="field-stack">
            <ElSelect v-model="selectedAlertPolicy" clearable placeholder="请选择通知策略" aria-label="通知策略">
              <ElOption v-for="item in alertPolicyOptions" :key="item" :label="item" :value="item" />
            </ElSelect>
          </label>
        </QueryPanel>

        <div class="toolbar-row">
          <div class="summary-inline">
            <span v-for="item in summaryCards" :key="item.label" class="summary-chip">{{ item.label }} {{ item.value }}</span>
          </div>
          <div class="action-row">
            <button type="button" class="action-button action-button--secondary" @click="resetFilters">重置</button>
            <button type="button" class="action-button" @click="openCreateDialog">新增配置</button>
          </div>
        </div>
      </template>

      <template #table>
        <table class="data-table">
          <thead>
            <tr>
              <th>配置名称</th>
              <th>配置分组</th>
              <th>接入地址</th>
              <th>连接状态</th>
              <th>通知策略</th>
              <th>说明</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredRecords" :key="item.id">
              <td>{{ item.name }}</td>
              <td>{{ item.category }}</td>
              <td>{{ item.endpoint }}</td>
              <td>{{ item.status }}</td>
              <td>{{ item.alertPolicy }}</td>
              <td>{{ item.description }}</td>
              <td>
                <div class="table-actions">
                  <button type="button" class="action-link" @click="openEditDialog(item)">编辑</button>
                  <button type="button" class="action-link" @click="toggleStatus(item)">
                    {{ item.status === '已停用' ? '启用' : '停用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </WebListPageTemplate>

    <div v-if="dialogOpen" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog-card panel" aria-label="系统配置编辑对话框">
        <header class="dialog-header">
          <div>
            <p>系统管理</p>
            <h3>{{ dialogTitle }}</h3>
          </div>
          <button type="button" class="dialog-close" @click="closeDialog">关闭</button>
        </header>

        <div class="dialog-body">
          <label class="field-stack">
            <span>配置名称</span>
            <input v-model="form.name" aria-label="配置名称" />
          </label>
          <label class="field-stack">
            <span>配置分组</span>
            <input v-model="form.category" aria-label="配置分组" />
          </label>
          <label class="field-stack dialog-body__full">
            <span>接入地址</span>
            <input v-model="form.endpoint" aria-label="接入地址" />
          </label>
          <label class="field-stack">
            <span>连接状态</span>
            <select v-model="form.status" aria-label="连接状态编辑">
              <option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="field-stack">
            <span>通知策略</span>
            <input v-model="form.alertPolicy" aria-label="通知策略编辑" />
          </label>
          <label class="field-stack dialog-body__full">
            <span>说明</span>
            <textarea v-model="form.description" rows="4" aria-label="系统配置说明" />
          </label>
        </div>

        <footer class="dialog-footer">
          <button type="button" class="action-button action-button--secondary" @click="closeDialog">取消</button>
          <button type="button" class="action-button" @click="handleSave">保存</button>
        </footer>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
@import '../admin-shared.css';

.toolbar-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.summary-inline,
.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 999px;
  background: rgba(10, 26, 45, 0.72);
  color: rgba(238, 245, 255, 0.88);
  font-size: 12px;
}

.dialog-close {
  border: 0;
  background: transparent;
  color: #73ebff;
  cursor: pointer;
  padding: 0;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(4, 10, 20, 0.56);
  z-index: 30;
}

.dialog-card {
  width: min(720px, 100%);
  display: grid;
  gap: 20px;
}

.dialog-header,
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.dialog-header p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(205, 222, 248, 0.78);
}

.dialog-header h3 {
  margin: 6px 0 0;
}

.dialog-body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.dialog-body__full {
  grid-column: 1 / -1;
}

:deep(.data-table) {
  border-radius: 18px;
  overflow: hidden;
}

@media (max-width: 720px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }

  .dialog-body__full {
    grid-column: auto;
  }

  .dialog-header,
  .dialog-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
