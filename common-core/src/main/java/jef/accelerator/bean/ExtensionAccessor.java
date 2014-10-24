package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import jef.tools.reflect.Property;

final class ExtensionAccessor extends BeanAccessor {
	private BeanAccessor accessor;
	/**
	 * 扩展属性列表
	 */
	private final Map<String, Property> extProperties;
	/**
	 * 混合后的属性列表
	 */
	private final Collection<String> allPropNames;
	
	private final Collection<Property> allProps;

	/**
	 * 构造
	 * @param raw
	 * @param provider
	 */
	public ExtensionAccessor(BeanAccessor raw, String extensionName,BeanExtensionProvider provider) {
		this.accessor = raw;
		this.extProperties=provider.getExtensionProperties(raw.getType(),extensionName);
		Collection<String> rawNames=accessor.getPropertyNames();
		Collection<? extends Property> rawProperties=accessor.getProperties();
		List<String> mergeNames=new ArrayList<String>(rawNames.size()+extProperties.size());
		List<Property> mergeProperties=new ArrayList<Property>(rawNames.size()+extProperties.size());
		mergeNames.addAll(rawNames);
		mergeNames.addAll(extProperties.keySet());
		mergeProperties.addAll(rawProperties);
		mergeProperties.addAll(extProperties.values());
		allPropNames=mergeNames;
		allProps=mergeProperties;
	}

	@Override
	public Collection<String> getPropertyNames() {
		return allPropNames;
	}

	@Override
	public Class<?> getPropertyType(String name) {
		Property pp = extProperties.get(name);
		return pp == null ? accessor.getPropertyType(name) : pp.getType();
	}

	@Override
	public Type getGenericType(String name) {
		Property pp = extProperties.get(name);
		return pp == null ? accessor.getGenericType(name) : pp.getGenericType();
	}

	@Override
	public Object getProperty(Object bean, String name) {
		Property pp = extProperties.get(name);
		return pp == null ? accessor.getProperty(bean, name) : pp.get(bean);
	}

	@Override
	public boolean setProperty(Object bean, String name, Object v) {
		Property pp = extProperties.get(name);
		if (pp == null) {
			return accessor.setProperty(bean, name, v);
		} else {
			pp.set(bean, v);
			return true;
		}
	}

	@Override
	public void copy(Object o1, Object o2) {
		accessor.copy(o1, o2);
		for (Property pp : extProperties.values()) {
			pp.set(o2, pp.get(o1));
		}
	}

	@Override
	public Property getProperty(String name) {
		Property pp = extProperties.get(name);
		return pp == null ? accessor.getProperty(name) : pp;
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return allProps;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnField(String name) {
		return accessor.getAnnotationOnField(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnGetter(String name) {
		return accessor.getAnnotationOnGetter(name);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public IdentityHashMap<Class, Annotation> getAnnotationOnSetter(String name) {
		return accessor.getAnnotationOnSetter(name);
	}

	@Override
	public Object newInstance() {
		return accessor.newInstance();
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

}
