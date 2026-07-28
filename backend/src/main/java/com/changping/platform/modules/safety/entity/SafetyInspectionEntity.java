package com.changping.platform.modules.safety.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_safety_inspection")
public class SafetyInspectionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private Long inspectorId;

    private String inspectorName;

    private LocalDate inspectionDate;

    private String fireRiskLevel;

    private String safetyStatus;

    private String hazardsFound;

    private Boolean rectificationRequired;

    private LocalDate rectificationDeadline;

    private String rectificationStatus;

    private String remarks;

    private String photoUrls;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
