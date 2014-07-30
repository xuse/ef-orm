package jef.database.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface DataSource {
	String name() default "";
	String url();
	String user() default "";
	String password() default "";
	String field() default "";
	String dirverClass() default "";
}
