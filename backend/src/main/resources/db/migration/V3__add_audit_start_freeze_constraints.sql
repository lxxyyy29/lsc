ALTER TABLE biz_process_instance ADD COLUMN template_version INT NOT NULL DEFAULT 1;

ALTER TABLE biz_process_instance
    ADD CONSTRAINT uk_biz_process_instance_business UNIQUE (business_type, business_id);
