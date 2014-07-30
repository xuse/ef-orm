package jef.database.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RUNTIME)
public @interface IgnoreOn {
	/**
	 * 在这些数据源的情况下跳过测试。
	 * @return
	 */
	String[] value() default {};
	
	/**
	 * 除了指定的这些数据源以外，跳过测试
	 * @return
	 */
	String[] allButExcept() default{};
}
