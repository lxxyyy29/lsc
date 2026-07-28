---
AIGC:
    Label: "1"
    ContentProducer: 001191440300708461136T1XGW3
    ProduceID: 936631c4a34b216fb8fe61b642ab3a96_bd9a47fa874211f18108525400287e28
    ReservedCode1: gzHd0l1dggPx5rOnWj001fq5AlNwCKRexR2RMWumnjBPY7skZT5KmrPpU1YFoV1XoImVQFjHfMAXHIQsP93ino+Xv9q/ANB2z6GVqObUoWbJGkG/zJgoSmmGjsGzYbX8upGjrWeC9uFL6iJilMaAJs8OqyhKaD0q2EuZUO0OFSoogPWHv83dE/F70N0=
    ContentPropagator: 001191440300708461136T1XGW3
    PropagateID: 936631c4a34b216fb8fe61b642ab3a96_bd9a47fa874211f18108525400287e28
    ReservedCode2: gzHd0l1dggPx5rOnWj001fq5AlNwCKRexR2RMWumnjBPY7skZT5KmrPpU1YFoV1XoImVQFjHfMAXHIQsP93ino+Xv9q/ANB2z6GVqObUoWbJGkG/zJgoSmmGjsGzYbX8upGjrWeC9uFL6iJilMaAJs8OqyhKaD0q2EuZUO0OFSoogPWHv83dE/F70N0=
---

# 前端无数据问题分析报告

**项目**：网格社区综合治理系统（拔蛟窝社区）
**前端**：`web-v2`（Vue3 + Vite + Axios）| **后端**：Spring Boot（`backend`）
**分析日期**：2026-07-24

---

## 1. 项目架构概要

| 项目 | 路径 | 技术栈 | 端口 |
|------|------|--------|------|
| 前端 | `web-v2/` | Vue 3 + Vite 5 + Axios + TypeScript | **5175** |
| 后端 | `backend/` | Spring Boot + MyBatis-Plus + Spring Security | **8080** |

- **后端 `context-path`**：`/api`（配置于 `application.yml`：`server.servlet.context-path: /api`）
- **前端代理**：Vite dev server 将 `/api` 代理到 `http://127.0.0.1:8080`（`vite.config.ts`）
- **前端 Axios baseURL**：`/api`（`src/api/index.ts`）

---

## 2. 接口路径对照分析

### 2.1 Dashboard 首页数据接口（核心页面）

| 前端调用 | 前端路径 | 后端 Controller | 后端路径 | 匹配? |
|----------|----------|-----------------|----------|-------|
| `getDashboardOverview()` | `GET /api/community/dashboard/overview` | `DashboardController` | `@RequestMapping("/community/dashboard")` + `@GetMapping("/overview")` | **匹配** |
| `getGridStats()` | `GET /api/community/dashboard/grid-stats` | `DashboardController` | `@GetMapping("/grid-stats")` | **匹配** |
| `getGridTree()` | `GET /api/community/grids/tree` | `GridController` | `@RequestMapping("/community/grids")` + `@GetMapping("/tree")` | **匹配** |
| `getEvents()` | `GET /api/events` | `EventController` | `@RequestMapping("/events")` + `@GetMapping` | **匹配** |

> Dashboard 页面调用的 4 个接口路径与后端完全匹配，**路径层面无问题**。

### 2.2 无人机/设备管理接口

| 前端调用 | 前端路径 | 后端 Controller | 后端实际路径 | 匹配? |
|----------|----------|-----------------|-------------|-------|
| `getDrones()` | `GET /api/drone/devices` | `DroneDashboardController` | `@RequestMapping("/drone/dashboard")` + `@GetMapping("/devices")` → **`/drone/dashboard/devices`** | **不匹配** |
| `getDroneJobs()` | `GET /api/drone/jobs` | `DroneDashboardController` | `@RequestMapping("/drone/dashboard")` + `@GetMapping("/jobs")` → **`/drone/dashboard/jobs`** | **不匹配** |

> **问题**：前端请求 `/drone/devices` 和 `/drone/jobs`，但后端实际路径是 `/drone/dashboard/devices` 和 `/drone/dashboard/jobs`，缺少中间 `/dashboard` 段，会导致 **404**。

### 2.3 其他已验证匹配的接口

| 接口 | 状态 |
|------|------|
| `POST /auth/login` | **匹配** |
| `GET /community/patrol-tasks` / `POST` / `/statistics` / `/generate` / `/mark-overdue` / `/{id}/complete` | **匹配** |
| `GET /community/org-members` | **匹配** |
| `POST /events` / `GET /events/{id}` / `PUT /events/{id}/close` / `POST /events/{id}/dispatch` 等 | **匹配** |

### 2.4 前端定义但后端缺失的接口

| 前端函数 | 请求路径 | 状态 |
|----------|----------|------|
| `getMenuTree()` | `GET /auth/menu-tree` | **后端无此端点** |

> 该函数定义了但未被任何组件调用（菜单项在 `App.vue` 中硬编码），属于**死代码**，不影响数据加载。

---

## 3. 可能导致无数据的根因分析

### 3.1 最可能的原因（按优先级排序）

#### 原因 1：后端依赖服务未启动
后端依赖以下外部服务（`application.yml`）：

| 服务 | 地址 | 用途 |
|------|------|------|
| MySQL | `127.0.0.1:3306/zhsq` | 主数据库 |
| Redis | `127.0.0.1:6379` | 缓存/会话 |
| MongoDB | `127.0.0.1:27017/alarm_dgcp` | 告警事件存储 |
| MinIO | `127.0.0.1:9009` | 文件存储 |

- 如果 MySQL 未启动，Spring Boot 虽然可能启动成功（HikariCP 默认惰性连接），但 DashboardMapper 查询会抛异常，接口返回 500 错误。
- 如果 Redis 或 MongoDB 未启动，相关功能模块会受影响，但 Dashboard 核心查询（基于 MySQL）可能仍能返回。

#### 原因 2：登录认证失败导致后续请求被拦截（401）
Spring Security 配置中，除 `/auth/login` 外**所有接口都需要认证**：

```java
.authorizeHttpRequests(authorize -> authorize
    .requestMatchers("/auth/login", ...).permitAll()
    .anyRequest().authenticated()
)
```

请求流程：
1. 前端登录 → `POST /api/auth/login` → 后端返回 `{ success: true, data: { token: "..." } }`
2. 前端拦截器提取 `data.data`，存储到 `localStorage('grid-session')`
3. 后续请求，拦截器从 `localStorage` 取 `session.token`，加 `Authorization: Bearer <token>`
4. 后端 `BearerTokenAuthenticationFilter` 解析 JWT，验证后放行

**可能失败环节**：
- 登录时前端发送 `{ account, password, clientType: 'web' }`，后端 `LoginRequest` 只接受 `account` 和 `password`（`clientType` 是多余字段，会被忽略，无影响）。
- 如果默认账号 `admin/admin123` 在数据库中不存在或密码不匹配，登录会失败，错误信息在页面底部显示。
- 如果 JWT 签名密钥与存储时不一致（如重启后密钥变化），Token 验证失败返回 401，前端会清除 localStorage 并重定向到登录页。

#### 原因 3：前端响应拦截器逻辑与后端返回值不匹配
前端拦截器：
```typescript
if (data && data.success === true) {
    return data.data   // 解包：返回 ApiResponse.data
}
return Promise.reject(data?.message || '请求失败')
```

后端 `ApiResponse` 结构：
```json
{ "success": true, "code": "OK", "message": "操作成功", "data": {...} }
```

`success` 字段存在且值为 `true`，解包逻辑正确。

但需注意：**如果后端因异常返回非 200 状态码**（如 500），axios 会走到 `error` 回调：
```typescript
error => {
    return Promise.reject(error.response?.data?.message || '网络错误')
}
```
此时 Dashboard 页面的 `await getDashboardOverview()` 会抛出异常且无 try-catch 包裹，错误会被 Vue 静默吞掉。

#### 原因 4：数据库表无数据
后端配置的数据源是 `jdbc:mysql://127.0.0.1:3306/zhsq`。即使所有接口正常返回 200，如果 `zhsq` 库中：
- `cmn_grid` 表无数据 → `getGridTree()` 返回空数组，地图无内容
- `biz_event` 表无数据 → `getEvents()` 返回空分页，事件列表为空
- Dashboard SQL 查询结果为 0 → 所有 KPI 卡片显示 0

**这是"后端正常但前端显示 0"的最常见场景。**

#### 原因 5：前端 `DashboardView.vue` 缺少错误处理
```typescript
onMounted(async () => {
    overview.value = await getDashboardOverview()  // 无 try-catch
    const stats = await getGridStats()
    const tree = await getGridTree()
    const evtResult = await getEvents()
    ...
})
```

如果任一接口报错（网络错误、500、401），后续代码不会执行，地图和图表也不会初始化。且页面不会显示错误提示，用户只看到空白/无数据。

---

## 4. 排查步骤建议

### 第一步：验证后端接口
在浏览器中直接访问（先通过前端登录拿到 token，或用 Postman）：

```
# 登录
POST http://localhost:8080/api/auth/login
Body: { "account": "admin", "password": "admin123" }

# 拿到 token 后测试 Dashboard
GET http://localhost:8080/api/community/dashboard/overview
Header: Authorization: Bearer <token>
```

预期返回：
```json
{
  "success": true,
  "code": "OK",
  "data": {
    "merchantCount": 123,
    "gridCount": 5,
    "eventTotal": 456,
    "eventRed": 12,
    "eventYellow": 34,
    "eventGreen": 410
  }
}
```

- 如果返回 401 → Token 或认证问题
- 如果返回 500 → 后端异常，查看后端日志
- 如果 `data` 中所有值为 0 → 数据库无数据

### 第二步：检查前端浏览器控制台
打开 DevTools → Network 标签，刷新页面：
- 查看 `/api/community/dashboard/overview` 请求状态码
- 查看 Response 内容
- 查看 Console 是否有 JavaScript 错误

### 第三步：验证数据库
```sql
-- 检查网格数据
SELECT COUNT(*) FROM cmn_grid;

-- 检查事件数据
SELECT COUNT(*) FROM biz_event;

-- 检查用户表（确认 admin 账号存在）
SELECT * FROM sys_user WHERE account = 'admin';
```

---

## 5. 已发现的代码问题（需修复）

| # | 问题 | 文件 | 严重程度 | 修复建议 |
|---|------|------|----------|----------|
| 1 | 无人机接口路径不匹配：`/drone/devices` → 应为 `/drone/dashboard/devices` | `web-v2/src/api/index.ts` L129 | 中 | 修改为 `/drone/dashboard/devices` 和 `/drone/dashboard/jobs`，或在后端添加 `/drone` 映射 |
| 2 | `DashboardView.vue` 无错误处理，接口失败后页面静默无数据 | `web-v2/src/views/DashboardView.vue` L148-152 | 中 | 添加 try-catch，在页面显示错误提示 |
| 3 | 所有 View 组件缺少 loading 状态 | 多个 `.vue` 文件 | 低 | 接口调用期间添加骨架屏或 loading 指示器 |
| 4 | `getMenuTree()` 定义了但后端无对应端点 | `web-v2/src/api/index.ts` L69 | 低 | 如不需要可删除；如需要则后端补充 `/auth/menu-tree` 端点 |

---

## 6. 结论

**前端无数据的最可能根因（按概率排序）**：

1. **数据库未初始化或表为空** → Dashboard 显示全 0，看起来"无数据"
2. **MySQL/Redis/MongoDB 未启动** → 后端虽启动但查询报错，接口 500
3. **登录 token 失效或未正确传递** → 后续接口全部 401，页面白屏后跳回登录

建议优先按第四节排查步骤验证后端接口和数据库状态，这比修改代码更能快速定位问题。
*（内容由AI生成，仅供参考）*
