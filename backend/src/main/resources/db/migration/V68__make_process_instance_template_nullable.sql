-- V68: 流程实例脱离流程模板（模板引擎不再用于派单，流程实例仅作审计壳）
-- template_id 改为可空，使派单无需绑定流程模板
ALTER TABLE biz_process_instance MODIFY template_id BIGINT NULL;
