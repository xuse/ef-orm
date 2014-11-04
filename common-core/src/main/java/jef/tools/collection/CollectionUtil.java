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
import java.lang.reflect.Method;
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

import javax.management.ReflectionException;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.StringUtils;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.ClassEx;
import jef.tools.reflect.FieldEx;
import jef.tools.reflect.GenericUtils;

import org.apache.commons.lang.ObjectUtils;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class CollectionUtil {
	

	/**
	 * 给定字段名称和值，比较目标的字段值
	 * @author Administrator
	 * @Date 2011-6-15 
	 * @param <T>
	 */
	private static class FieldValueFilter<T> implements Function<T,Boolean> {
		private FieldEx field;
		private Object value;
		
		public FieldValueFilter(Class<?> clz,String fieldname,Object value){
			ClassEx cw=new ClassEx(clz);
			this.field=cw.getField(fieldname);
			Assert.notNull(this.field,"the field "+ fieldname+" is not found in class "+ cw.getName());
			this.value=value;
		}
		public Boolean apply(T input) {
			try {
				Object v=field.get(input);
				return ObjectUtils.equals(v, value);
			} catch (IllegalArgumentException e) {
				throw new IllegalAccessError(e.getMessage());
			}
		}

	}
	
	public static <T> void setElement(List<T> list,int index,T value){
		if(index==list.size()){
			list.add(value);
		}else if(index>list.size()){
			for(int i=list.size();i<index;i++){
				list.add(null);
			}
			list.add(value);
		}else{
			list.set(index, value);
		}
	}
	
	
	/**
	 * 将一个可遍历的对象中的某个property取出，组成所需要的list.
	 * @param iterable
	 * @param fieldName
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getFieldValues(Object iterable,String fieldName,Class<T> type){
		IterableAccessor<Object> iter=iterable(iterable);
		if(iter==null)return null;
		List<T> result=new ArrayList<T>();
		for(Object bean:iter){
			BeanWrapper bw=BeanWrapper.wrap(bean);
			Object value=bw.getPropertyValue(fieldName);
			result.add((T)value);
		}
		return result;
	}
	
	/**
	 * 将一个可集合对象的每个元素进行函数处理后重新组成一个集合
	 * @param collection
	 * @param function
	 * @return
	 */
	public static <T,A> List<T> extract(Collection<A> collection,Function<A,T> function){
		List<T> result=new ArrayList<T>();
		if(collection!=null){
			for(A a:collection){
				result.add(function.apply(a));
			}
		}
		return result;
	}
	
	/**
	 * 将集合对象的每个元素获取其方法属性后，重新组成一个集合
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getPropertyValues(Object iterable,String methodName,Class<T> type){
		IterableAccessor<Object> iter=iterable(iterable);
		if(iter==null)return null;
		List<T> result=new ArrayList<T>();
		Method me=null;
		for(Object bean:iter){
			if(bean==null){
				result.add(null);
				continue;
			}
			if(me==null){
				try {
					me=bean.getClass().getMethod(methodName);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			Object value;
			try {
				value = me.invoke(bean);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			result.add((T)value);
		}
		return result;
	}
	
	/**
	 * 将集合类型中的
	 * @param iterable
	 * @param fieldName
	 * @param type
	 * @return
	 */
	public static List<String> toString(Object iterable,String fieldName){
		IterableAccessor<Object> iter=iterable(iterable);
		if(iter==null)return null;
		List<String> result=new ArrayList<String>();
		for(Object bean:iter){
			result.add(StringUtils.toString(bean));
		}
		return result;
	}
	
	/**
	 * 对Map对象进行翻转，Key变为Value,Value变为key
	 * 
	 *  比如 有一个记录学生考试成绩的Map
	 *  <pre><tt>{tom: 100},{jack: 95},{king: 88}, {mar: 77}, {jim: 88}</tt></pre> 
	 *  分组后，得到的新的map为
	 *  <pre><tt>{100:[tom]}, {95:[jack]}, {88: [king,jim]}, {77:[mar]}</tt></pre> 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @return A new Multimap that reverse key and value
	 */
	public static <K, V> Multimap<V, K> inverse(Map<K, V> map) {
		Multimap<V, K> result = ArrayListMultimap.create();
		for (Entry<K,V> e : map.entrySet()) {
			result.put(e.getValue(), e.getKey());
		}
		return result;
	}

	/**
	 * 在集合中查找符合条件的首个元素
	 * 
	 * @param <T>
	 * @param collection
	 * @param filter
	 * @return
	 */
	public static <T> T findFirst(Collection<T> collection, Function<T,Boolean> filter) {
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
	 * @param <T>
	 * @param collection
	 * @param fieldname
	 * @param value
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
	 * @param collection
	 * @param fieldname
	 * @param value
	 * @return
	 */
	public static <T> List<T> findAll(Collection<T> collection, String fieldname, Object value) {
		Class<?> clz = collection.iterator().next().getClass();
		FieldValueFilter<T> f = new FieldValueFilter<T>(clz, fieldname, value);
		return find(collection, f);
	}

	/**
	 * 在集合中查找符合条件的元素
	 * 
	 * @param <T>
	 * @param collection
	 * @param filter
	 * @return
	 */
	public static <T> List<T> find(Collection<T> collection, Function<T,Boolean> filter) {
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
	
	public static IterableAccessor<Object> iterable(Object data){
		if(data==null)return null;
		if(isArrayOrCollection(data.getClass())){
			return new IterableAccessor<Object>(data);	
		}else{
			return null;
		}
	}
	
	/**
	 * 判断指定的类型是否为数组或集合类型
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isArrayOrCollection(Type type) {
		if (type instanceof GenericArrayType) {
			return true;
		} else if (type instanceof Class) {
			Class<?> rawType = (Class<?>) type;
			return rawType.isArray() || Collection.class.isAssignableFrom(rawType);
		}
		Class<?> rawType = GenericUtils.getRawClass(type);
		return Collection.class.isAssignableFrom(rawType);
	}

	/**
	 * 判断一个类型是否为Collection
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isCollection(Type type) {
		if (type instanceof GenericArrayType) {
			return false;
		} else if (type instanceof Class) {
			Class<?> rawType = (Class<?>) type;
			return Collection.class.isAssignableFrom(rawType);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			return isArrayOrCollection(pType.getRawType());
		}
		return false;
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
	 * 对列表进行分组
	 * 
	 * @param collection 要分组的集合
	 * @param function 获取分组Key的函数
	 * @return
	 */
	public static <T,A> Multimap<A,T> groupBy(Collection<T> collection, Function<T,A> function) {
		Multimap<A, T> result = ArrayListMultimap.create();
		for (T value : collection) {
			A attrib = function.apply(value);
			result.put(attrib, value);
		}
		return result;
	}

	/**
	 * 获取List当中的值
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
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static int length(Object obj) {
		if(obj.getClass().isArray()){
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
		Class raw=collectionType.getWrappered();
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
	 * 转换为数组
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object[] toArray(Object obj) {
		if(obj==null)return null;
		if(obj.getClass().isArray()){
			return ArrayUtils.toObject(obj);
		}else if(obj instanceof Collection){
			return ((Collection) obj).toArray();
		}
		throw new IllegalArgumentException("The input object "+ obj.getClass()+" is not array or collection !");
	}
	
	
	/**
	 * 转换为制定类型的数组
	 * @param collection
	 * @param clz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Collection<?> collection, Class<T> clz) {
		T[] result = (T[]) Array.newInstance(clz, collection.size());
		int n = 0;
		for (Object o : collection) {
			result[n] = (T) o;
			n++;
		}
		return result;
	}

	/**
	 * 两个集合对象的合并
	 * 
	 * @param <T>
	 * @param a
	 * @param b
	 * @return
	 */
	public static <T> Collection<T> union(Collection<T> a, Collection<T> b) {
		HashSet<T> s=new HashSet<T>(a.size()+b.size());
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
	public static boolean isEmpty(Map<?,?> map) {
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
	public static boolean contains(Iterator<?> iterator, Object element) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtils.equals(candidate, element)) {
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
				if (ObjectUtils.equals(candidate, element)) {
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
	 * Return the first element in '<code>candidates</code>' that is contained
	 * in '<code>source</code>'. If no element in '<code>candidates</code>' is
	 * present in '<code>source</code>' returns <code>null</code>. Iteration
	 * order is {@link Collection} implementation specific.
	 * 
	 * @param source
	 *            the source Collection
	 * @param candidates
	 *            the candidates to search for
	 * @return the first present object, or <code>null</code> if not found
	 */
	public static Object findFirstMatch(Collection<?> source, Collection<?> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Find the common element type of the given Collection, if any.
	 * 
	 * @param collection
	 *            the Collection to check
	 * @return the common element type, or <code>null</code> if no clear common
	 *         type has been found (or the collection was empty)
	 *         如果集合中的元素都是同一类型，返回这个类型。如果集合中数据类型不同，返回null
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
	 * Create a new identityHashSet.
	 * @return
	 */
	public static <E> Set<E> identityHashSet(){
		return Collections.newSetFromMap(new IdentityHashMap<E,Boolean>());
	}
	

	/**
	 * Adapts an enumeration to an iterator.
	 * 
	 * @param enumeration
	 *            the enumeration
	 * @return the iterator
	 */
	public static <E> Iterator<E> toIterator(Enumeration<E> enumeration) {
		return new EnumerationIterator<E>(enumeration);
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
	 * 将数组，Enumeration等都作为Collection处理
	 * @param o
	 * @return
	 */
	public Collection<?> toCollection(Object o){
		if(o==null)return null;
		if(o instanceof Collection<?>){
			return (Collection<?>)o;
		}
		if(o.getClass().isArray()){
			return Arrays.asList(ArrayUtils.toObject(o));
		}
		if(o instanceof Iterable<?>){
			List<Object> list=new ArrayList<Object>();
			for(Object x: (Iterable<?>)o){
				list.add(x);
			}
			return list;
		}
		if(o instanceof Enumeration<?>){
			List<Object> list=new ArrayList<Object>();
			Enumeration<?> e=(Enumeration<?>)o;
			for(;e.hasMoreElements();){
				list.add(e.nextElement());
			}
			return list;
		}
		throw new IllegalArgumentException("The type "+o.getClass().getName()+" Can not cast to Collection.");
	}
}
