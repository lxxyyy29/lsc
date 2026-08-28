-- P2-2：智慧党建补齐 —— 党建任务下沉 / 党群议事 / 政策推送

-- 1. 党建任务下沉表
CREATE TABLE IF NOT EXISTS sys_party_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_title VARCHAR(200) NOT NULL COMMENT '任务标题',
    task_type VARCHAR(32) NOT NULL COMMENT '任务类型: PATROL巡查 / MEDIATION矛盾调解 / VISIT走访 / MEETING三会一课 / PUBLICITY政策宣传 / OTHER其他',
    description VARCHAR(500) COMMENT '任务描述',
    grid_id BIGINT COMMENT '所属网格',
    assigned_member_id BIGINT COMMENT '指派党员ID',
    deadline DATE COMMENT '截止日期',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态: PENDING待领办 / ACCEPTED已领办 / COMPLETED已完成 / CANCELLED已取消',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_grid (grid_id),
    INDEX idx_member (assigned_member_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党建任务下沉';

-- 2. 党群议事表
CREATE TABLE IF NOT EXISTS sys_party_deliberation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '议事议题',
    content TEXT COMMENT '议事内容',
    grid_id BIGINT COMMENT '所属网格',
    status VARCHAR(16) DEFAULT 'OPEN' COMMENT '状态: OPEN征集中 / CLOSED已结项',
    support_count INT DEFAULT 0 COMMENT '赞成数',
    oppose_count INT DEFAULT 0 COMMENT '反对数',
    abstain_count INT DEFAULT 0 COMMENT '弃权数',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_grid (grid_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党群议事';

-- 3. 议事投票/意见记录表
CREATE TABLE IF NOT EXISTS sys_party_deliberation_vote (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    deliberation_id BIGINT NOT NULL COMMENT '议事ID',
    user_id BIGINT NOT NULL COMMENT '投票人ID',
    vote_type VARCHAR(16) NOT NULL COMMENT 'SUPPORT赞成 / OPPOSE反对 / ABSTAIN弃权',
    comment VARCHAR(500) COMMENT '意见内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_deliber_user (deliberation_id, user_id),
    INDEX idx_deliberation (deliberation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='议事投票记录';

-- 4. 政策推送记录表
CREATE TABLE IF NOT EXISTS sys_policy_push (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL COMMENT '关联政策资源ID',
    push_target VARCHAR(32) NOT NULL COMMENT '推送目标: GRID网格 / POPULATION人群 / ALL全员',
    grid_id BIGINT COMMENT '目标网格ID（当target=GRID时）',
    push_count INT DEFAULT 0 COMMENT '推送人次',
    created_by BIGINT COMMENT '推送人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_policy (policy_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策推送记录';

-- 示例数据
INSERT INTO sys_party_task (task_title, task_type, description, grid_id, assigned_member_id, deadline, status, created_by) VALUES
('本周消防安全巡查', 'PATROL', '对网格内商铺开展消防安全检查', 1, 1, DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'ACCEPTED', 1),
('邻里纠纷调解', 'MEDIATION', '协调龙景小区邻里噪音矛盾', 1, 2, DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'PENDING', 1),
('独居老人走访', 'VISIT', '走访网格内独居老人', 2, 3, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'PENDING', 1);

INSERT INTO sys_party_deliberation (title, content, grid_id, status, created_by) VALUES
('小区停车位改造方案', '拟将小区东侧空地改造为停车位，征求党员群众意见', 1, 'OPEN', 1),
('社区文化活动选址', '年度文艺汇演选址：广场 vs 礼堂', 2, 'OPEN', 1);
