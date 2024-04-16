package com.ruoyi.web.controller.admin.annotation;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface UserSyncLock {

    String key();


    /**
     * 单位：秒
     * @return
     */
    int time() default 120;

}