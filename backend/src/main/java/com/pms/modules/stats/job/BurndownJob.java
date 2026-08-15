package com.pms.modules.stats.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pms.modules.project.entity.Sprint;
import com.pms.modules.project.mapper.SprintMapper;
import com.pms.modules.stats.entity.SprintBurndown;
import com.pms.modules.stats.mapper.SprintBurndownMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 燃尽图快照定时任务: 每天 23:55 统计所有进行中迭代的剩余工时
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BurndownJob {

    private final SprintMapper sprintMapper;
    private final TaskMapper taskMapper;
    private final SprintBurndownMapper burndownMapper;

    @Scheduled(cron = "0 55 23 * * ?")
    public void snapshot() {
        doSnapshot(LocalDate.now());
    }

    /** 手动触发(也方便测试): 为进行中的迭代生成当日快照 */
    public void doSnapshot(LocalDate date) {
        List<Sprint> doingSprints = sprintMapper.selectList(
                new LambdaQueryWrapper<Sprint>().eq(Sprint::getStatus, "doing"));
        for (Sprint sprint : doingSprints) {
            List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                    .eq(Task::getSprintId, sprint.getId())
                    .select(Task::getId, Task::getStatus, Task::getLeft));
            BigDecimal leftHours = tasks.stream()
                    .map(t -> t.getLeft() == null ? BigDecimal.ZERO : t.getLeft())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long done = tasks.stream().filter(t -> "done".equals(t.getStatus()) || "closed".equals(t.getStatus())).count();

            SprintBurndown snapshot = new SprintBurndown();
            snapshot.setSprintId(sprint.getId());
            snapshot.setStatDate(date);
            snapshot.setLeftHours(leftHours);
            snapshot.setTaskTotal(tasks.size());
            snapshot.setTaskDone((int) done);

            // 幂等: 同一天重复执行则更新
            SprintBurndown existing = burndownMapper.selectOne(new QueryWrapper<SprintBurndown>()
                    .eq("sprint_id", sprint.getId()).eq("stat_date", date));
            if (existing == null) {
                burndownMapper.insert(snapshot);
            } else {
                snapshot.setId(existing.getId());
                burndownMapper.updateById(snapshot);
            }
        }
        log.info("燃尽图快照完成, 迭代数: {}", doingSprints.size());
    }
}
