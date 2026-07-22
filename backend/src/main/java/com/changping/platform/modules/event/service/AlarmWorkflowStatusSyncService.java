package com.changping.platform.modules.event.service;

import java.time.LocalDateTime;

/**
 * @Author tangxinglin
 * @Description //告警工作流状态同步服务接口，负责将事件工作流状态同步到外部存储（如 MongoDB）
 * @Date 2026/04/18 10:00
 */
public interface AlarmWorkflowStatusSyncService {

    /**
     * @Author tangxinglin
     * @Description //同步工作流状态快照到外部存储
     * @Date 2026/04/18 10:00
     * @Param [snapshot 包含事件ID、当前状态和同步时间的工作流状态快照]
     * @return void
     */
    void syncWorkflowStatus(WorkflowStatusSnapshot snapshot);

    /**
     * @Author tangxinglin
     * @Description //便捷方法：根据事件ID和当前状态构建快照并同步，同步时间默认为当前时间
     * @Date 2026/04/18 10:00
     * @Param [sqlEventId 事件数据库主键ID, currentStatus 当前事件状态]
     * @return void
     */
    default void syncWorkflowStatus(Long sqlEventId, String currentStatus) {
        syncWorkflowStatus(new WorkflowStatusSnapshot(sqlEventId, currentStatus, LocalDateTime.now()));
    }

    record WorkflowStatusSnapshot(Long sqlEventId, String currentStatus, LocalDateTime lastSyncedAt) {
    }
}
