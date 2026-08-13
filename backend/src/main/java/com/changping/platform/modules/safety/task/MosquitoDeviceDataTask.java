package com.changping.platform.modules.safety.task;

import com.changping.platform.modules.safety.service.MosquitoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 爱卫蚊媒 — 监测设备演示数据补充任务
 * 真实设备接入前，每 30 分钟为在管孳生地的设备生成一条最新监测数据，
 * 保证演示趋势图持续有新的数据点；真实设备接入后仅保留推送通道，此任务可停用。
 */
@Component
public class MosquitoDeviceDataTask {

    private static final Logger log = LoggerFactory.getLogger(MosquitoDeviceDataTask.class);

    private final MosquitoService mosquitoService;

    public MosquitoDeviceDataTask(MosquitoService mosquitoService) {
        this.mosquitoService = mosquitoService;
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void generateLatestPoint() {
        try {
            log.info("蚊媒监测设备演示数据补充开始");
            mosquitoService.simulateLatestPoint();
            log.info("蚊媒监测设备演示数据补充完成");
        } catch (Exception e) {
            log.error("蚊媒监测设备演示数据补充失败: {}", e.getMessage(), e);
        }
    }
}
