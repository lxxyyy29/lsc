-- V69：事件归档（关闭/忽略后的案件可归档留存，从活跃视图过滤）
ALTER TABLE biz_event ADD COLUMN archived TINYINT NOT NULL DEFAULT 0;
ALTER TABLE biz_event ADD COLUMN archived_at TIMESTAMP NULL;
CREATE INDEX idx_biz_event_archived ON biz_event (archived);
