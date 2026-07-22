<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { logoutWeb } from '../api/auth'
import { getWebSession, type SystemMenuNode } from '../auth/session'
import { isMenuRouteLoadable } from '../router/dynamic-routes'

interface NavLeafItem {
  key: string
  to: string
  label: string
}

interface NavGroupItem {
  key: string
  label: string
  children: NavLeafItem[]
}

type NavigationItem = NavLeafItem | NavGroupItem

const router = useRouter()
const currentRoutePath = computed(() => router?.currentRoute?.value?.path ?? '')
const session = computed(() => getWebSession())

function toNavLeaf(node: SystemMenuNode): NavLeafItem | null {
  if (!isMenuRouteLoadable(node)) {
    return null
  }

  return {
    key: node.permissionCode,
    to: node.path.startsWith('/') ? node.path : `/${node.path}`,
    label: node.permissionName
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
    expandedGroups.value = expandedGroups.value.filter((key) => groupKeys.includes(key))
    for (const key of groupKeys) {
      if (!expandedGroups.value.includes(key)) {
        expandedGroups.value.push(key)
      }
    }
  },
  { immediate: true }
)

function isNavGroup(item: NavigationItem): item is NavGroupItem {
  return 'children' in item
}

function getNavItemPath(item: NavigationItem) {
  return isNavGroup(item) ? item.children[0]?.to ?? '/' : item.to
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
  <div class="admin-shell-layout admin-shell">
    <header class="admin-shell__header">
      <div class="admin-shell__brand">
        <!-- Sci-fi banner elements using CSS polygons/gradients -->
        <div class="banner-bg-glow"></div>
        <div class="banner-center-plate">
          <div class="banner-center-plate__inner">
            <h1>居里智能低空巡检综合监管平台</h1>
          </div>
        </div>
      </div>
      
      <div class="admin-shell__account" aria-label="账户区">
        <div class="admin-shell__account-avatar" aria-hidden="true">
          {{ session?.userName?.slice(0, 1) || '管' }}
        </div>
        <div class="admin-shell__account-meta">
          <strong>{{ session?.userName || '管理员' }}</strong>
          <span>{{ session?.account || 'admin' }}</span>
        </div>
        <button type="button" class="admin-shell__logout" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <div class="admin-shell__body">
      <aside class="admin-shell__sidebar">
        <nav aria-label="主导航" class="admin-shell__nav">
          <div v-for="item in navItems" :key="item.key" class="admin-shell__nav-group">
            <template v-if="isNavGroup(item) && item.children.length">
              <button
                type="button"
                class="admin-shell__nav-link admin-shell__nav-toggle"
                :class="{ 'admin-shell__nav-link--active': isNavGroupActive(item) }"
                @click="toggleGroup(item)"
              >
                <span>{{ item.label }}</span>
                <span class="admin-shell__nav-caret" :class="{ 'admin-shell__nav-caret--expanded': isGroupExpanded(item) }">▾</span>
              </button>
              <div v-if="isGroupExpanded(item)" class="admin-shell__subnav">
                <RouterLink v-for="child in item.children" :key="child.to" :to="child.to" class="admin-shell__subnav-link">
                  {{ child.label }}
                </RouterLink>
              </div>
            </template>
            <RouterLink
              v-else
              :to="getNavItemPath(item)"
              class="admin-shell__nav-link"
              :class="{ 'admin-shell__nav-link--active': isNavGroupActive(item) }"
            >
              {{ item.label }}
            </RouterLink>
          </div>
        </nav>
      </aside>

      <main class="admin-shell__main" role="main">
        <section class="admin-shell__content">
          <RouterView />
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0b1a29; /* Matches the deep blue base of the screenshot */
  overflow: hidden;
}

.admin-shell__header {
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 24px;
  background: #061626;
  position: relative;
  z-index: 10;
  overflow: hidden;
}

.admin-shell__brand {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(180deg, #102d4f 0%, #1d518d 50%, #102d4f 100%);
  display: flex;
  justify-content: center;
  align-items: stretch;
  pointer-events: none;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.5);
}

.banner-bg-glow {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  width: 60%;
  height: 120px;
  background: radial-gradient(ellipse at center, rgba(63, 153, 255, 0.4) 0%, transparent 70%);
  z-index: 1;
}

/* Banner wings removed to use a single unified header background */

.banner-center-plate {
  position: relative;
  top: 0;
  height: 100%;
  width: 50%;
  min-width: 600px;
  z-index: 3;
  display: flex;
  justify-content: center;
  align-items: stretch;
  padding: 4px 15%;
}

.banner-center-plate__inner {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.banner-center-plate__inner h1 {
  margin: 0;
  font-size: 32px;
  font-weight: 800;
  letter-spacing: 4px;
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.6), 0 0 10px rgba(255, 255, 255, 0.5);
  font-style: italic;
}



.admin-shell__body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.admin-shell__sidebar {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #0f2745;
  border-right: 1px solid rgba(125, 163, 220, 0.16);
  overflow-y: auto;
}

.admin-shell__header-copy {
  display: grid;
  gap: 8px;
}


.admin-shell__nav {
  display: flex;
  flex-direction: column;
  padding: 16px 0;
}

.admin-shell__nav-group {
  display: flex;
  flex-direction: column;
}

.admin-shell__nav-link,
.admin-shell__subnav-link {
  display: block;
  color: rgba(235, 242, 255, 0.92);
  text-decoration: none;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.admin-shell__nav-link {
  font-size: 15px; /* Slightly larger for main menu items */
  padding: 16px 20px;
  border-left: 3px solid transparent;
  background: transparent;
}

.admin-shell__nav-link:hover {
  background: rgba(40, 115, 214, 0.15);
}

.admin-shell__nav-link.router-link-active,
.admin-shell__nav-link.router-link-exact-active,
.admin-shell__nav-link--active {
  background: #23a0fa; /* Approximating the solid bright blue */
  border-left: 3px solid #73ebff;
  color: #fff;
}

.admin-shell__nav-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  border: none;
  font: inherit;
  text-align: left;
}

.admin-shell__nav-caret {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  font-size: 20px;
  line-height: 1;
  transition: transform 0.2s ease;
}

.admin-shell__nav-caret--expanded {
  transform: rotate(180deg);
}

.admin-shell__nav-toggle span {
  pointer-events: none;
}

.admin-shell__subnav {
  display: flex;
  flex-direction: column;
  background: #0a1b2d;
}

.admin-shell__subnav-link {
  padding: 14px 20px 14px 44px;
  background: transparent;
  color: rgba(235, 242, 255, 0.92);
  font-size: 14px;
}

.admin-shell__subnav-link:hover {
  background: rgba(40, 115, 214, 0.15);
}

.admin-shell__subnav-link.router-link-active,
.admin-shell__subnav-link.router-link-exact-active {
  color: #73ebff;
  font-weight: 600;
}

.admin-shell__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* Important for flex children to allow inner scrolling/truncation */
}

.admin-shell__account {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 20;
}

.admin-shell__account-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: linear-gradient(135deg, #4a90ff 0%, #2d6edf 100%);
  color: #fff;
  font-weight: 700;
}

.admin-shell__account-meta {
  display: grid;
  gap: 2px;
}

.admin-shell__account-meta strong,
.admin-shell__account-meta span {
  display: block;
}

.admin-shell__account-meta strong {
  font-size: 14px;
  line-height: 1.2;
  color: #ffffff;
}

.admin-shell__account-meta span {
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
}

.admin-shell__logout {
  border: 0;
  border-radius: 999px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.15);
  color: #ffffff;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.admin-shell__logout:hover {
  background: rgba(255, 255, 255, 0.25);
}

.admin-shell__content {
  flex: 1;
  min-height: 0;
  padding: 16px;
  background: #0b1a29; /* Merge with the layout background */
  overflow-y: auto;
}

@media (max-width: 1080px) {
  .admin-shell__body {
    flex-direction: column;
  }

  .admin-shell__sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid rgba(125, 163, 220, 0.16);
  }

  .admin-shell__nav {
    flex-direction: row;
    overflow-x: auto;
    padding: 0;
  }

  .admin-shell__nav-link {
    padding: 12px 16px;
    border-left: none;
    border-bottom: 3px solid transparent;
  }

  .admin-shell__nav-link.router-link-active,
  .admin-shell__nav-link.router-link-exact-active,
  .admin-shell__nav-link--active {
    border-left: none;
    border-bottom: 3px solid #73ebff;
  }
}
</style>
