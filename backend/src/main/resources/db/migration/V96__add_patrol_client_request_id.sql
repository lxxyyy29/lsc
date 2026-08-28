-- 移动端离线采集(R07):巡查打卡记录加客户端请求ID,用于离线重试幂等去重
-- 幂等写法:已手动加过列/索引的环境重复执行不报错
-- 可空列唯一索引:NULL 不参与唯一性判断,老数据不受影响
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cmn_patrol_record' AND COLUMN_NAME = 'client_request_id');
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE cmn_patrol_record ADD COLUMN client_request_id VARCHAR(64) NULL COMMENT ''客户端请求ID(离线重试幂等)'' AFTER photo_urls',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cmn_patrol_record' AND INDEX_NAME = 'uk_client_request');
SET @ddl := IF(@idx_exists = 0,
    'ALTER TABLE cmn_patrol_record ADD UNIQUE KEY uk_client_request (client_request_id)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
