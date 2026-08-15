package com.pms.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解: 记录业务操作到 sys_action_log
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {

    /** 对象类型: story/task/bug/project... */
    String objectType();

    /** 动作描述: 创建/指派/解决... */
    String action();

    /** SpEL 表达式取对象ID, 如 "#id" 或 "#body.id" */
    String objectId() default "";
}
