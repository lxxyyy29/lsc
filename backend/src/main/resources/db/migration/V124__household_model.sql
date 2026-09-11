-- ============================================================
-- V124: 实有人口「户」建模
-- 背景：cmn_population 原先没有"户"实体，仅靠 address 字符串在查询时临时分组，
--       导致①无法把人员挂到指定户下 ②户主变更后成员"与户主关系"无法联动。
-- 本迁移：
--   1) 新增 cmn_household 户表（户号 / 地址 / 网格 / 户主人口ID）
--   2) cmn_population 增加 household_id 归属列
--   3) 存量数据按「网格 + 居住地址(TRIM)」自动归户并回填 household_id
--   4) 每户指定户主（取 relation='户主' 的成员，多个取最小 id）
-- 说明：CREATE TABLE 用 IF NOT EXISTS，本脚本只应执行一次（Flyway 保障）。
-- ============================================================

CREATE TABLE IF NOT EXISTS `cmn_household` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `household_no` varchar(64) DEFAULT NULL COMMENT '户号',
  `address` varchar(255) DEFAULT NULL COMMENT '居住地址（归户分组依据）',
  `grid_id` bigint DEFAULT NULL COMMENT '所属网格ID',
  `head_population_id` bigint DEFAULT NULL COMMENT '户主（cmn_population.id）',
  `household_type` varchar(32) DEFAULT NULL COMMENT '户籍类型',
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hh_grid` (`grid_id`),
  KEY `idx_hh_head` (`head_population_id`),
  KEY `idx_hh_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实有人口-户';

-- ------------------------------------------------------------
-- cmn_population 增加 household_id
-- ------------------------------------------------------------
ALTER TABLE `cmn_population`
  ADD COLUMN `household_id` bigint DEFAULT NULL COMMENT '所属户ID（cmn_household.id）' AFTER `grid_id`;

ALTER TABLE `cmn_population` ADD KEY `idx_pop_household_id` (`household_id`);

-- ------------------------------------------------------------
-- 存量归户：按 网格 + TRIM(地址) 建户（仅常驻、有地址的记录）
-- 流动人口不建户（无"户"概念）
-- ------------------------------------------------------------
INSERT INTO `cmn_household` (`household_no`, `address`, `grid_id`, `household_type`, `status`, `created_at`, `updated_at`)
SELECT NULL, TRIM(p.`address`), p.`grid_id`, MIN(p.`household_type`), 'ACTIVE', NOW(), NOW()
FROM `cmn_population` p
WHERE p.`status` = 'ACTIVE'
  AND p.`address` IS NOT NULL
  AND TRIM(p.`address`) <> ''
  AND COALESCE(p.`household_type`, '') <> 'FLOATING'
GROUP BY p.`grid_id`, TRIM(p.`address`);

-- ------------------------------------------------------------
-- 回填 household_id
-- ------------------------------------------------------------
UPDATE `cmn_population` p
SET p.`household_id` = (
  SELECT MIN(h.`id`)
  FROM `cmn_household` h
  -- cmn_population 建表时使用了 utf8mb4_german2_ci，而 cmn_household 为 utf8mb4_0900_ai_ci，
  -- 两列直接比较会触发 "Illegal mix of collations"，此处显式统一 collation
  WHERE h.`address` = TRIM(p.`address`) COLLATE utf8mb4_0900_ai_ci
    AND COALESCE(h.`grid_id`, -1) = COALESCE(p.`grid_id`, -1)
)
WHERE p.`status` = 'ACTIVE'
  AND p.`address` IS NOT NULL
  AND TRIM(p.`address`) <> ''
  AND COALESCE(p.`household_type`, '') <> 'FLOATING'
  AND p.`household_id` IS NULL;

-- ------------------------------------------------------------
-- 指定户主：每户取 relation='户主' 的成员（多个取最小 id）
-- 无「户主」标记的户暂不指定，由管理端「变更户主」设置
-- ------------------------------------------------------------
UPDATE `cmn_household` h
JOIN (
  SELECT `household_id`, MIN(`id`) AS head_id
  FROM `cmn_population`
  WHERE `household_id` IS NOT NULL
    AND `relation` = '户主'
    AND `status` = 'ACTIVE'
  GROUP BY `household_id`
) t ON t.`household_id` = h.`id`
SET h.`head_population_id` = t.`head_id`
WHERE h.`head_population_id` IS NULL;
