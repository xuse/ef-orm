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
import jef.common.SimpleMap;

/**
 * 深拷贝工具，基于CG-Lib，可以对任何对象实施Clone.是目前已知的最高效的Java实现
 * @author Administrator
 *
 */
public class CloneUtils {
	private static ConcurrentMap<Class<?>, Cloner> BEAN_CLONERS = new ConcurrentHashMap<Class<?>, Cloner>();
	
	static{
		BEAN_CLONERS.put(ArrayList.class, new Cloner._ArrayList());
		BEAN_CLONERS.put(HashSet.class, new Cloner._HashSet());
		BEAN_CLONERS.put(Arrays.asList().getClass(), new Cloner._ArrayList());
		BEAN_CLONERS.put(HashMap.class, new Cloner._HashMap());
		BEAN_CLONERS.put(SimpleMap.class, new Cloner._HashMap());
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
		Cloner cl=BEAN_CLONERS.get(bean.getClass());
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
	
	private static Cloner getCloner(Class<?> clz) {
		Cloner cloner=BEAN_CLONERS.get(clz);
		if(cloner!=null)return cloner;
		cloner=new BeanCloner(BeanCopier.create(clz, clz, true));
		BEAN_CLONERS.putIfAbsent(clz, cloner);
		return cloner;
	}

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
