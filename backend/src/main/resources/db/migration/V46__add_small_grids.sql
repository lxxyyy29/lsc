-- =============================================================================
-- V46: 添加12个小网格（每个大网格分2个小网格）
-- =============================================================================

-- 临时禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 使用变量存储大网格ID
SET @g1 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G01');
SET @g2 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G02');
SET @g3 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G03');
SET @g4 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G04');
SET @g5 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G05');
SET @g6 = (SELECT id FROM cmn_grid WHERE grid_code = 'BJW-G06');

-- 插入12个小网格
INSERT INTO cmn_grid (grid_code, grid_name, grid_level, parent_id, roi_json, area, population, building_count, sort_order, status, remark, created_at, updated_at)
VALUES
-- 第一网格的子网格
('BJW-G01-A', '第一网格A', 3, @g1, '[[113.930,22.965],[113.933,22.965],[113.933,22.973],[113.930,22.973]]', 0.21, 1100, 13, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G01-B', '第一网格B', 3, @g1, '[[113.933,22.965],[113.936,22.965],[113.936,22.973],[113.933,22.973]]', 0.21, 1000, 12, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 第二网格的子网格
('BJW-G02-A', '第二网格A', 3, @g2, '[[113.936,22.965],[113.939,22.965],[113.939,22.973],[113.936,22.973]]', 0.20, 1000, 11, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G02-B', '第二网格B', 3, @g2, '[[113.939,22.965],[113.942,22.965],[113.942,22.973],[113.939,22.973]]', 0.20, 900, 11, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 第三网格的子网格
('BJW-G03-A', '第三网格A', 3, @g3, '[[113.942,22.965],[113.945,22.965],[113.945,22.973],[113.942,22.973]]', 0.19, 950, 10, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G03-B', '第三网格B', 3, @g3, '[[113.945,22.965],[113.948,22.965],[113.948,22.973],[113.945,22.973]]', 0.19, 850, 10, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 第四网格的子网格
('BJW-G04-A', '第四网格A', 3, @g4, '[[113.930,22.973],[113.933,22.973],[113.933,22.980],[113.930,22.980]]', 0.21, 1050, 12, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G04-B', '第四网格B', 3, @g4, '[[113.933,22.973],[113.936,22.973],[113.936,22.980],[113.933,22.980]]', 0.20, 950, 12, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 第五网格的子网格
('BJW-G05-A', '第五网格A', 3, @g5, '[[113.936,22.973],[113.939,22.973],[113.939,22.980],[113.936,22.980]]', 0.20, 900, 10, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G05-B', '第五网格B', 3, @g5, '[[113.939,22.973],[113.942,22.973],[113.942,22.980],[113.939,22.980]]', 0.19, 800, 9, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 第六网格的子网格
('BJW-G06-A', '第六网格A', 3, @g6, '[[113.942,22.973],[113.945,22.973],[113.945,22.980],[113.942,22.980]]', 0.20, 800, 9, 1, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BJW-G06-B', '第六网格B', 3, @g6, '[[113.945,22.973],[113.948,22.973],[113.948,22.980],[113.945,22.980]]', 0.20, 700, 9, 2, 'ACTIVE', '小网格', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;
