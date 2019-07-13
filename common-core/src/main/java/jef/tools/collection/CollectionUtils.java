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

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Array;
import java.util.AbstractList;
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
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;

import jef.tools.Assert;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;

/**
 * 集合操作工具类 v2.0
 * 
 * @author Joe
 */
public class CollectionUtils {
	protected CollectionUtils() {
	}

	/**
	 * 集合转换为Map
	 * 
	 * @param c
	 * @param keyExt
	 * @param valueExt
	 * @return
	 */
	public static <E, K, V> Map<K, List<V>> bucket(Collection<E> c, Function<E, K> keyExt, Function<E, V> valueExt) {
		Map<K, List<V>> buckets = new HashMap<>();
		if (c != null) {
			for (Iterator<E> it = c.iterator(); it.hasNext();) {
				E value = it.next();
				K key = keyExt.apply(value);
				List<V> bucket = buckets.get(key);
				if (bucket == null) {
					buckets.put(key, bucket = new LinkedList<>());
				}
				bucket.add(valueExt.apply(value));
			}
		}
		return buckets;
	}

	/**
	 * 对集合进行分组
	 * 
	 * @param collection 要分组的集合
	 * @param function   获取分组Key的函数
	 * @return 分组后的集合，每个Key可对应多个Value值。
	 */
	public static <T, K> Multimap<K, T> bucket(Collection<T> collection, Function<T, K> function) {
		Multimap<K, T> result = ArrayListMultimap.create();
		if (collection != null) {
			for (T value : collection) {
				K attrib = function.apply(value);
				result.put(attrib, value);
			}
		}
		return result;
	}

	/**
	 * 将数组转换为Map。（Map不保证顺序）
	 * 
	 * @param array        数组
	 * @param keyExtractor 键值提取函数
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
	 * 将Collection转换为Map。（Map不保证顺序）
	 * 
	 * @param collection   集合
	 * @param keyExtractor 键值提取函数
	 * @param checkIsDup   true时，如果出现重复的键值，将检查对应的value是否互相equal，如果equals则发生replace。否则抛出异常。
	 *                     为false时，不检查是否equal。直接新的value覆盖老的value。
	 * 
	 * @return 在每个元素中提取键值后，形成Map。相同键值的记录将发生叠加（仅保留最后的一个）
	 */
	public static <T, V> Map<T, V> groupUnique(Collection<V> collection, Function<V, T> keyExtractor) {
		if (collection == null || collection.isEmpty())
			return Collections.emptyMap();
		Map<T, V> result = new HashMap<T, V>(collection.size());
		for (V value : collection) {
			T key = keyExtractor.apply(value);
			V oldValue = result.put(key, value);
			if (oldValue != null) {
				if (!oldValue.equals(value)) {
					throw new IllegalStateException(
							"Detect two different objects with a same key, and one object will be replaced in the map." + value + " to " + oldValue);
				}
			}
		}
		return result;
	}

	/**
	 * nullsafe的集合
	 * 
	 * @param value
	 * @return
	 */
	public static <E> List<E> nullSafe(List<E> value) {
		return value == null ? Collections.emptyList() : value;
	}

	/**
	 * nullsafe的集合
	 * 
	 * @param value
	 * @return
	 */
	public static <E> Set<E> nullSafe(Set<E> value) {
		return value == null ? Collections.emptySet() : value;
	}

	/**
	 * nullsafe的集合
	 * 
	 * @param value
	 * @return
	 */
	public static <K, V> Map<K, V> nullSafe(Map<K, V> value) {
		return value == null ? Collections.emptyMap() : value;
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
	 * @param     <K>
	 * @param     <V>
	 * @param map 要反转的Map
	 * @return A new Multimap that reverse key and value
	 */
	public static <K, V> Multimap<V, K> reverse(Map<K, V> map) {
		Multimap<V, K> result = ArrayListMultimap.create();
		if (map != null) {
			for (Entry<K, V> e : map.entrySet()) {
				result.put(e.getValue(), e.getKey());
			}
		}
		return result;
	}

	/**
	 * List to map
	 * 
	 * @param list
	 * @return
	 */
	public static <V> Map<V, Integer> getIndexMap(List<V> list) {
		if (isEmpty(list)) {
			return Collections.emptyMap();
		}
		Map<V, Integer> indexes = new HashMap<>();
		int index = 0;
		for (Iterator<V> it = list.iterator(); it.hasNext();) {
			indexes.put(it.next(), Integer.valueOf(index++));
		}
		return indexes;
	}

	/**
	 * 将一个数组的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param array      数组
	 * @param extractor  提取函数
	 * @param ignoreNull 如果为true，那么提出后的null值会被忽略
	 * @return 提取后形成的列表
	 */
	public static <F, T> List<T> extract(F[] array, Function<F, T> extractor) {
		return extract(Arrays.asList(array), extractor, false);
	}

	/**
	 * 将一个集合对象的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param collection 集合对象
	 * @param extractor  提取函数
	 * @return 提取后形成的列表
	 */
	public static <F, T> List<T> extract(Collection<F> collection, Function<F, T> extractor) {
		return extract(collection, extractor, false);
	}

	/**
	 * 将一个集合对象的每个元素进行函数处理后重新组成一个集合
	 * 
	 * @param collection 集合对象
	 * @param extractor  提取函数
	 * @param ignoreNull 如果为true，那么提出后的null值会被忽略
	 * @return 提取后形成的列表
	 */
	public static <F, T> List<T> extract(Collection<F> collection, Function<F, T> extractor, boolean ignoreNull) {
		List<T> result = new ArrayList<T>(collection.size());
		if (collection != null) {
			for (F a : collection) {
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
	 * 将数组进行转换
	 * 
	 * @param source
	 * @param function
	 * @return
	 */
	public static <F, T> List<T> transform(List<F> source, Function<F, T> function) {
		if (isEmpty(source)) {
			return Collections.emptyList();
		}
		return new TransformedList<F, T>(source, function);
	}

	/**
	 * 转换，和{@link #extract(Collection, Function)}
	 * 基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * 
	 * @param collection 集合对象
	 * @param function   提取函数
	 * @return 提取后形成的集合
	 */
	public static <F, T> Collection<F> transform(Collection<T> collection, Function<T, F> function) {
		return Collections2.transform(collection, function);
	}

	/**
	 * 转换，和{@link #extract(Collection, Function)}
	 * 基本一样，区别在于extract是立即计算，transform是延迟计算的。
	 * 
	 * @param array     数组
	 * @param extractor 提取函数
	 * @return 提取后形成的集合
	 */
	public static <F, T> Collection<F> transform(T[] array, Function<T, F> extractor) {
		return Collections2.transform(Arrays.asList(array), extractor);
	}

	/**
	 * 在集合中查找符合条件的首个元素
	 * 
	 * @param collection 集合
	 * @param filter     过滤器
	 * @return
	 */
	public static <T> T findFirst(Collection<T> collection, Predicate<T> filter) {
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
	 * @param            <T> 泛型
	 * @param collection 集合
	 * @param fieldname  字段
	 * @param value      值
	 * @return 过滤后的集合
	 */
	public static <T> List<T> find(Collection<T> collection, String fieldname, Object value) {
		if (collection == null || collection.isEmpty())
			return Collections.emptyList();
		Class<?> clz = collection.iterator().next().getClass();
		return filter(collection, new FieldValueFilter<T>(clz, fieldname, value));
	}

	/**
	 * 在集合中查找符合条件的元素
	 * 
	 * @param            <T> 泛型
	 * @param collection 集合
	 * @param filter     过滤器
	 * @return
	 */
	public static <T> List<T> filter(Collection<T> collection, Predicate<T> filter) {
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
	 * 对Map进行过滤，获得一个新的Map. 如果传入的是有序Map，新Map会保留原来的顺序。
	 * 
	 * @param map    要处理的Map
	 * @param filter 过滤器
	 * @return 过滤后的新Map
	 */
	public static <K, V> Map<K, V> filter(Map<K, V> map, Predicate<Map.Entry<K, V>> filter) {
		if (map == null) {
			return Collections.emptyMap();
		}
		Map<K, V> result;
		if (map instanceof SortedMap) {
			result = new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
		} else {
			result = new HashMap<K, V>(map.size());
		}
		for (Map.Entry<K, V> e : map.entrySet()) {
			Boolean applied = filter.apply(e);
			if (applied) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	/**
	 * 从集合中去除不需要的元素（精炼，提炼） 注意，如果传入的Collection类型不支持Iterator.remove()方式移除元素，将抛出异常。(
	 * 一般是UnsupportedOperationException)
	 * 
	 * @param            <T> 泛型
	 * @param collection 集合
	 * @param filter
	 * @return
	 */
	public static <T> void refine(Collection<T> collection, Predicate<T> filter) {
		if (collection != null) {
			for (Iterator<T> iter = collection.iterator(); iter.hasNext();) {
				T e = iter.next();
				if (!filter.apply(e)) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * 从集合中去除不需要的元素（精炼，提炼），返回true时元素被保留。反之被删除。<br>
	 * 注意，如果传入的Map类型不支持Iterator.remove()方式移除元素，将抛出异常。(
	 * 一般是UnsupportedOperationException)
	 * 
	 * @param map    要处理的Map
	 * @param filter Function，用于指定哪些元素要保留
	 * @throws UnsupportedOperationException
	 */
	public static <K, V> void refine(Map<K, V> map, Predicate<Map.Entry<K, V>> filter) {
		if (map != null) {
			for (Iterator<Map.Entry<K, V>> iter = map.entrySet().iterator(); iter.hasNext();) {
				Map.Entry<K, V> e = iter.next();
				if (!filter.apply(e)) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * 将传入的对象转换为可遍历的对象。
	 * 
	 * @param data
	 * @return
	 */
	public static <E> Iterator<E> iterator(Enumeration<E> data) {
		Assert.notNull(data);
		return new EnumerationIterator<E>(data);
	}

	/**
	 * 将Enumeration转换为一个新的List
	 * 
	 * @param data
	 * @return 转换后的List
	 */
	public static <E> List<E> toList(Enumeration<E> data) {
		if (data == null) {
			return Collections.emptyList();
		}
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
		if (data == null) {
			return Collections.emptyList();
		}
		List<E> result = new ArrayList<E>();
		for (Iterator<E> iter = data.iterator(); iter.hasNext();) {
			result.add(iter.next());
		}
		return result;
	}

	/**
	 * 得到数组或集合类型的长度
	 * 
	 * @param obj
	 * @return
	 * @throws IllegalArgumentException 如果传入参数不是一个数组或者一个集合抛出
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
	 * 两个集合对象的合并，并去除重复对象
	 * 
	 * @param   <T>
	 * @param a 集合A
	 * @param b 集合B
	 * @return
	 */
	public static <T> Collection<T> union(Collection<T> a, Collection<T> b) {
		if (a == null && b == null) {
			return Collections.emptySet();
		}
		if (a == null)
			a = Collections.emptyList();
		if (b == null)
			b = Collections.emptyList();
		HashSet<T> s = new HashSet<T>(a.size() + b.size());
		s.addAll(a);
		s.addAll(b);
		return s;
	}

	/**
	 * Return <code>true</code> if the supplied Collection is <code>null</code> or
	 * empty. Otherwise, return <code>false</code>.
	 * 
	 * @param collection the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isEmpty(Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Collection is NOT<code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * 
	 * @param collection the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isNotEmpty(Collection<?> collection) {
		return !(collection == null || collection.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Map is <code>null</code> or empty.
	 * Otherwise, return <code>false</code>.
	 * 
	 * @param map the Map to check
	 * @return whether the given Map is empty
	 */
	public static boolean isEmpty(Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Map is NOT <code>null</code> or
	 * empty. Otherwise, return <code>false</code>.
	 * 
	 * @param map the Map to check
	 * @return whether the given Map is empty
	 */
	public static boolean isNotEmpty(Map<?, ?> map) {
		return !(map == null || map.isEmpty());
	}

	/**
	 * Check whether the given Iterator contains the given element.
	 * 
	 * @param iterator the Iterator to check
	 * @param element  the element to look for
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
	 * @param enumeration the Enumeration to check
	 * @param element     the element to look for
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
	 * @param collection the Collection to check
	 * @param element    the element to look for
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
	 * @param source     the source Collection
	 * @param candidates the candidates to search for
	 * @return whether any of the candidates has been found
	 */
	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(candidates)) {
			return true;
		}
		if (isEmpty(source)) {
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
	 * Return <code>true</code> if all elements in '<code>candidates</code>' is
	 * contained in '<code>source</code>'; otherwise returns <code>false</code>.
	 * 
	 * @param source     the source Collection
	 * @param candidates the candidates to search for
	 * @return whether any of the candidates has been found
	 */
	public static boolean containsAll(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(candidates)) {
			return true;
		}
		if (isEmpty(source)) {
			return false;
		}
		for (Object candidate : candidates) {
			if (!source.contains(candidate)) {
				return false;
			}
		}
		return true;
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
	 * @param list  List
	 * @param index 序号
	 * @param value 值
	 */
	public static <T> void setElement(List<T> list, int index, T value) {
		Assert.notNull(list);
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

	final static class TransformedList<F, T> extends AbstractList<T> {
		final List<F> fromCollection;
		final Function<? super F, ? extends T> function;

		TransformedList(List<F> fromCollection, Function<? super F, ? extends T> function) {
			this.fromCollection = checkNotNull(fromCollection);
			this.function = checkNotNull(function);
		}

		@Override
		public void clear() {
			fromCollection.clear();
		}

		@Override
		public boolean isEmpty() {
			return fromCollection.isEmpty();
		}

		@Override
		public Iterator<T> iterator() {
			return Iterators.transform(fromCollection.iterator(), function);
		}

		@Override
		public int size() {
			return fromCollection.size();
		}

		@Override
		public T get(int index) {
			return function.apply(fromCollection.get(index));
		}
	}

	/**
	 * Iterator wrapping an Enumeration.
	 */
	static class EnumerationIterator<E> implements Iterator<E> {
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
	private static class FieldValueFilter<T> implements Predicate<T> {
		private FieldEx field;
		private Object value;

		public FieldValueFilter(Class<?> clz, String fieldname, Object value) {
			ClassEx cw = new ClassEx(clz);
			this.field = cw.getField(fieldname);
			Assert.notNull(this.field, "the field " + fieldname + " is not found in class " + cw.getName());
			this.value = value;
		}

		public boolean apply(T input) {
			try {
				Object v = field.get(input);
				return Objects.equal(v, value);
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}
	}
}
