# 网格社区治理平台（拔蛟窝智慧网格）

本仓库包含网格社区治理平台的四个应用，分别面向后端接口、Web 管理端、H5 移动端和居民小程序。四个应用共同支撑"发现上报→智能派单→现场处置→复核核查→督办预警→归档"全流程闭环管理。

## 项目结构

- `backend/`：Spring Boot 3.3 后端服务，Java 17，默认端口 `8080`，接口统一挂载在 `/api`
- `web-v2/`：Vue 3 + TypeScript + Vite Web 管理端，默认端口 `5175`
- `h5/`：Vue 3 + uni-app H5 移动端，默认端口 `5174`
- `mp_mysql_test/`：Vue 3 + TypeScript + Vite 居民小程序，默认端口 `5176`
- `docs/`：项目文档
  - `docs/系统说明文档.md`：完整接口文档
  - `docs/项目进度记录.md`：项目进度记录
  - `docs/api/apifox-openapi.json`：可导入 Apifox 的 OpenAPI 文件
  - `docs/architecture/`：接口契约和人工验收说明

## 环境要求

- JDK 17
- Node.js 18+
- Maven Wrapper：后端已包含 `mvnw` / `mvnw.cmd`
- pnpm：如本机未全局安装，可使用 `npx pnpm ...`
- MySQL 9.7
- MongoDB 6.0
- Redis 7
- MinIO

## 启动服务

建议分别打开四个终端，在对应目录执行命令。

### 后端

```bash
cd backend
./mvnw spring-boot:run
```

启动后接口地址：`http://localhost:8080/api`

### Web 管理端

```bash
cd web-v2
npx pnpm install
npx pnpm dev --host 0.0.0.0 --port 5175
```

启动后访问：`http://localhost:5175`

### H5 移动端

```bash
cd h5
npx pnpm install
npx pnpm dev --host 0.0.0.0 --port 5174
```

启动后访问：`http://localhost:5174`

### 居民小程序

```bash
cd mp_mysql_test
npx pnpm install
npx pnpm dev --host 0.0.0.0 --port 5176
```

启动后访问：`http://localhost:5176`

## 构建与测试

### 后端

```bash
cd backend
./mvnw test
./mvnw package
```

### Web 管理端

```bash
cd web-v2
npx pnpm test
npx pnpm build
```

### H5 移动端

```bash
cd h5
npx pnpm test
npx pnpm build
```

### 居民小程序

```bash
cd mp_mysql_test
npx pnpm build
```

## 主要业务流程

平台核心流程覆盖：

1. 事件接入（群众随手拍、系统上报）
2. 审核发起与流程节点审批
3. 派发工单给网格员
4. H5 端接单、到达、处置、核查
5. Web 端确认闭环或驳回处理
6. 三色分级（绿/黄/红）和超期自动升级

## 认证与权限

- Web 登录接口：`POST /api/auth/login`
- H5 登录接口：`POST /api/h5/auth/login`
- 小程序登录：`POST /api/auth/login`（clientType: web）
- 受保护接口使用 `Authorization: Bearer <token>`
- Web 与 H5 使用独立的客户端权限面
- 前端菜单通过 `menu:*` 权限控制显示和访问
- 后端接口通过显式权限校验控制访问

## 测试账号

| 账号 | 密码 | 角色 | 端 |
|------|------|------|------|
| admin | admin123 | 系统管理员 | Web |
| grid01-grid05 | grid123 | 网格员 | H5 |
| yonghu | 123456 | 普通群众 | 小程序 |

## 配置说明

后端主配置位于 `backend/src/main/resources/application.yml`。实际部署时请根据环境配置数据库、MongoDB、Redis、对象存储和第三方接口参数。

环境变量配置（可在 `.env` 文件中设置）：

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_URL` | MySQL 连接地址 | `jdbc:mysql://127.0.0.1:3306/zhsq` |
| `DB_USERNAME` | MySQL 用户名 | `root` |
| `DB_PASSWORD` | MySQL 密码 | `123456` |
| `MONGODB_HOST` | MongoDB 主机 | `127.0.0.1` |
| `REDIS_HOST` | Redis 主机 | `127.0.0.1` |
| `JWT_SECRET` | JWT 签名密钥 | 开发用默认值 |
| `OSS_ENDPOINT` | MinIO 地址 | `http://127.0.0.1:9009/` |
| `APP_TEST_ENABLED` | 是否启用测试接口 | `false`（生产环境务必不设置） |

Flyway 数据库迁移脚本位于：`backend/src/main/resources/db/migration/`

## 交付文档

- 完整接口文档：`docs/系统说明文档.md`
- 项目进度记录：`docs/项目进度记录.md`
- Apifox 接口文件：`docs/api/apifox-openapi.json`
- 接口契约：`docs/architecture/phase1-endpoints.md`
- 人工验收清单：`docs/architecture/phase1-verification-checklist.md`

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.3 / Java 17 |
| 数据库 | MySQL 9.7（业务数据） |
| 缓存 | Redis 7（会话/令牌缓存） |
| 文档数据库 | MongoDB 6.0（告警事件） |
| 对象存储 | MinIO（文件/图片/视频） |
| 数据库迁移 | Flyway |
| 认证 | JWT Bearer Token |
| 前端框架 | Vue 3 + TypeScript + Vite |
| UI 库 | Web端: Element Plus / H5端: Vant + uni-app |
| 地图 | 高德地图 AMap JSAPI 2.0 |

---

*最后更新：2026-07-28*
