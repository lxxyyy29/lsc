package com.changping.platform.modules.process.domain;

/**
 * @Author tangxinglin
 * @Description //流程实例运行时状态枚举，描述审核流程实例在执行阶段的各种状态
 * @Date 2026/04/18 10:00
 */
public enum ProcessStatus {
    /** 待处理（已创建但尚未开始） */
    PENDING,
    /** 运行中（审核流程进行中） */
    RUNNING,
    /** 已审批通过 */
    APPROVED,
    /** 已驳回 */
    REJECTED,
    /** 已终止 */
    TERMINATED
}
