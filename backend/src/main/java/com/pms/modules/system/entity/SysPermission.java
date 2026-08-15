package com.pms.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("sys_permission")
public class SysPermission implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String permName;

    /** 1目录 2菜单 3按钮 */
    private Integer permType;

    private String permKey;

    private String path;

    private String icon;

    private Integer sort;

    @TableLogic
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer deleted;

    @TableField(exist = false)
    private List<SysPermission> children;
}
