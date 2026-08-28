-- 性能优化：添加缺失索引

-- 事件记录表：按事件+时间查询（事件历史）
CREATE INDEX idx_event_record_event_time ON biz_event_record(event_id, created_at);

-- 审计日志表：按时间范围查询
CREATE INDEX idx_audit_log_time ON sys_audit_log(operation_time);

-- 工单表：按状态+创建时间筛选
CREATE INDEX idx_work_order_status_time ON biz_work_order(status, created_at);

-- 通知表：按用户+已读状态查询
CREATE INDEX idx_notification_user_read ON sys_notification(user_id, is_read);

-- 巡查记录表：按网格+时间查询
CREATE INDEX idx_patrol_grid_time ON cmn_patrol_record(grid_id, created_at);
