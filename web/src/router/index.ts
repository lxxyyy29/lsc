import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { getSession } from '../api'
import { permKeyForPath, isSuperAdminSession, firstVisiblePath } from '../menu'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', component: () => import('../views/DashboardView.vue') },
      { path: 'big-screen', component: () => import('../views/BigScreenView.vue') },
 
      { path: 'events', component: () => import('../views/EventsView.vue') },
      { path: 'events/create', component: () => import('../views/EventCreateView.vue') },
      { path: 'events/:id', component: () => import('../views/EventDetailView.vue') },
      { path: 'work-orders', component: () => import('../views/CompletedOrderView.vue') },
      { path: 'audits', component: () => import('../views/AuditView.vue') },
      { path: 'abnormal-orders', component: () => import('../views/AbnormalOrderView.vue') },
 
      { path: 'gis', component: () => import('../views/GISView.vue') },
      { path: 'grid-manage', component: () => import('../views/GridManageView.vue') },
      { path: 'org-members', component: () => import('../views/OrgMemberView.vue') },
      { path: 'biz-areas', component: () => import('../views/BizAreaView.vue') },
 
      { path: 'population', component: () => import('../views/PopulationView.vue') },
      { path: 'buildings', component: () => import('../views/BuildingsView.vue') },
      { path: 'places', component: () => import('../views/PlacesView.vue') },
      { path: 'ledger', component: () => import('../views/LedgerView.vue') },
 
      { path: 'drones', component: () => import('../views/DronesView.vue') },
      { path: 'patrol', component: () => import('../views/PatrolView.vue') },
 
      { path: 'resident-reports', component: () => import('../views/ResidentReportView.vue') },
      { path: 'policy-resources', component: () => import('../views/PolicyResourceView.vue') },
 
      { path: 'party', component: () => import('../views/PartyView.vue') },

      { path: 'reports', component: () => import('../views/ReportsView.vue') },
      { path: 'assessment', component: () => import('../views/AssessmentView.vue') },
      { path: 'trend-alerts', component: () => import('../views/TrendAlertView.vue') },
 
      { path: 'audit-logs', component: () => import('../views/AuditLogView.vue') },
 
      { path: 'system-roles', component: () => import('../views/RoleManageView.vue') },
      { path: 'system-users', component: () => import('../views/UserManageView.vue') },
      { path: 'system-menus', component: () => import('../views/MenuManageView.vue') },
      { path: 'system-dicts', component: () => import('../views/DictManageView.vue') },
 
      { path: 'processes', component: () => import('../views/ProcessView.vue') },
      { path: 'help', component: () => import('../views/HelpView.vue') },

      // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
      // { path: '/integration', component: () => import('../views/IntegrationView.vue') },
    ]
  },
  // 已下线路由：直接访问时重定向到首页
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  try {
    const session = getSession()
    
    // 需要认证但未登录 → 跳转登录页
    if (to.meta.requiresAuth !== false && !session?.token) {
      return '/login'
    }
    
    // 已登录但访问登录页 → 跳转首页
    if (to.path === '/login' && session?.token) {
      return firstVisiblePath(session)
    }
    
    // 菜单权限守卫（仅对需要认证的路由生效）
    const permKey = permKeyForPath(to.path)
    if (permKey && !isSuperAdminSession(session) && !(session?.permissionCodes || []).includes(permKey)) {
      return firstVisiblePath(session)
    }
  } catch (error) {
    console.error('[路由守卫异常]', error)
    
    try {
      localStorage.removeItem('grid-session')
    } catch (e) {
      console.error('[清除session失败]', e)
    }
    
    return '/login'
  }
})

// 处理导航错误（防止白屏）
router.afterEach((to, from, error) => {
  if (error) {
    console.error('[路由跳转失败]', {
      from: from.fullPath,
      to: to.fullPath,
      error: error.message || error
    })
  }
})

export default router