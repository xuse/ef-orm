package jef.tools.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
@SuppressWarnings("unchecked")
public final class NopBeanWrapper extends BeanWrapper{
	private static NopBeanWrapper instance=new NopBeanWrapper();
	
	public static NopBeanWrapper getInstance(){
		return instance;
	}
	
	private NopBeanWrapper(){
		super(null);
	};
	
	@Override
	public boolean isProperty(String fieldName) {
		return false;
	}

	@Override
	public boolean isReadableProperty(String fieldName) {
		return false;
	}

	@Override
	public boolean isWritableProperty(String fieldName) {
		return false;
	}

	@Override
	public Type getPropertyType(String fieldName) {
		return null;
	}

	@Override
	public Object getWrapped() {
		return null;
	}

	@Override
	public String getClassName() {
		return "java.lang.Void";
	}

	@Override
	public Object getPropertyValue(String name) {
		return null;
	}

	@Override
	public void setPropertyValue(String fieldName, Object newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<String> getPropertyNames() {
		return Collections.EMPTY_SET;
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		return Collections.EMPTY_SET;
	}

	@Override
	public Property getProperty(String name) {
		return null;
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return Collections.EMPTY_SET;
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
		return null;
	}
}
