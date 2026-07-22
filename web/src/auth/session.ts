import { HttpResponseError } from '../api/http'

export type SystemMenuNodeType = 'CATALOG' | 'MENU' | 'BUTTON'
export type SystemMenuClientType = 'WEB' | 'H5'
export type SystemMenuStatus = 'ACTIVE' | 'DISABLED'

export interface SystemMenuNode {
  id: number
  permissionCode: string
  permissionName: string
  permissionType: SystemMenuNodeType
  clientType: SystemMenuClientType
  parentId: number | null
  path: string
  component: string
  icon: string
  sortOrder: number
  status: SystemMenuStatus
  remark: string
  children: SystemMenuNode[]
}

export interface WebSession {
  token: string
  userId: number
  userName: string
  account: string
  roleCodes: string[]
  permissionCodes: string[]
  menuPermissionCodes: string[]
  menuTree?: SystemMenuNode[]
}

export interface WebLoginResponse {
  token: string
  userId: number
  userName: string
  account: string
  roleCodes: string[]
  permissionCodes: string[]
  menuTree?: SystemMenuNode[]
}

export interface CurrentWebUser {
  id: number
  username: string
  realName: string
  phone: string | null
  roleCodes: string[]
  permissionCodes: string[]
  menuPermissionCodes: string[]
  menuTree?: SystemMenuNode[]
}

interface LegacyMenuDefinition {
  aliases: string[]
  permissionName: string
  path: string
  component: string
  sortOrder: number
  parentCode?: string
}

interface LegacyMenuGroup {
  permissionCode: string
  permissionName: string
  path: string
  icon: string
  sortOrder: number
}

const LEGACY_MENU_GROUPS: LegacyMenuGroup[] = [
  {
    permissionCode: 'catalog:community',
    permissionName: '网格治理',
    path: '/community/grid',
    icon: 'Grid',
    sortOrder: 40
  },
  {
    permissionCode: 'catalog:biz',
    permissionName: '业务管理',
    path: '/areas',
    icon: '',
    sortOrder: 50
  },
  {
    permissionCode: 'catalog:system',
    permissionName: '系统配置',
    path: '/system/config',
    icon: '',
    sortOrder: 100
  }
]

const LEGACY_MENU_DEFINITIONS: LegacyMenuDefinition[] = [
  {
    aliases: ['menu:dashboard:view'],
    permissionName: '首页',
    path: '/dashboard',
    component: 'dashboard/DashboardView',
    sortOrder: 10
  },
  {
    aliases: ['menu:community:grid'],
    permissionName: 'GIS网格可视化',
    path: '/community/grid',
    component: 'community/GridView',
    sortOrder: 15,
    parentCode: 'catalog:community'
  },
  {
    aliases: ['menu:event:list'],
    permissionName: '事件中心',
    path: '/events',
    component: 'event/EventListView',
    sortOrder: 20
  },
  {
    aliases: ['menu:workorder:list'],
    permissionName: '工单中心',
    path: '/work-orders',
    component: 'workorder/WorkOrderListView',
    sortOrder: 30
  },
  {
    aliases: ['menu:event:false-alarm'],
    permissionName: '误报记录',
    path: '/false-alarm-records',
    component: 'event/FalseAlarmRecordView',
    sortOrder: 40
  },
  {
    aliases: ['menu:biz:area:list', 'menu:biz:area'],
    permissionName: '片区管理',
    path: '/areas',
    component: 'biz/BizAreaManageView',
    sortOrder: 10,
    parentCode: 'catalog:biz'
  },
  {
    aliases: ['menu:biz:merchant:list', 'menu:biz:merchant'],
    permissionName: '商户管理',
    path: '/merchants',
    component: 'biz/BizMerchantManageView',
    sortOrder: 20,
    parentCode: 'catalog:biz'
  },
  {
    aliases: ['menu:biz:vendor:list', 'menu:biz:vendor'],
    permissionName: '流动摊贩管理',
    path: '/mobile-vendors',
    component: 'biz/BizVendorManageView',
    sortOrder: 30,
    parentCode: 'catalog:biz'
  },
  {
    aliases: ['menu:biz:violation-area:list', 'menu:biz:violation-area'],
    permissionName: '违章区域管理',
    path: '/violation-areas',
    component: 'biz/ViolationAreaManageView',
    sortOrder: 40,
    parentCode: 'catalog:biz'
  },
  {
    aliases: ['menu:report:district'],
    permissionName: '数据报表',
    path: '/reports/district',
    component: 'report/DistrictReportView',
    sortOrder: 60
  },
  {
    aliases: ['menu:drone:media', 'menu:media:results', 'menu:system:view'],
    permissionName: '媒体中心',
    path: '/media/results',
    component: 'media/MediaRecognitionView',
    sortOrder: 70
  },
  {
    aliases: ['menu:drone:list'],
    permissionName: '飞控接入',
    path: '/drones',
    component: 'drone/DroneListView',
    sortOrder: 80
  },
  {
    aliases: ['menu:process:list'],
    permissionName: '流程配置',
    path: '/processes',
    component: 'process/ProcessTemplateListView',
    sortOrder: 90
  },
  {
    aliases: ['menu:system:menu', 'menu:system:view'],
    permissionName: '菜单管理',
    path: '/system/menu',
    component: 'system/MenuManageView',
    sortOrder: 10,
    parentCode: 'catalog:system'
  },
  {
    aliases: ['menu:system:role', 'menu:system:view'],
    permissionName: '角色管理',
    path: '/system/role',
    component: 'system/RoleManageView',
    sortOrder: 20,
    parentCode: 'catalog:system'
  },
  {
    aliases: ['menu:system:user', 'menu:system:view'],
    permissionName: '用户管理',
    path: '/system/user',
    component: 'system/UserManageView',
    sortOrder: 30,
    parentCode: 'catalog:system'
  }
]

export const WEB_SESSION_STORAGE_KEY = 'dgcp-oa-web-session'

let memorySession: WebSession | null = null

function getStorage() {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    const storage = window.localStorage
    if (
      storage &&
      typeof storage.getItem === 'function' &&
      typeof storage.setItem === 'function' &&
      typeof storage.removeItem === 'function'
    ) {
      return storage
    }
  } catch {
    return null
  }

  return null
}

function isNonEmptyString(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

function normalizeStringArray(value: unknown) {
  if (!Array.isArray(value)) {
    return null
  }

  return dedupeStrings(
    value
      .filter((item): item is string => typeof item === 'string')
      .map((item) => item.trim())
      .filter((item) => item.length > 0)
  )
}

function dedupeStrings(values: string[]) {
  return [...new Set(values)]
}

function normalizeMenuNodeType(value: unknown): SystemMenuNodeType | null {
  return value === 'CATALOG' || value === 'MENU' || value === 'BUTTON' ? value : null
}

function normalizeClientType(value: unknown): SystemMenuClientType {
  return value === 'H5' ? 'H5' : 'WEB'
}

function normalizeStatus(value: unknown): SystemMenuStatus {
  return value === 'DISABLED' ? 'DISABLED' : 'ACTIVE'
}

function normalizeMenuTree(
  value: unknown,
  fallbackPermissionCodes: string[],
  parentId: number | null = null,
  idSeed: { value: number } = { value: 1 },
  allowLegacyFallback: boolean = true
): SystemMenuNode[] {
  if (!Array.isArray(value)) {
    return allowLegacyFallback ? createLegacyMenuTree(fallbackPermissionCodes) : []
  }

  const normalized = value
    .map((item) => normalizeMenuTreeNode(item, fallbackPermissionCodes, parentId, idSeed))
    .filter((item): item is SystemMenuNode => item !== null)

  if (normalized.length > 0) {
    return sortMenuTree(normalized)
  }

  return allowLegacyFallback ? createLegacyMenuTree(fallbackPermissionCodes) : []
}

function normalizeMenuTreeNode(
  value: unknown,
  fallbackPermissionCodes: string[],
  parentId: number | null,
  idSeed: { value: number }
): SystemMenuNode | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const candidate = value as Partial<SystemMenuNode>
  const permissionCode = isNonEmptyString(candidate.permissionCode) ? candidate.permissionCode.trim() : ''
  const permissionType = normalizeMenuNodeType(candidate.permissionType)

  if (!permissionCode || !permissionType) {
    return null
  }

  const id = typeof candidate.id === 'number' && Number.isInteger(candidate.id) && candidate.id > 0
    ? candidate.id
    : idSeed.value++
  const currentParentId = typeof candidate.parentId === 'number' && Number.isInteger(candidate.parentId)
    ? candidate.parentId
    : parentId
  const children = normalizeMenuTree(candidate.children, fallbackPermissionCodes, id, idSeed, false)

  return {
    id,
    permissionCode,
    permissionName: isNonEmptyString(candidate.permissionName) ? candidate.permissionName.trim() : permissionCode,
    permissionType,
    clientType: normalizeClientType(candidate.clientType),
    parentId: currentParentId,
    path: isNonEmptyString(candidate.path) ? candidate.path.trim() : '',
    component: isNonEmptyString(candidate.component) ? candidate.component.trim() : '',
    icon: isNonEmptyString(candidate.icon) ? candidate.icon.trim() : '',
    sortOrder: typeof candidate.sortOrder === 'number' && Number.isFinite(candidate.sortOrder) ? candidate.sortOrder : 0,
    status: normalizeStatus(candidate.status),
    remark: isNonEmptyString(candidate.remark) ? candidate.remark.trim() : '',
    children
  }
}

function sortMenuTree(tree: SystemMenuNode[]): SystemMenuNode[] {
  return [...tree]
    .sort((left, right) => {
      if (left.sortOrder !== right.sortOrder) {
        return left.sortOrder - right.sortOrder
      }
      return left.id - right.id
    })
    .map((node): SystemMenuNode => ({
      ...node,
      children: sortMenuTree(node.children ?? [])
    }))
}

function createLegacyMenuTree(permissionCodes: string[]) {
  const normalizedPermissionCodes = dedupeStrings(permissionCodes.filter((code) => code.startsWith('menu:')))
  let nextId = 1
  const groupedMenus = new Map<string, SystemMenuNode[]>()
  const rootMenus: SystemMenuNode[] = []

  for (const definition of LEGACY_MENU_DEFINITIONS) {
    const matchedCode = definition.aliases.find((alias) => normalizedPermissionCodes.includes(alias))
    if (!matchedCode) {
      continue
    }

    const node: SystemMenuNode = {
      id: nextId++,
      permissionCode: matchedCode,
      permissionName: definition.permissionName,
      permissionType: 'MENU',
      clientType: 'WEB',
      parentId: null,
      path: definition.path,
      component: definition.component,
      icon: '',
      sortOrder: definition.sortOrder,
      status: 'ACTIVE',
      remark: '',
      children: []
    }

    if (definition.parentCode) {
      const siblings = groupedMenus.get(definition.parentCode) ?? []
      siblings.push(node)
      groupedMenus.set(definition.parentCode, siblings)
      continue
    }

    rootMenus.push(node)
  }

  for (const group of LEGACY_MENU_GROUPS) {
    const children = groupedMenus.get(group.permissionCode) ?? []
    if (!children.length) {
      continue
    }

    rootMenus.push({
      id: nextId++,
      permissionCode: group.permissionCode,
      permissionName: group.permissionName,
      permissionType: 'CATALOG',
      clientType: 'WEB',
      parentId: null,
      path: group.path,
      component: '',
      icon: group.icon,
      sortOrder: group.sortOrder,
      status: 'ACTIVE',
      remark: '',
      children: sortMenuTree(children)
    })
  }

  return sortMenuTree(rootMenus)
}

function collectMenuPermissionCodes(tree: SystemMenuNode[]) {
  const permissionCodes: string[] = []

  const walk = (nodes: SystemMenuNode[]) => {
    for (const node of nodes) {
      if (node.permissionType !== 'BUTTON' && node.status !== 'DISABLED') {
        permissionCodes.push(node.permissionCode)
      }
      if (node.children.length > 0) {
        walk(node.children)
      }
    }
  }

  walk(tree)
  return dedupeStrings(permissionCodes)
}

export function normalizeWebSession(value: unknown): WebSession | null {
  if (!value || typeof value !== 'object') {
    return null
  }

  const candidate = value as Partial<WebSession>
  const token = isNonEmptyString(candidate.token) ? candidate.token.trim() : ''
  const account = isNonEmptyString(candidate.account) ? candidate.account.trim() : ''
  const userName = isNonEmptyString(candidate.userName) ? candidate.userName.trim() : ''
  const userId = typeof candidate.userId === 'number' && Number.isInteger(candidate.userId) && candidate.userId > 0
    ? candidate.userId
    : NaN
  const roleCodes = normalizeStringArray(candidate.roleCodes)
  const permissionCodes = normalizeStringArray(candidate.permissionCodes)
  const menuPermissionCodes = normalizeStringArray(candidate.menuPermissionCodes) ?? []

  if (!token || !account || !userName || Number.isNaN(userId) || !roleCodes || !permissionCodes) {
    return null
  }

  const seedMenuPermissionCodes = dedupeStrings([
    ...permissionCodes.filter((code) => code.startsWith('menu:')),
    ...menuPermissionCodes.filter((code) => code.startsWith('menu:'))
  ])
  const menuTree = normalizeMenuTree(candidate.menuTree, seedMenuPermissionCodes)
  const normalizedMenuPermissionCodes = dedupeStrings([
    ...menuPermissionCodes.filter((code) => code.startsWith('menu:')),
    ...collectMenuPermissionCodes(menuTree)
  ])

  return {
    token,
    userId,
    userName,
    account,
    roleCodes,
    permissionCodes,
    menuPermissionCodes: normalizedMenuPermissionCodes,
    menuTree
  }
}

export function getWebSession(): WebSession | null {
  const storage = getStorage()
  if (!storage) {
    return normalizeWebSession(memorySession)
  }

  const rawValue = storage.getItem(WEB_SESSION_STORAGE_KEY)
  if (!rawValue) {
    return null
  }

  try {
    const session = normalizeWebSession(JSON.parse(rawValue))
    if (!session) {
      storage.removeItem(WEB_SESSION_STORAGE_KEY)
      memorySession = null
      return null
    }

    memorySession = session
    return session
  } catch {
    storage.removeItem(WEB_SESSION_STORAGE_KEY)
    memorySession = null
    return null
  }
}

export function persistWebSession(session: WebSession) {
  const normalized = normalizeWebSession(session)
  if (!normalized) {
    clearWebSession()
    return
  }

  memorySession = normalized

  const storage = getStorage()
  if (!storage) {
    return
  }

  storage.setItem(WEB_SESSION_STORAGE_KEY, JSON.stringify(normalized))
}

export function clearWebSession() {
  memorySession = null

  const storage = getStorage()
  if (!storage) {
    return
  }

  storage.removeItem(WEB_SESSION_STORAGE_KEY)
}

export function hasWebSession() {
  return getWebSession() !== null
}

export function hasWebPermission(code: string) {
  return getWebSession()?.permissionCodes.includes(code) ?? false
}

export function createWebSessionFromLoginResponse(response: WebLoginResponse): WebSession {
  return normalizeWebSession({
    token: response.token,
    userId: response.userId,
    userName: response.userName,
    account: response.account,
    roleCodes: [...response.roleCodes],
    permissionCodes: [...response.permissionCodes],
    menuPermissionCodes: response.permissionCodes.filter((code) => code.startsWith('menu:')),
    menuTree: response.menuTree ?? []
  }) as WebSession
}

export function mergeCurrentUserIntoSession(session: WebSession, currentUser: CurrentWebUser): WebSession {
  return normalizeWebSession({
    token: session.token,
    userId: currentUser.id,
    userName: currentUser.realName,
    account: currentUser.username,
    roleCodes: [...currentUser.roleCodes],
    permissionCodes: [...currentUser.permissionCodes],
    menuPermissionCodes: [...currentUser.menuPermissionCodes],
    menuTree: currentUser.menuTree ?? session.menuTree ?? []
  }) as WebSession
}

export async function recoverWebSession(fetchCurrentUser: () => Promise<CurrentWebUser>) {
  const session = getWebSession()
  if (!session?.token) {
    return null
  }

  try {
    const currentUser = await fetchCurrentUser()
    const recoveredSession = mergeCurrentUserIntoSession(session, currentUser)
    persistWebSession(recoveredSession)
    return recoveredSession
  } catch (error) {
    if (error instanceof HttpResponseError && error.status === 401) {
      clearWebSession()
      return null
    }

    return session
  }
}
