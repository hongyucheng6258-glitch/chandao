package com.pms.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.annotation.LogOperation;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.product.entity.Story;
import com.pms.modules.product.mapper.StoryMapper;
import com.pms.modules.project.entity.Sprint;
import com.pms.modules.project.entity.SprintStory;
import com.pms.modules.project.mapper.SprintMapper;
import com.pms.modules.project.mapper.SprintStoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/sprints")
@RequiredArgsConstructor
public class SprintController {

    private final SprintMapper sprintMapper;
    private final SprintStoryMapper sprintStoryMapper;
    private final StoryMapper storyMapper;

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "wait", Set.of("doing", "closed"),
            "doing", Set.of("closed"),
            "closed", Set.of());

    @GetMapping
    public Result<List<Sprint>> list(@RequestParam Long projectId) {
        return Result.ok(sprintMapper.selectList(new LambdaQueryWrapper<Sprint>()
                .eq(Sprint::getProjectId, projectId).orderByDesc(Sprint::getId)));
    }

    @GetMapping("/options")
    public Result<List<Sprint>> options() {
        return Result.ok(sprintMapper.selectList(new LambdaQueryWrapper<Sprint>()
                .select(Sprint::getId, Sprint::getName, Sprint::getStatus, Sprint::getProjectId)
                .orderByAsc(Sprint::getId)));
    }

    @GetMapping("/{id}")
    public Result<Sprint> detail(@PathVariable Long id) {
        return Result.ok(sprintMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "sprint", action = "创建迭代", objectId = "#result.data")
    public Result<Long> create(@RequestBody Sprint sprint) {
        sprint.setId(null);
        sprint.setStatus("wait");
        sprintMapper.insert(sprint);
        return Result.ok(sprint.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Sprint sprint) {
        sprint.setId(id);
        sprint.setStatus(null);
        sprintMapper.updateById(sprint);
        return Result.ok();
    }

    /** 状态流转: action = start / close */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "sprint", action = "迭代状态流转", objectId = "#id")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Sprint sprint = sprintMapper.selectById(id);
        if (sprint == null) {
            throw new BizException("迭代不存在");
        }
        String target = "start".equals(body.get("action")) ? "doing" : "closed";
        if (!TRANSITIONS.getOrDefault(sprint.getStatus(), Set.of()).contains(target)) {
            throw new BizException(String.format("迭代状态不允许从 [%s] 流转到 [%s]", sprint.getStatus(), target));
        }
        Sprint update = new Sprint();
        update.setId(id);
        update.setStatus(target);
        sprintMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> delete(@PathVariable Long id) {
        sprintMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 迭代内需求 ----------

    @GetMapping("/{id}/stories")
    public Result<List<Story>> stories(@PathVariable Long id) {
        List<Long> storyIds = sprintStoryMapper.selectList(new LambdaQueryWrapper<SprintStory>()
                        .eq(SprintStory::getSprintId, id))
                .stream().map(SprintStory::getStoryId).toList();
        if (storyIds.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(storyMapper.selectList(new LambdaQueryWrapper<Story>()
                .in(Story::getId, storyIds)));
    }

    /** 把需求拉入迭代 */
    @PostMapping("/{id}/stories")
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "sprint", action = "需求拉入迭代", objectId = "#id")
    @Transactional
    public Result<Void> linkStories(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> storyIds = body.getOrDefault("storyIds", List.of());
        for (Long storyId : storyIds) {
            Long exists = sprintStoryMapper.selectCount(new LambdaQueryWrapper<SprintStory>()
                    .eq(SprintStory::getSprintId, id).eq(SprintStory::getStoryId, storyId));
            if (exists == 0) {
                sprintStoryMapper.insert(new SprintStory(null, id, storyId));
                // 需求进入研发阶段
                Story update = new Story();
                update.setId(storyId);
                update.setStage("developing");
                storyMapper.updateById(update);
            }
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}/stories/{storyId}")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> unlinkStory(@PathVariable Long id, @PathVariable Long storyId) {
        sprintStoryMapper.delete(new LambdaQueryWrapper<SprintStory>()
                .eq(SprintStory::getSprintId, id).eq(SprintStory::getStoryId, storyId));
        return Result.ok();
    }
}
