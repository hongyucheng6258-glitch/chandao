package com.pms.modules.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.mapper.SysUserMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskMapper taskMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper sysUserMapper;

    /** 状态机: wait -> doing/cancel; doing -> done/pause/cancel; pause -> doing/cancel; done -> closed; */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "wait", Set.of("doing", "cancel", "closed"),
            "doing", Set.of("done", "pause", "cancel"),
            "pause", Set.of("doing", "cancel"),
            "done", Set.of("closed"),
            "cancel", Set.of("closed"),
            "closed", Set.of());

    @GetMapping("/page")
    public Result<Page<Task>> page(@RequestParam(defaultValue = "1") long pageNum,
                                   @RequestParam(defaultValue = "10") long pageSize,
                                   @RequestParam(required = false) Long sprintId,
                                   @RequestParam(required = false) Long storyId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) Long assignedTo,
                                   @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(sprintId != null, Task::getSprintId, sprintId)
                .eq(storyId != null, Task::getStoryId, storyId)
                .eq(status != null && !status.isBlank(), Task::getStatus, status)
                .eq(assignedTo != null, Task::getAssignedTo, assignedTo)
                .like(keyword != null && !keyword.isBlank(), Task::getName, keyword)
                .orderByDesc(Task::getId);
        return Result.ok(taskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    /** 看板数据: 某迭代全部任务 */
    @GetMapping("/board")
    public Result<List<Task>> board(@RequestParam Long sprintId) {
        return Result.ok(taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getSprintId, sprintId).orderByAsc(Task::getPriority)));
    }

    @GetMapping("/{id}")
    public Result<Task> detail(@PathVariable Long id) {
        return Result.ok(taskMapper.selectById(id));
    }

    @PostMapping
    @LogOperation(objectType = "task", action = "创建任务", objectId = "#result.data")
    public Result<Long> create(@RequestBody Task task) {
        task.setId(null);
        task.setStatus("wait");
        task.setConsumed(BigDecimal.ZERO);
        task.setLeft(task.getEstimate() == null ? BigDecimal.ZERO : task.getEstimate());
        taskMapper.insert(task);
        return Result.ok(task.getId());
    }

    @PutMapping("/{id}")
    @LogOperation(objectType = "task", action = "编辑任务", objectId = "#id")
    public Result<Void> update(@PathVariable Long id, @RequestBody Task task) {
        task.setId(id);
        task.setStatus(null);
        taskMapper.updateById(task);
        return Result.ok();
    }

    /** 指派(带分布式锁防并发超派) */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('task:assign')")
    @LogOperation(objectType = "task", action = "指派任务", objectId = "#id")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        String lockKey = "lock:task:assign:" + id;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BizException("任务正在被其他人指派, 请稍后重试");
        }
        try {
            Task task = taskMapper.selectById(id);
            if (task == null) {
                throw new BizException("任务不存在");
            }
            if ("closed".equals(task.getStatus()) || "cancel".equals(task.getStatus())) {
                throw new BizException("已关闭/取消的任务不能指派");
            }
            Task update = new Task();
            update.setId(id);
            update.setAssignedTo(body.get("assignedTo"));
            taskMapper.updateById(update);
            return Result.ok();
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /** 状态流转: action = start / finish / pause / cancel / close */
    @PutMapping("/{id}/status")
    @LogOperation(objectType = "task", action = "任务状态流转", objectId = "#id")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String action = body == null ? null : body.get("action");
        if (action == null || action.isBlank()) {
            throw new BizException("操作类型(action)不能为空");
        }
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        String target = switch (body.get("action")) {
            case "start" -> "doing";
            case "finish" -> "done";
            case "pause" -> "pause";
            case "cancel" -> "cancel";
            case "close" -> "closed";
            default -> throw new BizException("未知操作");
        };
        if (!TRANSITIONS.getOrDefault(task.getStatus(), Set.of()).contains(target)) {
            throw new BizException(String.format("任务状态不允许从 [%s] 流转到 [%s]", task.getStatus(), target));
        }
        Task update = new Task();
        update.setId(id);
        update.setStatus(target);
        if ("done".equals(target)) {
            update.setFinishedBy(com.pms.common.utils.SecurityUtil.getUserId());
            update.setFinishedTime(LocalDateTime.now());
            update.setLeft(BigDecimal.ZERO);
        }
        taskMapper.updateById(update);
        return Result.ok();
    }

    /** 看板拖拽: 直接指定目标状态, 复用状态机校验 */
    @PutMapping("/{id}/move")
    @LogOperation(objectType = "task", action = "拖拽改状态", objectId = "#id")
    public Result<Void> move(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        String target = body.get("status");
        if (target == null || target.isBlank()) {
            throw new BizException("目标状态不能为空");
        }
        if (task.getStatus().equals(target)) {
            return Result.ok();
        }
        Set<String> allowed = TRANSITIONS.getOrDefault(task.getStatus(), Set.of());
        if (!allowed.contains(target)) {
            throw new BizException(String.format("任务状态不允许从 [%s] 拖拽到 [%s]", task.getStatus(), target));
        }
        Task update = new Task();
        update.setId(id);
        update.setStatus(target);
        if ("done".equals(target)) {
            update.setFinishedBy(com.pms.common.utils.SecurityUtil.getUserId());
            update.setFinishedTime(LocalDateTime.now());
            update.setLeft(BigDecimal.ZERO);
        } else if ("done".equals(task.getStatus())) {
            update.setFinishedBy(null);
            update.setFinishedTime(null);
            update.setLeft(task.getEstimate() == null ? BigDecimal.ZERO : task.getEstimate());
        }
        taskMapper.updateById(update);
        return Result.ok();
    }

    /** 登记工时: consumed += hours, left = left */
    @PutMapping("/{id}/hours")
    @LogOperation(objectType = "task", action = "登记工时", objectId = "#id")
    public Result<Void> logHours(@PathVariable Long id, @RequestBody HoursBody body) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if (!"doing".equals(task.getStatus()) && !"wait".equals(task.getStatus())) {
            throw new BizException("只有进行中/未开始的任务可以登记工时");
        }
        if (body.getConsumed() == null || body.getConsumed().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("消耗工时必须大于 0");
        }
        Task update = new Task();
        update.setId(id);
        update.setConsumed(task.getConsumed().add(body.getConsumed()));
        update.setLeft(body.getLeft() == null ? task.getLeft() : body.getLeft());
        taskMapper.updateById(update);
        return Result.ok();
    }

    /** 成员工时汇总: 按处理人聚合预估/已耗/剩余工时与任务数 */
    @GetMapping("/workhour-summary")
    public Result<List<Map<String, Object>>> workhourSummary(@RequestParam(required = false) Long sprintId,
                                                             @RequestParam(required = false) Long assignedTo) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(sprintId != null, Task::getSprintId, sprintId)
                .orderByDesc(Task::getId);
        List<Task> tasks = taskMapper.selectList(wrapper);
        Map<Long, BigDecimal[]> agg = new LinkedHashMap<>();
        for (Task t : tasks) {
            Long aid = t.getAssignedTo();
            if (aid == null) continue;
            if (assignedTo != null && !assignedTo.equals(aid)) continue;
            BigDecimal[] a = agg.computeIfAbsent(aid, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            a[0] = a[0].add(t.getEstimate() == null ? BigDecimal.ZERO : t.getEstimate());
            a[1] = a[1].add(t.getConsumed() == null ? BigDecimal.ZERO : t.getConsumed());
            a[2] = a[2].add(t.getLeft() == null ? BigDecimal.ZERO : t.getLeft());
            a[3] = a[3].add(BigDecimal.ONE);
            if ("done".equals(t.getStatus())) a[4] = a[4].add(BigDecimal.ONE);
        }
        List<SysUser> users = sysUserMapper.selectList(null);
        Map<Long, String> nameMap = users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> b));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal[]> e : agg.entrySet()) {
            BigDecimal[] a = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("assigneeId", e.getKey());
            m.put("assigneeName", nameMap.getOrDefault(e.getKey(), "未知用户"));
            m.put("estimateTotal", a[0]);
            m.put("consumedTotal", a[1]);
            m.put("leftTotal", a[2]);
            m.put("taskCount", a[3].intValue());
            m.put("doneCount", a[4].intValue());
            result.add(m);
        }
        result.sort((x, y) -> ((BigDecimal) y.get("consumedTotal")).compareTo((BigDecimal) x.get("consumedTotal")));
        return Result.ok(result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskMapper.deleteById(id);
        return Result.ok();
    }

    @Data
    public static class HoursBody {
        private BigDecimal consumed;
        private BigDecimal left;
    }
}
