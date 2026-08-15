package com.pms.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 需求(Story): draft草稿 -> active已激活 -> changed已变更 -> closed已关闭
 */
@Data
@TableName("story")
public class Story implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long planId;

    private String title;

    private String description;

    /** 优先级 1-4, 1最高 */
    private Integer priority;

    private BigDecimal estimate;

    /** draft/active/changed/closed */
    private String status;

    /** 阶段: wait未开始 developing研发中 testing测试中 released已发布 */
    private String stage;

    private Long assignedTo;

    private String closedReason;

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
