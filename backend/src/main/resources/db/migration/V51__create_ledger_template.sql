-- 台账模板表
CREATE TABLE IF NOT EXISTS sys_ledger_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_name VARCHAR(128) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(32) NOT NULL COMMENT '模板类型: EVENT/POPULATION/BUILDING/MERCHANT/PATROL/SAFETY',
    description VARCHAR(255) COMMENT '模板描述',
    columns_json JSON NOT NULL COMMENT '列配置: [{"field":"title","label":"标题","width":20}]',
    filters_json JSON COMMENT '默认筛选条件',
    sort_field VARCHAR(64) COMMENT '默认排序字段',
    sort_order VARCHAR(8) DEFAULT 'DESC' COMMENT '排序方向',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (template_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='台账模板';

-- 插入默认模板
INSERT INTO sys_ledger_template (template_name, template_type, description, columns_json, sort_field, sort_order) VALUES
('事件台账', 'EVENT', '事件闭环处置台账', '[{"field":"event_code","label":"事件编号","width":15},{"field":"title","label":"标题","width":25},{"field":"event_type","label":"类型","width":12},{"field":"status","label":"状态","width":10},{"field":"urgency_level","label":"紧急程度","width":10},{"field":"grid_name","label":"网格","width":12},{"field":"incident_address","label":"事发地址","width":20},{"field":"occurred_at","label":"发生时间","width":18}]', 'created_at', 'DESC'),
('实有人口台账', 'POPULATION', '实有人口信息台账', '[{"field":"name","label":"姓名","width":10},{"field":"phone","label":"电话","width":15},{"field":"household_type","label":"户籍类型","width":12},{"field":"address","label":"地址","width":25},{"field":"grid_name","label":"网格","width":12},{"field":"created_at","label":"登记时间","width":18}]', 'created_at', 'DESC'),
('房屋台账', 'BUILDING', '房屋/出租屋台账', '[{"field":"building_no","label":"楼栋编号","width":15},{"field":"address","label":"地址","width":25},{"field":"landlord_name","label":"房东","width":10},{"field":"landlord_phone","label":"房东电话","width":15},{"field":"fire_risk_level","label":"消防风险","width":10},{"field":"grid_name","label":"网格","width":12}]', 'created_at', 'DESC'),
('场所台账', 'MERCHANT', '九小场所台账', '[{"field":"merchant_name","label":"场所名称","width":20},{"field":"legal_person_name","label":"负责人","width":10},{"field":"legal_person_phone","label":"电话","width":15},{"field":"remark","label":"备注","width":25},{"field":"status","label":"状态","width":10}]', 'created_at', 'DESC'),
('巡查台账', 'PATROL', '网格巡查台账', '[{"field":"grid_name","label":"网格","width":15},{"field":"patrol_type","label":"巡查类型","width":12},{"field":"content","label":"内容","width":30},{"field":"status","label":"状态","width":10},{"field":"created_at","label":"时间","width":18}]', 'created_at', 'DESC'),
('安全检査台账', 'SAFETY', '安全检查台账', '[{"field":"merchant_name","label":"场所","width":20},{"field":"fire_risk_level","label":"消防风险","width":10},{"field":"safety_status","label":"安全状态","width":10},{"field":"rectification_status","label":"整改状态","width":10},{"field":"inspection_date","label":"检查日期","width":15}]', 'inspection_date', 'DESC');
