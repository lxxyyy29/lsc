-- V131: 场所资源库「场所类型」「经营类别」接入字典（缺陷 BUG-07）
--
-- 背景：场所资源库的「类型」是前端硬编码数组、「经营类别」只能从台账 extra_data 里推导，
-- 两者都无处新增。现建立字典类型并补齐默认项：
--   1) 系统设置 → 字典管理 中可继续新增/停用项，前端下拉优先读字典（读不到时回退内置列表）
--   2) item_value 存中文，与场所表现有数据（place_type / business_category 存中文）保持一致

-- ---------- 1. 字典类型 ----------
INSERT IGNORE INTO sys_dict_type (dict_code, dict_name, status, remark, created_at, updated_at) VALUES
('place_type',              '场所类型', 'ACTIVE', '场所资源库-场所类型（下拉可维护）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '经营类别', 'ACTIVE', '场所资源库-经营类别（下拉可维护）', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 2. 场所类型默认项（与前端原内置列表一致） ----------
INSERT IGNORE INTO sys_dict_item (dict_code, item_value, item_label, sort_order, status, remark, created_at, updated_at) VALUES
('place_type', '出租屋',     '出租屋',     1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '小档口',     '小档口',     2, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '小娱乐场所', '小娱乐场所', 3, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '小作坊',     '小作坊',     4, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '工业园',     '工业园',     5, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '住宅小区',   '住宅小区',   6, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_type', '其他场所',   '其他场所',   9, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ---------- 3. 经营类别默认项（常见类别，可在字典管理中继续扩展） ----------
INSERT IGNORE INTO sys_dict_item (dict_code, item_value, item_label, sort_order, status, remark, created_at, updated_at) VALUES
('place_business_category', '餐饮',     '餐饮',     1, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '零售',     '零售',     2, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '美容美发', '美容美发', 3, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '维修',     '维修',     4, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '娱乐',     '娱乐',     5, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '住宿',     '住宿',     6, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '教育',     '教育',     7, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('place_business_category', '其他',     '其他',     9, 'ACTIVE', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
