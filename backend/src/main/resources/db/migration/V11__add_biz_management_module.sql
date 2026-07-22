CREATE TABLE biz_area (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    area_name VARCHAR(100) NOT NULL,
    principal_name VARCHAR(100),
    principal_phone VARCHAR(50),
    roi_json TEXT NOT NULL,
    remark VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE biz_merchant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    merchant_name VARCHAR(100) NOT NULL,
    merchant_photo_url VARCHAR(500),
    longitude DECIMAL(10, 6),
    latitude DECIMAL(10, 6),
    legal_person_name VARCHAR(100),
    legal_person_photo_url VARCHAR(500),
    legal_person_phone VARCHAR(50),
    area_id BIGINT,
    area_match_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
    remark VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_biz_merchant_area FOREIGN KEY (area_id) REFERENCES biz_area (id)
);

CREATE TABLE biz_mobile_vendor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vendor_name VARCHAR(100) NOT NULL,
    vendor_photo_url VARCHAR(500),
    legal_person_name VARCHAR(100),
    legal_person_photo_url VARCHAR(500),
    legal_person_phone VARCHAR(50),
    remark VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_area_status ON biz_area (status);
CREATE INDEX idx_biz_merchant_area_id ON biz_merchant (area_id);
CREATE INDEX idx_biz_merchant_status ON biz_merchant (status);
CREATE INDEX idx_biz_mobile_vendor_status ON biz_mobile_vendor (status);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'catalog:biz', '业务管理', 'CATALOG', 'WEB', NULL, '/biz', NULL, 'OfficeBuilding', 500, 'ACTIVE', 'Business management catalog', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'catalog:biz');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:biz:area', '区域管理', 'MENU', 'WEB', p.id, '/biz/areas', 'biz/areas/index', 'MapLocation', 10, 'ACTIVE', 'Business area management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:biz'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:biz:area');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:biz:merchant', '商户管理', 'MENU', 'WEB', p.id, '/biz/merchants', 'biz/merchants/index', 'Shop', 20, 'ACTIVE', 'Business merchant management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:biz'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:biz:merchant');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT 'menu:biz:vendor', '摊贩管理', 'MENU', 'WEB', p.id, '/biz/vendors', 'biz/vendors/index', 'UserFilled', 30, 'ACTIVE', 'Business vendor management menu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_permission p
WHERE p.permission_code = 'catalog:biz'
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = 'menu:biz:vendor');

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, permission_type, 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'button:biz:area:create' AS permission_code, '新建区域' AS permission_name, 'BUTTON' AS permission_type,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area') AS parent_id,
           NULL AS path, 11 AS sort_order, 'Business area create button' AS remark
    UNION ALL SELECT 'button:biz:area:update', '编辑区域', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), NULL, 12, 'Business area update button'
    UNION ALL SELECT 'button:biz:area:delete', '删除区域', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), NULL, 13, 'Business area delete button'
    UNION ALL SELECT 'button:biz:merchant:create', '新建商户', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), NULL, 21, 'Business merchant create button'
    UNION ALL SELECT 'button:biz:merchant:update', '编辑商户', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), NULL, 22, 'Business merchant update button'
    UNION ALL SELECT 'button:biz:merchant:delete', '删除商户', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), NULL, 23, 'Business merchant delete button'
    UNION ALL SELECT 'button:biz:vendor:create', '新建摊贩', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), NULL, 31, 'Business vendor create button'
    UNION ALL SELECT 'button:biz:vendor:update', '编辑摊贩', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), NULL, 32, 'Business vendor update button'
    UNION ALL SELECT 'button:biz:vendor:delete', '删除摊贩', 'BUTTON', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), NULL, 33, 'Business vendor delete button'
) seeded_buttons
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_buttons.permission_code);

INSERT INTO sys_permission (permission_code, permission_name, permission_type, client_type, parent_id, path, component, icon, sort_order, status, remark, created_at, updated_at)
SELECT permission_code, permission_name, 'API', 'WEB', parent_id, path, NULL, NULL, sort_order, 'ACTIVE', remark, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
    SELECT 'api:biz:area:list' AS permission_code, '查询区域列表' AS permission_name,
           (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area') AS parent_id,
           '/api/areas' AS path, 101 AS sort_order, 'Business area list API' AS remark
    UNION ALL SELECT 'api:biz:area:detail', '查询区域详情', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), '/api/areas/{id}', 102, 'Business area detail API'
    UNION ALL SELECT 'api:biz:area:create', '创建区域', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), '/api/areas', 103, 'Business area create API'
    UNION ALL SELECT 'api:biz:area:update', '更新区域', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), '/api/areas/{id}', 104, 'Business area update API'
    UNION ALL SELECT 'api:biz:area:delete', '删除区域', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:area'), '/api/areas/{id}', 105, 'Business area delete API'
    UNION ALL SELECT 'api:biz:merchant:list', '查询商户列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), '/api/merchants', 201, 'Business merchant list API'
    UNION ALL SELECT 'api:biz:merchant:detail', '查询商户详情', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), '/api/merchants/{id}', 202, 'Business merchant detail API'
    UNION ALL SELECT 'api:biz:merchant:create', '创建商户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), '/api/merchants', 203, 'Business merchant create API'
    UNION ALL SELECT 'api:biz:merchant:update', '更新商户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), '/api/merchants/{id}', 204, 'Business merchant update API'
    UNION ALL SELECT 'api:biz:merchant:delete', '删除商户', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:merchant'), '/api/merchants/{id}', 205, 'Business merchant delete API'
    UNION ALL SELECT 'api:biz:vendor:list', '查询摊贩列表', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), '/api/mobile-vendors', 301, 'Business vendor list API'
    UNION ALL SELECT 'api:biz:vendor:detail', '查询摊贩详情', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), '/api/mobile-vendors/{id}', 302, 'Business vendor detail API'
    UNION ALL SELECT 'api:biz:vendor:create', '创建摊贩', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), '/api/mobile-vendors', 303, 'Business vendor create API'
    UNION ALL SELECT 'api:biz:vendor:update', '更新摊贩', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), '/api/mobile-vendors/{id}', 304, 'Business vendor update API'
    UNION ALL SELECT 'api:biz:vendor:delete', '删除摊贩', (SELECT id FROM sys_permission WHERE permission_code = 'menu:biz:vendor'), '/api/mobile-vendors/{id}', 305, 'Business vendor delete API'
) seeded_apis
WHERE parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_permission WHERE permission_code = seeded_apis.permission_code);

INSERT INTO sys_role_permission (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM sys_role r
JOIN sys_permission p ON p.permission_code IN (
    'catalog:biz',
    'menu:biz:area',
    'menu:biz:merchant',
    'menu:biz:vendor',
    'button:biz:area:create',
    'button:biz:area:update',
    'button:biz:area:delete',
    'button:biz:merchant:create',
    'button:biz:merchant:update',
    'button:biz:merchant:delete',
    'button:biz:vendor:create',
    'button:biz:vendor:update',
    'button:biz:vendor:delete',
    'api:biz:area:list',
    'api:biz:area:detail',
    'api:biz:area:create',
    'api:biz:area:update',
    'api:biz:area:delete',
    'api:biz:merchant:list',
    'api:biz:merchant:detail',
    'api:biz:merchant:create',
    'api:biz:merchant:update',
    'api:biz:merchant:delete',
    'api:biz:vendor:list',
    'api:biz:vendor:detail',
    'api:biz:vendor:create',
    'api:biz:vendor:update',
    'api:biz:vendor:delete'
)
WHERE r.role_code = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission srp
      WHERE srp.role_id = r.id
        AND srp.permission_id = p.id
  );
