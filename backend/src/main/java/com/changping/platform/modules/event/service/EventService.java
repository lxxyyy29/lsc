package com.changping.platform.modules.event.service;

import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.event.vo.EventDetailVo;

import java.util.List;

/**
 * @Author tangxinglin
 * @Description //事件业务服务接口，提供事件的创建、查询、分页列表及删除功能
 * @Date 2026/04/18 10:00
 */
public interface EventService {

    /**
     * @Author tangxinglin
     * @Description //创建新事件
     * @Date 2026/04/18 10:00
     * @Param [request 创建事件请求对象]
     * @return EventDetailVo 新建的事件详情
     */
    EventDetailVo createEvent(CreateEventRequest request);

    /**
     * @Author tangxinglin
     * @Description //根据主键ID获取事件详情
     * @Date 2026/04/18 10:00
     * @Param [id 事件主键ID]
     * @return EventDetailVo 事件详情
     */
    EventDetailVo getEventDetail(Long id);

    /**
     * 通过外部事件ID获取事件详情
     */
    EventDetailVo getEventDetailByExternalEventId(String externalEventId);

    /**
     * @Author tangxinglin
     * @Description //分页查询事件列表，支持多条件过滤
     * @Date 2026/04/18 10:00
     * @Param [externalEventId 外部事件ID（可选）, page 页码, size 每页条数, status 事件状态（可选）, startDate 开始日期（可选）, endDate 结束日期（可选）, areaId 区域ID（可选）]
     * @return PagedResult<EventDetailVo> 分页事件详情列表
     */
    PagedResult<EventDetailVo> queryEvents(String externalEventId, int page, int size, String status, String startDate, String endDate, Long areaId);

    /**
     * @Author tangxinglin
     * @Description //删除事件及其所有关联的子数据（工单、审核、媒体文件等）
     * @Date 2026/04/18 10:00
     * @Param [eventId 事件主键ID]
     * @return void
     */
    void deleteEvent(Long eventId);

    /**
     * 更新事件紧急程度
     */
    boolean updateUrgencyLevel(Long eventId, String urgencyLevel);

    /**
     * 三色分级自动升级：超期未处置的事件自动升级紧急程度
     * GREEN -> YELLOW (超过24小时)
     * YELLOW -> RED (超过48小时)
     */
    void autoEscalateUrgency();

    /**
     * 审核事件
     * @param id 事件ID
     * @param passed 是否通过
     * @param remark 备注
     */
    boolean auditEvent(Long id, boolean passed, String remark);

    /**
     * 关闭事件
     */
    void closeEvent(Long eventId, String reason);

    /**
     * 重新打开已关闭事件
     */
    void reopenEvent(Long eventId);

    /**
     * 获取事件生命周期时间轴
     */
    List<EventDetailVo.LifecycleRecordVo> getTimeline(Long eventId);

    /**
     * 事件统计数据
     */
    EventStatistics getStatistics();

    /**
     * 群众对事件处置结果进行评价
     */
    boolean rateEvent(Long id, int rating, String comment);

    /**
     * 事件统计VO
     */
    record EventStatistics(
            long total,
            long waitingDispatch,
            long dispatched,
            long closed,
            long ignored,
            long greenCount,
            long yellowCount,
            long redCount) {
    }
}
