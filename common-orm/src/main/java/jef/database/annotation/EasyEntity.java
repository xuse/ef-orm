package jef.database.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 用于描述一个实体的行为
 * @author jiyi
 *
 */
@Target(TYPE) 
@Retention(RUNTIME)
public @interface EasyEntity {
	/**
	 * 是否检查增强
	 * @return
	 */
	boolean checkEnhanced() default true;
	
	/**
	 * 当使用自动扫描实体类时，是否刷新数据库中的表
	 * @return
	 */
	boolean refresh() default true;
	
	/**
	 * 当使用自动扫描实体类时，是否新建数据库中的表
	 * @return
	 */
	boolean create() default true;
	
	/**
	 * 使用外连接
	 * @return
	 */
	boolean useOuterJoin() default true;
}
