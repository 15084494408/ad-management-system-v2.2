package com.enterprise.ad.common.annotation;

import java.lang.annotation.*;

/**
 * ★ 修复 P3-21: 操作日志注解
 * 标注在 Controller 方法上，自动记录操作日志到数据库
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作描述 */
    String value() default "";

    /** 操作模块 */
    String module() default "";

    /** 是否记录请求参数 */
    boolean recordParams() default true;
}
