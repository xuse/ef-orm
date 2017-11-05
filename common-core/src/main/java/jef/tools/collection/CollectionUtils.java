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
import java.util.Comparator;
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
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;

import jef.common.wrapper.ArrayIterator;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.GenericUtils;

/**
 * 集合操作工具类 v2.0
 * 
 * @author Joe
 */
public class CollectionUtils {
	/**
	 * 常量，用于获得一个空的SortedMap
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static final SortedMap EMPTY_SORTEDMAP = Collections.unmodifiableSortedMap(new TreeMap());

	private CollectionUtils() {
	}

	public static final class C extends CollectionUtils {
	}

	/**
	 * 将数组进行转换
	 * 
	 * @param source
	 * @param function
	 * @return
	 */
	public static <K, V> List<V> convert(List<K> source, Function<K, V> function) {
		List<V> result = new ArrayList<V>(source.size());
		for (int i = 0; i < source.size(); i++) {
			result.add(function.apply(source.get(i)));
		}
		return result;
	}

	/**
	 * 将数组转换为Map。（Map不保证顺序）
	 * 
	 * @param array
	 *            数组
	 * @param keyExtractor
	 *            键值提取函数
	 * @return 在每个元素中提取键值后，形成Map，如果多个对象返回相同的key，那么会互相覆盖。如果不希望互相覆盖，请使用
	 *         {@linkplain #group(Collection, Function)}
	 */
	public static <K, V> Map<K, V> group(V[] array, Function<V, K> keyExtractor) {
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
	 * 将数组转换为Map。（Map按Key排序）
	 * 
	 * @param array
	 *            数组
	 * @param keyExtractor
	 *            键值提取函数
	 * @param comp
	 *            键值比较器
	 * @return 在每个元素中提取键值后，形成Map，如果多个对象返回相同的key，那么会互相覆盖。如果不希望互相覆盖，请使用
	 *         {@linkplain #group(Collection, Function)}
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> SortedMap<K, V> groupToSortedMap(V[] array, Function<V, K> keyExtractor, Comparator<K> comp) {
		if (array == null || array.length == 0)
			return EMPTY_SORTEDMAP;
		SortedMap<K, V> result = new TreeMap<K, V>(comp);
		for (V value : array) {
			K key = keyExtractor.apply(value);
			result.put(key, value);
		}
		return result;
	}

	/**
	 * 将集合转换为Map。（Map按Key排序）
	 * 
	 * @param collection
	 *            集合
	 * @param keyExtractor
	 *            键值提取函数
	 * @param comp
	 *            键值比较器
	 * @return 在每个元素中提取键值后，形成Map，如果多个对象返回相同的key，那么会互相覆盖。如果不希望互相覆盖，请使用
	 *         {@linkplain #group(Collection, Function)}
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> SortedMap<K, V> groupToSortedMap(Collection<V> collection, Function<V, K> keyExtractor, Comparator<K> comp) {
		if (collection == null || collection.size() == 0)
			return EMPTY_SORTEDMAP;
		SortedMap<K, V> result = new TreeMap<K, V>(comp);
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
	 * 转换，和{@link #extract(Collection, Function)}
	 * 基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * 
	 * @param collection
	 *            集合对象
	 * @param extractor
	 *            提取函数
	 * @return 提取后形成的集合
	 */
	public static <T, A> Collection<T> transform(Collection<A> collection, Function<A, T> extractor) {
		return Collections2.transform(collection, extractor);
	}

	/**
	 * 转换，和{@link #extract(Collection, Function)}
	 * 基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * 
	 * @param array
	 *            数组
	 * @param extractor
	 *            提取函数
	 * @return 提取后形成的集合
	 */
	public static <T, A> Collection<T> transform(A[] array, Function<A, T> extractor) {
		return Collections2.transform(Arrays.asList(array), extractor);
	}

	/**
	 * 对Map对象进行翻转（键值互换），Key变为Value,Value变为key。
	 * 由于value值不能保证唯一，因此转换后的是一个{@link Multimap}。
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
	 * @param map
	 *            要反转的Map
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
	 * 对集合进行分组
	 * 
	 * @param collection
	 *            要分组的集合
	 * @param function
	 *            获取分组Key的函数
	 * @return 分组后的集合，每个Key可对应多个Value值。
	 */
	public static <T, A> Multimap<A, T> group(Collection<T> collection, Function<T, A> function) {
		Assert.notNull(collection);
		Multimap<A, T> result = ArrayListMultimap.create();
		for (T value : collection) {
			A attrib = function.apply(value);
			result.put(attrib, value);
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
	 * @param checkIsDup true时，如果出现重复的键值，将检查对应的value是否互相equal，如果equals则发生replace。否则抛出异常。
	 * 为false时，不检查是否equal。直接新的value覆盖老的value。
	 * 
	 * @return 在每个元素中提取键值后，形成Map。相同键值的记录将发生叠加（仅保留最后的一个）
	 */
	public static <T, V> Map<T, V> groupWithReplace(Collection<V> collection, Function<V, T> keyExtractor, boolean checkIsDup) {
		if (collection == null || collection.isEmpty())
			return Collections.emptyMap();
		Map<T, V> result = new HashMap<T, V>(collection.size());
		for (V value : collection) {
			T key = keyExtractor.apply(value);
			V oldValue = result.put(key, value);
			if (oldValue != null && checkIsDup) {
				if (!oldValue.equals(value)) {
					throw new IllegalStateException("Detect two different objects with a same key, and one object will be replaced in the map."+value+" to "+oldValue);
				}
			}
		}
		return result;
	}

	/**
	 * 在集合中查找符合条件的首个元素
	 * 
	 * @param collection
	 *            集合
	 * @param filter
	 *            过滤器
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
	 * @param collection
	 *            集合
	 * @param fieldname
	 *            字段名
	 * @param value
	 *            查找值
	 * @return 查找到的元素
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
	 * @return 过滤后的集合
	 */
	public static <T> List<T> getFiltered(Collection<T> collection, String fieldname, Object value) {
		if (collection.isEmpty())
			return Collections.emptyList();
		Class<?> clz = collection.iterator().next().getClass();
		return getFiltered(collection, new FieldValueFilter<T>(clz, fieldname, value));
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
	public static <T> List<T> getFiltered(Collection<T> collection, Function<T, Boolean> filter) {
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
	public static <T> void filter(Collection<T> collection, Function<T, Boolean> filter) {
		for (Iterator<T> iter = collection.iterator(); iter.hasNext();) {
			T e = iter.next();
			if (!Boolean.TRUE.equals(filter.apply(e))) {
				iter.remove();
			}
		}
	}

	/**
	 * 对Map进行过滤，获得一个新的Map. 如果传入的是有序Map，新Map会保留原来的顺序。
	 * 
	 * @param map
	 *            要处理的Map
	 * @param filter
	 *            过滤器
	 * @return 过滤后的新Map
	 */
	public static <K, V> Map<K, V> getFiltered(Map<K, V> map, Function<Map.Entry<K, V>, Boolean> filter) {
		Map<K, V> result;
		if (map instanceof SortedMap) {
			result = new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
		} else {
			result = new HashMap<K, V>(map.size());
		}
		for (Map.Entry<K, V> e : map.entrySet()) {
			Boolean b = filter.apply(e);
			if (Boolean.TRUE.equals(b)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	/**
	 * 对Map进行过滤，当filter返回true时，元素被保留。反之被删除。<br>
	 * 注意，如果传入的Map类型不支持Iterator.remove()方式移除元素，将抛出异常。(
	 * 一般是UnsupportedOperationException)
	 * 
	 * @param map
	 *            要处理的Map
	 * @param filter
	 *            Function，用于指定哪些元素要保留
	 * @throws UnsupportedOperationException
	 */
	public static <K, V> void filter(Map<K, V> map, Function<Map.Entry<K, V>, Boolean> filter) {
		for (Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Map.Entry<K, V> e = iter.next();
			if (!Boolean.TRUE.equals(filter.apply(e))) {
				iter.remove();
			}
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
	 * 将Enumeration转换为一个新的List
	 * 
	 * @param data
	 * @return 转换后的List
	 */
	public static <E> List<E> toList(Enumeration<E> data) {
		List<E> result = new ArrayList<E>();
		for (; data.hasMoreElements();) {
			result.add(data.nextElement());
		}
		return result;
	}

	/**
	 * 将Iterable转换为List
	 * 
	 * @param data
	 * @return 转换后的List
	 */
	public static <E> List<E> toList(Iterable<E> data) {
		List<E> result = new ArrayList<E>();
		for (Iterator<E> iter = data.iterator(); iter.hasNext();) {
			result.add(iter.next());
		}
		return result;
	}

	/**
	 * 将传入的对象转换为可遍历的对象。
	 * 
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
	 * 两个集合对象的合并，并去除重复对象
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
	 * 
	 * @param collection
	 * @return
	 */
	public static <T> T last(List<T> collection) {
		if (collection == null || collection.isEmpty()) {
			return null;
		}
		return collection.get(collection.size() - 1);

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
}
