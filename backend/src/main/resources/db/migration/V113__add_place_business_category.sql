-- ============================================================
-- V113: 场所资源库 - 新增经营类别字段
-- 说明：cmn_place 新增 business_category 列（经营类别），
--       台账数据 cmn_place_ledger.extra_data.place_type 作为经营类别来源
-- ============================================================

ALTER TABLE cmn_place
    ADD COLUMN business_category varchar(64) NULL COMMENT '经营类别'
        AFTER place_type;
