package com.changping.platform.modules.integration.alarm.service;

import com.changping.platform.modules.drone.DroneProxyService;
import com.changping.platform.modules.drone.config.DroneApiProperties;
import com.changping.platform.modules.integration.alarm.config.AlarmIntegrationProperties;
import com.changping.platform.modules.integration.alarm.dto.ThirdPartyAlarmIngestResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;

/**
 * 三方平台告警「主动拉取」服务。
 *
 * <p>为什么需要它：本平台原有的告警接入是「推送」模式 —— 板端/平台把告警 POST 到
 * {@code /integrations/alarms/callback}，平台侧不配置回调就一条都不会来。
 * 而大疆 AI 智慧巡查平台本身提供告警查询接口（{@code /manage/api/v1/alarmData/getAlarmDataListPageVo}），
 * 因此本服务定时/手动去平台抓取告警，再交给既有的告警摄入管线。
 *
 * <p>入库链路完全复用 {@link ThirdPartyAlarmIngestService#ingest(Map, boolean)}：
 * 归一化 → 去重（dedupKey）→ 写 MongoDB alarm_events → 投影成 MySQL 事件 → 违章区域过滤。
 * 因此拉取与推送两条路径产生的数据完全一致，重复拉取也不会产生重复事件。</p>
 */
@Service
public class AlarmPullService {

    private static final Logger log = LoggerFactory.getLogger(AlarmPullService.class);

    private final DroneProxyService droneProxyService;
    private final DroneApiProperties droneApiProperties;
    private final AlarmIntegrationProperties properties;
    private final ThirdPartyAlarmIngestService ingestService;

    public AlarmPullService(
            DroneProxyService droneProxyService,
            DroneApiProperties droneApiProperties,
            AlarmIntegrationProperties properties,
            ThirdPartyAlarmIngestService ingestService) {
        this.droneProxyService = droneProxyService;
        this.droneApiProperties = droneApiProperties;
        this.properties = properties;
        this.ingestService = ingestService;
    }

    /**
     * 拉取一次三方告警并入库（幂等：已存在的告警会被识别为 duplicate 而不重复建事件）。
     *
     * @return 统计信息：fetched 拉取条数、created 新建事件数、duplicated 重复跳过数、failed 失败数、pages 实际页数
     */
    public Map<String, Object> pullOnce() {
        AlarmIntegrationProperties.Pull pull = properties.getPull();
        int fetched = 0;
        int created = 0;
        int duplicated = 0;
        int failed = 0;
        int pages = 0;
        // 保留第一条失败原因回传：平台接口不通/字段不匹配时，调用方（页面/日志）能直接看到原因
        String firstError = null;

        int maxPages = Math.max(1, pull.getMaxPages());
        for (int page = 1; page <= maxPages; page++) {
            List<Map<String, Object>> items;
            try {
                items = droneProxyService.fetchAlarmList(pull.getPath(), buildRequest(pull, page));
            } catch (Exception exception) {
                // 平台没有该接口、鉴权失败、网络不通都会走到这里：记日志并结束本轮，避免刷屏
                log.warn("拉取三方告警失败：path={}, page={}, 原因={}", pull.getPath(), page, exception.toString());
                failed++;
                if (firstError == null) {
                    firstError = "拉取失败: " + exception;
                }
                break;
            }
            pages++;
            if (items.isEmpty()) {
                break;
            }
            fetched += items.size();
            for (Map<String, Object> item : items) {
                try {
                    ThirdPartyAlarmIngestResult result = ingestService.ingest(item, true);
                    if (result.duplicate()) {
                        duplicated++;
                    } else {
                        created++;
                    }
                } catch (Exception exception) {
                    if (isAlreadyIngested(exception)) {
                        // 同一个 externalEventId 在 Mongo 里已存在多份文档时，读取会抛
                        // IncorrectResultSizeDataAccessException：说明该告警早就入库了
                        // （历史推送与平台拉取的去重键不同会留下多份文档），按「重复」处理，
                        // 否则每轮同步都会把整批误报成失败。
                        duplicated++;
                        log.info("告警已存在（Mongo 同 externalEventId 有多份文档），按重复跳过：{}",
                                item.get("alarmId") != null ? item.get("alarmId") : item.get("id"));
                    } else {
                        failed++;
                        log.warn("告警入库失败：externalEventId={}, 原因={}",
                                item.get("alarmId") != null ? item.get("alarmId") : item.get("id"),
                                exception.toString());
                        if (firstError == null) {
                            firstError = "入库失败: " + exception;
                        }
                    }
                }
            }
            if (items.size() < pull.getPageSize()) {
                break;
            }
        }

        Map<String, Object> statistic = new LinkedHashMap<>();
        statistic.put("fetched", fetched);
        statistic.put("created", created);
        statistic.put("duplicated", duplicated);
        statistic.put("failed", failed);
        statistic.put("pages", pages);
        if (firstError != null) {
            statistic.put("firstError", firstError);
        }
        log.info("三方告警拉取完成：{}", statistic);
        return statistic;
    }

    /** 是否为「该告警已在库」类的异常（Mongo 同 externalEventId 存在多份文档，读取时抛非唯一结果） */
    private boolean isAlreadyIngested(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current instanceof IncorrectResultSizeDataAccessException) {
                return true;
            }
        }
        return false;
    }

    /** 组装三方接口请求体（沿用平台分页约定 page_num / page_size） */
    private Map<String, Object> buildRequest(AlarmIntegrationProperties.Pull pull, int page) {
        Map<String, Object> request = new LinkedHashMap<>();
        if (pull.isIncludeWorkspace()) {
            request.put("workspace_id", droneApiProperties.getFixedWorkspaceId());
        }
        request.put("page_num", page);
        request.put("page_size", pull.getPageSize());
        return request;
    }
}
