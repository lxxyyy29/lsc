# 双端真实登录与统一 RBAC 设计

## 1. 目标

为当前项目补齐一套可落地的统一认证与授权能力，覆盖：

- Web 管理端真实登录
- H5 处置端真实登录
- Web/H5 共用一套用户与角色体系
- 菜单权限、按钮权限、接口权限三层 RBAC
- H5 继续保留“本人工单归属”业务校验

本设计聚焦 phase-1 可用闭环，不扩展到部门级数据权限、多租户、SSO、refresh token、复杂验证码等增强能力。

## 2. 当前现状

### 2.1 已有能力

后端已具备：

- `sys_role`
- `sys_user`
- 业务主流程接口与状态机
- H5 工单归属校验
- Spring Security 基础依赖

Web/H5 前端已具备：

- 登录页或登录占位
- 本地 session / 路由守卫雏形
- 业务页面骨架与主流程页面

### 2.2 主要缺口

当前缺失以下关键能力：

- Web 真实登录接口与会话体系
- H5 真实登录接口与会话体系
- 统一 token 认证解析
- 完整 RBAC 表结构与权限装载
- Web 菜单/按钮权限控制
- 后端接口权限校验

### 2.3 当前模型限制

`sys_user` 当前仅通过 `role_id` 绑定单角色，无法支持：

- 多角色用户
- 角色权限关联
- 菜单/按钮/API 权限模型
- Web/H5 同账号多端能力边界

因此本次需要将当前单角色模型升级为标准 RBAC 关联模型。

## 3. 范围

## 3.1 本次实现范围

### 后端

- Web 登录：`POST /api/auth/login`
- H5 登录：`POST /api/h5/auth/login`
- Web 登出：`POST /api/auth/logout`
- H5 登出：`POST /api/h5/auth/logout`
- Web 当前用户：`GET /api/auth/me`
- H5 当前用户：`GET /api/h5/auth/me`
- 统一 Bearer token 认证
- 统一当前登录用户上下文
- 统一权限码装载与接口权限校验
- RBAC 关联表、权限种子数据、角色种子数据

### Web 前端

- 真实登录页
- token/session 持久化
- `/api/auth/me` 会话恢复
- 菜单按权限过滤
- 页面按钮按权限控制
- 请求自动携带 Bearer token

### H5 前端

- 真实登录页接入
- token/session 持久化
- `/api/h5/auth/me` 会话恢复
- 请求自动携带 Bearer token
- 动作按钮按权限显示
- 业务提交不再依赖前端手工传 `X-Foundation-*` 头

## 3.2 明确不做

- 数据权限分层（全部/本部门/本人体系化建模）
- 部门组织架构权限
- refresh token
- token 黑名单
- 单点登录
- 图形验证码真实实现
- Web/H5 之外的第三端认证

## 4. 总体方案

采用“统一认证 + 统一 RBAC + 双端差异化接入”的方案。

### 4.1 统一认证

- Web 与 H5 共用一套用户表、角色表、权限表
- 使用 Bearer token 承载登录态
- 后端统一认证过滤器解析 token
- 受保护接口从认证上下文读取当前用户

### 4.2 双端差异化边界

虽然 Web/H5 共用账号体系，但登录入口与允许能力不同：

- Web 登录的准入判断以权限码白名单为唯一真相，用户必须至少具备一个 Web 入口权限，例如 `menu:dashboard:view`、`menu:event:list`、`menu:audit:list`
- H5 登录的准入判断同样以权限码白名单为唯一真相，用户必须至少具备一个 H5 入口权限，例如 `menu:h5:workbench:view`、`menu:h5:workorder:list`
- 不额外以“角色名是否匹配某端”作为第二套独立规则，避免角色判断和权限判断分叉
- 同一用户可以同时拥有 Web 与 H5 能力

### 4.3 RBAC 粒度

权限类型固定为：

- `MENU`
- `BUTTON`
- `API`

权限控制策略：

- Web 菜单显示由 `MENU` 权限控制
- Web/H5 页面动作按钮由 `BUTTON` 权限控制
- 后端接口访问由 `API` 权限控制
- H5 工单具体操作额外保留 ownership 校验

## 5. 后端设计

### 5.1 数据模型调整

在现有 `sys_user`、`sys_role` 基础上补充以下模型：

#### 保留并升级

- `sys_user`
- `sys_role`

#### 新增

- `sys_permission`
- `sys_user_role`
- `sys_role_permission`

#### 推荐字段设计

##### `sys_permission`

- `id`
- `permission_code`，唯一，例如 `event:list`
- `permission_name`
- `permission_type`：`MENU` / `BUTTON` / `API`
- `client_type`：`WEB` / `H5` / `COMMON`
- `parent_id`，支持菜单树
- `path`，菜单路由或接口匹配标识
- `sort_order`
- `status`
- `remark`
- `created_at`
- `updated_at`

##### `sys_user_role`

- `id`
- `user_id`
- `role_id`
- `created_at`

##### `sys_role_permission`

- `id`
- `role_id`
- `permission_id`
- `created_at`

#### 兼容策略

当前 `sys_user.role_id` 为单角色旧字段。本次建议：

- 新增多对多表并作为后续唯一有效来源
- 保留 `role_id` 一段时间用于兼容旧测试数据
- 新逻辑优先从 `sys_user_role` 查询角色
- 如用户在关联表中无角色，可回退读取 `role_id` 以平滑迁移

这样能控制改造成本，同时避免一次性破坏现有业务测试。

### 5.2 认证接口

#### 统一响应约定

认证接口不单独发明返回格式，继续使用项目现有统一包裹：`ApiResponse<T>`。

因此以下认证接口中，文档示例的业务字段都位于：

```json
{
  "success": true,
  "code": "OK",
  "message": "Success",
  "data": {
    "token": "bearer-token"
  }
}
```

#### Web 登录

`POST /api/auth/login`

请求：

```json
{
  "account": "admin",
  "password": "123456",
  "captcha": "optional"
}
```

响应 `data`：

```json
{
  "token": "bearer-token",
  "userId": 1,
  "userName": "平台管理员",
  "account": "admin",
  "roleCodes": ["SUPER_ADMIN"],
  "permissionCodes": ["menu:dashboard:view", "menu:event:list", "api:event:list"]
}
```

#### H5 登录

`POST /api/h5/auth/login`

请求与 Web 登录结构一致。

差异点：

- H5 登录的准入判断以权限码为唯一真相，不以“是否拥有某个角色名”做额外分叉
- 具体规则为：用户必须至少具备一个 H5 登录入口权限，例如 `menu:h5:workbench:view` 或其他明确列入 H5 入口白名单的权限码
- 不满足时返回“当前账号无 H5 访问权限”

#### 登出

`POST /api/auth/logout`
`POST /api/h5/auth/logout`

phase-1 处理为无状态接口：

- 后端返回成功包裹响应
- 前端负责清理本地 token/session

#### 当前用户

`GET /api/auth/me`
`GET /api/h5/auth/me`

返回统一包裹响应，`data` 中包含当前用户基础信息、角色、权限码集合、菜单权限码集合。
### 5.3 token 与密码方案

采用 phase-1 自包含 Bearer token，但实现必须选择当前依赖栈中可稳定落地的具体机制。

#### token 具体实现

本次建议新增 JWT 依赖，使用 HMAC 签名的 JWT 作为 Bearer token。

token 至少承载：

- `userId`
- `account`
- `userName`
- `clientType`
- `iat`
- `exp`

`roleCodes` 和 `permissionCodes` 不放入 token 作为唯一真相，避免权限变更后令牌长期不一致。

推荐策略：

- token 只负责身份识别与端类型识别
- 角色与权限每次请求按用户实时加载
- phase-1 可增加轻量缓存，但缓存不是唯一真相

#### token 配置

新增认证配置项：

- `security.auth.jwt-secret`
- `security.auth.access-token-expire-minutes`

本地、测试、生产均通过配置注入，不在代码中硬编码密钥。

#### 密码校验

当前库表字段是 `sys_user.password_hash`，但代码库中尚无密码编码器与校验实现。本次明确采用 Spring Security `PasswordEncoder`，推荐 `BCryptPasswordEncoder`。

约束如下：

- 新增或初始化用户时，密码必须以 BCrypt 哈希保存到 `password_hash`
- 登录时使用 `passwordEncoder.matches(rawPassword, passwordHash)` 校验
- 不允许继续使用明文密码或自定义简化哈希

#### 测试与种子数据要求

由于当前测试库和种子数据可能没有 BCrypt 密码，本次需要：

- 为认证相关测试补充 BCrypt 编码后的密码样本
- 为初始化账号提供明确的默认演示密码说明
- 对现有非认证业务测试，尽量不要求它们理解密码细节
### 5.4 认证上下文

新增统一认证上下文对象，例如：

- `AuthenticatedUser`

字段包括：

- `userId`
- `account`
- `userName`
- `clientType`
- `roleCodes`
- `permissionCodes`

并新增统一访问入口，例如：

- `CurrentUserResolver`

业务层与控制器均通过统一入口获取当前登录人，替代当前 foundation 阶段的头部模拟身份方式。

### 5.5 Spring Security 调整

当前 `SecurityConfig` 是全放通模式，不能一步切成“所有 `/api/**` 默认登录后访问”而不考虑测试与迁移成本。本次需要分阶段收紧。

#### 第一阶段：建立认证能力但最小破坏现有测试

`permitAll`：

- `/api/auth/login`
- `/api/h5/auth/login`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

其余接口虽然开始接入认证过滤器，但不立即全部启用细粒度权限注解。先优先覆盖：

- Web 登录后访问的管理端主接口
- H5 业务接口

并同步补充测试辅助能力，确保现有单元/集成测试能构造有效 token 或显式使用测试认证配置。

#### 第二阶段：收紧为 authenticated-by-default

当认证测试和主要业务测试稳定后，再收紧到：

- 其余 `/api/**` 默认要求认证
- 控制器方法按权限码执行二次校验

#### 认证过滤器

新增 Bearer token 认证过滤器：

- 解析 `Authorization: Bearer ...`
- 校验 token
- 加载当前用户、角色、权限
- 注入 `SecurityContext` 与统一请求上下文

#### 测试迁移要求

需要同时补充：

- token 构造工具或测试认证辅助
- 对旧的 MockMvc / 集成测试给出统一认证注入方式
- 明确哪些接口在迁移初期仍允许旧测试通过，哪些已切换为必须认证
### 5.6 权限校验

推荐在控制器层或方法层增加权限声明，统一使用权限码校验。

为避免菜单、按钮、接口命名漂移，本次统一采用：

- `menu:<domain>:<action>`
- `button:<domain>:<action>`
- `api:<domain>:<action>`

示例：

- 事件列表菜单：`menu:event:list`
- 事件详情页面入口：`menu:event:detail`
- 启动审核按钮：`button:audit:start`
- 审核通过按钮：`button:audit:approve`
- 审核驳回按钮：`button:audit:reject`
- 派单按钮：`button:workorder:dispatch`
- 办结确认按钮：`button:workorder:confirm-close`
- 流程列表菜单：`menu:process-template:list`
- 流程编辑按钮：`button:process-template:edit`
- 事件列表接口：`api:event:list`
- 事件详情接口：`api:event:detail`
- 审核启动接口：`api:audit:start`
- 审核通过接口：`api:process-instance:approve`
- 审核驳回接口：`api:process-instance:reject`
- 派单接口：`api:workorder:dispatch`
- 办结确认接口：`api:workorder:confirm-close`
- H5 工作台菜单入口：`menu:h5:workbench:view`
- H5 工单列表接口：`api:h5:workorder:list`
- H5 接单按钮：`button:h5:workorder:accept`
- H5 到场按钮：`button:h5:workorder:arrive`
- H5 处理按钮：`button:h5:workorder:handle`
- H5 核查按钮：`button:h5:workorder:verify`

实现时需要基于当前 controller 路由，产出一份一对一接口权限映射清单，避免仅停留在示例层。
### 5.7 H5 归属校验保留

H5 的权限判断分两层：

1. 权限校验：当前用户是否具备该动作的 RBAC 权限
2. ownership 校验：当前用户是否就是工单 assignee

两层缺一不可。

因此现有 workorder service 中的归属校验逻辑保留，但当前用户来源改为真实登录态。

### 5.8 与现有 `FoundationActorResolver` 的关系

当前 `FoundationActorResolver` 仅适用于 foundation 阶段伪身份注入，而现有 `WorkOrderServiceImpl`、`ProcessInstanceService` 等核心服务已经直接依赖它。因此本次不能只在控制器层“新增一个当前用户对象”，必须明确服务层迁移策略。

#### 推荐迁移方案

将 `FoundationActorResolver` 升级为统一演员解析入口，但其数据源从旧 Header 模式切换为“认证上下文优先，旧 Header 兜底仅用于过渡”。

具体顺序：

1. 新增统一认证上下文，例如 `AuthenticatedUserContextHolder`
2. Bearer token 认证过滤器在请求进入时填充该上下文
3. `FoundationActorResolver.resolveActor()` 优先读取认证上下文
4. 仅在显式允许的过渡接口或测试场景中，才回退读取 `X-Foundation-*`
5. 等 Web/H5 业务完全切换完成后，再删除 Header 回退逻辑

#### 这样设计的原因

- 现有 service 代码改动面更小
- 可以先把身份来源替换掉，再逐步把命名从 foundation 过渡到真正 auth 语义
- 能降低一次性改写所有 service 签名的风险

#### 最终目标状态

- 所有真实业务操作都从 Bearer token 认证上下文获得当前人
- 客户端提交的 `X-Foundation-*` 不再作为真实身份来源
- `FoundationActorResolver` 后续可重命名为更准确的 `CurrentActorResolver`，但这不是本次必须项
## 6. RBAC 权限矩阵

### 6.1 角色建议

最小可用角色集合：

- `SUPER_ADMIN`
- `EVENT_OPERATOR`
- `AUDITOR`
- `DISPATCHER`
- `H5_WORKER`
- `H5_VERIFIER`

### 6.2 权限建议

#### Web 菜单权限（示例）

- `menu:dashboard:view`
- `menu:event:list`
- `menu:audit:list`
- `menu:process-template:list`
- `menu:workorder:list`
- `menu:patrol-task:list`
- `menu:drone:list`
- `menu:map:view`
- `menu:system:user-role`
- `menu:system:config`

#### Web 按钮权限（示例）

- `button:audit:start`
- `button:audit:approve`
- `button:audit:reject`
- `button:workorder:dispatch`
- `button:workorder:confirm-close`
- `button:workorder:return-processing`
- `button:process-template:create`
- `button:process-template:edit`
- `button:process-template:enable`
- `button:process-template:disable`

#### Web API 权限（示例）

- `api:event:create`
- `api:event:list`
- `api:event:detail`
- `api:audit:start`
- `api:process-instance:approve`
- `api:process-instance:reject`
- `api:workorder:dispatch`
- `api:workorder:confirm-close`
- `api:process-template:list`
- `api:process-template:edit`

#### H5 权限（示例）

- `menu:h5:workbench:view`
- `menu:h5:workorder:list`
- `menu:h5:workorder:detail`
- `button:h5:workorder:accept`
- `button:h5:workorder:arrive`
- `button:h5:workorder:handle`
- `button:h5:workorder:verify`
- `menu:h5:history:view`
- `menu:h5:mine:view`
- `api:h5:workbench:view`
- `api:h5:workorder:list`
- `api:h5:workorder:detail`
- `api:h5:workorder:accept`
- `api:h5:workorder:arrive`
- `api:h5:workorder:handle`
- `api:h5:workorder:verify`
### 6.3 默认角色矩阵

#### `SUPER_ADMIN`

- 全部 Web/H5 权限

#### `EVENT_OPERATOR`

- 事件录入、事件查看

#### `AUDITOR`

- 审核列表、审核详情、审核通过/驳回

#### `DISPATCHER`

- 待派单事件查看、工单派发、办结确认

#### `H5_WORKER`

- H5 登录、工作台、工单列表/详情、接单、到场、处理

#### `H5_VERIFIER`

- H5 登录、核查、历史查看

## 7. Web 前端设计

当前 Web 端并不只是“补一下登录接口”，而是需要新增一整层认证前端基础设施。现状是：

- 路由中只有一个登录占位视图
- 本地仅保存布尔值 `authenticated`
- 没有 auth API 模块
- 没有通用 axios 客户端与请求拦截器
- 没有基于权限码的路由元数据与菜单过滤

因此本次 Web 改造必须显式包含这些基础设施建设。

### 7.1 登录态模型

当前 Web 仅保存布尔值，需要升级为结构化 session：

- `token`
- `userId`
- `userName`
- `account`
- `roleCodes`
- `permissionCodes`

### 7.2 登录页

新增真实登录页组件，替代当前路由中的占位视图。

功能包括：

- 账号密码输入
- 登录调用 `/api/auth/login`
- 登录成功后跳转 redirect 或默认首页
- 错误提示

### 7.3 会话恢复

应用初始化时：

- 若本地存在 token，调用 `/api/auth/me`
- 成功则刷新当前会话与权限集
- 失败则清理本地 session 并跳登录页

### 7.4 路由与菜单权限

每个 Web 路由定义权限码，例如：

- `meta.permission = 'menu:dashboard:view'`

守卫逻辑：

- 未登录：跳登录
- 已登录但无路由权限：跳 403 或默认首页

左侧菜单基于当前用户 `MENU` 权限过滤显示。

### 7.5 按钮权限

页面按钮按权限码显示，例如：

- 审核通过按钮需要 `button:audit:approve`
- 派单按钮需要 `button:workorder:dispatch`
- 办结确认按钮需要 `button:workorder:confirm-close`

按钮权限建议通过轻量工具函数或指令统一判断，避免页面内散落硬编码。

## 8. H5 前端设计

H5 当前虽然已有登录页、结构化 local session、登出函数，但真实认证接入还缺少关键拼图：

- 请求层尚未统一注入 `Authorization: Bearer <token>`
- 没有 `/api/h5/auth/me` 的会话恢复调用
- 当前 `auth.ts` 直接把 axios 返回当作业务对象使用，接真实后端时必须适配 `ApiResponse<T>` 包裹
- 路由与页面动作还没有基于权限码做统一判断

因此 H5 不是“只换个接口地址”，还要同步补齐认证请求层与 session 恢复层。

### 8.1 登录态模型

当前 H5 session 需要增加：

- `userId`
- `roleCodes`
- `permissionCodes`

### 8.2 登录接入

保留现有登录页交互，改为接真实接口：

- `POST /api/h5/auth/login`

登录成功后：

- 保存 token/session
- 跳转工作台或 redirect

### 8.3 会话恢复

启动时：

- 若本地 token 存在，调用 `/api/h5/auth/me`
- 成功则恢复用户信息与权限
- 失败则清空登录态并跳转登录页

### 8.4 请求认证

H5 的 axios 客户端统一增加：

- `Authorization: Bearer <token>`

并移除依赖前端主动构造 `X-Foundation-*` 的做法。

### 8.5 页面动作权限

H5 不做复杂菜单树，但页面动作应按权限码控制：

- 接单按钮：`button:h5:workorder:accept`
- 到场按钮：`button:h5:workorder:arrive`
- 处理提交：`button:h5:workorder:handle`
- 核查提交：`button:h5:workorder:verify`

最终是否允许操作仍以后端鉴权和 ownership 校验为准。

## 9. 迁移与兼容策略

### 9.1 数据库迁移

新增 migration，完成：

- `sys_permission`
- `sys_user_role`
- `sys_role_permission`
- 必要索引与唯一约束
- 初始角色/权限种子数据
- 用户角色初始化数据

### 9.2 测试兼容

现有测试大量依赖 `sys_user.role_id` 或宽松安全策略。为降低重构风险：

- 测试数据允许同时写 `role_id` 与 `sys_user_role`
- 新认证测试独立覆盖
- 旧流程测试逐步切换到 token 认证上下文

### 9.3 接口兼容

为了平滑过渡，可短期保留旧 login 占位前端结构，但后端真实接口将成为唯一有效入口。

真实业务接口目标状态：

- 不再依赖 `X-Foundation-*`
- 必须依赖 Bearer token

## 10. 测试设计

### 10.1 后端测试

至少覆盖：

- Web 登录成功
- H5 登录成功
- 账号不存在/密码错误失败
- 用户状态禁用失败
- 用户无 Web 访问权限时 Web 登录失败
- 用户无 H5 访问权限时 H5 登录失败
- 未登录访问受保护接口返回 401/403
- 已登录但无接口权限返回 403
- H5 有权限但非本人派单工单仍拒绝
- `/api/auth/me` 与 `/api/h5/auth/me` 返回正确身份信息

### 10.2 Web 前端测试

至少覆盖：

- 登录成功后保存 session
- token 自动注入请求头
- 未登录访问受保护路由跳登录
- 登录后菜单按权限过滤
- 按钮按权限显隐
- 登录态失效后自动清理并跳登录

### 10.3 H5 前端测试

至少覆盖：

- 登录成功后保存 session
- token 自动注入请求头
- 未登录访问受保护路由跳登录
- H5 动作按钮按权限显示
- 登录态失效后自动清理并跳登录

## 11. 风险与控制

### 风险 1：安全配置改造影响现有业务测试

控制：

- 先补认证测试
- 再逐步收紧 `SecurityConfig`
- 为测试提供明确的 token/header 构造辅助

### 风险 2：旧单角色模型与新多角色模型并存

控制：

- 明确新逻辑优先读 `sys_user_role`
- 旧字段仅用于过渡兼容
- 后续可再安排一次清理迁移

### 风险 3：前端权限码与后端权限码不一致

控制：

- 在设计与实现中统一维护权限码清单
- 尽量把权限码定义集中到单处或少数文件

## 12. 实施顺序建议

建议按以下顺序实现：

1. 数据库迁移与权限种子数据
2. 后端认证模型、token、当前用户上下文
3. Web/H5 登录接口与 `me` 接口
4. `SecurityConfig` 收紧与接口权限校验
5. H5 业务接口切换到真实认证上下文
6. Web 前端真实登录与 token/session
7. Web 菜单/路由/按钮权限控制
8. H5 前端真实登录与按钮权限控制
9. 全链路测试与回归

## 13. 验收标准

满足以下条件视为完成：

- Web 可通过真实后端登录进入系统
- H5 可通过真实后端登录进入系统
- Web/H5 请求均携带 Bearer token
- 后端能识别当前登录用户并进行权限校验
- 菜单、按钮、接口权限按 RBAC 生效
- H5 仍能正确限制只操作本人工单
- 未登录、无权限、越权访问均被正确拒绝
- 现有 phase-1 主流程仍可正常运行
