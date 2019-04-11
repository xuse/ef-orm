package jef.tools.string;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(FIELD) 
@Retention(RUNTIME)
public @interface RandomValue {
	/**
	 * 略过此字段
	 * @return
	 */
	boolean ignore() default false;
	/**
	 * 
	 * @return 生成文字最大长度
	 */
	int length() default 32;
	/**
	 *  
	 * @return 生成文字最小长度
	 */
	int minLength() default 1;
	
	/**
	 * @return 生成数据类型
	 */
	ValueType value() default ValueType.AUTO;
	/**
	 * 
	 * @return 生成数值最小值
	 */
	long numberMin() default 0L;

	/**
	 * 
	 * @return 生成数值最大值
	 */
	long numberMax() default 1000L;

	/**
	 * 
	 * @return 生成日期最小值
	 */
	String dateMin() default "";
	/**
	 * 
	 * @return 生成日期最大值
	 */
	String dateMax() default "";
	/**
	 * value={@link ValueType#OPTIONS}时，选项
	 * @return
	 */
	String[] options() default {};
	/**
	 * 当生成数组对象时，数组的长度
	 * @return
	 */
	int count() default 1;
	/**
	 * @return 当value=ValueType#RANGED_STRING 时，字符范围
	 */
	String characters() default "";
	/**
	 * @return 加在生成的随机文本后
	 */
	String suffix() default "";
	/**
	 * @return 加在生成的随机文本前
	 */
	String prefix() default "";
}
