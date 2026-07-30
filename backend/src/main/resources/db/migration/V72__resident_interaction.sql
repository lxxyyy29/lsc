-- P2-3：居民互动补齐 —— 便民报修 / 志愿服务积分

-- 1. 便民报修表
CREATE TABLE IF NOT EXISTS biz_repair_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_name VARCHAR(64) COMMENT '报修人姓名',
    reporter_phone VARCHAR(20) COMMENT '报修人电话',
    reporter_user_id BIGINT COMMENT '报修人用户ID（注册用户）',
    repair_type VARCHAR(32) NOT NULL COMMENT '报修类型: WATER水电 / ELEVATOR电梯 / DOOR门禁 / PIPE管道 / ROOF屋面 / OTHER其他',
    title VARCHAR(200) NOT NULL COMMENT '报修标题',
    description TEXT COMMENT '报修描述',
    address VARCHAR(255) COMMENT '报修地址',
    grid_id BIGINT COMMENT '所属网格',
    photo_urls VARCHAR(1000) COMMENT '图片URL（逗号分隔）',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态: PENDING待处理 / ASSIGNED已派单 / PROCESSING处理中 / COMPLETED已完成 / REJECTED已驳回',
    handler_user_id BIGINT COMMENT '处理人ID',
    handle_result VARCHAR(500) COMMENT '处理结果',
    handled_at TIMESTAMP COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_grid (grid_id),
    INDEX idx_reporter (reporter_user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='便民报修';

-- 2. 志愿服务积分账户表
CREATE TABLE IF NOT EXISTS sys_volunteer_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    total_points INT DEFAULT 0 COMMENT '累计积分',
    available_points INT DEFAULT 0 COMMENT '可用积分',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='志愿服务积分账户';

-- 3. 积分流水表
CREATE TABLE IF NOT EXISTS sys_volunteer_points_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    points INT NOT NULL COMMENT '变动积分（正数增加/负数扣除）',
    reason VARCHAR(200) COMMENT '变动原因',
    source_type VARCHAR(32) COMMENT '来源: VOLUNTEER_ACTIVITY志愿活动 / REPAIR报修奖励 / EXCHANGE兑换',
    source_id BIGINT COMMENT '来源记录ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 示例数据
INSERT INTO biz_repair_request (reporter_name, reporter_phone, repair_type, title, description, address, grid_id, status) VALUES
('王女士', '13800138001', 'WATER', '厨房水管漏水', '厨房下水管破裂，急需维修', '龙景小区A栋101', 1, 'PENDING'),
('李先生', '13800138002', 'ELEVATOR', '电梯故障', '2单元电梯按键失灵', '龙景小区B栋', 1, 'ASSIGNED'),
('张阿姨', '13800138003', 'DOOR', '门禁失灵', '小区东门门禁无法刷卡', '建材城1号', 2, 'COMPLETED');
