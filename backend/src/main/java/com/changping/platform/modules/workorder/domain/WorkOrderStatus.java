package com.changping.platform.modules.workorder.domain;

/**
 * @Author tangxinglin
 * @Description //工单状态枚举，定义工单的生命周期状态
 * @Date 2026/04/18 09:10
 */
public enum WorkOrderStatus {
    /** 处理中 */
    PROCESSING,
    /** 已完成 */
    COMPLETED,
    /** 已关闭 */
    CLOSED,
    /** 已超时 */
    TIMEOUT
}
