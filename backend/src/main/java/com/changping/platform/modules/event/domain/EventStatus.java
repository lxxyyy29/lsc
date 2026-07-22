package com.changping.platform.modules.event.domain;

/**
 * @Author tangxinglin
 * @Description //事件生命周期状态枚举，定义事件从接入到最终关闭的全部阶段状态
 * @Date 2026/04/18 10:25
 */
public enum EventStatus {
    /** 等待派发工单 */
    WAITING_DISPATCH,
    /** 已派发至工单 */
    DISPATCHED_TO_WORK_ORDER,
    /** 已关闭 */
    CLOSED,
    /** 已忽略（误报等） */
    IGNORED
}
