package com.pms.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pms.modules.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("SELECT DISTINCT p.* FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.deleted = 0 ORDER BY p.sort")
    List<SysPermission> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT DISTINCT p.* FROM sys_permission p " +
            "JOIN sys_role_permission rp ON p.id = rp.perm_id " +
            "JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.perm_type IN (1,2) ORDER BY p.sort")
    List<SysPermission> selectMenusByUserId(@Param("userId") Long userId);
}
