package com.changping.platform.modules.community.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class MediaUploadMapper {

    private final JdbcTemplate jdbcTemplate;

    public MediaUploadMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public Long insert(Map<String, Object> record) {
        String sql = "INSERT INTO biz_media_file (business_type, business_id, file_name, file_url, file_type, mime_type, status, uploader_user_id, uploader_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql,
                record.get("businessType"),
                record.get("businessId"),
                record.get("fileName"),
                record.get("fileUrl"),
                record.get("fileType"),
                record.get("mimeType"),
                record.get("status"),
                record.get("uploaderUserId"),
                record.get("uploaderName"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public List<Map<String, Object>> findByBusiness(String businessType, Long businessId) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, business_type, business_id, file_name, file_url, file_type, mime_type, status, created_at " +
            "FROM biz_media_file WHERE status = 'ACTIVE'");
        List<Object> params = new java.util.ArrayList<>();

        if (businessType != null) {
            sql.append(" AND business_type = ?");
            params.add(businessType);
        }
        if (businessId != null) {
            sql.append(" AND business_id = ?");
            params.add(businessId);
        }
        sql.append(" ORDER BY created_at DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }
}
