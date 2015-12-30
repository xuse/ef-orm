package jef.database.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * 当数据类型为int, long, short, char等基础类型时。如果Entity的修改记录了该值是设置过的，
 * 那么认为是有效值。<br>
 * 如果无记录，那么不等于UnsavedValue的值认为是有效值。
 * @author jiyi
 *
 */
@Target({ TYPE,FIELD })
@Retention(RUNTIME)
public @interface Comment {
	String value();
}
