-- V121: 事件类型字典 event_type（事件创建表单-事件类型下拉）
-- item_value 与 item_label 均为中文，与 biz_event.event_type 现有数据一致，无需迁移

INSERT IGNORE INTO sys_dict_type (dict_code, dict_name, status, remark) VALUES
('event_type', '事件类型', 'ACTIVE', '事件创建表单-事件类型下拉');

INSERT IGNORE INTO sys_dict_item (dict_code, item_value, item_label, sort_order, status) VALUES
('event_type', '市容环境', '市容环境', 1, 'ACTIVE'),
('event_type', '消防安全', '消防安全', 2, 'ACTIVE'),
('event_type', '矛盾纠纷', '矛盾纠纷', 3, 'ACTIVE'),
('event_type', '安全生产', '安全生产', 4, 'ACTIVE'),
('event_type', '民生诉求', '民生诉求', 5, 'ACTIVE'),
('event_type', '防汛防台风', '防汛防台风', 6, 'ACTIVE'),
('event_type', '违建', '违建', 7, 'ACTIVE'),
('event_type', '其他', '其他', 8, 'ACTIVE');
