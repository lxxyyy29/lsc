package com.changping.platform.modules.integration.alarm.repository;

import com.changping.platform.modules.integration.alarm.document.AlarmEventDocument;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @Author tangxinglin
 * @Description //告警事件MongoDB数据访问接口，提供按外部事件ID、去重键、SQL事件ID等维度的查询及删除操作
 * @Date 2026/04/18 10:00
 */
public interface AlarmEventRepository extends MongoRepository<AlarmEventDocument, String> {

    Optional<AlarmEventDocument> findByExternalEventId(String externalEventId);

    Optional<AlarmEventDocument> findByIngestionDedupKey(String dedupKey);

    List<AlarmEventDocument> findByExternalEventIdContainingIgnoreCaseOrderByOccurredAtDesc(String externalEventId);

    List<AlarmEventDocument> findByExternalEventIdContainingIgnoreCaseOrderByOccurredAtDesc(String externalEventId, Pageable pageable);

    long countByExternalEventIdContainingIgnoreCase(String externalEventId);

    List<AlarmEventDocument> findByWorkflowStatusSqlEventIdIn(Collection<Long> sqlEventIds);

    long countByWorkflowStatusCurrentStatusNotIn(Collection<String> statuses);

    List<AlarmEventDocument> findByWorkflowStatusCurrentStatusNotIn(Collection<String> statuses, Pageable pageable);

    long countByExternalEventIdContainingIgnoreCaseAndWorkflowStatusCurrentStatusNotIn(String externalEventId, Collection<String> statuses);

    List<AlarmEventDocument> findByExternalEventIdContainingIgnoreCaseAndWorkflowStatusCurrentStatusNotIn(String externalEventId, Collection<String> statuses, Pageable pageable);

    void deleteAllByExternalEventId(String externalEventId);
}
