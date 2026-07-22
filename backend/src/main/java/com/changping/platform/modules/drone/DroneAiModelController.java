package com.changping.platform.modules.drone;

import com.changping.platform.common.response.ApiResponse;
import com.changping.platform.modules.auth.security.PermissionCodes;
import com.changping.platform.modules.auth.security.PermissionGuard;
import com.changping.platform.modules.auth.service.AuthService;
import com.changping.platform.modules.auth.service.CurrentUserService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import com.changping.platform.modules.drone.entity.WaylineAlgoViolationAreaEntity;
import com.changping.platform.modules.drone.service.WaylineAlgoViolationAreaService;
import com.changping.platform.modules.qwen.QwenAlgorithmModelService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author tangxinglin
 * @Description //无人机AI模型控制器，提供AI模型列表查询、航线AI绑定详情查询及千问模型绑定接口
 * @Date 2026/04/18 10:00
 */
@RestController
@RequestMapping("/drone/ai-models")
public class DroneAiModelController {

    private final DroneProxyService droneProxyService;
    private final CurrentUserService currentUserService;
    private final PermissionGuard permissionGuard;
    private final DroneApiProperties droneApiProperties;
    private final QwenAlgorithmModelService qwenAlgorithmModelService;
    private final WaylineAlgoViolationAreaService waylineAlgoViolationAreaService;

    public DroneAiModelController(
            DroneProxyService droneProxyService,
            CurrentUserService currentUserService,
            PermissionGuard permissionGuard,
            DroneApiProperties droneApiProperties,
            QwenAlgorithmModelService qwenAlgorithmModelService,
            WaylineAlgoViolationAreaService waylineAlgoViolationAreaService) {
        this.droneProxyService = droneProxyService;
        this.currentUserService = currentUserService;
        this.permissionGuard = permissionGuard;
        this.droneApiProperties = droneApiProperties;
        this.qwenAlgorithmModelService = qwenAlgorithmModelService;
        this.waylineAlgoViolationAreaService = waylineAlgoViolationAreaService;
    }

    /**
     * @Author tangxinglin
     * @Description //分页查询AI算法模型列表
     * @Date 2026/04/18 10:00
     * @Param [page 页码，默认为1, pageSize 每页条数，默认为10]
     * @return ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> 分页AI模型列表
     */
    @GetMapping
    public ApiResponse<DroneProxyService.PageResult<Map<String, Object>>> listAiModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_AI_MODEL_LIST);
        return ApiResponse.ok(droneProxyService.listAiModels(droneApiProperties.getFixedWorkspaceId(), page, pageSize));
    }

    /**
     * @Author tangxinglin
     * @Description //获取指定航线的AI模型绑定详情，并将千问模型名称映射为本地模型名称
     * @Date 2026/04/18 10:00
     * @Param [flyLineId 航线ID]
     * @return ApiResponse<List<Map<String, Object>>> AI模型绑定详情列表
     */
    @GetMapping("/binding/{flyLineId}")
    public ApiResponse<List<Map<String, Object>>> getWaylineAiBindingDetail(@PathVariable String flyLineId) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_AI_MODEL_LIST);

        List<Map<String, Object>> bindings = droneProxyService.getWaylineAiBindingDetail(flyLineId);

        // Build label → local model name mapping from local qwen_algorithm_model table
        Map<String, String> labelToLocalName = qwenAlgorithmModelService.listEnabled().stream()
                .collect(Collectors.toMap(
                        QwenAlgorithmModelService.ModelItem::label,
                        QwenAlgorithmModelService.ModelItem::name,
                        (a, b) -> a));

        // Build (label|start|end) → violationAreaIds from local binding-area association table
        List<WaylineAlgoViolationAreaEntity> localAreas = waylineAlgoViolationAreaService.listByWayline(flyLineId);
        Map<String, List<Long>> areaIdsByKey = new LinkedHashMap<>();
        for (WaylineAlgoViolationAreaEntity entity : localAreas) {
            String key = entity.getLabel() + "|" + entity.getStartPointIndex() + "|" + entity.getEndPointIndex();
            areaIdsByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(entity.getViolationAreaId());
        }

        // Enrich Qwen bindings: map upstream model_name to local model name via label + attach area ids
        List<Map<String, Object>> enriched = bindings.stream().map(item -> {
            Object modelNo = item.get("model_no");
            Map<String, Object> copy = new LinkedHashMap<>(item);
            if ("QWENMODEL".equals(String.valueOf(modelNo))) {
                Object label = item.get("label");
                if (label != null) {
                    String localName = labelToLocalName.get(String.valueOf(label));
                    if (localName != null) {
                        copy.put("model_name", localName);
                        copy.put("modelName", localName);
                    }
                    Object startObj = item.get("start_point_index");
                    Object endObj = item.get("end_point_index");
                    if (startObj instanceof Number startNum && endObj instanceof Number endNum) {
                        String key = label + "|" + startNum.intValue() + "|" + endNum.intValue();
                        copy.put("violationAreaIds", areaIdsByKey.getOrDefault(key, List.of()));
                    } else {
                        copy.put("violationAreaIds", List.of());
                    }
                }
            }
            return copy;
        }).toList();

        return ApiResponse.ok(enriched);
    }

    /**
     * @Author tangxinglin
     * @Description //将千问AI模型绑定到指定航线,同时保存每条绑定的违章区域配置
     * @Date 2026/04/22 00:00
     * @Param [flyLineId 航线ID, request 包含绑定关系列表的请求体(每项可带 violationAreaIds)]
     * @return ApiResponse<Map<String, Object>> 绑定操作结果
     */
    @PostMapping("/waylines/{flyLineId}/bind-qwen")
    public ApiResponse<Map<String, Object>> bindQwenToWayline(
            @PathVariable String flyLineId,
            @RequestBody BindQwenRequest request) {
        currentUserService.requireClientType(AuthService.ClientType.WEB);
        permissionGuard.require(PermissionCodes.API_DRONE_WAYLINE_BIND_QWEN);

        List<Map<String, Object>> bindings = request.bindings() == null ? List.of() : request.bindings();

        // 先清空该航线的所有区域关联,再按 bindings 重建;即使某项没带 violationAreaIds 也不会残留旧记录
        waylineAlgoViolationAreaService.clearByWayline(flyLineId);

        // 上游只需要标准绑定字段,剥掉 violationAreaIds 再透传,避免污染 DJI 平台调用
        List<Map<String, Object>> upstreamBindings = new ArrayList<>(bindings.size());
        for (Map<String, Object> binding : bindings) {
            Map<String, Object> copy = new LinkedHashMap<>(binding);
            Object areaIdsObj = copy.remove("violationAreaIds");

            String label = copy.get("label") != null ? String.valueOf(copy.get("label")) : null;
            Integer startIdx = toInt(copy.get("startPointIndex"));
            Integer endIdx = toInt(copy.get("endPointIndex"));
            List<Long> areaIds = toLongList(areaIdsObj);
            if (label != null && startIdx != null && endIdx != null && !areaIds.isEmpty()) {
                waylineAlgoViolationAreaService.replaceBindingAreas(flyLineId, label, startIdx, endIdx, areaIds);
            }
            upstreamBindings.add(copy);
        }

        return ApiResponse.ok(droneProxyService.bindQwenToWayline(flyLineId, upstreamBindings));
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<Long> toLongList(Object value) {
        if (value instanceof List<?> list) {
            List<Long> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item == null) continue;
                if (item instanceof Number number) {
                    result.add(number.longValue());
                } else {
                    try {
                        result.add(Long.parseLong(String.valueOf(item)));
                    } catch (NumberFormatException ignored) {}
                }
            }
            return result;
        }
        return List.of();
    }

    public record BindQwenRequest(List<Map<String, Object>> bindings) {
    }
}
