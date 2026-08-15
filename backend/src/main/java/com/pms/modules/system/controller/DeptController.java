package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysDept;
import com.pms.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/depts")
@RequiredArgsConstructor
public class DeptController {

    private final SysDeptMapper deptMapper;

    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSort));
        Map<Long, SysDept> map = new HashMap<>();
        all.forEach(d -> map.put(d.getId(), d));
        List<SysDept> roots = new ArrayList<>();
        for (SysDept d : all) {
            if (d.getParentId() == 0 || !map.containsKey(d.getParentId())) {
                roots.add(d);
            } else {
                SysDept parent = map.get(d.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(d);
            }
        }
        return Result.ok(roots);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<Void> create(@RequestBody SysDept dept) {
        dept.setId(null);
        deptMapper.insert(dept);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        deptMapper.updateById(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<Void> delete(@PathVariable Long id) {
        deptMapper.deleteById(id);
        return Result.ok();
    }
}
