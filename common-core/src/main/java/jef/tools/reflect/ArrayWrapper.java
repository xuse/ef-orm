package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import jef.tools.Assert;

public final class ArrayWrapper extends BeanWrapper{
	private Object bean;
	private Class<?> arrayClass;
	private int length;
	
	public ArrayWrapper(Object obj) {
		super(obj);
		this.bean=obj;
		Class<?> clz=obj.getClass();
		Assert.isTrue(clz.isArray());
		this.arrayClass=clz.getComponentType();
		this.length=Array.getLength(obj);
	}

	@Override
	public boolean isProperty(String fieldName) {
		try{
			int i=Integer.parseInt(fieldName);
			return i<length;
		}catch(NumberFormatException e){
			return false;
		}
	}

	@Override
	public boolean isReadableProperty(String fieldName) {
		return isProperty(fieldName);
	}

	@Override
	public boolean isWritableProperty(String fieldName) {
		return isProperty(fieldName);
	}

	@Override
	public Type getPropertyType(String fieldName) {
		return arrayClass;
	}

	@Override
	public Object getWrapped() {
		return bean;
	}

	@Override
	public String getClassName() {
		return "["+arrayClass.toString();
	}

	@Override
	public Object getPropertyValue(String name) {
		try{
			int i=Integer.parseInt(name);
			if(i<length){
				return Array.get(bean, i);
			}
		}catch(NumberFormatException e){
		}
		throw new NoSuchElementException(name);
	}

	@Override
	public void setPropertyValue(String name, Object newValue) {
		try{
			int i=Integer.parseInt(name);
			if(i<length){
				Array.set(bean, i,newValue);
			}
		}catch(NumberFormatException e){
		}
		throw new NoSuchElementException(name);
	}

	private Collection<String> names;
	
	@Override
	public Collection<String> getPropertyNames() {
		if(names!=null)return names;
		Collection<String> values=new ArrayList<String>();
		for(int i=0;i<length;i++){
			values.add(String.valueOf(i));
		}
		names=values;
		return values;
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		return getPropertyNames();
	}

	@Override
	public Property getProperty(String name) {
		try{
			int i=Integer.parseInt(name);
			if(i<length){
				return new ArrayProperty(i);
			}
		}catch(NumberFormatException e){
		}
		throw new NoSuchElementException(name);
	}

	@Override
	public Collection<? extends Property> getProperties() {
		
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnField(String name, Class<T> clz) {
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnGetter(String name, Class<T> clz) {
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnSetter(String name, Class<T> clz) {
		return null;
	}

	@Override
	public Class<?> getPropertyRawType(String fieldName) {
		return arrayClass;
	}
	
	private final class ArrayProperty implements Property{
		int index;
		public String getName() {
			return String.valueOf(index);
		}

		public boolean isReadable() {
			return true;
		}

		public boolean isWriteable() {
			return true;
		}

		public Object get(Object obj) {
			return Array.get(obj, index);
		}

		public void set(Object obj, Object value) {
			Array.set(obj, index, value);
		}

		public Class<?> getType() {
			return ArrayWrapper.this.arrayClass;
		}

		public Type getGenericType() {
			return ArrayWrapper.this.arrayClass;
		}
		private ArrayProperty(int i){
			this.index=i;
			
		}
		
	}

}
