package com.changping.platform.modules.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.changping.platform.common.jackson.PhotoUrlsDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cmn_resident_report")
public class ResidentReportEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long gridId;

    private String residentName;

    private String residentPhone;

    private String reportType;

    private String title;

    private String content;

    @JsonDeserialize(using = PhotoUrlsDeserializer.class)
    private String photoUrls;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String queryCode;

    /** 归口生成的事件 ID（居民上报统一进入事件闭环处理中心） */
    private Long eventId;

    private String status;

    private Long handlerUserId;

    private String handleResult;

    private LocalDateTime handledAt;

    @TableField(exist = false)
    private String gridName;

    @TableField(exist = false)
    private String handlerUserName;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
