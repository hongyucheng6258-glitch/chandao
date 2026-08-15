package com.pms.framework.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.modules.system.entity.SysPermission;
import com.pms.modules.system.entity.SysRole;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.mapper.SysPermissionMapper;
import com.pms.modules.system.mapper.SysRoleMapper;
import com.pms.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return buildLoginUser(user);
    }

    public LoginUser buildLoginUser(SysUser user) {
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        List<SysPermission> perms = permissionMapper.selectByUserId(user.getId());
        boolean isAdmin = roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
        // 管理员: 展开为全部权限点(同时保留 *:*:* 标记), 保证 @PreAuthorize 逐键校验通过
        List<String> permKeys = isAdmin
                ? permissionMapper.selectList(null).stream()
                        .map(SysPermission::getPermKey).filter(k -> k != null && !k.isBlank()).distinct()
                        .collect(java.util.stream.Collectors.toList())
                : perms.stream().map(SysPermission::getPermKey).filter(k -> k != null && !k.isBlank()).distinct()
                        .collect(java.util.stream.Collectors.toList());
        if (isAdmin) {
            permKeys.add("*:*:*");
        }
        return new LoginUser(user, permKeys,
                roles.stream().map(SysRole::getRoleCode).toList());
    }
}
