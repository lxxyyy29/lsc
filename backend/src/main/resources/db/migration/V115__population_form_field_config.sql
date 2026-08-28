-- ============================================================
-- V115: 人口库表单字段配置
-- 说明：sys_form_field_config 存人口库表单/导入字段的启用、排序、必填配置，
--       前端表单与导入模板按此动态渲染（字段配置器，仅人口库）
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_form_field_config (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    module        VARCHAR(50)  NOT NULL COMMENT '配置模块（如 population）',
    field_key     VARCHAR(64)  NOT NULL COMMENT '字段键（对应实体属性）',
    field_label   VARCHAR(64)  NOT NULL COMMENT '字段显示名',
    field_type    VARCHAR(32)  NOT NULL DEFAULT 'text' COMMENT '字段类型：text/select/date/checkbox/textarea',
    options       VARCHAR(500) NULL COMMENT 'select 选项（逗号分隔）',
    enabled       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用 0否1是',
    sort_order    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    required      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填 0否1是',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_field (module, field_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '表单字段配置（字段配置器）';

-- 初始化人口库字段配置
INSERT INTO sys_form_field_config (module, field_key, field_label, field_type, options, enabled, sort_order, required) VALUES
('population', 'name',                 '姓名',       'text',     NULL, 1, 1, 1),
('population', 'gender',               '性别',       'select',   '男,女', 1, 2, 0),
('population', 'age',                  '年龄',       'text',     NULL, 1, 3, 0),
('population', 'idCard',               '身份证号',   'text',     NULL, 1, 4, 1),
('population', 'phone',                '联系电话',   'text',     NULL, 1, 5, 0),
('population', 'birthday',             '出生日期',   'date',     NULL, 1, 6, 0),
('population', 'householdType',        '户籍类型',   'select',   'LOCAL:本地户籍,NON_LOCAL:外地户籍,FLOATING:流动人口,LOW_INCOME:低保户,SPECIAL_CARE:优抚对象,OTHER:其他', 1, 7, 0),
('population', 'special_population',   '特殊人群',   'checkbox', NULL, 1, 8, 0),
('population', 'special_population_type', '特殊人群类型', 'select', '低保户,优抚对象,残疾人,孤寡老人,困境儿童,其他', 1, 9, 0),
('population', 'relation',             '与户主关系', 'select',   '户主,配偶,儿子,女儿,父亲,母亲,其他', 1, 10, 0),
('population', 'address',              '居住地址',   'text',     NULL, 1, 11, 0),
('population', 'buildingNo',           '楼栋号',     'text',     NULL, 1, 12, 0),
('population', 'roomNo',               '房号',       'text',     NULL, 1, 13, 0),
('population', 'gridId',               '所属网格',   'select',   NULL, 1, 14, 0),
('population', 'remark',               '备注',       'textarea', NULL, 1, 15, 0);
