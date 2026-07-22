<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { SystemMenu, SystemMenuSavePayload, SystemMenuType, SystemMenuStatus } from '../../api/system-menu'
import SystemDialog from './SystemDialog.vue'

const props = defineProps<{
  open: boolean
  menu?: SystemMenu | null
  menuTree: SystemMenu[]
}>()

const emit = defineEmits<{
  close: []
  save: [payload: SystemMenuSavePayload]
}>()

const form = reactive<SystemMenuSavePayload>({
  parentId: null,
  permissionCode: '',
  permissionName: '',
  permissionType: 'MENU',
  clientType: 'WEB',
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  status: 'ACTIVE',
  remark: ''
})

interface ParentOption extends SystemMenu {
  _level: number
  _hasChildren: boolean
}

const title = computed(() => (props.menu ? '编辑菜单' : '新增菜单'))
const parentTreeOpen = ref(false)
const expandedIds = ref(new Set<number>())

const parentOptions = computed(() => flattenParentOptions(props.menuTree, 0))
const selectedParentLabel = computed(() => {
  if (form.parentId === null) return '无（顶级）'
  return parentOptions.value.find((item) => item.id === form.parentId)?.permissionName ?? '请选择上级菜单'
})

watch(
  () => [props.open, props.menu],
  () => {
    if (!props.open) return
    form.id = props.menu?.id
    form.parentId = props.menu?.parentId ?? null
    form.permissionCode = props.menu?.permissionCode ?? ''
    form.permissionName = props.menu?.permissionName ?? ''
    form.permissionType = props.menu?.permissionType ?? 'MENU'
    form.clientType = props.menu?.clientType ?? 'WEB'
    form.path = props.menu?.path ?? ''
    form.component = props.menu?.component ?? ''
    form.icon = props.menu?.icon ?? ''
    form.sortOrder = props.menu?.sortOrder ?? 0
    form.status = props.menu?.status ?? 'ACTIVE'
    form.remark = props.menu?.remark ?? ''
    parentTreeOpen.value = false
    expandedIds.value = collectExpandableIds(props.menuTree)
  },
  { immediate: true }
)

function flattenParentOptions(items: SystemMenu[], level: number): ParentOption[] {
  return items.flatMap((item) => {
    if (isCurrentOrDescendant(item)) return []
    if (!['CATALOG', 'MENU'].includes(item.permissionType)) return []

    const children = (item.children ?? []).filter((child) => !isCurrentOrDescendant(child))
    const isExpanded = expandedIds.value.has(item.id)
    return [
      { ...item, _level: level, _hasChildren: children.some((child) => ['CATALOG', 'MENU'].includes(child.permissionType)) },
      ...(isExpanded ? flattenParentOptions(children, level + 1) : [])
    ]
  })
}

function isCurrentOrDescendant(item: SystemMenu): boolean {
  if (!props.menu) return false
  if (item.id === props.menu.id) return true
  return (item.children ?? []).some((child) => isCurrentOrDescendant(child))
}

function collectExpandableIds(items: SystemMenu[]) {
  const ids = new Set<number>()
  const walk = (nodes: SystemMenu[]) => {
    for (const item of nodes) {
      if (isCurrentOrDescendant(item)) continue
      const children = (item.children ?? []).filter((child) => !isCurrentOrDescendant(child))
      if (children.some((child) => ['CATALOG', 'MENU'].includes(child.permissionType))) {
        ids.add(item.id)
        walk(children)
      }
    }
  }
  walk(items)
  return ids
}

function toggleParentTree() {
  parentTreeOpen.value = !parentTreeOpen.value
}

function toggleExpand(item: ParentOption) {
  const next = new Set(expandedIds.value)
  if (next.has(item.id)) {
    next.delete(item.id)
  } else {
    next.add(item.id)
  }
  expandedIds.value = next
}

function selectParent(parentId: number | null) {
  form.parentId = parentId
  parentTreeOpen.value = false
}

function handleSave() {
  emit('save', {
    id: form.id,
    parentId: form.parentId,
    permissionCode: form.permissionCode.trim(),
    permissionName: form.permissionName.trim(),
    permissionType: form.permissionType as SystemMenuType,
    clientType: form.clientType as 'WEB' | 'H5',
    path: form.path,
    component: form.component,
    icon: form.icon,
    sortOrder: form.sortOrder,
    status: form.status as SystemMenuStatus,
    remark: form.remark
  })
}
</script>

<template>
  <SystemDialog :open="open" :title="title" subtitle="菜单配置" @close="emit('close')">
    <div class="menu-edit-dialog__body">
      <label class="field-stack">
        <span>上级菜单</span>
        <div class="parent-tree-select">
          <button
            type="button"
            class="parent-tree-select__trigger"
            aria-label="上级菜单"
            @click="toggleParentTree"
          >
            <span>{{ selectedParentLabel }}</span>
            <span class="parent-tree-select__caret" :class="{ 'parent-tree-select__caret--open': parentTreeOpen }">▾</span>
          </button>
          <div v-if="parentTreeOpen" class="parent-tree-select__panel">
            <button
              type="button"
              class="parent-tree-select__option"
              :class="{ 'parent-tree-select__option--active': form.parentId === null }"
              @click="selectParent(null)"
            >
              <span class="parent-tree-select__spacer"></span>
              <span>无（顶级）</span>
            </button>
            <div
              v-for="item in parentOptions"
              :key="item.id"
              class="parent-tree-select__option"
              :class="{ 'parent-tree-select__option--active': form.parentId === item.id }"
              :style="{ paddingLeft: `${10 + item._level * 22}px` }"
            >
              <button
                v-if="item._hasChildren"
                type="button"
                class="parent-tree-select__toggle"
                :aria-label="expandedIds.has(item.id) ? '收起菜单' : '展开菜单'"
                @click.stop="toggleExpand(item)"
              >
                {{ expandedIds.has(item.id) ? '▾' : '▸' }}
              </button>
              <span v-else class="parent-tree-select__spacer"></span>
              <button type="button" class="parent-tree-select__label" @click="selectParent(item.id)">
                {{ item.permissionName }}
              </button>
            </div>
          </div>
        </div>
      </label>
      <label class="field-stack">
        <span>菜单名称</span>
        <input v-model="form.permissionName" aria-label="菜单名称" />
      </label>
      <label class="field-stack">
        <span>权限标识</span>
        <input v-model="form.permissionCode" aria-label="权限标识" />
      </label>
      <label class="field-stack">
        <span>类型</span>
        <select v-model="form.permissionType" aria-label="菜单类型">
          <option value="CATALOG">目录</option>
          <option value="MENU">菜单</option>
          <option value="BUTTON">按钮</option>
        </select>
      </label>
      <label class="field-stack">
        <span>终端</span>
        <select v-model="form.clientType" aria-label="终端类型">
          <option value="WEB">Web 管理端</option>
          <option value="H5">H5 移动端</option>
        </select>
      </label>
      <label class="field-stack">
        <span>路由路径</span>
        <input v-model="form.path" aria-label="路由路径" />
      </label>
      <label class="field-stack">
        <span>组件路径</span>
        <input v-model="form.component" aria-label="组件路径" />
      </label>
      <label class="field-stack">
        <span>图标</span>
        <input v-model="form.icon" aria-label="图标" />
      </label>
      <label class="field-stack">
        <span>排序</span>
        <input v-model.number="form.sortOrder" type="number" aria-label="排序" />
      </label>
      <label class="field-stack">
        <span>状态</span>
        <select v-model="form.status" aria-label="状态">
          <option value="ACTIVE">启用</option>
          <option value="DISABLED">停用</option>
        </select>
      </label>
      <label class="field-stack menu-edit-dialog__full">
        <span>备注</span>
        <textarea v-model="form.remark" rows="3" aria-label="备注" />
      </label>
    </div>

    <template #footer>
      <button type="button" class="action-button action-button--secondary" @click="emit('close')">取消</button>
      <button type="button" class="action-button" @click="handleSave">保存</button>
    </template>
  </SystemDialog>
</template>

<style scoped>
@import '../../views/admin-shared.css';

.menu-edit-dialog__body {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.menu-edit-dialog__full {
  grid-column: 1 / -1;
}

.parent-tree-select {
  position: relative;
}

.parent-tree-select__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 42px;
  border: 1px solid rgba(64, 158, 255, 0.24);
  border-radius: 6px;
  padding: 0 12px;
  color: #eef5ff;
  background: rgba(5, 16, 28, 0.92);
  text-align: left;
  cursor: pointer;
}

.parent-tree-select__caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 20px;
  transition: transform 0.2s ease;
}

.parent-tree-select__caret--open {
  transform: rotate(180deg);
}

.parent-tree-select__panel {
  position: absolute;
  z-index: 10;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  display: grid;
  gap: 2px;
  max-height: 320px;
  overflow: auto;
  border: 1px solid rgba(64, 158, 255, 0.32);
  border-radius: 6px;
  padding: 6px;
  background: #071421;
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.42);
}

.parent-tree-select__option {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  border: 0;
  border-radius: 5px;
  color: #eef5ff;
  background: transparent;
  text-align: left;
}

button.parent-tree-select__option {
  width: 100%;
  cursor: pointer;
}

.parent-tree-select__option:hover,
.parent-tree-select__option--active {
  background: rgba(35, 160, 250, 0.22);
}

.parent-tree-select__toggle,
.parent-tree-select__spacer {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
}

.parent-tree-select__toggle {
  border: 0;
  color: rgba(205, 222, 248, 0.78);
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.parent-tree-select__label {
  flex: 1;
  min-width: 0;
  border: 0;
  padding: 0;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

@media (max-width: 720px) {
  .menu-edit-dialog__body {
    grid-template-columns: 1fr;
  }
}
</style>
