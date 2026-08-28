package com.changping.platform.modules.biz.service;

import com.changping.platform.common.exception.BusinessException;
import com.changping.platform.common.response.PagedResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //业务管理服务，提供辖区、商户、摊贩和违规区域的完整 CRUD 能力，包含坐标点多边形归属判断和 ROI 区域缓存
 * @Date 2026/04/18 10:20
 */
@Service
public class BizManagementService {

    private static final Set<String> SUPPORTED_STATUS = Set.of("ACTIVE", "DISABLED");
    private static final Set<String> SUPPORTED_AREA_MATCH_MODE = Set.of("MANUAL", "AUTO");
    private static final Set<String> SUPPORTED_VIOLATION_AREA_TYPE = Set.of(
            "ILLEGAL_STALL", "ILLEGAL_ROAD_OCCUPATION", "ILLEGAL_ADVERTISING", "ILLEGAL_PARKING");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @Author lxy
     * @Description //构造函数注入 JDBC 模板和 JSON 序列化器
     * @Date 2026/04/18 10:20
     * @Param [jdbcTemplate JDBC 模板, objectMapper JSON 序列化器]
     * @return void
     */
    public BizManagementService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @Author lxy
     * @Description //查询全部辖区列表，按 ID 升序返回
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<AreaItem> 辖区列表
     */
    @Transactional(readOnly = true)
    public List<AreaItem> listAreas() {
        return jdbcTemplate.query(
                "SELECT id, area_name, principal_name, principal_phone, roi_json, remark, status, created_at, updated_at FROM biz_area ORDER BY id ASC",
                (rs, rowNum) -> new AreaItem(
                        rs.getLong("id"),
                        rs.getString("area_name"),
                        rs.getString("principal_name"),
                        rs.getString("principal_phone"),
                        rs.getString("roi_json"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))));
    }

    /**
     * @Author lxy
     * @Description //分页查询辖区列表，支持辖区名称关键字和状态过滤
     * @Date 2026/04/18 10:20
     * @Param [page 当前页码, pageSize 每页大小, keyword 辖区名称关键字（可为null）, status 状态过滤（可为null）]
     * @return PagedResult<AreaItem> 分页辖区结果
     */
    @Transactional(readOnly = true)
    public PagedResult<AreaItem> listAreasPaged(int page, int pageSize, String keyword, String status) {
        int p = PagedResult.safePage(page);
        int ps = PagedResult.safePageSize(pageSize);
        int offset = PagedResult.safeOffset(page, pageSize);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (StringUtils.hasText(keyword)) {
            where.append(" AND area_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        String countSql = "SELECT COUNT(*) FROM biz_area" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        params.add(ps);
        params.add(offset);
        String dataSql = "SELECT id, area_name, principal_name, principal_phone, roi_json, remark, status, created_at, updated_at FROM biz_area" + where + " ORDER BY id ASC LIMIT ? OFFSET ?";
        List<AreaItem> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> new AreaItem(
                rs.getLong("id"), rs.getString("area_name"), rs.getString("principal_name"),
                rs.getString("principal_phone"), rs.getString("roi_json"), rs.getString("remark"),
                rs.getString("status"), toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))), params.toArray());
        return PagedResult.of(items, total == null ? 0 : total, p, ps);
    }

    /**
     * @Author lxy
     * @Description //查询状态为 ACTIVE 的辖区选项列表（仅含ID和名称），用于前端下拉选择
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<AreaOptionItem> 辖区选项列表
     */
    @Transactional(readOnly = true)
    public List<AreaOptionItem> listAreaOptions() {
        return jdbcTemplate.query(
                "SELECT id, area_name FROM biz_area WHERE status = 'ACTIVE' ORDER BY id ASC",
                (rs, rowNum) -> new AreaOptionItem(rs.getLong("id"), rs.getString("area_name")));
    }

    /**
     * Dynamically resolve which area a coordinate falls into using ray-casting point-in-polygon.
     * Returns null if no active area contains the given point.
     * Area polygon data is cached for 30 seconds to avoid repeated DB queries in list views.
     */
    /**
     * @Author lxy
     * @Description //根据经纬度坐标通过射线法判断坐标落在哪个辖区多边形内，未命中时返回 null，辖区多边形数据有 30 秒本地缓存
     * @Date 2026/04/18 10:20
     * @Param [longitude 经度, latitude 纬度]
     * @return AreaOptionItem 命中的辖区选项，未命中时返回 null
     */
    public AreaOptionItem resolveAreaByCoordinates(java.math.BigDecimal longitude, java.math.BigDecimal latitude) {
        if (longitude == null || latitude == null) return null;
        double lng = longitude.doubleValue();
        double lat = latitude.doubleValue();

        List<CachedAreaPolygon> areas = getCachedAreaPolygons();
        for (CachedAreaPolygon area : areas) {
            if (pointInPolygon(lng, lat, area.polyLng, area.polyLat)) {
                return new AreaOptionItem(area.id, area.areaName);
            }
        }
        return null;
    }

    // ---- Area polygon cache (30s TTL) ----

    private record CachedAreaPolygon(long id, String areaName, double[] polyLng, double[] polyLat) {}

    private volatile List<CachedAreaPolygon> cachedPolygons = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30_000;

    /**
     * @Author lxy
     * @Description //获取有效辖区多边形缓存列表，超过 30 秒 TTL 时重新从数据库加载并解析 ROI JSON
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<CachedAreaPolygon> 辖区多边形缓存列表
     */
    private List<CachedAreaPolygon> getCachedAreaPolygons() {
        long now = System.currentTimeMillis();
        List<CachedAreaPolygon> cached = cachedPolygons;
        if (cached != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cached;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, area_name, roi_json FROM biz_area WHERE status = 'ACTIVE' AND roi_json IS NOT NULL");
        List<CachedAreaPolygon> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String roiJson = (String) row.get("roi_json");
            if (roiJson == null || roiJson.isBlank()) continue;
            try {
                List<RoiPoint> points = objectMapper.readValue(roiJson, new com.fasterxml.jackson.core.type.TypeReference<List<RoiPoint>>() {});
                if (points.size() < 3) continue;
                double[] polyLng = new double[points.size()];
                double[] polyLat = new double[points.size()];
                for (int i = 0; i < points.size(); i++) {
                    polyLng[i] = points.get(i).lng().doubleValue();
                    polyLat[i] = points.get(i).lat().doubleValue();
                }
                result.add(new CachedAreaPolygon(((Number) row.get("id")).longValue(), (String) row.get("area_name"), polyLng, polyLat));
            } catch (Exception ignored) {}
        }
        cachedPolygons = result;
        cacheTimestamp = now;
        return result;
    }

    /**
     * @Author lxy
     * @Description //射线法判断点 (x,y) 是否在多边形内部
     * @Date 2026/04/18 10:20
     * @Param [x 点的经度, y 点的纬度, polyX 多边形顶点经度数组, polyY 多边形顶点纬度数组]
     * @return boolean 是否在多边形内部
     */
    private boolean pointInPolygon(double x, double y, double[] polyX, double[] polyY) {
        int n = polyX.length;
        boolean inside = false;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            if ((polyY[i] > y) != (polyY[j] > y)
                    && x < (polyX[j] - polyX[i]) * (y - polyY[i]) / (polyY[j] - polyY[i]) + polyX[i]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * @Author lxy
     * @Description //根据辖区ID查询辖区详情，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [areaId 辖区ID]
     * @return AreaItem 辖区详情
     */
    @Transactional(readOnly = true)
    public AreaItem getArea(Long areaId) {
        return requireArea(areaId);
    }

    /**
     * @Author lxy
     * @Description //创建新辖区，校验请求参数并持久化到数据库，同时清除辖区多边形缓存
     * @Date 2026/04/18 10:20
     * @Param [request 创建辖区请求]
     * @return AreaItem 创建后的辖区详情
     */
    @Transactional
    public AreaItem createArea(CreateAreaRequest request) {
        validateAreaRequest(request, null);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        String roiJson = normalizeRoiJson(request.roiJson());
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_area (area_name, principal_name, principal_phone, roi_json, remark, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.areaName().trim());
            statement.setString(2, normalizeNullable(request.principalName()));
            statement.setString(3, normalizeNullable(request.principalPhone()));
            statement.setString(4, roiJson);
            statement.setString(5, normalizeNullable(request.remark()));
            statement.setString(6, normalizeStatus(request.status()));
            return statement;
        }, keyHolder);
        cachedPolygons = null;
        return getArea(extractGeneratedId(keyHolder, "BIZ_AREA_CREATE_FAILED", "Failed to create area"));
    }

    /**
     * @Author lxy
     * @Description //更新指定辖区信息，校验辖区存在性和请求参数，更新后清除多边形缓存
     * @Date 2026/04/18 10:20
     * @Param [areaId 辖区ID, request 更新辖区请求]
     * @return AreaItem 更新后的辖区详情
     */
    @Transactional
    public AreaItem updateArea(Long areaId, UpdateAreaRequest request) {
        requireArea(areaId);
        validateAreaRequest(request, areaId);
        jdbcTemplate.update(
                "UPDATE biz_area SET area_name = ?, principal_name = ?, principal_phone = ?, roi_json = ?, remark = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.areaName().trim(),
                normalizeNullable(request.principalName()),
                normalizeNullable(request.principalPhone()),
                normalizeRoiJson(request.roiJson()),
                normalizeNullable(request.remark()),
                normalizeStatus(request.status()),
                areaId);
        cachedPolygons = null;
        return getArea(areaId);
    }

    /**
     * @Author lxy
     * @Description //删除指定辖区，若辖区下存在商户则拒绝删除，删除后清除多边形缓存
     * @Date 2026/04/18 10:20
     * @Param [areaId 辖区ID]
     * @return void
     */
    @Transactional
    public void deleteArea(Long areaId) {
        requireArea(areaId);
        Integer merchantCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_merchant WHERE area_id = ?", Integer.class, areaId);
        if (merchantCount != null && merchantCount > 0) {
            throw new BusinessException("BIZ_AREA_MERCHANT_REFERENCED", "区域被商户引用，无法删除");
        }
        jdbcTemplate.update("DELETE FROM biz_area WHERE id = ?", areaId);
        cachedPolygons = null;
    }

    /**
     * @Author lxy
     * @Description //查询全部商户列表，关联辖区名称，按 ID 升序返回
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<MerchantItem> 商户列表
     */
    @Transactional(readOnly = true)
    public List<MerchantItem> listMerchants() {
        return jdbcTemplate.query(
                "SELECT m.id, m.merchant_name, m.merchant_photo_url, m.longitude, m.latitude, m.legal_person_name, m.legal_person_photo_url, "
                        + "m.legal_person_phone, m.area_id, a.area_name, m.area_match_mode, m.remark, m.status, m.created_at, m.updated_at "
                        + "FROM biz_merchant m LEFT JOIN biz_area a ON a.id = m.area_id ORDER BY m.id ASC",
                (rs, rowNum) -> new MerchantItem(
                        rs.getLong("id"),
                        rs.getString("merchant_name"),
                        rs.getString("merchant_photo_url"),
                        rs.getBigDecimal("longitude"),
                        rs.getBigDecimal("latitude"),
                        rs.getString("legal_person_name"),
                        rs.getString("legal_person_photo_url"),
                        rs.getString("legal_person_phone"),
                        rs.getObject("area_id") == null ? null : rs.getLong("area_id"),
                        rs.getString("area_name"),
                        rs.getString("area_match_mode"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))));
    }

    /**
     * @Author lxy
     * @Description //分页查询商户列表，支持名称关键字、辖区ID和状态过滤
     * @Date 2026/04/18 10:20
     * @Param [page 当前页码, pageSize 每页大小, keyword 商户名称关键字（可为null）, areaId 辖区ID（可为null）, status 状态（可为null）]
     * @return PagedResult<MerchantItem> 分页商户结果
     */
    @Transactional(readOnly = true)
    public PagedResult<MerchantItem> listMerchantsPaged(int page, int pageSize, String keyword, Long areaId, String status) {
        int p = PagedResult.safePage(page);
        int ps = PagedResult.safePageSize(pageSize);
        int offset = PagedResult.safeOffset(page, pageSize);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (StringUtils.hasText(keyword)) {
            where.append(" AND m.merchant_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (areaId != null) {
            where.append(" AND m.area_id = ?");
            params.add(areaId);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND m.status = ?");
            params.add(status.trim());
        }
        String countSql = "SELECT COUNT(*) FROM biz_merchant m" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        params.add(ps);
        params.add(offset);
        String dataSql = "SELECT m.id, m.merchant_name, m.merchant_photo_url, m.longitude, m.latitude, m.legal_person_name, m.legal_person_photo_url, "
                + "m.legal_person_phone, m.area_id, a.area_name, m.area_match_mode, m.remark, m.status, m.created_at, m.updated_at "
                + "FROM biz_merchant m LEFT JOIN biz_area a ON a.id = m.area_id" + where + " ORDER BY m.id ASC LIMIT ? OFFSET ?";
        List<MerchantItem> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> new MerchantItem(
                rs.getLong("id"), rs.getString("merchant_name"), rs.getString("merchant_photo_url"),
                rs.getBigDecimal("longitude"), rs.getBigDecimal("latitude"), rs.getString("legal_person_name"),
                rs.getString("legal_person_photo_url"), rs.getString("legal_person_phone"),
                rs.getObject("area_id") == null ? null : rs.getLong("area_id"), rs.getString("area_name"),
                rs.getString("area_match_mode"), rs.getString("remark"), rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("updated_at"))),
                params.toArray());
        return PagedResult.of(items, total == null ? 0 : total, p, ps);
    }

    /**
     * @Author lxy
     * @Description //根据商户ID查询商户详情（含辖区名称），不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [merchantId 商户ID]
     * @return MerchantItem 商户详情
     */
    @Transactional(readOnly = true)
    public MerchantItem getMerchant(Long merchantId) {
        List<MerchantItem> merchants = jdbcTemplate.query(
                "SELECT m.id, m.merchant_name, m.merchant_photo_url, m.longitude, m.latitude, m.legal_person_name, m.legal_person_photo_url, "
                        + "m.legal_person_phone, m.area_id, a.area_name, m.area_match_mode, m.remark, m.status, m.created_at, m.updated_at "
                        + "FROM biz_merchant m LEFT JOIN biz_area a ON a.id = m.area_id WHERE m.id = ?",
                (rs, rowNum) -> new MerchantItem(
                        rs.getLong("id"),
                        rs.getString("merchant_name"),
                        rs.getString("merchant_photo_url"),
                        rs.getBigDecimal("longitude"),
                        rs.getBigDecimal("latitude"),
                        rs.getString("legal_person_name"),
                        rs.getString("legal_person_photo_url"),
                        rs.getString("legal_person_phone"),
                        rs.getObject("area_id") == null ? null : rs.getLong("area_id"),
                        rs.getString("area_name"),
                        rs.getString("area_match_mode"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                merchantId);
        if (merchants.isEmpty()) {
            throw new BusinessException("BIZ_MERCHANT_NOT_FOUND", "商户未找到");
        }
        return merchants.get(0);
    }

    /**
     * @Author lxy
     * @Description //创建新商户，校验请求参数（含辖区状态校验）并持久化到数据库
     * @Date 2026/04/18 10:20
     * @Param [request 创建商户请求]
     * @return MerchantItem 创建后的商户详情
     */
    @Transactional
    public MerchantItem createMerchant(CreateMerchantRequest request) {
        validateMerchantRequest(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_merchant (merchant_name, merchant_photo_url, longitude, latitude, legal_person_name, legal_person_photo_url, legal_person_phone, area_id, area_match_mode, remark, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.merchantName().trim());
            statement.setString(2, normalizeUrl(request.merchantPhotoUrl(), "Merchant photo URL"));
            statement.setBigDecimal(3, request.longitude());
            statement.setBigDecimal(4, request.latitude());
            statement.setString(5, normalizeNullable(request.legalPersonName()));
            statement.setString(6, normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL"));
            statement.setString(7, normalizeNullable(request.legalPersonPhone()));
            statement.setObject(8, normalizeAreaBinding(request.areaId()));
            statement.setString(9, normalizeAreaMatchMode(request.areaMatchMode()));
            statement.setString(10, normalizeNullable(request.remark()));
            statement.setString(11, normalizeStatus(request.status()));
            return statement;
        }, keyHolder);
        return getMerchant(extractGeneratedId(keyHolder, "BIZ_MERCHANT_CREATE_FAILED", "Failed to create merchant"));
    }

    /**
     * @Author lxy
     * @Description //更新指定商户信息，校验商户存在性和请求参数后执行更新
     * @Date 2026/04/18 10:20
     * @Param [merchantId 商户ID, request 更新商户请求]
     * @return MerchantItem 更新后的商户详情
     */
    @Transactional
    public MerchantItem updateMerchant(Long merchantId, UpdateMerchantRequest request) {
        requireMerchantExists(merchantId);
        validateMerchantRequest(request);
        jdbcTemplate.update(
                "UPDATE biz_merchant SET merchant_name = ?, merchant_photo_url = ?, longitude = ?, latitude = ?, legal_person_name = ?, legal_person_photo_url = ?, legal_person_phone = ?, area_id = ?, area_match_mode = ?, remark = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.merchantName().trim(),
                normalizeUrl(request.merchantPhotoUrl(), "Merchant photo URL"),
                request.longitude(),
                request.latitude(),
                normalizeNullable(request.legalPersonName()),
                normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL"),
                normalizeNullable(request.legalPersonPhone()),
                normalizeAreaBinding(request.areaId()),
                normalizeAreaMatchMode(request.areaMatchMode()),
                normalizeNullable(request.remark()),
                normalizeStatus(request.status()),
                merchantId);
        return getMerchant(merchantId);
    }

    /**
     * @Author lxy
     * @Description //删除指定商户，校验商户存在性后执行删除
     * @Date 2026/04/18 10:20
     * @Param [merchantId 商户ID]
     * @return void
     */
    @Transactional
    public void deleteMerchant(Long merchantId) {
        requireMerchantExists(merchantId);
        jdbcTemplate.update("DELETE FROM biz_merchant WHERE id = ?", merchantId);
    }

    /**
     * @Author lxy
     * @Description //查询全部摊贩列表，按 ID 升序返回
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<VendorItem> 摊贩列表
     */
    @Transactional(readOnly = true)
    public List<VendorItem> listVendors() {
        return jdbcTemplate.query(
                "SELECT id, vendor_name, vendor_photo_url, legal_person_name, legal_person_photo_url, legal_person_phone, remark, status, created_at, updated_at FROM biz_mobile_vendor ORDER BY id ASC",
                (rs, rowNum) -> new VendorItem(
                        rs.getLong("id"),
                        rs.getString("vendor_name"),
                        rs.getString("vendor_photo_url"),
                        rs.getString("legal_person_name"),
                        rs.getString("legal_person_photo_url"),
                        rs.getString("legal_person_phone"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))));
    }

    /**
     * @Author lxy
     * @Description //分页查询摊贩列表，支持名称关键字和状态过滤
     * @Date 2026/04/18 10:20
     * @Param [page 当前页码, pageSize 每页大小, keyword 摊贩名称关键字（可为null）, status 状态（可为null）]
     * @return PagedResult<VendorItem> 分页摊贩结果
     */
    @Transactional(readOnly = true)
    public PagedResult<VendorItem> listVendorsPaged(int page, int pageSize, String keyword, String status) {
        int p = PagedResult.safePage(page);
        int ps = PagedResult.safePageSize(pageSize);
        int offset = PagedResult.safeOffset(page, pageSize);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (StringUtils.hasText(keyword)) {
            where.append(" AND vendor_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        String countSql = "SELECT COUNT(*) FROM biz_mobile_vendor" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        params.add(ps);
        params.add(offset);
        String dataSql = "SELECT id, vendor_name, vendor_photo_url, legal_person_name, legal_person_photo_url, legal_person_phone, remark, status, created_at, updated_at FROM biz_mobile_vendor" + where + " ORDER BY id ASC LIMIT ? OFFSET ?";
        List<VendorItem> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> new VendorItem(
                rs.getLong("id"), rs.getString("vendor_name"), rs.getString("vendor_photo_url"),
                rs.getString("legal_person_name"), rs.getString("legal_person_photo_url"),
                rs.getString("legal_person_phone"), rs.getString("remark"), rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("updated_at"))),
                params.toArray());
        return PagedResult.of(items, total == null ? 0 : total, p, ps);
    }

    /**
     * @Author lxy
     * @Description //根据摊贩ID查询摊贩详情，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [vendorId 摊贩ID]
     * @return VendorItem 摊贩详情
     */
    @Transactional(readOnly = true)
    public VendorItem getVendor(Long vendorId) {
        List<VendorItem> vendors = jdbcTemplate.query(
                "SELECT id, vendor_name, vendor_photo_url, legal_person_name, legal_person_photo_url, legal_person_phone, remark, status, created_at, updated_at FROM biz_mobile_vendor WHERE id = ?",
                (rs, rowNum) -> new VendorItem(
                        rs.getLong("id"),
                        rs.getString("vendor_name"),
                        rs.getString("vendor_photo_url"),
                        rs.getString("legal_person_name"),
                        rs.getString("legal_person_photo_url"),
                        rs.getString("legal_person_phone"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                vendorId);
        if (vendors.isEmpty()) {
            throw new BusinessException("BIZ_VENDOR_NOT_FOUND", "厂商未找到");
        }
        return vendors.get(0);
    }

    /**
     * @Author lxy
     * @Description //创建新摊贩，校验请求参数并持久化到数据库
     * @Date 2026/04/18 10:20
     * @Param [request 创建摊贩请求]
     * @return VendorItem 创建后的摊贩详情
     */
    @Transactional
    public VendorItem createVendor(CreateVendorRequest request) {
        validateVendorRequest(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO biz_mobile_vendor (vendor_name, vendor_photo_url, legal_person_name, legal_person_photo_url, legal_person_phone, remark, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.vendorName().trim());
            statement.setString(2, normalizeUrl(request.vendorPhotoUrl(), "Vendor photo URL"));
            statement.setString(3, normalizeNullable(request.legalPersonName()));
            statement.setString(4, normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL"));
            statement.setString(5, normalizeNullable(request.legalPersonPhone()));
            statement.setString(6, normalizeNullable(request.remark()));
            statement.setString(7, normalizeStatus(request.status()));
            return statement;
        }, keyHolder);
        return getVendor(extractGeneratedId(keyHolder, "BIZ_VENDOR_CREATE_FAILED", "Failed to create vendor"));
    }

    /**
     * @Author lxy
     * @Description //更新指定摊贩信息，校验摊贩存在性和请求参数后执行更新
     * @Date 2026/04/18 10:20
     * @Param [vendorId 摊贩ID, request 更新摊贩请求]
     * @return VendorItem 更新后的摊贩详情
     */
    @Transactional
    public VendorItem updateVendor(Long vendorId, UpdateVendorRequest request) {
        requireVendorExists(vendorId);
        validateVendorRequest(request);
        jdbcTemplate.update(
                "UPDATE biz_mobile_vendor SET vendor_name = ?, vendor_photo_url = ?, legal_person_name = ?, legal_person_photo_url = ?, legal_person_phone = ?, remark = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.vendorName().trim(),
                normalizeUrl(request.vendorPhotoUrl(), "Vendor photo URL"),
                normalizeNullable(request.legalPersonName()),
                normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL"),
                normalizeNullable(request.legalPersonPhone()),
                normalizeNullable(request.remark()),
                normalizeStatus(request.status()),
                vendorId);
        return getVendor(vendorId);
    }

    /**
     * @Author lxy
     * @Description //删除指定摊贩，校验摊贩存在性后执行删除
     * @Date 2026/04/18 10:20
     * @Param [vendorId 摊贩ID]
     * @return void
     */
    @Transactional
    public void deleteVendor(Long vendorId) {
        requireVendorExists(vendorId);
        jdbcTemplate.update("DELETE FROM biz_mobile_vendor WHERE id = ?", vendorId);
    }

    /**
     * @Author lxy
     * @Description //校验辖区请求参数，包括名称非空、ROI JSON 格式和状态合法性
     * @Date 2026/04/18 10:20
     * @Param [request 辖区请求对象, areaId 辖区ID（更新时传入，创建时为null）]
     * @return void
     */
    private void validateAreaRequest(AreaUpsertRequest request, Long areaId) {
        if (request == null) {
            throw new BusinessException("VALIDATION_ERROR", "区域请求不能为空");
        }
        if (!StringUtils.hasText(request.areaName())) {
            throw new BusinessException("VALIDATION_ERROR", "区域名称不能为空");
        }
        normalizeRoiJson(request.roiJson());
        normalizeStatus(request.status());
    }

    /**
     * @Author lxy
     * @Description //校验商户请求参数，包括名称非空、坐标对完整性、照片URL格式、区域匹配模式及绑定辖区的状态
     * @Date 2026/04/18 10:20
     * @Param [request 商户请求对象]
     * @return void
     */
    private void validateMerchantRequest(MerchantUpsertRequest request) {
        if (request == null) {
            throw new BusinessException("VALIDATION_ERROR", "商户请求不能为空");
        }
        if (!StringUtils.hasText(request.merchantName())) {
            throw new BusinessException("VALIDATION_ERROR", "商户名称不能为空");
        }
        validateCoordinatePair(request.longitude(), request.latitude());
        normalizeUrl(request.merchantPhotoUrl(), "Merchant photo URL");
        normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL");
        normalizeAreaMatchMode(request.areaMatchMode());
        normalizeStatus(request.status());
        if (request.areaId() != null) {
            AreaItem area = requireArea(request.areaId());
            if (!"ACTIVE".equalsIgnoreCase(area.status())) {
                throw new BusinessException("BIZ_AREA_INACTIVE_BIND_FORBIDDEN", "未启用的区域不能绑定商户");
            }
        }
    }

    /**
     * @Author lxy
     * @Description //校验摊贩请求参数，包括名称非空、照片URL格式和状态合法性
     * @Date 2026/04/18 10:20
     * @Param [request 摊贩请求对象]
     * @return void
     */
    private void validateVendorRequest(VendorUpsertRequest request) {
        if (request == null) {
            throw new BusinessException("VALIDATION_ERROR", "厂商请求不能为空");
        }
        if (!StringUtils.hasText(request.vendorName())) {
            throw new BusinessException("VALIDATION_ERROR", "厂商名称不能为空");
        }
        normalizeUrl(request.vendorPhotoUrl(), "Vendor photo URL");
        normalizeUrl(request.legalPersonPhotoUrl(), "Legal person photo URL");
        normalizeStatus(request.status());
    }

    /**
     * @Author lxy
     * @Description //对 ROI JSON 字符串进行解析和规范化，要求至少包含 3 个有效坐标点，返回标准化后的 JSON 字符串
     * @Date 2026/04/18 10:20
     * @Param [roiJson ROI JSON 字符串]
     * @return String 规范化后的 ROI JSON 字符串
     */
    private String normalizeRoiJson(String roiJson) {
        if (!StringUtils.hasText(roiJson)) {
            throw new BusinessException("VALIDATION_ERROR", "ROI 配置不能为空");
        }
        try {
            List<RoiPoint> points = objectMapper.readValue(roiJson, new TypeReference<List<RoiPoint>>() {});
            validateRoiPoints(points);
            return objectMapper.writeValueAsString(points);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("VALIDATION_ERROR", "ROI 配置必须是包含经纬度坐标的数组");
        }
    }

    /**
     * @Author lxy
     * @Description //校验 ROI 坐标点列表，要求至少 3 个且每个坐标点的经纬度均不为 null
     * @Date 2026/04/18 10:20
     * @Param [roiPoints ROI 坐标点列表]
     * @return void
     */
    private void validateRoiPoints(List<RoiPoint> roiPoints) {
        if (roiPoints == null || roiPoints.size() < 3) {
            throw new BusinessException("VALIDATION_ERROR", "ROI 至少需要 3 个坐标点");
        }
        for (RoiPoint point : roiPoints) {
            if (point == null || point.lng() == null || point.lat() == null) {
                throw new BusinessException("VALIDATION_ERROR", "ROI 坐标点必须包含经度和纬度");
            }
        }
    }

    /**
     * @Author lxy
     * @Description //校验经纬度坐标对必须同时提供或同时为 null，否则抛出校验异常
     * @Date 2026/04/18 10:20
     * @Param [longitude 经度, latitude 纬度]
     * @return void
     */
    private void validateCoordinatePair(BigDecimal longitude, BigDecimal latitude) {
        if ((longitude == null) != (latitude == null)) {
            throw new BusinessException("VALIDATION_ERROR", "经度和纬度必须同时提供");
        }
    }

    /**
     * @Author lxy
     * @Description //根据辖区ID从数据库查询辖区，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [areaId 辖区ID]
     * @return AreaItem 辖区详情
     */
    private AreaItem requireArea(Long areaId) {
        List<AreaItem> areas = jdbcTemplate.query(
                "SELECT id, area_name, principal_name, principal_phone, roi_json, remark, status, created_at, updated_at FROM biz_area WHERE id = ?",
                (rs, rowNum) -> new AreaItem(
                        rs.getLong("id"),
                        rs.getString("area_name"),
                        rs.getString("principal_name"),
                        rs.getString("principal_phone"),
                        rs.getString("roi_json"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                areaId);
        if (areas.isEmpty()) {
            throw new BusinessException("BIZ_AREA_NOT_FOUND", "区域未找到");
        }
        return areas.get(0);
    }

    /**
     * @Author lxy
     * @Description //校验商户存在性，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [merchantId 商户ID]
     * @return void
     */
    private void requireMerchantExists(Long merchantId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_merchant WHERE id = ?", Integer.class, merchantId);
        if (count == null || count == 0) {
            throw new BusinessException("BIZ_MERCHANT_NOT_FOUND", "商户未找到");
        }
    }

    /**
     * @Author lxy
     * @Description //校验摊贩存在性，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [vendorId 摊贩ID]
     * @return void
     */
    private void requireVendorExists(Long vendorId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_mobile_vendor WHERE id = ?", Integer.class, vendorId);
        if (count == null || count == 0) {
            throw new BusinessException("BIZ_VENDOR_NOT_FOUND", "厂商未找到");
        }
    }

    /**
     * @Author lxy
     * @Description //规范化状态字段，空值默认返回 ACTIVE，非法值抛出校验异常
     * @Date 2026/04/18 10:20
     * @Param [status 原始状态字符串]
     * @return String 规范化后的状态字符串
     */
    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!SUPPORTED_STATUS.contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的状态: " + status);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //规范化辖区匹配模式，空值默认返回 MANUAL，非法值抛出校验异常
     * @Date 2026/04/18 10:20
     * @Param [areaMatchMode 原始匹配模式字符串]
     * @return String 规范化后的匹配模式字符串
     */
    private String normalizeAreaMatchMode(String areaMatchMode) {
        if (!StringUtils.hasText(areaMatchMode)) {
            return "MANUAL";
        }
        String normalized = areaMatchMode.trim().toUpperCase();
        if (!SUPPORTED_AREA_MATCH_MODE.contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的区域匹配模式: " + areaMatchMode);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //规范化辖区绑定，null 时返回 null，非 null 时校验辖区存在并返回辖区ID
     * @Date 2026/04/18 10:20
     * @Param [areaId 辖区ID（可为null）]
     * @return Long 规范化后的辖区ID
     */
    private Long normalizeAreaBinding(Long areaId) {
        if (areaId == null) {
            return null;
        }
        return requireArea(areaId).id();
    }

    /**
     * @Author lxy
     * @Description //规范化可空字符串，有内容时去除首尾空白返回，否则返回 null
     * @Date 2026/04/18 10:20
     * @Param [value 原始字符串]
     * @return String 规范化后的字符串或 null
     */
    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * @Author lxy
     * @Description //规范化 URL 字符串，有内容时去除首尾空白返回，否则返回 null
     * @Date 2026/04/18 10:20
     * @Param [value 原始 URL 字符串, fieldName 字段名称（用于日志/错误提示）]
     * @return String 规范化后的 URL 或 null
     */
    private String normalizeUrl(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * @Author lxy
     * @Description //将 SQL Timestamp 转换为 LocalDateTime，null 时返回 null
     * @Date 2026/04/18 10:20
     * @Param [timestamp SQL 时间戳]
     * @return LocalDateTime 本地日期时间或 null
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * @Author lxy
     * @Description //从 KeyHolder 中提取数据库自增生成的主键ID，获取失败时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [keyHolder 主键持有者, errorCode 失败时的业务错误码, errorMessage 失败时的错误描述]
     * @return Long 生成的主键ID
     */
    private Long extractGeneratedId(KeyHolder keyHolder, String errorCode, String errorMessage) {
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null && keys.containsKey("ID")) {
            return ((Number) keys.get("ID")).longValue();
        }
        Number key = keyHolder.getKey();
        if (key != null) {
            return key.longValue();
        }
        throw new BusinessException(errorCode, errorMessage);
    }

    public interface AreaUpsertRequest {
        String areaName();
        String principalName();
        String principalPhone();
        String roiJson();
        String remark();
        String status();
    }

    public interface MerchantUpsertRequest {
        String merchantName();
        String merchantPhotoUrl();
        BigDecimal longitude();
        BigDecimal latitude();
        String legalPersonName();
        String legalPersonPhotoUrl();
        String legalPersonPhone();
        Long areaId();
        String areaMatchMode();
        String remark();
        String status();
    }

    public interface VendorUpsertRequest {
        String vendorName();
        String vendorPhotoUrl();
        String legalPersonName();
        String legalPersonPhotoUrl();
        String legalPersonPhone();
        String remark();
        String status();
    }

    public record CreateAreaRequest(
            String areaName,
            String principalName,
            String principalPhone,
            String roiJson,
            String remark,
            String status) implements AreaUpsertRequest {
    }

    public record UpdateAreaRequest(
            String areaName,
            String principalName,
            String principalPhone,
            String roiJson,
            String remark,
            String status) implements AreaUpsertRequest {
    }

    public record AreaItem(
            Long id,
            String areaName,
            String principalName,
            String principalPhone,
            String roiJson,
            String remark,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record AreaOptionItem(Long id, String areaName) {
    }

    public record CreateMerchantRequest(
            String merchantName,
            String merchantPhotoUrl,
            BigDecimal longitude,
            BigDecimal latitude,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            Long areaId,
            String areaMatchMode,
            String remark,
            String status) implements MerchantUpsertRequest {
    }

    public record UpdateMerchantRequest(
            String merchantName,
            String merchantPhotoUrl,
            BigDecimal longitude,
            BigDecimal latitude,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            Long areaId,
            String areaMatchMode,
            String remark,
            String status) implements MerchantUpsertRequest {
    }

    public record MerchantItem(
            Long id,
            String merchantName,
            String merchantPhotoUrl,
            BigDecimal longitude,
            BigDecimal latitude,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            Long areaId,
            String areaName,
            String areaMatchMode,
            String remark,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateVendorRequest(
            String vendorName,
            String vendorPhotoUrl,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            String remark,
            String status) implements VendorUpsertRequest {
    }

    public record UpdateVendorRequest(
            String vendorName,
            String vendorPhotoUrl,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            String remark,
            String status) implements VendorUpsertRequest {
    }

    public record VendorItem(
            Long id,
            String vendorName,
            String vendorPhotoUrl,
            String legalPersonName,
            String legalPersonPhotoUrl,
            String legalPersonPhone,
            String remark,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record RoiPoint(BigDecimal lng, BigDecimal lat) {
    }

    // ---- Violation Area ----

    public interface ViolationAreaUpsertRequest {
        String areaName();
        String areaType();
        String roiJson();
        String remark();
        String status();
    }

    public record CreateViolationAreaRequest(
            String areaName,
            String areaType,
            String roiJson,
            String remark,
            String status) implements ViolationAreaUpsertRequest {
    }

    public record UpdateViolationAreaRequest(
            String areaName,
            String areaType,
            String roiJson,
            String remark,
            String status) implements ViolationAreaUpsertRequest {
    }

    public record ViolationAreaItem(
            Long id,
            String areaName,
            String areaType,
            String roiJson,
            String remark,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    /**
     * @Author lxy
     * @Description //查询全部违规区域列表，按 ID 升序返回
     * @Date 2026/04/18 10:20
     * @Param []
     * @return List<ViolationAreaItem> 违规区域列表
     */
    @Transactional(readOnly = true)
    public List<ViolationAreaItem> listViolationAreas() {
        return jdbcTemplate.query(
                "SELECT id, area_name, area_type, roi_json, remark, status, created_at, updated_at FROM violation_area ORDER BY id ASC",
                (rs, rowNum) -> new ViolationAreaItem(
                        rs.getLong("id"),
                        rs.getString("area_name"),
                        rs.getString("area_type"),
                        rs.getString("roi_json"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))));
    }

    /**
     * @Author lxy
     * @Description //分页查询违规区域列表，支持名称关键字和状态过滤
     * @Date 2026/04/18 10:20
     * @Param [page 当前页码, pageSize 每页大小, keyword 区域名称关键字（可为null）, status 状态（可为null）]
     * @return PagedResult<ViolationAreaItem> 分页违规区域结果
     */
    @Transactional(readOnly = true)
    public PagedResult<ViolationAreaItem> listViolationAreasPaged(int page, int pageSize, String keyword, String status) {
        int p = PagedResult.safePage(page);
        int ps = PagedResult.safePageSize(pageSize);
        int offset = PagedResult.safeOffset(page, pageSize);
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (StringUtils.hasText(keyword)) {
            where.append(" AND area_name LIKE ?");
            params.add("%" + keyword.trim() + "%");
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        String countSql = "SELECT COUNT(*) FROM violation_area" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        params.add(ps);
        params.add(offset);
        String dataSql = "SELECT id, area_name, area_type, roi_json, remark, status, created_at, updated_at FROM violation_area" + where + " ORDER BY id ASC LIMIT ? OFFSET ?";
        List<ViolationAreaItem> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> new ViolationAreaItem(
                rs.getLong("id"), rs.getString("area_name"), rs.getString("area_type"),
                rs.getString("roi_json"), rs.getString("remark"), rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("updated_at"))),
                params.toArray());
        return PagedResult.of(items, total == null ? 0 : total, p, ps);
    }

    /**
     * @Author lxy
     * @Description //根据违规区域ID查询详情，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [id 违规区域ID]
     * @return ViolationAreaItem 违规区域详情
     */
    @Transactional(readOnly = true)
    public ViolationAreaItem getViolationArea(Long id) {
        return requireViolationArea(id);
    }

    /**
     * @Author lxy
     * @Description //创建新违规区域，校验请求参数并持久化到数据库
     * @Date 2026/04/18 10:20
     * @Param [request 创建违规区域请求]
     * @return ViolationAreaItem 创建后的违规区域详情
     */
    @Transactional
    public ViolationAreaItem createViolationArea(CreateViolationAreaRequest request) {
        validateViolationAreaRequest(request);
        String roiJson = normalizeRoiJson(request.roiJson());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO violation_area (area_name, area_type, roi_json, remark, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.areaName().trim());
            statement.setString(2, normalizeViolationAreaType(request.areaType()));
            statement.setString(3, roiJson);
            statement.setString(4, normalizeNullable(request.remark()));
            statement.setString(5, normalizeStatus(request.status()));
            return statement;
        }, keyHolder);
        return getViolationArea(extractGeneratedId(keyHolder, "VIOLATION_AREA_CREATE_FAILED", "Failed to create violation area"));
    }

    /**
     * @Author lxy
     * @Description //更新指定违规区域信息，校验区域存在性和请求参数后执行更新
     * @Date 2026/04/18 10:20
     * @Param [id 违规区域ID, request 更新违规区域请求]
     * @return ViolationAreaItem 更新后的违规区域详情
     */
    @Transactional
    public ViolationAreaItem updateViolationArea(Long id, UpdateViolationAreaRequest request) {
        requireViolationArea(id);
        validateViolationAreaRequest(request);
        jdbcTemplate.update(
                "UPDATE violation_area SET area_name = ?, area_type = ?, roi_json = ?, remark = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                request.areaName().trim(),
                normalizeViolationAreaType(request.areaType()),
                normalizeRoiJson(request.roiJson()),
                normalizeNullable(request.remark()),
                normalizeStatus(request.status()),
                id);
        return getViolationArea(id);
    }

    /**
     * @Author lxy
     * @Description //删除指定违规区域，校验区域存在性后执行删除
     * @Date 2026/04/18 10:20
     * @Param [id 违规区域ID]
     * @return void
     */
    @Transactional
    public void deleteViolationArea(Long id) {
        requireViolationArea(id);
        jdbcTemplate.update("DELETE FROM violation_area WHERE id = ?", id);
    }

    /**
     * @Author lxy
     * @Description //校验违规区域请求参数，包括名称非空、区域类型合法性和 ROI JSON 格式
     * @Date 2026/04/18 10:20
     * @Param [request 违规区域请求对象]
     * @return void
     */
    private void validateViolationAreaRequest(ViolationAreaUpsertRequest request) {
        if (request == null) {
            throw new BusinessException("VALIDATION_ERROR", "违规区域请求不能为空");
        }
        if (!StringUtils.hasText(request.areaName())) {
            throw new BusinessException("VALIDATION_ERROR", "区域名称不能为空");
        }
        normalizeViolationAreaType(request.areaType());
        normalizeRoiJson(request.roiJson());
        normalizeStatus(request.status());
    }

    /**
     * @Author lxy
     * @Description //规范化违规区域类型，空值返回 null，非法值抛出校验异常
     * @Date 2026/04/18 10:20
     * @Param [areaType 原始区域类型字符串]
     * @return String 规范化后的区域类型字符串
     */
    private String normalizeViolationAreaType(String areaType) {
        if (!StringUtils.hasText(areaType)) {
            return null;
        }
        String normalized = areaType.trim().toUpperCase();
        if (!SUPPORTED_VIOLATION_AREA_TYPE.contains(normalized)) {
            throw new BusinessException("VALIDATION_ERROR", "不支持的违规区域类型: " + areaType);
        }
        return normalized;
    }

    /**
     * @Author lxy
     * @Description //根据违规区域ID从数据库查询详情，不存在时抛出业务异常
     * @Date 2026/04/18 10:20
     * @Param [id 违规区域ID]
     * @return ViolationAreaItem 违规区域详情
     */
    private ViolationAreaItem requireViolationArea(Long id) {
        List<ViolationAreaItem> items = jdbcTemplate.query(
                "SELECT id, area_name, area_type, roi_json, remark, status, created_at, updated_at FROM violation_area WHERE id = ?",
                (rs, rowNum) -> new ViolationAreaItem(
                        rs.getLong("id"),
                        rs.getString("area_name"),
                        rs.getString("area_type"),
                        rs.getString("roi_json"),
                        rs.getString("remark"),
                        rs.getString("status"),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                id);
        if (items.isEmpty()) {
            throw new BusinessException("VIOLATION_AREA_NOT_FOUND", "违规区域未找到");
        }
        return items.get(0);
    }
}
