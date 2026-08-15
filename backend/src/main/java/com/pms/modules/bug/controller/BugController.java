package com.pms.modules.bug.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.bug.entity.Bug;
import com.pms.modules.bug.mapper.BugMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/bugs")
@RequiredArgsConstructor
public class BugController {

    private final BugMapper bugMapper;

    /** 缺陷状态机: active -> resolved/closed; resolved -> closed/active(打回) */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "active", Set.of("resolved", "closed"),
            "resolved", Set.of("closed", "active"),
            "closed", Set.of("active"));

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('bug:list')")
    public Result<Page<Bug>> page(@RequestParam(defaultValue = "1") long pageNum,
                                  @RequestParam(defaultValue = "10") long pageSize,
                                  @RequestParam(required = false) Long productId,
                                  @RequestParam(required = false) Long sprintId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Integer severity,
                                  @RequestParam(required = false) Long assignedTo,
                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Bug> wrapper = new LambdaQueryWrapper<Bug>()
                .eq(productId != null, Bug::getProductId, productId)
                .eq(sprintId != null, Bug::getSprintId, sprintId)
                .eq(status != null && !status.isBlank(), Bug::getStatus, status)
                .eq(severity != null, Bug::getSeverity, severity)
                .eq(assignedTo != null, Bug::getAssignedTo, assignedTo)
                .like(keyword != null && !keyword.isBlank(), Bug::getTitle, keyword)
                .orderByAsc(Bug::getSeverity).orderByDesc(Bug::getId);
        return Result.ok(bugMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    public Result<Bug> detail(@PathVariable Long id) {
        return Result.ok(bugMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('bug:add')")
    @LogOperation(objectType = "bug", action = "提交Bug", objectId = "#result.data")
    public Result<Long> create(@RequestBody Bug bug) {
        bug.setId(null);
        bug.setStatus("active");
        bugMapper.insert(bug);
        return Result.ok(bug.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('bug:add')")
    @LogOperation(objectType = "bug", action = "编辑Bug", objectId = "#id")
    public Result<Void> update(@PathVariable Long id, @RequestBody Bug bug) {
        bug.setId(id);
        bug.setStatus(null);
        bugMapper.updateById(bug);
        return Result.ok();
    }

    /** 指派 */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('bug:handle')")
    @LogOperation(objectType = "bug", action = "指派Bug", objectId = "#id")
    public Result<Void> assign(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        Bug update = new Bug();
        update.setId(id);
        update.setAssignedTo(body.get("assignedTo"));
        bugMapper.updateById(update);
        return Result.ok();
    }

    /**
     * 状态流转: action = resolve(解决, 需 resolution) / close(验证关闭) / reopen(打回激活)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('bug:handle')")
    @LogOperation(objectType = "bug", action = "Bug状态流转", objectId = "#id")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody FlowBody body) {
        Bug bug = bugMapper.selectById(id);
        if (bug == null) {
            throw new BizException("Bug不存在");
        }
        String target = switch (body.getAction()) {
            case "resolve" -> "resolved";
            case "close" -> "closed";
            case "reopen" -> "active";
            default -> throw new BizException("未知操作: " + body.getAction());
        };
        if (!TRANSITIONS.getOrDefault(bug.getStatus(), Set.of()).contains(target)) {
            throw new BizException(String.format("Bug状态不允许从 [%s] 流转到 [%s]", bug.getStatus(), target));
        }
        Bug update = new Bug();
        update.setId(id);
        update.setStatus(target);
        switch (target) {
            case "resolved" -> {
                if (body.getResolution() == null || body.getResolution().isBlank()) {
                    throw new BizException("解决时必须选择解决方案");
                }
                update.setResolution(body.getResolution());
                update.setResolvedBy(SecurityUtil.getUserId());
                update.setResolvedTime(LocalDateTime.now());
            }
            case "closed" -> {
                update.setClosedBy(SecurityUtil.getUserId());
                update.setClosedTime(LocalDateTime.now());
            }
            case "active" -> {
                update.setResolution("");
                update.setResolvedBy(null);
                update.setResolvedTime(null);
                update.setClosedBy(null);
                update.setClosedTime(null);
            }
            default -> { }
        }
        bugMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('bug:add')")
    public Result<Void> delete(@PathVariable Long id) {
        bugMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    @PreAuthorize("hasAuthority('bug:add')")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.ok();
        }
        bugMapper.deleteBatchIds(ids);
        return Result.ok();
    }

    @PostMapping("/batch-assign")
    @PreAuthorize("hasAuthority('bug:handle')")
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
            Bug update = new Bug();
            update.setId(id);
            update.setAssignedTo(assignedTo);
            bugMapper.updateById(update);
        }
        return Result.ok();
    }

    @Data
    public static class FlowBody {
        private String action;
        private String resolution;
    }
}
