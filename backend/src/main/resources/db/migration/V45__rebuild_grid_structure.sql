-- =============================================================================
-- V45: 重建网格结构 — 1社区 → 6大网格 → 12小网格
-- =============================================================================

-- 临时禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清除旧的大网格和小网格数据
DELETE FROM cmn_grid WHERE grid_level >= 2;

-- 插入6个大网格
INSERT INTO cmn_grid (grid_code, grid_name, grid_level, parent_id, roi_json, area, population, building_count, sort_order, status, remark, created_at, updated_at)
VALUES
('BJW-G01', '第一网格', 2, 1, '[[113.930,22.965],[113.936,22.965],[113.936,22.973],[113.930,22.973]]', 0.42, 2100, 25, 1, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G02', '第二网格', 2, 1, '[[113.936,22.965],[113.942,22.965],[113.942,22.973],[113.936,22.973]]', 0.40, 1900, 22, 2, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G03', '第三网格', 2, 1, '[[113.942,22.965],[113.948,22.965],[113.948,22.973],[113.942,22.973]]', 0.38, 1800, 20, 3, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G04', '第四网格', 2, 1, '[[113.930,22.973],[113.936,22.973],[113.936,22.980],[113.930,22.980]]', 0.41, 2000, 24, 4, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G05', '第五网格', 2, 1, '[[113.936,22.973],[113.942,22.973],[113.942,22.980],[113.936,22.980]]', 0.39, 1700, 19, 5, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G06', '第六网格', 2, 1, '[[113.942,22.973],[113.948,22.973],[113.948,22.980],[113.942,22.980]]', 0.40, 1500, 18, 6, 'ACTIVE', '大网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
