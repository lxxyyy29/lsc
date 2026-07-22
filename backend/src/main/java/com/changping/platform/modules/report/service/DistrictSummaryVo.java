package com.changping.platform.modules.report.service;

/**
 * @Author tangxinglin
 * @Description //区域汇总视图对象，聚合单个区域的事件总量、各状态分类统计、工单数量及平均处理时长
 * @Date 2026/04/18 10:00
 */
public record DistrictSummaryVo(
        Long areaId,
        String areaName,
        Long totalEvents,
        Long pendingEvents,
        Long waitingDispatchEvents,
        Long processingEvents,
        Long closedEvents,
        Long ignoredEvents,
        Long totalWorkOrders,
        Long completedWorkOrders,
        Double avgCompletionHours) {
}
