package com.changping.platform.modules.drone.service;

import com.changping.platform.common.geo.PointInPolygonUtil;
import com.changping.platform.modules.drone.entity.WaylineAlgoViolationAreaEntity;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //航线算法绑定 ↔ 违章区域 关联服务:负责绑定行的区域配置、按告警查询命中区域判定
 * @Date 2026/04/22 00:00
 */
@Service
public class WaylineAlgoViolationAreaService {

    private final JdbcTemplate jdbcTemplate;

    public WaylineAlgoViolationAreaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 全量替换某条绑定行(航线+算法+起止航点)的违章区域配置
     */
    @Transactional
    public void replaceBindingAreas(String flyLineId, String label, Integer startPointIndex, Integer endPointIndex, List<Long> violationAreaIds) {
        if (!StringUtils.hasText(flyLineId) || !StringUtils.hasText(label) || startPointIndex == null || endPointIndex == null) {
            return;
        }
        jdbcTemplate.update(
                "DELETE FROM biz_wayline_algo_violation_area WHERE fly_line_id = ? AND label = ? AND start_point_index = ? AND end_point_index = ?",
                flyLineId, label, startPointIndex, endPointIndex);
        if (violationAreaIds == null || violationAreaIds.isEmpty()) {
            return;
        }
        for (Long areaId : violationAreaIds) {
            if (areaId == null) continue;
            jdbcTemplate.update(
                    "INSERT INTO biz_wayline_algo_violation_area (fly_line_id, label, start_point_index, end_point_index, violation_area_id, created_at) "
                            + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                    flyLineId, label, startPointIndex, endPointIndex, areaId);
        }
    }

    /**
     * 删除某条航线下所有绑定的区域配置(绑定整体被清除时调用)
     */
    @Transactional
    public void clearByWayline(String flyLineId) {
        if (!StringUtils.hasText(flyLineId)) return;
        jdbcTemplate.update("DELETE FROM biz_wayline_algo_violation_area WHERE fly_line_id = ?", flyLineId);
    }

    /**
     * 查询某条绑定的所有违章区域 ID
     */
    public List<Long> findAreaIdsByBinding(String flyLineId, String label, Integer startPointIndex, Integer endPointIndex) {
        if (!StringUtils.hasText(flyLineId) || !StringUtils.hasText(label) || startPointIndex == null || endPointIndex == null) {
            return Collections.emptyList();
        }
        return jdbcTemplate.queryForList(
                "SELECT violation_area_id FROM biz_wayline_algo_violation_area WHERE fly_line_id = ? AND label = ? AND start_point_index = ? AND end_point_index = ?",
                Long.class, flyLineId, label, startPointIndex, endPointIndex);
    }

    /**
     * 批量查询多条绑定的区域配置,返回 (flyLineId+label+start+end) -> areaIds 映射,用于详情接口回填
     */
    public List<WaylineAlgoViolationAreaEntity> listByWayline(String flyLineId) {
        if (!StringUtils.hasText(flyLineId)) return Collections.emptyList();
        return jdbcTemplate.query(
                "SELECT id, fly_line_id, label, start_point_index, end_point_index, violation_area_id, created_at "
                        + "FROM biz_wayline_algo_violation_area WHERE fly_line_id = ?",
                (rs, rowNum) -> {
                    WaylineAlgoViolationAreaEntity entity = new WaylineAlgoViolationAreaEntity();
                    entity.setId(rs.getLong("id"));
                    entity.setFlyLineId(rs.getString("fly_line_id"));
                    entity.setLabel(rs.getString("label"));
                    entity.setStartPointIndex(rs.getInt("start_point_index"));
                    entity.setEndPointIndex(rs.getInt("end_point_index"));
                    entity.setViolationAreaId(rs.getLong("violation_area_id"));
                    if (rs.getTimestamp("created_at") != null) {
                        entity.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
                    }
                    return entity;
                },
                flyLineId);
    }

    /**
     * 告警入库前判断:仅当 (flyLineId + label) 在本地有区域配置,且告警坐标落在任一 ACTIVE 区域多边形内,才允许入库。
     *
     * 约定:
     * - flyLineId 或 label 为空 → 无法定位绑定 → 丢弃
     * - (flyLineId + label) 有绑定但没配置区域 → 放行(不过滤)
     * - (flyLineId + label) 有绑定且配置了区域 → 必须坐标在区域内才放行
     * - lng / lat 缺失 → 如果有区域配置则丢弃,没有区域配置则放行
     */
    public boolean allowAlarm(String flyLineId, String label, Double lng, Double lat) {
        if (!StringUtils.hasText(flyLineId) || !StringUtils.hasText(label)) {
            return false;
        }

        // 查询该绑定配置的违章区域
        List<Long> areaIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT violation_area_id FROM biz_wayline_algo_violation_area WHERE fly_line_id = ? AND label = ?",
                Long.class, flyLineId, label);

        // 如果没有配置任何区域,表示不过滤,直接放行
        if (areaIds.isEmpty()) {
            return true;
        }

        // 配置了区域但缺少坐标,无法判断,丢弃
        if (lng == null || lat == null) {
            return false;
        }

        // 查询所有配置区域的多边形
        List<String> roiJsons = jdbcTemplate.query(
                "SELECT roi_json FROM violation_area WHERE id IN ("
                        + String.join(",", Collections.nCopies(areaIds.size(), "?"))
                        + ") AND status = 'ACTIVE' AND roi_json IS NOT NULL",
                (rs, rowNum) -> rs.getString("roi_json"),
                areaIds.toArray());

        // 判断坐标是否在任一区域内
        for (String roiJson : roiJsons) {
            List<double[]> polygon = PointInPolygonUtil.parseRoiJson(roiJson);
            if (PointInPolygonUtil.contains(polygon, lng, lat)) {
                return true;
            }
        }

        // 配置了区域但坐标不在任一区域内,丢弃
        return false;
    }
}
