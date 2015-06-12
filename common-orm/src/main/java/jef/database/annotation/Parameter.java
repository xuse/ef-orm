package jef.database.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({})
@Retention(RUNTIME)
public @interface Parameter {
	/**
	 * The parameter name.
	 */
	String name();

	/**
	 * The parameter value.
	 */
	String value();
}