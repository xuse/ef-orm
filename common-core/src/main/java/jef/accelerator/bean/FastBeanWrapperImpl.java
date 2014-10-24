package jef.accelerator.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.tools.reflect.BeanAccessorMapImpl;
import jef.tools.reflect.BeanWrapper;
import jef.tools.reflect.Property;

import org.springframework.util.Assert;

/**
 * 快速Bean访问器实现
 * @author jiyi
 *
 */
public final class FastBeanWrapperImpl extends BeanWrapper{
	private static final BeanAccessorFactory bf=new ASMAccessorFactory();
	private Object obj;
	private BeanAccessor accessor;
	
	
	/**
	 * 注册一个扩展属性提供器
	 * @param prov
	 */
	public static void registerBeanExtensionProvider(BeanExtensionProvider prov){
		Assert.notNull(prov);
		bf.registerExtensionProvider(prov);
	}
	
	public static final BeanAccessor getAccessorFor(Class<?> clz){
		if(Map.class.isAssignableFrom(clz))return BeanAccessorMapImpl.INSTANCE;
		return bf.getBeanAccessor(clz);
	}
	
	public static final BeanAccessor getAccessorFor(Class<?> clz,String extensionName){
		BeanAccessor ba=bf.getBeanAccessor(clz);
		return ba.bind(extensionName);
	}
	
	public FastBeanWrapperImpl(Object obj) {
		super(obj);
		this.accessor=bf.getBeanAccessor(obj.getClass());
		this.obj=obj;
	}

	@Override
	public boolean isProperty(String fieldName) {
		return accessor.getPropertyNames().contains(fieldName);
	}

	@Override
	public boolean isReadableProperty(String fieldName) {
		return accessor.getPropertyNames().contains(fieldName);
	}

	@Override
	public boolean isWritableProperty(String fieldName) {
		return accessor.getPropertyNames().contains(fieldName);
	}

	@Override
	public Type getPropertyType(String fieldName) {
		return accessor.getGenericType(fieldName);
	}

	@Override
	public Class<?> getPropertyRawType(String fieldName) {
		return accessor.getPropertyType(fieldName);
	}

	@Override
	public Object getWrapped() {
		return obj;
	}

	@Override
	public String getClassName() {
		return obj.getClass().getName();
	}

	@Override
	public Object getPropertyValue(String name) {
		return accessor.getProperty(obj, name);
	}

	@Override
	public void setPropertyValue(String fieldName, Object newValue) {
		boolean flag=accessor.setProperty(obj, fieldName, newValue);
		if(!flag){
			throw new NoSuchElementException("There's no accessable field "+ fieldName +" in bean "+getClassName());
		}	
	}

	@Override
	public Collection<String> getPropertyNames() {
		return accessor.getPropertyNames();
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		return accessor.getPropertyNames();
	}

	@Override
	public Property getProperty(String name) {
		return accessor.getProperty(name);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends Annotation> T getAnnotationOnField(String name, Class<T> clz) {
		IdentityHashMap<Class,Annotation> map=accessor.getAnnotationOnField(name);
		return map==null?null:(T)map.get(clz);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends Annotation> T getAnnotationOnGetter(String name, Class<T> clz) {
		IdentityHashMap<Class,Annotation> map=accessor.getAnnotationOnGetter(name);
		return map==null?null:(T)map.get(clz);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends Annotation> T getAnnotationOnSetter(String name, Class<T> clz) {
		IdentityHashMap<Class,Annotation> map=accessor.getAnnotationOnSetter(name);
		return map==null?null:(T)map.get(clz);
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return accessor.getProperties();
	}
}
