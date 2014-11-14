package jef.database.meta.extension;

import java.util.Map;

import jef.database.meta.ExtensionConfigFactory;
import jef.tools.reflect.Property;

/**
 * 扩展属性提供器，为了满足ORM-2.0版本中的扩展属性支持而设计。
 * @author jiyi
 *
 */
public interface BeanExtensionProvider {
	/**
	 * 判断一个类是不是动态属性类
	 * @param clz
	 * @return
	 */
	boolean isDynamicExtensionClass(Class<?> clz);
	
	/**
	 * 获得类的扩展名
	 * 仅限静态类
	 * @return
	 */
	String getStaticExtensionName(Class<?> clz);

	/**
	 * 如果是扩展属性类，那么返回所有的扩展属性
	 * @param type
	 * @return
	 */
	Map<String, Property> getExtensionProperties(Class<?> clz,String extensionName,ExtensionModificationListener listener);
	
	public ExtensionConfigFactory getExtensionFactory(Class<?> javaBean);
}
