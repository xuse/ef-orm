/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import jef.accelerator.cglib.core.Transformer;

/**
 * @author Joey
 */
public final class CollectionsX extends CollectionUtils {
	private CollectionsX() {
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
	 * 集合转换为Map
	 * 
	 * @param collection
	 * @param keyExt
	 * @param valueExt
	 * @return
	 */
	public static <E, K, V> Map<K, List<V>> bucket(Collection<E> collection, Function<E, K> keyExt, Function<E, V> valueExt) {
		Map<K, List<V>> buckets = new HashMap<>();
		if (collection != null) {
			for (Iterator<E> it = collection.iterator(); it.hasNext();) {
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
	 * 将集合转换为另一种类型
	 * 
	 * @param c
	 * @param t
	 * @return
	 */
	public static <E, T> List<T> extract(Collection<E> c, Function<E, T> t) {
		List<T> result = new ArrayList<>(c.size());
		if (c != null) {
			for (Iterator<E> it = c.iterator(); it.hasNext();) {
				result.add(t.apply(it.next()));
			}
		}
		return result;
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
	 * bucket function with key transformed
	 * 
	 * @param c
	 * @param t
	 * @return
	 */
	public static <K, V> Map<K, List<V>> bucket(Collection<V> c, Transformer<V, K> t) {
		Map<K, List<V>> buckets = new HashMap<>();
		if (c != null) {
			for (Iterator<V> it = c.iterator(); it.hasNext();) {
				V value = it.next();
				K key = t.transform(value);
				List<V> bucket = (List<V>) buckets.get(key);
				if (bucket == null) {
					buckets.put(key, bucket = new LinkedList<>());
				}
				bucket.add(value);
			}
		}
		return buckets;
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
			if (filter.test(obj)) {
				return obj;
			}
		}
		return null;
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
			if (filter.test(obj)) {
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
			Boolean b = filter.test(e);
			if (Boolean.TRUE.equals(b)) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	/**
	 * 从集合中去除不需要的元素（精炼，提炼）
	 * 
	 * @param            <T> 泛型
	 * @param collection 集合
	 * @param filter     过滤器
	 * @return
	 */
	public static <T> void refine(Collection<T> collection, Predicate<T> filter) {
		if (collection != null) {
			for (Iterator<T> iter = collection.iterator(); iter.hasNext();) {
				T e = iter.next();
				if (!filter.test(e)) {
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
				if (!filter.test(e)) {
					iter.remove();
				}
			}
		}
	}
}
