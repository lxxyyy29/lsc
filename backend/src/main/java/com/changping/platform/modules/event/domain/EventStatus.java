package com.changping.platform.modules.event.domain;

/**
 * @Author lxy
 * @Description //事件生命周期状态枚举，定义事件从接入到最终关闭的全部阶段状态
 * @Date 2026/04/18 10:25
 */
public enum EventStatus {
    /** 待审核（事件接入后先进入审核环节） */
    PENDING_AUDIT,
    /** 审核中（流程节点审批进行中） */
    IN_AUDIT,
    /** 审核通过 */
    AUDIT_APPROVED,
    /** 审核驳回 */
    AUDIT_REJECTED,
    /** 等待派发工单 */
    WAITING_DISPATCH,
    /** 等待网格组长审核派单（两级派单：事件先推送网格组长，再由组长派给下属网格员） */
    WAITING_LEADER_REVIEW,
    /** 已派发至工单 */
    DISPATCHED_TO_WORK_ORDER,
    /** 已关闭 */
    CLOSED,
    /** 已忽略（误报等） */
    IGNORED
}
