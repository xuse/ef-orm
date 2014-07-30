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
package jef.script.javascript;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jef.tools.reflect.BeanWrapperImpl;

/**
 * 用于存储结果的Map
 * 其中key是大小写不敏感的
 * @Date 2010-12-20
 */
public class Var implements Map<String,Object> ,Serializable{
	private static final long serialVersionUID = 2228797892548164705L;
	private final HashMap<String,Object> map=new HashMap<String,Object>();

	public Var() {
	}
	
	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Object get(Object key) {
		if(key instanceof String){
			return map.get(((String) key).toLowerCase());
		}
		return map.get(key);
	}

	public Object put(String key, Object value) {
		return map.put(key==null?key:key.toLowerCase(), value);	
	}
	
	public Object getLowerKey(String key) {
		return map.get(key);	
	}

	public Object putLowerKey(String key, Object value) {
		return map.put(key, value);	
	}

	public Object remove(Object key) {
		return map.remove(key);
	}

	public void putAll(Map<? extends String, ? extends Object> m) {
		map.putAll(m);
	}

	public void clear() {
		map.clear();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public Collection<Object> values() {
		return map.values();
	}

	public Set<java.util.Map.Entry<String, Object>> entrySet() {
		return map.entrySet();
	}
	
	public static Var createFromBean(Object bean){
		Var v=new Var();
		BeanWrapperImpl bw=new BeanWrapperImpl(bean);
		for(String name:bw.getRwPropertyNames()){
			v.put(name, bw.getPropertyValue(name));
		}
		return v;
	}
	@Override
	public int hashCode() {
		return map.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		return map.equals(obj);
	}
	@Override
	public String toString() {
		return  map.toString();
	}
}
