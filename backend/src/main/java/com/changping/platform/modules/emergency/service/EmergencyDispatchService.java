package com.changping.platform.modules.emergency.service;

import com.changping.platform.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 应急会商/一键多方联合调度（B1）— R06 大屏端应急调度
 * 一键发起 → 按级别（社区/大网格/小网格）确定接收人 → 生成回执 + 推送站内通知 → 接收人反馈闭环
 * 状态机：指令 DISPATCHED(已下达) → RESPONDING(有响应) → COMPLETED(指挥端完成)
 * 回执：PENDING(未接收) → RECEIVED(已接收) → RESPONDING(响应中)/COMPLETED(已完成)
 */
@Service
public class EmergencyDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyDispatchService.class);

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 类型字典：RAIN暴雨 / FIRE火灾 / MASS群体性事件 / OTHER其他 */
    private static final Map<String, String> TYPE_NAMES = Map.of(
            "RAIN", "暴雨防汛", "FIRE", "火灾救援", "MASS", "群体性事件", "OTHER", "其他突发");

    /** 级别字典：COMMUNITY社区 / GRID大网格 / SUB_GRID小网格 */
    private static final Map<String, String> LEVEL_NAMES = Map.of(
            "COMMUNITY", "社区级", "GRID", "大网格级", "SUB_GRID", "小网格级");

    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    public EmergencyDispatchService(JdbcTemplate jdbcTemplate, NotificationService notificationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationService = notificationService;
    }

    /** 发起应急调度：创建指令 + 按级别确定接收人 + 生成回执 + 推送站内通知 */
    @Transactional
    public Map<String, Object> create(String title, String type, String level, Long gridId,
                                      String content, Long eventId, String eventCode,
                                      String videoCameraIds, String meetingUrl,
                                      Long creatorUserId, String creatorName) {
        String dispatchNo = "EMD" + LocalDateTime.now().format(NO_FMT) + ThreadLocalRandom.current().nextInt(100, 999);
        String gridName = null;
        if (gridId != null) {
            gridName = jdbcTemplate.queryForObject("SELECT grid_name FROM cmn_grid WHERE id = ?",
                    String.class, gridId);
        }
        Long dispatchId = insertDispatch(dispatchNo, title, type, level, gridId, gridName,
                content, eventId, eventCode, videoCameraIds, meetingUrl, creatorUserId, creatorName);

        // 按级别确定接收人（网格成员中已关联账号的）
        List<Map<String, Object>> receivers = resolveReceivers(level, gridId);
        if (receivers.isEmpty()) {
            // 兜底：全部网格员角色用户，避免无接收人导致指令空转
            receivers = jdbcTemplate.queryForList(
                    "SELECT DISTINCT u.id AS user_id, u.real_name AS user_name FROM sys_user u " +
                    "JOIN sys_user_role ur ON ur.user_id = u.id " +
                    "JOIN sys_role r ON r.id = ur.role_id " +
                    "WHERE r.role_code = 'GRID_WORKER' AND u.status = 'ACTIVE'");
        }
        for (Map<String, Object> receiver : receivers) {
            Long uid = ((Number) receiver.get("user_id")).longValue();
            String uname = (String) receiver.get("user_name");
            jdbcTemplate.update(
                    "INSERT INTO biz_emergency_receipt (dispatch_id, user_id, user_name, status, updated_at) " +
                    "VALUES (?, ?, ?, 'PENDING', NOW()) " +
                    "ON DUPLICATE KEY UPDATE user_name = VALUES(user_name)",
                    dispatchId, uid, uname);
            notifyReceiver(uid, dispatchId, title, level, content);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", dispatchId);
        result.put("dispatchNo", dispatchNo);
        result.put("receiverCount", receivers.size());
        return result;
    }

    private Long insertDispatch(String dispatchNo, String title, String type, String level, Long gridId,
                                String gridName, String content, Long eventId, String eventCode,
                                String videoCameraIds, String meetingUrl, Long creatorUserId, String creatorName) {
        jdbcTemplate.update(
                "INSERT INTO biz_emergency_dispatch " +
                "(dispatch_no, title, type, level, grid_id, grid_name, content, event_id, event_code, " +
                "video_camera_ids, meeting_url, status, creator_user_id, creator_name, dispatch_time, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DISPATCHED', ?, ?, NOW(), NOW(), NOW())",
                dispatchNo, title, type, level, gridId, gridName, content, eventId, eventCode,
                videoCameraIds, meetingUrl, creatorUserId, creatorName);
        return jdbcTemplate.queryForObject("SELECT id FROM biz_emergency_dispatch WHERE dispatch_no = ?",
                Long.class, dispatchNo);
    }

    /** 按调度级别解析接收人：社区级=全部在册成员；大网格级=该网格及子网格成员；小网格级=该网格成员 */
    private List<Map<String, Object>> resolveReceivers(String level, Long gridId) {
        List<Map<String, Object>> receivers;
        if ("GRID".equals(level) && gridId != null) {
            receivers = jdbcTemplate.queryForList(
                    "SELECT DISTINCT m.sys_user_id AS user_id, u.real_name AS user_name " +
                    "FROM cmn_org_member m " +
                    "JOIN cmn_grid g ON g.id = m.grid_id " +
                    "JOIN sys_user u ON u.id = m.sys_user_id " +
                    "WHERE m.status = 'ACTIVE' AND m.sys_user_id IS NOT NULL " +
                    "AND u.status = 'ACTIVE' AND (g.id = ? OR g.parent_id = ?)",
                    gridId, gridId);
        } else if ("SUB_GRID".equals(level) && gridId != null) {
            receivers = jdbcTemplate.queryForList(
                    "SELECT DISTINCT m.sys_user_id AS user_id, u.real_name AS user_name " +
                    "FROM cmn_org_member m " +
                    "JOIN sys_user u ON u.id = m.sys_user_id " +
                    "WHERE m.status = 'ACTIVE' AND m.sys_user_id IS NOT NULL " +
                    "AND u.status = 'ACTIVE' AND m.grid_id = ?",
                    gridId);
        } else {
            receivers = jdbcTemplate.queryForList(
                    "SELECT DISTINCT m.sys_user_id AS user_id, u.real_name AS user_name " +
                    "FROM cmn_org_member m " +
                    "JOIN sys_user u ON u.id = m.sys_user_id " +
                    "WHERE m.status = 'ACTIVE' AND m.sys_user_id IS NOT NULL AND u.status = 'ACTIVE'");
        }
        return receivers;
    }

    /** 推送站内通知（H5/Web 消息页均可查看，点击跳转指令详情） */
    private void notifyReceiver(Long userId, Long dispatchId, String title, String level, String content) {
        try {
            String brief = content != null && content.length() > 100 ? content.substring(0, 100) + "…" : content;
            notificationService.createNotification(userId,
                    "【应急调度】" + title,
                    "级别：" + LEVEL_NAMES.getOrDefault(level, level) + "｜指令：" + brief,
                    "EMERGENCY", "URGENT", "EMERGENCY", dispatchId);
        } catch (Exception e) {
            log.error("[EmergencyDispatch] 通知接收人 {} 失败: {}", userId, e.getMessage());
        }
    }

    /** 指令分页列表 */
    public Map<String, Object> list(String status, String level, int page, int size) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        if (level != null && !level.isBlank()) {
            where.append(" AND level = ?");
            params.add(level.trim());
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_emergency_dispatch" + where, Long.class, params.toArray());
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add((page - 1) * size);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT d.*, (SELECT COUNT(*) FROM biz_emergency_receipt r WHERE r.dispatch_id = d.id) AS receiver_count, " +
                "(SELECT COUNT(*) FROM biz_emergency_receipt r WHERE r.dispatch_id = d.id AND r.status <> 'PENDING') AS responded_count " +
                "FROM biz_emergency_dispatch d" + where + " ORDER BY d.id DESC LIMIT ? OFFSET ?",
                pageParams.toArray());
        decorate(rows);
        Map<String, Object> result = new HashMap<>();
        result.put("total", total != null ? total : 0L);
        result.put("items", rows);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    /** 指令详情（含回执列表） */
    public Map<String, Object> detail(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM biz_emergency_dispatch WHERE id = ?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("指令不存在");
        }
        Map<String, Object> row = rows.get(0);
        decorate(List.of(row));
        List<Map<String, Object>> receipts = jdbcTemplate.queryForList(
                "SELECT * FROM biz_emergency_receipt WHERE dispatch_id = ? ORDER BY FIELD(status,'PENDING','RECEIVED','RESPONDING','COMPLETED'), updated_at DESC",
                id);
        row.put("receipts", receipts);
        return row;
    }

    /** 当前用户在某指令下的回执 */
    public Map<String, Object> myReceipt(Long dispatchId, Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM biz_emergency_receipt WHERE dispatch_id = ? AND user_id = ?", dispatchId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** H5：我收到的指令列表（含我的回执状态） */
    public List<Map<String, Object>> myDispatches(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT d.id, d.dispatch_no, d.title, d.type, d.level, d.grid_name, d.content, d.status, " +
                "d.creator_name, d.dispatch_time, d.completed_at, d.meeting_url, " +
                "r.status AS my_status, r.feedback AS my_feedback " +
                "FROM biz_emergency_receipt r " +
                "JOIN biz_emergency_dispatch d ON d.id = r.dispatch_id " +
                "WHERE r.user_id = ? ORDER BY d.id DESC", userId);
        decorate(rows);
        return rows;
    }

    /** H5：指令详情（查看即标记已接收） */
    @Transactional
    public Map<String, Object> detailForH5(Long dispatchId, Long userId) {
        Map<String, Object> row = detail(dispatchId);
        Map<String, Object> my = myReceipt(dispatchId, userId);
        row.put("myReceipt", my);
        if (my != null && "PENDING".equals(my.get("status"))) {
            jdbcTemplate.update(
                    "UPDATE biz_emergency_receipt SET status = 'RECEIVED', received_at = COALESCE(received_at, NOW()) " +
                    "WHERE dispatch_id = ? AND user_id = ? AND status = 'PENDING'",
                    dispatchId, userId);
            my.put("status", "RECEIVED");
        }
        return row;
    }

    /** 接收人反馈：更新回执状态 + 聚合指令状态为响应中 */
    @Transactional
    public Map<String, Object> feedback(Long dispatchId, Long userId, String status, String feedback) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM biz_emergency_receipt WHERE dispatch_id = ? AND user_id = ?", dispatchId, userId);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("您不是该指令的接收人");
        }
        if (!List.of("RECEIVED", "RESPONDING", "COMPLETED").contains(status)) {
            throw new IllegalArgumentException("无效的反馈状态");
        }
        String respondedExpr = ("RESPONDING".equals(status) || "COMPLETED".equals(status))
                ? "COALESCE(responded_at, NOW())" : "responded_at";
        String completedExpr = "COMPLETED".equals(status) ? "COALESCE(completed_at, NOW())" : "completed_at";
        jdbcTemplate.update(
                "UPDATE biz_emergency_receipt SET status = ?, feedback = ?, " +
                "received_at = COALESCE(received_at, NOW()), " +
                "responded_at = " + respondedExpr + ", " +
                "completed_at = " + completedExpr + " " +
                "WHERE dispatch_id = ? AND user_id = ?",
                status, feedback, dispatchId, userId);
        // 聚合：有任一响应即指令进入响应中
        jdbcTemplate.update(
                "UPDATE biz_emergency_dispatch SET status = 'RESPONDING', updated_at = NOW() " +
                "WHERE id = ? AND status = 'DISPATCHED' " +
                "AND EXISTS (SELECT 1 FROM biz_emergency_receipt r WHERE r.dispatch_id = ? AND r.status <> 'PENDING')",
                dispatchId, dispatchId);
        return myReceipt(dispatchId, userId);
    }

    /** 指挥端完成指令 */
    @Transactional
    public void complete(Long dispatchId) {
        int updated = jdbcTemplate.update(
                "UPDATE biz_emergency_dispatch SET status = 'COMPLETED', completed_at = NOW(), updated_at = NOW() " +
                "WHERE id = ? AND status <> 'COMPLETED'", dispatchId);
        if (updated == 0) {
            throw new IllegalArgumentException("指令不存在或已完成");
        }
    }

    /** 字典：类型/级别 */
    public Map<String, Object> meta() {
        Map<String, Object> result = new HashMap<>();
        result.put("types", TYPE_NAMES);
        result.put("levels", LEVEL_NAMES);
        return result;
    }

    /** 附加展示字段 */
    private void decorate(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            row.put("type_name", TYPE_NAMES.getOrDefault((String) row.get("type"), (String) row.get("type")));
            row.put("level_name", LEVEL_NAMES.getOrDefault((String) row.get("level"), (String) row.get("level")));
        }
    }
}
