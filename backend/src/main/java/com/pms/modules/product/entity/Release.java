package com.pms.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "`release`", autoResultMap = true)
public class Release implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    /** 版本号 如 v1.0.0 */
    private String name;

    private LocalDate releaseDate;

    private String description;

    /** 本次完成的需求ID清单(JSON) */
    @TableField(typeHandler = com.pms.common.handler.LongListTypeHandler.class)
    private List<Long> storyIds;

    /** 本次修复的BugID清单(JSON) */
    @TableField(typeHandler = com.pms.common.handler.LongListTypeHandler.class)
    private List<Long> bugIds;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
