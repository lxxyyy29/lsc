-- 组织人员职务字段，用于组织人员列表展示网格员职务
SET @column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'cmn_org_member'
      AND COLUMN_NAME = 'position'
);

SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE cmn_org_member ADD COLUMN position VARCHAR(50) DEFAULT NULL COMMENT ''职务'' AFTER phone',
    'SELECT 1'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE cmn_org_member
SET position = '网格员'
WHERE member_type = 'GRID_WORKER'
  AND (position IS NULL OR position = '');
