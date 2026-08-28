-- 手机号唯一索引：支持手机号登录（每个账号绑定唯一手机号）
-- 注意：MySQL 唯一索引允许多个 NULL，存量空手机号不受影响；
-- 已删除（deleted=1）用户的手机号先置空，避免与活跃用户冲突。
-- 若存量活跃用户存在重复的非空手机号，本迁移会失败，需先人工去重。
UPDATE sys_user SET phone = NULL WHERE deleted = 1;
ALTER TABLE sys_user ADD UNIQUE KEY uk_sys_user_phone (phone);
