package com.github.geequery.orm.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 用于定制数据初始化的行为
 * 
 * @author jiyi
 *
 */
@Target({ TYPE })
@Retention(RUNTIME)
public @interface InitializeData {
    /**
     * 配置一个资源文件名，记录了所有的初始化记录值 如果不指定，默认使用 classpath:/{classname}+全局扩展名 例如：配置为
     * /class1.txt 。
     * 
     * @return
     */
    String value() default "";

    /**
     * 禁用该表的自动初始化特性
     * 
     * @return
     */
    boolean enable() default true;

    /**
     * 自增键使用记录中的值，不使用数据库的自增编号
     * 
     * @return
     */
    boolean manualSequence() default true;

    /**
     * 数据文件的字符集 默认使用全局配置
     * 
     * @return
     */
    String charset() default "";

    /**
     * 确认资源文件必需存在，如果资源不存在将抛出异常
     */
    boolean ensureFileExists() default true;
}
