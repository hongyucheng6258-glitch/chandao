package com.pms.modules.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 迭代(Sprint): wait未开始 doing进行中 closed已关闭
 */
@Data
@TableName("sprint")
public class Sprint implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String name;

    private String goal;

    private LocalDate beginDate;

    private LocalDate endDate;

    /** wait/doing/closed */
    private String status;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
