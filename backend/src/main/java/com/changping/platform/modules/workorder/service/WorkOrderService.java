package com.changping.platform.modules.workorder.service;

import com.changping.platform.modules.workorder.entity.WorkOrderEntity;
import java.util.List;
import java.util.Map;

/**
 * @Author tangxinglin
 * @Description //工单服务接口，定义工单派发、处理、查询及删除等核心业务操作
 * @Date 2026/04/18 09:25
 */
public interface WorkOrderService {

    /**
     * @Author tangxinglin
     * @Description //将事件派发为工单，创建流程实例并分配首个节点处理人
     * @Date 2026/04/18 09:25
     * @Param [eventId 事件ID, request 派发请求，包含流程模板ID和备注]
     * @return WorkOrderEntity 新建的工单实体
     */
    WorkOrderEntity dispatch(Long eventId, DispatchRequest request);

    /**
     * 获取工单附件列表
     */
    List<Map<String, Object>> getAttachments(Long workOrderId);

    /**
     * 确认关闭工单（Web端）
     */
    WorkOrderEntity confirmClose(Long workOrderId, String remark);

    /**
     * 驳回关闭（Web端）→ 工单退回处理中
     */
    WorkOrderEntity rejectClose(Long workOrderId, String remark);

    /**
     * @Author tangxinglin
     * @Description //处理工单节点，根据结果推进或关闭流程
     * @Date 2026/04/18 09:25
     * @Param [workOrderId 工单ID, request 处理请求，包含处理结果、备注及附件]
     * @return WorkOrderEntity 更新后的工单实体
     */
    WorkOrderEntity handle(Long workOrderId, HandleRequest request);

    /**
     * @Author tangxinglin
     * @Description //查询Web端全量工单列表（不分页）
     * @Date 2026/04/18 09:25
     * @Param []
     * @return List<WebWorkOrderSummary> 工单汇总列表
     */
    List<WebWorkOrderSummary> queryWebWorkOrders();

    /**
     * @Author tangxinglin
     * @Description //分页查询Web端工单列表，支持多条件过滤
     * @Date 2026/04/18 09:25
     * @Param [page 页码, pageSize 每页条数, status 状态筛选, assignee 经办人筛选, areaId 区域ID筛选]
     * @return PagedWorkOrders 分页工单结果
     */
    PagedWorkOrders queryWebWorkOrdersPaged(int page, int pageSize, String status, String assignee, Long areaId);

    /**
     * @Author tangxinglin
     * @Description //获取Web端工单详情，包含流程流转记录
     * @Date 2026/04/18 09:25
     * @Param [workOrderId 工单ID]
     * @return WebWorkOrderDetail 工单详情
     */
    WebWorkOrderDetail getWebWorkOrderDetail(Long workOrderId);

    /**
     * @Author tangxinglin
     * @Description //查询H5端当前用户参与的工单列表
     * @Date 2026/04/18 09:25
     * @Param []
     * @return List<H5WorkOrderListItem> H5工单列表
     */
    List<H5WorkOrderListItem> queryH5WorkOrders();

    /**
     * @Author tangxinglin
     * @Description //获取工单实体（H5侧，会校验当前用户是否为参与者）
     * @Date 2026/04/18 09:25
     * @Param [workOrderId 工单ID]
     * @return WorkOrderEntity 工单实体
     */
    WorkOrderEntity getWorkOrderDetail(Long workOrderId);

    /**
     * @Author tangxinglin
     * @Description //删除工单及其关联的流程实例、节点、操作记录和媒体文件
     * @Date 2026/04/18 09:25
     * @Param [workOrderId 工单ID]
     * @return void
     */
    void deleteWorkOrder(Long workOrderId);

    /**
     * @Author tangxinglin
     * @Description //获取H5端工单详情，包含流程节点和操作记录
     * @Date 2026/04/18 09:25
     * @Param [workOrderId 工单ID]
     * @return H5WorkOrderDetail H5工单详情
     */
    H5WorkOrderDetail getH5WorkOrderDetail(Long workOrderId);

    /**
     * @Author tangxinglin
     * @Description //获取H5工作台统计数据，汇总各状态工单数量
     * @Date 2026/04/18 09:25
     * @Param []
     * @return H5WorkbenchSummary 工作台汇总信息
     */
    H5WorkbenchSummary getH5Workbench();

    /**
     * 设置工单展示隐藏状态（隐藏后仅工单中心可见，大屏等面板不再统计展示）
     */
    boolean setWorkOrderHidden(Long workOrderId, boolean hidden);

    record DispatchRequest(Long assigneeUserId, String remark) {
    }

    record HandleAttachment(
            String fileName,
            String fileUrl,
            String fileType,
            String mimeType) {
    }

    record HandleRequest(String result, String remark, List<HandleAttachment> attachments, String subjectType, Long subjectId) {
    }

    record H5WorkbenchSummary(
            int totalCount,
            int waitingAcceptCount,
            int pendingCloseCount,
            int closedCount) {
    }

    record H5WorkOrderListItem(
            Long id,
            String workOrderNo,
            String status,
            Long assigneeUserId,
            String assigneeName,
            String dispatcherName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            String eventTitle,
            String currentNodeName,
            boolean isCurrentHandler,
            String areaName,
            String urgencyLevel) {
    }

    record H5ProcessNodeVo(
            Integer nodeOrder,
            String nodeName,
            Long assigneeUserId,
            String assigneeName,
            String status) {
    }

    record H5ActionRecordVo(
            String action,
            String result,
            String remark,
            java.time.LocalDateTime operatedAt,
            String operatorName,
            Integer nodeOrder,
            String subjectType,
            Long subjectId,
            String subjectName,
            @com.fasterxml.jackson.annotation.JsonRawValue String attachments) {
    }

    record H5WorkOrderDetail(
            Long id,
            String workOrderNo,
            String status,
            Long assigneeUserId,
            String assigneeName,
            String dispatcherName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            Long sourceEventId,
            String eventTitle,
            String eventLocation,
            String eventDescription,
            String merchantName,
            String eventType,
            boolean isCurrentHandler,
            java.util.List<H5ProcessNodeVo> processNodes,
            java.util.List<H5ActionRecordVo> actionRecords) {
    }

    record WebWorkOrderSummary(
            Long id,
            String workOrderNo,
            Long sourceEventId,
            String eventCode,
            String eventTitle,
            String status,
            String assigneeName,
            String dispatcherName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            Long areaId,
            String areaName,
            String urgencyLevel,
            Boolean hidden) {
    }

    record PagedWorkOrders(
            java.util.List<WebWorkOrderSummary> items,
            long total,
            int page,
            int pageSize) {
    }

    record WebWorkOrderDetail(
            Long id,
            String workOrderNo,
            Long sourceEventId,
            Long processInstanceId,
            String status,
            String assigneeName,
            String dispatcherName,
            String eventCode,
            String eventTitle,
            String eventType,
            String sourceType,
            String eventStatus,
            String description,
            String closeReason,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            java.time.LocalDateTime completedAt,
            java.time.LocalDateTime closedAt,
            List<WebWorkOrderFlowRecord> flowRecords,
            String urgencyLevel) {
    }

    record WebWorkOrderFlowRecord(
            Long id,
            String action,
            String status,
            String remark,
            String operatorName,
            String nodeName,
            java.time.LocalDateTime occurredAt,
            String subjectType,
            Long subjectId,
            String subjectName,
            @com.fasterxml.jackson.annotation.JsonRawValue String attachments) {
    }
}
