package com.pms.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.product.entity.Story;
import com.pms.modules.product.mapper.StoryMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryMapper storyMapper;
    private final TaskMapper taskMapper;

    /** 状态机: draft -> active; active -> changed/closed; changed -> active/closed; closed -> active(重新打开) */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "draft", Set.of("active", "closed"),
            "active", Set.of("changed", "closed"),
            "changed", Set.of("active", "closed"),
            "closed", Set.of("active"));

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('story:list')")
    public Result<Page<Story>> page(@RequestParam(defaultValue = "1") long pageNum,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Long productId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer priority,
                                    @RequestParam(required = false) Long assignedTo,
                                    @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Story> wrapper = new LambdaQueryWrapper<Story>()
                .eq(productId != null, Story::getProductId, productId)
                .eq(status != null && !status.isBlank(), Story::getStatus, status)
                .eq(priority != null, Story::getPriority, priority)
                .eq(assignedTo != null, Story::getAssignedTo, assignedTo)
                .like(keyword != null && !keyword.isBlank(), Story::getTitle, keyword)
                .orderByAsc(Story::getPriority).orderByDesc(Story::getId);
        return Result.ok(storyMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/options")
    public Result<java.util.List<Story>> options(@RequestParam(required = false) Long productId) {
        return Result.ok(storyMapper.selectList(new LambdaQueryWrapper<Story>()
                .eq(productId != null, Story::getProductId, productId)
                .in(Story::getStatus, "active", "changed")
                .select(Story::getId, Story::getTitle, Story::getProductId, Story::getStatus)
                .orderByDesc(Story::getId)));
    }

    @GetMapping("/{id}")
    public Result<Story> detail(@PathVariable Long id) {
        return Result.ok(storyMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('story:add')")
    @LogOperation(objectType = "story", action = "创建需求", objectId = "#result.data")
    public Result<Long> create(@RequestBody Story story) {
        story.setId(null);
        story.setStatus("draft");
        story.setStage("wait");
        storyMapper.insert(story);
        return Result.ok(story.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('story:add')")
    @LogOperation(objectType = "story", action = "编辑需求", objectId = "#id")
    public Result<Void> update(@PathVariable Long id, @RequestBody Story story) {
        story.setId(id);
        story.setStatus(null); // 状态只走流转接口
        story.setStage(null);
        // 已激活的需求被修改后进入"已变更"
        Story old = storyMapper.selectById(id);
        if (old != null && "active".equals(old.getStatus())) {
            story.setStatus("changed");
        }
        storyMapper.updateById(story);
        return Result.ok();
    }

    /** 状态流转: action = activate / close / reopen */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('story:flow')")
    @LogOperation(objectType = "story", action = "需求状态流转", objectId = "#id")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody FlowBody body) {
        if (body == null || body.getAction() == null || body.getAction().isBlank()) {
            throw new BizException("操作类型(action)不能为空");
        }
        Story story = storyMapper.selectById(id);
        if (story == null) {
            throw new BizException("需求不存在");
        }
        String target = switch (body.getAction()) {
            case "activate" -> "active";
            case "close" -> "closed";
            case "reopen" -> "active";
            default -> throw new BizException("未知操作: " + body.getAction());
        };
        checkTransition(story.getStatus(), target);
        Story update = new Story();
        update.setId(id);
        update.setStatus(target);
        if ("closed".equals(target)) {
            update.setClosedReason(body.getClosedReason() == null ? "done" : body.getClosedReason());
        }
        storyMapper.updateById(update);
        return Result.ok();
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('story:flow')")
    @LogOperation(objectType = "story", action = "指派需求", objectId = "#id")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Story update = new Story();
        update.setId(id);
        update.setAssignedTo(body.get("assignedTo"));
        storyMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('story:add')")
    public Result<Void> delete(@PathVariable Long id) {
        storyMapper.deleteById(id);
        return Result.ok();
    }

    private void checkTransition(String from, String to) {
        if (!TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new BizException(String.format("需求状态不允许从 [%s] 流转到 [%s]", from, to));
        }
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('story:add')")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.ok();
        }
        storyMapper.deleteBatchIds(ids);
        return Result.ok();
    }

    /** 拆分任务: 由需求派生一条执行任务(任务必带 Sprint), 同步需求阶段为研发中 */
    @PostMapping("/{id}/tasks")
    @PreAuthorize("hasAuthority('story:add')")
    @LogOperation(objectType = "story", action = "拆分任务", objectId = "#id")
    public Result<Long> createTask(@PathVariable Long id, @RequestBody StoryTaskBody body) {
        Story story = storyMapper.selectById(id);
        if (story == null) {
            throw new BizException("需求不存在");
        }
        if (body.getSprintId() == null) {
            throw new BizException("请选择所属迭代");
        }
        if (body.getAssignedTo() == null) {
            throw new BizException("请选择处理人");
        }
        Task task = new Task();
        task.setSprintId(body.getSprintId());
        task.setStoryId(id);
        task.setName(body.getName() != null && !body.getName().isBlank() ? body.getName() : story.getTitle());
        task.setAssignedTo(body.getAssignedTo());
        task.setEstimate(body.getEstimate() == null ? BigDecimal.ZERO : body.getEstimate());
        task.setLeft(body.getEstimate() == null ? BigDecimal.ZERO : body.getEstimate());
        task.setConsumed(BigDecimal.ZERO);
        task.setStatus("wait");
        task.setDeadline(body.getDeadline());
        taskMapper.insert(task);
        // 需求进入研发中
        Story upd = new Story();
        upd.setId(id);
        upd.setStage("developing");
        storyMapper.updateById(upd);
        return Result.ok(task.getId());
    }

    @PostMapping("/batch-assign")
    @PreAuthorize("hasAuthority('story:flow')")
    public Result<Void> batchAssign(@RequestBody Map<String, Object> body) {
        List<?> rawIds = (List<?>) body.get("ids");
        if (rawIds == null || rawIds.isEmpty()) {
            return Result.ok();
        }
        Object to = body.get("assignedTo");
        if (to == null) {
            return Result.ok();
        }
        Long assignedTo = to instanceof Number ? ((Number) to).longValue() : Long.valueOf(to.toString());
        List<Long> ids = new ArrayList<>();
        for (Object o : rawIds) {
            ids.add(((Number) o).longValue());
        }
        for (Long id : ids) {
            Story update = new Story();
            update.setId(id);
            update.setAssignedTo(assignedTo);
            storyMapper.updateById(update);
        }
        return Result.ok();
    }

    @Data
    public static class FlowBody {
        private String action;
        private String closedReason;
    }

    @Data
    public static class StoryTaskBody {
        private Long sprintId;
        private Long assignedTo;
        private String name;
        private BigDecimal estimate;
        private LocalDate deadline;
    }
}
