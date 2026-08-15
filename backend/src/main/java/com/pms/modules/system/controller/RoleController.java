package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysRole;
import com.pms.modules.system.entity.SysRolePermission;
import com.pms.modules.system.mapper.SysRoleMapper;
import com.pms.modules.system.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Page<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .like(keyword != null && !keyword.isBlank(), SysRole::getRoleName, keyword)
                .orderByAsc(SysRole::getId);
        Page<SysRole> page = roleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(r -> r.setPermIds(rolePermissionMapper.selectPermIdsByRoleId(r.getId())));
        return Result.ok(page);
    }

    @GetMapping("/options")
    public Result<List<SysRole>> options() {
        return Result.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    @Transactional
    public Result<Void> create(@RequestBody SysRole role) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode()));
        if (exists > 0) {
            throw new BizException("角色编码已存在");
        }
        role.setId(null);
        roleMapper.insert(role);
        saveRolePerms(role.getId(), role.getPermIds());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleMapper.updateById(role);
        if (role.getPermIds() != null) {
            rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                    .eq(SysRolePermission::getRoleId, id));
            saveRolePerms(id, role.getPermIds());
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Void> delete(@PathVariable Long id) {
        if (id == 1L) {
            throw new BizException("内置管理员角色不允许删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        return Result.ok();
    }

    private void saveRolePerms(Long roleId, List<Long> permIds) {
        if (permIds == null) {
            return;
        }
        permIds.stream().distinct().forEach(permId ->
                rolePermissionMapper.insert(new SysRolePermission(null, roleId, permId)));
    }
}
