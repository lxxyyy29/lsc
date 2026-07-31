package com.changping.platform.modules.event.mapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EventRatingMapper {

    private final JdbcTemplate jdbcTemplate;

    public EventRatingMapper(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertRating(Long eventId, Long userId, String userName, Integer score, String content, String tags) {
        String sql = "INSERT INTO biz_event_rating (event_id, user_id, user_name, score, content, tags, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, eventId, userId, userName, score, content, tags);
    }

    public List<Map<String, Object>> findByEventId(Long eventId) {
        return jdbcTemplate.queryForList(
            "SELECT id, event_id, user_id, user_name, score, content, tags, created_at " +
            "FROM biz_event_rating WHERE event_id = ? ORDER BY created_at DESC", eventId);
    }

    public Map<String, Object> getRatingStats(Long eventId) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS total, ROUND(AVG(score), 1) AS avgScore, " +
                "SUM(CASE WHEN score >= 4 THEN 1 ELSE 0 END) AS goodCount, " +
                "SUM(CASE WHEN score <= 2 THEN 1 ELSE 0 END) AS badCount " +
                "FROM biz_event_rating WHERE event_id = ?", eventId);
        } catch (Exception e) {
            return Map.of("total", 0, "avgScore", 0, "goodCount", 0, "badCount", 0);
        }
    }

    public boolean hasRated(Long eventId, Long userId) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM biz_event_rating WHERE event_id = ? AND user_id = ?",
            Long.class, eventId, userId);
        return count != null && count > 0;
    }

    // 全局评分统计
    public Map<String, Object> getOverallStats() {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT COUNT(*) AS totalRatings, ROUND(AVG(score), 1) AS avgScore, " +
                "SUM(CASE WHEN score = 5 THEN 1 ELSE 0 END) AS fiveStar, " +
                "SUM(CASE WHEN score = 4 THEN 1 ELSE 0 END) AS fourStar, " +
                "SUM(CASE WHEN score = 3 THEN 1 ELSE 0 END) AS threeStar, " +
                "SUM(CASE WHEN score = 2 THEN 1 ELSE 0 END) AS twoStar, " +
                "SUM(CASE WHEN score = 1 THEN 1 ELSE 0 END) AS oneStar " +
                "FROM biz_event_rating");
        } catch (Exception e) {
            return Map.of("totalRatings", 0, "avgScore", 0);
        }
    }
}
