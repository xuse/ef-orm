package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.management.ReflectionException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.Entry;
import jef.common.log.LogUtil;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.DateUtils;
import jef.tools.Exceptions;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtil;
import jef.tools.collection.IterableAccessor;

import org.apache.commons.lang.ObjectUtils;

public class BeanUtils {
	private static Map<Class<?>, ClassFieldAccessor> ACCESSOR_CACHE = new IdentityHashMap<Class<?>, ClassFieldAccessor>(256);

	/**
	 * 返回八个原生类型的默认数值(的装箱类型)
	 * 
	 * @param javaClass
	 * @return
	 */
	public static Object defaultValueOfPrimitive(Class<?> javaClass) {
		// int 226
		// short 215
		// long 221
		// boolean 222
		// float 219
		// double 228
		// char 201
		// byte 237
		if (javaClass.isPrimitive()) {
			String s = javaClass.getName();
			switch (s.charAt(1) + s.charAt(2)) {
			case 226:
				return 0;
			case 215:
				return Short.valueOf((short) 0);
			case 221:
				return 0L;
			case 222:
				return Boolean.FALSE;
			case 219:
				return 0f;
			case 228:
				return 0d;
			case 201:
				return (char) 0;
			case 237:
				return Byte.valueOf((byte) 0);
			}
		}
		throw new IllegalArgumentException(javaClass + " is not Primitive Type.");
	}

	/**
	 * 得到原生对象和String的缺省值。
	 * 
	 * @param cls
	 *            类型
	 * 
	 * @return 指定类型数据的缺省值。如果传入类型是primitive和String之外的类型返回null。
	 */
	public static Object defaultValueForBasicType(Class<?> cls) {
		if (cls == Integer.class || cls == Integer.TYPE) {
			return Integer.valueOf(0);
		} else if (cls == Byte.class || cls == Byte.TYPE) {
			return (byte) 0;
		} else if (cls == Character.class || cls == Character.TYPE) {
			return (char) 0;
		} else if (cls == Boolean.class || cls == Boolean.TYPE) {
			return Boolean.FALSE;
		} else if (cls == Long.class || cls == Long.TYPE) {
			return Long.valueOf(0);
		} else if (cls == Short.class || cls == Short.TYPE) {
			return (short) 0;
		} else if (cls == Double.class || cls == Double.TYPE) {
			return Double.valueOf(0);
		} else if (cls == Float.class || cls == Float.TYPE) {
			return Float.valueOf(0);
		} else if (cls == String.class) {
			return "";
		}
		return null;
	}

	/**
	 * 将bean的可读属性变为map 和 {@link #describe(Object)}含义相同
	 * 
	 * @param obj
	 *            对象
	 * @return 对象的属性并转换为Map
	 * 
	 */
	public static Map<String, Object> toMap(Object obj) {
		return describe(obj);
	}

	/**
	 * 将bean的可读属性变为map
	 * 
	 * @param obj
	 * @return
	 */
	public static Map<String, Object> describe(Object obj) {
		Map<String, Object> map = new HashMap<String, Object>();
		BeanWrapper bw = BeanWrapper.wrap(obj);
		for (String s : bw.getPropertyNames()) {
			if (bw.isReadableProperty(s)) {
				map.put(s, bw.getPropertyValue(s));
			}
		}
		return map;
	}

	/**
	 * 强行获取字段值 ，且不抛出受检异常(慎用)
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 */
	public static Object getFieldValue(Object obj, String fieldName) {
		if (obj == null)
			return null;
		Class<?> clz = obj.getClass();
		ClassFieldAccessor accessors = ACCESSOR_CACHE.get(clz);
		if (accessors == null) {
			accessors = new ClassFieldAccessor();
			ACCESSOR_CACHE = copyOnWritePut(clz, accessors, ACCESSOR_CACHE);
		} else {
			FieldAccessor acc = accessors.get(fieldName);
			if (acc != null) {
				return acc.getObject(obj);
			}
		}
		FieldEx f = getField(clz, fieldName);
		if (f == null)
			throw new NoSuchElementException(fieldName + " in " + clz.getName());
		accessors.put(fieldName, f.getAccessor());
		return f.get(obj);
	}

	/**
	 * get the field accessor.
	 * 
	 * @param field
	 *            field
	 * @param cache
	 *            if true, the result accessor will be cached.
	 * @return
	 */
	public static FieldAccessor getFieldAccessor(java.lang.reflect.Field field, boolean cache) {
		Class<?> clz = field.getDeclaringClass();
		FieldAccessor accessor = null;
		ClassFieldAccessor accessors = ACCESSOR_CACHE.get(clz);
		if (accessors != null) {
			accessor = accessors.get(field.getName());
		}
		if (accessor == null) {
			accessor = FieldAccessor.generateAccessor(field);
		}
		if (cache) {
			if (accessors == null) {
				accessors = new ClassFieldAccessor();
				ACCESSOR_CACHE = copyOnWritePut(clz, accessors, ACCESSOR_CACHE);
			}
			accessors.put(field.getName(), accessor);
		}
		return accessor;

	}

	/**
	 * 获取指定的Field的类型
	 * 
	 * @Title: getField
	 */
	public static Class<?> getFieldType(Class<?> clz, String fieldName) {
		ClassFieldAccessor accessors = ACCESSOR_CACHE.get(clz);
		if (accessors == null) {
			accessors = new ClassFieldAccessor();
			ACCESSOR_CACHE = copyOnWritePut(clz, accessors, ACCESSOR_CACHE);
		} else {
			FieldAccessor acc = accessors.get(fieldName);
			if (acc != null) {
				return acc.getType();
			}
		}

		FieldEx f = getField(clz, fieldName);
		if (f == null)
			throw new NoSuchElementException(fieldName + " in " + clz.getName());
		accessors.put(fieldName, f.getAccessor());
		return f.getType();
	}

	private static <K, V> Map<K, V> copyOnWritePut(K k, V v, Map<K, V> source) {
		Map<K, V> map = new IdentityHashMap<K, V>(source);
		map.put(k, v);
		return map;
	}

	/**
	 * 强行设置字段值
	 */
	public static void setFieldValue(Object obj, String fieldName, Object value) {
		if (obj == null)
			return;
		Class<?> clz = obj.getClass();
		ClassFieldAccessor accessors = ACCESSOR_CACHE.get(clz);
		if (accessors == null) {
			accessors = new ClassFieldAccessor();
			ACCESSOR_CACHE = copyOnWritePut(clz, accessors, ACCESSOR_CACHE);
		} else {
			FieldAccessor acc = accessors.get(fieldName);
			if (acc != null) {
				acc.set(obj, value);
				return;
			}
		}
		FieldEx f = getField(obj.getClass(), fieldName);
		if (f == null)
			throw new NoSuchElementException(fieldName + " in " + clz.getName());
		accessors.put(fieldName, f.getAccessor());
		f.set(obj, value);
	}

	/**
	 * 获取指定的Field <li>Class.getField不会返回受保护的和私有的方法，getDeclaredField不会返回父类中的方法。
	 * 此处可以返回受保护的和私有的方法(并可以运行)。子类中没有的也会去父类中找。</li>
	 * 
	 * @param cls
	 * @param fieldName
	 * @return
	 */
	public static FieldEx getField(Class<?> cls, String fieldName) {
		return getField(new ClassEx(cls), fieldName);
	}

	/**
	 * 获取指定的Field
	 * 
	 * @Title: getField
	 */
	static FieldEx getField(ClassEx c, String name) {
		java.lang.reflect.Field f = null;
		Class<?> cls = c.getWrappered();
		while (cls != null) {
			try {
				f = cls.getDeclaredField(name);
			} catch (SecurityException e) {
				LogUtil.exception(e);
			} catch (NoSuchFieldException e) {
				// do nothing
			}
			if (f != null)
				break;
			cls = cls.getSuperclass();
		}
		if (f == null)
			return null;
		return new FieldEx(f, c);
	}

	/**
	 * 用String设置字段的值
	 */
	public static boolean setFieldValueByString(Object obj, String fieldName, String value) throws ReflectionException {
		if (obj == null)
			return false;
		FieldEx f = getField(obj.getClass(), fieldName);
		if (f == null)
			return false;
		Type c = f.getGenericType();
		try {
			Object oldValue = f.get(obj);
			Object objV = toProperType(value, new ClassEx(c), oldValue);
			f.set(obj, objV);
		} catch (IllegalArgumentException e) {
			LogUtil.error("Error processing:" + fieldName);
			LogUtil.exception(e);
			throw new ReflectionException(e, e.getMessage());
		}
		return true;
	}

	/**
	 * 转换为合适的数组类型
	 * 
	 * @param values
	 * @param c
	 * @param oldValue
	 * @return
	 */
	public static Object toProperArrayType(IterableAccessor<?> values, ClassEx c, Object oldValue) {
		ClassEx arrType = new ClassEx(c.getComponentType());
		Object array = Array.newInstance(arrType.getWrappered(), values.length());// 创建数组容器
		int i = 0;
		for (Object o : values) {
			ArrayUtils.set(array, i, toProperType(ObjectUtils.toString(o), arrType, null));
			i++;
		}
		return array;
	}

	/**
	 * 转换为合适的集合类型
	 * 
	 * @param values
	 * @param c
	 * @param oldValue
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object toProperCollectionType(IterableAccessor values, ClassEx c, Object oldValue) {
		ClassEx cType = new ClassEx(c.getComponentType());
		try {
			Collection l = (Collection) CollectionUtil.createContainerInstance(c, 0);
			for (Object o : values) {
				l.add(toProperType(ObjectUtils.toString(o), cType, findElementInstance(oldValue)));
			}
			return l;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 尝试将String转换为合适的类型，以设置到某个Bean或容器中。<br>
	 * 目前能支持各种基本类型、数组、Map等复杂类型
	 * 
	 * @param value
	 *            要转换的String
	 * @param c
	 *            容器类型
	 * @param oldValue
	 *            容器中原来的旧值，或可参照的对象实例。（可为null）
	 * @throws UnsupportedOperationException
	 *             如果无法转换，将抛出此异常
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object toProperType(String value, ClassEx c, Object oldValue) {
		// 容器所允许的最宽松类型
		ClassEx containerClass = c;
		if (oldValue != null)
			c = new ClassEx(oldValue.getClass());
		if (c.isAssignableFrom(String.class)) {
			return value;
		}
		if (StringUtils.isEmpty(value)) {
			return defaultValueForBasicType(c.getWrappered());
		}
		if (Date.class.isAssignableFrom(c.getWrappered())) {
			Date date = DateUtils.autoParse(value);
			if (date == null) {
				throw new RuntimeException("unknow format of date: " + value);
			}
			if (java.sql.Date.class == c.getWrappered()) {
				return DateUtils.toSqlDate(date);
			} else if (java.sql.Time.class == c.getWrappered()) {
				return DateUtils.toSqlTime(date);
			} else if (java.sql.Timestamp.class == c.getWrappered()) {
				return DateUtils.toSqlTimeStamp(date);
			} else {
				return date;
			}
		} else if (Integer.class == c.getWrappered() || Integer.TYPE == c.getWrappered()) {
			return Integer.valueOf(value);
		} else if (Boolean.class == c.getWrappered() || Boolean.TYPE == c.getWrappered()) {
			return Boolean.valueOf(value);
		} else if (Double.class == c.getWrappered() || Double.TYPE == c.getWrappered()) {
			return Double.valueOf(value);
		} else if (Float.class == c.getWrappered() || Float.TYPE == c.getWrappered()) {
			return Float.valueOf(value);
		} else if (Byte.class == c.getWrappered() || Byte.TYPE == c.getWrappered()) {
			return Byte.valueOf(value);
		} else if (Character.class == c.getWrappered() || Character.TYPE == c.getWrappered()) {
			return value.charAt(0);
		} else if (Long.class == c.getWrappered() || Long.TYPE == c.getWrappered()) {
			return Long.valueOf(value);
		} else if (Short.class == c.getWrappered() || Short.TYPE == c.getWrappered()) {
			return Short.valueOf(value);
		} else if (c.isEnum()) {
			return Enums.valueOf(c.getWrappered().asSubclass(Enum.class), value, null);
		} else if (Object.class == c.getWrappered()) {
			return value;
		} else if (Number.class == c.getWrappered()) {
			return Double.valueOf(value);
		} else if (c.isArray()) {
			String[] values = value.split(",");
			return toProperArrayType(new IterableAccessor(values), c, oldValue);
		} else if (c.isCollection()) {
			String[] values = value.split(",");
			return toProperCollectionType(new IterableAccessor(values), c, oldValue);
		} else if (Map.class.isAssignableFrom(c.getWrappered())) {
			return stringToMap(value, c, oldValue);
		} else if (oldValue != null) {// 采用旧值类型转换不成功，尝试采用容器类型转换
			return toProperType(value, containerClass, null);
		} else if (StringUtils.isEmpty(value)) {// 没东西，转啥呀
			return null;
		} else {
			StringBuilder sb = new StringBuilder("Can not convert [");
			sb.append(StringUtils.truncate(value, 200));
			sb.append("] to proper javatype:" + c.getName());
			throw new UnsupportedOperationException(sb.toString());
		}
	}
	
	/**
	 * 将输入对象视为集合、数组对象，查找其中的非空元素，返回第一个 注意：Map不是Collection
	 * 
	 * @param 参数
	 */
	public static Object findElementInstance(Object collection) {
		if (collection == null)
			return null;
		if (collection.getClass().isArray()) {
			for (int i = 0; i <  Array.getLength(collection); i++) {
				Object o = Array.get(collection, i);
				if (o != null) {
					return o;
				}
			}
		} else if (collection instanceof Collection) {
			for (Object o : ((Collection<?>) collection)) {
				if (o != null) {
					return o;
				}
			}
		}
		return null;
	}

	/**
	 * 将输入对象视为集合、数组对象，根据其中的元素类型，返回新的元素实例
	 * 
	 * @throws
	 */
	public static Object createElementByElement(Object collection) {
		Object o = findElementInstance(collection);
		try {
			if (o != null) {
				return BeanUtils.newInstanceAnyway(o.getClass());
			}
		} catch (ReflectionException e) {
			LogUtil.exception(e);
		}
		return null;
	}

	/**
	 * 使用空参数构造，无视构造参数一律传null
	 */
	public static Object newInstanceAnyway(Class<?> cls) throws ReflectionException {
		Constructor<?> c = null;
		Object o = BeanUtils.defaultValueForBasicType(cls);
		if (o != null)
			return null;
		for (Constructor<?> con : cls.getDeclaredConstructors()) {// 获取全部构造
			if (c == null) {
				c = con;
			} else if (c.getParameterTypes().length > con.getParameterTypes().length) {// 取参数较少的构造
				c = con;
			}
			if (c.getParameterTypes().length == 0) {// 如果已经有参数为0的，就不再循环
				break;
			}
		}
		if (c == null)
			return null;
		if (!Modifier.isPublic(c.getModifiers())) {
			try {
				c.setAccessible(true);
			} catch (SecurityException e) {
				LogUtil.exception(c.getName(), e);
			}
		}
		Class<?>[] types = c.getParameterTypes();
		Object[] p = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			// modified by mjj,2011.9.30
			p[i] = BeanUtils.defaultValueForBasicType(types[i]);
		}
		try {
			return c.newInstance(p);
		} catch (Exception e) {
			LogUtil.exception(e);
			return null;
		}
	}

	/**
	 * 包装类转换为原生类
	 * 
	 * @param wrapperClass
	 * @return
	 */
	public static Class<?> toPrimitiveClass(Class<?> wrapperClass) {
		if (wrapperClass == Integer.class) {
			return Integer.TYPE;
		} else if (wrapperClass == Byte.class) {
			return Byte.TYPE;
		} else if (wrapperClass == Short.class) {
			return Short.TYPE;
		} else if (wrapperClass == Long.class) {
			return Long.TYPE;
		} else if (wrapperClass == Float.class) {
			return Float.TYPE;
		} else if (wrapperClass == Double.class) {
			return Double.TYPE;
		} else if (wrapperClass == Character.class) {
			return Character.TYPE;
		} else if (wrapperClass == Boolean.class) {
			return Boolean.TYPE;
		} else {
			return wrapperClass;
		}
	}

	/**
	 * 将8原生类型的类转换为对应的包装的类型。
	 */
	public static Class<?> toWrapperClass(Class<?> primitiveClass) {
		if (primitiveClass == Integer.TYPE)
			return Integer.class;
		if (primitiveClass == Long.TYPE)
			return Long.class;
		if (primitiveClass == Double.TYPE)
			return Double.class;
		if (primitiveClass == Short.TYPE)
			return Short.class;
		if (primitiveClass == Float.TYPE)
			return Float.class;
		if (primitiveClass == Character.TYPE)
			return Character.class;
		if (primitiveClass == Byte.TYPE)
			return Byte.class;
		if (primitiveClass == Boolean.TYPE)
			return Boolean.class;
		return primitiveClass;
	}

	/**
	 * 得到所有的字段名称
	 * 
	 * @param cls
	 * @param resolveUntilSuperclass
	 * @return
	 */
	public static String[] getFieldNames(Class<?> cls, Class<?> resolveUntilSuperclass) {
		FieldEx[] fields = getFields(cls, resolveUntilSuperclass, false, false);
		String[] result = new String[fields.length];
		for (int i = 0; i < fields.length; i++) {
			result[i] = fields[i].getName();
		}
		return result;
	}

	public static FieldEx[] getFields(Class<?> c) {
		return getFields(c, true);
	}

	public static FieldEx[] getFields(Class<?> c, boolean resolveSuper) {
		return getFields(c, resolveSuper ? Object.class : null, false, false);
	}

	public static FieldEx[] getFieldsWithGetterAndSetter(Class<?> cls, Class<?> resolveUntilSuperclass) {
		return getFields(cls, resolveUntilSuperclass, true, true);
	}

	public static FieldEx[] getFieldsWithGetter(Class<?> cls, Class<Object> resolveUntilSuperclass) {
		return getFields(cls, resolveUntilSuperclass, true, false);
	}

	public static FieldEx[] getFieldsWithSetter(Class<?> cls, Class<Object> resolveUntilSuperclass) {
		return getFields(cls, resolveUntilSuperclass, false, true);
	}

	/**
	 * 获取一个类中的全部字段。
	 * 
	 * @param c
	 * @param resolveUntilSuperclass
	 *            : 解析父类，直到出现指定的类型，为null,表示不解析父类
	 * @return
	 */
	public static FieldEx[] getFields(Class<?> c, Class<?> resolveUntilSuperclass, boolean mustWithGetter, boolean mustWithSetter) {
		if (resolveUntilSuperclass == null) {
			resolveUntilSuperclass = c.getSuperclass();
		}
		List<FieldEx> fields = new ArrayList<FieldEx>();
		HashSet<String> names = new HashSet<String>();
		ClassEx context = new ClassEx(c);
		Class<?> cls = c;
		while (cls != resolveUntilSuperclass) {
			for (Field field : cls.getDeclaredFields()) {
				int mod = field.getModifiers();
				if (Modifier.isStatic(mod) || Modifier.isNative(mod)) {
					continue;
				}
				if (names.contains(field.getName()))
					continue;// 如果父类中有和子类同名的字段，则跳过（被子类覆盖）

				FieldEx fex = new FieldEx(field, context);
				if (mustWithGetter) {
					MethodEx getter = getGetter(fex);
					if (getter == null)
						continue;
				}
				if (mustWithSetter) {
					MethodEx setter = getSetter(fex);
					if (setter == null)
						continue;
				}
				names.add(field.getName());
				fields.add(fex);
				if (!Modifier.isPublic(field.getModifiers())) {
					try {
						field.setAccessible(true);
					} catch (SecurityException e) {
						System.out.println(field.toString() + "\n" + e.getMessage());
					}
				}
			}
			cls = cls.getSuperclass();
		}
		return fields.toArray(new FieldEx[fields.size()]);
	}

	/**
	 * 判断Field在其所定义的类当中是否有默认的getter和setter
	 * 
	 * @param field
	 * @return
	 */
	public static boolean hasGetterAndSetter(FieldEx field) {
		MethodEx getter = getGetter(field);
		MethodEx setter = getSetter(field);
		return getter != null && setter != null;
	}

	/**
	 * 根据类的名称和参数构造实例。 如果不能创建则返回null;
	 * 
	 * @param className
	 *            类名
	 * @param params
	 *            构造参数
	 * @return 实例。如果过程中出现错误返回null。（不抛出异常）
	 */
	public static Object newInstance(String className, Object... params) {
		Class<?> clz;
		try {
			clz = Class.forName(className);
			return newInstance(clz, params);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	/**
	 * 待参数调用构造方法来创建实例
	 * 
	 * @param cls
	 *            类
	 * @param params
	 *            构造参数
	 * @return 实例
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> cls, Object... params){
		Constructor<T> me = null;
		if (params.length == 0) {
			try {
				me = cls.getDeclaredConstructor();
				if (me != null) {
					if (!Modifier.isPublic(me.getModifiers())) {
						me.setAccessible(true);
					}
					return me.newInstance(params);
				}
			} catch (Exception e) {
				Exceptions.thorwAsIllegalState(e); 
			}
		}
		List<Class<?>> list = new ArrayList<Class<?>>();
		for (Object pobj : params) {
			list.add(pobj.getClass());
		}
		Class<?>[] inputTypes = list.toArray(new Class[list.size()]);
		for (Constructor<?> ct : cls.getDeclaredConstructors()) {
			if (ct.getParameterTypes().length == list.size()) {
				if (isParameterCompatible(ct.getParameterTypes(), inputTypes, ct.isVarArgs())) {
					if (!Modifier.isPublic(ct.getModifiers())) {
						try {
							ct.setAccessible(true);
						} catch (SecurityException e) {
							System.out.println(ct.toString() + "\n" + e.getMessage());
						}
					}
					try {
						return (T) ct.newInstance(params);
					} catch (IllegalArgumentException e) {
						LogUtil.exception(e);
					} catch (InstantiationException e) {
						LogUtil.exception(e);
					} catch (IllegalAccessException e) {
						LogUtil.exception(e);
					} catch (InvocationTargetException e) {
						LogUtil.exception(e);
					}
				}
			}
		}
		return null;
	}

	/**
	 * 给定方法参数类型和 值参数类型，比较值类型是否可以作为该方法的参数。
	 * 
	 * @param methodTypes
	 * @param inputTypes
	 * @return
	 */
	public static boolean isParameterCompatible(Class<?>[] methodTypes, Class<?>[] inputTypes, boolean varArg) {
		if (varArg && methodTypes.length == inputTypes.length - 1) {// 如果是可变参数就加上最后一个参数
			inputTypes = ArrayUtils.addElement(inputTypes, methodTypes[methodTypes.length - 1]);
		}

		if (methodTypes.length != inputTypes.length)
			return false;
		for (int i = 0; i < methodTypes.length; i++) {// 检测每一个参数的类型是否匹配
			if (inputTypes[i] == null)
				continue;
			if (methodTypes[i].isPrimitive()) { // 如果方法的参数类型 是 原生类型，则包装
				methodTypes[i] = BeanUtils.toWrapperClass(methodTypes[i]);
			}
			if (inputTypes[i].isPrimitive()) { // 如果输入参数类型 是 原生类型，则包装
				inputTypes[i] = BeanUtils.toWrapperClass(inputTypes[i]);
			}
			if (!methodTypes[i].isAssignableFrom(inputTypes[i])) {
				return false;
			}
		}
		return true;
	}

	// @SuppressWarnings({"unchecked","rawtypes"})
	// private static boolean isAssignableFrom(Class mType, Class iType) {
	// if(mType.isInterface()){
	// Class s=iType;
	// while(s!=Object.class){
	// Class[] intfs=s.getInterfaces();
	// if(jef.tools.ArrayUtils.fastContains(intfs, mType)){
	// return true;
	// }
	// s=s.getSuperclass();
	// }
	// return false;
	// }else{
	// return mType.isAssignableFrom(iType);
	// }
	// }

	/**
	 * 获取所有方法：
	 * 
	 * @param cls
	 * @return
	 */
	public static MethodEx[] getMethods(Class<?> cls) {
		Method[] methods = cls.getMethods();
		ClassEx cw = new ClassEx(cls);
		MethodEx[] result = new MethodEx[methods.length];
		for (int i = 0; i < methods.length; i++) {
			result[i] = new MethodEx(methods[i], cw);
		}
		return result;
	}

	/**
	 * 返回符合参数的方法，和jdk的Class.getMethod(String name, Class...
	 * parameterTypes)有以下区别： <li>
	 * Class.getMethod不会返回受保护的和私有的方法，getDeclaredMethod不会返回父类中的方法。
	 * 此处可以返回受保护的和私有的方法(并可以运行)。子类中没有的也会去父类中找。</li> <li>
	 * Class.getMethod要求paramTypes完全一致才会返回方法，此处只要paramTypes兼容，即可返回。
	 * (例如：方法输入参数为Object,JDK默认要求你用Object.class作为参数才能得到方法，此处你可以用Object的任意子类来得到方法)
	 * </li> <li>此处的方法允许输入参数中的某个Class为null，表示放弃对该参数的兼容性检查。</li>
	 * 
	 * @param c
	 * @param method
	 * @param paramTypes
	 * @return
	 */
	public static MethodEx getCompatibleMethod(Class<?> c, String method, Class<?>... paramTypes) {
		Method m = null;
		Class<?> su = c;
		while (su != null) {
			m = getDeclaredMethod(su, method, paramTypes);
			if (m != null)
				break;
			if (su.isInterface()) {
				Class<?>[] clz = su.getInterfaces();
				if (clz.length > 0) {
					su = clz[0];
				} else {
					su = null;
				}
			} else {
				su = su.getSuperclass();
			}
		}
		if (m != null && !Modifier.isPublic(m.getModifiers())) {
			try {
				m.setAccessible(true);
			} catch (SecurityException e) {
				System.out.println(m.toString() + "\n" + e.getMessage());
			}
		}
		if (m == null)
			return null;
		return new MethodEx(m, c);
	}

	/**
	 * 根据方法名和参数从类中获得方法
	 * 
	 * 
	 */
	private static Method getDeclaredMethod(Class<?> su, String method, Class<?>... paramTypes) {
		try {
			for (Method m : su.getDeclaredMethods()) {
				if (!m.getName().equals(method))
					continue;
				Class<?>[] types = m.getParameterTypes();
				if (BeanUtils.isParameterCompatible(types, paramTypes, m.isVarArgs()))
					return m;
			}
		} catch (SecurityException e) {
			LogUtil.exception(e);
		}
		return null;
	}

	/**
	 * 返回有所符合名称的方法
	 * 
	 * @param c
	 *            类
	 * @param name
	 *            方法名
	 * @param maxReturn
	 *            最多返回的方法数,设置较少的数量有利于减少循环次数。
	 * @return
	 */
	public static MethodEx[] getMethodByName(ClassEx c, String name, int maxReturn) {
		return getMethodByName(c, name, maxReturn, SearchMode.ALLWAYS_IN_SUPER);
	}

	public enum SearchMode {
		/**
		 * 永远不查找父类
		 */
		NOT_IN_SUPER,
		/**
		 * 即便查找数量不满足，但是只要找到了(>0)，就不查找父类
		 */
		NOT_IN_SUPER_IF_FOUND,
		/**
		 * 无论如何都要查找父类
		 */
		ALLWAYS_IN_SUPER
	}

	/**
	 * 返回有所符合名称的方法
	 * 
	 * @param c
	 * @param name
	 * @param maxReturn
	 *            最多返回
	 * @param mode
	 *            当找不到时，继续查找父类的方法，行为特征
	 * @return
	 */
	public static MethodEx[] getMethodByName(ClassEx c, String name, int maxReturn, SearchMode mode) {
		List<MethodEx> methods = new ArrayList<MethodEx>();
		HashSet<String> resultNames = new HashSet<String>();
		Class<?> su = c.getWrappered();
		while (su != Object.class) {
			for (Method m : su.getDeclaredMethods()) {
				if (!m.getName().equals(name)) {// 名称不等则跳过
					continue;
				}
				Method bridgeMethod = null;
				if (m.isBridge()) {
					bridgeMethod = m;
					m = BridgeMethodResolver.findBridgedMethod(m);
					// 桥接
				} else {
					// 非橋接計算的方法必須讓位給橋接計算的方法
					if (resultNames.contains(m.toString())) {
						continue;
					}
				}
				resultNames.add(m.toString());
				MethodEx mex = new MethodEx(m, c);
				mex.setBridgeMethod(bridgeMethod);
				if (methods.contains(mex)) {
					methods.remove(mex);
				}
				methods.add(mex);
			}
			if (maxReturn > 0 && methods.size() >= maxReturn) {// 数量满足则返回
				break;
			}
			if (methods.size() > 0 && mode == SearchMode.NOT_IN_SUPER_IF_FOUND) {
				break;
			} else if (mode == SearchMode.NOT_IN_SUPER) {
				break;
			}
			su = su.getSuperclass();
		}
		for (MethodEx m : methods) {
			if (m != null && !Modifier.isPublic(m.getModifiers())) {
				try {
					m.setAccessible(true);
				} catch (SecurityException e) {
					System.out.println(m.toString() + "\n" + e.getMessage());
				}
			}
		}
		return methods.toArray(new MethodEx[methods.size()]);
	}

	/**
	 * 针对已经构造好的对象，执行这个方法
	 * 
	 * @param obj
	 *            目标对象
	 * @param method
	 *            方法名
	 * @param params
	 *            方法参数
	 * @return 调用结果
	 * @throws ReflectionException
	 */
	public static Object invokeMethod(Object obj, String method, Object... params) throws ReflectionException {
		ClassEx r = new ClassEx(obj.getClass());
		return r.innerInvoke(obj, method, false, params);
	}

	/**
	 * 反射调用静态方法
	 * 
	 * @param clz
	 *            目标类
	 * @param method
	 *            目标方法
	 * @param params
	 *            传入参数
	 * @return 调用结果
	 * @throws ReflectionException
	 *             反射异常
	 */
	@SuppressWarnings("unchecked")
	public static <X> X invokeStatic(Class<?> clz, String method, Object... params) throws ReflectionException {
		ClassEx r = new ClassEx(clz);
		return (X) r.invokeStaticMethod(method, params);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object stringToMap(String value, ClassEx c, Object oldValue) {
		String[] values = StringUtils.split(value, ',');
		Entry<Type, Type> types = GenericUtils.getMapTypes(c.getGenericType());
		try {
			Map l = (Map) CollectionUtil.createContainerInstance(c, 0);
			Object hintKey = null;
			Object hintValue = null;
			if (oldValue != null) {
				Map old = (Map) oldValue;
				Set ks = old.keySet();
				Collection vs = old.values();
				if (ks.size() > 0) {
					hintKey = ks.iterator().next();
				}
				if (vs.size() > 0) {
					hintValue = vs.iterator().next();
				}
			}
			for (int i = 0; i < values.length; i++) {
				CommentEntry e = CommentEntry.createFromString(values[i], ':', '=');
				l.put(toProperType(e.getKey(), new ClassEx(types.getKey()), hintKey), toProperType(e.getValue(), new ClassEx(types.getValue()), hintValue));
			}
			return l;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 从集合对象bean中的每个元素都获取一个字段的值，并且将这些值组成数组返回
	 * 
	 * @param entities
	 * @param fieldName
	 * @param type
	 * @return
	 */
	public static <T> T[] getFieldValues(Collection<?> entities, String fieldName, Class<T> type) {
		return getFieldValues(entities, fieldName, type, true);
	}

	/**
	 * 从集合对象bean中的每个元素都获取一个字段的值，并且将这些值组成数组返回
	 * 
	 * @param entities
	 *            对象集合
	 * @param fieldName
	 *            字段名（支持复杂语法，如. [n]等表示嵌套元素或者数组下标）
	 * @param type
	 * @param ignoreNullElement
	 *            如果为true，出现null元素则跳过，否则在返回结果中插入null值。
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] getFieldValues(Collection<?> entities, String fieldName, Class<T> type, boolean ignoreNullElement) {
		Assert.notNull(type, "the input class cann't be null.");
		List<T> result = new ArrayList<T>(entities.size());
		if (fieldName.indexOf('.') > -1 || fieldName.indexOf("[") > -1) {
			for (Object e : entities) {
				if (e == null) {
					if (!ignoreNullElement) {
						result.add(null);
					}
					continue;
				}
				BeanWrapperImpl bw = new BeanWrapperImpl(e);
				result.add((T) bw.getNestedProperty(fieldName));
			}
		} else {
			for (Object e : entities) {
				if (e == null) {
					if (!ignoreNullElement) {
						result.add(null);
					}
					continue;
				}
				BeanAccessor ba = FastBeanWrapperImpl.getAccessorFor(e.getClass());
				result.add((T) ba.getProperty(e, fieldName));
			}
		}
		return result.toArray((T[]) Array.newInstance(type, result.size()));
	}


	/**
	 * 获取一个 public的，非Native非static指定名称的方法(非Declared模式：找父类，不找私有)
	 * 
	 * @param c
	 *            类
	 * @param name
	 *            方法名
	 * @param types
	 *            方法类型参数
	 * @return
	 */
	static Method getNonStaticNativeMethod(Class<?> c, String name, Class<?>... types) {
		try {
			Method method = c.getMethod(name, types);
			int mod = method.getModifiers();
			if (Modifier.isStatic(mod) || Modifier.isNative(mod)) {
				return null;
			}
			return method;
		} catch (SecurityException e) {
			LogUtil.exception(e);
		} catch (NoSuchMethodException e) {
		}
		return null;
	}

	/**
	 * 获取一个非Native非static指定名称的方法(Declared模式：不找父类，可找私有)
	 * 
	 * @param c
	 *            类
	 * @param name
	 *            方法名
	 * @param types
	 *            方法参数类型
	 * @return
	 */
	static Method getNonStaticNativeDeclaredMethod(Class<?> c, String name, Class<?>... types) {
		try {
			Method method = c.getDeclaredMethod(name, types);
			int mod = method.getModifiers();
			if (Modifier.isStatic(mod) || Modifier.isNative(mod)) {
				return null;
			}
			if (!Modifier.isPublic(method.getModifiers())) {
				method.setAccessible(true);
			}
			return method;
		} catch (SecurityException e) {
			LogUtil.exception(e);
		} catch (NoSuchMethodException e) {
		}
		return null;
	}

	/**
	 * 获取一个非Native非static指定名称的方法(高级模式：可找私有，可找父类)
	 * 
	 * @param c
	 *            要查找的类
	 * @param name
	 *            方法名
	 * @param types
	 *            方法参数类型
	 * @return
	 */
	static Method getNonStaticNativeDeclaredMethodWithSuperClz(Class<?> c, String name, Class<?>... types) {
		do {
			Method result = getNonStaticNativeDeclaredMethod(c, name, types);
			if (result != null)
				return result;
			c = c.getSuperclass();
		} while (c != null);
		return null;
	}

	/**
	 * 将字段名变为大写或小写。根据Java Beans规范，有一种特殊情况。<br>
	 * 首字母小写但第二个字母大写时，property名称不变。
	 * 
	 * @param fieldName
	 * @return
	 */
	public static String capitalizeFieldName(String fieldName) {
		if (fieldName.length() > 1 && Character.isLowerCase(fieldName.charAt(0)) && Character.isUpperCase(fieldName.charAt(1))) {
			return fieldName;
		} else {
			return StringUtils.capitalize(fieldName);
		}
	}

	/**
	 * 获取field对应的getter
	 * 
	 * @param field
	 * @return Getter方法
	 */
	public static MethodEx getGetter(FieldEx field) {
		Class<?> c = field.getDeclaringClass();
		Class<?> type = field.getDeclaringType();

		Method getter = getNonStaticNativeDeclaredMethodWithSuperClz(c, "get" + capitalizeFieldName(field.getName()));
		if (getter == null && (type == Boolean.class || type == Boolean.TYPE)) {
			if (field.getName().startsWith("is")) {
				getter = getNonStaticNativeDeclaredMethodWithSuperClz(c, field.getName());
			} else {
				getter = getNonStaticNativeDeclaredMethodWithSuperClz(c, "is" + capitalizeFieldName(field.getName()));
			}
		}
		if (getter == null)
			return null;
		return new MethodEx(getter, field.instanceClass);
	}

	/**
	 * 获得field的setter方法
	 * 
	 * @param field
	 *            field对象
	 * @return Setter方法。
	 * @see MethodEx
	 * @see FieldEX
	 */
	public static MethodEx getSetter(FieldEx field) {
		Class<?> c = field.getDeclaringClass();
		Class<?> type = field.getDeclaringType();
		Method setter = getNonStaticNativeDeclaredMethodWithSuperClz(c, "set".concat(capitalizeFieldName(field.getName())), type);
		if (setter == null && field.getName().startsWith("is") && (type == Boolean.class || type == Boolean.TYPE)) {
			setter = getNonStaticNativeDeclaredMethodWithSuperClz(c, "set".concat(capitalizeFieldName(StringUtils.substringAfter(field.getName(), "is"))), type);
		}
		if (setter == null)
			return null;
		return new MethodEx(setter, field.instanceClass);
	}

	/**
	 * 获得位于方法的参数上的Annotation，
	 * 
	 * @param method
	 *            方法
	 * @param i
	 *            参数序号
	 * @param annotationClz
	 *            注解类的类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T getMethodParamAnnotation(Method method, int i, Class<T> annotationClz) {
		Annotation[][] annos = method.getParameterAnnotations();
		if (i >= annos.length) {
			throw new IllegalArgumentException("the param max index is:" + annos.length + ". the input index " + i + " is out of bound.");
		}
		Annotation[] anns = annos[i];
		if (anns == null || anns.length == 0) {
			return null;
		}
		for (Annotation a : anns) {
			if (ArrayUtils.contains(a.getClass().getInterfaces(), annotationClz)) {
				return (T) a;
			}
		}
		return null;
	}

	/**
	 * 在两个bean之间复制属性。
	 * <p>
	 * 使用了动态类技术，将为拷贝的两个对象生成一个专门的动态拷贝类，性能达到反射极限。
	 * <p>
	 * 注意：这个方法使用了BeanAccessor,如果有批量的Bean需要拷贝，使用BeanAccessor性能更好(
	 * 超过CGLib的BeanCopier，不过功能没有其强大)，具体代码如下 <code><pre>
	 * BeanAccessor accessor=FastBeanWrapperImpl.getAccessorFor(source.getClass());
	 * accessor.copy(source,target); //循环中调用
	 * </pre></code>
	 * 
	 * @param source
	 *            源对象
	 * @param target
	 *            目标对象，必须是source的子类
	 */
	public final static void copyProperties(Object source, Object target) {
		Assert.notNull(source);
		if (target == null)
			return;
		BeanAccessor accessor = FastBeanWrapperImpl.getAccessorFor(source.getClass());
		if (source.getClass() == target.getClass() || source.getClass().isAssignableFrom(target.getClass())) {// 这个校验的性能有点差
			accessor.copy(source, target);
		} else {
			for (String s : accessor.getPropertyNames()) {
				accessor.setProperty(target, s, accessor.getProperty(source, s));
			}
		}
	}

	/**
	 * 将Map的数据封装为一个Annotation(使用代理实现)
	 * 
	 * @param type
	 *            Annotation类型
	 * @param data
	 *            Annotation的数据。
	 * @return Annotation对象
	 */
	@SuppressWarnings("unchecked")
	public static final <T extends Annotation> T asAnnotation(Class<T> type, Map<String, Object> data) {
		for (Method method : type.getMethods()) {
			if (method.getDeclaringClass() != type) {
				continue;
			}
			// 如果Map中缺少数据，则用注解的默认值补上。
			if (!data.containsKey(method.getName())) {
				data.put(method.getName(), method.getDefaultValue());
			}
		}
		// 创建代理。
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new AnnotationInvocationHandler(type, data));
	}
}
