package jef.database.meta;

import jef.database.meta.AnnotationProvider.ClassAnnotationProvider;

/**
 * 接口。定义元模型的加载方式.
 * @author jiyi
 *
 */
public interface MetadataConfiguration {
	
	/**
	 * 获得指定类型的注解提供器
	 * @param clz Entity类 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	ClassAnnotationProvider getAnnotations(Class clz);
}
