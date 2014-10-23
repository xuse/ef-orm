package jef.database.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 动态资源的真表实现
 * 
 * 用来描述表中的动态属性
 * @author jiyi
 *
 */
@Target(TYPE) 
@Retention(RUNTIME)
public @interface DynamicTable {
	String resourceTypePrefix() default "";
	
	/**
	 * 元数据定义的ID。资源类型将作为元数据进行管理。
	 * @return
	 */
	String resourceTypeField();
}
