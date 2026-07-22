package com.changping.platform.modules.community.mapper;

import com.changping.platform.modules.community.entity.OrgMemberEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.util.List;

@Component
public class OrgMemberMapper {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OrgMemberEntity> ROW_MAPPER = (rs, rowNum) -> {
        OrgMemberEntity e = new OrgMemberEntity();
        e.setId(rs.getLong("id"));
        e.setGridId(rs.getLong("grid_id"));
        long uid = rs.getLong("sys_user_id");
        e.setSysUserId(rs.wasNull() ? null : uid);
        e.setMemberType(rs.getString("member_type"));
        e.setName(rs.getString("name"));
        e.setPhone(rs.getString("phone"));
        e.setStatus(rs.getString("status"));
        e.setRemark(rs.getString("remark"));
        e.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        e.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return e;
    };

    public OrgMemberMapper(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public List<OrgMemberEntity> findByGridId(Long gridId) {
        return jdbcTemplate.query("SELECT * FROM cmn_org_member WHERE grid_id = ? AND status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER, gridId);
    }
    public List<OrgMemberEntity> findAllActive() {
        return jdbcTemplate.query("SELECT * FROM cmn_org_member WHERE status = 'ACTIVE' ORDER BY id DESC", ROW_MAPPER);
    }
    public OrgMemberEntity findById(Long id) {
        return jdbcTemplate.queryForObject("SELECT * FROM cmn_org_member WHERE id = ?", ROW_MAPPER, id);
    }
    public Long insert(OrgMemberEntity e) {
        String sql = "INSERT INTO cmn_org_member (grid_id, sys_user_id, member_type, name, phone, status, remark, created_at, updated_at) VALUES (?,?,?,?,?,?,?,NOW(),NOW())";
        jdbcTemplate.update(sql, e.getGridId(), e.getSysUserId(), e.getMemberType(), e.getName(), e.getPhone(), e.getStatus(), e.getRemark());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    public int update(OrgMemberEntity e) {
        String sql = "UPDATE cmn_org_member SET grid_id=?, sys_user_id=?, member_type=?, name=?, phone=?, status=?, remark=?, updated_at=NOW() WHERE id=?";
        return jdbcTemplate.update(sql, e.getGridId(), e.getSysUserId(), e.getMemberType(), e.getName(), e.getPhone(), e.getStatus(), e.getRemark(), e.getId());
    }
    public int deleteById(Long id) {
        return jdbcTemplate.update("DELETE FROM cmn_org_member WHERE id = ?", id);
    }
}
