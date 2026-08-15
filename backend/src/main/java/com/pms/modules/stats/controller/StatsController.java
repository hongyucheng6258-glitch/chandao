package com.pms.modules.stats.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.result.Result;
import com.pms.modules.bug.entity.Bug;
import com.pms.modules.bug.mapper.BugMapper;
import com.pms.modules.stats.entity.SprintBurndown;
import com.pms.modules.stats.mapper.SprintBurndownMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计报表: 燃尽图 + Bug 分布 + 任务分布
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final SprintBurndownMapper burndownMapper;
    private final BugMapper bugMapper;
    private final TaskMapper taskMapper;

    /** 燃尽图: 优先读快照表 */
    @GetMapping("/burndown")
    public Result<List<SprintBurndown>> burndown(@RequestParam Long sprintId) {
        return Result.ok(burndownMapper.selectList(new LambdaQueryWrapper<SprintBurndown>()
                .eq(SprintBurndown::getSprintId, sprintId)
                .orderByAsc(SprintBurndown::getStatDate)));
    }

    /** Bug 状态分布 */
    @GetMapping("/bug-distribution")
    public Result<List<Map<String, Object>>> bugDistribution(@RequestParam(required = false) Long productId) {
        List<Bug> bugs = bugMapper.selectList(new LambdaQueryWrapper<Bug>()
                .eq(productId != null, Bug::getProductId, productId)
                .select(Bug::getId, Bug::getStatus));
        Map<String, Long> counter = new java.util.HashMap<>();
        bugs.forEach(b -> counter.merge(b.getStatus(), 1L, Long::sum));
        return Result.ok(counter.entrySet().stream()
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList());
    }

    /** 迭代任务状态分布 */
    @GetMapping("/task-distribution")
    public Result<List<Map<String, Object>>> taskDistribution(@RequestParam Long sprintId) {
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getSprintId, sprintId)
                .select(Task::getId, Task::getStatus));
        Map<String, Long> counter = new java.util.HashMap<>();
        tasks.forEach(t -> counter.merge(t.getStatus(), 1L, Long::sum));
        return Result.ok(counter.entrySet().stream()
                .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue()))
                .toList());
    }
}
