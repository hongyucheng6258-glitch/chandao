package com.pms.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_suite")
public class TestSuite implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long productId;

    private Long sprintId;

    /** planned待执行 running执行中 done已完成 */
    private String status;

    private String remark;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
