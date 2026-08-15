package com.pms.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志/动态(参考禅道 action 模块)
 */
@Data
@TableName("sys_action_log")
public class SysActionLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String objectType;

    private Long objectId;

    private String action;

    private Long actorId;

    private String actorName;

    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
