package com.changping.platform.modules.workorder.domain;

/**
 * @Author tangxinglin
 * @Description //工单状态枚举，定义工单的生命周期状态
 * @Date 2026/04/18 09:10
 */
public enum WorkOrderStatus {
    /** 待接单 */
    WAITING_ACCEPT,
    /** 处理中 */
    PROCESSING,
    /** 待核实 */
    WAITING_VERIFY,
    /** 待关闭确认 */
    WAITING_CLOSE_CONFIRM,
    /** 已完成 */
    COMPLETED,
    /** 已关闭 */
    CLOSED,
    /** 已超时 */
    TIMEOUT
}
