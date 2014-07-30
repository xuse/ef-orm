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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.ReflectionException;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;

/**
 * 拷贝工具，基于反射，可以对任何对象实施Clone.
 * 可以实现 浅拷贝、深拷贝
 * 在拷贝过程中，还可以实现集合容器替换， 实现类替换等功能。
 * @author Administrator
 *
 */
public class CloneUtils {
	/**
	 * 克隆配置
	 */
	public static class CloneConfig{
		private static final HashMap<String,Class<?>> EMPTY=new HashMap<String,Class<?>>(0);
		
		/**
		 * 类替换表，深拷贝时发现前面的类的实例，会自动替换为后面的类实例，属性按相同field方式注入
		 */
		private Map<String,Class<?>> replaceMap;
		private Set<Class<?>> statelessType = new HashSet<Class<?>>();
		
		/**
		 * 拷贝的层次，层次无限大即为深拷贝，层次为零即为浅拷贝
		 * 默认为1，即复制对象本身，以及对象本身内的属性。
		 * 注意：当对象内部的属性有List,Array,Map等形式时，容器本身作为一个层次。
		 * 即当层次为1时，会创建一个新的容器，但容器内部的数据依然是原来的对象本身。
		 */
		private int deepCopy = 1;
		
		/**
		 * bean拷贝的实现方式，true表示基于相同的field name, false表示基于相同的get/set方法
		 * 默认按照相同的Field名称拷贝。
		 */
		boolean fieldCopy=true;
		
		public Map<String, Class<?>> getReplaceMap() {
			if(replaceMap==null)return EMPTY;
			return replaceMap;
		}
		
		public Set<Class<?>> getStatelessType() {
			return statelessType;
		}

		public void setStatelessType(Set<Class<?>> statelessType) {
			this.statelessType = statelessType;
		}
		
		/**
		 * 增加一个类型，不可变对象的类型，被制定类型的对象不会被克隆，原值返回
		 * @param clz
		 */
		public void addStatelessType(Class<?> clz){
			this.statelessType.add(clz);
		}

		/**
		 * 设置替换类列表，描述哪些类将被替换为哪些类
		 * @return
		 */
		public void setReplaceMap(Map<String, Class<?>> replaceMap) {
			this.replaceMap = replaceMap;
		}
		public int isDeepCopy() {
			return deepCopy;
		}
		public void setDeepCopy(int deepCopy) {
			this.deepCopy = deepCopy;
		}
//		//默认的集合ReplaceMap配置，凡是拷贝中发现的集合方法，都是用此列表进行实现
		public static final Map<String,Class<?>> simpleCollection=new HashMap<String,Class<?>>();
		static{
			simpleCollection.put("map", HashMap.class);
			simpleCollection.put("list", ArrayList.class);
			simpleCollection.put("set", HashSet.class);
		}
		public boolean isFieldCopy() {
			return fieldCopy;
		}
		public void setFieldCopy(boolean fieldCopy) {
			this.fieldCopy = fieldCopy;
		}
		public CloneConfig(){}
		public CloneConfig(int deepLevel){
			this.deepCopy=deepLevel;
		}
	}
	
	/**
	 * 常量：拷贝层次不限（深拷贝）
	 * 为了防止出现死循环 
	 */
	public static final CloneConfig DEEP_COPY=new CloneConfig(10);
	/**
	 * 常量：拷贝对象本身，内部数据不拷贝（浅拷贝）
	 */
	public static final CloneConfig SHADOW_COPY=new CloneConfig(0);
	
	public static <T> T clone(T obj){
		return clone(obj,new CloneConfig());
	}
	
	public static <T> T clone(T obj,CloneConfig config){
		return clone(obj,config,0);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static <T> T clone(T obj,CloneConfig config,int currentLevel){
		if(obj==null)return null;
		Class<?> cls=obj.getClass();
		try{
			//如果是指定的非可变对象，直接返回
			if(config.getStatelessType().contains(cls)){
				return obj;
			}
			
			//数组
			if(cls.isArray())return (T)cloneArray(obj,config,currentLevel);
			//Map
			if(obj instanceof Map){
				return (T) cloneMap((Map)obj,config,currentLevel);
			}	
			//Collection
			if(obj instanceof Collection){
				return (T) cloneCollection((Collection)obj,config,currentLevel);
			}
			//日期型的克隆返回
			if(obj instanceof Date){
				return (T) new Date(((Date)obj).getTime());
			}

			//不处理的拷贝，一般来说java.lang包下的对象多数是不可变对象，无需拷贝
			if(isStateLessType(cls))return obj;
	

			//处理java bean
			if(config.getReplaceMap().containsKey(cls.getName())){
				return (T)copyOtherClassFields(obj,config,cls,currentLevel);
			}else{
				return (T)copySameClassFields(obj,config,cls,currentLevel);
			}
		} catch (ReflectionException e) {
			throw new RuntimeException (e);
		}
	}
	
	public static boolean isStateLessType(Class<?> cls){
		if(cls.isPrimitive())return true;
		if(cls.getName().startsWith("java.lang."))return true;
		if(cls.getName().startsWith("java.io."))return true;
		if(cls.isEnum())return true;
		return false;
	}

	private static Object copySameClassFields(Object obj,CloneConfig config,Class<?> cls,int currentLevel) throws ReflectionException {
		Object o=BeanUtils.newInstance(cls);
		if(o==null){
			throw new ReflectionException(null,cls.toString() +"Can't be instace.");
		}
		for(FieldEx f:BeanUtils.getFields(cls)){
			try {
				Object value=f.get(obj);
				if(config.isDeepCopy()>currentLevel){
					value=clone(value,config,currentLevel+1);
				}
				f.set(o, value);
			} catch (IllegalArgumentException e) {
				LogUtil.exception(e);
				throw new ReflectionException(e);
			}
		}
		return o;
	}

	private static Object copyOtherClassFields(Object obj,CloneConfig config,Class<?> cls,int currentLevel) throws ReflectionException {
		Object o=BeanUtils.newInstance(config.getReplaceMap().get(cls.getName()));
		if(config.isFieldCopy()){//基于Field的值拷贝
			BeanWrapperImpl bean=new BeanWrapperImpl(o,true);
			for(FieldEx f:BeanUtils.getFields(cls)){
				if(!bean.isProperty(f.getName()))continue;
				//校验两侧数据类型能否相容
				Class<?> source=f.getType();
				Class<?> target=GenericUtils.getRawClass(bean.getPropertyType(f.getName()));
				if(source.isPrimitive())source=BeanUtils.toWrapperClass(source);
				if(target.isPrimitive())target=BeanUtils.toWrapperClass(target);
				if(!target.isAssignableFrom(source)){//目标不能兼容原类型
					continue;
				}
				try {
					Object value=f.get(obj);
					if(config.isDeepCopy()>currentLevel){
						value=clone(value,config,currentLevel+1);
					}
					bean.setPropertyValue(f.getName(), value);
				} catch (IllegalArgumentException e) {
					throw new ReflectionException(e);
				}
			}	
		}else{//基于get/set方式的值拷贝
			BeanWrapperAsMethod source=new BeanWrapperAsMethod(obj);
			BeanWrapperAsMethod target=new BeanWrapperAsMethod(o);
			for(Property p:source.getProperties()){
				if(!source.isReadableProperty(p.getName()))continue;
				if(!target.isWritableProperty(p.getName()))continue;
				try {
					Object value=source.getPropertyValue(p.getName());
					if(config.isDeepCopy()>currentLevel){
						value=clone(value,config,currentLevel+1);
					}
					target.setPropertyValue(p.getName(), value);
				} catch (IllegalArgumentException e) {
					throw new ReflectionException(e);
				}
			}
		}
		return o;
	}

	/**
	 * 克隆Map
	 * @param obj
	 * @param config
	 * @param currentLevel
	 * @return
	 * @throws ReflectionException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object cloneMap(Map obj, CloneConfig config,int currentLevel)throws ReflectionException  {
		Class<?> cls=obj.getClass();
		
		Map result=null;
		Map<String, Class<?>> replace=config.getReplaceMap();
		if(replace.containsKey("map")){
			result=(Map) BeanUtils.newInstance(replace.get("map"));
		}else if(replace.containsKey(cls.getName())){
			result=(Map) BeanUtils.newInstance(replace.get(cls.getName()));
		}else{
			result=(Map) BeanUtils.newInstance(cls);			
		}
		if(config.isDeepCopy()>currentLevel){
			for(Object key: obj.keySet()){
				result.put(key, clone(obj.get(key),config));
			}
		}else{
			result.putAll(obj);	
		}
		return result;
	}

	/**
	 * 克隆集合
	 * @param obj
	 * @param config
	 * @param currentLevel
	 * @return
	 * @throws ReflectionException
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object cloneCollection(Collection obj, CloneConfig config,int currentLevel) throws ReflectionException {
		Class<?> cls=obj.getClass();
		Collection result=null;
		Map<String, Class<?>> replace=config.getReplaceMap();
		if(replace.containsKey("set") && obj instanceof Set){
			result=(Collection) BeanUtils.newInstance(replace.get("set"));
		}else if(replace.containsKey("list") && obj instanceof Set){
			result=(Collection) BeanUtils.newInstance(replace.get("list"));
		}else if(replace.containsKey(cls.getName())){
			result=(Collection) BeanUtils.newInstance(replace.get(cls.getName()));
		}else{
			result=(Collection) BeanUtils.newInstance(cls);
		}
		
		if(config.isDeepCopy()>currentLevel){
			for(Object key: obj){
				result.add(clone(key,config,currentLevel+1));
			}
		}else{
			result.addAll(obj);
		}
		return result;
	}
	
	/**
	 * 克隆数组
	 * @param obj
	 * @param config
	 * @param currentLevel
	 * @return
	 * @throws ReflectionException
	 */
	private static Object cloneArray(Object obj,CloneConfig config,int currentLevel) throws ReflectionException{
		Assert.isTrue(obj.getClass().isArray());
		Class<?> priType=obj.getClass().getComponentType();
		
		if (priType == Boolean.TYPE) {
			return ArrayUtils.clone((boolean[])obj);
		} else if (priType == Byte.TYPE) {
			return ArrayUtils.clone((byte[])obj);
		} else if (priType == Character.TYPE) {
			return ArrayUtils.clone((char[])obj);
		} else if (priType == Integer.TYPE) {
			return ArrayUtils.clone((int[])obj);
		} else if (priType == Long.TYPE) {
			return ArrayUtils.clone((long[])obj);
		} else if (priType == Float.TYPE) {
			return ArrayUtils.clone((float[])obj);
		} else if (priType == Double.TYPE) {
			return ArrayUtils.clone((double[])obj);
		} else if (priType == Short.TYPE) {
			return ArrayUtils.clone((short[])obj);
		} else {
			int len=((Object[]) obj).length;
			Object result=Array.newInstance(priType, len);
			if(config.isDeepCopy()>currentLevel){
				Object[] source=(Object[])obj;
				Object[] results=(Object[])result;
				for(int i=0;i<len;i++){
					results[i]=CloneUtils.clone(source[i], config, currentLevel+1);
				}
			}else{
				System.arraycopy(obj, 0, result, 0, len);
			}
			return result;
		}
	}
}
