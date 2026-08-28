ALTER TABLE biz_process_instance_node ADD COLUMN cycle_no INT NOT NULL DEFAULT 1;
ALTER TABLE biz_process_instance_node ADD COLUMN is_current TINYINT NOT NULL DEFAULT 1;
ALTER TABLE biz_process_instance_node ADD COLUMN node_mode VARCHAR(32) NOT NULL DEFAULT 'SINGLE';

CREATE INDEX idx_biz_process_instance_node_current
    ON biz_process_instance_node (process_instance_id, is_current, node_order);
