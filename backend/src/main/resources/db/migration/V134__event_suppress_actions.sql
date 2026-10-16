-- V134: 补两列，让「不派单 / 不通知 / 昨夜被抑制过」都能精确落地
--
-- 1) suppress_actions —— 记录本次实际生效的抑制动作。
--    背景：V133 只记了 suppress_until（截止时间），无法区分规则当时勾选了哪些动作；
--    而三个动作是可独立勾选的（方案 §2.2）：只勾 HIDE 时，夜间事件仍应正常派单与通知。
--    派单（SmartDispatchService）与通知（NotificationService）入口按本列判定，
--    避免"只勾 HIDE 也被顺带禁掉派单/通知"。
--
-- 2) suppress_released_at —— 抑制解除时间。
--    背景：自动恢复任务原先会把 suppress_until/rule_id 清空，"昨夜抑制过哪些事件"就无迹可查。
--    改为留痕：恢复时只把 hidden 置 0 并记下 released_at，原始抑制事实保留，
--    既满足"白天提示昨夜抑制 N 条 + 列表可筛选复盘"，也不影响展示（查询按 suppress_until > NOW() 判定）。
--
-- 幂等：列存在则跳过。

SET @ddl := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_event ADD COLUMN suppress_actions VARCHAR(64) NULL COMMENT ''本次抑制生效的动作：HIDE/NO_DISPATCH/NO_NOTIFY 逗号分隔''',
  'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'suppress_actions');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl := (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE biz_event ADD COLUMN suppress_released_at DATETIME NULL COMMENT ''抑制解除时间：到点自动恢复或人工放出时写入''',
  'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'suppress_released_at');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 历史遗留：V133 之后已有 suppress_until 但未记动作的行，按「当时规则=全部动作」保守补写，
-- 保证这些已抑制事件的派单/通知拦截行为不变（不会因为本次升级而误放行）；
-- 已过期的视为已释放，补记 released_at，便于白天提示统计。
UPDATE biz_event
   SET suppress_actions = 'HIDE,NO_DISPATCH,NO_NOTIFY'
 WHERE suppress_until IS NOT NULL
   AND suppress_actions IS NULL;

UPDATE biz_event
   SET suppress_released_at = suppress_until
 WHERE suppress_until IS NOT NULL
   AND suppress_until <= NOW()
   AND suppress_released_at IS NULL
   AND COALESCE(suppress_manual, 0) = 0;

-- 统计索引：白天提示「近 N 小时自动恢复了多少条」
SET @ddl := (SELECT IF(COUNT(*) = 0,
  'CREATE INDEX idx_biz_event_suppress_released_at ON biz_event (suppress_released_at)',
  'SELECT 1')
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND INDEX_NAME = 'idx_biz_event_suppress_released_at');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
