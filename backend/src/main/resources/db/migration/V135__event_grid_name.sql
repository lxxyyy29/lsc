-- 违停回调补充协议（2026-10-18）：新增「所属网格」文本字段
-- 上游板端在回调 JSON 中新增可选字段 gridName（人工填写的文本，去除首尾空白，最多 60 字），
-- 平台按文本原样保存并展示到事件基本信息的「所属网格」。
-- 注意：本次不涉及网格实体匹配/绑定（不写 grid_id），grid_id 仍由其他来源（人工创建/坐标解析）维护。
ALTER TABLE `biz_event`
    ADD COLUMN `grid_name` VARCHAR(60) NULL COMMENT '所属网格（上游回调 gridName 文本，未绑定网格实体）' AFTER `grid_id`;
