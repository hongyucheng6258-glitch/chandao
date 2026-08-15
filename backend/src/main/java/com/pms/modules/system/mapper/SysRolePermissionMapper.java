package com.pms.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pms.modules.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    @Select("SELECT perm_id FROM sys_role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermIdsByRoleId(@Param("roleId") Long roleId);
}
