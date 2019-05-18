package jef.tools.collection;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import jef.common.wrapper.ArrayIterator;
import jef.tools.collection.CollectionUtils.EnumerationIterator;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.GenericUtils;

public class CollectionRefelect {

	/**
	 * 检查传入的对象类型，并尝试获取其遍历器句柄
	 * 
	 * @param data
	 *            要判断的对象
	 * @param clz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> iterator(Object data, Class<T> clz) {
		if (data == null)
			return null;
		if (data instanceof Collection) {
			return ((Collection<T>) data).iterator();
		} else if (data.getClass().isArray()) {
			return new ArrayIterator<T>(data);
		} else if (data instanceof Enumeration) {
			return new EnumerationIterator<T>((Enumeration<T>) data);
		}
		return null;
	}

	/**
	 * 判断指定的类型是否为数组或集合类型
	 * 
	 * @param type
	 * @return true if type is a collection type.
	 */
	public static boolean isArrayOrCollection(Type type) {
		if (type instanceof GenericArrayType) {
			return true;
		} else if (type instanceof Class) {
			Class<?> rawType = (Class<?>) type;
			return rawType.isArray() || Collection.class.isAssignableFrom(rawType);
		} else {
			return Collection.class.isAssignableFrom(GenericUtils.getRawClass(type));
		}
	}

	/**
	 * 得到指定的数组或集合类型的原始类型
	 * 
	 * @param type
	 * @return 如果给定的类型不是数组或集合，返回null,否则返回数组或集合的单体类型
	 */
	public static Type getComponentType(Type type) {
		if (type instanceof GenericArrayType) {
			return ((GenericArrayType) type).getGenericComponentType();
		} else if (type instanceof Class) {
			Class<?> rawType = (Class<?>) type;
			if (rawType.isArray()) {
				return rawType.getComponentType();
			} else if (Collection.class.isAssignableFrom(rawType)) {
				// 此时泛型类型已经丢失，只能返Object
				return Object.class;
			}
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			Type rawType = pType.getRawType();
			if (isCollection(rawType)) {
				return pType.getActualTypeArguments()[0];
			}
		}
		return null;
	}

	/**
	 * 得到指定类型（或泛型）的集合元素类型 。如果这个类型还是泛型，那么就丢弃参数得到原始的class
	 * 
	 * @param type
	 * @return
	 */
	public static Class<?> getSimpleComponentType(Type type) {
		Type result = getComponentType(type);
		if (result instanceof Class<?>) {
			return (Class<?>) result;
		}
		// 不是集合/数组。或者集合数组内的泛型参数不是Class而是泛型变量、泛型边界等其他复杂泛型
		return null;
	}
	

	/**
	 * 将根据传入的集合对象创建合适的集合容器
	 */
	@SuppressWarnings("rawtypes")
	public static Object createContainerInstance(ClassEx collectionType, int size) {
		Class raw = collectionType.getWrappered();
		try {
			if (collectionType.isArray()) {
				if (size < 0)
					size = 0;
				Object array = Array.newInstance(GenericUtils.getRawClass(collectionType.getComponentType()), size);
				return array;
			} else if (!Modifier.isAbstract(collectionType.getModifiers())) {// 非抽象集合
				Object c = raw.newInstance();
				return c;
			} else if (Object.class == raw || raw == List.class || raw == AbstractList.class) {
				return new ArrayList();
			} else if (raw == Set.class || raw == AbstractSet.class) {
				return new HashSet();
			} else if (raw == Map.class || raw == AbstractMap.class) {
				return new HashMap();
			} else if (raw == Queue.class || raw == AbstractQueue.class) {
				return new LinkedList();
			} else {
				throw new IllegalArgumentException("Unknown collection class for create:" + collectionType.getName());
			}
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Find the common element type of the given Collection, if any.<br>
	 * 如果集合中的元素都是同一类型，返回这个类型。如果集合中数据类型不同，返回null
	 * 
	 * @param collection
	 *            the Collection to check
	 * @return the common element type, or <code>null</code> if no clear common
	 *         type has been found (or the collection was empty)
	 * 
	 */
	public static Class<?> findCommonElementType(Collection<?> collection) {
		if (collection==null || collection.isEmpty()) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				} else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}


	/**
	 * 判断一个类型是否为Collection
	 * 
	 * @param type
	 * @return true if type is a collection type.
	 */
	public static boolean isCollection(Type type) {
		if (type instanceof GenericArrayType) {
			return false;
		} else if (type instanceof Class) {
			Class<?> rawType = (Class<?>) type;
			return Collection.class.isAssignableFrom(rawType);
		} else {
			return Collection.class.isAssignableFrom(GenericUtils.getRawClass(type));
		}
	}

	/**
	 * 将数组或Enumation转换为Collection
	 * 
	 * @param data
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> toCollection(Object data, Class<T> type) {
		if (data == null)
			return null;
		if (data instanceof Collection) {
			return ((Collection<T>) data);
		} else if (data.getClass().isArray()) {
			if (data.getClass().getComponentType().isPrimitive()) {
				int len = Array.getLength(data);
				List<T> result = new ArrayList<T>(len);
				for (int i = 0; i < len; i++) {
					result.add((T) Array.get(data, i));
				}
			} else {
				return Arrays.asList((T[]) data);
			}
		} else if (data instanceof Enumeration) {
			Enumeration<T> e = (Enumeration<T>) data;
			List<T> result = new ArrayList<T>();
			for (; e.hasMoreElements();) {
				result.add(e.nextElement());
			}
			return result;
		}
		throw new IllegalArgumentException("The input type " + data.getClass() + " can not convert to Collection.");
	}

}
