-- 为工单处理记录增加商户/摊贩关联字段，支持开罚单功能
ALTER TABLE biz_process_action_record
  ADD COLUMN subject_type VARCHAR(20) NULL COMMENT '关联对象类型: MERCHANT | VENDOR',
  ADD COLUMN subject_id   BIGINT      NULL COMMENT '关联对象ID（merchant 或 vendor）';
