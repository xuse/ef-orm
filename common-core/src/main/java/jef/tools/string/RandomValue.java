package jef.tools.string;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(FIELD	) 
@Retention(RUNTIME)
public @interface RandomValue {
	boolean ignore() default false;
	
	int length() default -1;

	ValueType value() default ValueType.AUTO;

	long numberMin() default 0L;

	long numberMax() default 1000L;

	String dateMin() default "";

	String dateMax() default "";
	
	String[] options() default {};
	
	int count() default 1;
	
	String characters() default "";
}
