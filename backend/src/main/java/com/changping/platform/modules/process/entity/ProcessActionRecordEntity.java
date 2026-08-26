package com.changping.platform.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author lxy
 * @Description //流程操作记录实体，映射数据库表 biz_process_action_record，记录流程实例各节点的审批操作历史
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_process_action_record")
public class ProcessActionRecordEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的流程实例ID */
    private Long processInstanceId;
    /** 关联的流程实例节点ID */
    private Long processInstanceNodeId;
    /** 操作类型（如 AUDIT_START、APPROVE、REJECT） */
    private String actionType;
    /** 操作结果 */
    private String actionResult;
    /** 操作人用户ID */
    private Long operatorUserId;
    /** 操作人名称 */
    private String operatorName;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
