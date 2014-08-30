package jef.database.datasource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import jef.database.DbUtils;
import jef.tools.StringUtils;

public class ReflectionProperties extends Properties {
	private Class clz;
	private Object obj;
	
	public ReflectionProperties(Class clz,Object obj){
		this.clz=clz;
		this.obj=obj;
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		try {
			return put0(key,value);
		} catch (Exception e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	private Object put0(Object key, Object value) throws IllegalAccessException, InvocationTargetException {
		for (Method method: clz.getDeclaredMethods()){
			String tmp = null;
			if (method.getName().startsWith("is")){
				tmp = StringUtils.uncapitalize(method.getName().substring(2));
			} else if (method.getName().startsWith("set")){
				tmp = StringUtils.uncapitalize(method.getName().substring(3));
			} else {
				continue;
			}
			if(!StringUtils.equals((String)key, tmp)){
				continue;
			}

			if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(int.class)){
				if (value != null) {
					try{
						return method.invoke(this, Integer.parseInt((String)value));
					} catch (NumberFormatException e){
						// do nothing, use the default value
					}
				}
			} else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(long.class)){
				if (value != null) {
					try{
						return method.invoke(this, Long.parseLong((String)value));
					} catch (NumberFormatException e){
						// do nothing, use the default value
					}
				}
			} else if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(String.class)){
				if (value != null) {
					return method.invoke(this, (String)value);
				}
			} if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(boolean.class)){
				if (value != null) {
					return method.invoke(obj, Boolean.parseBoolean((String)value));
				}
			}
		}
		return null;
	}

	@Override
	public synchronized Object get(Object key) {
		try {
			return get0((String)key);
		} catch (Exception e) {
			throw DbUtils.toRuntimeException(e);
		}
	}

	private Object get0(String key) throws IllegalAccessException, InvocationTargetException {
		for (Method method: clz.getDeclaredMethods()){
			String tmp = null;
			if (method.getName().startsWith("is")){
				tmp = StringUtils.uncapitalize(method.getName().substring(2));
			} else if (method.getName().startsWith("get")){
				tmp = StringUtils.uncapitalize(method.getName().substring(3));
			} else {
				continue;
			}
			if(!StringUtils.equals((String)key, tmp)){
				continue;
			}

			if (method.getParameterTypes().length ==0){
				Object r=method.invoke(obj);
				return r==null?"":r.toString();
			}
		}
		return null;
	}

}
