/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.management.ReflectionException;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;

public final class BeanWrapperImpl extends BeanWrapper {
	private static final int MAX_ENTRIES = 200;
	
	private static Map<Class<?>, Map<String, PropertyHolder>> cache = new LinkedHashMap<Class<?>, Map<String, PropertyHolder>>() {
		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(Entry<Class<?>, Map<String, PropertyHolder>> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

	
	@Override
	public String toString() {
		return obj.toString();
	}

	public <T extends Annotation> T getAnnotationOnField(String name, Class<T> clz) {
		PropertyHolder pp = properties.get(name);
		Assert.notNull(pp, name + " is not a property of class " + this.getClassName());
		return pp.getFieldAnnotation(clz);
	}

	public <T extends Annotation> T getAnnotationOnGetter(String name, Class<T> clz) {
		PropertyHolder pp = properties.get(name);
		Assert.notNull(pp, name + " is not a property of class " + this.getClassName());
		return pp.getGetterAnnotation(clz);
	}

	public <T extends Annotation> T getAnnotationOnSetter(String name, Class<T> clz) {
		PropertyHolder pp = properties.get(name);
		Assert.notNull(pp, name + " is not a property of class " + this.getClassName());
		return pp.getSetterAnnotation(clz);
	}

	public Object getWrapped() {
		return obj;
	}

	protected Object obj;
	private ClassWrapper clz;
	private Map<String, PropertyHolder> properties = null;

	public BeanWrapperImpl(Object obj) {
		this(obj, false);
	}

	/**
	 * 构造对象
	 * <p>
	 * Title:
	 * </p>
	 * <p>
	 * Description:
	 * </p>
	 * 
	 * @param obj
	 * @param fieldBase
	 *            是否基于字段来分析属性。如果为是，则不论是否有setter和getter，返回全部field
	 */
	public BeanWrapperImpl(Object obj, boolean fieldBase) {
		super(obj);
		this.obj = obj;
		Class<?> c = obj.getClass();
		this.clz=new ClassWrapper(c);
		if (cache.containsKey(c)) {
			properties = cache.get(c);
		} else {
			properties = init(clz, fieldBase);
			cache.put(c, properties);
		}
	}

	private static Map<String, PropertyHolder> init(ClassWrapper cls, boolean fieldBase) {
		List<Class<?>> supers = new ArrayList<Class<?>>();
		Class<?> c=cls.getWrappered();
		while (c != Object.class){
			supers.add(c);
			c = c.getSuperclass();	
		};
		Map<String, PropertyHolder> result = new HashMap<String, PropertyHolder>();
		for (int i = supers.size(); i > 0; i--) {
			result.putAll(initByClass(cls,supers.get(i - 1), fieldBase));
		}
		return result;
	}

	private static Map<String, PropertyHolder> initByClass(ClassWrapper cw,Class<?> c, boolean fieldBase) {
		Map<String, PropertyHolder> myMap = new HashMap<String, PropertyHolder>();
		for (Field field : c.getDeclaredFields()) {
			int mod = field.getModifiers();
			if (Modifier.isStatic(mod) || Modifier.isNative(mod)) {
				continue;
			}
			FieldEx fex=new FieldEx(field,cw);
			// 由于这里是根据field来查找getter 和 setter 的，对于非标准bean可能会出问题。
			MethodEx getter = BeanUtils.getGetter(fex);
			MethodEx setter = BeanUtils.getSetter(fex);
			if (setter != null || getter != null || fieldBase) {
				if(!Modifier.isPublic(mod)){
					field.setAccessible(true);
				}
				PropertyHolder holder=new PropertyHolder(getter, setter, fex, field.getName());
				myMap.put(field.getName(), holder);
			}
		}
		return myMap;
	}

	/**
	 * 是否属性
	 */
	public boolean isProperty(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		return pp != null;
	}

	/**
	 * 判断属性可读？
	 */
	public boolean isReadableProperty(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)
			return false;
		return pp.getReadMethod() != null;
	}

	/**
	 * 判断属性可否写
	 */
	public boolean isWritableProperty(String fieldName) {
		if (fieldName == null)
			return false;
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)
			return false;
		return pp.getWriteMethod() != null;
	}

	public Type getPropertyType(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)
			throw new NoSuchElementException("Cound not find field ["+fieldName+"] in "+this.obj.getClass().getName());
		return pp.getGenericType();
	}
	
	@Override
	public Class<?> getPropertyRawType(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)
			throw new NoSuchElementException("Cound not find field ["+fieldName+"] in "+this.obj.getClass().getName());
		return pp.getType();
	}
	

	/**
	 * 获得属性值
	 */
	public Object getPropertyValue(String name) {
		PropertyHolder pp = properties.get(name);
		if (pp == null)
			throw new NullPointerException(name + " is not a property of class " + this.getClassName());
		Method m = pp.getReadMethod();
		if (m == null)
			throw new NullPointerException();
		try {
			return m.invoke(obj, new Object[] {});
		} catch (IllegalArgumentException e) {
			LogUtil.exception(e);
			return null;
		} catch (IllegalAccessException e) {
			LogUtil.exception(e);
			return null;
		} catch (InvocationTargetException e) {
			LogUtil.exception(e);
			return null;
		}
	}

	/**
	 * 设置属性
	 */
	public void setPropertyValue(String fieldName, Object newValue) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)
			throw new NullPointerException("Can not find property '" + fieldName + "' in bean " + obj.getClass().getName());
		Method m = pp.getWriteMethod();
		if (m == null)
			throw new NullPointerException("Can not find set method '" + fieldName + "' in bean " + obj.getClass().getName());
		try {
			m.invoke(obj, new Object[] { newValue });
		} catch (IllegalArgumentException e) {
			String detail=StringUtils.exceptionStack(e, "jef","com");
			String message=StringUtils.concat("IllegalArgumentException while setting property", 
					fieldName," in bean ",obj.getClass().getName()," value:", (newValue == null ? "null" : newValue.getClass().getName()),"\ndetail:",detail);
			LogUtil.error(message);
		} catch (IllegalAccessException e) {
			String detail=StringUtils.exceptionStack(e, "jef","com");
			String message=StringUtils.concat("IllegalAccessException while setting property", 
					fieldName," in bean ",obj.getClass().getName(),"\ndetail:",detail);
			LogUtil.error(message);
		} catch (InvocationTargetException e) {
			String detail=StringUtils.exceptionStack(e.getTargetException(), "jef","com");
			String message=StringUtils.concat("InvocationTargetException while setting property", 
					fieldName," in bean ",obj.getClass().getName(),"\ndetail:",detail);
			LogUtil.error(message);
		}
	}

	/**
	 * 查找全部可读写的属性
	 */
	public PropertyHolder[] getRwPropertyDescriptors() {
		ArrayList<PropertyHolder> list = new ArrayList<PropertyHolder>();
		for (PropertyHolder pp : properties.values()) {
			if (pp.getReadMethod() != null && pp.getWriteMethod() != null) {
				list.add(pp);
			}
		}
		return list.toArray(new PropertyHolder[list.size()]);
	}

	/**
	 * 查找属性名称（忽略大小写）
	 */
	public String findPropertyIgnoreCase(String string) {
		if (properties.containsKey(string))
			return string;
		for (String field : properties.keySet()) {
			if (field.equalsIgnoreCase(string))
				return field;
		}
		return null;
	}

	/**
	 * 通过反射执行一个方法，不论该方法是否私有或受保护
	 * 
	 * @param methodName
	 * @param param
	 * @return
	 * @throws ReflectionException
	 */
	public Object invokeMethod(String methodName, Object... param) throws ReflectionException {
		try {
			Class<?>[] cs = new Class[param.length];
			for (int i = 0; i < param.length; i++) {
				if (param[i] == null) {
					cs[i] = Object.class;
				} else {
					cs[i] = param[i].getClass();
				}
			}
			MethodEx me = BeanUtils.getCompatibleMethod(obj.getClass(), methodName, cs);
			return me.invoke(obj, param);
		} catch (IllegalArgumentException e) {
			throw new ReflectionException(e);
		} catch (IllegalAccessException e) {
			throw new ReflectionException(e);
		} catch (InvocationTargetException e) {
			throw new ReflectionException(e);
		}
	}

//	/**
//	 * 获取某个field的类型
//	 */
//	public Class<?> getFieldType(String fieldName) {
//		PropertyHolder pp = properties.get(fieldName);
//		if (pp == null)
//			throw new NullPointerException(StringUtils.concat("Can not find PropertyDescriptor in ",getClassName()," for field: ", fieldName));
//		return GenericUtils.getRawClass(pp.getPropertyType());
//	}

	public String getClassName() {
		return obj.getClass().getName();
	}

	/**
	 * 更为复杂的获取属性方法
	 * 
	 * @param name
	 *            : 支持复杂属性递归获取。 例如： friend.name.first
	 *            取bean的friend属性，再取其name属性，再取其first属性 例如： friends[2].name
	 *            取bean的friends属性，然后取这个集合的第3个元素，再取其name属性 例如： children[-2]
	 *            取bean的children属性，然后取这个集合的倒数第2个元素
	 * @throws ReflectionException
	 */
	public Object getNestedProperty(String name) {
		int n = name.indexOf('.');
		if (n > -1) {
			String field = name.substring(0, n);
			name = name.substring(n + 1);
			Object tmp = null;
			try {
				tmp = getIndexedObject(field, false);
			} catch (ReflectionException e) {
				LogUtil.exception(e);
			}
			if (tmp == null)
				return null;
			return new BeanWrapperImpl(tmp).getNestedProperty(name);
		} else {
			try {
				return getIndexedObject(name, false);
			} catch (ReflectionException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @throws ReflectionException
	 * 
	 */
	public void setNestedProperty(String name, Object value) throws ReflectionException {
		int n = name.indexOf('.');
		if (n > -1) {
			String field = name.substring(0, n);
			name = name.substring(n + 1);
			Object tmp = getIndexedObject(field, true);
			new BeanWrapperImpl(tmp).setNestedProperty(name, value);
		} else {
			setIndexedPropertyValue(name, value);
		}
	}

	/**
	 * 设置字段属性，允许使用[index]表示第几个元素
	 */
	@SuppressWarnings("rawtypes")
	public void setIndexedPropertyValue(String field, Object value) {
		int x = field.indexOf('[');
		int y = field.indexOf(']');
		if (x > -1 && y > -1 && y > x) {
			Entry<String, Integer> info = toKeyAndIndex(field, x, y);
			int index = info.getValue();
			ClassWrapper c = new ClassWrapper(getPropertyType(info.getKey()));
			Object o = getPropertyValue(info.getKey());
			if (o == null) {
				if (c.isArray()) {
					ClassWrapper cmpType=new ClassWrapper(c.getComponentType());
					Object array = Array.newInstance(cmpType.getWrappered(), 0);
					// 修正数据类型
					value = ClassWrapper.toProperType(value, cmpType, null);
					array = ArrayUtils.setValueAndExpandArray(array, index, value);
					this.setPropertyValue(info.getKey(), array);
				} else if (List.class.isAssignableFrom(c.getWrappered())) {
					List list = new ArrayList();
					CollectionUtil.listSetAndExpand(list, index, value);
					this.setPropertyValue(info.getKey(), list);
				}
			} else {
				if (c.isArray()) {
					ClassWrapper cmpType=new ClassWrapper(c.getComponentType());
					value = ClassWrapper.toProperType(value, cmpType, null);
					Object array = ArrayUtils.setValueAndExpandArray(o, index, value);
					if (array != value)
						this.setPropertyValue(info.getKey(), array);
				} else if (List.class.isAssignableFrom(c.getWrappered())) {
					ClassWrapper cmpType=new ClassWrapper(c.getComponentType());
					Object old = CollectionUtil.findElementInstance(o);
					value = ClassWrapper.toProperType(value, cmpType, old);
					CollectionUtil.listSetAndExpand((List) value, index, value);
				}
			}
		} else {
			Type c = this.getPropertyType(field);
			Object old = this.isReadableProperty(field) ? this.getPropertyValue(field) : null;
			value = ClassWrapper.toProperType(value, new ClassWrapper(c), old);
			this.setPropertyValue(field, value);
		}
	}

	public Object getIndexedObject(String field) {
		try {
			return getIndexedObject(field, false);
		} catch (ReflectionException e) {
			LogUtil.exception(e);
			return null;
		}
	}

	/**
	 * 获取字段属性，允许使用[index]表示第几个元素
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings("rawtypes")
	private Object getIndexedObject(String field, boolean create) throws ReflectionException {
		int x = field.indexOf('[');
		int y = field.indexOf(']');
		if (x > -1 && y > -1 && y > x) {
			Entry<String, Integer> info = toKeyAndIndex(field, x, y);
			int index = info.getValue();
			Object o = getPropertyValue(info.getKey());
			if (o == null) {
				throw new NullPointerException("value of " + info.getKey() + " is null.");
			}
			if (o.getClass().isArray()) {
				if (create && ArrayUtils.isIndexValid(o, index) == false) {
					Object r = ArrayUtils.toFixLength(o, (index < 0 ? -index : index + 1));
					if (r != o)
						setPropertyValue(info.getKey(), r);// 扩展数组
					Object instance = CollectionUtil.createElementByElement(r);
					if (instance != null)
						ArrayUtils.set(r, index, instance);
				}
				return ArrayUtils.get(o, index);
			} else if (o instanceof List) {
				if (create && CollectionUtil.isIndexValid(o, index) == false) {
					CollectionUtil.toFixedSize((List) o, (index < 0 ? -index : index + 1));
					Object instance = CollectionUtil.createElementByElement(o);
					if (instance != null)
						CollectionUtil.listSet((List) o, index, instance);
				}
				List<?> l = (List<?>) o;
				return index >= 0 ? l.get(index) : l.get(l.size() + index);
			} else {
				throw new IllegalArgumentException("Not a Indexed Object(" + field + "):" + o.getClass().getName());
			}
		} else {
			Object obj = getPropertyValue(field);
			if (obj == null && create) {
				ClassWrapper c = new ClassWrapper(getPropertyType(field));
				try {
					obj = c.newInstance();
				} catch (Exception e) {
					throw new ReflectionException(e);
				}
				setPropertyValue(field, obj);
			}
			return obj;
		}
	}

	private static jef.common.Entry<String, Integer> toKeyAndIndex(String name, int x, int y) {
		try {
			return new jef.common.Entry<String, Integer>(name.substring(0, x), Integer.parseInt(name.substring(x + 1, y)));
		} catch (NumberFormatException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Collection<String> getPropertyNames() {
		return this.properties.keySet();
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		ArrayList<String> list = new ArrayList<String>();
		for (PropertyHolder pp : properties.values()) {
			if (pp.getReadMethod() != null && pp.getWriteMethod() != null) {
				list.add(pp.getName());
			}
		}
		return list;
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return properties.values();
	}

	@Override
	public Property getProperty(String name) {
		return properties.get(name);
	}
}
