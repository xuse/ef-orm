package jef.accelerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 用于在geeQuery的反射框架中覆盖field的原始名称，改为一个新的字段名
 * @author jiyi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD})
public @interface GeeField {
	/**
	 * 字段名称
	 * @return
	 */
	String name() default "";
	/**
	 * 忽略此字段
	 * @return
	 */
    boolean ignore() default false;
}
