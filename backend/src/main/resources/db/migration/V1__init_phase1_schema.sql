CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Role status',
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (role_code)
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'User status',
    role_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (username),
    CONSTRAINT fk_sys_user_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
);

CREATE TABLE biz_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_code VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' COMMENT 'Event source type',
    status VARCHAR(64) NOT NULL COMMENT 'EventStatus: intake to closure lifecycle',
    report_user_id BIGINT,
    report_user_name VARCHAR(100),
    report_phone VARCHAR(32),
    incident_address VARCHAR(255),
    longitude DECIMAL(10, 6),
    latitude DECIMAL(10, 6),
    occurred_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_code),
    CONSTRAINT fk_biz_event_report_user FOREIGN KEY (report_user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_biz_event_status CHECK (status IN (
        'PENDING_AUDIT',
        'IN_AUDIT',
        'AUDIT_APPROVED',
        'AUDIT_REJECTED',
        'WAITING_DISPATCH',
        'DISPATCHED_TO_WORK_ORDER',
        'CLOSED'
    ))
);

CREATE TABLE biz_event_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    from_status VARCHAR(64) COMMENT 'Previous EventStatus',
    to_status VARCHAR(64) NOT NULL COMMENT 'Next EventStatus',
    action_type VARCHAR(64) NOT NULL COMMENT 'Lifecycle action type',
    operator_user_id BIGINT,
    operator_name VARCHAR(100),
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_event_record_event FOREIGN KEY (event_id) REFERENCES biz_event (id),
    CONSTRAINT fk_biz_event_record_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
);

CREATE TABLE biz_process_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(64) NOT NULL,
    template_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    version_no INT NOT NULL DEFAULT 1,
    status VARCHAR(64) NOT NULL COMMENT 'ProcessTemplateStatus: template availability state',
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_code),
    CONSTRAINT chk_biz_process_template_status CHECK (status IN (
        'DRAFT',
        'ACTIVE',
        'DISABLED'
    ))
);

CREATE TABLE biz_process_template_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    node_order INT NOT NULL,
    approve_mode VARCHAR(32) NOT NULL DEFAULT 'ANY_ONE',
    status VARCHAR(64) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Template node status',
    role_code VARCHAR(64),
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_id, node_key),
    UNIQUE (template_id, node_order),
    CONSTRAINT fk_biz_process_template_node_template FOREIGN KEY (template_id) REFERENCES biz_process_template (id)
);

CREATE TABLE biz_process_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_no VARCHAR(64) NOT NULL,
    template_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL COMMENT 'ProcessStatus: process instance runtime state',
    current_node_order INT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (process_no),
    CONSTRAINT fk_biz_process_instance_template FOREIGN KEY (template_id) REFERENCES biz_process_template (id),
    CONSTRAINT chk_biz_process_instance_status CHECK (status IN (
        'PENDING',
        'RUNNING',
        'APPROVED',
        'REJECTED',
        'TERMINATED'
    ))
);

CREATE TABLE biz_process_instance_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_instance_id BIGINT NOT NULL,
    template_node_id BIGINT,
    node_key VARCHAR(64) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    node_order INT NOT NULL,
    status VARCHAR(64) NOT NULL COMMENT 'Node execution status',
    assignee_user_id BIGINT,
    assignee_name VARCHAR(100),
    handled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_process_instance_node_instance FOREIGN KEY (process_instance_id) REFERENCES biz_process_instance (id),
    CONSTRAINT fk_biz_process_instance_node_template_node FOREIGN KEY (template_node_id) REFERENCES biz_process_template_node (id),
    CONSTRAINT fk_biz_process_instance_node_assignee FOREIGN KEY (assignee_user_id) REFERENCES sys_user (id)
);

CREATE TABLE biz_process_action_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    process_instance_id BIGINT NOT NULL,
    process_instance_node_id BIGINT,
    action_type VARCHAR(64) NOT NULL,
    action_result VARCHAR(64),
    operator_user_id BIGINT,
    operator_name VARCHAR(100),
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_process_action_instance FOREIGN KEY (process_instance_id) REFERENCES biz_process_instance (id),
    CONSTRAINT fk_biz_process_action_node FOREIGN KEY (process_instance_node_id) REFERENCES biz_process_instance_node (id),
    CONSTRAINT fk_biz_process_action_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
);

CREATE TABLE biz_audit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    process_instance_id BIGINT,
    decision VARCHAR(32) NOT NULL,
    status VARCHAR(64) NOT NULL COMMENT 'Audit record status',
    auditor_user_id BIGINT,
    auditor_name VARCHAR(100),
    opinion VARCHAR(500),
    audited_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_audit_record_event FOREIGN KEY (event_id) REFERENCES biz_event (id),
    CONSTRAINT fk_biz_audit_record_instance FOREIGN KEY (process_instance_id) REFERENCES biz_process_instance (id),
    CONSTRAINT fk_biz_audit_record_auditor FOREIGN KEY (auditor_user_id) REFERENCES sys_user (id)
);

CREATE TABLE biz_work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    work_order_no VARCHAR(64) NOT NULL,
    source_event_id BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL COMMENT 'WorkOrderStatus: handling to close lifecycle',
    assignee_user_id BIGINT,
    assignee_name VARCHAR(100),
    dispatcher_user_id BIGINT,
    dispatcher_name VARCHAR(100),
    accepted_at TIMESTAMP,
    completed_at TIMESTAMP,
    closed_at TIMESTAMP,
    close_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (work_order_no),
    UNIQUE (source_event_id),
    CONSTRAINT fk_biz_work_order_event FOREIGN KEY (source_event_id) REFERENCES biz_event (id),
    CONSTRAINT fk_biz_work_order_assignee FOREIGN KEY (assignee_user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_biz_work_order_dispatcher FOREIGN KEY (dispatcher_user_id) REFERENCES sys_user (id),
    CONSTRAINT chk_biz_work_order_status CHECK (status IN (
        'WAITING_ACCEPT',
        'PROCESSING',
        'WAITING_VERIFY',
        'WAITING_CLOSE_CONFIRM',
        'COMPLETED',
        'CLOSED',
        'TIMEOUT'
    ))
);

CREATE TABLE biz_media_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    mime_type VARCHAR(100),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'Media file status',
    uploader_user_id BIGINT,
    uploader_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_media_file_uploader FOREIGN KEY (uploader_user_id) REFERENCES sys_user (id)
);

CREATE TABLE biz_drone (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    drone_code VARCHAR(64) NOT NULL,
    drone_name VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    serial_no VARCHAR(100),
    status VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT 'Drone availability status',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (drone_code)
);

CREATE TABLE biz_patrol_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'RESERVED',
    status VARCHAR(32) NOT NULL DEFAULT 'SKELETON' COMMENT 'Reserved patrol task status for phase 1',
    planned_start_at TIMESTAMP,
    planned_end_at TIMESTAMP,
    linked_event_id BIGINT,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (task_no),
    CONSTRAINT fk_biz_patrol_task_event FOREIGN KEY (linked_event_id) REFERENCES biz_event (id)
);

CREATE INDEX idx_biz_event_status ON biz_event (status);
CREATE INDEX idx_biz_process_template_status ON biz_process_template (status);
CREATE INDEX idx_biz_process_instance_business ON biz_process_instance (business_type, business_id);
CREATE INDEX idx_biz_process_instance_status ON biz_process_instance (status);
CREATE INDEX idx_biz_process_instance_node_status ON biz_process_instance_node (status);
CREATE INDEX idx_biz_audit_record_event ON biz_audit_record (event_id);
CREATE INDEX idx_biz_work_order_status ON biz_work_order (status);
CREATE INDEX idx_biz_media_file_business ON biz_media_file (business_type, business_id);
CREATE INDEX idx_biz_drone_status ON biz_drone (status);
CREATE INDEX idx_biz_patrol_task_status ON biz_patrol_task (status);
