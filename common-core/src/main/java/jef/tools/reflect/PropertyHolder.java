package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * 基于Method反射来实现的Property
 * @author jiyi
 *
 */
public class PropertyHolder implements Property{
	MethodEx getter;
	MethodEx setter;
	FieldEx field;
	private String name;

	PropertyHolder(MethodEx getter,MethodEx setter, FieldEx field,String name) {
		this.getter=getter;
		this.setter=setter;
		this.field = field;
		this.name=name;
	}

	public Method getWriteMethod() {
		if(setter==null)return null;
		return setter.getJavaMethod();
	}

	public Method getReadMethod() {
		if(getter==null)return null;
		return getter.getJavaMethod();
	}

	public String getName() {
		return name;
	}
	
	public <T extends Annotation> T getFieldAnnotation(Class<T> clz){
		if(field==null)return null;
		return field.getAnnotation(clz);
	}
	
	public <T extends Annotation> T getSetterAnnotation(Class<T> clz){
		if(setter==null)return null;
		return setter.getAnnotation(clz);
	}
	
	public <T extends Annotation> T getGetterAnnotation(Class<T> clz){
		if(getter==null)return null;
		return getter.getAnnotation(clz);
	}

	public boolean isReadable() {
		return getter!=null;
	}

	public boolean isWriteable() {
		return setter!=null;
	}

	public Object get(Object obj) {
		try {
			return getter.invoke(obj);
		} catch (RuntimeException e) {
			throw e;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public void set(Object obj, Object value) {
		try {
			setter.invoke(obj,value);
		} catch (RuntimeException e) {
			throw e;
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

	public Class<?> getType() {
		if(field!=null)return field.getType();
		if(getter!=null)return getter.getReturnType();
		return setter.getParameterTypes()[0];
	}

	public Type getGenericType() {
		if(field!=null)return field.getGenericType();
		if(getter!=null)return getter.getGenericReturnType();
		return setter.getGenericParameterTypes()[0];
	}
}
