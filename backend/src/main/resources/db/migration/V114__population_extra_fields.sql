-- ============================================================
-- V114: 人口库扩展字段
-- 说明：cmn_population 新增 年龄 / 特殊人群 / 特殊人群类型 / 与户主关系
-- ============================================================

ALTER TABLE cmn_population
    ADD COLUMN age INT NULL COMMENT '年龄' AFTER gender,
    ADD COLUMN special_population TINYINT NOT NULL DEFAULT 0 COMMENT '是否特殊人群 0否1是' AFTER household_type,
    ADD COLUMN special_population_type VARCHAR(255) NULL COMMENT '特殊人群类型（预置+自定义）' AFTER special_population,
    ADD COLUMN relation VARCHAR(64) NULL COMMENT '与户主关系（户主/妻/长子/女等）' AFTER special_population_type;
