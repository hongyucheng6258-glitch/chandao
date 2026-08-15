package com.pms.modules.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product_plan")
public class ProductPlan implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String title;

    private LocalDate beginDate;

    private LocalDate endDate;

    private String goal;

    @TableLogic
    @JsonIgnore
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
