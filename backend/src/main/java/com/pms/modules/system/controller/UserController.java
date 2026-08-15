package com.pms.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.exception.BizException;
import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.entity.SysUserRole;
import com.pms.modules.system.mapper.SysUserMapper;
import com.pms.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long deptId,
                                      @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                .eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByAsc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        page.getRecords().forEach(u -> u.setRoleIds(
                userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, u.getId()))
                        .stream().map(SysUserRole::getRoleId).toList()));
        return Result.ok(page);
    }

    /** 全量简单列表(下拉框用), 登录即可访问 */
    @GetMapping("/options")
    public Result<List<SysUser>> options() {
        return Result.ok(userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName)
                .orderByAsc(SysUser::getId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    @Transactional
    public Result<Void> create(@RequestBody SysUser user) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));
        if (exists > 0) {
            throw new BizException("账号已存在");
        }
        user.setId(null);
        user.setPassword(passwordEncoder.encode(
                user.getPassword() == null || user.getPassword().isBlank() ? "123456" : user.getPassword()));
        userMapper.insert(user);
        saveUserRoles(user.getId(), user.getRoleIds());
        return Result.ok();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    @Transactional
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        user.setUsername(null); // 账号不允许修改
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不修改密码
        }
        userMapper.updateById(user);
        if (user.getRoleIds() != null) {
            userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
            saveUserRoles(id, user.getRoleIds());
        }
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> delete(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null && "admin".equals(user.getUsername())) {
            throw new BizException("内置管理员不允许删除");
        }
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(body.get("status"));
        userMapper.updateById(update);
        return Result.ok();
    }

    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        roleIds.stream().distinct().forEach(roleId ->
                userRoleMapper.insert(new SysUserRole(null, userId, roleId)));
    }
}
