package com.pms.modules.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("sprint_burndown")
public class SprintBurndown implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sprintId;

    private LocalDate statDate;

    private BigDecimal leftHours;

    private Integer taskTotal;

    private Integer taskDone;
}
