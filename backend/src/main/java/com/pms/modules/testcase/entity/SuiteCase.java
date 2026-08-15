package com.pms.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("suite_case")
public class SuiteCase implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long suiteId;

    private Long caseId;
}
