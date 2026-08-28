-- 志愿积分流水防重复发放：同一用户、同一来源类型、同一来源仅允许一条加分流水。
-- 作为应用层"UPDATE 受影响行数闸门"之外的数据库级第二道防线，
-- 并发签到/网络重试场景下第二个事务的流水插入将失败并整体回滚。
ALTER TABLE sys_volunteer_points_log
    ADD UNIQUE KEY uk_user_source (user_id, source_type, source_id);
