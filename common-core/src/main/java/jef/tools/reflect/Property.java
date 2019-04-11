package jef.tools.reflect;

import java.lang.reflect.Type;

/**
 * 描述一个属性。提供针对该属性的访问接口
 * @author Administrator
 *
 */
public interface Property {
	/**
	 * 得到名称
	 * @return
	 */
	String getName();
	/**
	 * 得到值
	 * @param obj
	 * @return
	 */
	Object get(Object obj);
	/**
	 * 设置值
	 * @param obj
	 * @param value
	 */
	void set(Object obj,Object value);
	/**
	 * 得到类型
	 * @return
	 */
	Class<?> getType();
	/**
	 * 得到泛型类型
	 * @return
	 */
	Type getGenericType();
}
