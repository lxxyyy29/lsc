package com.changping.platform.modules.event.controller;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.AuthenticatedUserContextHolder;
import com.changping.platform.modules.event.mapper.EventRatingMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/event-ratings")
public class EventRatingController {

    private final EventRatingMapper ratingMapper;

    public EventRatingController(EventRatingMapper ratingMapper) {
        this.ratingMapper = ratingMapper;
    }

    /**
     * 提交评价
     */
    @PostMapping
    public ApiResponse<Long> submit(@RequestBody Map<String, Object> request) {
        Long eventId = Long.parseLong(request.get("eventId").toString());
        Integer score = Integer.parseInt(request.get("score").toString());
        String content = (String) request.get("content");
        String tags = (String) request.get("tags");

        if (score < 1 || score > 5) {
            return ApiResponse.fail("INVALID_SCORE", "评分必须在 1-5 之间");
        }

        // 获取当前用户
        Long userId = AuthenticatedUserContextHolder.getOptional().map(u -> u.id()).orElse(null);
        String userName = AuthenticatedUserContextHolder.getOptional().map(u -> u.userName()).orElse("匿名");

        // 检查是否已评价
        if (userId != null && ratingMapper.hasRated(eventId, userId)) {
            return ApiResponse.fail("ALREADY_RATED", "您已评价过该事件");
        }

        ratingMapper.insertRating(eventId, userId, userName, score, content, tags);
        return ApiResponse.ok(eventId);
    }

    /**
     * 获取事件评价列表
     */
    @GetMapping("/event/{eventId}")
    public ApiResponse<List<Map<String, Object>>> list(@PathVariable Long eventId) {
        return ApiResponse.ok(ratingMapper.findByEventId(eventId));
    }

    /**
     * 获取事件评分统计
     */
    @GetMapping("/event/{eventId}/stats")
    public ApiResponse<Map<String, Object>> stats(@PathVariable Long eventId) {
        return ApiResponse.ok(ratingMapper.getRatingStats(eventId));
    }

    /**
     * 全局评分统计
     */
    @GetMapping("/overall-stats")
    public ApiResponse<Map<String, Object>> overallStats() {
        return ApiResponse.ok(ratingMapper.getOverallStats());
    }
}
