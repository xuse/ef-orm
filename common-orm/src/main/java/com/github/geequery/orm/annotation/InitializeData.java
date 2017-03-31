package com.github.geequery.orm.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ TYPE })
@Retention(RUNTIME)
public @interface InitializeData {
	/**
	 * 配置一个资源文件名，记录了所有的初始化记录值
	 * 如果不指定，默认使用 classpath:/{classname}.csv
	 * 1、支持SQL语句
	 * 2、支持CSV格式
	 * @return
	 */
	String value() default "";
	
	
	/**
	 * 禁用该表的自动初始化特性
	 * @return
	 */
	boolean enable() default true;
}
