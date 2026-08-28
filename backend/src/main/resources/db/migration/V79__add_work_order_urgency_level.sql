-- 工单表补充紧急程度字段
-- 背景：WorkOrderAutoHandleTask / NotificationScheduler / ExportController 均引用
-- biz_work_order.urgency_level，但该字段一直未建，导致定时任务每小时报
-- "Unknown column 'urgency_level'" 错误，工单超期升级与通知功能失效。
-- 取值与 biz_event.urgency_level 保持一致：GREEN / YELLOW / RED

ALTER TABLE biz_work_order
    ADD COLUMN urgency_level VARCHAR(32) NOT NULL DEFAULT 'GREEN' COMMENT '紧急程度：GREEN/YELLOW/RED' AFTER status;

CREATE INDEX idx_work_order_urgency ON biz_work_order (urgency_level);

-- 存量工单按关联事件的紧急程度初始化，保持三色分级语义一致
UPDATE biz_work_order wo
    JOIN biz_event e ON e.id = wo.source_event_id
SET wo.urgency_level = COALESCE(e.urgency_level, 'GREEN')
WHERE wo.urgency_level = 'GREEN';
