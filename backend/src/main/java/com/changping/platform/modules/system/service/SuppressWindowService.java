package com.changping.platform.modules.system.service;

import com.changping.platform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 时段抑制（夜间静默）规则服务。
 *
 * 行为：落在可配置时段内上传的事件，按规则指定的动作（HIDE/NO_DISPATCH/NO_NOTIFY）被抑制；
 * 抑制只打标记（suppress_until=命中时段的结束时刻），到点后查询条件自然放行，
 * 因此**无需定时任务**即可自动恢复，数据不删除、可追溯。
 *
 * 判定维度：时段（支持跨天）、生效星期、来源系统/事件类型范围、豁免紧急等级。
 */
@Service
public class SuppressWindowService {

    private static final Logger log = LoggerFactory.getLogger(SuppressWindowService.class);
    private static final String TZ_KEY = "suppress.timezone";
    private static final String ENABLED_KEY = "suppress.enabled";

    private final JdbcTemplate jdbcTemplate;

    public SuppressWindowService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 命中结果：命中规则与抑制截止时间 */
    public record Hit(Long ruleId, String ruleName, Set<String> actions, LocalDateTime until) {
    }

    /** 规则行 */
    public record RuleRow(Long id, String name, String startTime, String endTime, String weekdays,
                          String action, String scopeSource, String scopeType, String exemptUrgency,
                          Integer enabled, String remark) {
    }

    // ==================== 规则 CRUD ====================

    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList(
                "SELECT id, name, TIME_FORMAT(start_time,'%H:%i') AS startTime, TIME_FORMAT(end_time,'%H:%i') AS endTime, "
                        + "weekdays, action, scope_source AS scopeSource, scope_type AS scopeType, "
                        + "exempt_urgency AS exemptUrgency, enabled, remark FROM sys_suppress_window ORDER BY id");
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String name = text(body, "name", "未命名规则");
        String start = normalizeTime(text(body, "startTime", ""));
        String end = normalizeTime(text(body, "endTime", ""));
        if (start.isEmpty() || end.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "开始时间与结束时间不能为空");
        }
        Set<String> actions = normalizeActions(text(body, "action", "HIDE"));
        if (actions.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "至少选择一个抑制动作");
        }
        jdbcTemplate.update(
                "INSERT INTO sys_suppress_window (name, start_time, end_time, weekdays, action, scope_source, scope_type, exempt_urgency, enabled, remark) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                name, start, end,
                defaultIfBlank(text(body, "weekdays", ""), "1,2,3,4,5,6,7"),
                String.join(",", actions),
                blankToNull(text(body, "scopeSource", "")),
                blankToNull(text(body, "scopeType", "")),
                defaultIfBlank(text(body, "exemptUrgency", ""), "RED"),
                intOr(body.get("enabled"), 1),
                blankToNull(text(body, "remark", "")));
        return list().isEmpty() ? Map.of() : list().get(list().size() - 1);
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        RuleRow existing = requireRule(id);
        Set<String> actions = normalizeActions(text(body, "action", existing.action()));
        if (actions.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", "至少选择一个抑制动作");
        }
        jdbcTemplate.update(
                "UPDATE sys_suppress_window SET name=?, start_time=?, end_time=?, weekdays=?, action=?, "
                        + "scope_source=?, scope_type=?, exempt_urgency=?, enabled=?, remark=? WHERE id=?",
                defaultIfBlank(text(body, "name", ""), existing.name()),
                normalizeTime(defaultIfBlank(text(body, "startTime", ""), existing.startTime())),
                normalizeTime(defaultIfBlank(text(body, "endTime", ""), existing.endTime())),
                defaultIfBlank(text(body, "weekdays", ""), existing.weekdays()),
                String.join(",", actions),
                blankToNull(text(body, "scopeSource", "")),
                blankToNull(text(body, "scopeType", "")),
                defaultIfBlank(text(body, "exemptUrgency", ""), existing.exemptUrgency()),
                intOr(body.get("enabled"), existing.enabled() == null ? 1 : existing.enabled()),
                blankToNull(text(body, "remark", "")),
                id);
        return jdbcTemplate.queryForMap("SELECT * FROM sys_suppress_window WHERE id=?", id);
    }

    public void delete(Long id) {
        requireRule(id);
        jdbcTemplate.update("DELETE FROM sys_suppress_window WHERE id=?", id);
    }

    public void setEnabled(Long id, boolean enabled) {
        requireRule(id);
        jdbcTemplate.update("UPDATE sys_suppress_window SET enabled=? WHERE id=?", enabled ? 1 : 0, id);
    }

    private RuleRow requireRule(Long id) {
        List<RuleRow> rows = jdbcTemplate.query(
                "SELECT id, name, TIME_FORMAT(start_time,'%H:%i') AS s, TIME_FORMAT(end_time,'%H:%i') AS e, "
                        + "weekdays, action, scope_source, scope_type, exempt_urgency, enabled, remark FROM sys_suppress_window WHERE id=?",
                (rs, i) -> new RuleRow(rs.getLong("id"), rs.getString("name"), rs.getString("s"), rs.getString("e"),
                        rs.getString("weekdays"), rs.getString("action"), rs.getString("scope_source"),
                        rs.getString("scope_type"), rs.getString("exempt_urgency"), rs.getInt("enabled"),
                        rs.getString("remark")),
                id);
        if (rows.isEmpty()) {
            throw new BusinessException("SUPPRESS_RULE_NOT_FOUND", "时段抑制规则不存在: " + id);
        }
        return rows.get(0);
    }

    // ==================== 命中判定 ====================

    /**
     * 试算：给定来源系统/事件类型/紧急等级/时间，返回命中的规则（用于后台"保存前预演"）。
     */
    public Map<String, Object> test(String sourceSystem, String eventType, String urgency, LocalDateTime at) {
        LocalDateTime moment = at == null ? LocalDateTime.now(zone()) : at;
        Optional<Hit> hit = evaluate(sourceSystem, eventType, urgency, moment);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("evaluatedAt", moment.toString());
        result.put("suppressed", hit.isPresent());
        hit.ifPresent(h -> {
            result.put("ruleId", h.ruleId());
            result.put("ruleName", h.ruleName());
            result.put("actions", h.actions());
            result.put("until", h.until().toString());
        });
        return result;
    }

    /**
     * 判定是否命中抑制。总开关关闭时永远不命中。
     *
     * @param sourceSystem 来源系统（可空）
     * @param eventType    事件类型（可空）
     * @param urgency      紧急等级（可空，命中豁免列表则不受抑制）
     * @param at           事件发生/入库时间
     */
    public Optional<Hit> evaluate(String sourceSystem, String eventType, String urgency, LocalDateTime at) {
        if (!isGloballyEnabled()) {
            return Optional.empty();
        }
        LocalDateTime moment = at == null ? LocalDateTime.now(zone()) : at;
        for (RuleRow rule : enabledRules()) {
            if (!matchWeekday(rule.weekdays(), moment)) {
                continue;
            }
            if (!matchScope(rule.scopeSource(), sourceSystem) || !matchScope(rule.scopeType(), eventType)) {
                continue;
            }
            if (inExemptUrgency(rule.exemptUrgency(), urgency)) {
                continue;
            }
            LocalDateTime until = windowEnd(rule.startTime(), rule.endTime(), moment);
            if (until == null) {
                continue;
            }
            Set<String> actions = normalizeActions(rule.action());
            log.info("[suppress] 命中规则 id={} name={} 事件时间={} 抑制至={} 动作={}",
                    rule.id(), rule.name(), moment, until, actions);
            return Optional.of(new Hit(rule.id(), rule.name(), actions, until));
        }
        return Optional.empty();
    }

    /** 供查询层使用的"可见性"SQL 片段（列表/大屏统一追加，避免各处口径不一致） */
    public static String visibleClause(String alias) {
        String prefix = (alias == null || alias.isBlank()) ? "" : alias + ".";
        return " AND (" + prefix + "suppress_manual = 1 OR " + prefix + "suppress_until IS NULL OR "
                + prefix + "suppress_until <= NOW()) ";
    }

    private List<RuleRow> enabledRules() {
        return jdbcTemplate.query(
                "SELECT id, name, TIME_FORMAT(start_time,'%H:%i') AS s, TIME_FORMAT(end_time,'%H:%i') AS e, "
                        + "weekdays, action, scope_source, scope_type, exempt_urgency, enabled, remark "
                        + "FROM sys_suppress_window WHERE enabled = 1 ORDER BY id",
                (rs, i) -> new RuleRow(rs.getLong("id"), rs.getString("name"), rs.getString("s"), rs.getString("e"),
                        rs.getString("weekdays"), rs.getString("action"), rs.getString("scope_source"),
                        rs.getString("scope_type"), rs.getString("exempt_urgency"), rs.getInt("enabled"),
                        rs.getString("remark")));
    }

    private boolean isGloballyEnabled() {
        try {
            List<String> values = jdbcTemplate.queryForList(
                    "SELECT config_value FROM sys_config WHERE config_key = ?", String.class, ENABLED_KEY);
            return values.isEmpty() || !"0".equals(values.get(0).trim());
        } catch (Exception e) {
            log.warn("[suppress] 读取总开关失败，按启用处理: {}", e.getMessage());
            return true;
        }
    }

    private ZoneId zone() {
        try {
            List<String> values = jdbcTemplate.queryForList(
                    "SELECT config_value FROM sys_config WHERE config_key = ?", String.class, TZ_KEY);
            if (!values.isEmpty()) {
                return ZoneId.of(values.get(0).trim());
            }
        } catch (Exception ignored) {
            // 回退默认时区
        }
        return ZoneId.of("Asia/Shanghai");
    }

    /** 星期匹配：weekdays "1,2,3" 对应周一..周日 */
    private boolean matchWeekday(String weekdays, LocalDateTime moment) {
        if (weekdays == null || weekdays.isBlank()) {
            return true;
        }
        int iso = moment.getDayOfWeek().getValue();
        return Arrays.stream(weekdays.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> s.equals(String.valueOf(iso)));
    }

    /** 范围匹配：规则未限定（空）= 命中；已限定 = 需在列表内 */
    private boolean matchScope(String scope, String value) {
        if (scope == null || scope.isBlank()) {
            return true;
        }
        if (value == null || value.isBlank()) {
            return false;
        }
        return Arrays.stream(scope.split(",")).map(String::trim).anyMatch(s -> s.equalsIgnoreCase(value.trim()));
    }

    private boolean inExemptUrgency(String exempt, String urgency) {
        if (exempt == null || exempt.isBlank() || urgency == null || urgency.isBlank()) {
            return false;
        }
        return Arrays.stream(exempt.split(",")).map(String::trim).anyMatch(s -> s.equalsIgnoreCase(urgency.trim()));
    }

    /**
     * 计算"抑制截止时间"：moment 落在 [start, end) 时返回该时段的结束时刻，否则返回 null。
     * 支持跨天（start > end 表示跨零点）。
     */
    private LocalDateTime windowEnd(String startText, String endText, LocalDateTime moment) {
        try {
            LocalTime start = parseTime(startText);
            LocalTime end = parseTime(endText);
            LocalTime now = moment.toLocalTime();
            if (start.equals(end)) {
                return null; // 起止相同视为未生效，避免全天抑制
            }
            if (start.isBefore(end)) {
                // 同日时段，如 12:00-14:00
                if (!now.isBefore(start) && now.isBefore(end)) {
                    return moment.toLocalDate().atTime(end);
                }
                return null;
            }
            // 跨天时段，如 22:00-07:00
            if (!now.isBefore(start)) {
                return moment.toLocalDate().plusDays(1).atTime(end);
            }
            if (now.isBefore(end)) {
                return moment.toLocalDate().atTime(end);
            }
            return null;
        } catch (Exception e) {
            log.warn("[suppress] 时段解析失败 start={} end={}", startText, endText);
            return null;
        }
    }

    private LocalTime parseTime(String text) {
        String v = normalizeTime(text);
        return LocalTime.parse(v.length() == 5 ? v : "00:00");
    }

    /** 统一成 HH:mm，兼容 9:00 / 09:00:00 等写法 */
    private String normalizeTime(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return "";
        }
        String[] parts = v.split(":");
        try {
            int h = Integer.parseInt(parts[0].trim());
            int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            if (h < 0 || h > 23 || m < 0 || m > 59) {
                throw new NumberFormatException("out of range");
            }
            return String.format("%02d:%02d", h, m);
        } catch (Exception e) {
            throw new BusinessException("VALIDATION_ERROR", "时间格式不正确（应为 HH:mm）: " + raw);
        }
    }

    private Set<String> normalizeActions(String raw) {
        Set<String> allowed = new LinkedHashSet<>(List.of("HIDE", "NO_DISPATCH", "NO_NOTIFY"));
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(raw.split(","))
                .map(s -> s.trim().toUpperCase())
                .filter(allowed::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String text(Map<String, Object> body, String key, String fallback) {
        Object v = body == null ? null : body.get(key);
        return v == null ? fallback : String.valueOf(v);
    }

    private String defaultIfBlank(String v, String fallback) {
        return (v == null || v.isBlank()) ? fallback : v.trim();
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private int intOr(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    /** 供上层复用：把 LocalTime 转成 HH:mm 文本 */
    public static String hhmm(Time time) {
        return time == null ? null : String.format("%02d:%02d", time.toLocalTime().getHour(), time.toLocalTime().getMinute());
    }

    /** 供上层复用：今天是否为工作日（周一至周五），保留给后续扩展（如仅工作日夜间） */
    public boolean isWeekday(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }
}
