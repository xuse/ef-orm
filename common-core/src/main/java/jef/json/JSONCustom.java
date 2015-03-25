package jef.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface JSONCustom {
	/**
	 * 自定义序列化器实例，该序列化器中可以调用static public getInstance对象来得到序列化器实例
	 * 
	 * @return
	 * @since easyframe
	 */
	Class<?> serializer() default Void.class;

	/**
	 * 自定义反序列化器实例，该序列化器中可以调用static public getInstance对象来得到反序列化器实例
	 * 
	 * @return
	 * @since easyframe
	 */
	Class<?> deserializer() default Void.class;
}
