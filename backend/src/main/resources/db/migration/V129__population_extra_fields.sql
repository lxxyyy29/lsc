-- ============================================================
-- V129: 人口库「自定义字段值」存储
-- 背景：字段配置器（sys_form_field_config）支持新增/删除字段后，新增的自定义字段
--       没有对应的 cmn_population 列。这里统一以「键值对 JSON」存到本列。
-- 取舍：不用运行期动态 DDL（ALTER TABLE ADD COLUMN）——避免模式漂移、
--       列名注入与 Flyway 历史不一致；删除字段时也不需要 DROP COLUMN。
-- 说明：
--   1) 键为字段配置的 field_key（前端按驼峰提交，故按实际提交键存储）
--   2) 历史数据为 NULL，表示没有自定义字段值
-- 本迁移：cmn_population 增加 extra_fields 列（可空，历史数据不受影响）
-- ============================================================

ALTER TABLE `cmn_population`
  ADD COLUMN `extra_fields` json NULL COMMENT '自定义字段值（字段配置器中新增的字段，键值对）' AFTER `remark`;
