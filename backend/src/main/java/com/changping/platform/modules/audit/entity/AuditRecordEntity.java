package com.changping.platform.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author lxy
 * @Description //审核记录实体，对应数据库表 biz_audit_record，记录每次审核节点的决策、审核人和意见等信息
 * @Date 2026/04/18 10:10
 */
@Data
@TableName("biz_audit_record")
public class AuditRecordEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联事件ID */
    private Long eventId;
    /** 关联流程实例ID */
    private Long processInstanceId;
    /** 审核决策（如 APPROVE / REJECT） */
    private String decision;
    /** 审核状态 */
    private String status;
    /** 审核人用户ID */
    private Long auditorUserId;
    /** 审核人姓名 */
    private String auditorName;
    /** 审核意见 */
    private String opinion;
    /** 审核完成时间 */
    private LocalDateTime auditedAt;
    /** 记录创建时间 */
    private LocalDateTime createdAt;
    /** 记录最后更新时间 */
    private LocalDateTime updatedAt;
}
