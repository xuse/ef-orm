package jef.database.support.accessor;

import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
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

	/**
	 * 当注册成功后，可以得到BeanAccessor提供的一个视图，记录了目前已经缓存的所有BeanAccessor。可用于判断一个Class是否已经缓存了对应的BeanAccessor。
	 * 该视图不可修改。
	 * @param cache
	 */
	void setBeanAccessorCache(Map<Class, BeanAccessor> cache);
}
