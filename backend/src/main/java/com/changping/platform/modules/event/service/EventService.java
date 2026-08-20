package com.changping.platform.modules.event.service;

import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.event.dto.CreateEventRequest;
import com.changping.platform.modules.event.vo.EventDetailVo;

import java.util.List;
import java.util.Map;

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
     * 12345 政务热线转办导入：创建事件并标记来源为 12345
     * @param title 标题
     * @param description 描述
     * @param eventType 事件类型
     * @param location 地点
     * @param reporterName 来电人姓名
     * @param reporterPhone 来电人电话
     * @param externalNo 12345 转办单号
     * @return 新建的事件详情
     */
    EventDetailVo importFrom12345(String title, String description, String eventType, String location,
                                  String reporterName, String reporterPhone, String externalNo);

    /**
     * 物业上报：创建事件并标记来源为 PROPERTY
     * @param title 标题
     * @param description 描述
     * @param eventType 事件类型
     * @param location 地点
     * @param reporterName 上报人
     * @param propertyName 物业名称/小区名
     * @return 新建的事件详情
     */
    EventDetailVo reportFromProperty(String title, String description, String eventType, String location,
                                    String reporterName, String propertyName);

    /**
     * 居民上报统一归口：居民上报/报修提交时自动生成事件进入闭环，来源标记为 RESIDENT
     * @param title 标题
     * @param description 描述
     * @param eventType 事件类型
     * @param location 地点
     * @param reporterName 上报人姓名
     * @param reporterPhone 上报人电话
     * @param reporterUserId 上报人账号ID（可为 null）
     * @param longitude 经度（可为 null）
     * @param latitude 纬度（可为 null）
     * @return 新建的事件详情
     */
    EventDetailVo reportFromResident(String title, String description, String eventType, String location,
                                     String reporterName, String reporterPhone, Long reporterUserId,
                                     java.math.BigDecimal longitude, java.math.BigDecimal latitude);

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
     * 归档事件：关闭/忽略后的案件标记为已归档留存，从活跃视图过滤
     * @param id 事件ID
     */
    void archiveEvent(Long id);

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
     * 热力图数据：有坐标的事件列表
     */
    List<Map<String, Object>> getHeatmapData(String startDate, String endDate, String eventType);

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
