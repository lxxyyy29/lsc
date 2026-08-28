-- 车辆轨迹追踪（R10 停车管理 - 车辆与人员轨迹追踪）
-- 结合视频监控 AI 分析,记录车辆进出与移动轨迹,支持至少 7 天历史回溯
CREATE TABLE IF NOT EXISTS biz_vehicle_track_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vehicle_plate VARCHAR(16) NOT NULL COMMENT '车牌号',
    track_type VARCHAR(16) NOT NULL DEFAULT 'MOVE' COMMENT '轨迹类型:ENTER进入/EXIT离开/MOVE移动',
    camera_id BIGINT NULL COMMENT '抓拍点位ID(关联biz_video_camera)',
    camera_name VARCHAR(100) NULL COMMENT '抓拍点位名称',
    longitude DECIMAL(10,6) NULL COMMENT '经度',
    latitude DECIMAL(10,6) NULL COMMENT '纬度',
    address VARCHAR(255) NULL COMMENT '位置描述',
    speed DECIMAL(6,2) NULL COMMENT '速度(km/h)',
    captured_at TIMESTAMP NOT NULL COMMENT '抓拍时间',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_plate_time (vehicle_plate, captured_at),
    KEY idx_type_time (track_type, captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆轨迹记录(视频AI抓拍,7天回溯)';
