<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  getSystemRoleDetail,
  saveSystemRolePermissions,
  type SystemRole,
  type SystemRoleDetail
} from '../../api/system-role'
import { listSystemPermissionTree, type SystemPermission } from '../../api/system-permission'
import SystemDialog from './SystemDialog.vue'

interface PermissionGroup {
  id: string
  title: string
  subtitle: string
  permissions: SystemPermission[]
}

interface FlatPermission extends SystemPermission {
  _level: number
}

const props = defineProps<{
  open: boolean
  role?: SystemRole | null
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

const loading = ref(false)
const saving = ref(false)
const roleDetail = ref<SystemRoleDetail | null>(null)
const permissionTree = ref<SystemPermission[]>([])
const selectedPermissionIds = ref<number[]>([])
const activeTab = ref<'navigation' | 'actions' | 'advanced'>('navigation')

const title = computed(() => (props.role ? `分配权限 - ${props.role.roleName}` : '分配权限'))
const selectedPermissionIdSet = computed(() => new Set(selectedPermissionIds.value))
const allPermissions = computed(() => flattenPermissionTree(permissionTree.value))
const navigationPermissions = computed(() =>
  flattenNavigationPermissions(permissionTree.value, 0)
)
const actionGroups = computed<PermissionGroup[]>(() =>
  allPermissions.value
    .filter(
      (item) =>
        item.clientType === 'WEB' &&
        item.status === 'ACTIVE' &&
        ['CATALOG', 'MENU'].includes(item.permissionType)
    )
    .map((menu) => ({
      id: String(menu.id),
      title: menu.permissionName,
      subtitle: menu.path || menu.permissionCode,
      permissions: allPermissions.value.filter(
        (item) => item.parentId === menu.id && item.status === 'ACTIVE' && item.permissionType === 'BUTTON'
      )
    }))
    .filter((group) => group.permissions.length > 0)
)
const advancedGroups = computed<PermissionGroup[]>(() => {
  const h5Permissions = allPermissions.value.filter((item) => item.clientType === 'H5' && item.status === 'ACTIVE')
  const apiPermissions = allPermissions.value.filter((item) => item.permissionType === 'API' && item.status === 'ACTIVE')
  const disabledPermissions = allPermissions.value.filter((item) => item.status !== 'ACTIVE')

  return [
    {
      id: 'h5',
      title: 'H5 权限',
      subtitle: '移动端菜单、按钮和接口',
      permissions: h5Permissions
    },
    {
      id: 'api',
      title: '接口权限',
      subtitle: '后端 API 访问控制',
      permissions: apiPermissions
    },
    {
      id: 'disabled',
      title: '停用/历史权限',
      subtitle: '保留给历史角色或兼容逻辑',
      permissions: disabledPermissions
    }
  ].filter((group) => group.permissions.length > 0)
})
const selectedCount = computed(() => selectedPermissionIds.value.length)

watch(
  () => [props.open, props.role?.id],
  async ([open, roleId]) => {
    if (!open || typeof roleId !== 'number') return
    activeTab.value = 'navigation'
    await loadData(roleId)
  },
  { immediate: true }
)

function flattenPermissionTree(items: SystemPermission[]): SystemPermission[] {
  return items.flatMap((item) => [item, ...(item.children ? flattenPermissionTree(item.children) : [])])
}

function flattenNavigationPermissions(items: SystemPermission[], level: number): FlatPermission[] {
  return items.flatMap((item) => {
    const children = flattenNavigationPermissions(item.children ?? [], level + 1)
    if (
      item.clientType !== 'WEB' ||
      item.status !== 'ACTIVE' ||
      !['CATALOG', 'MENU'].includes(item.permissionType)
    ) {
      return children
    }

    return [{ ...item, _level: level }, ...children]
  })
}

async function loadData(roleId: number) {
  loading.value = true
  try {
    const [detail, permissions] = await Promise.all([
      getSystemRoleDetail(roleId),
      listSystemPermissionTree()
    ])
    roleDetail.value = detail
    permissionTree.value = permissions
    selectedPermissionIds.value = [...detail.permissionIds]
  } finally {
    loading.value = false
  }
}

function togglePermission(id: number) {
  if (selectedPermissionIdSet.value.has(id)) {
    selectedPermissionIds.value = selectedPermissionIds.value.filter((item) => item !== id)
    return
  }
  selectedPermissionIds.value = [...selectedPermissionIds.value, id]
}

function toggleGroup(group: PermissionGroup, checked: boolean) {
  const ids = new Set(selectedPermissionIds.value)
  for (const item of group.permissions) {
    if (checked) {
      ids.add(item.id)
    } else {
      ids.delete(item.id)
    }
  }
  selectedPermissionIds.value = [...ids]
}

function groupCheckedState(group: PermissionGroup) {
  const selected = group.permissions.filter((item) => selectedPermissionIdSet.value.has(item.id)).length
  return {
    checked: selected > 0 && selected === group.permissions.length,
    indeterminate: selected > 0 && selected < group.permissions.length
  }
}

function permissionMeta(item: SystemPermission) {
  if (item.permissionType === 'CATALOG') return '目录'
  if (item.permissionType === 'MENU') return item.path || '菜单'
  if (item.permissionType === 'BUTTON') return '页面操作'
  return item.path || '接口'
}

async function handleSave() {
  if (!props.role) return
  saving.value = true
  try {
    await saveSystemRolePermissions(props.role.id, { permissionIds: selectedPermissionIds.value })
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <SystemDialog :open="open" :title="title" subtitle="角色授权" panel-class="role-permission-dialog" @close="emit('close')">
    <div v-if="loading" class="role-permission-dialog__state">正在加载角色权限...</div>

    <div v-else class="role-permission-dialog__body">
      <aside class="permission-summary">
        <div>
          <p>已选择</p>
          <strong>{{ selectedCount }}</strong>
          <span>项权限</span>
        </div>
        <button
          type="button"
          class="permission-tab"
          :class="{ 'permission-tab--active': activeTab === 'navigation' }"
          @click="activeTab = 'navigation'"
        >
          导航权限
        </button>
        <button
          type="button"
          class="permission-tab"
          :class="{ 'permission-tab--active': activeTab === 'actions' }"
          @click="activeTab = 'actions'"
        >
          页面操作
        </button>
        <button
          type="button"
          class="permission-tab"
          :class="{ 'permission-tab--active': activeTab === 'advanced' }"
          @click="activeTab = 'advanced'"
        >
          高级权限
        </button>
      </aside>

      <section v-if="activeTab === 'navigation'" class="permission-panel">
        <header class="permission-panel__header">
          <div>
            <p>导航权限</p>
            <h4>控制左侧菜单和页面入口</h4>
          </div>
        </header>

        <div class="permission-card-grid">
          <label
            v-for="item in navigationPermissions"
            :key="item.id"
            class="permission-card"
            :style="{ paddingLeft: `${12 + item._level * 20}px` }"
          >
            <input
              :checked="selectedPermissionIdSet.has(item.id)"
              type="checkbox"
              :aria-label="item.permissionName"
              @change="togglePermission(item.id)"
            />
            <span>{{ permissionMeta(item) }}</span>
            <strong>{{ item.permissionName }}</strong>
          </label>
        </div>
      </section>

      <section v-else-if="activeTab === 'actions'" class="permission-panel">
        <header class="permission-panel__header">
          <div>
            <p>页面操作</p>
            <h4>按页面分配新建、编辑、删除等按钮</h4>
          </div>
        </header>

        <div class="permission-group-list">
          <section v-for="group in actionGroups" :key="group.id" class="permission-group">
            <header>
              <label>
                <input
                  type="checkbox"
                  :checked="groupCheckedState(group).checked"
                  :indeterminate="groupCheckedState(group).indeterminate"
                  @change="toggleGroup(group, ($event.target as HTMLInputElement).checked)"
                />
                <strong>{{ group.title }}</strong>
              </label>
              <span>{{ group.subtitle }}</span>
            </header>
            <div class="permission-chip-grid">
              <label v-for="item in group.permissions" :key="item.id" class="permission-chip-check">
                <input
                  :checked="selectedPermissionIdSet.has(item.id)"
                  type="checkbox"
                  :aria-label="item.permissionName"
                  @change="togglePermission(item.id)"
                />
                <span>{{ item.permissionName }}</span>
              </label>
            </div>
          </section>
        </div>
      </section>

      <section v-else class="permission-panel">
        <header class="permission-panel__header">
          <div>
            <p>高级权限</p>
            <h4>H5、接口和历史权限</h4>
          </div>
        </header>

        <div class="permission-group-list">
          <section v-for="group in advancedGroups" :key="group.id" class="permission-group">
            <header>
              <label>
                <input
                  type="checkbox"
                  :checked="groupCheckedState(group).checked"
                  :indeterminate="groupCheckedState(group).indeterminate"
                  @change="toggleGroup(group, ($event.target as HTMLInputElement).checked)"
                />
                <strong>{{ group.title }}</strong>
              </label>
              <span>{{ group.subtitle }}</span>
            </header>
            <div class="advanced-list">
              <label v-for="item in group.permissions" :key="item.id" class="advanced-item">
                <input
                  :checked="selectedPermissionIdSet.has(item.id)"
                  type="checkbox"
                  :aria-label="item.permissionName"
                  @change="togglePermission(item.id)"
                />
                <strong>{{ item.permissionName }}</strong>
                <span>{{ permissionMeta(item) }}</span>
              </label>
            </div>
          </section>
        </div>
      </section>
    </div>

    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="emit('close')">取消</button>
      <button type="button" class="action-button" :disabled="saving" @click="handleSave">
        {{ saving ? '保存中...' : '保存' }}
      </button>
    </template>
  </SystemDialog>
</template>

<style scoped>
@import '../../views/admin-shared.css';

:deep(.role-permission-dialog) {
  width: min(1120px, 100%) !important;
}

.role-permission-dialog__body {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.role-permission-dialog__state {
  margin: 0;
  color: rgba(205, 222, 248, 0.78);
}

.permission-summary,
.permission-panel,
.permission-group {
  border: 1px solid rgba(125, 163, 220, 0.18);
  border-radius: 10px;
  background: rgba(7, 20, 33, 0.42);
}

.permission-summary {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.permission-summary p,
.permission-summary span,
.permission-panel__header p,
.permission-group header > span,
.advanced-item span {
  margin: 0;
  color: rgba(205, 222, 248, 0.72);
}

.permission-summary strong {
  display: block;
  margin-top: 4px;
  color: #eef5ff;
  font-size: 28px;
}

.permission-tab {
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 8px;
  padding: 10px 12px;
  color: #eef5ff;
  text-align: left;
  background: rgba(12, 29, 48, 0.82);
  cursor: pointer;
}

.permission-tab--active,
.permission-tab:hover {
  border-color: rgba(115, 235, 255, 0.45);
  background: rgba(35, 160, 250, 0.18);
}

.permission-panel {
  display: grid;
  gap: 14px;
  padding: 14px;
}

.permission-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.permission-panel__header h4 {
  margin: 4px 0 0;
  color: #eef5ff;
}

.permission-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.permission-card,
.permission-chip-check,
.advanced-item {
  display: grid;
  gap: 4px 10px;
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 8px;
  padding: 10px 12px;
  color: #eef5ff;
  background: rgba(12, 29, 48, 0.82);
  cursor: pointer;
}

.permission-card {
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
}

.permission-card input {
  grid-row: 1 / 3;
}

.permission-card span,
.permission-card strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-card span {
  color: rgba(115, 235, 255, 0.78);
  font-size: 12px;
}

.permission-group-list {
  display: grid;
  gap: 12px;
}

.permission-group {
  display: grid;
  gap: 12px;
  padding: 12px;
}

.permission-group header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.permission-group header label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #eef5ff;
}

.permission-chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-chip-check {
  display: inline-flex;
  align-items: center;
  padding: 8px 10px;
}

.advanced-list {
  display: grid;
  gap: 8px;
}

.advanced-item {
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
}

.advanced-item strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .role-permission-dialog__body {
    grid-template-columns: 1fr;
  }

  .permission-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
