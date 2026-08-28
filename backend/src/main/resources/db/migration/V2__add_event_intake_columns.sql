ALTER TABLE biz_event ADD COLUMN external_event_id VARCHAR(128);
ALTER TABLE biz_event ADD COLUMN source_system VARCHAR(100) DEFAULT 'LEGACY';
ALTER TABLE biz_event ADD COLUMN event_type VARCHAR(64) DEFAULT 'UNSPECIFIED';

UPDATE biz_event
SET source_system = 'LEGACY'
WHERE source_system IS NULL;

UPDATE biz_event
SET event_type = 'UNSPECIFIED'
WHERE event_type IS NULL;

ALTER TABLE biz_event MODIFY COLUMN source_system VARCHAR(100) NOT NULL DEFAULT 'LEGACY';
ALTER TABLE biz_event MODIFY COLUMN event_type VARCHAR(64) NOT NULL DEFAULT 'UNSPECIFIED';

ALTER TABLE biz_event ADD CONSTRAINT uk_biz_event_external_event_id UNIQUE (external_event_id);
CREATE INDEX idx_biz_event_external_event_id ON biz_event (external_event_id);
