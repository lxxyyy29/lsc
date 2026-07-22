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
 * @Description //流程模板实体，映射数据库表 biz_process_template，存储审核流程的模板定义及其节点配置
 * @Date 2026/04/18 10:00
 */
@Data
@TableName("biz_process_template")
public class ProcessTemplateEntity {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模板编码 */
    private String templateCode;
    /** 模板名称 */
    private String templateName;
    /** 业务类型（对应事件类型） */
    private String businessType;
    /** 版本号 */
    private Integer versionNo;
    /** 模板状态（ACTIVE/DISABLED/DRAFT） */
    private String status;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 是否启用（非数据库字段，由status派生） */
    @TableField(exist = false)
    private Boolean enabled;

    /** 事件类型（非数据库字段，与businessType同义） */
    @TableField(exist = false)
    private String eventType;

    /** 版本号（非数据库字段，与versionNo同义） */
    @TableField(exist = false)
    private Integer version;

    /** 模板节点列表（非数据库字段） */
    @TableField(exist = false)
    private List<ProcessTemplateNodeEntity> nodes = new ArrayList<>();
}
