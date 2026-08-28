export const h5NavigationItems = [
  { to: '/workbench', label: '工作台', permission: 'menu:h5:workbench:view' },
  { to: '/work-orders', label: '工单', permission: 'menu:h5:workorder:list' },
  { to: '/history', label: '历史', permission: 'menu:h5:history:view' },
  { to: '/mine', label: '我的', permission: 'menu:h5:mine:view' },
  { to: '/merchants', label: '商户管理', permission: 'menu:h5:merchant:view' },
  { to: '/vendors', label: '摊贩管理', permission: 'menu:h5:vendor:view' },
  // 志愿服务已取消（需求12.2），保留代码便于追溯后续开发
  // { to: '/volunteer', label: '志愿服务', permission: 'menu:h5:volunteer:view' },
  // 信息互通（实时聊天）功能暂不启用，保留代码后续开发
  // { to: '/messages', label: '信息互通', permission: 'menu:h5:message:view' }
] as const
