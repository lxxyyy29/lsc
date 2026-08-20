// 菜单配置统一出口：App.vue 侧边栏、路由权限守卫、登录后落地页均从这里取数，
// 避免菜单项/权限码在多处重复维护。permKey 对应 sys_permission 中的 web:menu:* 菜单权限，
// 超管在角色管理页勾选后，对应角色用户才能看到该菜单。
export interface MenuItem {
  path: string
  name: string
  permKey?: string
  badgeKey?: string
}

export interface MenuGroup {
  name: string
  icon: string
  items: MenuItem[]
}

export const menuGroups: MenuGroup[] = [
  {
    name: '首页概览', icon: 'fas fa-tachometer-alt',
    items: [
      { path: '/', name: '全域态势看板', permKey: 'web:menu:dashboard' },
      { path: '/big-screen', name: '综合监管大屏', permKey: 'web:menu:big-screen' },
    ]
  },
  {
    name: '事件工单', icon: 'fas fa-tasks',
    items: [
      { path: '/events', name: '事件闭环处置', badgeKey: 'eventsPending', permKey: 'web:menu:events' },
      { path: '/work-orders', name: '工单中心', badgeKey: 'workOrdersPending', permKey: 'web:menu:work-orders' },
      { path: '/audits', name: '审核中心', badgeKey: 'auditsPending', permKey: 'web:menu:audits' },
      { path: '/dispatch-rules', name: '智能派单规则', permKey: 'web:menu:dispatch-rules' },
    ]
  },
  {
    name: '网格治理', icon: 'fas fa-map-marked-alt',
    items: [
      { path: '/gis', name: 'GIS网格可视化', permKey: 'web:menu:gis' },
      { path: '/grid-manage', name: '网格管理', permKey: 'web:menu:grid-manage' },
      { path: '/biz-areas', name: '辖区管理', permKey: 'web:menu:biz-areas' },
      { path: '/org-members', name: '组织人员', badgeKey: 'pwdResetsPending', permKey: 'web:menu:org-members' },
    ]
  },
  {
    name: '基础台账', icon: 'fas fa-database',
    items: [
      { path: '/population', name: '实有人口库', permKey: 'web:menu:population' },
      { path: '/buildings', name: '房屋/出租屋库', permKey: 'web:menu:buildings' },
      { path: '/places', name: '场所资源库', permKey: 'web:menu:places' },
      { path: '/ledger', name: '场所台账', permKey: 'web:menu:ledger' },
    ]
  },
  {
    name: '居民服务', icon: 'fas fa-users',
    items: [
      { path: '/resident-reports', name: '居民上报', badgeKey: 'residentReportsPending', permKey: 'web:menu:resident-reports' },
      { path: '/policy-resources', name: '政策资源', permKey: 'web:menu:policy-resources' },
    ]
  },
  {
    name: '巡查防控', icon: 'fas fa-shield-alt',
    items: [
      { path: '/patrol', name: '网格巡查', permKey: 'web:menu:patrol' },
      { path: '/parking', name: '停车管理', permKey: 'web:menu:parking' },
    ]
  },
  {
    name: '智慧应用', icon: 'fas fa-helicopter',
    items: [
      { path: '/party', name: '智慧党建', permKey: 'web:menu:party' },
      { path: '/drones', name: '无人机管理', permKey: 'web:menu:drones' },
      { path: '/video', name: '视频轮巡', permKey: 'web:menu:video' },
    ]
  },
  {
    name: '数据决策', icon: 'fas fa-chart-bar',
    items: [
      { path: '/reports', name: '数据报表', permKey: 'web:menu:reports' },
      { path: '/assessment', name: '考核研判', permKey: 'web:menu:assessment' },
      { path: '/trend-alerts', name: '趋势预判预警', badgeKey: 'trendAlerts', permKey: 'web:menu:trend-alerts' },
      { path: '/audit-logs', name: '审计日志', permKey: 'web:menu:audit-logs' },
    ]
  },
  {
    name: '系统设置', icon: 'fas fa-cog',
    items: [
      { path: '/system-roles', name: '角色管理', permKey: 'web:menu:system-roles' },
      { path: '/system-users', name: '账号管理', permKey: 'web:menu:system-users' },
      { path: '/system-menus', name: '菜单管理', permKey: 'web:menu:system-menus' },
      { path: '/system-dicts', name: '字典管理', permKey: 'web:menu:system-dicts' },
    ]
  },
]

// 子路由沿用父级菜单权限（详情页/创建页不应单独放行）
const childPermMap: Record<string, string> = {
  '/events/create': 'web:menu:events',
}

const permByPath: Record<string, string> = {}
for (const g of menuGroups) {
  for (const i of g.items) {
    if (i.permKey) permByPath[i.path] = i.permKey
  }
}

// 取路由对应的菜单权限码；/events/:id 这类带参路径归一到父路径
export function permKeyForPath(path: string): string | undefined {
  if (childPermMap[path]) return childPermMap[path]
  if (permByPath[path]) return permByPath[path]
  const m = path.match(/^(\/events)\/[^/]+$/)
  if (m) return permByPath[m[1]]
  return undefined
}

interface SessionLike {
  roleCodes?: string[]
  permissionCodes?: string[]
}

export function isSuperAdminSession(session: SessionLike | null): boolean {
  return (session?.roleCodes || []).includes('SUPER_ADMIN')
}

// 按会话权限过滤后的可见菜单（超管豁免，防止误操作把菜单全取消后锁死）
export function visibleGroupsFor(session: SessionLike | null): MenuGroup[] {
  if (isSuperAdminSession(session)) return menuGroups
  const codes = session?.permissionCodes || []
  return menuGroups
    .map(g => ({ ...g, items: g.items.filter(i => !i.permKey || codes.includes(i.permKey)) }))
    .filter(g => g.items.length > 0)
}

// 登录后落地页：第一个可见菜单项（无任何可见菜单时兜底首页，由接口 403 兜底拦截）
export function firstVisiblePath(session: SessionLike | null): string {
  const groups = visibleGroupsFor(session)
  return groups[0]?.items[0]?.path || '/'
}
