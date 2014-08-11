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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jef.accelerator.cglib.beans.BeanCopier;
import jef.accelerator.cglib.core.Converter;

/**
 * 深拷贝工具，基于反射，可以对任何对象实施Clone.
 * @author Administrator
 *
 */
public class CloneUtils {
	private static ConcurrentMap<Class<?>, Cloner> beanCopiers = new ConcurrentHashMap<Class<?>, Cloner>();
	
	static{
		beanCopiers.put(ArrayList.class, new Cloner._ArrayList());
		beanCopiers.put(HashSet.class, new Cloner._HashSet());
		beanCopiers.put(Arrays.asList().getClass(), new Cloner._ArrayList());
		beanCopiers.put(HashMap.class, new Cloner._HashMap());
	}
	
	static Cloner getCloner(Class<?> clz) {
		Cloner cloner=beanCopiers.get(clz);
		if(cloner!=null)return cloner;
		cloner=new BeanCloner(BeanCopier.create(clz, clz, true));
		beanCopiers.putIfAbsent(clz, cloner);
		return cloner;
	}
	
	
	public static Object clone(Object obj) {
		Cloner copier = getCloner(obj.getClass());
		return copier.clone(obj);
	}
	
	static final Converter clone_cvt = new Converter() {
		@SuppressWarnings("rawtypes")
		public Object convert(Object pojo, Class fieldType, Object fieldName) {
			return _clone(pojo);
		}
	};
	
	private static Object _clone(Object bean) {
		if (bean == null) {
			return null;
		}else if (bean instanceof DeepCloneable) {
			return CloneUtils.clone(bean);
		}
		Class<?> clz=bean.getClass();
		Cloner cl=beanCopiers.get(bean.getClass());
		if(cl!=null){
			return cl.clone(bean);
		}
		if (clz.isArray()) {
			return cloneArray(bean);
		}
		return bean;
//		if(isStateLessType(bean.getClass())){
//			return bean;
//		}else{
//			return CloneUtils.clone(bean);
//		}
	}
//	
//	private static boolean isStateLessType(Class<?> cls){
//		if(cls.isPrimitive() || cls.isEnum())return true;
//		if(cls.getName().startsWith("java.lang."))return true;
//		if(cls.getName().startsWith("java.io."))return true;
//		return false;
//	}
//	
//	
//	@SuppressWarnings("rawtypes")
//	private static Object cloneCollection(Object obj) {
//		Class<?> cls=obj.getClass();
//		Collection result;
//		try {
//			result = (Collection<?>) cls.newInstance();
//		} catch (InstantiationException e) {
//			throw new RuntimeException(e);
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e);
//		}
//		for(Object key: (Collection)obj){
//			result.add(_clone(key));
//		}
//		return result;
//	}
//	
//	@SuppressWarnings("rawtypes")
//	private static Object cloneMap(Object obj){
//		Class<?> cls=obj.getClass();
//		Map result;
//		try {
//			result = (Map) cls.newInstance();
//		} catch (InstantiationException e) {
//			throw new RuntimeException(e);
//		} catch (IllegalAccessException e) {
//			throw new RuntimeException(e);
//		}
//		Set<Map.Entry> entries=((Map)obj).entrySet();
//		for(Map.Entry key: entries){
//			result.put(key.getKey(),_clone(key.getValue()));
//		}
//		return result;
//	}

	private static Object cloneArray(Object obj) {
		int len = Array.getLength(obj);
		Class<?> priType = obj.getClass().getComponentType();
		Object clone = Array.newInstance(priType, len);

		if (priType.isPrimitive()) {
			System.arraycopy(obj, 0, clone, 0, len);
			return clone;
		}
		for (int i = 0; i < len; i++) {
			Array.set(clone, i, _clone(Array.get(obj, i)));
		}
		return clone;
	}
	
}
