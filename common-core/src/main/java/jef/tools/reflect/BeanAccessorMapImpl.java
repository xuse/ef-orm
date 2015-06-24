package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import jef.accelerator.bean.BeanAccessor;
import jef.tools.reflect.MapWrapper.MapProperty;

@SuppressWarnings("rawtypes")
public final class BeanAccessorMapImpl extends BeanAccessor{
	public static final BeanAccessorMapImpl INSTANCE=new BeanAccessorMapImpl();
	
	
	private BeanAccessorMapImpl(){
	}
	
	public Collection<String> getPropertyNames() {
		throw new UnsupportedOperationException();
	}

	public Class<?> getPropertyType(String name) {
		return Object.class;
	}

	public Type getGenericType(String name) {
		return Object.class;
	}

	public Object getProperty(Object bean, String name) {
		return ((Map)bean).get(name);
	}

	@SuppressWarnings("unchecked")
	public boolean setProperty(Object bean, String name, Object v) {
		((Map)bean).put(name,v);
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void copy(Object o1, Object o2) {
		Map m1=(Map)o1;
		Map m2=(Map)o2;
		m2.clear();
		m2.putAll(m1);
	}

	public Property getProperty(String name) {
		return new MapProperty(name);
	}

	public Collection<? extends Property> getProperties() {
		throw new UnsupportedOperationException();
	}

	public IdentityHashMap<Class<?>, Annotation> getAnnotationOnField(String name) {
		return null;
	}

	public IdentityHashMap<Class<?>, Annotation> getAnnotationOnGetter(String name) {
		return null;
	}

	public IdentityHashMap<Class<?>, Annotation> getAnnotationOnSetter(String name) {
		return null;
	}

	public void initAnnotations(IdentityHashMap<Class<?>, Annotation>[] field, IdentityHashMap<Class<?>, Annotation>[] getter, IdentityHashMap<Class<?>, Annotation>[] setter) {
	}

	public void initNthGenericType(int index, Class<?> raw, Type type, int total, String fieldName) {
	}

	@Override
	public Object newInstance() {
		return new HashMap();
	}

	@Override
	public Class<?> getType() {
		return Map.class;
	}

	@Override
	public Map<String, Object> convert(Object obj) {
		return (Map)obj;
	}
}
