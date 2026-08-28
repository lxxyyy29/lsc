package com.changping.platform.modules.event.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author lxy
 * @Description //事件忽略记录实体，映射数据库表 biz_event_ignore_record，记录事件被标记为误报的操作信息
 * @Date 2026/04/18 10:00
 */
@Data
public class EventIgnoreRecord {

    /** 主键ID */
    private Long id;
    /** 关联的事件ID */
    private Long eventId;
    /** 操作人用户ID */
    private Long operatorId;
    /** 操作人名称 */
    private String operatorName;
    /** 忽略（误报）原因 */
    private String reason;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
