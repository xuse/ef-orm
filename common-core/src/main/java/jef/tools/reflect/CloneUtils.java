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
 * 
 * @author Administrator
 * 
 */
public class CloneUtils {
	private static ConcurrentMap<Class<?>, Cloner> BEAN_CLONERS = new ConcurrentHashMap<Class<?>, Cloner>();

	static {
		BEAN_CLONERS.put(ArrayList.class, new Cloner._ArrayList());
		BEAN_CLONERS.put(HashSet.class, new Cloner._HashSet());
		BEAN_CLONERS.put(Arrays.asList().getClass(), new Cloner._ArrayList());
		BEAN_CLONERS.put(HashMap.class, new Cloner._HashMap());
		BEAN_CLONERS.put(SimpleMap.class, new Cloner._HashMap());
		BEAN_CLONERS.put(String.class, Cloner.RAW);
		BEAN_CLONERS.put(Integer.class, Cloner.RAW);
		BEAN_CLONERS.put(Long.class, Cloner.RAW);
		BEAN_CLONERS.put(Short.class, Cloner.RAW);
		BEAN_CLONERS.put(Float.class, Cloner.RAW);
		BEAN_CLONERS.put(Double.class, Cloner.RAW);
		BEAN_CLONERS.put(Boolean.class, Cloner.RAW);
		BEAN_CLONERS.put(Byte.class, Cloner.RAW);
		BEAN_CLONERS.put(Character.class, Cloner.RAW);
	}
	

	public static Object clone(Object obj, boolean deep) {
		Cloner copier = getCloner(obj.getClass());
		return copier.clone(obj, deep);
	}

	public static Object clone(Object obj) {
		Cloner copier = getCloner(obj.getClass());
		return copier.clone(obj, false);
	}

	static final Converter clone_cvt_safe = new Converter() {
		@SuppressWarnings("rawtypes")
		public Object convert(Object pojo, Class fieldType, Object fieldName) {
			return _clone(pojo, false);
		}
	};

	static final Converter clone_cvt_deep = new Converter() {
		@SuppressWarnings("rawtypes")
		public Object convert(Object pojo, Class fieldType, Object fieldName) {
			return _clone(pojo, true);
		}
	};

	private static Object _clone(Object bean, boolean deep) {
		if (bean == null) {
			return null;
		}
		if (bean instanceof DeepCloneable) {
			return CloneUtils.clone(bean, deep);
		}
		Class<?> clz = bean.getClass();
		Cloner cl = BEAN_CLONERS.get(bean.getClass());
		if (cl != null) {
			return cl.clone(bean, deep);
		}
		if (clz.isArray()) {
			return cloneArray(bean,deep);
		}
		return bean;
	}

	private static Cloner getCloner(Class<?> clz) {
		Cloner cloner = BEAN_CLONERS.get(clz);
		if (cloner != null)
			return cloner;
		if(isStateLessType(clz)){
			cloner = Cloner.RAW;
		}else{
			cloner = new BeanCloner(BeanCopier.create(clz, clz, true));
		}
		BEAN_CLONERS.putIfAbsent(clz, cloner);
		return cloner;
	}

	
	public static boolean isStateLessType(Class<?> cls){
		if(cls.isPrimitive())return true;
		if(cls.getName().startsWith("java.lang."))return true;
		if(cls.getName().startsWith("java.io."))return true;
		if(cls.isEnum())return true;
		return false;
	}
	
	
	private static Object cloneArray(Object obj,boolean deep) {
		int len = Array.getLength(obj);
		Class<?> priType = obj.getClass().getComponentType();
		Object clone = Array.newInstance(priType, len);

		if (priType.isPrimitive()) {
			System.arraycopy(obj, 0, clone, 0, len);
			return clone;
		}
		for (int i = 0; i < len; i++) {
			Array.set(clone, i, _clone(Array.get(obj, i),deep));
		}
		return clone;
	}

}
