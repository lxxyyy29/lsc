-- V101: 智慧党建改造 —— 独立党支部实体管理（党支部CRUD/导入/人员构成书记+成员增删）
-- 旧数据迁移：从 sys_party_member.party_branch 字符串枚举自动抽取到独立党支部表

-- 1. 党支部主表
CREATE TABLE IF NOT EXISTS sys_party_branch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_name VARCHAR(128) NOT NULL COMMENT '党支部名称',
    secretary_member_id BIGINT COMMENT '支部书记（党员ID）',
    grid_id BIGINT COMMENT '联系网格ID',
    address VARCHAR(255) COMMENT '办公地址',
    phone VARCHAR(32) COMMENT '联系电话',
    establish_date DATE COMMENT '成立日期',
    remark VARCHAR(500) COMMENT '备注',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE启用 / DISABLED停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_branch_name (branch_name),
    INDEX idx_grid (grid_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党支部';

-- 2. 党支部-党员关联表（区分书记/成员角色；支持后续人员动态添加删除）
CREATE TABLE IF NOT EXISTS sys_party_branch_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL COMMENT '党支部ID',
    party_member_id BIGINT NOT NULL COMMENT '党员ID',
    role VARCHAR(16) NOT NULL COMMENT 'SECRETARY书记 / MEMBER成员',
    joined_date DATE COMMENT '进入支部日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_branch_member (branch_id, party_member_id),
    INDEX idx_branch (branch_id),
    INDEX idx_member (party_member_id),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党支部-党员关联';

-- 3. 从旧数据抽取党支部（去重 party_branch 字符串）—— 仅在支部为空时执行，避免重复
INSERT IGNORE INTO sys_party_branch (branch_name, grid_id, status)
SELECT DISTINCT
    TRIM(pm.party_branch) AS branch_name,
    MIN(pm.grid_id) AS grid_id,
    'ACTIVE' AS status
FROM sys_party_member pm
WHERE pm.party_branch IS NOT NULL AND TRIM(pm.party_branch) <> ''
GROUP BY TRIM(pm.party_branch);

-- 4. 迁移党员与党支部的关联（默认角色 MEMBER）
INSERT IGNORE INTO sys_party_branch_member (branch_id, party_member_id, role, joined_date)
SELECT
    pb.id AS branch_id,
    pm.id AS party_member_id,
    'MEMBER' AS role,
    pm.join_date AS joined_date
FROM sys_party_member pm
JOIN sys_party_branch pb ON pb.branch_name = TRIM(pm.party_branch)
WHERE pm.party_branch IS NOT NULL AND TRIM(pm.party_branch) <> '';

-- 5. 示例：给示例党支部指定书记（拔蛟窝社区党支部的第一位党员作为书记）
UPDATE sys_party_branch_member bm
JOIN sys_party_branch pb ON pb.id = bm.branch_id
SET bm.role = 'SECRETARY'
WHERE pb.branch_name = '拔蛟窝社区党支部'
  AND bm.party_member_id = (
    SELECT MIN(pm.id) FROM sys_party_member pm
    WHERE TRIM(pm.party_branch) = '拔蛟窝社区党支部'
  )
  AND bm.id > 0;
