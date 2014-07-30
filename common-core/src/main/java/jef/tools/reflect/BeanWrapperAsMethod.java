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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import jef.common.log.LogUtil;
import jef.tools.StringUtils;

public final class BeanWrapperAsMethod extends BeanWrapper{
	private static Map<Class<?>,Map<String, PropertyHolder>> cache=new HashMap<Class<?>,Map<String, PropertyHolder>>();
	
	private Object obj;
	private Map <String, PropertyHolder> properties=null;  
	
	public BeanWrapperAsMethod(Object obj) {
		super(obj);
		Map<String,MethodEx> methods=new HashMap<String,MethodEx>();
		this.obj=obj;
		Class<?> c=obj.getClass();
		if(cache.containsKey(c)){
			properties=cache.get(c);
			return;
		}
		
		properties=new HashMap<String, PropertyHolder>();
		List<String> ll=new ArrayList<String>();
		for(MethodEx m: new ClassWrapper(c).getMethods()){
			if(Modifier.isStatic(m.getModifiers())){
				continue;
			}
			String methodName=m.getName();
			if("getClass".equals(methodName))continue;
			if(methodName.startsWith("get")){
				if(m.getParameterTypes().length==0){
					String mField=StringUtils.uncapitalize(StringUtils.substringAfter(methodName, "get"));
					methods.put("get"+mField, m);
					if(!ll.contains(mField))ll.add(mField);
				}
			}else if(methodName.startsWith("is")){
				if(m.getParameterTypes().length==0){
					String mField=StringUtils.uncapitalize(StringUtils.substringAfter(methodName, "is"));
					methods.put("get"+mField, m);
					if(!ll.contains(mField))ll.add(mField);
				}
			}else if(methodName.startsWith("set")){
				String mField=StringUtils.uncapitalize(StringUtils.substringAfter(methodName, "set"));
				if(m.getParameterTypes().length==1){
					mField=StringUtils.uncapitalize(StringUtils.substringAfter(methodName, "set"));
					methods.put("set"+mField, m);
					if(!ll.contains(mField))ll.add(mField);
				}
			}
		}
		for(String mField:ll){
			if(StringUtils.isNotEmpty(mField)){
				PropertyHolder pp = new PropertyHolder(methods.get("get"+mField),methods.get("set"+mField),null,mField);
				properties.put(mField,pp);	
			}
		}
		cache.put(c, properties);
	}
	
	public boolean isReadableProperty(String fieldName) {
		PropertyHolder pp=properties.get(fieldName);
		if(pp==null)return false;
		return pp.getReadMethod()!=null;
	}
	
	public boolean isWritableProperty(String fieldName) {
		if(fieldName==null)return false;
		PropertyHolder pp=properties.get(fieldName);
		if(pp==null)return false;
		return pp.getWriteMethod()!=null;
	}
	
	public Object getPropertyValue(String fieldName) {
		PropertyHolder pp=properties.get(fieldName);
		if(pp==null)throw new NullPointerException(fieldName + " is not exist!");
		Method m=pp.getReadMethod();
		if(m==null)throw new NullPointerException();
		try {
			return m.invoke(obj, new Object[]{});
		} catch (IllegalArgumentException e) {
			LogUtil.exception(e);
			return null; 
		} catch (IllegalAccessException e) {
			LogUtil.exception(e);
			return null;
		} catch (InvocationTargetException e) {
			LogUtil.exception(e);
			return null;
		}
	}
	
	public void setPropertyValue(String fieldName, Object newValue) {
		PropertyHolder pp=properties.get(fieldName);
		if(pp==null)throw new NullPointerException("Can not find property '"+fieldName+"' in bean "+ obj.getClass().getName());
		Method m=pp.getWriteMethod();
		if(m==null)throw new NullPointerException("Can not find set method '"+fieldName+"' in bean "+ obj.getClass().getName());
		try {
			m.invoke(obj, new Object[]{newValue});
		} catch (IllegalArgumentException e) {
			StringBuilder sb=new StringBuilder("IllegalArgumentException:").append(e.getLocalizedMessage()).append('\n');
			sb.append(obj.getClass().getName()).append('\t').append(fieldName).append('\t').append(newValue.getClass().getName());
			throw new IllegalArgumentException(sb.toString());
		} catch (IllegalAccessException e) {
			StringBuilder sb=new StringBuilder("IllegalAccessException:").append(e.getLocalizedMessage()).append('\n');
			sb.append(obj.getClass().getName()).append('\t').append(fieldName).append('\t').append(newValue);
			throw new IllegalArgumentException(sb.toString());
		} catch (InvocationTargetException e) {
			StringBuilder sb=new StringBuilder("InvocationTargetException:").append(e.getLocalizedMessage()).append('\n');
			sb.append(obj.getClass().getName()).append('\t').append(fieldName).append('\t').append(newValue);
			throw new IllegalArgumentException(sb.toString());
		}
	}

	@Override
	public Collection<? extends Property> getProperties() {
		return properties.values();
	}

	public String getFieldIgnoreCase(String string) {
		if(properties.containsKey(string))return string;
		for(String field: properties.keySet()){
			if(field.equalsIgnoreCase(string))return field;
		}
		return null;
	}

	public Class<?> getFieldType(String fieldName) {
		PropertyHolder pp=properties.get(fieldName);
		if(pp==null)throw new NullPointerException("Can not find PropertyHolder for field: " + fieldName);
		Method m=pp.getWriteMethod();
		if(m==null)throw new NullPointerException("Can not find WriterMethod for field: " + fieldName);
		Class<?>[] cs=m.getParameterTypes();
		return cs[0];
	}

	@Override
	public boolean isProperty(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		return pp != null;

	}

	@Override
	public Type getPropertyType(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)throw new NoSuchElementException(fieldName+" not found in bean "+ this.obj.getClass().getName());
		return pp.getGenericType();
	}
	
	@Override
	public Class<?> getPropertyRawType(String fieldName) {
		PropertyHolder pp = properties.get(fieldName);
		if (pp == null)throw new NoSuchElementException(fieldName+" not found in bean "+ this.obj.getClass().getName());
		return pp.getType();
	}

	@Override
	public Object getWrapped() {
		return obj;
	}

	@Override
	public String getClassName() {
		return obj.getClass().getName();
	}

	public String findPropertyIgnoreCase(String string) {
		if (properties.containsKey(string))
			return string;
		for (String field : properties.keySet()) {
			if (field.equalsIgnoreCase(string))
				return field;
		}
		return null;
	}

	@Override
	public Collection<String> getPropertyNames() {
		return this.properties.keySet();
	}

	@Override
	public Collection<String> getRwPropertyNames() {
		ArrayList<String> list=new ArrayList<String>();
		for(PropertyHolder pp: properties.values()){
			if(pp.getReadMethod()!=null && pp.getWriteMethod()!=null){
				list.add(pp.getName());
			}
		}
		return list;
	}

	@Override
	public <T extends Annotation> T getAnnotationOnField(String name,
			Class<T> clz) {
		FieldEx field=BeanUtils.getField(obj.getClass(), name);
		if(field==null)return null;
		return field.getAnnotation(clz);
	}

	@Override
	public <T extends Annotation> T getAnnotationOnGetter(String name,
			Class<T> clz) {
		PropertyHolder pp = properties.get(name);
		if (pp == null)return null;
		Method method=pp.getReadMethod();
		if(method==null)return null;
		return method.getAnnotation(clz);
	}

	@Override
	public <T extends Annotation> T getAnnotationOnSetter(String name,
			Class<T> clz) {
		PropertyHolder pp = properties.get(name);
		if (pp == null)return null;
		Method method=pp.getWriteMethod();
		if(method==null)return null;
		return method.getAnnotation(clz);
	}

	@Override
	public Property getProperty(String name) {
		return properties.get(name);
	}
}
