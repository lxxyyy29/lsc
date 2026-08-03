-- 巡查任务 user_id 改为可空：生成本周任务时为网格级任务，尚未指派具体人员
-- 先删外键再改列，最后重建外键（MySQL 修改 FK 列需此顺序）
ALTER TABLE cmn_patrol_task DROP FOREIGN KEY fk_task_user;
ALTER TABLE cmn_patrol_task MODIFY COLUMN user_id bigint NULL COMMENT '指派人员ID（可为空，生成时未指派）';
ALTER TABLE cmn_patrol_task ADD CONSTRAINT fk_task_user FOREIGN KEY (user_id) REFERENCES sys_user (id);
