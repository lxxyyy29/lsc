ALTER TABLE biz_process_template_node
    ADD COLUMN assignee_user_id BIGINT NULL,
    ADD COLUMN assignee_name VARCHAR(100) NULL;

ALTER TABLE biz_process_template_node
    ADD CONSTRAINT fk_biz_process_template_node_assignee FOREIGN KEY (assignee_user_id) REFERENCES sys_user (id);

ALTER TABLE biz_work_order
    ADD COLUMN process_instance_id BIGINT NULL;

ALTER TABLE biz_work_order
    ADD CONSTRAINT fk_biz_work_order_process_instance FOREIGN KEY (process_instance_id) REFERENCES biz_process_instance (id);

ALTER TABLE biz_work_order
    DROP CONSTRAINT chk_biz_work_order_status;

ALTER TABLE biz_work_order
    ADD CONSTRAINT chk_biz_work_order_status CHECK (status IN (
        'PROCESSING',
        'COMPLETED',
        'CLOSED',
        'TIMEOUT'
    ));

CREATE INDEX idx_biz_work_order_process_instance ON biz_work_order (process_instance_id);
CREATE INDEX idx_biz_process_template_node_assignee ON biz_process_template_node (assignee_user_id);
