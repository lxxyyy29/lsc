package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cmn_org_member")
public class OrgMemberEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gridId;

    private Long sysUserId;

    private String memberType;

    private String name;

    private String phone;

    private String position;

    private String status;

    private String remark;

    /** 所属组长（cmn_org_member.id），用于网格员划分 */
    private Long leaderId;

    @TableField(exist = false)
    private String gridName;

    /** 组长姓名（查询时 JOIN 填充，非表字段） */
    @TableField(exist = false)
    private String leaderName;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
