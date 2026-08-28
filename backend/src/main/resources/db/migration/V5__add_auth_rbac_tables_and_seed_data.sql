CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_code VARCHAR(128) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_type VARCHAR(32) NOT NULL,
    client_type VARCHAR(32) NOT NULL,
    parent_id BIGINT,
    path VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_permission_code UNIQUE (permission_code),
    CONSTRAINT fk_sys_permission_parent FOREIGN KEY (parent_id) REFERENCES sys_permission (id)
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_role_user_role UNIQUE (user_id, role_id),
    CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
);

CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_permission_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_sys_role_permission_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
);

CREATE INDEX idx_sys_permission_client_type ON sys_permission (client_type, permission_type);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);
CREATE INDEX idx_sys_role_permission_role_id ON sys_role_permission (role_id);
CREATE INDEX idx_sys_role_permission_permission_id ON sys_role_permission (permission_id);

UPDATE sys_user su
SET role_id = (
    SELECT MIN(sur.role_id)
    FROM sys_user_role sur
    WHERE sur.user_id = su.id
)
WHERE su.role_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM sys_user_role sur
      WHERE sur.user_id = su.id
  );

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT su.id, su.role_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_user su
WHERE su.role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_user_role sur
      WHERE sur.user_id = su.id
        AND sur.role_id = su.role_id
  );

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'SUPER_ADMIN', '超级管理员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'SUPER_ADMIN');

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'EVENT_OPERATOR', '事件专员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'EVENT_OPERATOR');

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'AUDITOR', '审核员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'AUDITOR');

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'DISPATCHER', '派单员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'DISPATCHER');

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'H5_WORKER', '移动端处置人员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'H5_WORKER');

INSERT INTO sys_role (role_code, role_name, status, remark, created_at, updated_at)
SELECT 'H5_VERIFIER', '移动端核查人员', 'ACTIVE', 'System seeded role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'H5_VERIFIER');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:dashboard:view', 'Web首页入口', 'MENU', 'WEB', '/dashboard', 10, 'ACTIVE', 'Login-gating web entry permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:dashboard:view');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:event:list', '事件列表入口', 'MENU', 'WEB', '/events', 20, 'ACTIVE', 'Login-gating web entry permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:event:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:audit:list', '审核列表入口', 'MENU', 'WEB', '/audits', 30, 'ACTIVE', 'Login-gating web entry permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:audit:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:h5:workbench:view', 'H5工作台入口', 'MENU', 'H5', '/workbench', 10, 'ACTIVE', 'Login-gating h5 entry permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:h5:workbench:view');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:h5:workorder:list', 'H5工单列表入口', 'MENU', 'H5', '/workorders', 20, 'ACTIVE', 'Login-gating h5 entry permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:h5:workorder:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:event:create', '创建事件', 'API', 'WEB', '/api/events', 100, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:event:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:event:detail', '查看事件详情', 'API', 'WEB', '/api/events/{id}', 105, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:event:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:event:list', '查询事件列表', 'API', 'WEB', '/api/events', 108, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:event:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:audit:start', '发起审核', 'API', 'WEB', '/api/audits/{eventId}/start', 110, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:audit:start');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:audit:detail', '查看审核详情', 'API', 'WEB', '/api/audits/{eventId}', 115, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:audit:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-instance:approve', '审批流程实例', 'API', 'WEB', '/api/processes/instances/{id}/approve', 120, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-instance:approve');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-instance:reject', '驳回流程实例', 'API', 'WEB', '/api/processes/instances/{id}/reject', 125, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-instance:reject');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-template:create', '创建流程模板', 'API', 'WEB', '/api/processes/templates', 128, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-template:create');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:process-template:list', '查询流程模板', 'API', 'WEB', '/api/processes/templates', 129, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:process-template:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:workorder:dispatch', '派发工单', 'API', 'WEB', '/api/work-orders/{eventId}/dispatch', 130, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:workorder:dispatch');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:workorder:confirm-close', '确认关闭工单', 'API', 'WEB', '/api/work-orders/{id}/confirm-close', 135, 'ACTIVE', 'Task 4 web business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:workorder:confirm-close');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workbench:view', '查看H5工作台', 'API', 'H5', '/api/h5/workbench', 210, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workbench:view');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:list', '查看H5工单列表', 'API', 'H5', '/api/h5/work-orders', 220, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:list');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:detail', '查看H5工单详情', 'API', 'H5', '/api/h5/work-orders/{id}', 225, 'ACTIVE', 'Task 5 H5 business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:detail');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:accept', 'H5接单', 'API', 'H5', '/api/h5/work-orders/{id}/accept', 230, 'ACTIVE', 'First-wave RBAC API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:accept');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:arrive', 'H5到场', 'API', 'H5', '/api/h5/work-orders/{id}/arrive', 235, 'ACTIVE', 'Task 5 H5 business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:arrive');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:handle', 'H5处置', 'API', 'H5', '/api/h5/work-orders/{id}/handle', 236, 'ACTIVE', 'Task 5 H5 business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:handle');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:h5:workorder:verify', 'H5核实', 'API', 'H5', '/api/h5/work-orders/{id}/verify', 237, 'ACTIVE', 'Task 5 H5 business API permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:h5:workorder:verify');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:auth:web:me', '查看Web当前用户', 'API', 'WEB', '/api/auth/me', 140, 'ACTIVE', 'Task 3 auth current-user permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:auth:web:me');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, path, sort_order, status, remark, created_at, updated_at)
SELECT 'api:auth:h5:me', '查看H5当前用户', 'API', 'H5', '/api/h5/auth/me', 240, 'ACTIVE', 'Task 3 auth current-user permission', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'api:auth:h5:me');

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
CROSS JOIN sys_permission p
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('menu:dashboard:view', 'menu:event:list', 'api:auth:web:me', 'api:event:create', 'api:event:detail', 'api:event:list')
WHERE r.role_code = 'EVENT_OPERATOR'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('menu:dashboard:view', 'menu:audit:list', 'api:auth:web:me', 'api:event:detail', 'api:event:list', 'api:audit:start', 'api:audit:detail', 'api:process-instance:approve', 'api:process-instance:reject', 'api:process-template:list')
WHERE r.role_code = 'AUDITOR'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN ('menu:dashboard:view', 'api:auth:web:me', 'api:event:detail', 'api:event:list', 'api:workorder:dispatch', 'api:workorder:confirm-close')
WHERE r.role_code = 'DISPATCHER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:h5:workbench:view',
    'menu:h5:workorder:list',
    'api:auth:h5:me',
    'api:h5:workbench:view',
    'api:h5:workorder:list',
    'api:h5:workorder:detail',
    'api:h5:workorder:accept',
    'api:h5:workorder:arrive',
    'api:h5:workorder:handle',
    'api:h5:workorder:verify'
)
WHERE r.role_code = 'H5_WORKER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'menu:h5:workbench:view',
    'menu:h5:workorder:list',
    'api:auth:h5:me',
    'api:h5:workbench:view',
    'api:h5:workorder:list',
    'api:h5:workorder:detail',
    'api:h5:workorder:verify'
)
WHERE r.role_code = 'H5_VERIFIER'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
