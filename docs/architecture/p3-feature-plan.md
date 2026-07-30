# P3 功能规划文档

> 生成时间：2026-07-30
> 前置条件：P1/P2 全部完成

---

## 一、现状盘点

### 已有能力（无需新建）

| 功能 | 现状 | 文件 |
|------|------|------|
| Excel 导出 | ✅ 已有 5 个导出端点（事件/场所/人口/房屋/巡查） | `ExportController.java` + `ExportService.java` |
| 定时任务 | ✅ 已有 2 个调度（事件紧急度升级 + 巡查任务生成） | `EventAutoEscalationTask` + `PatrolTaskScheduler` |
| POI 依赖 | ✅ `poi-ooxml 5.2.5` 已在 pom.xml | — |

### 缺失能力（P3 需建设）

| 功能 | 现状 | 优先级 |
|------|------|--------|
| Excel 导入 | ❌ 完全没有导入接口，数据只能逐条录入 | 🔴 高 |
| 站内通知 | ❌ 无消息中心，超期/待办无感知 | 🔴 高 |
| 接口限流 | ❌ 无保护，核心接口可被恶意刷 | 🟡 中 |
| 操作手册 | ❌ 无帮助文档 | 🟢 低 |

---

## 二、P3-1：数据导入导出增强

### 2.1 目标

在现有导出能力基础上，**补齐导入能力**，并增强导出的灵活性（按筛选条件导出）。

### 2.2 后端新增接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/community/import/population` | POST | 导入实有人口（Excel） |
| `/api/community/import/buildings` | POST | 导入房屋数据（Excel） |
| `/api/community/import/places` | POST | 导入场所/商户（Excel） |
| `/api/community/import/template/{type}` | GET | 下载导入模板 |
| `/api/community/import/preview` | POST | 预览导入数据（不实际写入） |

### 2.3 导入流程

```
上传 Excel → 解析 → 数据校验 → 预览（显示成功/失败行）→ 确认导入 → 写入 DB
                                                              ↓
                                                        失败行导出错误报告
```

### 2.4 导入字段设计

**实有人口（population）**：
- 必填：姓名、身份证号、手机号
- 可选：户籍类型、地址、网格、标签

**房屋（buildings）**：
- 必填：楼栋编号、地址
- 可选：房东姓名、房东电话、消防风险等级、是否群租

**场所/商户（places）**：
- 必填：场所名称、负责人
- 可选：负责人电话、备注

### 2.5 技术方案

- 复用 `ExportService` 的 POI 依赖，新增 `ImportService`
- 使用 `multipart/form-data` 接收文件
- 校验规则：必填项、身份证格式、手机号格式、重复检测
- 预览模式：解析前 100 行返回前端展示
- 正式导入：事务批量插入，失败行收集错误原因

### 2.6 前端

- 在各基础数据管理页面（人口/房屋/场所）增加"导入"按钮
- 导入弹窗：上传文件 → 下载模板 → 预览 → 确认
- 导入结果提示：成功 N 条，失败 N 条（可下载错误报告）

---

## 三、P3-2：站内通知提醒

### 3.1 目标

建立站内消息中心，支持：
- 工单超期提醒
- 审核/派单待办通知
- 系统公告

### 3.2 数据库表

```sql
CREATE TABLE sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收人',
    title VARCHAR(128) NOT NULL,
    content TEXT,
    type VARCHAR(32) NOT NULL COMMENT 'WORK_ORDER/AUDIT/SYSTEM',
    level VARCHAR(16) DEFAULT 'NORMAL' COMMENT 'URGENT/NORMAL/LOW',
    related_type VARCHAR(32) COMMENT '关联业务类型',
    related_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT DEFAULT 0,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_created (created_at)
);
```

### 3.3 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/notifications` | GET | 分页查询当前用户通知 |
| `/api/notifications/unread-count` | GET | 未读数量（角标） |
| `/api/notifications/{id}/read` | POST | 标记已读 |
| `/api/notifications/read-all` | POST | 全部已读 |
| `/api/notifications/{id}` | DELETE | 删除通知 |

### 3.4 触发点

| 事件 | 通知对象 | 内容 |
|------|---------|------|
| 工单派发 | 被派单网格员 | "您有新工单待处理" |
| 工单超期 | 被派单网格员 + 管理员 | "工单 #xxx 已超期 N 小时" |
| 事件待审核 | 管理员 | "有新事件待审核" |
| 复核驳回 | 处置网格员 | "工单被驳回，请重新处理" |

### 3.5 定时任务增强

新增 `NotificationScheduler`：
- 每小时检查工单超期 → 生成通知
- 每天汇总未读数量（可后续接推送）

### 3.6 前端

- 顶部导航栏增加消息铃铛图标 + 未读角标
- 下拉面板展示最近 10 条通知
- 点击跳转关联业务详情
- 独立"消息中心"页面（可挂载到数据决策菜单下）

---

## 四、P3-3：接口限流

### 4.1 目标

保护核心业务接口，防止恶意刷接口和暴力破解。

### 4.2 限流策略

| 接口类别 | 限流阈值 | 说明 |
|---------|---------|------|
| 登录接口 | 5 次/分钟/账号 | 防暴力破解 |
| 事件上报 | 30 次/分钟/IP | 防恶意上报 |
| 公开接口 | 60 次/分钟/IP | 一般保护 |
| 其他接口 | 120 次/分钟/用户 | 常规限制 |

### 4.3 技术方案

**方案选择**：基于 Redis 令牌桶（利用已有 Redis 依赖）

- 使用 Redis + Lua 脚本实现原子化令牌桶
- 限流失败返回 `HTTP 429 Too Many Requests`
- 响应头携带 `X-RateLimit-Limit` / `X-RateLimit-Remaining` / `X-RateLimit-Reset`

### 4.4 新增组件

| 文件 | 说明 |
|------|------|
| `RateLimitConfig.java` | 限流配置（阈值可配置化） |
| `RateLimitAspect.java` | AOP 切面，拦截带 `@RateLimit` 注解的接口 |
| `RateLimitInterceptor.java` | 或改用 HandlerInterceptor 全局拦截 |
| `RateLimitException.java` | 限流异常 |

### 4.5 注解设计

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int limit() default 60;       // 请求数
    int window() default 60;      // 时间窗口（秒）
    RateLimitType type() default RateLimitType.USER;  // USER/IP
    String message() default "请求过于频繁，请稍后重试";
}
```

---

## 五、P3-4：操作手册（帮助文档）

### 5.1 轻量方案

在 Web 端内嵌"帮助中心"页面，按模块展示操作指南：
- 事件处置流程说明
- 工单操作指南
- 常见问题 FAQ

### 5.2 前端

- 顶部导航栏增加"?"帮助入口
- 独立 `HelpView.vue` 页面
- 按菜单分组展示帮助内容
- 内容硬编码或从 markdown 文件加载

---

## 六、实施顺序建议

```
P3-1 数据导入（最高频需求，基础数据录入效率提升）
  ↓
P3-2 站内通知（提升用户体验，闭环处置感知）
  ↓
P3-3 接口限流（安全保障，可稍后）
  ↓
P3-4 操作手册（锦上添花）
```

---

## 七、工作量估算

| 项 | 后端 | 前端 | 总计 |
|----|------|------|------|
| P3-1 数据导入 | ~3h | ~2h | ~5h |
| P3-2 站内通知 | ~4h | ~3h | ~7h |
| P3-3 接口限流 | ~2h | 0 | ~2h |
| P3-4 操作手册 | 0 | ~2h | ~2h |
| **合计** | **~9h** | **~7h** | **~16h** |

---

## 八、新增依赖

P3 无需新增 Maven 前端依赖：
- POI 已存在（导入导出）
- Redis 已存在（限流 + 通知缓存）
- 定时任务用已有 `@Scheduled`
