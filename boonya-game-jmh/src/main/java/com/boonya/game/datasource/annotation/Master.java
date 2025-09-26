package com.boonya.game.datasource.annotation;

import java.lang.annotation.*;

/**
 * 写操作注解，标记该方法使用主库
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {
    String value() default "";
}