import type { Router, RouteComponent, RouteRecordRaw } from 'vue-router'
import type { SystemMenuNode } from '../auth/session'

const v2ViewModules = import.meta.glob(['../views/v2/**/*.vue'])

/**
 * V2 固定路由：菜单树中不存在、但需要直接访问的详情/特殊页面。
 */
const v2StaticRoutes: RouteRecordRaw[] = [
  // TODO: 等 v2 视图文件生成后取消注释
  // {
  //   path: 'events/:id',
  //   name: 'v2-event-detail',
  //   component: () => import('../views/v2/event/EventDetailViewV2.vue'),
  //   meta: {
  //     permission: 'menu:event:list',
  //     dynamicMenu: false
  //   }
  // },
  // {
  //   path: 'big-screen',
  //   name: 'v2-big-screen',
  //   component: () => import('../views/v2/bigscreen/BigScreenViewV2.vue'),
  //   meta: {
  //     permission: 'menu:big-screen:view',
  //     fullscreen: true,
  //     dynamicMenu: false
  //   }
  // }
]

const v2ComponentAliases: Record<string, string> = {
  'v2/dashboard/DashboardViewV2': 'dashboard/DashboardViewV2',
  'v2/community/GridViewV2': 'community/GridViewV2',
  'v2/community/PopulationListViewV2': 'community/PopulationListViewV2',
  'v2/community/BuildingListViewV2': 'community/BuildingListViewV2',
  'v2/community/PlaceListViewV2': 'community/PlaceListViewV2',
  'v2/community/OrgMemberListViewV2': 'community/OrgMemberListViewV2',
  'v2/community/PatrolRecordListViewV2': 'community/PatrolRecordListViewV2',
  'v2/community/ResidentReportListViewV2': 'community/ResidentReportListViewV2',
  'v2/community/DashboardViewV2': 'community/DashboardViewV2',
  'v2/event/EventListViewV2': 'event/EventListViewV2',
  'v2/event/EventWorkflowViewV2': 'event/EventWorkflowViewV2',
  'v2/patrol/PatrolTaskListViewV2': 'patrol/PatrolTaskListViewV2',
  'v2/reports/AssessmentViewV2': 'reports/AssessmentViewV2',
  'v2/reports/LedgerViewV2': 'reports/LedgerViewV2',
  'v2/parking/ParkingViewV2': 'parking/ParkingViewV2',
  'v2/safety/SafetyViewV2': 'safety/SafetyViewV2',
  'v2/party/PartyViewV2': 'party/PartyViewV2',
  'v2/devices/DevicesViewV2': 'devices/DevicesViewV2'
}

function normalizeViewModuleKey(component: string) {
  const trimmed = component.trim().replace(/^\/?views\//, '').replace(/^\//, '').replace(/\.vue$/i, '')

  const aliased = v2ComponentAliases[trimmed]
  if (aliased) {
    return `../views/v2/${aliased}.vue`
  }

  if (trimmed.startsWith('v2/')) {
    return `../views/${trimmed}.vue`
  }

  return `../views/v2/${trimmed}.vue`
}

function normalizeRoutePath(path: string) {
  const trimmed = path.trim()
  if (!trimmed) {
    return ''
  }

  const withLeading = trimmed.startsWith('/') ? trimmed : `/${trimmed}`
  return withLeading.replace(/^\/v2/, '').replace(/^\//, '')
}

function buildRouteName(path: string) {
  const normalized = normalizeRoutePath(path)
  return `v2-${normalized.replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-+|-+$/g, '') || 'root'}`
}

function resolveV2View(component: string): (() => Promise<unknown>) | null {
  const key = normalizeViewModuleKey(component)
  return key in v2ViewModules ? v2ViewModules[key] : null
}

export function isV2MenuRouteLoadable(node: SystemMenuNode) {
  return node.permissionType === 'MENU'
    && node.status === 'ACTIVE'
    && normalizeRoutePath(node.path).length > 0
    && resolveV2View(node.component) !== null
}

function createV2MenuRoute(node: SystemMenuNode): RouteRecordRaw | null {
  const componentLoader = resolveV2View(node.component)
  if (!componentLoader) {
    return null
  }

  const childPath = normalizeRoutePath(node.path)

  return {
    path: childPath,
    name: buildRouteName(node.path),
    component: componentLoader as RouteComponent,
    meta: {
      permission: node.permissionCode,
      dynamicMenu: true,
      menuId: node.id
    }
  }
}

function flattenV2MenuRoutes(menuTree: SystemMenuNode[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  const walk = (nodes: SystemMenuNode[]) => {
    for (const node of nodes) {
      if (isV2MenuRouteLoadable(node)) {
        const route = createV2MenuRoute(node)
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

export function getFirstV2DynamicRoute(menuTree: SystemMenuNode[]) {
  const firstRoute = flattenV2MenuRoutes(menuTree)[0]
  if (!firstRoute) {
    return null
  }

  return `/v2/${String(firstRoute.path).replace(/^\//, '')}`
}

export function registerV2Routes(router: Router, menuTree: SystemMenuNode[]) {
  const registeredNames = new Set<string>()

  // 根据菜单树动态注册
  for (const route of flattenV2MenuRoutes(menuTree)) {
    const routeName = typeof route.name === 'string' ? route.name : ''
    if (!routeName || registeredNames.has(routeName) || router.hasRoute(routeName)) {
      continue
    }

    router.addRoute('v2-root', route)
    registeredNames.add(routeName)
  }

  // 注册固定详情/特殊路由
  for (const staticRoute of v2StaticRoutes) {
    const routeName = typeof staticRoute.name === 'string' ? staticRoute.name : ''
    if (!routeName || router.hasRoute(routeName)) {
      continue
    }

    router.addRoute('v2-root', staticRoute)
  }
}

export function getV2ViewModules() {
  return v2ViewModules as Record<string, () => Promise<unknown>>
}
