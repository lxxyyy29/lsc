-- V104: 界面中文化数据清洗
-- 规则：界面可见文本仅中文（登录账号 username 允许英文，权限编码 permission_code 为内部标识不展示）
-- 范围：权限名称/备注、字典标签、网格名、商户备注类别码与名称、组织人员姓名、用户真实姓名

-- ---------- 1. sys_permission.permission_name 英文术语中文化 ----------
UPDATE sys_permission SET permission_name = REPLACE(permission_name, 'H5', '居民端')
 WHERE permission_name LIKE '%H5%';
UPDATE sys_permission SET permission_name = REPLACE(permission_name, 'Web', '管理端')
 WHERE permission_name LIKE '%Web%';
UPDATE sys_permission SET permission_name = REPLACE(permission_name, 'AI', '智能')
 WHERE permission_name LIKE '%AI%';
UPDATE sys_permission SET permission_name = REPLACE(permission_name, 'GIS', '')
 WHERE permission_name LIKE '%GIS%';
UPDATE sys_permission SET permission_name = REPLACE(permission_name, 'BI', '')
 WHERE permission_name LIKE '%BI%';

-- ---------- 2. sys_permission.remark 清洗 ----------
-- 2.1 保留分组含义的术语替换（网格治理-BI态势看板 等）
UPDATE sys_permission SET remark = REPLACE(remark, 'BI', '') WHERE remark LIKE '%BI%';
UPDATE sys_permission SET remark = REPLACE(remark, 'GIS', '') WHERE remark LIKE '%GIS%';
-- 2.2 菜单分组备注去掉 "Web菜单-" 前缀（前端菜单管理页按 remark 直接分组）
UPDATE sys_permission SET remark = SUBSTRING(remark, CHAR_LENGTH('Web菜单-') + 1)
 WHERE remark LIKE 'Web菜单-%';
-- 2.3 其余英文备注（Task 4 web business API permission 等研发备注）清空
UPDATE sys_permission SET remark = NULL WHERE remark REGEXP '[A-Za-z]';

-- ---------- 3. 字典项标签 ----------
UPDATE sys_dict_item SET item_label = '智能监控抓拍'
 WHERE dict_code = 'event_report_source' AND item_value = 'AI_CAMERA';

-- ---------- 4. 网格名称：第一网格A/B → 第一网格一区/二区 ----------
UPDATE cmn_grid SET grid_name = REPLACE(REPLACE(grid_name, 'A', '一区'), 'B', '二区')
 WHERE grid_name REGEXP '[A-Za-z]';

-- ---------- 5. 商户备注中的类别码中文化 ----------
UPDATE biz_merchant SET remark = REPLACE(remark, '类别: SMALL_SHOP', '类别: 小型商铺')
 WHERE remark LIKE '%类别: SMALL_SHOP%';
UPDATE biz_merchant SET remark = REPLACE(remark, '类别: SMALL_WORKSHOP', '类别: 小作坊')
 WHERE remark LIKE '%类别: SMALL_WORKSHOP%';
UPDATE biz_merchant SET remark = REPLACE(remark, '类别: RENTAL_HOUSE', '类别: 出租屋')
 WHERE remark LIKE '%类别: RENTAL_HOUSE%';
UPDATE biz_merchant SET remark = REPLACE(remark, '类别: RESIDENTIAL', '类别: 住宅')
 WHERE remark LIKE '%类别: RESIDENTIAL%';
UPDATE biz_merchant SET remark = REPLACE(remark, '类别: OTHER', '类别: 其他')
 WHERE remark LIKE '%类别: OTHER%';

-- ---------- 6. 商户名称含字母的两条 ----------
UPDATE biz_merchant SET merchant_name = '亿家艺术工作室' WHERE merchant_name = 'E家艺术工作室';
UPDATE biz_merchant SET merchant_name = '艾欧史密斯仓库' WHERE merchant_name = 'A.O史密斯仓库';

-- ---------- 7. 组织人员与用户真实姓名 ----------
UPDATE cmn_org_member SET name = '李晓宇' WHERE name = 'lxy';
UPDATE sys_user SET real_name = '龙先生' WHERE real_name = 'long';
