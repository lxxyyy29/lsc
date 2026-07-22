package com.changping.platform.modules.qwen;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author tangxinglin
 * @Description //千问算法模型服务，提供模型的分页查询、启用列表、详情、创建、更新及逻辑删除功能
 * @Date 2026/04/18 10:28
 */
@Service
public class QwenAlgorithmModelService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @Author tangxinglin
     * @Description //构造函数，注入数据库操作模板
     * @Date 2026/04/18 10:28
     * @Param [jdbcTemplate 数据库操作模板]
     * @return
     */
    public QwenAlgorithmModelService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SELECT_COLS =
            "id, name, label, interval_second, status, description, create_time, update_time";
    private static final String FROM_TABLE = " FROM qwen_algorithm_model WHERE deleted = 0";

    /**
     * @Author tangxinglin
     * @Description //分页查询千问算法模型列表（仅返回未删除的记录）
     * @Date 2026/04/18 10:28
     * @Param [page 页码, pageSize 每页条数]
     * @return PagedResult<ModelItem> 分页模型列表
     */
    @Transactional(readOnly = true)
    public PagedResult<ModelItem> listPaged(int page, int pageSize) {
        int p = PagedResult.safePage(page);
        int ps = PagedResult.safePageSize(pageSize);
        int offset = PagedResult.safeOffset(page, pageSize);
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + FROM_TABLE, Long.class);
        List<ModelItem> items = jdbcTemplate.query(
                "SELECT " + SELECT_COLS + FROM_TABLE + " ORDER BY id ASC LIMIT ? OFFSET ?",
                (rs, rowNum) -> mapRow(rs),
                ps, offset);
        return PagedResult.of(items, total == null ? 0 : total, p, ps);
    }

    /**
     * @Author tangxinglin
     * @Description //根据 ID 获取千问算法模型详情，不存在时抛出业务异常
     * @Date 2026/04/18 10:28
     * @Param [id 模型ID]
     * @return ModelItem 模型详情
     */
    @Transactional(readOnly = true)
    public ModelItem getById(Long id) {
        return requireModel(id);
    }

    /**
     * @Author tangxinglin
     * @Description //查询所有已启用（status=1）的千问算法模型列表
     * @Date 2026/04/18 10:28
     * @Param []
     * @return List<ModelItem> 已启用的模型列表
     */
    @Transactional(readOnly = true)
    public List<ModelItem> listEnabled() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLS + FROM_TABLE + " AND status = 1 ORDER BY id ASC",
                (rs, rowNum) -> mapRow(rs));
    }

    /**
     * @Author tangxinglin
     * @Description //创建新的千问算法模型，校验字段后写入数据库
     * @Date 2026/04/18 10:28
     * @Param [req 创建模型请求，包含名称、标签、采集间隔、状态和描述]
     * @return ModelItem 新建的模型详情
     */
    @Transactional
    public ModelItem create(CreateQwenModelRequest req) {
        validateRequest(req);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO qwen_algorithm_model (name, label, interval_second, status, description, create_time, update_time, deleted) "
                            + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, req.name().trim());
            statement.setString(2, req.label().trim());
            statement.setInt(3, req.intervalSecond() != null ? req.intervalSecond() : 10);
            statement.setInt(4, req.status() != null ? req.status() : 1);
            statement.setString(5, normalizeNullable(req.description()));
            return statement;
        }, keyHolder);
        return getById(extractGeneratedId(keyHolder));
    }

    /**
     * @Author tangxinglin
     * @Description //更新指定千问算法模型的信息
     * @Date 2026/04/18 10:28
     * @Param [id 模型ID, req 更新模型请求]
     * @return ModelItem 更新后的模型详情
     */
    @Transactional
    public ModelItem update(Long id, UpdateQwenModelRequest req) {
        requireModel(id);
        validateUpdateRequest(req);
        jdbcTemplate.update(
                "UPDATE qwen_algorithm_model SET name = ?, label = ?, interval_second = ?, status = ?, description = ?, update_time = CURRENT_TIMESTAMP WHERE id = ? AND deleted = 0",
                req.name().trim(),
                req.label().trim(),
                req.intervalSecond() != null ? req.intervalSecond() : 10,
                req.status() != null ? req.status() : 1,
                normalizeNullable(req.description()),
                id);
        return getById(id);
    }

    /**
     * @Author tangxinglin
     * @Description //逻辑删除指定千问算法模型，设置 deleted=1
     * @Date 2026/04/18 10:28
     * @Param [id 模型ID]
     * @return void
     */
    @Transactional
    public void delete(Long id) {
        requireModel(id);
        jdbcTemplate.update(
                "UPDATE qwen_algorithm_model SET deleted = 1, update_time = CURRENT_TIMESTAMP WHERE id = ?",
                id);
    }

    /**
     * @Author tangxinglin
     * @Description //根据 ID 查询模型记录，不存在时抛出业务异常
     * @Date 2026/04/18 10:28
     * @Param [id 模型ID]
     * @return ModelItem 模型详情
     */
    private ModelItem requireModel(Long id) {
        List<ModelItem> items = jdbcTemplate.query(
                "SELECT " + SELECT_COLS + FROM_TABLE + " AND id = ?",
                (rs, rowNum) -> mapRow(rs),
                id);
        if (items.isEmpty()) {
            throw new BusinessException("QWEN_MODEL_NOT_FOUND", "千问算法模型未找到");
        }
        return items.get(0);
    }

    /**
     * @Author tangxinglin
     * @Description //将 ResultSet 行映射为 ModelItem 对象
     * @Date 2026/04/18 10:28
     * @Param [rs 数据库结果集]
     * @return ModelItem 映射后的模型对象
     */
    private ModelItem mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ModelItem(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("label"),
                rs.getInt("interval_second"),
                rs.getInt("status"),
                rs.getString("description"),
                toLocalDateTime(rs.getTimestamp("create_time")),
                toLocalDateTime(rs.getTimestamp("update_time")));
    }

    /**
     * @Author tangxinglin
     * @Description //校验创建模型请求的字段，名称和标签不能为空
     * @Date 2026/04/18 10:28
     * @Param [req 创建模型请求]
     * @return void
     */
    private void validateRequest(CreateQwenModelRequest req) {
        if (req == null) {
            throw new BusinessException("VALIDATION_ERROR", "请求不能为空");
        }
        if (!StringUtils.hasText(req.name())) {
            throw new BusinessException("VALIDATION_ERROR", "模型名称不能为空");
        }
        if (!StringUtils.hasText(req.label())) {
            throw new BusinessException("VALIDATION_ERROR", "模型标签不能为空");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //校验更新模型请求的字段，名称和标签不能为空
     * @Date 2026/04/18 10:28
     * @Param [req 更新模型请求]
     * @return void
     */
    private void validateUpdateRequest(UpdateQwenModelRequest req) {
        if (req == null) {
            throw new BusinessException("VALIDATION_ERROR", "请求不能为空");
        }
        if (!StringUtils.hasText(req.name())) {
            throw new BusinessException("VALIDATION_ERROR", "模型名称不能为空");
        }
        if (!StringUtils.hasText(req.label())) {
            throw new BusinessException("VALIDATION_ERROR", "模型标签不能为空");
        }
    }

    /**
     * @Author tangxinglin
     * @Description //规范化可空字符串，空白时返回null
     * @Date 2026/04/18 10:28
     * @Param [value 输入字符串]
     * @return String 规范化后的字符串或null
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @Author tangxinglin
     * @Description //将 Timestamp 转换为 LocalDateTime，为 null 时返回 null
     * @Date 2026/04/18 10:28
     * @Param [timestamp 数据库时间戳]
     * @return LocalDateTime 转换后的本地时间，可为null
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * @Author tangxinglin
     * @Description //从KeyHolder中提取自动生成的主键ID
     * @Date 2026/04/18 10:28
     * @Param [keyHolder 持有生成主键的KeyHolder]
     * @return Long 生成的主键ID
     */
    private Long extractGeneratedId(KeyHolder keyHolder) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("ID")) {
            return ((Number) keys.get("ID")).longValue();
        }
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new BusinessException("QWEN_MODEL_CREATE_FAILED", "创建千问算法模型失败");
    }

    public record CreateQwenModelRequest(
            String name,
            String label,
            Integer intervalSecond,
            Integer status,
            String description) {
    }

    public record UpdateQwenModelRequest(
            String name,
            String label,
            Integer intervalSecond,
            Integer status,
            String description) {
    }

    public record ModelItem(
            Long id,
            String name,
            String label,
            int intervalSecond,
            int status,
            String description,
            LocalDateTime createTime,
            LocalDateTime updateTime) {
    }
}
