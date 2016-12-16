package com.github.geequery.springdata.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来描述参数为某个指定的值的时候，参数不设置（忽略）不作为查询条件或更新字段
 * 配合GeeQuery中的NativeQuery子句自动省略功能。
 */
@Target({ java.lang.annotation.ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreIf {
	ParamIs value() default ParamIs.Null;
}
