package com.pms.modules.bug.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷: active激活 -> resolved已解决 -> closed已关闭; resolved 可打回 active
 */
@Data
@TableName("bug")
public class Bug implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long sprintId;

    private Long storyId;

    private Long taskId;

    private String title;

    /** 重现步骤 */
    private String steps;

    /** 严重程度 1致命 2严重 3一般 4轻微 */
    private Integer severity;

    private Integer priority;

    /** active/resolved/closed */
    private String status;

    private Long assignedTo;

    private Long resolvedBy;

    /** 解决方案 fixed/notbug/duplicate/bydesign/wontfix */
    private String resolution;

    private LocalDateTime resolvedTime;

    private Long closedBy;

    private LocalDateTime closedTime;

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
