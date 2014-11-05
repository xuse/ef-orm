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
	/**
	 * Map的key值
	 * @return
	 */
	String keyOfMap();

	/**
	 * Map的Value值，默认为“”，表示目标对象本身，如果非空则表示目标对象的某字段值作为Map的Value
	 * 
	 * @return
	 */
	String valueOfMap() default "";
}
