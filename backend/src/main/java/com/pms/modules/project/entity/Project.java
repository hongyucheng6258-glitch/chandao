package com.pms.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 项目: wait未开始 doing进行中 suspended已暂停 closed已关闭
 */
@Data
@TableName("project")
public class Project implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Long productId;

    private Long ownerId;

    private LocalDate beginDate;

    private LocalDate endDate;

    private String description;

    /** wait/doing/suspended/closed */
    private String status;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
