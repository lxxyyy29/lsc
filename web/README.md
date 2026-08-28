# 东莞杰瑞智慧网格治理平台 — Web 管理端

## 启动

```bash
cd web
npx pnpm install
npx pnpm dev --host 0.0.0.0 --port 5175
```

访问 http://localhost:5175

## 说明

- 后端 API: http://localhost:8080/api
- 默认账号: admin / admin123
- UI 库: Element Plus
- 地图: 高德地图 AMap JSAPI 2.0

## 功能模块

- 全域态势看板（KPI 卡片 + 图表）
- GIS 网格可视化
- 事件管理（创建/派单/关闭/审核）
- 工单管理（处理/复核/关闭确认）
- 审核与流程模板
- 网格/人口/楼栋/场所管理
- 巡查管理
- 安全检查
- 停车管理
- 智慧党建
- 考核研判
- 信息互通（外部系统对接）
- 电子台账
- 无人机与视频监控
- 系统管理（用户/角色/权限/菜单）
- 审计日志
