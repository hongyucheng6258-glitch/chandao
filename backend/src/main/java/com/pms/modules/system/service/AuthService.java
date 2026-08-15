package com.pms.modules.system.service;

import com.pms.common.exception.BizException;
import com.pms.common.utils.JwtUtil;
import com.pms.framework.security.JwtAuthFilter;
import com.pms.framework.security.LoginUser;
import com.pms.modules.system.entity.SysPermission;
import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.mapper.SysPermissionMapper;
import com.pms.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper userMapper;
    private final SysPermissionMapper permissionMapper;
    private final com.pms.framework.security.UserDetailsServiceImpl userDetailsService;

    /** 登录: 返回 token */
    public Map<String, String> login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        SysUser user = loginUser.getUser();
        if (user.getStatus() != 1) {
            throw new BizException("账号已被停用");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        long expire = jwtUtil.getExpireMillis();
        redisTemplate.opsForValue().set(JwtAuthFilter.TOKEN_KEY_PREFIX + user.getId(), token, expire, TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set("login:user:" + user.getId(), loginUser, expire, TimeUnit.MILLISECONDS);
        return Map.of("token", token);
    }

    public void logout(Long userId) {
        redisTemplate.delete(JwtAuthFilter.TOKEN_KEY_PREFIX + userId);
        redisTemplate.delete("login:user:" + userId);
    }

    /** 当前用户信息: 基本信息 + 角色 + 权限点 + 菜单树 */
    public Map<String, Object> info(Long userId) {
        SysUser user = userMapper.selectById(userId);
        LoginUser loginUser = userDetailsService.buildLoginUser(user);
        List<SysPermission> menus = loginUser.getPerms().contains("*:*:*")
                ? permissionMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getPermType, 1, 2).orderByAsc(SysPermission::getSort))
                : permissionMapper.selectMenusByUserId(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("avatar", user.getAvatar());
        result.put("roles", loginUser.getRoles());
        result.put("perms", loginUser.getPerms());
        result.put("menus", buildTree(menus));
        return result;
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
