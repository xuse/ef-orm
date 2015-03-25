package jef.database.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 支持类似KV表的动态属性实现扩展字段功能
 * 
 * 用来描述表中的动态属性
 * @author jiyi
 *
 */
@Target(TYPE) 
@Retention(RUNTIME)
public @interface DynamicKeyValueExtension {
	/**
	 * 属性表名
	 * @return
	 */
	String table();
	/**
	 * 属性表中标记当前属性的列名
	 * @return
	 */
	String keyColumn() default "key";
	
	/**
	 * 属性表中，记录Value的值
	 * @return
	 */
	String valueColumn() default "value_text";
	
	/**
	 * 元数据定义的ID。将元数据以动态表的形式存入MetaHolder，按ID获取使用。
	 * @return
	 */
	String metadata() default "";

	
}
