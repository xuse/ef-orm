package jef.database.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 这个注解可以加在一个实体上，作用是让实体在解析时可以无视增强警告。
 * 允许该实体可以不增强使用。
 * @deprecated use {@link EasyEntity}
 * @author Administrator
 */
@Target(TYPE) 
@Retention(RUNTIME)
public @interface NoForceEnhance {

}
