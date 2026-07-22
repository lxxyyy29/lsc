package com.changping.platform.modules.community.service;

import com.changping.platform.modules.community.entity.PatrolTaskEntity;
import com.changping.platform.modules.community.mapper.PatrolTaskMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PatrolTaskService {

    private final PatrolTaskMapper mapper;

    public PatrolTaskService(PatrolTaskMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 为所有小网格生成本周巡查任务
     */
    public int generateWeeklyTasks() {
        // 获取所有活跃的小网格
        List<Map<String, Object>> grids = mapper.findActiveSmallGrids();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        int count = 0;

        for (Map<String, Object> grid : grids) {
            Long gridId = ((Number) grid.get("grid_id")).longValue();
            String gridName = (String) grid.get("grid_name");
            // 检查是否已生成本周任务
            if (mapper.existsTaskForWeek(gridId, startOfWeek)) {
                continue;
            }
            PatrolTaskEntity task = new PatrolTaskEntity();
            task.setGridId(gridId);
            task.setTaskName(gridName + " - 周巡查任务");
            task.setPlannedDate(startOfWeek.plusDays(count % 7));
            task.setStatus("PENDING");
            mapper.insert(task);
            count++;
        }
        return count;
    }

    /**
     * 检查超期未巡任务并标记为 OVERDUE
     */
    public int markOverdueTasks() {
        return mapper.markOverdueTasks(LocalDate.now());
    }

    public List<PatrolTaskEntity> listByUser(Long userId) {
        return mapper.findByUserId(userId);
    }

    public List<PatrolTaskEntity> listByGrid(Long gridId) {
        return mapper.findByGridId(gridId);
    }

    public boolean completeTask(Long taskId) {
        return mapper.completeTask(taskId, LocalDate.now()) > 0;
    }
}
