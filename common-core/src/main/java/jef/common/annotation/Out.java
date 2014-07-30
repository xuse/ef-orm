package jef.common.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation，描述出参
 * @author Administrator
 *
 */
@Retention(RUNTIME)
@Target(PARAMETER) 
public @interface Out {

}
