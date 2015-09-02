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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import jef.common.wrapper.ArrayIterator;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.GenericUtils;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;

/**
 * 集合操作工具类 v2.0
 * 
 */
public final class CollectionUtils {
	private CollectionUtils() {
	}

	/**
	 * 在List中的指定位置插入元素。如果超出当前长度，则将list扩展到指定长度。
	 * 
	 * @param list
	 *            List
	 * @param index
	 *            序号
	 * @param value
	 *            值
	 */
	public static <T> void setElement(List<T> list, int index, T value) {
		if (index == list.size()) {
			list.add(value);
		} else if (index > list.size()) {
			for (int i = list.size(); i < index; i++) {
				list.add(null);
			}
			list.add(value);
		} else {
			list.set(index, value);
		}
	}

	/**
	 * 将数组转换为Map。（Map不保证顺序）
	 * 
	 * @param array
	 *            数组
	 * @param keyExtractor
	 *            键值提取函数
	 * @return 在每个元素中提取键值后，形成Map，如果多个对象返回相同的key，那么会互相覆盖。如果不希望互相覆盖，请使用{@linkplain #group(Collection, Function)}
	 */
	public static <K, V> Map<K, V> toMap(V[] array, Function<V, K> keyExtractor) {
		if (array == null || array.length == 0)
			return Collections.emptyMap();
		Map<K, V> result = new HashMap<K, V>(array.length);
		for (V value : array) {
			K key = keyExtractor.apply(value);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * 将Collection转换为Map。（Map不保证顺序）
	 * 
	 * @param collection
	 *            集合
	 * @param keyExtractor
	 *            键值提取函数
	 * @return 在每个元素中提取键值后，形成Map
	 */
	public static <K, V> Map<K, V> toMap(Collection<V> collection, Function<V, K> keyExtractor) {
		if (collection == null || collection.isEmpty())
			return Collections.emptyMap();
		Map<K, V> result = new HashMap<K, V>(collection.size());
		for (V value : collection) {
			K key = keyExtractor.apply(value);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * 将一个数组的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param array
	 *            数组
	 * @param extractor
	 *            提取函数
	 * @param ignoreNull
	 *            如果为true，那么提出后的null值会被忽略
	 * @return 提取后形成的列表
	 */
	public static <T, A> List<T> extract(A[] array, Function<A, T> extractor) {
		return extract(array, extractor, false);
	}

	/**
	 * 将一个数组的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param array
	 *            数组
	 * @param extractor
	 *            提取函数
	 * @param ignoreNull
	 *            如果为true，那么提出后的null值会被忽略
	 * @return 提取后形成的列表
	 */
	public static <T, A> List<T> extract(A[] array, Function<A, T> extractor, boolean ignoreNull) {
		List<T> result = new ArrayList<T>(array.length);
		if (array != null) {
			for (A a : array) {
				T t = extractor.apply(a);
				if (ignoreNull && t == null) {
					continue;
				}
				result.add(t);
			}
		}
		return result;
	}

	/**
	 * 将一个集合对象的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param collection
	 *            集合对象
	 * @param extractor
	 *            提取函数
	 * @return 提取后形成的列表
	 */
	public static <T, A> List<T> extract(Collection<A> collection, Function<A, T> extractor) {
		return extract(collection, extractor, false);
	}

	/**
	 * 将一个集合对象的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param collection
	 *            集合对象
	 * @param extractor
	 *            提取函数
	 * @param ignoreNull
	 *            如果为true，那么提出后的null值会被忽略
	 * @return 提取后形成的列表
	 */
	public static <T, A> List<T> extract(Collection<A> collection, Function<A, T> extractor, boolean ignoreNull) {
		List<T> result = new ArrayList<T>(collection.size());
		if (collection != null) {
			for (A a : collection) {
				T t = extractor.apply(a);
				if (ignoreNull && t == null) {
					continue;
				}
				result.add(t);
			}
		}
		return result;
	}

	/**
	 * 转换，和{@link #extract(Collection, Function)}基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * @param collection 集合对象
	 * @param extractor 提取函数
	 * @return 提取后形成的集合
	 */
	public static <T, A> Collection<T> transform(Collection<A> collection, Function<A, T> extractor) {
		return Collections2.transform(collection, extractor);
	}
	
	
	/**
	 * 转换，和{@link #extract(Collection, Function)}基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * @param array 数组
	 * @param extractor 提取函数
	 * @return 提取后形成的集合
	 */
	public static <T, A> Collection<T> transform(A[] array, Function<A, T> extractor) {
		return Collections2.transform(Arrays.asList(array), extractor);
	}
	
	/**
	 * 对Map对象进行翻转（键值互换），Key变为Value,Value变为key
	 * 
	 * 比如 有一个记录学生考试成绩的Map
	 * 
	 * <pre>
	 * <tt>{tom: 100},{jack: 95},{king: 88}, {mar: 77}, {jim: 88}</tt>
	 * </pre>
	 * 
	 * 分组后，得到的新的map为
	 * 
	 * <pre>
	 * <tt>{100:[tom]}, {95:[jack]}, {88: [king,jim]}, {77:[mar]}</tt>
	 * </pre>
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map 要反转的Map
	 * @return A new Multimap that reverse key and value
	 */
	public static <K, V> Multimap<V, K> inverse(Map<K, V> map) {
		Multimap<V, K> result = ArrayListMultimap.create();
		for (Entry<K, V> e : map.entrySet()) {
			result.put(e.getValue(), e.getKey());
		}
		return result;
	}
	
	/**
	 * 对列表进行分组
	 * 
	 * @param collection
	 *            要分组的集合
	 * @param function
	 *            获取分组Key的函数
	 * @return
	 */
	public static <T, A> Multimap<A, T> group(Collection<T> collection, Function<T, A> function) {
		Multimap<A, T> result = ArrayListMultimap.create();
		for (T value : collection) {
			A attrib = function.apply(value);
			result.put(attrib, value);
		}
		return result;
	}

	/**
	 * 在集合中查找符合条件的首个元素
	 * @param collection 集合
	 * @param filter   过滤器
	 * @return
	 */
	public static <T> T findFirst(Collection<T> collection, Function<T, Boolean> filter) {
		if (collection == null || collection.isEmpty())
			return null;
		for (T obj : collection) {
			if (filter.apply(obj)) {
				return obj;
			}
		}
		return null;
	}

	/**
	 * 根据字段名称和字段值查找第一个记录
	 * 
	 * @param collection 集合
	 * @param fieldname  字段名
	 * @param value      查找值
	 * @return
	 */
	public static <T> T findFirst(Collection<T> collection, String fieldname, Object value) {
		if (collection == null || collection.isEmpty())
			return null;
		Class<?> clz = collection.iterator().next().getClass();
		FieldValueFilter<T> f = new FieldValueFilter<T>(clz, fieldname, value);
		return findFirst(collection, f);
	}

	/**
	 * 根据字段名称和字段值查找所有记录
	 * 
	 * @param <T>
	 *            泛型
	 * @param collection
	 *            集合
	 * @param fieldname
	 *            字段
	 * @param value
	 *            值
	 * @return
	 */
	public static <T> List<T> filter(Collection<T> collection, String fieldname, Object value) {
		Class<?> clz = collection.iterator().next().getClass();
		FieldValueFilter<T> f = new FieldValueFilter<T>(clz, fieldname, value);
		return filter(collection, f);
	}

	/**
	 * 在集合中查找符合条件的元素
	 * 
	 * @param <T>
	 *            泛型
	 * @param collection
	 *            集合
	 * @param filter
	 *            过滤器
	 * @return
	 */
	public static <T> List<T> filter(Collection<T> collection, Function<T, Boolean> filter) {
		List<T> list = new ArrayList<T>();
		if (collection == null || collection.isEmpty())
			return list;
		for (T obj : collection) {
			if (filter.apply(obj)) {
				list.add(obj);
			}
		}
		return list;
	}
	
	
	/**
	 * 将数组或Enumation转换为Collection
	 * @param data
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Collection<T> toCollection(Object data,Class<T> type){
		if (data == null)
			return null;
		if(data instanceof Collection) {
			return ((Collection<T>) data);
		}else if(data.getClass().isArray()) {
			if(data.getClass().getComponentType().isPrimitive()) {
				int len=Array.getLength(data);
				List<T> result=new ArrayList<T>(len);
				for(int i=0;i<len;i++) {
					result.add((T) Array.get(data, i));
				}
			}else {
				return Arrays.asList((T[])data);
			}
		}else if(data instanceof Enumeration) {
			Enumeration<T> e=(Enumeration<T>)data;
			List<T> result=new ArrayList<T>();
			for(;e.hasMoreElements();) {
				result.add(e.nextElement());
			}
			return result;
		}
		throw new IllegalArgumentException("The input type "+data.getClass()+" can not convert to Collection.");
	}
	

	/**
	 * 检查传入的对象类型，并尝试获取其遍历器句柄
	 * @param data 要判断的对象
	 * @param clz  
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> Iterator<T> iterator(Object data,Class<T> clz) {
		if (data == null)
			return null;
		if(data instanceof Collection) {
			return ((Collection<T>) data).iterator();
		}else if(data.getClass().isArray()) {
			return new ArrayIterator<T>(data);
		}else if(data instanceof Enumeration) {
			return new EnumerationIterator<T>((Enumeration<T>)data);
		}
		return null;
	}
	
	/**
	 * 将传入的对象转换为可遍历的对象。
	 * @param data
	 * @return
	 */
	public static <E> Iterator<E> iterator(Enumeration<E> data) {
		return new EnumerationIterator<E>(data);
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
	 * 获取List当中的值
	 * 
	 * @param obj
	 * @param index
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object listGet(List obj, int index) {
		int length = obj.size();
		if (index < 0)
			index += length;
		return obj.get(index);
	}

	/**
	 * 设置List当中的值
	 * 
	 * @param obj
	 * @param index
	 * @param value
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void listSet(List obj, int index, Object value) {
		int length = obj.size();
		if (index < 0)
			index += length;
		obj.set(index, value);
	}

	/**
	 * 得到数组或集合类型的长度
	 * 
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static int length(Object obj) {
		if (obj.getClass().isArray()) {
			return Array.getLength(obj);
		}
		Assert.isTrue(obj instanceof Collection);
		return ((Collection) obj).size();
	}

	/**
	 * 检测索引是否有效 当序号为负数时，-1表示最后一个元素，-2表示倒数第二个，以此类推
	 */
	public static boolean isIndexValid(Object obj, int index) {
		int length = length(obj);
		if (index < 0)
			index += length;
		return index >= 0 && index < length;
	}

	@SuppressWarnings("rawtypes")
	public static void listSetAndExpand(List obj, int index, Object value) {
		int length = obj.size();
		if (index < 0 && index + length >= 0) {
			index += length;
		} else if (index < 0) {// 需要扩张
			toFixedSize(obj, -index);
		} else if (index >= length) {// 扩张
			toFixedSize(obj, index + 1);
		}
		listSet(obj, index, value);
	}

	/**
	 * 将list的大小调节为指定的大小 如果List长度大于制定的大小，后面的元素将被丢弃， 如果list小于指定大小，将会由null代替
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void toFixedSize(List obj, int newsize) {
		int len = obj.size();
		if (newsize == len)
			return;
		if (newsize > len) {
			for (int i = len; i < newsize; i++) {
				obj.add(null);
			}
		} else {
			for (int i = len; i > newsize; i--) {
				obj.remove(i - 1);
			}
		}
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
	 * 两个集合对象的合并
	 * 
	 * @param <T>
	 * @param a
	 *            集合A
	 * @param b
	 *            集合B
	 * @return
	 */
	public static <T> Collection<T> union(Collection<T> a, Collection<T> b) {
		HashSet<T> s = new HashSet<T>(a.size() + b.size());
		s.addAll(a);
		s.addAll(b);
		return s;
	}

	/**
	 * Return <code>true</code> if the supplied Collection is <code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * 
	 * @param collection
	 *            the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Map is <code>null</code> or
	 * empty. Otherwise, return <code>false</code>.
	 * 
	 * @param map
	 *            the Map to check
	 * @return whether the given Map is empty
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * Convert the supplied array into a List. A primitive array gets converted
	 * into a List of the appropriate wrapper type.
	 * <p>
	 * A <code>null</code> source value will be converted to an empty List.
	 * 
	 * @param source
	 *            the (potentially primitive) array
	 * @return the converted List result
	 * @see ObjectUtils#toObjectArray(Object)
	 */
	public static List<?> arrayToList(Object source) {
		return Arrays.asList(ArrayUtils.toObject(source));
	}

	/**
	 * Check whether the given Iterator contains the given element.
	 * 
	 * @param iterator
	 *            the Iterator to check
	 * @param element
	 *            the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean contains(Iterable<?> iterable, Object element) {
		if (iterable != null) {
			Iterator<?> iterator = iterable.iterator();
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (Objects.equal(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given Enumeration contains the given element.
	 * 
	 * @param enumeration
	 *            the Enumeration to check
	 * @param element
	 *            the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean contains(Enumeration<?> enumeration, Object element) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (Objects.equal(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given Collection contains the given element instance.
	 * <p>
	 * Enforces the given instance to be present, rather than returning
	 * <code>true</code> for an equal element as well.
	 * 
	 * @param collection
	 *            the Collection to check
	 * @param element
	 *            the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean fastContains(Collection<?> collection, Object element) {
		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return <code>true</code> if any element in '<code>candidates</code>' is
	 * contained in '<code>source</code>'; otherwise returns <code>false</code>.
	 * 
	 * @param source
	 *            the source Collection
	 * @param candidates
	 *            the candidates to search for
	 * @return whether any of the candidates has been found
	 */
	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return false;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return true;
			}
		}
		return false;
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
		if (isEmpty(collection)) {
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
	 * 
	 * Create a new identityHashSet.
	 * 
	 * @return
	 */
	public static <E> Set<E> identityHashSet() {
		return Collections.newSetFromMap(new IdentityHashMap<E, Boolean>());
	}


	/**
	 * Iterator wrapping an Enumeration.
	 */
	private static class EnumerationIterator<E> implements Iterator<E> {
		private Enumeration<E> enumeration;

		public EnumerationIterator(Enumeration<E> enumeration) {
			this.enumeration = enumeration;
		}

		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		public E next() {
			return this.enumeration.nextElement();
		}

		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	/**
	 * 给定字段名称和值，比较目标的字段值
	 * 
	 * @author Administrator
	 * @Date 2011-6-15
	 * @param <T>
	 */
	private static class FieldValueFilter<T> implements Function<T, Boolean> {
		private FieldEx field;
		private Object value;

		public FieldValueFilter(Class<?> clz, String fieldname, Object value) {
			ClassEx cw = new ClassEx(clz);
			this.field = cw.getField(fieldname);
			Assert.notNull(this.field, "the field " + fieldname + " is not found in class " + cw.getName());
			this.value = value;
		}

		public Boolean apply(T input) {
			try {
				Object v = field.get(input);
				return Objects.equal(v, value);
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}
	}
	
	/**
	 * 获得集合的最后一个元素
	 * @param collection
	 * @return
	 */
	public static <T> T last(List<T> collection) {
		if(collection==null || collection.isEmpty()) {
			return null;
		}
		return collection.get(collection.size()-1);
		
	}
}
