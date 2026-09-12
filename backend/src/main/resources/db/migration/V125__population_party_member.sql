-- ============================================================
-- V125: 实有人口「党员」标记（与智慧党建关联）
-- 背景：人口库新增/编辑人员时可勾选「党员」，作为党员身份标记；
--       后端保存时按手机号匹配系统账号，匹配到则在 sys_party_member 自动建档，
--       匹配不到仅保留人口库标记，不阻塞保存。
-- 本迁移：
--   1) cmn_population 增加 is_party_member 标记列
--   2) 人口库表单字段配置增加「党员」勾选项（表单为动态字段驱动，缺配置则前端不渲染）
-- 说明：Flyway 保证只执行一次
-- ============================================================

ALTER TABLE `cmn_population`
  ADD COLUMN `is_party_member` tinyint DEFAULT 0 COMMENT '是否党员 0否1是（与智慧党建关联标记）' AFTER `special_population_type`;

INSERT INTO `sys_form_field_config`
  (`module`, `field_key`, `field_label`, `field_type`, `options`, `enabled`, `sort_order`, `required`, `created_at`, `updated_at`)
VALUES
  ('population', 'is_party_member', '党员', 'checkbox', NULL, 1, 16, 0, NOW(), NOW());
