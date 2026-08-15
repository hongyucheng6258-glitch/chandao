package com.pms.modules.search.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.result.Result;
import com.pms.modules.bug.entity.Bug;
import com.pms.modules.bug.mapper.BugMapper;
import com.pms.modules.product.entity.Story;
import com.pms.modules.product.mapper.StoryMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final StoryMapper storyMapper;
    private final BugMapper bugMapper;
    private final TaskMapper taskMapper;

    /** 全局搜索: 跨需求/Bug/任务按关键字匹配标题, 每类最多 limit 条 */
    @GetMapping
    public Result<Map<String, Object>> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "10") int limit) {
        if (keyword == null || keyword.isBlank()) {
            return Result.ok(new HashMap<>());
        }
        int lim = Math.max(1, Math.min(limit, 50));
        Map<String, Object> result = new HashMap<>();
        result.put("stories", storyMapper.selectList(new LambdaQueryWrapper<Story>()
                .like(Story::getTitle, keyword)
                .last("LIMIT " + lim)
                .select(Story::getId, Story::getTitle, Story::getStatus, Story::getAssignedTo)));
        result.put("bugs", bugMapper.selectList(new LambdaQueryWrapper<Bug>()
                .like(Bug::getTitle, keyword)
                .last("LIMIT " + lim)
                .select(Bug::getId, Bug::getTitle, Bug::getStatus, Bug::getAssignedTo)));
        result.put("tasks", taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .like(Task::getName, keyword)
                .last("LIMIT " + lim)
                .select(Task::getId, Task::getName, Task::getStatus, Task::getAssignedTo)));
        return Result.ok(result);
    }
}
