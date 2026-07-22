<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  getSystemRoleDetail,
  type SystemRole,
  type SystemRoleSavePayload,
  type SystemRoleStatus
} from '../../api/system-role'
import { listSystemPermissionTree, type SystemPermission } from '../../api/system-permission'
import SystemDialog from './SystemDialog.vue'

interface FlatPermission extends SystemPermission {
  _level: number
}

const props = defineProps<{
  open: boolean
  role?: SystemRole | null
}>()

const emit = defineEmits<{
  close: []
  save: [payload: SystemRoleSavePayload, permissionIds: number[]]
}>()

const form = reactive<SystemRoleSavePayload>({
  roleCode: '',
  roleName: '',
  status: 'ACTIVE',
  remark: ''
})

const loadingPermissions = ref(false)
const permissionTree = ref<SystemPermission[]>([])
const selectedMenuPermissionIds = ref<number[]>([])
const hiddenPermissionIds = ref<number[]>([])
const expandedIds = ref(new Set<number>())

const title = computed(() => (props.role ? '编辑角色' : '新增角色'))
const selectedMenuPermissionIdSet = computed(() => new Set(selectedMenuPermissionIds.value))
const allPermissions = computed(() => flattenPermissions(permissionTree.value, 0))

// 角色配置展示 Web/H5 两端所有目录和菜单，停用菜单也要展示，方便从角色中取消引用后再删除。
const menuPermissions = computed(() =>
  allPermissions.value.filter((item) => ['CATALOG', 'MENU'].includes(item.permissionType))
)
const selectedMenuCount = computed(() =>
  menuPermissions.value.filter((item) => selectedMenuPermissionIdSet.value.has(item.id)).length
)

watch(
  () => [props.open, props.role?.id],
  async () => {
    if (!props.open) return

    form.id = props.role?.id
    form.roleCode = props.role?.roleCode ?? ''
    form.roleName = props.role?.roleName ?? ''
    form.status = props.role?.status ?? 'ACTIVE'
    form.remark = props.role?.remark ?? ''
    selectedMenuPermissionIds.value = []
    hiddenPermissionIds.value = []
    expandedIds.value = new Set<number>()

    await loadPermissions()
  },
  { immediate: true }
)

function flattenPermissions(items: SystemPermission[], level: number): FlatPermission[] {
  return items.flatMap((item) => {
    const children = flattenPermissions(item.children ?? [], level + 1)
    return [{ ...item, _level: level }, ...children]
  })
}

async function loadPermissions() {
  loadingPermissions.value = true
  try {
    const [permissions, detail] = await Promise.all([
      listSystemPermissionTree(),
      props.role?.id ? getSystemRoleDetail(props.role.id) : Promise.resolve(null)
    ])
    permissionTree.value = permissions
    expandCatalogs(permissions)

    const flattenedPermissions = flattenPermissions(permissions, 0)
    const menuIds = new Set(flattenedPermissions
      .filter((item) => ['CATALOG', 'MENU'].includes(item.permissionType))
      .map((item) => item.id))
    const allPermissionIds = new Set(flattenedPermissions.map((item) => item.id))
    const grantedIds = detail?.permissionIds ?? []

    selectedMenuPermissionIds.value = grantedIds.filter((id) => menuIds.has(id))
    hiddenPermissionIds.value = grantedIds.filter((id) => !allPermissionIds.has(id))
  } finally {
    loadingPermissions.value = false
  }
}

function expandCatalogs(items: SystemPermission[]) {
  for (const item of items) {
    if (item.permissionType === 'CATALOG') {
      expandedIds.value.add(item.id)
    }
    expandCatalogs(item.children ?? [])
  }
}

function directMenuChildren(parentId: number) {
  const parentIndex = menuPermissions.value.findIndex((item) => item.id === parentId)
  if (parentIndex < 0) return []

  const parent = menuPermissions.value[parentIndex]
  const children: FlatPermission[] = []
  for (const item of menuPermissions.value.slice(parentIndex + 1)) {
    if (item._level <= parent._level) break
    if (item._level === parent._level + 1) {
      children.push(item)
    }
  }
  return children
}

function descendantIds(permissionId: number, source = menuPermissions.value) {
  const parentIndex = source.findIndex((item) => item.id === permissionId)
  if (parentIndex < 0) return [permissionId]

  const parent = source[parentIndex]
  const ids = [permissionId]
  for (const item of source.slice(parentIndex + 1)) {
    if (item._level <= parent._level) break
    ids.push(item.id)
  }
  return ids
}

function visibleMenuPermissions() {
  const result: FlatPermission[] = []
  for (const item of menuPermissions.value) {
    const parentChain = getParentChain(item)
    if (parentChain.every((parent) => expandedIds.value.has(parent.id))) {
      result.push(item)
    }
  }
  return result
}

function getParentChain(item: FlatPermission) {
  const chain: FlatPermission[] = []
  let parentId = item.parentId
  while (typeof parentId === 'number') {
    const parent = menuPermissions.value.find((candidate) => candidate.id === parentId)
    if (!parent) break
    chain.unshift(parent)
    parentId = parent.parentId
  }
  return chain
}

function hasMenuChildren(item: FlatPermission) {
  return directMenuChildren(item.id).length > 0
}

function toggleExpand(item: FlatPermission) {
  const next = new Set(expandedIds.value)
  if (next.has(item.id)) {
    next.delete(item.id)
  } else {
    next.add(item.id)
  }
  expandedIds.value = next
}

function toggleMenuPermission(item: FlatPermission, checked: boolean) {
  const ids = new Set(selectedMenuPermissionIds.value)
  if (checked) {
    for (const parent of getParentChain(item)) {
      ids.add(parent.id)
    }
    for (const id of descendantIds(item.id)) {
      ids.add(id)
    }
  } else {
    for (const id of descendantIds(item.id)) {
      ids.delete(id)
    }
  }
  selectedMenuPermissionIds.value = [...ids]
}

function toggleAll(checked: boolean) {
  selectedMenuPermissionIds.value = checked ? menuPermissions.value.map((item) => item.id) : []
}

function permissionTypeLabel(type: SystemPermission['permissionType']) {
  return type === 'CATALOG' ? '目录' : '菜单'
}

function clientTypeLabel(clientType: SystemPermission['clientType']) {
  return clientType === 'H5' ? 'H5' : 'Web'
}

function statusLabel(status: string) {
  return status === 'ACTIVE' ? '启用' : '停用'
}

function collectSavePermissionIds() {
  const ids = new Set(hiddenPermissionIds.value)

  // 角色授权只提交目录/菜单权限；新增接口不需要重新给角色补权限。
  for (const id of selectedMenuPermissionIds.value) {
    ids.add(id)
  }

  return [...ids]
}

function handleSave() {
  emit('save', {
    id: form.id,
    roleCode: form.roleCode.trim(),
    roleName: form.roleName.trim(),
    status: form.status as SystemRoleStatus,
    remark: form.remark.trim()
  }, collectSavePermissionIds())
}
</script>

<template>
  <SystemDialog
    :open="open"
    :title="title"
    subtitle="角色配置"
    panel-class="system-dialog__panel--wide role-edit-dialog"
    @close="emit('close')"
  >
    <div class="role-edit-layout">
      <section class="role-form-panel">
        <label class="field-stack">
          <span>角色名称</span>
          <input v-model="form.roleName" aria-label="角色名称" />
        </label>
        <label class="field-stack">
          <span>权限字符</span>
          <input v-model="form.roleCode" aria-label="权限字符" />
        </label>
        <label class="field-stack">
          <span>角色状态</span>
          <select v-model="form.status" aria-label="角色状态">
            <option value="ACTIVE">启用</option>
            <option value="DISABLED">停用</option>
          </select>
        </label>
        <label class="field-stack">
          <span>备注</span>
          <textarea v-model="form.remark" rows="5" aria-label="备注" />
        </label>
      </section>

      <section class="role-permission-panel">
        <header class="role-permission-panel__header">
          <div>
            <p>菜单权限</p>
            <h4>Web 和 H5 菜单统一配置</h4>
          </div>
          <label class="check-all">
            <input
              type="checkbox"
              :checked="menuPermissions.length > 0 && selectedMenuCount === menuPermissions.length"
              @change="toggleAll(($event.target as HTMLInputElement).checked)"
            />
            全选
          </label>
        </header>

        <div v-if="loadingPermissions" class="permission-state">正在加载菜单权限...</div>
        <div v-else-if="!menuPermissions.length" class="permission-state">暂无可分配菜单</div>
        <div v-else class="permission-tree-block">
          <div class="permission-block-title">
            <span>菜单树</span>
            <strong>{{ selectedMenuCount }}/{{ menuPermissions.length }}</strong>
          </div>
          <div class="permission-tree" aria-label="角色菜单权限树">
            <div
              v-for="item in visibleMenuPermissions()"
              :key="item.id"
              :class="['permission-row', `permission-row--${item.permissionType.toLowerCase()}`]"
              :style="{ paddingLeft: `${10 + item._level * 24}px` }"
            >
              <button
                v-if="hasMenuChildren(item)"
                type="button"
                class="tree-toggle"
                :aria-label="expandedIds.has(item.id) ? '收起菜单' : '展开菜单'"
                @click="toggleExpand(item)"
              >
                {{ expandedIds.has(item.id) ? '▾' : '▸' }}
              </button>
              <span v-else class="tree-toggle-spacer"></span>
              <label class="permission-check">
                <input
                  type="checkbox"
                  :checked="selectedMenuPermissionIdSet.has(item.id)"
                  @change="toggleMenuPermission(item, ($event.target as HTMLInputElement).checked)"
                />
                <span class="permission-row__name">{{ item.permissionName }}</span>
                <span class="permission-row__type">{{ permissionTypeLabel(item.permissionType) }}</span>
                <span class="permission-row__client">{{ clientTypeLabel(item.clientType) }}</span>
                <span
                  v-if="item.status !== 'ACTIVE'"
                  class="permission-row__status permission-row__status--disabled"
                >
                  {{ statusLabel(item.status) }}
                </span>
              </label>
            </div>
          </div>
        </div>
      </section>
    </div>

    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="emit('close')">取消</button>
      <button type="button" class="action-button" @click="handleSave">保存</button>
    </template>
  </SystemDialog>
</template>

<style scoped>
@import '../../views/admin-shared.css';

:deep(.role-edit-dialog) {
  width: min(980px, 100%) !important;
}

.role-edit-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(460px, 1fr);
  gap: 22px;
  align-items: start;
}

.role-form-panel,
.role-permission-panel {
  display: grid;
  gap: 16px;
}

.role-permission-panel {
  border-left: 1px solid rgba(125, 163, 220, 0.18);
  padding-left: 22px;
}

.role-permission-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.role-permission-panel__header p {
  margin: 0;
  color: rgba(205, 222, 248, 0.72);
}

.role-permission-panel__header h4 {
  margin: 4px 0 0;
  color: #eef5ff;
  font-size: 15px;
  font-weight: 600;
}

.check-all,
.permission-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #eef5ff;
}

.permission-tree-block {
  min-width: 0;
}

.permission-block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #eef5ff;
  font-weight: 600;
}

.permission-block-title strong {
  color: rgba(115, 235, 255, 0.9);
  font-size: 13px;
}

.permission-tree {
  display: grid;
  gap: 4px;
  height: min(52vh, 560px);
  overflow: auto;
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 8px;
  padding: 10px;
  background: rgba(3, 14, 25, 0.56);
}

.permission-row {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 34px;
  border-radius: 6px;
  color: #eef5ff;
}

.permission-row:hover {
  background: rgba(35, 160, 250, 0.12);
}

.tree-toggle,
.tree-toggle-spacer {
  width: 22px;
  height: 26px;
  flex: 0 0 auto;
}

.tree-toggle {
  border: 0;
  color: rgba(205, 222, 248, 0.88);
  background: transparent;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.permission-row__name {
  flex: 0 0 auto;
  max-width: 190px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-row__type,
.permission-row__client,
.permission-row__status {
  flex: 0 0 auto;
  min-width: 42px;
  border-radius: 4px;
  padding: 2px 6px;
  color: #93ecff;
  background: rgba(115, 235, 255, 0.1);
  font-size: 12px;
  text-align: center;
}

.permission-row__client {
  color: #c7d9f2;
  background: rgba(125, 163, 220, 0.14);
}

.permission-row__status--disabled {
  color: #ffb4b4;
  background: rgba(255, 120, 120, 0.14);
}

.permission-state {
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 8px;
  padding: 18px;
  color: rgba(205, 222, 248, 0.72);
  background: rgba(3, 14, 25, 0.56);
}

@media (max-width: 900px) {
  .role-edit-layout {
    grid-template-columns: 1fr;
  }

  .role-permission-panel {
    border-left: 0;
    border-top: 1px solid rgba(125, 163, 220, 0.18);
    padding-left: 0;
    padding-top: 18px;
  }
}
</style>
