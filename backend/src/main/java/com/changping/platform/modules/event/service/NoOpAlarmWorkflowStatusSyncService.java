package com.changping.platform.modules.event.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * @Author tangxinglin
 * @Description //告警工作流状态同步服务的空实现，在没有其他实现Bean时自动注册，仅作占位，不执行任何同步操作
 * @Date 2026/04/18 10:00
 */
@Service
@ConditionalOnMissingBean(AlarmWorkflowStatusSyncService.class)
public class NoOpAlarmWorkflowStatusSyncService implements AlarmWorkflowStatusSyncService {

    /**
     * @Author tangxinglin
     * @Description //空实现，不执行任何操作，待 MongoDB 实现可用后替换
     * @Date 2026/04/18 10:00
     * @Param [snapshot 工作流状态快照]
     * @return void
     */
    @Override
    public void syncWorkflowStatus(WorkflowStatusSnapshot snapshot) {
        // Intentionally no-op until a Mongo-backed implementation is available.
    }
}
