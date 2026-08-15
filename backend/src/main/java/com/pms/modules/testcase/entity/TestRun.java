package com.pms.modules.testcase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("test_run")
public class TestRun implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long suiteId;

    private Long caseId;

    private Long executorId;

    /** pass通过 fail失败 blocked阻塞 null未执行 */
    private String result;

    private String remark;

    private Integer spentMinutes;

    private LocalDateTime executedTime;
}
