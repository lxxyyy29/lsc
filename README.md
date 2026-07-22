# 东莞常平智慧城管低空巡检平台

本仓库包含东莞常平低空巡检事件治理平台的三个应用，分别面向后端接口、Web 管理端和 H5 移动端。三个应用共同支撑事件接入、审核、派单、现场处置、核查和闭环确认流程。

## 项目结构

- `backend/`：Spring Boot 3.3 后端服务，Java 17，默认端口 `8080`，接口统一挂载在 `/api`
- `web/`：Vue 3 + TypeScript + Vite Web 管理端，默认端口 `5173`
- `h5/`：Vue 3 + TypeScript + Vite/uni-app H5 移动端，默认端口 `5174`
- `docs/architecture/`：接口契约和人工验收说明

## 环境要求

- JDK 17
- Node.js
- Maven Wrapper：后端已包含 `mvnw` / `mvnw.cmd`
- pnpm：如本机未全局安装，可使用 `npx pnpm ...`

## 启动服务

建议分别打开三个终端，在对应目录执行命令。

### 后端

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd backend
./mvnw spring-boot:run
```

启动后接口地址为：

```text
http://localhost:8080/api
```

### Web 管理端

```bash
cd web
npx pnpm install
npx pnpm dev
```

启动后访问：

```text
http://localhost:5173
```

### H5 移动端

```bash
cd h5
npx pnpm install
npx pnpm dev
```

启动后访问：

```text
http://localhost:5174
```

## 构建与测试

### 后端

Windows:

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
```

macOS / Linux:

```bash
cd backend
./mvnw test
./mvnw package
```

### Web 管理端

```bash
cd web
npx pnpm test
npx pnpm build
```

### H5 移动端

```bash
cd h5
npx pnpm test
npx pnpm build
```

## 主要业务流程

平台核心流程覆盖：

1. 事件接入
2. 审核发起与流程节点审批
3. 派发工单
4. H5 端接单、到达、处置、核查
5. Web 端确认闭环或驳回处理

## 认证与权限

- Web 登录接口：`POST /api/auth/login`
- H5 登录接口：`POST /api/h5/auth/login`
- 受保护接口使用 `Authorization: Bearer <token>`
- Web 与 H5 使用独立的客户端权限面
- 前端菜单通过 `menu:*` 权限控制显示和访问
- 后端接口通过显式权限校验控制访问

## 交付文档

- 接口契约：`docs/architecture/phase1-endpoints.md`
- 人工验收清单：`docs/architecture/phase1-verification-checklist.md`

## 配置说明

后端主配置位于 `backend/src/main/resources/application.yml`。实际部署时请根据客户环境配置数据库、MongoDB、Redis、对象存储和第三方接口参数。

Flyway 数据库迁移脚本位于：

```text
backend/src/main/resources/db/migration/
```
