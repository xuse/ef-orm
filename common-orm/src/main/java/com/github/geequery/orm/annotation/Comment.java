package com.github.geequery.orm.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 用来定义数据库中的字段或表注释。
 * 当建表时，优先从classpath上查找源码，然后使用源码中的注释。
 * 如果无法从源码中获得注释，那么会使用Annotation中的注释
 * @author jiyi
 *
 */
@Target({ TYPE,FIELD })
@Retention(RUNTIME)
public @interface Comment {
	String value();
}
