package com.changping.platform.common.async;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @Author lxy
 * @Description //通用异步批量插入写入器：内存队列攒批，定时批量落库，降低高频单条 INSERT 对数据库的压力。
 * 适用于通知、审计日志等允许轻微延迟（毫秒级）且失败可重试的写入场景。
 * - 自管理守护线程，不占用 Spring 调度线程池
 * - 攒批窗口 flushIntervalMs / 最大批次 maxBatchSize，先到先刷
 * - 批量失败自动降级为逐条重试，单条仍失败仅告警丢弃，不影响业务主流程
 * - 应用优雅停机时（@PreDestroy）兜底刷出剩余数据
 * @Date 2026/08/14
 */
public abstract class BatchInsertWorker<T> {

    private static final Logger log = LoggerFactory.getLogger(BatchInsertWorker.class);

    private static final int DEFAULT_QUEUE_CAPACITY = 100_000;

    private final BlockingQueue<T> queue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
    private final JdbcTemplate jdbcTemplate;
    private final String sql;
    private final int maxBatchSize;
    private final long flushIntervalMs;
    private final Thread workerThread;
    private volatile boolean running = true;

    protected BatchInsertWorker(JdbcTemplate jdbcTemplate, String sql,
                                int maxBatchSize, long flushIntervalMs, String threadName) {
        this.jdbcTemplate = jdbcTemplate;
        this.sql = sql;
        this.maxBatchSize = maxBatchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.workerThread = new Thread(this::run, threadName);
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    /** 入队一条待写入数据（非阻塞，队列满时丢弃并告警） */
    public boolean enqueue(T item) {
        boolean ok = queue.offer(item);
        if (!ok) {
            log.warn("[{}] batch queue full, item dropped", workerThread.getName());
        }
        return ok;
    }

    /** 将实体转换为 SQL 占位符参数数组 */
    protected abstract Object[] toArgs(T item);

    private void run() {
        List<T> batch = new ArrayList<>(maxBatchSize);
        while (running) {
            T item = null;
            try {
                item = queue.poll(flushIntervalMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (item != null) {
                batch.add(item);
                queue.drainTo(batch, maxBatchSize - batch.size());
            }
            boolean timedOut = item == null;
            if (!batch.isEmpty() && (timedOut || batch.size() >= maxBatchSize)) {
                flush(batch);
                batch = new ArrayList<>(maxBatchSize);
            }
        }
        // 线程退出前兜底刷出剩余数据（含已攒批未刷的 batch 与队列余量）
        if (!batch.isEmpty()) {
            flush(batch);
        }
        List<T> rest = new ArrayList<>();
        queue.drainTo(rest);
        if (!rest.isEmpty()) {
            flush(rest);
        }
    }

    private void flush(List<T> batch) {
        try {
            List<Object[]> batchArgs = new ArrayList<>(batch.size());
            for (T item : batch) {
                batchArgs.add(toArgs(item));
            }
            jdbcTemplate.batchUpdate(sql, batchArgs);
        } catch (Exception e) {
            log.error("[{}] batch insert failed ({} rows), degrade to single retry",
                    workerThread.getName(), batch.size(), e);
            for (T item : batch) {
                try {
                    jdbcTemplate.batchUpdate(sql, List.of(new Object[][]{toArgs(item)}));
                } catch (Exception e2) {
                    log.error("[{}] single insert failed, item dropped: {}", workerThread.getName(), item, e2);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        workerThread.interrupt();
        try {
            workerThread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
