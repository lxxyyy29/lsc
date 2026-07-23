<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { logoutWeb } from '../api/auth'
import { getWebSession, type SystemMenuNode } from '../auth/session'

interface NavLeafItem {
  key: string
  to: string
  label: string
  icon?: string
}

interface NavGroupItem {
  key: string
  label: string
  icon?: string
  children: NavLeafItem[]
}

type NavigationItem = NavLeafItem | NavGroupItem

const router = useRouter()
const currentRoutePath = computed(() => router?.currentRoute?.value?.path ?? '')
const session = computed(() => getWebSession())

function toNavLeaf(node: SystemMenuNode): NavLeafItem | null {
  if (node.permissionType !== 'MENU' || node.status !== 'ACTIVE' || !node.path) {
    return null
  }

  return {
    key: node.permissionCode,
    to: node.path.startsWith('/') ? `/v2${node.path}` : `/v2/${node.path}`,
    label: node.permissionName,
    icon: node.icon
  }
}

function flattenNavigationLeaves(items: NavigationItem[]): NavLeafItem[] {
  return items.flatMap((item) => ('children' in item ? item.children : [item]))
}

function toNavigationItems(nodes: SystemMenuNode[]): NavigationItem[] {
  const items: NavigationItem[] = []

  for (const node of nodes) {
    if (node.permissionType === 'CATALOG') {
      const children = flattenNavigationLeaves(toNavigationItems(node.children))
      if (children.length > 0) {
        items.push({
          key: node.permissionCode,
          label: node.permissionName,
          icon: node.icon,
          children
        })
      }
      continue
    }

    const leaf = toNavLeaf(node)
    if (leaf) {
      items.push(leaf)
      continue
    }

    items.push(...flattenNavigationLeaves(toNavigationItems(node.children)))
  }

  return items
}

const navItems = computed(() => toNavigationItems(session.value?.menuTree ?? []))
const expandedGroups = ref<string[]>([])

watch(
  navItems,
  (items) => {
    const groupKeys = items.filter((item): item is NavGroupItem => 'children' in item).map((item) => item.key)
    expandedGroups.value = groupKeys
  },
  { immediate: true }
)

function isNavGroup(item: NavigationItem): item is NavGroupItem {
  return 'children' in item
}

function isNavGroupActive(item: NavigationItem) {
  if (isNavGroup(item)) {
    return item.children.some((child) => currentRoutePath.value.startsWith(child.to))
  }
  return currentRoutePath.value === item.to
}

function isGroupExpanded(item: NavGroupItem) {
  return expandedGroups.value.includes(item.key)
}

function toggleGroup(item: NavGroupItem) {
  if (expandedGroups.value.includes(item.key)) {
    expandedGroups.value = expandedGroups.value.filter((value) => value !== item.key)
    return
  }
  expandedGroups.value = [...expandedGroups.value, item.key]
}

async function handleLogout() {
  await logoutWeb()
  await router.push('/login')
}
</script>

<template>
  <div class="admin-shell-v2">
    <header class="admin-shell-v2__header">
      <div class="admin-shell-v2__brand">
        <div class="admin-shell-v2__logo">
          <span class="admin-shell-v2__logo-icon">智</span>
        </div>
        <div>
          <h1 class="admin-shell-v2__title">拔蛟窝智慧社区综合治理平台</h1>
          <p class="admin-shell-v2__subtitle">小网格 · 大治理 · 数字化</p>
        </div>
      </div>

      <div class="admin-shell-v2__actions">
        <div class="admin-shell-v2__user">
          <div class="admin-shell-v2__avatar">
            {{ session?.userName?.slice(0, 1) || '管' }}
          </div>
          <div class="admin-shell-v2__meta">
            <strong>{{ session?.userName || '管理员' }}</strong>
            <span>{{ session?.account || 'admin' }}</span>
          </div>
        </div>
        <button type="button" class="admin-shell-v2__logout" @click="handleLogout">退出</button>
      </div>
    </header>

    <div class="admin-shell-v2__body">
      <aside class="admin-shell-v2__sidebar">
        <nav aria-label="V2 主导航" class="admin-shell-v2__nav">
          <div v-for="item in navItems" :key="item.key" class="admin-shell-v2__nav-group">
            <template v-if="isNavGroup(item) && item.children.length">
              <button
                type="button"
                class="admin-shell-v2__nav-link admin-shell-v2__nav-toggle"
                :class="{ 'admin-shell-v2__nav-link--active': isNavGroupActive(item) }"
                @click="toggleGroup(item)"
              >
                <span class="admin-shell-v2__nav-icon">{{ item.icon || '◈' }}</span>
                <span class="admin-shell-v2__nav-label">{{ item.label }}</span>
                <span class="admin-shell-v2__nav-caret" :class="{ 'admin-shell-v2__nav-caret--expanded': isGroupExpanded(item) }">▾</span>
              </button>
              <div v-show="isGroupExpanded(item)" class="admin-shell-v2__subnav">
                <RouterLink
                  v-for="child in item.children"
                  :key="child.to"
                  :to="child.to"
                  class="admin-shell-v2__subnav-link"
                  :class="{ 'admin-shell-v2__subnav-link--active': currentRoutePath === child.to }"
                >
                  {{ child.label }}
                </RouterLink>
              </div>
            </template>
            <RouterLink
              v-else
              :to="isNavGroup(item) ? item.children[0]?.to ?? '/v2/dashboard' : item.to"
              class="admin-shell-v2__nav-link"
              :class="{ 'admin-shell-v2__nav-link--active': isNavGroupActive(item) }"
            >
              <span class="admin-shell-v2__nav-icon">{{ isNavGroup(item) ? item.icon || '◈' : item.icon || '◉' }}</span>
              <span class="admin-shell-v2__nav-label">{{ item.label }}</span>
            </RouterLink>
          </div>
        </nav>
      </aside>

      <main class="admin-shell-v2__main" role="main">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell-v2 {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--v2-bg-page);
  overflow: hidden;
}

.admin-shell-v2__header {
  height: var(--v2-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(180deg, #0d2035 0%, #0b1a29 100%);
  border-bottom: 1px solid var(--v2-border);
  flex-shrink: 0;
}

.admin-shell-v2__brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.admin-shell-v2__logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #5ea2ff 0%, #2d6edf 100%);
  display: grid;
  place-items: center;
  box-shadow: 0 4px 16px rgba(94, 162, 255, 0.28);
}

.admin-shell-v2__logo-icon {
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

.admin-shell-v2__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--v2-text-primary);
  letter-spacing: 1px;
}

.admin-shell-v2__subtitle {
  margin: 2px 0 0;
  font-size: 11px;
  color: var(--v2-text-tertiary);
  letter-spacing: 2px;
}

.admin-shell-v2__actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-shell-v2__user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  background: var(--v2-bg-card-strong);
  border: 1px solid var(--v2-border);
  border-radius: 999px;
}

.admin-shell-v2__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5ea2ff 0%, #2d6edf 100%);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.admin-shell-v2__meta {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.admin-shell-v2__meta strong {
  font-size: 13px;
  color: var(--v2-text-primary);
}

.admin-shell-v2__meta span {
  font-size: 11px;
  color: var(--v2-text-tertiary);
}

.admin-shell-v2__logout {
  border: 1px solid var(--v2-border);
  border-radius: 999px;
  padding: 8px 16px;
  background: transparent;
  color: var(--v2-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.admin-shell-v2__logout:hover {
  border-color: var(--v2-danger);
  color: var(--v2-danger);
  background: rgba(245, 108, 108, 0.08);
}

.admin-shell-v2__body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.admin-shell-v2__sidebar {
  width: var(--v2-sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: var(--v2-bg-card);
  border-right: 1px solid var(--v2-border);
  overflow-y: auto;
}

.admin-shell-v2__nav {
  display: flex;
  flex-direction: column;
  padding: 16px 12px;
  gap: 6px;
}

.admin-shell-v2__nav-group {
  display: flex;
  flex-direction: column;
}

.admin-shell-v2__nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 14px;
  border-radius: var(--v2-radius-md);
  color: var(--v2-text-secondary);
  text-decoration: none;
  transition: all 0.2s ease;
  background: transparent;
  border: none;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.admin-shell-v2__nav-link:hover {
  background: var(--v2-bg-hover);
  color: var(--v2-text-primary);
}

.admin-shell-v2__nav-link--active {
  background: linear-gradient(135deg, rgba(94, 162, 255, 0.22) 0%, rgba(45, 110, 223, 0.14) 100%);
  color: var(--v2-primary-light);
  box-shadow: inset 2px 0 0 var(--v2-primary);
}

.admin-shell-v2__nav-icon {
  width: 18px;
  text-align: center;
  font-size: 13px;
}

.admin-shell-v2__nav-label {
  flex: 1;
  font-size: 14px;
}

.admin-shell-v2__nav-toggle {
  justify-content: flex-start;
}

.admin-shell-v2__nav-caret {
  margin-left: auto;
  font-size: 12px;
  transition: transform 0.2s ease;
  color: var(--v2-text-tertiary);
}

.admin-shell-v2__nav-caret--expanded {
  transform: rotate(180deg);
}

.admin-shell-v2__subnav {
  display: flex;
  flex-direction: column;
  padding: 4px 0 4px 42px;
  gap: 2px;
}

.admin-shell-v2__subnav-link {
  padding: 10px 14px;
  border-radius: var(--v2-radius-sm);
  color: var(--v2-text-secondary);
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s ease;
}

.admin-shell-v2__subnav-link:hover {
  background: var(--v2-bg-hover);
  color: var(--v2-text-primary);
}

.admin-shell-v2__subnav-link.router-link-active,
.admin-shell-v2__subnav-link--active {
  background: var(--v2-bg-active);
  color: var(--v2-primary-light);
  font-weight: 600;
}

.admin-shell-v2__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 16px;
  overflow-y: auto;
  background: var(--v2-bg-page);
}

@media (max-width: 1080px) {
  .admin-shell-v2__sidebar {
    width: 220px;
  }
}
</style>
