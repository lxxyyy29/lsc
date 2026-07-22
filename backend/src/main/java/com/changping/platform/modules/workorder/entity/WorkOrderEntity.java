package com.changping.platform.modules.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author tangxinglin
 * @Description //工单实体类，对应数据库表 biz_work_order，记录工单全生命周期信息
 * @Date 2026/04/18 09:20
 */
@Data
@TableName("biz_work_order")
public class WorkOrderEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 工单编号 */
    private String workOrderNo;
    /** 关联源事件ID */
    private Long sourceEventId;
    /** 关联流程实例ID */
    private Long processInstanceId;
    /** 工单状态 */
    private String status;
    /** 当前处理人用户ID */
    private Long assigneeUserId;
    /** 当前处理人姓名 */
    private String assigneeName;
    /** 派单人用户ID */
    private Long dispatcherUserId;
    /** 派单人姓名 */
    private String dispatcherName;
    /** 完成时间 */
    private LocalDateTime completedAt;
    /** 关闭时间 */
    private LocalDateTime closedAt;
    /** 关闭原因 */
    private String closeReason;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 最后更新时间 */
    private LocalDateTime updatedAt;
}
