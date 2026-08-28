-- 车位资源表
CREATE TABLE IF NOT EXISTS biz_parking_space (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    space_code VARCHAR(32) NOT NULL COMMENT '车位编号',
    space_type VARCHAR(16) DEFAULT 'NORMAL' COMMENT '车位类型: NORMAL/DISABLED/CHARGING/FIRE_LANE',
    area_id BIGINT COMMENT '所属区域ID',
    grid_id BIGINT COMMENT '所属网格ID',
    longitude DECIMAL(10, 6) COMMENT '经度',
    latitude DECIMAL(10, 6) COMMENT '纬度',
    address VARCHAR(255) COMMENT '地址描述',
    status VARCHAR(16) DEFAULT 'FREE' COMMENT '状态: FREE/OCCUPIED/RESERVED/DISABLED',
    vehicle_plate VARCHAR(16) COMMENT '当前停放车辆车牌',
    occupied_at TIMESTAMP COMMENT '占用时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_grid (grid_id),
    INDEX idx_area (area_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车位资源';

-- 违停记录表
CREATE TABLE IF NOT EXISTS biz_parking_violation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    space_id BIGINT COMMENT '车位ID',
    vehicle_plate VARCHAR(16) COMMENT '车牌号',
    violation_type VARCHAR(32) NOT NULL COMMENT '违停类型: ILLEGAL_PARKING/FIRE_LANE/OCCUPYING/OVERSTAY',
    longitude DECIMAL(10, 6) COMMENT '经度',
    latitude DECIMAL(10, 6) COMMENT '纬度',
    address VARCHAR(255) COMMENT '位置描述',
    photo_url VARCHAR(255) COMMENT '照片',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT '状态: PENDING/DISPATCHED/PROCESSING/CLOSED',
    dispatcher_id BIGINT COMMENT '派单网格员ID',
    remark VARCHAR(500) COMMENT '备注',
    occurred_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '违停时间',
    processed_at TIMESTAMP COMMENT '处理时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_type (violation_type),
    INDEX idx_occurred (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违停记录';

-- 插入示例车位数据
INSERT INTO biz_parking_space (space_code, space_type, grid_id, longitude, latitude, address, status) VALUES
('P-A001', 'NORMAL', 1, 113.939500, 22.971200, 'A区1号路', 'FREE'),
('P-A002', 'NORMAL', 1, 113.939600, 22.971300, 'A区1号路', 'OCCUPIED'),
('P-A003', 'FIRE_LANE', 1, 113.939700, 22.971400, 'A区消防通道', 'FREE'),
('P-B001', 'NORMAL', 2, 113.940000, 22.972000, 'B区2号路', 'FREE'),
('P-B002', 'CHARGING', 2, 113.940100, 22.972100, 'B区充电车位', 'OCCUPIED'),
('P-C001', 'NORMAL', 3, 113.941000, 22.973000, 'C区3号路', 'FREE'),
('P-C002', 'NORMAL', 3, 113.941100, 22.973100, 'C区3号路', 'OCCUPIED'),
('P-C003', 'DISABLED', 3, 113.941200, 22.973200, 'C区无障碍车位', 'FREE');

-- 插入示例违停数据
INSERT INTO biz_parking_violation (space_id, vehicle_plate, violation_type, longitude, latitude, address, status, occurred_at) VALUES
(3, '粤S·12345', 'FIRE_LANE', 113.939700, 22.971400, 'A区消防通道', 'PENDING', NOW()),
(5, '粤S·67890', 'OCCUPYING', 113.940100, 22.972100, 'B区充电车位', 'DISPATCHED', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(NULL, '粤S·11111', 'ILLEGAL_PARKING', 113.942000, 22.974000, 'C区主干路', 'CLOSED', DATE_SUB(NOW(), INTERVAL 2 HOUR));
