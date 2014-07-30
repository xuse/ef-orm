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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


public final class MapWrapper extends BeanWrapper{
	@SuppressWarnings("rawtypes")
	private Map obj;
	private Collection<MapProperty> properties;
//	private Type keyType=Object.class;
	private static Type valueType=Object.class;
	
	static final class MapProperty implements Property{
		private String name;
		public String getName() {
			return name;
		}
		MapProperty(String name){
			this.name=name;
		}
		public boolean isReadable() {
			return true;
		}
		public boolean isWriteable() {
			return true;
		}
		@SuppressWarnings("rawtypes")
		public Object get(Object obj) {
			return ((Map)obj).get(name);
		}
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void set(Object obj, Object value) {
			((Map)obj).put(name, value);
		}
		public Class<?> getType() {
			return GenericUtils.getRawClass(valueType);
		}
		public Type getGenericType() {
			return valueType;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public MapWrapper(Map obj){
		super(obj);
		this.obj=obj;
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
		return valueType;
	}

	@Override
	public Class<?> getPropertyRawType(String fieldName) {
		return GenericUtils.getRawClass(valueType);
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
		for(Object keystr:obj.keySet()){
			String key=String.valueOf(keystr);
			if(string.equalsIgnoreCase(key)){
				return key;
			}
		}
		return null;
	}

	@Override
	public Collection<String> getPropertyNames() {
		String[] s=new String[obj.size()];
		int n=0;
		for(Object o:obj.keySet()){
			s[n++]=String.valueOf(o);
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
		if(properties!=null)return properties;
		Collection<String> names=this.getPropertyNames();
		List<MapProperty> pps=new ArrayList<MapProperty>(names.size());
		for(String s:names){
			pps.add(new MapProperty(s));
		}
		properties=pps;
		return properties;
	}

	@Override
	public Property getProperty(String name) {
		return new MapProperty(name);
	}
}
