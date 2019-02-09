package com.github.geequery.springdata.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来描述多个查询条件的注解，代替Spring-data原生的 findBy......方法名过长的问题
 * 
 * 备注：目前不支持OR条件
 * 
 * @author jiyi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
@Documented
public @interface FindBy {
    /**
     * 多个查询字段和运算符
     * 
     * @return
     */
    Condition[] value();

    /**
     * 排序
     * 
     * @return
     */
    String orderBy() default "";

    /**
     * 多个条件之间是AND关系还是OR关系。
     * 
     * @return
     */
    Logic type() default Logic.AND;
}
