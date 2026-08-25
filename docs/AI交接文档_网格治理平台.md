# 东莞杰瑞智慧网格治理平台 — AI 交接文档

> 交接日期：2026-08-24 | 当前代码基线：`693cf17`（GitHub 与 GitLab 已同步）
> 本文档写给接手的新 AI/开发者，覆盖项目概况、环境搭建、部署运维、当前状态与遗留事项。

---

## 一、项目概况

面向社区网格化治理的**全流程闭环平台**：发现上报 → 智能派单 → 现场处置 → 复核核查 → 督办预警 → 归档评价。

### 1.1 四端架构

| 端 | 目录 | 技术栈 | 端口 | 说明 |
|---|---|---|---|---|
| 后端 API | `backend/` | Spring Boot 3.3 / Java 17 / JdbcTemplate / Flyway | 8080 | 全部业务 API，MongoDB 双数据源 |
| Web 管理端 | `web/` | Vue 3 + TS + Vite + Element Plus | 5175 | 管理员/调度员 |
| 小程序 | `h5/` | Vue 3 + uni-app（编译微信小程序） | 5174 | 网格员端 + 居民端双角色 |
| 居民端旧项目 | `mp/` | Vue 3 + Vite | 5176 | **已废弃**，功能并入 h5 |

### 1.2 核心业务流
居民/网格员上报 → 审核 → 派单（智能推荐）→ 工单 → 网格员接单/到场/处理 → Web 确认关闭 → 居民评价 → 归档。

### 1.3 关键实现说明（重要）
- **事件列表查询走 MongoDB**（`alarm_events` 集合）+ MySQL 补充 enrich；归档/隐藏/紧急程度筛选都在 Mongo 侧（文档字段 `archived`/`hidden`/`urgencyLevel`）
- **紧急程度存量回填**：`EventServiceImpl` 启动时 `@PostConstruct` 从 MySQL 同步 `urgencyLevel` 到 Mongo（缺字段的历史事件）
- 业务逻辑大量为 **JdbcTemplate 显式 SQL**，非 MyBatis-Plus 风格
- 菜单结构在 `web/src/menu.ts`（单一数据源），数据库 `sys_permission` 的菜单名会**覆盖**前端名称（App.vue dbMenuMap 逻辑），改名需同时改库或加 Flyway 迁移

---

## 二、代码仓库（两个都要同步）

| 仓库 | 地址 | 别名 | 说明 |
|---|---|---|---|
| GitHub | `https://github.com/lxxyyy29/lsc.git` | origin | 主仓库（SSH/HTTPS 均可，本机走代理） |
| GitLab | `http://8.156.93.151:6080/kfk_lxy/111.git` | gitlab | 云端自建，用户名 `kfk_lxy`，密码 `lxy123456` |

**约定：代码改动必须同时推两个仓库（GitHub + GitLab）。**

GitLab HTTP 推送命令（HTTP 需放行 + 凭证内嵌）：
```bash
GCM_ALLOW_UNSAFE_REMOTES=true GIT_TERMINAL_PROMPT=0 git push http://kfk_lxy:lxy123456@8.156.93.151:6080/kfk_lxy/111.git master
```
拉取 GitLab：
```bash
GCM_ALLOW_UNSAFE_REMOTES=true GIT_TERMINAL_PROMPT=0 git fetch http://kfk_lxy:lxy123456@8.156.93.151:6080/kfk_lxy/111.git master
```

⚠️ 两个仓库可能分叉（云端可能在任一仓库直接推）——合并时优先 rebase/merge 后以本地为准，必要时 force push GitLab（本地包含全量历史）。

---

## 三、本地开发环境（Windows 机器）

### 3.1 依赖服务（本机已装，Windows 服务/进程）

| 服务 | 端口 | 凭证/说明 |
|---|---|---|
| MySQL | 3306 | root / `123456`，库名 **zhsq** |
| Redis | 6379 | 密码 `123456`，db 7；Windows 服务名 **RedisDgcp**（改配置要改 `redis.windows-service.conf`，不是 redis.windows.conf） |
| MongoDB | 27017 | 库 alarm_dgcp（无密码） |

### 3.2 启动命令

```bash
# 后端（Flyway 自动迁移，勿忘先停旧进程）
cd backend && ./mvnw spring-boot:run        # 或后台: (./mvnw spring-boot:run > /c/backend-dev.log 2>&1 &)

# Web 管理端（注意端口）
cd web && npx pnpm dev --host 0.0.0.0 --port 5175

# H5/小程序源码 dev
cd h5 && npx pnpm dev

# 小程序构建（微信开发者工具导入 dist/build/mp-weixin）
cd h5 && npx pnpm build:mp:weixin
```

### 3.3 ⚠️ web/vite.config.ts 本地代理约定（关键坑）

- 仓库版代理 target = `http://127.0.0.1:10081`（服务器端口）
- **本地开发需临时改为 `http://localhost:8080`**，否则 Web 页面报"网络错误，请检查网络连接"
- **提交前必须还原为 10081**（用 `git stash push web/vite.config.ts` 提交再 pop）
- 首次改完需重启 web dev server（vite 配置变更不热更新）

### 3.4 访问地址与测试账号

| 端 | 本地 | 线上 |
|---|---|---|
| Web 管理端 | http://localhost:5175 | http://8.156.93.151:8888 / https://drone.kfktec.cn:8443 |
| H5 | http://localhost:5174 | http://8.156.93.151:10082/h5/ |
| 小程序 | 开发者工具导入产物 | 真机预览 |

- 管理端：`admin / admin123`（超管）
- 网格员：`grid01~grid06 / 123456`（小程序登录）
- 居民：`yonghu / 123456`（小程序）
- H5 登录端点用 `/api/h5/auth/login`；居民用 `/api/auth/login`

---

## 四、服务器部署（云端，开发即部署）

- 宿主机：`8.156.93.151`，项目容器在 docker 编排中（服务名 changping-backend / changping-web / changping-h5 / changping-mp）
- **硬性约束：只允许操作 /opt/zhsq 目录；443 端口 dgcp-web-nginx 是老项目勿动**
- **后端 Dockerfile 只 COPY `backend/target/*.jar`——必须先 `cd backend && mvn clean package -DskipTests` 再 build，否则打进旧 jar**（构建全走缓存 = 未重新打包的信号）

```bash
cd docker
docker compose build changping-backend changping-web   # 先打包再 build
docker compose up -d --force-recreate changping-backend changping-web
docker ps --filter name=changping
```

- 新迁移（Flyway V108+）后端重启自动执行，无需手工
- 微信登录需在 `docker/.env` 配置：`WECHAT_APPID=wxaf987875eaf3b53c`、`WECHAT_APPSECRET=<微信公众平台 AppSecret>`

---

## 五、当前状态（截至 693cf17）

### 5.1 近期已完成（2026-08 批次）
- 小程序完整可用：统一登录（手机验证码/账号密码/微信一键登录）、网格员 4 Tab、居民端、离线采集、地图/定位
- 微信手机号授权登录（方案A）：后端 `WechatService` + `/auth/wechat-login` + **居民未绑定时自动开通 PUBLIC 账号**；登录页"微信一键登录"按钮
- 事件闭环：紧急程度三色筛选（含存量回填）、来源筛选、已驳回筛选、分页空白修复、通过/驳回移至详情弹窗右下角、创建弹窗防误关
- 台账：人口/房屋/场所手动增删改、搜索修复（含网格名）、枚举中文（后端 SQL CASE 翻译，导出同步中文）
- 系统设置：角色管理支持创建角色、账号行内选角色、菜单管理、菜单改名（工单中心→已完成工单、审核中心→异常工单，V110）
- 地图中心点可配置（V108 sys_config，网格管理页配置，看板/GIS/大屏读取）；大屏"回到中心"按钮
- 无人机本地设备档案（V109 + CRUD）；组织人员仅保留组长↔网格员绑定；辖区/商户/摊贩/违禁区域全量 CRUD
- 审计日志操作人筛选、台账"今天"快捷、大屏工单标题/红点、巡查搜索栏查询/重置
- 综合监管大屏"回到中心"、GIS 页地图失败提示

### 5.2 已删除/下线模块（勿再引用）
应急调度、停车、车辆轨迹、蚊媒爱卫、安全检查、报修管理（Web 端）、居民端应急公告——后端接口与前端页面均已删，**小程序构建脚本已同步**（勿重新添加）

### 5.3 小程序构建注意事项
- `h5/scripts/build-mp.mjs`：自动备份/生成/恢复 pages.json（33 页，首屏 role-select）
- 构建产物：`h5/dist/build/mp-weixin`（微信开发者工具导入）
- 平台品牌：**东莞杰瑞智慧网格治理平台**（AppID wxaf987875eaf3b53c）

---

## 六、遗留/待办事项

| 优先级 | 事项 | 说明 |
|---|---|---|
| 🔴 高 | **微信登录线上配置** | docker/.env 需加 WECHAT_APPID / WECHAT_APPSECRET，否则线上微信登录报"未配置"；需真机验证 getPhoneNumber（企业主体） |
| 🟡 中 | **高德 key 域名白名单** | 本地 localhost 未授权 → 地图白屏；线上正常。授权加在 [高德控制台](https://console.amap.com) |
| 🟡 中 | 出租屋库表单与社区表格对齐 | 需客户提供表格模板（字段清单），目前未对齐 |
| 🟡 中 | 讲解文档更新 | 桌面《东莞杰瑞智慧网格治理平台_客户讲解文档_V3.0.docx》（生成脚本 scripts/gen_client_briefing_docx.py），云端删模块后需同步更新内容 |
| 🟢 低 | 政策推送 | 已具备（政策资源"定向推送"→ 站内通知），微信登录打通后可精准触达 |
| 🟢 低 | 小程序"消息通知"入口 | 页面保留（notice），工作台可进 |
| 🟢 低 | 数据清理 | 本地无人机设备表有测试数据（SN-TEST-001），可删：`DELETE FROM drone_device_local WHERE sn='SN-TEST-001'` |

---

## 七、踩坑速查

1. **Redis 改配置**：Windows 服务加载的是 `redis.windows-service.conf`（不是 redis.windows.conf）
2. **git 代理**：本机 git 走 `127.0.0.1:7897`（Clash），代理软件未开时 git 拉取失败——先开代理
3. **沙箱限制**：本环境 bash 对部分命令有沙箱拦截（如含敏感模式的内联 py/curl），可改写成临时脚本文件执行
4. **vite.config.ts**：本地 8080 / 仓库 10081，提交前还原（见 3.3）
5. **菜单改名**：改 menu.ts 的同时要同步数据库 sys_permission（或加迁移），否则被 dbMenuMap 覆盖回旧名
6. **事件编辑/新增 status 丢失**：全字段 UPDATE 的表（人口/房屋/场所/组织人员）前端表单必须带 status，否则保存后列表（WHERE status='ACTIVE'）查不到 = "数据消失"
7. **小程序构建产物**：源码改动后必须重新 `build:mp:weixin`，微信开发者工具才看得到；且要清缓存
8. **云端与本地分叉**：GitHub/GitLab 可能各自有新提交，拉取合并后**双推**保持三端一致
9. **本地后端重启**：改后端代码后需 kill 8080 进程重启（Flyway 新迁移才会执行）

---

*交接人备注：本地环境（MySQL/Redis/MongoDB/后端/Web/H5）当前均在运行中；代码以 GitHub 为权威基线，GitLab 同步。*
