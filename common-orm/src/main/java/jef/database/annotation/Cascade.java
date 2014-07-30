package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 支持用Map来表示一对多的映射关系
 * @author jiyi
 *
 */
@Target(FIELD) 
@Retention(RUNTIME)
public @interface Cascade {
	String keyOfMap();
}
