import { describe, expect, it } from 'vitest'
import { routes } from './index'
import { getFirstDynamicRoute, isMenuRouteLoadable } from './dynamic-routes'
import type { SystemMenuNode } from '../auth/session'

const sampleMenuTree: SystemMenuNode[] = [
  {
    id: 1,
    permissionCode: 'menu:dashboard:view',
    permissionName: '首页',
    permissionType: 'MENU',
    clientType: 'WEB',
    parentId: null,
    path: '/dashboard',
    component: 'dashboard/DashboardView',
    icon: '',
    sortOrder: 1,
    status: 'ACTIVE',
    remark: '',
    children: []
  },
  {
    id: 2,
    permissionCode: 'menu:event:list',
    permissionName: '事件中心',
    permissionType: 'MENU',
    clientType: 'WEB',
    parentId: null,
    path: '/events',
    component: 'event/EventListView',
    icon: '',
    sortOrder: 2,
    status: 'ACTIVE',
    remark: '',
    children: []
  }
]

describe('web route continuity', () => {
  it('keeps base routes registered and resolves first dynamic route from menu tree', () => {
    expect(routes.map((route) => route.path)).toEqual(
      expect.arrayContaining([
        '/login',
        '/',
        '/:pathMatch(.*)*'
      ])
    )
    expect(isMenuRouteLoadable(sampleMenuTree[0])).toBe(true)
    expect(getFirstDynamicRoute(sampleMenuTree)).toBe('/dashboard')
  })
})
