<template>
  <PageContainer title="菜单管理">
    <div class="menu-workspace">
      <section class="panel menu-tree-panel">
        <header class="menu-panel-header">
          <div>
            <h3>菜单管理</h3>
          </div>
          <button class="action-button" type="button" @click="handleAdd">新增菜单</button>
        </header>

        <div class="menu-filter-row">
          <label>
            <span>菜单名称</span>
            <input v-model.trim="menuNameKeyword" placeholder="搜索菜单名称" aria-label="菜单名称搜索" />
          </label>
          <label>
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="ACTIVE">启用</option>
              <option value="ALL">全部</option>
              <option value="DISABLED">停用</option>
            </select>
          </label>
          <label>
            <span>客户端</span>
            <select v-model="clientTypeFilter">
              <option value="ALL">全部</option>
              <option value="WEB">WEB</option>
              <option value="H5">H5</option>
            </select>
          </label>
        </div>

        <div class="menu-tree-list" aria-label="菜单树">
          <div
            v-for="item in flatNavigationMenus"
            :key="item.id"
            class="menu-tree-item"
            :class="{ 'menu-tree-item--active': selectedMenu?.id === item.id }"
            :style="{ paddingLeft: `${16 + item._level * 22}px` }"
          >
            <button
              v-if="item._hasChildren"
              type="button"
              class="tree-toggle"
              :aria-label="expandedIds.has(item.id) ? '收起菜单' : '展开菜单'"
              @click="toggleExpand(item)"
            >
              {{ expandedIds.has(item.id) ? '▾' : '▸' }}
            </button>
            <span v-else class="tree-toggle-spacer"></span>
            <button type="button" class="menu-tree-item__content" @click="selectMenu(item)">
              <span class="menu-tree-item__type">{{ typeLabel(item.permissionType) }}</span>
              <span class="menu-tree-item__client">{{ clientTypeLabel(item.clientType) }}</span>
              <span class="menu-tree-item__main">
                <strong>{{ item.permissionName }}</strong>
                <small>{{ item.path || item.permissionCode }}</small>
              </span>
              <StatusTag :status="item.status" />
            </button>
          </div>
          <div v-if="!flatNavigationMenus.length" class="empty-state">暂无可显示菜单</div>
        </div>
      </section>

      <section class="panel menu-detail-panel">
        <template v-if="selectedMenu">
          <header class="menu-panel-header">
            <div>
              <h3>{{ selectedMenu.permissionName }}</h3>
            </div>
          </header>

          <div class="detail-edit-grid">
            <label>
              <span>上级菜单</span>
              <div class="inline-parent-select">
                <button type="button" class="inline-parent-select__trigger" @click="toggleInlineParentTree">
                  <span>{{ selectedInlineParentLabel }}</span>
                  <span class="inline-parent-select__caret" :class="{ 'inline-parent-select__caret--open': inlineParentTreeOpen }">▾</span>
                </button>
                <div v-if="inlineParentTreeOpen" class="inline-parent-select__panel">
                  <button
                    type="button"
                    class="inline-parent-select__option"
                    :class="{ 'inline-parent-select__option--active': detailForm.parentId === null }"
                    @click="selectInlineParent(null)"
                  >
                    <span class="inline-parent-select__spacer"></span>
                    <span>无（顶级）</span>
                  </button>
                  <div
                    v-for="item in inlineParentOptions"
                    :key="item.id"
                    class="inline-parent-select__option"
                    :class="{ 'inline-parent-select__option--active': detailForm.parentId === item.id }"
                    :style="{ paddingLeft: `${10 + item._level * 22}px` }"
                  >
                    <button
                      v-if="item._hasChildren"
                      type="button"
                      class="inline-parent-select__toggle"
                      :aria-label="inlineParentExpandedIds.has(item.id) ? '收起菜单' : '展开菜单'"
                      @click.stop="toggleInlineParentExpand(item)"
                    >
                      {{ inlineParentExpandedIds.has(item.id) ? '▾' : '▸' }}
                    </button>
                    <span v-else class="inline-parent-select__spacer"></span>
                    <button type="button" class="inline-parent-select__label" @click="selectInlineParent(item.id)">
                      {{ item.permissionName }}
                    </button>
                  </div>
                </div>
              </div>
            </label>
            <label>
              <span>菜单名称</span>
              <input v-model="detailForm.permissionName" />
            </label>
            <label>
              <span>权限标识</span>
              <input v-model="detailForm.permissionCode" />
            </label>
            <label>
              <span>类型</span>
              <select v-model="detailForm.permissionType">
                <option value="CATALOG">目录</option>
                <option value="MENU">菜单</option>
                <option value="BUTTON">按钮</option>
              </select>
            </label>
            <label>
              <span>客户端</span>
              <select v-model="detailForm.clientType">
                <option value="WEB">WEB</option>
                <option value="H5">H5</option>
              </select>
            </label>
            <label>
              <span>状态</span>
              <select v-model="detailForm.status">
                <option value="ACTIVE">启用</option>
                <option value="DISABLED">停用</option>
              </select>
            </label>
            <label>
              <span>路由地址</span>
              <input v-model="detailForm.path" />
            </label>
            <label>
              <span>路由组件</span>
              <input v-model="detailForm.component" />
            </label>
            <label>
              <span>图标</span>
              <input v-model="detailForm.icon" />
            </label>
            <label>
              <span>排序</span>
              <input v-model.number="detailForm.sortOrder" type="number" />
            </label>
            <label class="detail-edit-grid__full">
              <span>备注</span>
              <textarea v-model="detailForm.remark" rows="3" />
            </label>
          </div>

          <section class="permission-section">
            <header>
              <div>
                <p>页面操作权限</p>
                <h4>按钮权限</h4>
              </div>
              <span>{{ selectedButtons.length }} 项</span>
            </header>
            <div v-if="selectedButtons.length" class="permission-chip-list">
              <span v-for="item in selectedButtons" :key="item.id" class="permission-chip">
                {{ item.permissionName }}
              </span>
            </div>
            <div v-else class="muted-text">当前菜单没有配置按钮权限。</div>
          </section>

          <div class="detail-form-actions">
            <button class="action-button action-button--secondary" type="button" @click="handleDeleteSelected">删除菜单</button>
            <button class="action-button" type="button" @click="handleSaveSelected">保存修改</button>
          </div>
        </template>

        <div v-else class="empty-state menu-detail-empty">请选择左侧菜单查看详情。</div>
      </section>
    </div>

    <MenuEditDialog
      :open="editDialogOpen"
      :menu="editingMenu"
      :menu-tree="menus"
      @close="editDialogOpen = false"
      @save="onSaveMenu"
    />
    <SystemConfirmDialog
      :open="deleteDialogOpen"
      subtitle="系统配置 / 菜单管理"
      :message="`确定删除菜单「${deleteTarget?.permissionName ?? ''}」吗？`"
      description="删除前请确认该菜单没有子资源且未被角色引用。"
      :error="deleteError"
      :loading="deleteSaving"
      @close="closeDeleteDialog"
      @confirm="confirmDelete"
    />
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageContainer from '../../components/admin/PageContainer.vue'
import StatusTag from '../../components/admin/StatusTag.vue'
import MenuEditDialog from '../../components/system/MenuEditDialog.vue'
import SystemConfirmDialog from '../../components/system/SystemConfirmDialog.vue'
import {
  deleteSystemMenu,
  listSystemMenus,
  saveSystemMenu,
  type SystemMenu,
  type SystemMenuSavePayload
} from '../../api/system-menu'

type MenuStatusFilter = 'ACTIVE' | 'DISABLED' | 'ALL'
type MenuClientTypeFilter = 'WEB' | 'H5' | 'ALL'

interface FlatMenu extends SystemMenu {
  _level: number
  _hasChildren: boolean
}

interface InlineParentOption extends SystemMenu {
  _level: number
  _hasChildren: boolean
}

const menus = ref<SystemMenu[]>([])
const selectedMenu = ref<SystemMenu | null>(null)
const editDialogOpen = ref(false)
const editingMenu = ref<SystemMenu | null>(null)
const menuNameKeyword = ref('')
const statusFilter = ref<MenuStatusFilter>('ACTIVE')
const clientTypeFilter = ref<MenuClientTypeFilter>('ALL')
const expandedIds = ref(new Set<number>())
const inlineParentTreeOpen = ref(false)
const inlineParentExpandedIds = ref(new Set<number>())
const deleteDialogOpen = ref(false)
const deleteTarget = ref<SystemMenu | null>(null)
const deleteError = ref('')
const deleteSaving = ref(false)
const detailForm = reactive<SystemMenuSavePayload>({
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

const menuById = computed(() => {
  const map = new Map<number, SystemMenu>()
  for (const item of flattenAllMenus(menus.value)) {
    map.set(item.id, item)
  }
  return map
})

const flatNavigationMenus = computed(() =>
  flattenNavigationMenus(filterMenuTree(menus.value), 0)
)

const selectedMenuCanonical = computed(() =>
  selectedMenu.value ? menuById.value.get(selectedMenu.value.id) ?? selectedMenu.value : null
)
const selectedChildren = computed(() => selectedMenuCanonical.value?.children ?? [])
const selectedButtons = computed(() => selectedChildren.value.filter((item) => item.permissionType === 'BUTTON'))
const inlineParentOptions = computed(() => flattenInlineParentOptions(menus.value, 0))
const selectedInlineParentLabel = computed(() => {
  if (detailForm.parentId === null) return '无（顶级）'
  return flattenAllMenus(menus.value).find((item) => item.id === detailForm.parentId)?.permissionName ?? '请选择上级菜单'
})

watch(flatNavigationMenus, (items) => {
  const visibleMenu = selectedMenu.value
    ? items.find((item) => item.id === selectedMenu.value?.id)
    : null
  if (visibleMenu) {
    selectedMenu.value = menuById.value.get(visibleMenu.id) ?? visibleMenu
    return
  }
  selectedMenu.value = items[0] ?? null
})

watch([menuNameKeyword, statusFilter, clientTypeFilter], () => {
  expandedIds.value = collectExpandableIds(filterMenuTree(menus.value))
})

watch(selectedMenuCanonical, (menu) => {
  if (!menu) return
  detailForm.id = menu.id
  detailForm.parentId = menu.parentId ?? null
  detailForm.permissionCode = menu.permissionCode ?? ''
  detailForm.permissionName = menu.permissionName ?? ''
  detailForm.permissionType = menu.permissionType
  detailForm.clientType = menu.clientType
  detailForm.path = menu.path ?? ''
  detailForm.component = menu.component ?? ''
  detailForm.icon = menu.icon ?? ''
  detailForm.sortOrder = menu.sortOrder ?? 0
  detailForm.status = menu.status
  detailForm.remark = menu.remark ?? ''
  inlineParentTreeOpen.value = false
  inlineParentExpandedIds.value = collectInlineParentExpandableIds(menus.value)
}, { immediate: true })

function flattenAllMenus(items: SystemMenu[]): SystemMenu[] {
  return items.flatMap((item) => [item, ...flattenAllMenus(item.children ?? [])])
}

function filterMenuTree(items: SystemMenu[]): SystemMenu[] {
  return items
    .map((item) => ({
      ...item,
      children: filterMenuTree(item.children ?? [])
    }))
    .filter((item) => {
      const keyword = menuNameKeyword.value.trim().toLowerCase()
      if (clientTypeFilter.value !== 'ALL' && item.clientType !== clientTypeFilter.value) return false
      if (!['CATALOG', 'MENU'].includes(item.permissionType)) return false
      if (statusFilter.value !== 'ALL' && item.status !== statusFilter.value) return false
      if (item.children?.length) return true
      if (keyword && !item.permissionName.toLowerCase().includes(keyword)) return false
      return ['CATALOG', 'MENU'].includes(item.permissionType)
    })
}

function flattenNavigationMenus(items: SystemMenu[], level: number): FlatMenu[] {
  return items.flatMap((item) => {
    const children = item.children ?? []
    const isExpanded = expandedIds.value.has(item.id)
    return [
      { ...item, _level: level, _hasChildren: children.length > 0 },
      ...(isExpanded ? flattenNavigationMenus(children, level + 1) : [])
    ]
  })
}

function collectExpandableIds(items: SystemMenu[]) {
  const ids = new Set<number>()
  const walk = (nodes: SystemMenu[]) => {
    for (const item of nodes) {
      if (item.children?.length) {
        ids.add(item.id)
        walk(item.children)
      }
    }
  }
  walk(items)
  return ids
}

function isCurrentOrDescendant(item: SystemMenu, menuId: number): boolean {
  if (item.id === menuId) return true
  return (item.children ?? []).some((child) => isCurrentOrDescendant(child, menuId))
}

function flattenInlineParentOptions(items: SystemMenu[], level: number): InlineParentOption[] {
  return items.flatMap((item) => {
    if (selectedMenu.value && isCurrentOrDescendant(item, selectedMenu.value.id)) return []
    if (!['CATALOG', 'MENU'].includes(item.permissionType)) return []

    const children = (item.children ?? []).filter((child) => !selectedMenu.value || !isCurrentOrDescendant(child, selectedMenu.value.id))
    const menuChildren = children.filter((child) => ['CATALOG', 'MENU'].includes(child.permissionType))
    const isExpanded = inlineParentExpandedIds.value.has(item.id)
    return [
      { ...item, _level: level, _hasChildren: menuChildren.length > 0 },
      ...(isExpanded ? flattenInlineParentOptions(children, level + 1) : [])
    ]
  })
}

function collectInlineParentExpandableIds(items: SystemMenu[]) {
  const ids = new Set<number>()
  const walk = (nodes: SystemMenu[]) => {
    for (const item of nodes) {
      if (selectedMenu.value && isCurrentOrDescendant(item, selectedMenu.value.id)) continue
      const children = (item.children ?? []).filter((child) => !selectedMenu.value || !isCurrentOrDescendant(child, selectedMenu.value.id))
      if (children.some((child) => ['CATALOG', 'MENU'].includes(child.permissionType))) {
        ids.add(item.id)
        walk(children)
      }
    }
  }
  walk(items)
  return ids
}

function toggleInlineParentTree() {
  inlineParentTreeOpen.value = !inlineParentTreeOpen.value
}

function toggleInlineParentExpand(item: InlineParentOption) {
  const next = new Set(inlineParentExpandedIds.value)
  if (next.has(item.id)) {
    next.delete(item.id)
  } else {
    next.add(item.id)
  }
  inlineParentExpandedIds.value = next
}

function selectInlineParent(parentId: number | null) {
  detailForm.parentId = parentId
  inlineParentTreeOpen.value = false
}

function typeLabel(type: SystemMenu['permissionType']) {
  if (type === 'CATALOG') return '目录'
  if (type === 'MENU') return '菜单'
  if (type === 'BUTTON') return '按钮'
  return '接口'
}

function clientTypeLabel(clientType: SystemMenu['clientType']) {
  return clientType === 'H5' ? 'H5' : 'WEB'
}

async function loadMenus() {
  menus.value = await listSystemMenus()
  expandedIds.value = collectExpandableIds(filterMenuTree(menus.value))
}

onMounted(() => loadMenus())

function selectMenu(menu: SystemMenu) {
  selectedMenu.value = menuById.value.get(menu.id) ?? menu
}

function toggleExpand(menu: FlatMenu) {
  const next = new Set(expandedIds.value)
  if (next.has(menu.id)) {
    next.delete(menu.id)
  } else {
    next.add(menu.id)
  }
  expandedIds.value = next
}

function handleAdd() {
  editingMenu.value = null
  editDialogOpen.value = true
}

function handleDelete(menu: SystemMenu) {
  deleteTarget.value = menu
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
    await deleteSystemMenu(deleteTarget.value.id)
    ElMessage.success('菜单已删除')
    deleteDialogOpen.value = false
    deleteTarget.value = null
    await loadMenus()
  } catch (error) {
    deleteError.value = error instanceof Error ? error.message : '菜单删除失败'
  } finally {
    deleteSaving.value = false
  }
}

async function handleDeleteSelected() {
  if (!selectedMenuCanonical.value) return
  handleDelete(selectedMenuCanonical.value)
}

async function handleSaveSelected() {
  if (!selectedMenuCanonical.value) return
  try {
    await ElMessageBox.confirm(`确定要保存菜单「${detailForm.permissionName}」的修改吗？`, '保存菜单', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      type: 'warning',
      autofocus: false,
      closeOnClickModal: false
    })
  } catch {
    return
  }

  const saved = await saveSystemMenu({
    id: selectedMenuCanonical.value.id,
    parentId: detailForm.parentId,
    permissionCode: detailForm.permissionCode.trim(),
    permissionName: detailForm.permissionName.trim(),
    permissionType: detailForm.permissionType,
    clientType: detailForm.clientType,
    path: detailForm.path,
    component: detailForm.component,
    icon: detailForm.icon,
    sortOrder: detailForm.sortOrder,
    status: detailForm.status,
    remark: detailForm.remark
  })
  ElMessage.success('菜单已保存')
  await loadMenus()
  selectedMenu.value = menuById.value.get(saved.id) ?? saved
}

async function onSaveMenu(payload: SystemMenuSavePayload) {
  await saveSystemMenu(payload)
  editDialogOpen.value = false
  await loadMenus()
}
</script>

<style scoped>
@import '../admin-shared.css';

.menu-workspace {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 16px;
}

.menu-tree-panel,
.menu-detail-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.menu-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.menu-panel-header p,
.permission-section p,
.muted-text {
  margin: 0;
  color: rgba(205, 222, 248, 0.72);
}

.menu-panel-header h3,
.permission-section h4 {
  margin: 4px 0 0;
  color: #eef5ff;
}

.menu-filter-row {
  display: grid;
  grid-template-columns: minmax(120px, 132px) minmax(104px, 122px) minmax(104px, 122px);
  justify-content: start;
  gap: 10px;
}

.menu-filter-row label {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: rgba(205, 222, 248, 0.78);
  font-size: 13px;
}

.menu-filter-row select,
.menu-filter-row input {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  min-height: 36px;
  border: 1px solid rgba(64, 158, 255, 0.22);
  border-radius: 6px;
  padding: 0 10px;
  color: #eef5ff;
  background: rgba(5, 16, 28, 0.92);
}

.menu-tree-list {
  display: grid;
  gap: 2px;
  max-height: calc(100vh - 280px);
  overflow: auto;
  border: 1px solid rgba(115, 235, 255, 0.12);
  border-radius: 8px;
  padding: 8px;
  background: rgba(3, 14, 25, 0.42);
}

.menu-tree-item {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  border-radius: 6px;
  color: #eef5ff;
}

.menu-tree-item:hover,
.menu-tree-item--active {
  background: rgba(35, 160, 250, 0.16);
}

.tree-toggle,
.tree-toggle-spacer {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
}

.tree-toggle {
  border: 0;
  color: rgba(205, 222, 248, 0.78);
  background: transparent;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.tree-toggle:hover {
  color: #73ebff;
}

.menu-tree-item__content {
  display: grid;
  grid-template-columns: 42px 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  border: 0;
  padding: 6px 8px;
  text-align: left;
  color: inherit;
  background: transparent;
  cursor: pointer;
}

.menu-tree-item__type {
  border-radius: 999px;
  padding: 2px 0;
  color: #73ebff;
  background: rgba(35, 160, 250, 0.14);
  font-size: 12px;
  text-align: center;
}

.menu-tree-item__client {
  border-radius: 999px;
  padding: 2px 0;
  color: #c7d9f2;
  background: rgba(125, 163, 220, 0.14);
  font-size: 12px;
  text-align: center;
}

.menu-tree-item__main {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.menu-tree-item__main strong,
.menu-tree-item__main small {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.menu-tree-item__main small {
  color: rgba(205, 222, 248, 0.62);
}

.detail-edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-edit-grid label {
  display: grid;
  gap: 6px;
  color: rgba(205, 222, 248, 0.72);
  font-size: 12px;
}

.detail-edit-grid input,
.detail-edit-grid select,
.detail-edit-grid textarea {
  min-height: 40px;
  border: 1px solid rgba(64, 158, 255, 0.22);
  border-radius: 6px;
  padding: 0 12px;
  color: #eef5ff;
  background: rgba(5, 16, 28, 0.92);
}

.detail-edit-grid textarea {
  padding: 10px 12px;
  resize: vertical;
}

.detail-edit-grid__full {
  grid-column: 1 / -1;
}

.inline-parent-select {
  position: relative;
}

.inline-parent-select__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 40px;
  border: 1px solid rgba(64, 158, 255, 0.22);
  border-radius: 6px;
  padding: 0 12px;
  color: #eef5ff;
  background: rgba(5, 16, 28, 0.92);
  text-align: left;
  cursor: pointer;
}

.inline-parent-select__caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 20px;
  line-height: 1;
  transition: transform 0.2s ease;
}

.inline-parent-select__caret--open {
  transform: rotate(180deg);
}

.inline-parent-select__panel {
  position: absolute;
  z-index: 20;
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

.inline-parent-select__option {
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

button.inline-parent-select__option {
  width: 100%;
  cursor: pointer;
}

.inline-parent-select__option:hover,
.inline-parent-select__option--active {
  background: rgba(35, 160, 250, 0.22);
}

.inline-parent-select__toggle,
.inline-parent-select__spacer {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
}

.inline-parent-select__toggle {
  border: 0;
  color: rgba(205, 222, 248, 0.78);
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.inline-parent-select__label {
  flex: 1;
  min-width: 0;
  border: 0;
  padding: 0;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.detail-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid rgba(125, 163, 220, 0.14);
  padding-top: 16px;
}

.permission-section {
  display: grid;
  gap: 12px;
  border-top: 1px solid rgba(125, 163, 220, 0.14);
  padding-top: 16px;
}

.permission-section header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.permission-section header span {
  color: rgba(205, 222, 248, 0.72);
}

.permission-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-chip {
  border-radius: 999px;
  padding: 6px 10px;
  color: #dff7ff;
  background: rgba(35, 160, 250, 0.18);
}

.menu-detail-empty {
  margin: auto;
}

@media (max-width: 1080px) {
  .menu-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
