package com.changping.platform.modules.event.service;

import com.changping.platform.common.response.PagedResult;
import com.changping.platform.modules.event.vo.EventIgnoreRecordVo;

/**
 * @Author lxy
 * @Description //事件忽略（误报）服务接口，提供将事件标记为误报及查询误报记录的能力
 * @Date 2026/04/18 10:00
 */
public interface EventIgnoreService {

    /**
     * Mark an event as ignored (false alarm).
     *
     * @param eventId    SQL primary key of the event
     * @param operatorId operator's user id
     * @param operatorName operator's display name
     * @param reason     reason for ignoring / marking as false alarm
     */
    void ignoreEvent(Long eventId, Long operatorId, String operatorName, String reason);

    /**
     * @Author lxy
     * @Description //分页查询误报（已忽略）事件记录列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码, size 每页条数]
     * @return PagedResult<EventIgnoreRecordVo> 分页误报记录列表
     */
    PagedResult<EventIgnoreRecordVo> listIgnoreRecords(int page, int size);
}
