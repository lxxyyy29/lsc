package com.changping.platform.modules.auth.security;

/**
 * @Author tangxinglin
 * @Description //权限码常量类，集中定义系统中所有菜单权限、API权限和按钮权限的字符串常量，供权限校验时引用
 * @Date 2026/04/18 09:55
 */
public final class PermissionCodes {

    /** Web端仪表盘查看权限 */
    public static final String MENU_DASHBOARD_VIEW = "menu:dashboard:view";
    /** Web端事件列表菜单权限 */
    public static final String MENU_EVENT_LIST = "menu:event:list";
    /** Web端审核列表菜单权限 */
    public static final String MENU_AUDIT_LIST = "menu:audit:list";
    /** H5端工作台查看菜单权限 */
    public static final String MENU_H5_WORKBENCH_VIEW = "menu:h5:workbench:view";
    /** H5端工单列表菜单权限 */
    public static final String MENU_H5_WORKORDER_LIST = "menu:h5:workorder:list";

    /** Web端获取当前用户信息接口权限 */
    public static final String API_AUTH_WEB_ME = "api:auth:web:me";
    /** H5端获取当前用户信息接口权限 */
    public static final String API_AUTH_H5_ME = "api:auth:h5:me";
    /** 创建事件接口权限 */
    public static final String API_EVENT_CREATE = "api:event:create";
    /** 查看事件详情接口权限 */
    public static final String API_EVENT_DETAIL = "api:event:detail";
    /** 查看事件列表接口权限 */
    public static final String API_EVENT_LIST = "api:event:list";
    /** 忽略事件接口权限 */
    public static final String API_EVENT_IGNORE = "api:event:ignore";
    /** 事件误报菜单权限 */
    public static final String MENU_EVENT_FALSE_ALARM = "menu:event:false-alarm";
    /** 发起审核接口权限 */
    public static final String API_AUDIT_START = "api:audit:start";
    /** 查看审核详情接口权限 */
    public static final String API_AUDIT_DETAIL = "api:audit:detail";
    /** 流程节点审批通过接口权限 */
    public static final String API_PROCESS_INSTANCE_APPROVE = "api:process-instance:approve";
    /** 流程节点审批拒绝接口权限 */
    public static final String API_PROCESS_INSTANCE_REJECT = "api:process-instance:reject";
    /** 创建流程模板接口权限 */
    public static final String API_PROCESS_TEMPLATE_CREATE = "api:process-template:create";
    /** 查看流程模板列表接口权限 */
    public static final String API_PROCESS_TEMPLATE_LIST = "api:process-template:list";
    /** 查看流程模板详情接口权限 */
    public static final String API_PROCESS_TEMPLATE_DETAIL = "api:process-template:detail";
    /** 更新流程模板接口权限 */
    public static final String API_PROCESS_TEMPLATE_UPDATE = "api:process-template:update";
    /** 删除流程模板接口权限 */
    public static final String API_PROCESS_TEMPLATE_DELETE = "api:process-template:delete";
    /** 派发工单接口权限 */
    public static final String API_WORKORDER_DISPATCH = "api:workorder:dispatch";
    /** 处理工单接口权限 */
    public static final String API_WORKORDER_HANDLE = "api:workorder:handle";
    /** 确认关闭工单接口权限 */
    public static final String API_WORKORDER_CONFIRM_CLOSE = "api:workorder:confirm-close";
    /** 信息互通查看权限 */
    public static final String API_INTEGRATION_VIEW = "api:integration:view";
    /** 信息互通管理权限 */
    public static final String API_INTEGRATION_MANAGE = "api:integration:manage";
    /** H5端工作台查看接口权限 */
    public static final String API_H5_WORKBENCH_VIEW = "api:h5:workbench:view";
    /** H5端工单列表接口权限 */
    public static final String API_H5_WORKORDER_LIST = "api:h5:workorder:list";
    /** H5端工单详情接口权限 */
    public static final String API_H5_WORKORDER_DETAIL = "api:h5:workorder:detail";
    /** H5端工单处理接口权限 */
    public static final String API_H5_WORKORDER_HANDLE = "api:h5:workorder:handle";

    /** 系统管理目录权限 */
    public static final String CATALOG_SYSTEM = "catalog:system";
    /** 系统用户管理菜单权限 */
    public static final String MENU_SYSTEM_USER = "menu:system:user";
    /** 系统角色管理菜单权限 */
    public static final String MENU_SYSTEM_ROLE = "menu:system:role";
    /** 系统菜单管理菜单权限 */
    public static final String MENU_SYSTEM_MENU = "menu:system:menu";
    /** 系统权限管理菜单权限 */
    public static final String MENU_SYSTEM_PERMISSION = "menu:system:permission";

    /** 查看系统用户列表接口权限 */
    public static final String API_SYSTEM_USER_LIST = "api:system:user:list";
    /** 查看系统用户详情接口权限 */
    public static final String API_SYSTEM_USER_DETAIL = "api:system:user:detail";
    /** 创建系统用户接口权限 */
    public static final String API_SYSTEM_USER_CREATE = "api:system:user:create";
    /** 更新系统用户接口权限 */
    public static final String API_SYSTEM_USER_UPDATE = "api:system:user:update";
    /** 修改系统用户状态接口权限 */
    public static final String API_SYSTEM_USER_STATUS = "api:system:user:status";
    /** 为系统用户分配角色接口权限 */
    public static final String API_SYSTEM_USER_ASSIGN_ROLES = "api:system:user:assign-roles";
    /** 删除系统用户接口权限 */
    public static final String API_SYSTEM_USER_DELETE = "api:system:user:delete";
    /** 修改系统用户密码接口权限 */
    public static final String API_SYSTEM_USER_CHANGE_PASSWORD = "api:system:user:change-password";
    /** 查看角色列表接口权限 */
    public static final String API_SYSTEM_ROLE_LIST = "api:system:role:list";
    /** 查看角色详情接口权限 */
    public static final String API_SYSTEM_ROLE_DETAIL = "api:system:role:detail";
    /** 创建角色接口权限 */
    public static final String API_SYSTEM_ROLE_CREATE = "api:system:role:create";
    /** 更新角色接口权限 */
    public static final String API_SYSTEM_ROLE_UPDATE = "api:system:role:update";
    /** 为角色分配权限接口权限 */
    public static final String API_SYSTEM_ROLE_ASSIGN_PERMISSIONS = "api:system:role:assign-permissions";
    /** 查看菜单列表接口权限 */
    public static final String API_SYSTEM_MENU_LIST = "api:system:menu:list";
    /** 创建菜单接口权限 */
    public static final String API_SYSTEM_MENU_CREATE = "api:system:menu:create";
    /** 更新菜单接口权限 */
    public static final String API_SYSTEM_MENU_UPDATE = "api:system:menu:update";
    /** 删除菜单接口权限 */
    public static final String API_SYSTEM_MENU_DELETE = "api:system:menu:delete";
    /** 查看权限列表接口权限 */
    public static final String API_SYSTEM_PERMISSION_LIST = "api:system:permission:list";

    /** 无人机目录权限 */
    public static final String CATALOG_DRONE = "catalog:drone";
    /** 无人机设备管理菜单权限 */
    public static final String MENU_DRONE_DEVICE = "menu:drone:device";
    /** 无人机任务管理菜单权限 */
    public static final String MENU_DRONE_JOB = "menu:drone:job";
    /** 无人机AI模型管理菜单权限 */
    public static final String MENU_DRONE_AI_MODEL = "menu:drone:ai-model";
    /** 无人机实时监控菜单权限 */
    public static final String MENU_DRONE_MONITOR = "menu:drone:monitor";

    /** 查看无人机工作空间列表接口权限 */
    public static final String API_DRONE_WORKSPACE_LIST = "api:drone:workspace:list";
    /** 查看无人机设备列表接口权限 */
    public static final String API_DRONE_DEVICE_LIST = "api:drone:device:list";
    /** 查看航线列表接口权限 */
    public static final String API_DRONE_WAYLINE_LIST = "api:drone:wayline:list";
    /** 查看航线坐标点接口权限 */
    public static final String API_DRONE_WAYLINE_POINTS = "api:drone:wayline:points";
    /** 查看无人机任务列表接口权限 */
    public static final String API_DRONE_JOB_LIST = "api:drone:job:list";
    /** 创建无人机任务接口权限 */
    public static final String API_DRONE_JOB_CREATE = "api:drone:job:create";
    /** 暂停/恢复无人机任务接口权限 */
    public static final String API_DRONE_JOB_PAUSE_RESUME = "api:drone:job:pause-resume";
    /** 无人机返航接口权限 */
    public static final String API_DRONE_JOB_RETURN_HOME = "api:drone:job:return-home";
    /** 查看无人机AI模型列表接口权限 */
    public static final String API_DRONE_AI_MODEL_LIST = "api:drone:ai-model:list";
    /** 绑定航线千问模型接口权限 */
    public static final String API_DRONE_WAYLINE_BIND_QWEN = "api:drone:wayline:bindQwen";
    /** 查看无人机喇叭文件列表接口权限 */
    public static final String API_DRONE_SPEAKER_FILE_LIST = "api:drone:speaker:file:list";
    /** 上传无人机喇叭文件接口权限 */
    public static final String API_DRONE_SPEAKER_FILE_UPLOAD = "api:drone:speaker:file:upload";
    /** 删除无人机喇叭文件接口权限 */
    public static final String API_DRONE_SPEAKER_FILE_DELETE = "api:drone:speaker:file:delete";
    /** 播放无人机喇叭接口权限 */
    public static final String API_DRONE_SPEAKER_PLAY = "api:drone:speaker:play";
    /** 停止无人机喇叭接口权限 */
    public static final String API_DRONE_SPEAKER_STOP = "api:drone:speaker:stop";
    /** 调节无人机喇叭音量接口权限 */
    public static final String API_DRONE_SPEAKER_VOLUME = "api:drone:speaker:volume";
    /** 设置无人机相机模式接口权限 */
    public static final String API_DRONE_PAYLOAD_CAMERA_MODE = "api:drone:payload:camera-mode";
    /** 开始无人机录制接口权限 */
    public static final String API_DRONE_PAYLOAD_RECORD_START = "api:drone:payload:record-start";
    /** 停止无人机录制接口权限 */
    public static final String API_DRONE_PAYLOAD_RECORD_STOP = "api:drone:payload:record-stop";
    /** 无人机 WebSocket 连接接口权限 */
    public static final String API_DRONE_WS_CONNECT = "api:drone:ws:connect";
    /** 查看无人机媒体列表接口权限 */
    public static final String API_DRONE_MEDIA_LIST = "api:drone:media:list";
    /** 查看无人机媒体文件接口权限 */
    public static final String API_DRONE_MEDIA_FILES = "api:drone:media:files";

    /** 网格治理目录权限 */
    public static final String CATALOG_COMMUNITY = "catalog:community";
    /** 网格治理-GIS网格可视化菜单权限 */
    public static final String MENU_COMMUNITY_GRID = "menu:community:grid";
    /** 网格治理-实有人口管理菜单权限 */
    public static final String MENU_COMMUNITY_POPULATION = "menu:community:population";
    /** 网格治理-房屋管理菜单权限 */
    public static final String MENU_COMMUNITY_BUILDING = "menu:community:building";
    /** 网格治理-场所管理菜单权限 */
    public static final String MENU_COMMUNITY_PLACE = "menu:community:place";
    /** 网格治理-组织力量管理菜单权限 */
    public static final String MENU_COMMUNITY_ORG_MEMBER = "menu:community:org-member";
    /** 网格治理-BI态势看板菜单权限 */
    public static final String MENU_COMMUNITY_DASHBOARD = "menu:community:dashboard";
    /** 综合监管大屏菜单权限 */
    public static final String MENU_BIG_SCREEN_VIEW = "menu:big-screen:view";
    /** 网格治理-巡查记录管理菜单权限 */
    public static final String MENU_COMMUNITY_PATROL_RECORD = "menu:community:patrol-record";
    /** 网格治理-居民上报管理菜单权限 */
    public static final String MENU_COMMUNITY_RESIDENT_REPORT = "menu:community:resident-report";
    /** 业务管理-场所台账菜单权限 */
    public static final String MENU_BIZ_LEDGER = "menu:biz:ledger";

    /** 业务管理目录权限 */
    public static final String CATALOG_BIZ = "catalog:biz";
    /** 辖区管理菜单权限 */
    public static final String MENU_BIZ_AREA = "menu:biz:area";
    /** 商户管理菜单权限 */
    public static final String MENU_BIZ_MERCHANT = "menu:biz:merchant";
    /** 摊贩管理菜单权限 */
    public static final String MENU_BIZ_VENDOR = "menu:biz:vendor";
    /** 违规区域管理菜单权限 */
    public static final String MENU_BIZ_VIOLATION_AREA = "menu:biz:violation-area";

    /** 创建辖区按钮权限 */
    public static final String BUTTON_BIZ_AREA_CREATE = "button:biz:area:create";
    /** 更新辖区按钮权限 */
    public static final String BUTTON_BIZ_AREA_UPDATE = "button:biz:area:update";
    /** 删除辖区按钮权限 */
    public static final String BUTTON_BIZ_AREA_DELETE = "button:biz:area:delete";
    /** 创建商户按钮权限 */
    public static final String BUTTON_BIZ_MERCHANT_CREATE = "button:biz:merchant:create";
    /** 更新商户按钮权限 */
    public static final String BUTTON_BIZ_MERCHANT_UPDATE = "button:biz:merchant:update";
    /** 删除商户按钮权限 */
    public static final String BUTTON_BIZ_MERCHANT_DELETE = "button:biz:merchant:delete";
    /** 创建摊贩按钮权限 */
    public static final String BUTTON_BIZ_VENDOR_CREATE = "button:biz:vendor:create";
    /** 更新摊贩按钮权限 */
    public static final String BUTTON_BIZ_VENDOR_UPDATE = "button:biz:vendor:update";
    /** 删除摊贩按钮权限 */
    public static final String BUTTON_BIZ_VENDOR_DELETE = "button:biz:vendor:delete";
    /** 创建违规区域按钮权限 */
    public static final String BUTTON_BIZ_VIOLATION_AREA_CREATE = "button:biz:violation-area:create";
    /** 更新违规区域按钮权限 */
    public static final String BUTTON_BIZ_VIOLATION_AREA_UPDATE = "button:biz:violation-area:update";
    /** 删除违规区域按钮权限 */
    public static final String BUTTON_BIZ_VIOLATION_AREA_DELETE = "button:biz:violation-area:delete";

    /** 查看辖区列表接口权限 */
    public static final String API_BIZ_AREA_LIST = "api:biz:area:list";
    /** 查看辖区详情接口权限 */
    public static final String API_BIZ_AREA_DETAIL = "api:biz:area:detail";
    /** 创建辖区接口权限 */
    public static final String API_BIZ_AREA_CREATE = "api:biz:area:create";
    /** 更新辖区接口权限 */
    public static final String API_BIZ_AREA_UPDATE = "api:biz:area:update";
    /** 删除辖区接口权限 */
    public static final String API_BIZ_AREA_DELETE = "api:biz:area:delete";
    /** 查看商户列表接口权限 */
    public static final String API_BIZ_MERCHANT_LIST = "api:biz:merchant:list";
    /** 查看商户详情接口权限 */
    public static final String API_BIZ_MERCHANT_DETAIL = "api:biz:merchant:detail";
    /** 创建商户接口权限 */
    public static final String API_BIZ_MERCHANT_CREATE = "api:biz:merchant:create";
    /** 更新商户接口权限 */
    public static final String API_BIZ_MERCHANT_UPDATE = "api:biz:merchant:update";
    /** 删除商户接口权限 */
    public static final String API_BIZ_MERCHANT_DELETE = "api:biz:merchant:delete";
    /** 查看摊贩列表接口权限 */
    public static final String API_BIZ_VENDOR_LIST = "api:biz:vendor:list";
    /** 查看摊贩详情接口权限 */
    public static final String API_BIZ_VENDOR_DETAIL = "api:biz:vendor:detail";
    /** 创建摊贩接口权限 */
    public static final String API_BIZ_VENDOR_CREATE = "api:biz:vendor:create";
    /** 更新摊贩接口权限 */
    public static final String API_BIZ_VENDOR_UPDATE = "api:biz:vendor:update";
    /** 删除摊贩接口权限 */
    public static final String API_BIZ_VENDOR_DELETE = "api:biz:vendor:delete";

    /** 查看违规区域列表接口权限 */
    public static final String API_BIZ_VIOLATION_AREA_LIST = "api:biz:violation-area:list";
    /** 查看违规区域详情接口权限 */
    public static final String API_BIZ_VIOLATION_AREA_DETAIL = "api:biz:violation-area:detail";
    /** 创建违规区域接口权限 */
    public static final String API_BIZ_VIOLATION_AREA_CREATE = "api:biz:violation-area:create";
    /** 更新违规区域接口权限 */
    public static final String API_BIZ_VIOLATION_AREA_UPDATE = "api:biz:violation-area:update";
    /** 删除违规区域接口权限 */
    public static final String API_BIZ_VIOLATION_AREA_DELETE = "api:biz:violation-area:delete";

    // H5 商户管理
    /** H5端商户查看菜单权限 */
    public static final String MENU_H5_MERCHANT_VIEW = "menu:h5:merchant:view";
    /** H5端摊贩查看菜单权限 */
    public static final String MENU_H5_VENDOR_VIEW = "menu:h5:vendor:view";

    /** H5端创建商户按钮权限 */
    public static final String BUTTON_H5_MERCHANT_CREATE = "button:h5:merchant:create";
    /** H5端更新商户按钮权限 */
    public static final String BUTTON_H5_MERCHANT_UPDATE = "button:h5:merchant:update";
    /** H5端删除商户按钮权限 */
    public static final String BUTTON_H5_MERCHANT_DELETE = "button:h5:merchant:delete";
    /** H5端创建摊贩按钮权限 */
    public static final String BUTTON_H5_VENDOR_CREATE = "button:h5:vendor:create";
    /** H5端更新摊贩按钮权限 */
    public static final String BUTTON_H5_VENDOR_UPDATE = "button:h5:vendor:update";
    /** H5端删除摊贩按钮权限 */
    public static final String BUTTON_H5_VENDOR_DELETE = "button:h5:vendor:delete";

    /** H5端查看商户列表接口权限 */
    public static final String API_H5_MERCHANT_LIST = "api:h5:merchant:list";
    /** H5端查看商户详情接口权限 */
    public static final String API_H5_MERCHANT_DETAIL = "api:h5:merchant:detail";
    /** H5端创建商户接口权限 */
    public static final String API_H5_MERCHANT_CREATE = "api:h5:merchant:create";
    /** H5端更新商户接口权限 */
    public static final String API_H5_MERCHANT_UPDATE = "api:h5:merchant:update";
    /** H5端删除商户接口权限 */
    public static final String API_H5_MERCHANT_DELETE = "api:h5:merchant:delete";

    // H5 摊贩管理
    /** H5端查看摊贩列表接口权限 */
    public static final String API_H5_VENDOR_LIST = "api:h5:vendor:list";
    /** H5端查看摊贩详情接口权限 */
    public static final String API_H5_VENDOR_DETAIL = "api:h5:vendor:detail";
    /** H5端创建摊贩接口权限 */
    public static final String API_H5_VENDOR_CREATE = "api:h5:vendor:create";
    /** H5端更新摊贩接口权限 */
    public static final String API_H5_VENDOR_UPDATE = "api:h5:vendor:update";
    /** H5端删除摊贩接口权限 */
    public static final String API_H5_VENDOR_DELETE = "api:h5:vendor:delete";

    // 辖区报表
    /** 辖区报表菜单权限 */
    public static final String MENU_REPORT_DISTRICT = "menu:report:district";
    /** 辖区报表接口权限 */
    public static final String API_REPORT_DISTRICT = "api:report:district";

    // 千问算法模型
    /** 千问模型管理菜单权限 */
    public static final String MENU_QWEN_MODEL = "menu:qwen:model";
    /** 查看千问模型列表接口权限 */
    public static final String API_QWEN_MODEL_LIST = "api:qwen-model:list";
    /** 创建千问模型接口权限 */
    public static final String API_QWEN_MODEL_CREATE = "api:qwen-model:create";
    /** 更新千问模型接口权限 */
    public static final String API_QWEN_MODEL_UPDATE = "api:qwen-model:update";
    /** 删除千问模型接口权限 */
    public static final String API_QWEN_MODEL_DELETE = "api:qwen-model:delete";

    /**
     * @Author tangxinglin
     * @Description //私有构造器，防止工具类被实例化
     * @Date 2026/04/18 09:55
     * @Param []
     * @return void
     */
    private PermissionCodes() {
    }
}
