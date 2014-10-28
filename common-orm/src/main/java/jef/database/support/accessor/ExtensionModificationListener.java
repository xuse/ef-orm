package jef.database.support.accessor;

import java.util.Map;

import jef.tools.reflect.Property;

/**
 * 扩展属性发生变化时的监听器
 * @author jiyi
 *
 */
public interface ExtensionModificationListener {
	public void setExtProperties(Map<String, Property> extProps);
}
