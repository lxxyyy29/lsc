package com.changping.platform.config;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * 修复失败的 V55 迁移并添加评价字段
 */
public class V56__fix_failed_migration extends BaseJavaMigration {

    private static final Logger log = LoggerFactory.getLogger(V56__fix_failed_migration.class);

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        // 1. 清理失败的 V55 记录
        Integer deleted = jdbc.update("DELETE FROM flyway_schema_history WHERE version = '55'");
        log.info("清理失败的 V55 迁移记录: {} 行", deleted);

        // 2. 添加评价字段（如果不存在）
        try {
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating'",
                Integer.class);
            Integer colCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating'",
                Integer.class);
            if (colCount != null && colCount == 0) {
                jdbc.update("ALTER TABLE biz_event ADD COLUMN rating INT DEFAULT NULL COMMENT '群众评价评分(1-5)'");
                log.info("添加 rating 字段成功");
            } else {
                log.info("rating 字段已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("检查/添加 rating 字段时出错: {}", e.getMessage());
        }

        try {
            Integer colCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rating_comment'",
                Integer.class);
            if (colCount != null && colCount == 0) {
                jdbc.update("ALTER TABLE biz_event ADD COLUMN rating_comment VARCHAR(500) DEFAULT NULL COMMENT '群众评价内容'");
                log.info("添加 rating_comment 字段成功");
            } else {
                log.info("rating_comment 字段已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("检查/添加 rating_comment 字段时出错: {}", e.getMessage());
        }

        try {
            Integer colCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_event' AND COLUMN_NAME = 'rated_at'",
                Integer.class);
            if (colCount != null && colCount == 0) {
                jdbc.update("ALTER TABLE biz_event ADD COLUMN rated_at DATETIME DEFAULT NULL COMMENT '评价时间'");
                log.info("添加 rated_at 字段成功");
            } else {
                log.info("rated_at 字段已存在，跳过");
            }
        } catch (Exception e) {
            log.warn("检查/添加 rated_at 字段时出错: {}", e.getMessage());
        }
    }

    @Override
    public Integer getChecksum() {
        return 56;
    }
}
