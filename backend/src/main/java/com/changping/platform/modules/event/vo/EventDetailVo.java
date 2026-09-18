package com.changping.platform.modules.event.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author lxy
 * @Description //事件详情视图对象，聚合MySQL事件数据与MongoDB告警文档数据，用于前端展示
 * @Date 2026/04/18 10:00
 */
public record EventDetailVo(
        Long id,
        String eventCode,
        String externalEventId,
        String sourceType,
        String sourceSystem,
        String eventType,
        String title,
        String description,
        String status,
        String currentStatus,
        LocalDateTime occurredAt,
        String location,
        String area,
        BigDecimal longitude,
        BigDecimal latitude,
        List<String> evidenceReferences,
        List<LifecycleRecordVo> lifecycleRecords,
        Long processTemplateId,
        String processTemplateName,
        String currentNodeName,
        String currentNodeStatus,
        Boolean dispatchable,
        Long areaId,
        String areaName,
        Long gridId,
        String gridName,
        String urgencyLevel,
        String reportSource,
        String reportUserName,
        String reportPhone,
        Boolean archived,
        Boolean hidden,
        Boolean deleted,
        String deletedReason,
        /** 当前跟进的受派人：取该事件最新一张工单的受派人，未派单时为空 */
        String assigneeName) {

    @JsonProperty
    public String statusLabel() {
        if (status == null) return "";
        return switch (status) {
            case "PENDING_AUDIT" -> "待审核";
            case "IN_AUDIT" -> "审核中";
            case "AUDIT_APPROVED" -> "已通过";
            case "AUDIT_REJECTED" -> "已驳回";
            case "WAITING_DISPATCH" -> "待派单";
            case "WAITING_LEADER_REVIEW" -> "组长审核";
            case "DISPATCHED_TO_WORK_ORDER" -> "已派单";
            case "CLOSED" -> "已关闭";
            case "IGNORED" -> "已忽略";
            default -> status;
        };
    }

    @JsonProperty
    public String urgencyLabel() {
        if (urgencyLevel == null) return "";
        return switch (urgencyLevel) {
            case "GREEN" -> "一般";
            case "YELLOW" -> "重点";
            case "RED" -> "紧急";
            default -> urgencyLevel;
        };
    }

    /**
     * 生命周期记录（处置时间轴条目）。
     *
     * <p>前 4 个字段为历史兼容字段，前端旧逻辑按「操作人 — 备注」渲染 remark；
     * 其余为结构化补充，用于回答「谁、以什么角色、做了什么、交给了谁」，例如派单环节的派单人与受派人。
     */
    public record LifecycleRecordVo(
            String action,
            String status,
            String remark,
            LocalDateTime occurredAt,
            /** 原始动作码（如 LEADER_DISPATCH），便于前端按动作着色/取图标 */
            String rawAction,
            /** 操作人姓名 */
            String operatorName,
            /** 操作人角色（如「网格组长」「网格员」「管理员」），无则 null */
            String operatorRole,
            /** 操作人填写的备注原文（不含操作人姓名） */
            String operatorRemark,
            /** 该动作涉及的对象摘要，如「派单给 刘核实」「处置结论：需补充证据」 */
            String detail,
            /** 关联工单号，无工单时为 null */
            String workOrderNo,
            /** 关联工单当前状态中文 */
            String workOrderStatus) {
    }
}
