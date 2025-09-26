package com.boonya.game.datasource.annotation;

import java.lang.annotation.*;

// 使用注解实现读写分离
/**
 * 读操作注解，标记该方法使用从库
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnly {
    String value() default "";
}
