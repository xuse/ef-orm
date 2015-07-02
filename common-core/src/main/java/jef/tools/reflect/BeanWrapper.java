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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.accelerator.bean.BeanAccessor;
import jef.accelerator.bean.FastBeanWrapperImpl;
import jef.common.log.LogUtil;

/**
 * 抽象类，用于实现对java bean的反射操作
 * @author Administrator
 *
 */
public abstract class BeanWrapper {
	public static final int NORMAL = 0;
	public static final int METHOD = 1;
	public static final int FAST = 2;
	
	public BeanWrapper(Object obj){}
	
	/**
	 *  是否属性
	 */
	public abstract boolean isProperty(String fieldName);
	
	/**
	 * 判断属性可读
	 */
	public abstract boolean isReadableProperty(String fieldName);

	/**
	 * 是否可写属性
	 * @param fieldName
	 * @return
	 */
	public abstract boolean isWritableProperty(String fieldName);
	
	/**
	 * 返回属性的泛型类型（泛型支持）
	 * @param fieldName
	 * @return
	 */
	public abstract Type getPropertyType(String fieldName);
	
	/**
	 * 得到被包装的对象
	 * @return
	 */
	public abstract Object getWrapped();
	/**
	 * 得到被包装的对象类型
	 * @return
	 */
	public abstract String getClassName();
	/**
	 * 得到值
	 *TODO 目前当属性不存在时的行为还不够统一，下面要设法统一
	 * @param name
	 * @return
	 * @throws NoSuchElementException 如果该属性不存在，抛出NoSuchElementException。
	 */
	public abstract Object getPropertyValue(String name);
	/**
	 * 设置值
	 * @param fieldName
	 * @param newValue
	 */
	public abstract void setPropertyValue(String fieldName, Object newValue);
	/**
	 * 得到全部属性名称
	 * @return
	 */
	public abstract Collection<String> getPropertyNames();
	/**
	 * 得到全部可读可写的属性名称
	 * @return
	 */
	public abstract Collection<String> getRwPropertyNames();
	/**
	 * 得到属性访问句柄
	 * @param name
	 * @return
	 */
	public abstract Property getProperty(String name);
	/**
	 * 得到所有属性访问句柄
	 * @return
	 */
	public abstract Collection<? extends Property> getProperties();

	/**
	 * 得到位于field上的annotation
	 * @param name
	 * @param clz
	 * @return
	 */
	public abstract <T extends Annotation> T getAnnotationOnField(String name,Class<T> clz);
	/**
	 * 得到位于getter方法上的annotation
	 * @param name
	 * @param clz
	 * @return
	 */
	public abstract <T extends Annotation> T getAnnotationOnGetter(String  name,Class<T> clz);
	/**
	 * 得到位于setter方法上的annotation
	 * @param name
	 * @param clz
	 * @return
	 */
	public abstract <T extends Annotation> T getAnnotationOnSetter(String name,Class<T> clz);
	
	/**
	 * 根据字符串来设置属性的值
	 * @param fieldName
	 * @param value
	 */
	public final void setPropertyValueByString(String fieldName, String value) {
		Type c=this.getPropertyType(fieldName);
		Object oldValue=null;
		if(this.isReadableProperty(fieldName)){
			oldValue=this.getPropertyValue(fieldName);
		}
		Object v=null;
		try{
			v=ConvertUtils.toProperType(value, new ClassEx(c),oldValue);
		}catch(Exception e){
			LogUtil.error("Error at setting property to "+this.getClassName()+":"+fieldName+" whith value ["+value+"]");
			throw new IllegalArgumentException(e.getMessage());
		}
		this.setPropertyValue(fieldName, v);
	}
	
	/**
	 * 将给定的Bean进行包装，以便于用反射来操作这个bean
	 * @param obj
	 * @param feature
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static BeanWrapper wrap(Object obj,int feature){
		if(obj instanceof Map){
			return new MapWrapper((Map)obj);
		}else if(obj!=null && obj.getClass().isArray()){
			return new ArrayWrapper(obj);
		}
		switch (feature) {
		case FAST:
			return new FastBeanWrapperImpl(obj);
		case METHOD:
			return new BeanWrapperAsMethod(obj);
		case NORMAL:
			return new BeanWrapperImpl(obj);
		default:
			throw new IllegalArgumentException("the feature id " + feature +" not supported!" );
		}
	}
	/**
	 * 把指定的bean包装起来
	 * @param obj
	 * @return
	 */
	public static BeanWrapper wrap(Object obj){
		return wrap(obj,FAST);
	}
	
	public static BeanWrapper wrap(Object obj,BeanAccessor ba){
		return new FastBeanWrapperImpl(obj,ba);
		
	}
	/**
	 * 返回属性类型（无泛型）
	 * @param fieldName
	 * @return
	 */
	public abstract Class<?> getPropertyRawType(String fieldName);
}
