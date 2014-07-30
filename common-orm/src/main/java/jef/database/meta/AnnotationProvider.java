package jef.database.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

/**
 * 得到一个实体的配置信息
 * @author jiyi
 *
 */
public interface AnnotationProvider {
	/**
	 * 实体类
	 * @return
	 */
	String getType();

	/**
	 * 得到在类上的注解
	 * @param type
	 * @return
	 */
	<T extends Annotation> T getAnnotation(Class<T> type);

	/**
	 * 得到在属性上的注解
	 * @param field
	 * @param type
	 * @return
	 */
	<T extends Annotation> T getFieldAnnotation(Field field, Class<T> type);


}
