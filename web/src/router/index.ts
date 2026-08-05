import { createRouter, createWebHashHistory } from 'vue-router'
import { getSession } from '../api'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/', component: () => import('../views/DashboardView.vue') },
  { path: '/gis', component: () => import('../views/GISView.vue') },
  { path: '/population', component: () => import('../views/PopulationView.vue') },
  { path: '/buildings', component: () => import('../views/BuildingsView.vue') },
  { path: '/places', component: () => import('../views/PlacesView.vue') },
  { path: '/events', component: () => import('../views/EventsView.vue') },
  { path: '/events/create', component: () => import('../views/EventCreateView.vue') },
  { path: '/events/:id', component: () => import('../views/EventDetailView.vue') },
  { path: '/ledger', component: () => import('../views/LedgerView.vue') },
  { path: '/drones', component: () => import('../views/DronesView.vue') },
  { path: '/work-orders', component: () => import('../views/WorkOrderView.vue') },
  { path: '/audits', component: () => import('../views/AuditView.vue') },
  { path: '/processes', component: () => import('../views/ProcessView.vue') },
  { path: '/reports', component: () => import('../views/ReportsView.vue') },
  { path: '/patrol', component: () => import('../views/PatrolView.vue') },
  { path: '/safety', component: () => import('../views/SafetyView.vue') },
  { path: '/party', component: () => import('../views/PartyView.vue') },
  { path: '/parking', component: () => import('../views/ParkingView.vue') },
  { path: '/assessment', component: () => import('../views/AssessmentView.vue') },
  { path: '/audit-logs', component: () => import('../views/AuditLogView.vue') },
  // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
  // { path: '/integration', component: () => import('../views/IntegrationView.vue') },
  { path: '/org-members', component: () => import('../views/OrgMemberView.vue') },
  { path: '/biz-areas', component: () => import('../views/BizAreaView.vue') },
  { path: '/resident-reports', component: () => import('../views/ResidentReportView.vue') },
  { path: '/policy-resources', component: () => import('../views/PolicyResourceView.vue') },
  { path: '/help', component: () => import('../views/HelpView.vue') },
  { path: '/big-screen', component: () => import('../views/BigScreenView.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const session = getSession()
  // 未登录且不是登录页 → 跳转登录
  if (to.path !== '/login' && !session?.token) {
    return '/login'
  }
  // 已登录且访问登录页 → 跳转首页
  if (to.path === '/login' && session?.token) {
    return '/'
  }
})

export default router
