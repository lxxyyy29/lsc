-- ============================================================
-- V117: cmn_patrol_record 新增备注列（小程序打卡备注，需求）
-- 说明：与 PatrolRecordEntity.remark / PatrolRecordMapper 对齐
-- ============================================================

ALTER TABLE cmn_patrol_record
    ADD COLUMN remark VARCHAR(500) NULL COMMENT '备注（非必填）' AFTER content;
