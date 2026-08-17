# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

This workspace contains four sibling applications that implement one event-governance platform:

- `backend/` — Spring Boot 3.3 / Java 17 API service on port `8080`, served under `/api`
- `web/` — Vue 3 + TypeScript + Vite admin application on port `5175`（管理人员使用）
- `h5/` — Vue 3 + uni-app mobile field application on port `5174`（网格员使用）
- `mp/` — Vue 3 + TypeScript + Vite mini-program on port `5176`（居民随手拍）
- `docs/architecture/` — delivery contract and manual verification docs for the current phase

Start with `docs/系统说明文档.md` for the current API reference (403 endpoints as of 2026-08-17，其中 V81~V96 新增模块接口清单待补; Flyway V96). `docs/architecture/phase1-endpoints.md` is the historical Phase1 contract, not the current state.

## 生产环境速查（服务器上开发，必读）

本项目运行在本云服务器上，**开发即部署**：改完代码用 docker compose 重建容器验证，然后 git commit + push 同步 GitHub（远程 `git@github.com:lxxyyy29/lsc.git`，master，SSH 免密）。宿主机装有 JDK 17 和 Maven（早期说"无 Java/Node 环境"已过时）；注意后端 Dockerfile 只 COPY 本地 `backend/target/*.jar`，**必须先 `cd backend && mvn clean package -DskipTests` 再 build 镜像**，否则打进去的是旧 jar（构建全走缓存是未重新打包的信号）。宿主机没有 Node 环境，前端编译交给 Docker 构建。

### 部署命令（在 `docker/` 目录）

```bash
docker compose build <服务名>          # 重建镜像（服务名: changping-backend/web/h5/mp）
docker compose up -d --force-recreate <服务名>   # 重建后重启
docker ps --filter name=changping      # 查看状态
```

端口映射（`docker/.env`）：后端 10081→8080、web 8888→80、h5 10082→80、mp 10083→80、**HTTPS 聚合入口 8443→443（changping-web）**。

### 访问地址

| 端 | HTTP（兼容） | HTTPS（精确定位需要） |
|---|---|---|
| 管理端 | http://8.156.93.151:8888 | https://drone.kfktec.cn:8443/ |
| H5 移动端 | http://8.156.93.151:10082/h5/ | https://drone.kfktec.cn:8443/h5/ |
| 居民端 | http://8.156.93.151:10083 | https://drone.kfktec.cn:8443/mp/ |

域名 `drone.kfktec.cn` 是借用服务器上无人机老项目的（仅开发阶段借挂，交付时客户自购域名切换：换 docker/ssl/ 证书 + nginx server_name + .env DOMAIN + 重建容器）。证书 2026-08-23 到期。443 端口被老项目 dgcp-web-nginx 占用，勿动。

### 测试账号

- 管理端：admin / admin123
- H5 网格员：grid01~grid06 / 123456（登录端点 `/api/h5/auth/login`，不要用 WEB 端点）
- MySQL：容器 changping-mysql，库名 **zhsq**（不是 changping），root 密码在 `docker/.env`；sys_user 表用户名字段是 `username`

### 近期关键实现（2026-08）

- **HTTPS 聚合入口**：`docker/nginx-web.conf` 同时监听 80/443，代理 `/h5/` `/mp/` 子入口；这两个 location 必须带 `^~`（否则静态资源被正则 location 拦截 404）；后端 CORS 白名单在 SecurityConfig.java，新增域名需同步
- **定位**：浏览器精确定位仅 HTTPS 可用；H5 工具在 `h5/src/utils/geolocation.ts`（navigator.geolocation + WGS84→GCJ02 → 高德 IP 定位回退），web 端同款工具在 `web/src/utils/geolocation.ts`；AMap 2.0 的 CitySearch 只有 `getLocalCity`（1.x 的 getLocalPosition 已移除）
- **媒体文件**：扁平存储 `/media/files/{filename}`，公网 URL 前缀由 `docker/.env` 的 `DOMAIN` 拼接（当前为 HTTPS 域名）；上传目录挂载 `/home/docker/uav/changping/uploads`
- **网格数据**：cmn_grid（grid_level 1=社区/2=大网格/3=小网格，roiJson 存边界）；H5 专用接口 `/community/grids/h5/tree`、`/community/grids/h5/my-grid`、`/events/h5/map-points`（WEB 专属接口 H5 令牌会被拒，勿混用）

### 硬性约束

- 只允许修改 `/opt/zhsq` 目录；服务器上还有其他老项目在跑，严禁影响（如 443 端口的 dgcp-web-nginx、/home/docker/uav 下的其他服务，只读不写）
- 文档不可全信，以代码和运行时实际状态为准
- 私钥类文件（docker/ssl/）不入库，已在 .gitignore

## Common commands

Run commands from each app directory.

### Backend (`backend/`)

- Start dev server: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw -Dtest=PermissionGuardTest test`
- Package app: `./mvnw package`

### Web admin (`web/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5175`
- Build production bundle: `npx pnpm build`

### H5 (`h5/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5174`
- Run all tests: `npx pnpm test`（`src/tests/http.spec.ts`、`src/tests/navigation.spec.ts`）
- Build production bundle: `npx pnpm build`

### Mini-program (`mp/`)

- Start dev server: `npx pnpm dev --host 0.0.0.0 --port 5176`
- Build production bundle: `npx pnpm build`

Notes:

- H5 tests run through `node ./scripts/run-vitest.mjs`, which forwards extra CLI args to `vitest run`.
- This environment may not expose a global `pnpm` binary, so prefer `npx pnpm ...`.
- The backend currently has a single test class: `backend/src/test/java/com/changping/platform/modules/auth/security/PermissionGuardTest.java`.

## High-level architecture

### Core business flow

The main product flow spans all four apps:

1. event intake
2. audit start and process-node approval
3. dispatch to a work order
4. H5 assignee accepts / arrives / handles / verifies
5. Web confirms close or rejects back to processing

To understand a feature, read across backend controllers/services plus the corresponding `web/` or `h5/` API layer and views. This is a cross-app workflow, not three isolated codebases.

### Backend structure

Backend entry point:

- `backend/src/main/java/com/changping/platform/Application.java`

Important backend modules:

- `modules/auth` — login, current-user lookup, JWT, permission checks, client-type separation for Web vs H5
- `modules/event` — event intake and event detail
- `modules/audit` — starting audit against an event
- `modules/process` — process templates, process instances, approve/reject node transitions
- `modules/workorder` — dispatch, H5 assignee actions, close confirmation
- `common/response` — unified API response envelope
- `common/exception` — global exception normalization
- `common/security` — security config and actor resolution

Important implementation detail: although MyBatis-Plus is present in `pom.xml`, much of the business logic is implemented with explicit SQL and state updates in service classes, especially work-order flow logic. Do not assume a JPA-style repository architecture.

Files to inspect first for backend behavior:

- `backend/src/main/java/com/changping/platform/common/security/SecurityConfig.java`
- `backend/src/main/java/com/changping/platform/modules/auth/security/BearerTokenAuthenticationFilter.java`
- `backend/src/main/java/com/changping/platform/modules/auth/security/PermissionGuard.java`
- `backend/src/main/java/com/changping/platform/modules/workorder/service/impl/WorkOrderServiceImpl.java`
- `backend/src/main/resources/db/migration/`

### Web admin structure

Web app bootstrap:

- `web/src/main.ts`

Routing and access control:

- `web/src/router/index.ts`

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Element Plus is the UI library
- `AdminShellLayout` hosts route-driven admin pages

Main Web domains include dashboard, big screen, events, audits, process templates, work orders, patrol tasks, drones, map oversight, org members, biz areas, resident reports, policy resources, and system configuration.

### H5 structure

H5 app bootstrap:

- `h5/main.ts`

Pages and bottom tab bar:

- `h5/pages.json` (uni-app page registry)
- `h5/src/navigation.ts` (navigation entries gated by `menu:h5:*` permissions)

Key patterns:

- session is recovered before app mount
- router guards enforce auth and menu-permission access
- Vant is the UI library
- `MobileShellLayout` hosts the main field workflow

Main H5 domains include workbench, work-order list/detail, verification, history, patrol checkin, merchant/vendor management, map, message (chat entry hidden for now), and mine/profile.

### Mini-program (居民小程序) structure

Mini-program app bootstrap:

- `mp/src/main.ts`

Routing and access control:

- `mp/src/router/index.ts`

Key patterns:

- Bottom tab bar navigation (report/history/mine)
- Session stored in localStorage as `grid-mp-session`
- Uses same JWT bearer token flow as Web
- Proxy `/api` → `http://localhost:8080` via Vite config

Main mini-program features: resident report (随手拍), report history with status tracking, event rating (1-5 stars), community services (activities/repairs/policies/points), user profile.

## Auth and permission model

The current steady-state auth model is bearer-token based:

- Web login: `POST /api/auth/login`
- H5 login: `POST /api/h5/auth/login`
- Mini-program login: `POST /api/auth/login` (with clientType: web)
- Public report (免认证): `POST /api/events/public-report`
- protected requests send `Authorization: Bearer <token>`

Important conventions:

- backend responses use a unified envelope: `success`, `code`, `message`, `data`
- Web and H5 are separate client types with separate permission surfaces
- frontend navigation is gated by `menu:*` permissions
- backend API access is gated by explicit permission checks
- H5 work-order actions also enforce assignee ownership in the service layer

The legacy `X-Foundation-*` headers still exist only as a fallback/testing path; use the bearer-token flow for real integration work.

## Data and schema notes

- Backend config lives in `backend/src/main/resources/application.yml`
- Flyway migrations in `backend/src/main/resources/db/migration/` are the source of truth for schema evolution
- Treat `application.yml` as sensitive: it contains concrete fallback connection settings and secrets that should not be copied into docs, summaries, or commits

## Testing and verification entry points

Useful references:

- `docs/系统说明文档.md` — current full API reference (updated 2026-08-17；接口清单收录 315/403，V81~V96 新增模块待补)
- `docs/architecture/phase1-endpoints.md` — historical Phase1 API contract and workflow mapping
- `docs/architecture/phase1-verification-checklist.md` — manual verification checklist and known gaps

Existing backend test:

- `backend/src/test/java/com/changping/platform/modules/auth/security/PermissionGuardTest.java`

H5 page tests:

- `h5/src/tests/` (run via `npx pnpm test`)

## UI and styling conventions

All frontend apps import shared Figma-oriented tokens:

- `web/src/styles/figma-tokens.css`
- `h5/src/styles/figma-tokens.css`

There are also Figma/design-derived templates and planning artifacts under `docs/superpowers/`, but those are supporting design/history materials rather than the runtime source of truth.
