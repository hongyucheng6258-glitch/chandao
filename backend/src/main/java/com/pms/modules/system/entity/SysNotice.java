package com.pms.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SysNotice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    /** 0未读 1已读 */
    private Integer readFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
