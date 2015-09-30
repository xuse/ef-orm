package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 关于连接（外键）的描述,可以设置为全外连接，还可以自定义join过滤条件
 * @author jiyi
 *
 */
@Target(FIELD) 
@Retention(RUNTIME)
public @interface JoinDescription {
	/**
	 * 连接属性
	 * @return
	 */
	JoinType type() default JoinType.LEFT;
	
	/**
	 * 可以设定连接ON条件中的过滤条件
	 * <br>
	 * 1.10版本后，可以使用 this$ that$来指代当前表的字段和右侧表的字段。例如——
	 * this$dictType+'.GENDER'=that$type 
	 * 但这种用法限制要求外连接查询时使用。仅供特例使用，一般用户请勿使用此功能。
	 * @return
	 */
	String filterCondition() default "";
	
	/**
	 * 当对多连接时，限制结果记录数最大值
	 * @return
	 */
	int maxRows() default 0;
}
