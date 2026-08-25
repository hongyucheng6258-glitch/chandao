package com.pms.modules.stats.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.bug.entity.Bug;
import com.pms.modules.bug.mapper.BugMapper;
import com.pms.modules.product.entity.Story;
import com.pms.modules.product.mapper.StoryMapper;
import com.pms.modules.project.entity.Project;
import com.pms.modules.project.mapper.ProjectMapper;
import com.pms.modules.task.entity.Task;
import com.pms.modules.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作台(我的地盘): 我的任务/Bug/需求聚合
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TaskMapper taskMapper;
    private final BugMapper bugMapper;
    private final StoryMapper storyMapper;
    private final ProjectMapper projectMapper;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Long userId = SecurityUtil.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("myTaskCount", taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getAssignedTo, userId).in(Task::getStatus, "wait", "doing", "pause")));
        data.put("myBugCount", bugMapper.selectCount(new LambdaQueryWrapper<Bug>()
                .eq(Bug::getAssignedTo, userId).in(Bug::getStatus, "active", "resolved")));
        data.put("myStoryCount", storyMapper.selectCount(new LambdaQueryWrapper<Story>()
                .eq(Story::getAssignedTo, userId).in(Story::getStatus, "active", "changed")));
        data.put("doingProjectCount", projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, "doing")));
        return Result.ok(data);
    }

    /** 我的待办任务列表 */
    @GetMapping("/my-tasks")
    public Result<Object> myTasks() {
        Long userId = SecurityUtil.getUserId();
        return Result.ok(taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getAssignedTo, userId)
                .in(Task::getStatus, "wait", "doing", "pause")
                .orderByAsc(Task::getDeadline).last("LIMIT 20")));
    }

    /** 指派给我的待处理Bug(只显示激活状态, 已解决/已关闭的不再展示) */
    @GetMapping("/my-bugs")
    public Result<Object> myBugs() {
        Long userId = SecurityUtil.getUserId();
        return Result.ok(bugMapper.selectList(new LambdaQueryWrapper<Bug>()
                .eq(Bug::getAssignedTo, userId)
                .eq(Bug::getStatus, "active")
                .orderByAsc(Bug::getSeverity).last("LIMIT 20")));
    }
}
