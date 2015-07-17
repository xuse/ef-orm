package jef.tools.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.ReflectionException;

import jef.common.Entry;
import jef.common.Node;
import jef.common.log.LogUtil;
import jef.http.client.support.CommentEntry;
import jef.tools.ArrayUtils;
import jef.tools.DateUtils;
import jef.tools.StringUtils;
import jef.tools.collection.CollectionUtils;
import jef.tools.reflect.convert.Converter;

import org.apache.commons.lang.ObjectUtils;

/**
 * 数据类型转换工具类
 * @author jiyi
 *
 */
public final class ConvertUtils {
	private static final Converter<Long> N2J = new Converter<Long>() {
		public Long apply(Object input) {
			return ((Number) input).longValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Integer> N2I = new Converter<Integer>() {
		public Integer apply(Object input) {
			return ((Number) input).intValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Short> N2S = new Converter<Short>() {
		public Short apply(Object input) {
			return ((Number) input).shortValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Float> N2F = new Converter<Float>() {
		public Float apply(Object input) {
			return ((Number) input).floatValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Double> N2D = new Converter<Double>() {
		public Double apply(Object input) {
			return ((Number) input).doubleValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Character> N2C = new Converter<Character>() {
		public Character apply(Object input) {
			return (char) ((Number) input).intValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Byte> N2B = new Converter<Byte>() {
		public Byte apply(Object input) {
			return (byte) ((Number) input).intValue();
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<Boolean> N2Z = new Converter<Boolean>() {
		public Boolean apply(Object input) {
			return ((Number) input).intValue() != 0;
		}

		public boolean accept(Object v) {
			return v instanceof Number;
		}
	};
	private static final Converter<String> O2String = new Converter<String>() {
		public String apply(Object input) {
			return String.valueOf(input);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Integer> O2I = new Converter<Integer>() {
		public Integer apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0;
			}
			return Integer.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Long> O2J = new Converter<Long>() {
		public Long apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0L;
			}
			return Long.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Short> O2S = new Converter<Short>() {
		public Short apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0;
			}
			return Short.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Float> O2F = new Converter<Float>() {
		public Float apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0F;
			}
			return Float.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Double> O2D = new Converter<Double>() {
		public Double apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0D;
			}
			return Double.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Character> O2C = new Converter<Character>() {
		public Character apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0;
			}
			return s.charAt(0);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Byte> O2B = new Converter<Byte>() {
		public Byte apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return 0;
			}
			return Byte.valueOf(s);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};
	private static final Converter<Boolean> O2Z = new Converter<Boolean>() {
		public Boolean apply(Object input) {
			String s = String.valueOf(input);
			if (StringUtils.isEmpty(s)) {
				return false;
			}
			return StringUtils.toBoolean(s, false);
		}

		public boolean accept(Object v) {
			return v instanceof Object;
		}
	};

	private static final Converter<java.util.Date> String2Date = new Converter<java.util.Date>() {
		@Override
		public Date apply(Object input) {
			String value = String.valueOf(input);
			Date date = DateUtils.autoParse(value);
			return date;
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}

	};
	private static final Converter<java.sql.Date> String2SqlDate = new Converter<java.sql.Date>() {
		@Override
		public java.sql.Date apply(Object input) {
			String value = String.valueOf(input);
			Date date = DateUtils.autoParse(value);
			return new java.sql.Date(date.getTime());
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}

	};
	private static final Converter<java.sql.Timestamp> String2TimeStamp = new Converter<java.sql.Timestamp>() {
		@Override
		public Timestamp apply(Object input) {
			String value = String.valueOf(input);
			Date date = DateUtils.autoParse(value);
			return new java.sql.Timestamp(date.getTime());
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}

	};
	private static final Converter<java.sql.Time> String2Time = new Converter<java.sql.Time>() {
		@Override
		public Time apply(Object input) {
			String value = String.valueOf(input);
			Date date = DateUtils.autoParse(value);
			return new java.sql.Time(date.getTime());
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}
	};
	private static final Converter<Object> RAW = new Converter<Object>() {
		@Override
		public Object apply(Object input) {
			return input;
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}
	};
	private static final Converter<Number> ToNumber = new Converter<Number>() {
		@Override
		public Number apply(Object input) {
			String value = String.valueOf(input);
			if (value.indexOf('.') > -1) {
				double d = Double.valueOf(value);
				return d;
			} else {
				long l = Long.valueOf(value);
				if (l <= Integer.MAX_VALUE) {
					return (int) l;
				} else {
					return l;
				}
			}
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}
	};

	private static class EnumConverter<T extends Enum<T>> extends Converter<T> {
		private Class<T> clz;

		EnumConverter(Class<T> clz) {
			this.clz = clz;
		}

		@Override
		public T apply(Object input) {
			if (input instanceof Integer) {
				int i = ((Integer) input).intValue();
				T[] ts = clz.getEnumConstants();
				if (i >= 0 && i < ts.length) {
					return ts[i];
				} else {
					return null;
				}
			} else {
				String s = String.valueOf(input);
				return Enums.valueOf(clz, s, null);
			}
		}

		@Override
		public boolean accept(Object v) {
			return true;
		}
	};

	private static class StringArrayConverter extends Converter< Object> {
		private Class<?> clz;
		private ClassEx cex;
//		StringArrayConverter(Class<?> clz) {
//			this.clz = clz;
//			this.cex=new ClassEx(clz);
//		}
		
		StringArrayConverter(Type clz) {
			this.cex=new ClassEx(clz);
			this.clz =cex.getWrappered();
		}

		@Override
		public Object apply(Object input) {
			String text=String.valueOf(input);
			String[] values = StringUtils.split(text,',');
			Object array = Array.newInstance(clz, values.length);// 创建数组容器
			int i = 0;
			for (Object o : values) {
				Array.set(array, i, toProperType(ObjectUtils.toString(o), cex, null));
				i++;
			}
			return array;
		}

		@Override
		public boolean accept(Object v) {
			return v instanceof CharSequence;
		}
	}
	
	
	private static final Map<Class<?>, Node<Converter<?>>> CACHE = new ConcurrentHashMap<Class<?>, Node<Converter<?>>>();

	static {
		Node<Converter<?>> toInt = new Node<Converter<?>>(N2I).append(O2I);
		CACHE.put(int.class, toInt);
		CACHE.put(Integer.class, toInt);

		Node<Converter<?>> toLong = new Node<Converter<?>>(N2J).append(O2J);
		CACHE.put(long.class, toLong);
		CACHE.put(Long.class, toLong);

		Node<Converter<?>> toShort = new Node<Converter<?>>(N2S).append(O2S);
		CACHE.put(short.class, toShort);
		CACHE.put(Short.class, toShort);

		Node<Converter<?>> toFloat = new Node<Converter<?>>(N2F).append(O2F);
		CACHE.put(float.class, toFloat);
		CACHE.put(Float.class, toFloat);

		Node<Converter<?>> toDouble = new Node<Converter<?>>(N2D).append(O2D);
		CACHE.put(double.class, toDouble);
		CACHE.put(Double.class, toDouble);

		Node<Converter<?>> toCharacter = new Node<Converter<?>>(N2C).append(O2C);
		CACHE.put(char.class, toCharacter);
		CACHE.put(Character.class, toCharacter);

		Node<Converter<?>> toByte = new Node<Converter<?>>(N2B).append(O2B);
		CACHE.put(byte.class, toByte);
		CACHE.put(Byte.class, toByte);

		Node<Converter<?>> toBoolean = new Node<Converter<?>>(N2Z).append(O2Z);
		CACHE.put(boolean.class, toBoolean);
		CACHE.put(Boolean.class, toBoolean);

		CACHE.put(String.class, new Node<Converter<?>>(O2String));

		CACHE.put(java.util.Date.class, new Node<Converter<?>>(String2Date));
		CACHE.put(java.sql.Date.class, new Node<Converter<?>>(String2SqlDate));
		CACHE.put(java.sql.Timestamp.class, new Node<Converter<?>>(String2TimeStamp));
		CACHE.put(java.sql.Time.class, new Node<Converter<?>>(String2Time));
		CACHE.put(Object.class, new Node<Converter<?>>(RAW));
		CACHE.put(Number.class, new Node<Converter<?>>(ToNumber));
	}

	private ConvertUtils() {
	}

	/**
	 * 尝试将String转换为合适的类型，以设置到某个Bean或容器中。<br>
	 * 目前能支持各种基本类型、数组、Map等复杂类型
	 * 
	 * @param value
	 *            要转换的String
	 * @param clz
	 *            容器类型
	 * @param oldValue
	 *            容器中原来的旧值，或可参照的对象实例。（可为null）
	 * @throws UnsupportedOperationException
	 *             如果无法转换，将抛出此异常
	 * 
	 */
	public static Object toProperType(String value, ClassEx clz, Object oldValue) {
		if (value == null) {
			if (clz.isPrimitive()) {
				return defaultValueOfPrimitive(clz.getWrappered());
			} else {
				return null;
			}
		}

		// 容器所允许的最宽松类型
		ClassEx container = clz;
		if (oldValue != null)
			clz = new ClassEx(oldValue.getClass());
		if (clz.isAssignableFrom(String.class)) {
			return value;
		}
		if (StringUtils.isEmpty(value)) {
			return defaultValueForBasicType(clz.getWrappered());
		}
		// 凡是在转换器池里的都是必定支持的转换
		Node<Converter<?>> c = CACHE.get(container.getWrappered());
		if (c != null) {
			return process(c, value);
		}

		if (clz.isEnum()) {
			EnumConverter<?> ec = new EnumConverter<>(clz.getWrappered().asSubclass(Enum.class));
			CACHE.put(clz.getWrappered(), new Node<Converter<?>>(ec));
			return ec.apply(value);
		} else if (clz.isArray()) {
			StringArrayConverter ec=new StringArrayConverter(clz.getComponentType());
			CACHE.put(clz.getWrappered(), new Node<Converter<?>>(ec));
			return ec.apply(value);
		} else if (clz.isCollection()) {
			String[] values = value.split(",");
			return toProperCollectionType(Arrays.asList(values), clz, oldValue);
		} else if (Map.class.isAssignableFrom(clz.getWrappered())) {
			return stringToMap(value, clz, oldValue);
		} else if (oldValue != null) {// 采用旧值类型转换不成功，尝试采用容器类型转换
			return toProperType(value, container, null);
		} else if (StringUtils.isEmpty(value)) {// 没东西，转啥呀
			return null;
		} else {
			StringBuilder sb = new StringBuilder("Can not convert [");
			sb.append(StringUtils.truncate(value, 200));
			sb.append("] to proper javatype:" + clz.getName());
			throw new UnsupportedOperationException(sb.toString());
		}
	}

	/**
	 * 转换为合适的值
	 * 
	 * @param value
	 * @param container
	 * @return
	 */
	public static Object toProperType(Object value, Type container) {
		return toProperType(value, new ClassEx(container), null);
	}

	/**
	 * 根据容器类型，转换数值
	 * 
	 * @param value
	 * @param container
	 * @param oldValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object toProperType(Object value, ClassEx container, Object oldValue) {
		if (value == null) {
			if (container.isPrimitive()) {
				return defaultValueOfPrimitive(container.getWrappered());
			} else {
				return null;
			}
		}

		Class<?> clz = value.getClass();
		if (container.isAssignableFrom(clz)) {
			if (container.isCollection()) {
				return checkAndConvertCollection((Collection) value, container);
			} else if (container.isMap()) {
				return checkAndConvertMap((Map) value, container);
			} else {
				return value;
			}
		} else if (CollectionUtils.isArrayOrCollection(clz)) {
			Collection<Object> accessor = CollectionUtils.toCollection(value, Object.class);
			if (container.isArray()) {
				return toProperArrayType(accessor, container, oldValue);
			} else if (container.isCollection()) {
				return toProperCollectionType(accessor, container, oldValue);
			} else {
				toProperType(accessor.toString(), container, oldValue);
			}
		}

		Node<Converter<?>> c = CACHE.get(container.getWrappered());
		if (c != null) {
			Object result = process(c, value);
			if (result != ObjectUtils.NULL)
				return result;
		}

		return toProperType(ObjectUtils.toString(value), container, oldValue);
	}

	private static Object process(Node<Converter<?>> cs, Object value) {
		for (Converter<?> c : cs) {
			if (c.accept(value)) {
				return c.apply(value);
			}
		}
		return ObjectUtils.NULL;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object checkAndConvertMap(Map rawValue, ClassEx container) {
		Entry<Type, Type> types = GenericUtils.getMapTypes(container.genericType);
		boolean checkKey = types.getKey() != Object.class;
		boolean checkValue = types.getValue() != Object.class;
		if (!checkKey && !checkValue)
			return rawValue;

		ClassEx tk = new ClassEx(types.getKey());
		ClassEx tv = new ClassEx(types.getValue());

		Set<Map.Entry> es = rawValue.entrySet();
		Map result;
		try {
			result = rawValue.getClass().newInstance();
		} catch (InstantiationException e1) {
			throw new IllegalArgumentException(e1);
		} catch (IllegalAccessException e1) {
			throw new IllegalArgumentException(e1);
		}
		for (Map.Entry e : es) {
			Object key = e.getKey();
			Object value = e.getValue();
			if (key != null && checkKey)
				key = toProperType(key, tk, null);
			if (value != null && checkValue)
				value = toProperType(value, tv, null);
			result.put(key, value);
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Object checkAndConvertCollection(Collection rawValue, ClassEx container) {
		Type type = GenericUtils.getCollectionType(container.genericType);
		if (type == Object.class)
			return rawValue;
		ClassEx t = new ClassEx(type);
		Collection result;
		try {
			result = rawValue.getClass().newInstance();
		} catch (InstantiationException e1) {
			throw new IllegalArgumentException(e1);
		} catch (IllegalAccessException e1) {
			throw new IllegalArgumentException(e1);
		}

		for (Object e : rawValue) {
			e = toProperType(e, t, null);
			result.add(e);
		}
		return result;
	}

	/**
	 * 转换为合适的数组类型
	 * 
	 * @param values
	 * @param c
	 * @param oldValue
	 * @return
	 */
	static Object toProperArrayType(Collection<?> values, ClassEx c, Object oldValue) {
		ClassEx arrType = new ClassEx(c.getComponentType());
		Object array = Array.newInstance(arrType.getWrappered(), values.size());// 创建数组容器
		int i = 0;
		for (Object o : values) {
			ArrayUtils.set(array, i, toProperType(ObjectUtils.toString(o), arrType, null));
			i++;
		}
		return array;
	}

	/**
	 * 返回八个原生类型的默认数值(的装箱类型)
	 * 
	 * @param javaClass
	 *            数据类型
	 * @return 返回该种技术类型的默认数值
	 * @throws IllegalArgumentException
	 *             如果传入的javaClass不是八种基础类型之一抛出。
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
		if (cls == String.class) {
			return "";
		} else if (cls.isPrimitive()) {
			return defaultValueOfPrimitive(cls);
		}
		return null;
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
	private static Object toProperCollectionType(Collection<?> values, ClassEx c, Object oldValue) {
		ClassEx cType = new ClassEx(c.getComponentType());
		try {
			Collection l = (Collection) CollectionUtils.createContainerInstance(c, 0);
			for (Object o : values) {
				l.add(ConvertUtils.toProperType(ObjectUtils.toString(o), cType, findElementInstance(oldValue)));
			}
			return l;
		} catch (Exception e) {
			throw new RuntimeException(e);
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
			for (int i = 0; i < Array.getLength(collection); i++) {
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object stringToMap(String value, ClassEx c, Object oldValue) {
		String[] values = StringUtils.split(value, ',');
		Entry<Type, Type> types = GenericUtils.getMapTypes(c.getGenericType());
		try {
			Map l = (Map) CollectionUtils.createContainerInstance(c, 0);
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
				l.put(ConvertUtils.toProperType(e.getKey(), new ClassEx(types.getKey()), hintKey), ConvertUtils.toProperType(e.getValue(), new ClassEx(types.getValue()), hintValue));
			}
			return l;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
