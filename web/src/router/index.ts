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
import { hasMenuPermission } from '../auth/permissions'
import { getWebSession } from '../auth/session'
import { getFirstDynamicRoute, registerDynamicRoutes } from './dynamic-routes'

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
