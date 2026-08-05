-- 事件表补充现场照片字段
-- 背景：群众随手拍上报（/events/public-report）支持上传现场照片，
-- 原 biz_event 表无照片存储字段，前端上传的照片无法随事件保存。
-- images 存储 JSON 数组格式的图片 URL 列表，如 ["http://.../a.png","http://.../b.png"]

ALTER TABLE biz_event
    ADD COLUMN images JSON NULL COMMENT '现场照片URL列表（JSON数组）' AFTER incident_address;
