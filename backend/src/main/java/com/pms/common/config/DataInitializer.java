package com.pms.common.config;

import com.pms.modules.system.entity.SysUser;
import com.pms.modules.system.entity.SysUserRole;
import com.pms.modules.system.mapper.SysUserMapper;
import com.pms.modules.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化: 创建 admin/123456 并绑定管理员角色
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("123456"));
        admin.setRealName("系统管理员");
        admin.setEmail("admin@pms.local");
        admin.setDeptId(1L);
        admin.setStatus(1);
        userMapper.insert(admin);
        userRoleMapper.insert(new SysUserRole(null, admin.getId(), 1L));
        log.info("初始化账号完成: admin / 123456");
    }
}
