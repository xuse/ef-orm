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
package jef.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;

import jef.tools.StringUtils;

/**
 * 描述由两个值构成的值对
 * @param <K>
 * @param <V>
 */
public class Entry<K,V> implements Serializable,java.util.Map.Entry<K,V>{
	private static final long serialVersionUID = 2805658306682403737L;
	private K key;
	private V value;

	public String toString() {
		return StringUtils.toString(key)+":"+StringUtils.toString(value);
	}

	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		V old=this.value;
		this.value=value;
		return old;
	}
	
	public final void setKey(K key) {
		this.key = key;
	}

	public Entry(){};
	
	public Entry(K k,V v){
		this.key=k;
		this.value=v;
	}
	
	public static <K,V> List<Entry<K,V>> fromMap(Map<K,V> map){
		List<Entry<K,V>> list=new ArrayList<Entry<K,V>>();
		for(K k:map.keySet()){
			list.add(new Entry<K,V>(k,map.get(k)));
		}
		return list;
	}

	@Override
	public int hashCode() {
		int i=0;
		if(key!=null)i+=key.hashCode();
		if(value!=null)i+=value.hashCode();
		return i;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Entry){
			Entry e=(Entry)obj;
			return Objects.equal(e.key, this.key) && Objects.equal(e.value, this.value);
		}
		return false;
	}
}
