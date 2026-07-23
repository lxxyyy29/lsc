import {
  createMemoryHistory,
  createRouter,
  createWebHistory,
  type RouteLocationNormalized,
  type RouteRecordRaw
} from 'vue-router'
import { h } from 'vue'
import LoginView from '../views/auth/LoginView.vue'
import AdminShellLayout from '../layouts/AdminShellLayout.vue'
import AdminShellLayoutV2 from '../layouts/AdminShellLayoutV2.vue'
import { hasMenuPermission } from '../auth/permissions'
import { getWebSession } from '../auth/session'
import { getFirstDynamicRoute, registerDynamicRoutes } from './dynamic-routes'
import { getFirstV2DynamicRoute, registerV2Routes } from './v2-routes'

function createPlaceholderView(title: string, description: string) {
  return {
    name: `${title.replace(/\s+/g, '')}View`,
    render() {
      return h('section', { class: 'placeholder-view' }, [
        h('h2', title),
        h('p', description)
      ])
    }
  }
}

function getDefaultAuthorizedRoute() {
  // 优先使用 v2 路由，若 v2 视图未生成则回退到 v1
  const v2Route = getFirstV2DynamicRoute(getWebSession()?.menuTree ?? [])
  if (v2Route) return v2Route
  return getFirstDynamicRoute(getWebSession()?.menuTree ?? []) ?? '/dashboard'
}

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/',
    name: 'admin-root',
    component: AdminShellLayout,
    redirect: () => getDefaultAuthorizedRoute(),
    meta: {
      requiresAuth: true
    },
    children: []
  },
  {
    path: '/v2',
    name: 'v2-root',
    component: AdminShellLayoutV2,
    redirect: () => getDefaultAuthorizedRoute(),
    meta: {
      requiresAuth: true,
      v2: true
    },
    children: []
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: createPlaceholderView('未找到页面', '当前页面不存在，请检查访问路径。')
  }
]

function resolveHistory() {
  return typeof window === 'undefined' || import.meta.env.MODE === 'test'
    ? createMemoryHistory()
    : createWebHistory()
}

export function createAppRouter() {
  const router = createRouter({
    history: resolveHistory(),
    routes
  })

  const session = getWebSession()
  if (session?.menuTree?.length) {
    registerDynamicRoutes(router, session.menuTree)
  }

  router.beforeEach((to: RouteLocationNormalized) => {
    const currentSession = getWebSession()
    const fallbackRoute = getFirstDynamicRoute(currentSession?.menuTree ?? []) ?? '/dashboard'

    if (currentSession?.menuTree?.length) {
      registerDynamicRoutes(router, currentSession.menuTree)
    }

    if (to.path !== '/login' && !currentSession) {
      return {
        path: '/login',
        query: {
          redirect: to.fullPath
        }
      }
    }

    if (to.path === '/login' && currentSession) {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : fallbackRoute
      return redirect
    }

    if (to.path === '/' && currentSession) {
      return fallbackRoute
    }

    const requiredPermission = to.matched
      .map((record) => record.meta.permission)
      .find((permission): permission is string => typeof permission === 'string' && permission.length > 0)

    if (requiredPermission && !hasMenuPermission(requiredPermission)) {
      if (fallbackRoute === to.path) {
        return true
      }
      return fallbackRoute
    }

    return true
  })

  return router
}

export const router = createAppRouter()
