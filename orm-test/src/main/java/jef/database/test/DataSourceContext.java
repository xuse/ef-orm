package jef.database.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 描述一个测试的数据源
 * @author Administrator
 *
 */
@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface DataSourceContext {
	DataSource[] value();
	boolean routing() default false;
}
