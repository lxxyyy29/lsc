-- web端管理员注册:sys_user 增加注册来源列,区分 web 管理员注册与小程序网格员注册
-- 幂等写法:已手动加过列的环境重复执行不报错
-- 存量记录(含历史待审批的网格员申请)默认 GRID
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'reg_source');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN reg_source VARCHAR(20) NOT NULL DEFAULT ''GRID'' COMMENT ''注册来源:GRID=小程序网格员注册,WEB=web管理员注册'' AFTER phone',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
