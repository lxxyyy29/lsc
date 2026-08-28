-- 添加缺失索引，优化高频查询性能
-- 注意：MySQL 8.0 不支持 CREATE INDEX IF NOT EXISTS，重复执行会报错
-- 此迁移仅执行一次，Flyway 会记录已应用

-- 1. biz_event.area_id: 工单分页按辖区筛选
CREATE INDEX idx_biz_event_area_id ON biz_event(area_id);

-- 2. biz_process_instance_node 复合索引: H5工单按节点+受理人查询
CREATE INDEX idx_biz_process_instance_node_assignee ON biz_process_instance_node(process_instance_id, assignee_user_id);

-- 3. biz_work_order 复合索引: 工作台按状态+受理人统计
CREATE INDEX idx_biz_work_order_assignee_status ON biz_work_order(assignee_user_id, status);

-- 4. biz_process_action_record: 流程记录按主体类型+ID关联查询
CREATE INDEX idx_biz_process_action_record_subject ON biz_process_action_record(subject_type, subject_id);

-- 5. cmn_patrol_task 复合索引: 过期巡查任务查询 (status + planned_date)
CREATE INDEX idx_cmn_patrol_task_overdue ON cmn_patrol_task(status, planned_date);

-- 6. cmn_patrol_record 复合索引: 巡查记录按网格+用户查询
CREATE INDEX idx_cmn_patrol_record_grid_user ON cmn_patrol_record(grid_id, user_id);

-- 7. biz_work_order.status + created_at: 工单列表排序
CREATE INDEX idx_biz_work_order_status_created ON biz_work_order(status, created_at DESC);
