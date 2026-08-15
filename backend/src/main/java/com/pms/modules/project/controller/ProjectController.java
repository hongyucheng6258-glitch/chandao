package com.pms.modules.project.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.project.entity.Project;
import com.pms.modules.project.entity.ProjectMember;
import com.pms.modules.project.mapper.ProjectMapper;
import com.pms.modules.project.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "wait", Set.of("doing", "closed"),
            "doing", Set.of("suspended", "closed"),
            "suspended", Set.of("doing", "closed"),
            "closed", Set.of());

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('project:list')")
    public Result<Page<Project>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .like(keyword != null && !keyword.isBlank(), Project::getName, keyword)
                .eq(status != null && !status.isBlank(), Project::getStatus, status)
                .orderByDesc(Project::getId);
        return Result.ok(projectMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/options")
    public Result<List<Project>> options() {
        return Result.ok(projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .ne(Project::getStatus, "closed").orderByDesc(Project::getId)));
    }

    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        return Result.ok(projectMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "project", action = "创建项目", objectId = "#result.data")
    @Transactional
    public Result<Long> create(@RequestBody Project project) {
        project.setId(null);
        project.setStatus("wait");
        projectMapper.insert(project);
        if (project.getOwnerId() != null) {
            memberMapper.insert(new ProjectMember() {{
                setProjectId(project.getId());
                setUserId(project.getOwnerId());
                setRole("pm");
            }});
        }
        return Result.ok(project.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "project", action = "编辑项目", objectId = "#id")
    public Result<Void> update(@PathVariable Long id, @RequestBody Project project) {
        project.setId(id);
        project.setStatus(null);
        projectMapper.updateById(project);
        return Result.ok();
    }

    /** 状态流转: action = start / suspend / resume / close */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('project:add')")
    @LogOperation(objectType = "project", action = "项目状态流转", objectId = "#id")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new BizException("项目不存在");
        }
        String target = switch (body.get("action")) {
            case "start", "resume" -> "doing";
            case "suspend" -> "suspended";
            case "close" -> "closed";
            default -> throw new BizException("未知操作");
        };
        if (!TRANSITIONS.getOrDefault(project.getStatus(), Set.of()).contains(target)) {
            throw new BizException(String.format("项目状态不允许从 [%s] 流转到 [%s]", project.getStatus(), target));
        }
        Project update = new Project();
        update.setId(id);
        update.setStatus(target);
        projectMapper.updateById(update);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> delete(@PathVariable Long id) {
        projectMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 项目成员 ----------

    @GetMapping("/{id}/members")
    public Result<List<ProjectMember>> members(@PathVariable Long id) {
        return Result.ok(memberMapper.selectList(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, id)));
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> addMember(@PathVariable Long id, @RequestBody ProjectMember member) {
        Long exists = memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, id).eq(ProjectMember::getUserId, member.getUserId()));
        if (exists > 0) {
            throw new BizException("该成员已在项目中");
        }
        member.setId(null);
        member.setProjectId(id);
        memberMapper.insert(member);
        return Result.ok();
    }

    @DeleteMapping("/members/{memberId}")
    @PreAuthorize("hasAuthority('project:add')")
    public Result<Void> removeMember(@PathVariable Long memberId) {
        memberMapper.deleteById(memberId);
        return Result.ok();
    }
}
