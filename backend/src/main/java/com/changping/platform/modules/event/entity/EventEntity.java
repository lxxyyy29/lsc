package com.changping.platform.modules.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author lxy
 * @Description //事件实体，映射数据库表 biz_event，记录平台接收的各类告警/事件信息
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_event")
public class EventEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件编码 */
    private String eventCode;
    /** 外部系统事件ID */
    private String externalEventId;
    /** 事件标题 */
    private String title;
    /** 事件描述 */
    private String description;
    /** 来源类型 */
    private String sourceType;
    /** 来源系统 */
    private String sourceSystem;
    /** 事件类型 */
    private String eventType;
    /** 事件状态 */
    private String status;

    /** 事发地址 */
    @TableField("incident_address")
    private String location;

    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;
    /** 事件发生时间 */
    private LocalDateTime occurredAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
    /** 所属区域ID */
    private Long areaId;
    /** 所属区域名称 */
    private String areaName;
    /** 所属网格ID */
    private Long gridId;
    /** 紧急程度: GREEN=一般 YELLOW=重点 RED=紧急险情 */
    private String urgencyLevel;
    /** 事件来源: GRID_MEMBER=网格员 RESIDENT=居民 12345=12345转办 PROPERTY=物业 AI_CAMERA=AI监控 */
    private String reportSource;
    /** 是否已归档：0=活跃 1=已归档留存 */
    private Integer archived;
    /** 展示隐藏：0=显示 1=隐藏（仅事件闭环/工单中心可见） */
    private Integer hidden;
}
