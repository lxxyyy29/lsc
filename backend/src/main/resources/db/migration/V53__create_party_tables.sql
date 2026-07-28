-- 党员信息表
CREATE TABLE IF NOT EXISTS sys_party_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE COMMENT '关联用户ID',
    party_branch VARCHAR(128) COMMENT '党支部',
    join_date DATE COMMENT '入党日期',
    grid_id BIGINT COMMENT '联系网格ID',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_grid (grid_id),
    INDEX idx_branch (party_branch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党员信息';

-- 党员联户关系表
CREATE TABLE IF NOT EXISTS sys_party_household (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_member_id BIGINT NOT NULL COMMENT '党员ID',
    population_id BIGINT COMMENT '关联人口ID',
    household_name VARCHAR(64) COMMENT '户主姓名',
    household_address VARCHAR(255) COMMENT '家庭地址',
    grid_id BIGINT COMMENT '所属网格',
    visit_count INT DEFAULT 0 COMMENT '走访次数',
    last_visit_date DATE COMMENT '最近走访日期',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member (party_member_id),
    INDEX idx_grid (grid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党员联户关系';

-- 志愿服务活动表
CREATE TABLE IF NOT EXISTS sys_volunteer_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL COMMENT '活动标题',
    description VARCHAR(500) COMMENT '活动描述',
    activity_date DATE COMMENT '活动日期',
    grid_id BIGINT COMMENT '所属网格',
    max_participants INT COMMENT '最大参与人数',
    status VARCHAR(16) DEFAULT 'PLANNED' COMMENT '状态: PLANNED/ONGOING/COMPLETED/CANCELLED',
    created_by BIGINT COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (activity_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿服务活动';

-- 志愿者报名记录
CREATE TABLE IF NOT EXISTS sys_volunteer_signup (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '报名人ID',
    status VARCHAR(16) DEFAULT 'SIGNED_UP' COMMENT '状态: SIGNED_UP/ATTENDED/ABSENT',
    check_in_time TIMESTAMP COMMENT '签到时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity (activity_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿者报名记录';

-- 三会一课记录表
CREATE TABLE IF NOT EXISTS sys_party_meeting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_type VARCHAR(32) NOT NULL COMMENT '会议类型: 支部党员大会/支委会/党小组会/党课',
    title VARCHAR(128) NOT NULL COMMENT '主题',
    meeting_date DATE NOT NULL COMMENT '会议日期',
    party_branch VARCHAR(128) COMMENT '党支部',
    content TEXT COMMENT '会议内容',
    participant_count INT COMMENT '参会人数',
    status VARCHAR(16) DEFAULT 'PLANNED' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (meeting_date),
    INDEX idx_type (meeting_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三会一课记录';

-- 党员量化考核记录
CREATE TABLE IF NOT EXISTS sys_party_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    party_member_id BIGINT NOT NULL COMMENT '党员ID',
    assessment_month VARCHAR(7) COMMENT '考核月份: YYYY-MM',
    patrol_count INT DEFAULT 0 COMMENT '参与巡查次数',
    mediation_count INT DEFAULT 0 COMMENT '矛盾调解次数',
    volunteer_hours DECIMAL(5,1) DEFAULT 0 COMMENT '志愿服务时长',
    meeting_attendance INT DEFAULT 0 COMMENT '会议出勤次数',
    total_score DECIMAL(5,1) DEFAULT 0 COMMENT '综合得分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_member_month (party_member_id, assessment_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='党员量化考核';

-- 插入示例数据
INSERT INTO sys_party_member (user_id, party_branch, join_date, grid_id) VALUES
(1, '拔蛟窝社区党支部', '2015-07-01', 1),
(2, '拔蛟窝社区党支部', '2018-03-15', 2),
(3, '拔蛟窝社区第一党小组', '2020-06-20', 3);

INSERT INTO sys_party_household (party_member_id, household_name, household_address, grid_id, visit_count, last_visit_date) VALUES
(1, '张三', '龙景小区A栋101', 1, 5, CURDATE()),
(1, '李四', '龙景小区B栋202', 1, 3, DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(2, '王五', '建材城1号', 2, 8, CURDATE()),
(3, '赵六', '市场路3号', 3, 2, DATE_SUB(CURDATE(), INTERVAL 14 DAY));

INSERT INTO sys_volunteer_activity (title, description, activity_date, grid_id, max_participants, status) VALUES
('环境清洁志愿活动', '社区环境卫生清理', DATE_ADD(CURDATE(), INTERVAL 3 DAY), 1, 20, 'PLANNED'),
('关爱老人志愿服务', '走访独居老人', DATE_SUB(CURDATE(), INTERVAL 7 DAY), 2, 10, 'COMPLETED'),
('文明交通劝导', '路口交通秩序维护', DATE_SUB(CURDATE(), INTERVAL 14 DAY), 3, 15, 'COMPLETED');

INSERT INTO sys_party_meeting (meeting_type, title, meeting_date, party_branch, participant_count, status) VALUES
('支部党员大会', '2026年第三季度党员大会', DATE_SUB(CURDATE(), INTERVAL 10 DAY), '拔蛟窝社区党支部', 25, 'COMPLETED'),
('支委会', '研究社区治理重点工作', DATE_SUB(CURDATE(), INTERVAL 5 DAY), '拔蛟窝社区党支部', 8, 'COMPLETED'),
('党课', '学习党纪党规', DATE_ADD(CURDATE(), INTERVAL 7 DAY), '拔蛟窝社区党支部', 30, 'PLANNED');
