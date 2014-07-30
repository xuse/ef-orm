package jef.common.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ElementType.TYPE,ElementType.PARAMETER,ElementType.FIELD,ElementType.METHOD}) 
public @interface ObjectName {
	String value() default "";
}
