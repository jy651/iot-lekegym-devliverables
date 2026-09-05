package com.lehaha.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeviceToken {
    /** 从哪个 Header 取 token，默认 "Device-Token" */
    String header() default "Device-Token";

    /** true 表示必须携带，缺失则报错；false 表示缺失时跳过校验 */
    boolean required() default true;
}
