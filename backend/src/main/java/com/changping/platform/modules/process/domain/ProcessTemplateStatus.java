package com.changping.platform.modules.process.domain;

/**
 * @Author lxy
 * @Description //流程模板可用性状态枚举，描述模板从草稿到启用/禁用的配置生命周期
 * @Date 2026/04/18 10:00
 */
public enum ProcessTemplateStatus {
    /** 草稿状态（尚未启用） */
    DRAFT,
    /** 已启用（可用于创建流程实例） */
    ACTIVE,
    /** 已禁用 */
    DISABLED
}
