package com.changping.platform.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author tangxinglin
 * @Description //流程实例节点实体，映射数据库表 biz_process_instance_node，记录流程实例中每个审批节点的执行状态
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_process_instance_node")
public class ProcessInstanceNodeEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的流程实例ID */
    private Long processInstanceId;
    /** 关联的模板节点ID */
    private Long templateNodeId;
    /** 节点唯一标识Key */
    private String nodeKey;
    /** 节点名称 */
    private String nodeName;
    /** 节点序号 */
    private Integer nodeOrder;
    /** 节点状态（WAITING/PENDING/APPROVED/REJECTED） */
    private String status;
    /** 所属审批周期编号（驳回后重新提交时递增） */
    private Integer cycleNo;
    /** 是否为当前活跃节点 */
    private Boolean isCurrent;
    /** 节点模式（SEQUENTIAL/PARALLEL） */
    private String nodeMode;
    /** 指派审批人用户ID */
    private Long assigneeUserId;
    /** 指派审批人名称 */
    private String assigneeName;
    /** 节点处理时间 */
    private LocalDateTime handledAt;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
