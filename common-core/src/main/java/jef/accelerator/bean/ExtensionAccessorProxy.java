package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jef.tools.reflect.Property;

/**
 * 这是一个无法直接使用的BeanAccessor，对应于一个模板类。
 * 一个模板类由若干静态属性和若干动态属性构成。
 * 由于动态属性的模板名称未确定前，是无法计算出需要对应的属性的，因此本类中的方法大多不能用，需要绑定到一个模板名称后才能使用
 * @author jiyi
 *
 */
final class ExtensionAccessorProxy extends BeanAccessor {
	private BeanAccessor accessor;
	private BeanExtensionProvider extensionProvider;
	private final Map<String,BeanAccessor> cache=new ConcurrentHashMap<String,BeanAccessor>();
	
	/**
	 * 将当前Bean绑定后形成实例。
	 * @param templateName
	 * @return
	 */
	public BeanAccessor bind(String templateName){
		BeanAccessor ba=cache.get(templateName);
		if(ba!=null)return ba;
		ba=new ExtensionAccessor(accessor, templateName, extensionProvider);
		cache.put(templateName, ba);
		return ba;
	}
	
	/**
	 * 构造
	 * 
	 * @param raw
	 * @param provider
	 */
	public ExtensionAccessorProxy(BeanAccessor raw, BeanExtensionProvider provider) {
		this.accessor = raw;
		this.extensionProvider = provider;
	}

	/**
	 * 未知Bean的模板对应的Bean时，无法形成对应的属性清单
	 */
	@Override
	public Collection<String> getPropertyNames() {
		throw new UnsupportedOperationException();
	}
	@Override
	public Class<?> getPropertyType(String name) {
		throw new UnsupportedOperationException();
	}
	@Override
	public Type getGenericType(String name) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnField(String name) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnGetter(String name) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnSetter(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object newInstance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Class<?> getType() {
		return accessor.getType();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void initAnnotations(IdentityHashMap<Class, Annotation>[] field, IdentityHashMap<Class, Annotation>[] getter, IdentityHashMap<Class, Annotation>[] setter) {
		accessor.initAnnotations(field, getter, setter);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void initNthGenericType(int index, Class raw, Type type, int total, String fieldName) {
		accessor.initNthGenericType(index, raw, type, total, fieldName);
	}

	@Override
	public Object getProperty(Object bean, String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setProperty(Object bean, String name, Object v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void copy(Object o1, Object o2) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Property getProperty(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<? extends Property> getProperties() {
		throw new UnsupportedOperationException();
	}

}
