package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.IdentityHashMap;
import java.util.NoSuchElementException;

import jef.tools.reflect.Property;

/**
 * 使用hash算法的抽象类
 * @author Administrator
 *
 */
public abstract class HashBeanAccessor extends BeanAccessor{
	protected IdentityHashMap<Class<?>, Annotation>[] fieldAnnoMaps;
	protected IdentityHashMap<Class<?>, Annotation>[] getterAnnoMaps;
	protected IdentityHashMap<Class<?>, Annotation>[] setterAnnoMaps;

	public final Class<?> getPropertyType(String name) {
		Property pp=getProperty(name);
		if(pp==null){
			throw new NoSuchElementException(name);
		}
		return pp.getType();
	}
	
	public final Type getGenericType(String name) {
		Property pp=getProperty(name);
		if(pp==null){
			throw new NoSuchElementException(name);
		}
		return pp.getGenericType();
	}

	public final Object getProperty(Object bean, String name) {
		Property pp=getProperty(name);
		if(pp==null){
			throw new NoSuchElementException(name);
		}
		return pp.get(bean);
	}

	public final boolean setProperty(Object bean, String name, Object v) {
		Property pp=getProperty(name);
		if(pp==null)return false;
		if(v==null && pp.getType().isPrimitive()){
			return true; 
						//1 忽略设置操作
						//3 调用defaultValueOfPrimitive，设为默认值
						//4 抛出异常。	
//			throw new IllegalArgumentException("The field "+this.getType().getName()+"."+name+" is primitive, setting null value is invalid.");
		}
		pp.set(bean,v);
		return true;
	}
	
	public final void initAnnotations(IdentityHashMap<Class<?>,Annotation>[] field,IdentityHashMap<Class<?>,Annotation>[] getter,IdentityHashMap<Class<?>,Annotation>[] setter){
		this.fieldAnnoMaps=field;
		this.getterAnnoMaps=getter;
		this.setterAnnoMaps=setter;
	}
	public final void initNthGenericType(int index,Class<?> raw,Type type,int total,String fieldName){
		AbstractFastProperty pp=(AbstractFastProperty)getProperty(fieldName);
		pp.n=index;
		pp.genericType=type;
		pp.rawType=raw;
	}
}
