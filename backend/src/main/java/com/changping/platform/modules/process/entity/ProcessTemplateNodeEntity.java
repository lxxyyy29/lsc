package com.changping.platform.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @Author tangxinglin
 * @Description //流程模板节点实体，映射数据库表 biz_process_template_node，定义流程模板中每个审批节点的配置信息
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_process_template_node")
public class ProcessTemplateNodeEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的模板ID */
    private Long templateId;
    /** 节点唯一标识Key */
    private String nodeKey;
    /** 节点名称 */
    private String nodeName;
    /** 节点序号 */
    private Integer nodeOrder;
    /** 审批模式（SEQUENTIAL/PARALLEL） */
    private String approveMode;
    /** 节点状态 */
    private String status;
    /** 角色编码（可选，用于角色级别的节点权限） */
    private String roleCode;
    /** 备注 */
    private String remark;
    /** 指派审批人用户ID */
    private Long assigneeUserId;
    /** 指派审批人名称 */
    private String assigneeName;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 节点模式（非数据库字段，与approveMode同义） */
    @TableField(exist = false)
    private String nodeMode;
}
