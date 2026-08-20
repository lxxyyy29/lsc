package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.OrgMemberEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class OrgMemberMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OrgMemberEntity> ROW_MAPPER = (rs, rowNum) -> {
        OrgMemberEntity e = new OrgMemberEntity();
        e.setId(rs.getLong("id"));
        long gridId = rs.getLong("grid_id");
        e.setGridId(rs.wasNull() ? null : gridId);
        long uid = rs.getLong("sys_user_id");
        e.setSysUserId(rs.wasNull() ? null : uid);
        e.setMemberType(rs.getString("member_type"));
        e.setName(rs.getString("name"));
        e.setPhone(rs.getString("phone"));
        e.setPosition(rs.getString("position"));
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setGridName(rs.getString("grid_name"));
        long leaderId = rs.getLong("leader_id");
        e.setLeaderId(rs.wasNull() ? null : leaderId);
        e.setLeaderName(rs.getString("leader_name"));
        e.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        e.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return e;
    };

    public OrgMemberMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<OrgMemberEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query(
                "SELECT m.*, g.grid_name AS grid_name, l.name AS leader_name FROM cmn_org_member m " +
                        "LEFT JOIN cmn_grid g ON g.id = m.grid_id " +
                        "LEFT JOIN cmn_org_member l ON l.id = m.leader_id " +
                        "WHERE m.grid_id = ? AND m.status = 'ACTIVE' ORDER BY m.id DESC",
                ROW_MAPPER,
                gridId);
    }
    /**
     * 查询指定用户关联的活跃网格 ID 列表（用于 H5 端“我的网格”定位）
     */
    public List<Long> findGridIdsByUserId(Long sysUserId) {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT grid_id FROM cmn_org_member " +
                        "WHERE sys_user_id = ? AND status = 'ACTIVE' AND grid_id IS NOT NULL",
                Long.class,
                sysUserId);
    }

    public List<OrgMemberEntity> findAllActive() {
        return jdbcTemplate.query(
                "SELECT m.*, g.grid_name AS grid_name, l.name AS leader_name FROM cmn_org_member m " +
                        "LEFT JOIN cmn_grid g ON g.id = m.grid_id " +
                        "LEFT JOIN cmn_org_member l ON l.id = m.leader_id " +
                        "WHERE m.status = 'ACTIVE' ORDER BY m.id DESC",
                ROW_MAPPER);
    }
    public OrgMemberEntity findById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT m.*, g.grid_name AS grid_name, l.name AS leader_name FROM cmn_org_member m " +
                        "LEFT JOIN cmn_grid g ON g.id = m.grid_id " +
                        "LEFT JOIN cmn_org_member l ON l.id = m.leader_id " +
                        "WHERE m.id = ?",
                ROW_MAPPER,
                id);
    }
    public Long insert(OrgMemberEntity e) {
        String sql = "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, position, status, remark, leader_id, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getSysUserId(), e.getMemberType(), e.getName(), e.getPhone(), e.getPosition(), e.getStatus(), e.getRemark(), e.getLeaderId());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    public int update(OrgMemberEntity e) {
        String sql = "UPDATE cmn_org_member SET grid_id=?, sys_user_id=?, member_type=?, name=?, phone=?, position=?, status=?, remark=?, leader_id=?, updated_at=NOW() WHERE id=?";
        return jdbcTemplate.update(sql, e.getGridId(), e.getSysUserId(), e.getMemberType(), e.getName(), e.getPhone(), e.getPosition(), e.getStatus(), e.getRemark(), e.getLeaderId(), e.getId());
    }
    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_org_member WHERE id = ?", id);
    }

    /**
     * 组长候选人：职务含「组长/网格长」或类型为社区领导的活跃成员
     */
    public List<OrgMemberEntity> findLeaderCandidates() {
        return jdbcTemplate.query(
                "SELECT m.*, g.grid_name AS grid_name, l.name AS leader_name FROM cmn_org_member m " +
                        "LEFT JOIN cmn_grid g ON g.id = m.grid_id " +
                        "LEFT JOIN cmn_org_member l ON l.id = m.leader_id " +
                        "WHERE m.status = 'ACTIVE' AND (m.position LIKE '%组长%' OR m.position LIKE '%网格长%' OR m.member_type = 'LEADER') ORDER BY m.id",
                ROW_MAPPER);
    }

    /**
     * 批量划分：将指定成员划入某组长名下（leaderId 为 null 表示取消划分）
     */
    public int assignLeader(List<Long> memberIds, Long leaderId) {
        if (memberIds == null || memberIds.isEmpty()) return 0;
        String placeholders = String.join(",", memberIds.stream().map(x -> "?").toList());
        Object[] params = new Object[memberIds.size() + 1];
        params[0] = leaderId;
        for (int i = 0; i < memberIds.size(); i++) params[i + 1] = memberIds.get(i);
        return jdbcTemplate.update(
                "UPDATE cmn_org_member SET leader_id = ?, updated_at = NOW() WHERE id IN (" + placeholders + ")",
                params);
    }
}
