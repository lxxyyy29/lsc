package com.changping.platform.common.geo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @Author lxy
 * @Description //几何工具,提供点在多边形内判断与 roi_json 解析
 * @Date 2026/04/22 00:00
 */
public final class PointInPolygonUtil {

    private static final Logger log = LoggerFactory.getLogger(PointInPolygonUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PointInPolygonUtil() {}

    /**
     * 射线法判断点 (lng, lat) 是否落在多边形内;点在边上视为在内部。
     */
    public static boolean contains(List<double[]> polygon, double lng, double lat) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];
            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 解析 roi_json 字符串为多边形顶点列表 [[lng, lat], ...],失败返回空列表。
     * 支持格式: [{"lng":x,"lat":y}, ...]
     */
    public static List<double[]> parseRoiJson(String roiJson) {
        List<double[]> polygon = new ArrayList<>();
        if (!StringUtils.hasText(roiJson)) {
            return polygon;
        }
        try {
            List<Map<String, Object>> points = MAPPER.readValue(roiJson, new TypeReference<>() {});
            for (Map<String, Object> point : points) {
                Double lng = toDouble(point.get("lng"));
                Double lat = toDouble(point.get("lat"));
                if (lng != null && lat != null) {
                    polygon.add(new double[] { lng, lat });
                }
            }
        } catch (Exception exception) {
            log.warn("Failed to parse roi_json: {}", roiJson, exception);
        }
        return polygon;
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return new BigDecimal(String.valueOf(value)).doubleValue();
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
