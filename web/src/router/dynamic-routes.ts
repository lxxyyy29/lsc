import type { Router, RouteComponent, RouteRecordRaw } from 'vue-router'
import type { SystemMenuNode } from '../auth/session'

const viewModules = import.meta.glob(['../views/**/*.vue', '!../views/auth/LoginView.vue'])

const dynamicMenuRoutes: RouteRecordRaw[] = [
  {
    path: 'community/grid',
    name: 'dynamic-menu-community-grid',
    component: () => import('../views/community/GridView.vue'),
    meta: {
      permission: 'menu:community:grid'
    }
  },
  {
    path: 'community/population',
    name: 'dynamic-menu-community-population',
    component: () => import('../views/community/PopulationListView.vue'),
    meta: {
      permission: 'menu:community:population'
    }
  },
  {
    path: 'community/buildings',
    name: 'dynamic-menu-community-buildings',
    component: () => import('../views/community/BuildingListView.vue'),
    meta: {
      permission: 'menu:community:building'
    }
  },
  {
    path: 'community/places',
    name: 'dynamic-menu-community-places',
    component: () => import('../views/community/PlaceListView.vue'),
    meta: {
      permission: 'menu:community:place'
    }
  },
  {
    path: 'community/org-members',
    name: 'dynamic-menu-community-org-members',
    component: () => import('../views/community/OrgMemberListView.vue'),
    meta: {
      permission: 'menu:community:org-member'
    }
  },
  {
    path: 'community/dashboard',
    name: 'dynamic-menu-community-dashboard',
    component: () => import('../views/community/DashboardView.vue'),
    meta: {
      permission: 'menu:community:dashboard'
    }
  },
  {
    path: 'big-screen',
    name: 'dynamic-menu-big-screen',
    component: () => import('../views/BigScreenView.vue'),
    meta: {
      permission: 'menu:big-screen:view',
      fullscreen: true
    }
  },
  {
    path: 'community/patrol-records',
    name: 'dynamic-menu-community-patrol-records',
    component: () => import('../views/community/PatrolRecordListView.vue'),
    meta: {
      permission: 'menu:community:patrol-record'
    }
  },
  {
    path: 'community/resident-reports',
    name: 'dynamic-menu-community-resident-reports',
    component: () => import('../views/community/ResidentReportListView.vue'),
    meta: {
      permission: 'menu:community:resident-report'
    }
  },
  {
    path: 'biz/ledger',
    name: 'dynamic-menu-biz-ledger',
    component: () => import('../views/biz/MerchantLedgerView.vue'),
    meta: {
      permission: 'menu:biz:ledger'
    }
  }
]

const dynamicDetailRoutes: RouteRecordRaw[] = [
  {
    path: 'events/:id',
    name: 'dynamic-detail-events-id',
    component: () => import('../views/event/EventDetailView.vue'),
    meta: {
      permission: 'menu:event:list'
    }
  },
  {
    path: 'audits/:id',
    name: 'dynamic-detail-audits-id',
    component: () => import('../views/audit/AuditDetailView.vue'),
    meta: {
      permission: 'menu:audit:list'
    }
  },
  {
    path: 'processes/:id/edit',
    name: 'dynamic-detail-processes-id-edit',
    component: () => import('../views/process/ProcessTemplateEditView.vue'),
    meta: {
      permission: 'menu:process:list'
    }
  },
  {
    path: 'work-orders/:id',
    name: 'dynamic-detail-work-orders-id',
    component: () => import('../views/workorder/WorkOrderDetailView.vue'),
    meta: {
      permission: 'menu:workorder:list'
    }
  },
  {
    path: 'patrol-tasks/:id',
    name: 'dynamic-detail-patrol-tasks-id',
    component: () => import('../views/patrol/PatrolTaskDetailView.vue'),
    meta: {
      permission: 'menu:patrol-task:list'
    }
  }
]

function normalizeViewModuleKey(component: string) {
  const trimmed = component.trim().replace(/^\/?views\//, '').replace(/^\//, '')
  return `../views/${trimmed.replace(/\.vue$/i, '')}.vue`
}

function normalizeRoutePath(path: string) {
  const trimmed = path.trim()
  if (!trimmed) {
    return ''
  }

  return trimmed.startsWith('/') ? trimmed : `/${trimmed}`
}

function toChildRoutePath(path: string) {
  return normalizeRoutePath(path).replace(/^\//, '')
}

function buildRouteName(path: string) {
  const normalized = normalizeRoutePath(path)
  return `dynamic-${normalized.replace(/^\//, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'root'}`
}

function resolveDynamicView(component: string): (() => Promise<unknown>) | null {
  const key = normalizeViewModuleKey(component)
  return key in viewModules ? viewModules[key] : null
}

export function isMenuRouteLoadable(node: SystemMenuNode) {
  return node.permissionType === 'MENU'
    && node.status === 'ACTIVE'
    && normalizeRoutePath(node.path).length > 0
    && resolveDynamicView(node.component) !== null
}

function createMenuRoute(node: SystemMenuNode): RouteRecordRaw | null {
  const componentLoader = resolveDynamicView(node.component)
  if (!componentLoader) {
    return null
  }

  const route: RouteRecordRaw = {
    path: toChildRoutePath(node.path),
    name: buildRouteName(node.path),
    component: componentLoader as RouteComponent,
    meta: {
      permission: node.permissionCode,
      dynamicMenu: true,
      menuId: node.id
    }
  }

  return route
}

function flattenMenuRoutes(menuTree: SystemMenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  const walk = (nodes: SystemMenuNode[]) => {
    for (const node of nodes) {
      if (isMenuRouteLoadable(node)) {
        const route = createMenuRoute(node)
        if (route) {
          routes.push(route)
        }
      }

      if (node.children.length > 0) {
        walk(node.children)
      }
    }
  }

  walk(menuTree)
  return routes
}

export function getFirstDynamicRoute(menuTree: SystemMenuNode[]) {
  const firstRoute = flattenMenuRoutes(menuTree)[0]
  if (!firstRoute) {
    return null
  }

  return `/${String(firstRoute.path).replace(/^\//, '')}`
}

export function registerDynamicRoutes(router: Router, menuTree: SystemMenuNode[]) {
  const registeredNames = new Set<string>()

  for (const route of flattenMenuRoutes(menuTree)) {
    const routeName = typeof route.name === 'string' ? route.name : ''
    if (!routeName || registeredNames.has(routeName) || router.hasRoute(routeName)) {
      continue
    }

    router.addRoute('admin-root', route)
    registeredNames.add(routeName)
  }

  for (const menuRoute of dynamicMenuRoutes) {
    const routeName = typeof menuRoute.name === 'string' ? menuRoute.name : ''
    if (!routeName || router.hasRoute(routeName)) {
      continue
    }
    router.addRoute('admin-root', menuRoute)
  }

  for (const detailRoute of dynamicDetailRoutes) {
    const routeName = typeof detailRoute.name === 'string' ? detailRoute.name : ''
    if (!routeName || router.hasRoute(routeName)) {
      continue
    }

    router.addRoute('admin-root', detailRoute)
  }
}

export function getDynamicViewModules() {
  return viewModules as Record<string, () => Promise<unknown>>
}
