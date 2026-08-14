-- V94：应急会商/一键多方联合调度（R06 大屏端应急调度）

CREATE TABLE IF NOT EXISTS biz_emergency_dispatch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispatch_no VARCHAR(40) NOT NULL COMMENT '指令编号',
    title VARCHAR(200) NOT NULL COMMENT '指令标题',
    type VARCHAR(32) NOT NULL DEFAULT 'OTHER' COMMENT '事件类型: RAIN暴雨/FIRE火灾/MASS群体性事件/OTHER其他',
    level VARCHAR(32) NOT NULL DEFAULT 'COMMUNITY' COMMENT '调度级别: COMMUNITY社区/GRID大网格/SUB_GRID小网格',
    grid_id BIGINT DEFAULT NULL COMMENT '目标网格ID(大/小网格级别时)',
    grid_name VARCHAR(100) DEFAULT NULL COMMENT '目标网格名称',
    content TEXT NOT NULL COMMENT '指令内容',
    event_id BIGINT DEFAULT NULL COMMENT '关联事件ID',
    event_code VARCHAR(64) DEFAULT NULL COMMENT '关联事件编号',
    video_camera_ids VARCHAR(500) DEFAULT NULL COMMENT '附带视频点位ID(逗号分隔)',
    meeting_url VARCHAR(500) DEFAULT NULL COMMENT '视频会议地址(预留外接会议系统)',
    status VARCHAR(32) NOT NULL DEFAULT 'DISPATCHED' COMMENT '状态: DISPATCHED已下达/RESPONDING响应中/COMPLETED已完成',
    creator_user_id BIGINT DEFAULT NULL COMMENT '发起人ID',
    creator_name VARCHAR(100) DEFAULT NULL COMMENT '发起人姓名',
    dispatch_time DATETIME DEFAULT NULL COMMENT '下达时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_emergency_dispatch_no (dispatch_no),
    KEY idx_emergency_dispatch_status (status),
    KEY idx_emergency_dispatch_level (level),
    KEY idx_emergency_dispatch_grid (grid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急调度指令';

CREATE TABLE IF NOT EXISTS biz_emergency_receipt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispatch_id BIGINT NOT NULL COMMENT '指令ID',
    user_id BIGINT NOT NULL COMMENT '接收人用户ID',
    user_name VARCHAR(100) DEFAULT NULL COMMENT '接收人姓名',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING未接收/RECEIVED已接收/RESPONDING响应中/COMPLETED已完成',
    feedback TEXT DEFAULT NULL COMMENT '反馈内容',
    received_at DATETIME DEFAULT NULL COMMENT '接收时间',
    responded_at DATETIME DEFAULT NULL COMMENT '响应时间',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_emergency_receipt_dispatch_user (dispatch_id, user_id),
    KEY idx_emergency_receipt_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应急调度接收回执';
