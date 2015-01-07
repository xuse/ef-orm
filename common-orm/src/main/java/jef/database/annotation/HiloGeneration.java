package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 
 *  这个注解总是和下面的注解一起使用，表示用自动自增序列值作为高位，再根据算法补充低位。
 *  <ul>
 *  <li>&#64;GeneratedValue(strategy=GenerationType.SEQUENCE)</li>
 * 	<li>&#64;GeneratedValue(strategy=GenerationType.TABLE)</li>
 * </ul>
 * <p>
 * hilo算法仅当采用sequence或者table方式生成主键时才会生效。
 * 
 * @see javax.persistence.GeneratedValue
 * @see javax.persistence.GenerationType
 * 
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface HiloGeneration {
	
	/**
	 * 默认false,根据jef.properties中的配置绝顶是否使用hilo算法。
	 * 如果设置为true，那么hilo算法总是生效。<br>
	 * 由于hilo是一种优化手段，因此建议always=false，然后在实际项目中根据jef.peoperties中的配置来决定要不要启用hilo。
	 * @return
	 */
	boolean always() default false;
	
	/**
	 * 低位的空间大小
	 */
	int maxLo() default 10;
}
