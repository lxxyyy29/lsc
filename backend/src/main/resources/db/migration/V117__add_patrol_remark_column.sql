-- ============================================================
-- V117: cmn_patrol_record 新增备注列（小程序打卡备注，需求）
-- 说明：与 PatrolRecordEntity.remark / PatrolRecordMapper 对齐
-- 幂等处理：列已存在（历史手工补列场景）则跳过，避免重复加列报错
-- ============================================================

SET @sql := IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'cmn_patrol_record'
       AND COLUMN_NAME = 'remark') = 0,
    'ALTER TABLE cmn_patrol_record ADD COLUMN remark VARCHAR(500) NULL COMMENT ''备注（非必填）'' AFTER content',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
