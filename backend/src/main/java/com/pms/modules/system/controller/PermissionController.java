package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysPermission;
import com.pms.modules.system.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/perms")
@RequiredArgsConstructor
public class PermissionController {

    private final SysPermissionMapper permissionMapper;

    /** 权限树(含按钮), 角色授权弹窗和权限管理页共用 */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:perm:list') or hasAuthority('system:role:list')")
    public Result<List<SysPermission>> tree() {
        List<SysPermission> all = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>().orderByAsc(SysPermission::getSort));
        return Result.ok(buildTree(all));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:perm:list')")
    public Result<Void> create(@RequestBody SysPermission perm) {
        perm.setId(null);
        permissionMapper.insert(perm);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:perm:list')")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysPermission perm) {
        perm.setId(id);
        permissionMapper.updateById(perm);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:perm:list')")
    public Result<Void> delete(@PathVariable Long id) {
        permissionMapper.deleteById(id);
        permissionMapper.delete(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, id));
        return Result.ok();
    }

    private List<SysPermission> buildTree(List<SysPermission> perms) {
        Map<Long, SysPermission> map = new HashMap<>();
        perms.forEach(p -> map.put(p.getId(), p));
        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission p : perms) {
            if (p.getParentId() == 0 || !map.containsKey(p.getParentId())) {
                roots.add(p);
            } else {
                SysPermission parent = map.get(p.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(p);
            }
        }
        return roots;
    }
}
