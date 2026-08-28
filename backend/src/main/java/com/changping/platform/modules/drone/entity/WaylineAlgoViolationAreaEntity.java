package com.changping.platform.modules.drone.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author lxy
 * @Description //航线算法绑定与违章区域的多对多关联实体,映射 biz_wayline_algo_violation_area
 * @Date 2026/04/22 00:00
 */
@Data
public class WaylineAlgoViolationAreaEntity {
    private Long id;
    private String flyLineId;
    private String label;
    private Integer startPointIndex;
    private Integer endPointIndex;
    private Long violationAreaId;
    private LocalDateTime createdAt;
}
