-- V108: 系统配置表（key-value），当前用于地图中心点配置
-- 地图中心点：map.center.lng / map.center.lat，地图类页面默认以该坐标为中心
CREATE TABLE IF NOT EXISTS sys_config (
    config_key VARCHAR(64) NOT NULL PRIMARY KEY COMMENT '配置键',
    config_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '配置值',
    remark VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT IGNORE INTO sys_config (config_key, config_value, remark) VALUES
('map.center.lng', '113.939521', '地图中心点-经度（拔蛟窝社区默认）'),
('map.center.lat', '22.971231', '地图中心点-纬度（拔蛟窝社区默认）');
