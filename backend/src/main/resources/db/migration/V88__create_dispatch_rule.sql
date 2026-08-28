-- 智能分级派单规则表：事件类型 → 目标受理角色
-- 背景：派单时按事件类型智能路由（重点事件→两委干部 EVENT_OPERATOR，简易事件→网格员 H5_WORKER）。
-- 原映射硬编码于 WorkOrderServiceImpl.SERIOUS_EVENT_TYPES，现迁移为可配置表，管理员可在 Web 端调整。
CREATE TABLE IF NOT EXISTS biz_dispatch_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型（编码或中文名）',
    target_role_code VARCHAR(64) NOT NULL COMMENT '目标受理角色编码',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级（数值小者优先）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dispatch_rule_event_type (event_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='智能派单规则';

-- 初始化默认规则：重点事件类型 → 两委干部（与原 SERIOUS_EVENT_TYPES 常量保持一致）
INSERT IGNORE INTO biz_dispatch_rule (event_type, target_role_code, priority, enabled, remark)
VALUES
    ('COMPLAINT', 'EVENT_OPERATOR', 10, 1, '民生诉求'),
    ('FIRE', 'EVENT_OPERATOR', 10, 1, '消防'),
    ('ILLEGAL_BUILDING', 'EVENT_OPERATOR', 10, 1, '违建'),
    ('PUBLIC_SAFETY', 'EVENT_OPERATOR', 10, 1, '公共安全'),
    ('SAFETY', 'EVENT_OPERATOR', 10, 1, '安全生产'),
    ('SAFE', 'EVENT_OPERATOR', 10, 1, '安全生产(简)'),
    ('民生诉求', 'EVENT_OPERATOR', 10, 1, '民生诉求(中文)'),
    ('消防安全', 'EVENT_OPERATOR', 10, 1, '消防安全(中文)'),
    ('违建', 'EVENT_OPERATOR', 10, 1, '违建(中文)'),
    ('公共安全', 'EVENT_OPERATOR', 10, 1, '公共安全(中文)'),
    ('安全生产', 'EVENT_OPERATOR', 10, 1, '安全生产(中文)'),
    ('矛盾纠纷', 'EVENT_OPERATOR', 10, 1, '矛盾纠纷(中文)'),
    ('防汛防台风', 'EVENT_OPERATOR', 10, 1, '防汛防台风(中文)');
