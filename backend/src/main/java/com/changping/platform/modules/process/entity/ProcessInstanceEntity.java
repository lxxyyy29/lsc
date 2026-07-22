package com.changping.platform.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * @Author tangxinglin
 * @Description //流程实例实体，映射数据库表 biz_process_instance，记录基于模板创建的审核流程实例的运行信息
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_process_instance")
public class ProcessInstanceEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 流程实例编号 */
    private String processNo;
    /** 关联的流程模板ID */
    private Long templateId;
    /** 使用的模板版本号 */
    private Integer templateVersion;
    /** 业务类型（如 EVENT_AUDIT） */
    private String businessType;
    /** 业务主键ID（如事件ID） */
    private Long businessId;
    /** 流程实例状态 */
    private String status;
    /** 当前节点序号 */
    private Integer currentNodeOrder;
    /** 流程启动时间 */
    private LocalDateTime startedAt;
    /** 流程结束时间 */
    private LocalDateTime finishedAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 流程实例ID（非数据库字段，用于响应扩展） */
    @TableField(exist = false)
    private Long processInstanceId;

    /** 关联的事件ID（非数据库字段，用于响应扩展） */
    @TableField(exist = false)
    private Long eventId;

    /** 关联事件的状态（非数据库字段，用于响应扩展） */
    @TableField(exist = false)
    private String eventStatus;

    /** 流程状态（非数据库字段，用于响应扩展） */
    @TableField(exist = false)
    private String processStatus;

    /** 当前周期的流程节点列表（非数据库字段） */
    @TableField(exist = false)
    private List<ProcessInstanceNodeEntity> nodes = new ArrayList<>();
}
