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
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public final class MapWrapper extends BeanWrapper {
	@SuppressWarnings("rawtypes")
	private Map obj;
	private BeanAccessorMapImpl accessor=BeanAccessorMapImpl.INSTANCE;

	@SuppressWarnings("rawtypes")
	public MapWrapper(Map obj) {
		super(obj);
		this.obj = obj;
	}

	@Override
	public boolean isProperty(String fieldName) {
		return true;
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
		return obj.get(name);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setPropertyValue(String fieldName, Object newValue) {
		obj.put(fieldName, newValue);
	}

	public String findPropertyIgnoreCase(String string) {
		for (Object keystr : obj.keySet()) {
			String key = String.valueOf(keystr);
			if (string.equalsIgnoreCase(key)) {
				return key;
			}
		}
		return null;
	}

	@Override
	public Collection<String> getPropertyNames() {
		String[] s = new String[obj.size()];
		int n = 0;
		for (Object o : obj.keySet()) {
			s[n++] = String.valueOf(o);
		}
		return Arrays.asList(s);
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		return getPropertyNames();
	}

	@Override
	public <T extends Annotation> T getAnnotationOnField(String name,
			Class<T> clz) {
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnGetter(String name,
			Class<T> clz) {
		return null;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnSetter(String name,
			Class<T> clz) {
		return null;
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return accessor.getProperties();
	}

	@Override
	public Property getProperty(String name) {
		return accessor.getProperty(name);
	}
}
