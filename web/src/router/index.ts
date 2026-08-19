import { createRouter, createWebHashHistory } from 'vue-router'
import { getSession } from '../api'
import { permKeyForPath, isSuperAdminSession, firstVisiblePath } from '../menu'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/', component: () => import('../views/DashboardView.vue') },
  { path: '/gis', component: () => import('../views/GISView.vue') },
  { path: '/grid-manage', component: () => import('../views/GridManageView.vue') },
  { path: '/population', component: () => import('../views/PopulationView.vue') },
  { path: '/buildings', component: () => import('../views/BuildingsView.vue') },
  { path: '/places', component: () => import('../views/PlacesView.vue') },
  { path: '/events', component: () => import('../views/EventsView.vue') },
  { path: '/events/create', component: () => import('../views/EventCreateView.vue') },
  { path: '/events/:id', component: () => import('../views/EventDetailView.vue') },
  { path: '/ledger', component: () => import('../views/LedgerView.vue') },
  { path: '/drones', component: () => import('../views/DronesView.vue') },
  { path: '/video', component: () => import('../views/VideoView.vue') },
  { path: '/work-orders', component: () => import('../views/WorkOrderView.vue') },
  { path: '/dispatch-rules', component: () => import('../views/DispatchRuleView.vue') },
    { path: '/trend-alerts', component: () => import('../views/TrendAlertView.vue') },
  { path: '/audits', component: () => import('../views/AuditView.vue') },
  { path: '/processes', component: () => import('../views/ProcessView.vue') },
  { path: '/reports', component: () => import('../views/ReportsView.vue') },
  { path: '/patrol', component: () => import('../views/PatrolView.vue') },
  { path: '/emergency', component: () => import('../views/EmergencyView.vue') },
  { path: '/mosquito', component: () => import('../views/MosquitoView.vue') },
  { path: '/safety', component: () => import('../views/SafetyView.vue') },
  { path: '/party', component: () => import('../views/PartyView.vue') },
  { path: '/parking', component: () => import('../views/ParkingView.vue') },
  { path: '/vehicle-track', component: () => import('../views/VehicleTrackView.vue') },
  { path: '/assessment', component: () => import('../views/AssessmentView.vue') },
  { path: '/audit-logs', component: () => import('../views/AuditLogView.vue') },
  { path: '/system-roles', component: () => import('../views/RoleManageView.vue') },
  { path: '/system-users', component: () => import('../views/UserManageView.vue') },
  { path: '/system-menus', component: () => import('../views/MenuManageView.vue') },
  // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
  // { path: '/integration', component: () => import('../views/IntegrationView.vue') },
  { path: '/org-members', component: () => import('../views/OrgMemberView.vue') },
  { path: '/biz-areas', component: () => import('../views/BizAreaView.vue') },
  { path: '/resident-reports', component: () => import('../views/ResidentReportView.vue') },
  { path: '/repairs', component: () => import('../views/RepairView.vue') },
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
    return firstVisiblePath(session)
  }
  // 菜单权限守卫：无权限的页面直接 URL 访问时重定向到首个可见菜单
  // （超管豁免；后端 403 仍作为最终兑底，这里避免用户看到"获取失败"报错页）
  const permKey = permKeyForPath(to.path)
  if (permKey && !isSuperAdminSession(session) && !(session?.permissionCodes || []).includes(permKey)) {
    return firstVisiblePath(session)
  }
})

export default router
