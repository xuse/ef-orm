package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 *  这个注解总是和下面的注解一起使用，表示用自动自增序列值作为高位，再根据算法补充低位。
 *  <li>&#64;GeneratedValue(strategy=GenerationType.SEQUENCE)</li>
 * 	<li>&#64;GeneratedValue(strategy=GenerationType.TABLE)</li>
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface HiloGeneration {
	
	/**
	 * 默认false,根据jef.properties中的配置绝顶是否使用hilo算法
	 * @return
	 */
	boolean always() default false;
	
	/**
	 * 低位的空间大小
	 */
	int maxLo() default 10;
}
