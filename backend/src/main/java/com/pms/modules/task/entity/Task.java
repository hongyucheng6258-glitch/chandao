package com.pms.modules.task.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 任务: wait未开始 doing进行中 done已完成 pause已暂停 cancel已取消 closed已关闭
 */
@Data
@TableName("task")
public class Task implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sprintId;

    private Long storyId;

    private Long parentId;

    private String name;

    private String description;

    /** dev开发 test测试 design设计 study研究 */
    private String type;

    private Integer priority;

    private Long assignedTo;

    /** 预估/已消耗/剩余 工时 */
    private BigDecimal estimate;
    private BigDecimal consumed;
    @TableField("`left`")
    private BigDecimal left;

    private LocalDate deadline;

    /** wait/doing/done/pause/cancel/closed */
    private String status;

    private Long finishedBy;

    private LocalDateTime finishedTime;

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
