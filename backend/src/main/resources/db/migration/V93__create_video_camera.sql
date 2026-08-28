-- V93：固定摄像头监控点位表（视频轮巡）

CREATE TABLE IF NOT EXISTS biz_video_camera (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    camera_name VARCHAR(64) NOT NULL COMMENT '点位名称',
    camera_type VARCHAR(16) NOT NULL DEFAULT 'FIXED' COMMENT '类型: FIXED固定/PTZ球机',
    device_no VARCHAR(64) NOT NULL COMMENT '设备编号(平台侧唯一标识)',
    stream_type VARCHAR(16) NOT NULL DEFAULT 'HLS' COMMENT '流类型: HLS/FLV/RTSP',
    stream_url VARCHAR(512) NOT NULL COMMENT '视频流地址(或平台取流地址)',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
    address VARCHAR(128) DEFAULT NULL COMMENT '安装位置',
    grid_id BIGINT DEFAULT NULL COMMENT '所属网格ID',
    grid_name VARCHAR(64) DEFAULT NULL COMMENT '所属网格名称',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE在线/OFFLINE离线/MAINTENANCE维护',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_video_camera_device_no (device_no),
    KEY idx_video_camera_status (status),
    KEY idx_video_camera_grid (grid_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定摄像头监控点位';
