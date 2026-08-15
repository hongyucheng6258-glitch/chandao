package com.pms.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_case")
public class TestCase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long storyId;

    private String title;

    private String precondition;

    /** 步骤与预期(JSON数组) */
    private String steps;

    /** feature功能 ui界面 performance性能 */
    private String type;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
