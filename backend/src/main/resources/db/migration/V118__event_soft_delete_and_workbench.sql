-- ============================================================
-- V118: 事件工单整理——四类工单（闭环处置/事件审核/已完成/异常）+ 事件软删除
-- 说明：
--   1. biz_event 增加软删除字段（deleted / deleted_reason / deleted_at），
--      事件删除改为标记删除，异常工单可查询删除记录及删除原因
--   2. 菜单调整：
--      - web:menu:audits 名称改回"事件审核"（V110 曾改为"异常工单"）
--      - 停用 web:menu:dispatch-rules（删除智能派单规则页面）
--      - 新增 web:menu:abnormal-orders（异常工单）
-- ============================================================

-- 1. biz_event 软删除字段（幂等：列已存在则跳过）
SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'biz_event'
       AND COLUMN_NAME = 'deleted') = 0,
    'ALTER TABLE biz_event ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''软删除标记 0正常 1已删除'' AFTER hidden',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'biz_event'
       AND COLUMN_NAME = 'deleted_reason') = 0,
    'ALTER TABLE biz_event ADD COLUMN deleted_reason VARCHAR(500) NULL COMMENT ''删除原因'' AFTER deleted',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'biz_event'
       AND COLUMN_NAME = 'deleted_at') = 0,
    'ALTER TABLE biz_event ADD COLUMN deleted_at DATETIME NULL COMMENT ''删除时间'' AFTER deleted_reason',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 菜单调整
-- 2.0 web:menu:work-orders 统一为"已完成工单"（历史名称可能是"工单中心"/"工单处置"）
UPDATE sys_permission
SET permission_name = '已完成工单', updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'web:menu:work-orders'
  AND permission_name IN ('工单中心', '工单处置');

-- 2.1 web:menu:audits 改回"事件审核"（网格员处理后的 PC 审核）
UPDATE sys_permission
SET permission_name = '事件审核', updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'web:menu:audits' AND permission_name = '异常工单';

-- 2.2 停用智能派单规则菜单并解除角色绑定
UPDATE sys_permission
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'web:menu:dispatch-rules';

DELETE FROM sys_role_permission
WHERE permission_id IN (
    SELECT id FROM sys_permission WHERE permission_code = 'web:menu:dispatch-rules'
);

-- 2.3 新增异常工单菜单权限并授权 SUPER_ADMIN 与 EVENT_OPERATOR
INSERT IGNORE INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
VALUES ('web:menu:abnormal-orders', '异常工单', 'MENU', 'WEB', '/abnormal-orders', 206, 'ACTIVE', 'Web菜单-事件工单', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code = 'web:menu:abnormal-orders'
WHERE r.role_code IN ('SUPER_ADMIN', 'EVENT_OPERATOR');
