package jef.database.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * 得到一个实体的配置信息
 * 
 * @author jiyi
 * 
 */
public interface AnnotationProvider {
	/**
	 * 得到在类上的注解
	 * 
	 * @param type
	 * @return
	 */
	<T extends Annotation> T getAnnotation(Class<T> type);

	/**
	 * 得到对象的名称
	 * 
	 * @return
	 */
	String getName();

	public interface ClassAnnotationProvider extends AnnotationProvider {
		/**
		 * 得到在属性上的注解
		 * 
		 * @param field
		 * @param type
		 * @return
		 */
		FieldAnnotationProvider forField(Field field);

	}
		
	public interface FieldAnnotationProvider extends AnnotationProvider{

		Class<?> getDeclaringClass();

		Type getGenericType();

		Class<?> getType();
	}
}
