-- V107: 事件/工单展示隐藏功能
-- hidden=0 默认显示；hidden=1 隐藏后仅在事件闭环处置与工单中心可见，
-- 监管大屏、全域态势、GIS 网格等面板不再统计与展示
ALTER TABLE biz_event ADD COLUMN hidden TINYINT NOT NULL DEFAULT 0 COMMENT '展示隐藏：0显示 1隐藏';
ALTER TABLE biz_work_order ADD COLUMN hidden TINYINT NOT NULL DEFAULT 0 COMMENT '展示隐藏：0显示 1隐藏';
