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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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
	private static ConcurrentMap<Class<?>, Cloner> BEAN_CLONERS = new ConcurrentHashMap<Class<?>, Cloner>(32);

	/**
	 * 常用的克隆策略，预定义好。
	 */
	static {
		BEAN_CLONERS.put(ArrayList.class, Cloner._ArrayList);
		BEAN_CLONERS.put(Arrays.asList().getClass(), Cloner._ArrayList);
		BEAN_CLONERS.put(LinkedList.class, new Cloner._OtherCollection(LinkedList.class));

		BEAN_CLONERS.put(HashSet.class, Cloner._HashSet);
		BEAN_CLONERS.put(TreeSet.class, new Cloner._OtherCollection(TreeSet.class));

		BEAN_CLONERS.put(HashMap.class, Cloner._HashMap);
		BEAN_CLONERS.put(SimpleMap.class, Cloner._HashMap);
		BEAN_CLONERS.put(ConcurrentHashMap.class, new Cloner._OtherMap(ConcurrentHashMap.class));
		BEAN_CLONERS.put(TreeMap.class, new Cloner._OtherMap(TreeMap.class));

		BEAN_CLONERS.put(String.class, Cloner.RAW);
		BEAN_CLONERS.put(Integer.class, Cloner.RAW);
		BEAN_CLONERS.put(Long.class, Cloner.RAW);
		BEAN_CLONERS.put(Short.class, Cloner.RAW);
		BEAN_CLONERS.put(Float.class, Cloner.RAW);
		BEAN_CLONERS.put(Double.class, Cloner.RAW);
		BEAN_CLONERS.put(Boolean.class, Cloner.RAW);
		BEAN_CLONERS.put(Byte.class, Cloner.RAW);
		BEAN_CLONERS.put(Character.class, Cloner.RAW);

		BEAN_CLONERS.put(java.util.Date.class, Cloner.DATE);
		BEAN_CLONERS.put(java.sql.Date.class, Cloner.SQL_DATE);
		BEAN_CLONERS.put(java.sql.Time.class, Cloner.TIME);
		BEAN_CLONERS.put(java.sql.Timestamp.class, Cloner.TIMESTAMP);
	}

	/**
	 * 注册一个自定义的克隆策略。
	 * 默认策略能处理大多数的集合、数组、自定义Bean、常见基本类型等克隆。但如果你有特殊的Bean需要克隆，可以注册自定义的策略。
	 * 
	 * @param type
	 *            要注册的克隆类
	 * @param cloner
	 *            克隆实现。
	 * @return 如果注册的策略替换了系统已注册的某项的策略，那么返回被替换的策略。
	 */
	public static Cloner register(Class<?> type, Cloner cloner) {
		return BEAN_CLONERS.put(type, cloner);
	}

	/**
	 * 给定一个类型吗，返回该类型的克隆策略。
	 * 
	 * @param clz
	 * @return
	 */
	public static Cloner getCloner(Class<?> clz) {
		Cloner cloner = BEAN_CLONERS.get(clz);
		if (cloner != null)
			return cloner;
		if (isStateLessType(clz)) {//
			cloner = Cloner.RAW;
		} else if(clz.isArray()){
			cloner = Cloner._Array;
		} else if (Collection.class.isAssignableFrom(clz)) {// 按未知集合类型处理
			cloner = new Cloner._OtherCollection(clz);
		} else if (Map.class.isAssignableFrom(clz)) {// 按未知Map类型处理
			cloner = new Cloner._OtherMap(clz);
		} else {// 按自定义Bean处理
			cloner = new BeanCloner(BeanCopier.create(clz, clz, true));
		}
		BEAN_CLONERS.putIfAbsent(clz, cloner);
		return cloner;
	}

	/**
	 * 克隆
	 * 
	 * @param obj
	 *            要拷贝的对象
	 * @return 拷贝后的对象，那么除非属性继承了{@link DeepCloneable}接口，否则不会深拷贝。
	 */
	public static Object clone(Object obj) {
		return clone(obj, false);
	}
	
	/**
	 * 克隆
	 * 
	 * @param obj
	 *            要拷贝的对象
	 * @param deep
	 *            对象内的属性是否深拷贝。如果为true则尝试递归向下深拷贝。如为false，那么除非属性继承了
	 *            {@link DeepCloneable}接口，否则不会深拷贝。
	 * @return 拷贝后的对象
	 */
	public static Object clone(Object obj, boolean deep) {
		return _clone(obj, deep?Integer.MAX_VALUE:1);
	}
	
	static Object _clone(Object bean, int restLevel) {
		if (bean == null) {
			return null;
		}
		if (restLevel>0 || bean instanceof DeepCloneable) {
			return getCloner(bean.getClass()).clone(bean, restLevel-1);
		}else{
			return bean;
		}
	}
	
	static final Converter clone_cvt_dummy = new Converter() {
		public Object convert(Object pojo, Class fieldType, Object fieldName) {
			return pojo;
		}
	};

	static final class CloneConvert implements Converter {
		private int restLevel;
		CloneConvert(int restLevel){
			this.restLevel=restLevel;
		}
		@SuppressWarnings("rawtypes")
		public Object convert(Object pojo, Class fieldType, Object fieldName) {
			return _clone(pojo, restLevel);
		}
	};

	public static boolean isStateLessType(Class<?> cls) {
		if (cls.isPrimitive())
			return true;
		if (cls.getName().startsWith("java.lang."))
			return true;
		if (cls.getName().startsWith("java.io."))
			return true;
		if (cls.isEnum())
			return true;
		return false;
	}


}
