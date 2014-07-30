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
package jef.jre5support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

/**
 * Key不区分大小写的Map
 * 
 * @author jiyi
 */
public class Headers implements Map<String, String[]>, Serializable {
	private static final long serialVersionUID = -1981755352480832731L;
	private boolean ignorCase;
	private HashMap<String, List<String>> map;

	public Headers() {
		this(24, true);
	}

	public Map<String, List<String>> unwrap() {
		return map;
	}

	public Headers(int i, boolean ignorCase) {
		map = new LinkedHashMap<String, List<String>>(i);
		this.ignorCase = ignorCase;
	}

	public Headers(int i) {
		this(i, true);
	}

	// 转小写
	private String normalize(String s) {
		if (!ignorCase)
			return s;
		if (s == null)
			return null;
		return s.toLowerCase();
	}

	public int size() {
		return map.size();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof String))
			return false;
		else
			return map.containsKey(normalize((String) obj));
	}

	public boolean containsValue(Object obj) {
		return map.containsValue(obj);
	}

	/**
	 * 无论Map本身是否忽略大小写，都按忽略大小写进行查找
	 * 
	 * @param key
	 *            要获取的key
	 * @return 返回类型
	 */
	public List<String> getIgnorCase(String key) {
		for (String s : map.keySet()) {
			if (s.equalsIgnoreCase(key)) {
				return (List<String>) map.get(s);
			}
		}
		return null;
	}

	/**
	 * 无论Map本身是否忽略大小写，都按忽略大小写进行查找
	 * 
	 * @Title: getIgnorCase
	 * @param 参数
	 * @return List<String> 返回类型
	 * @throws
	 */
	public String getFirstIgnorCase(String s) {
		List<String> list = getIgnorCase(s);
		if (list == null)
			return null;
		else
			return (String) list.get(0);
	}

	/**
	 * 无论Map本身是否忽略大小写，都按忽略大小写进行查找
	 * 
	 * @Title: getIgnorCase
	 * @param 参数
	 * @return List<String> 返回类型
	 * @throws
	 */
	public List<String> removeIgnorCase(String key) {
		for (String s : map.keySet()) {
			if (s.equalsIgnoreCase(key)) {
				return (List<String>) map.remove(s);
			}
		}
		return null;
	}

	public String[] get(Object obj) {
		List<String> value = map.get(normalize((String) obj));
		return value == null ? null : value.toArray(new String[value.size()]);
	}

	public String getFirst(String s) {
		List<String> list = map.get(normalize(s));
		if (list == null)
			return null;
		else
			return (String) list.get(0);
	}

	/**
	 * 对于像ContentType一类的头，本身用;分割描述了多个属性。返回解析后的属性
	 * 
	 * @Title: getAsMap
	 * @param 参数
	 * @return Map<String,String> 返回类型
	 * @throws
	 */
	public Map<String, String> getAsMap(String s) {
		List<String> list = map.get(normalize(s));
		Map<String, String> result = new HashMap<String, String>();
		if (list == null)
			return result;
		for (String data : list) {
			String[] args = StringUtils.split(data, ";");
			for (String cell : args) {
				int n = cell.indexOf("=");
				if (n > -1) {
					result.put(cell.substring(0, n).toLowerCase().trim(), cell.substring(n + 1).trim());
				} else {
					result.put(cell.toLowerCase().trim(), null);
				}
			}
		}
		return result;
	}

	public void add(String s, String s1) {
		String s2 = normalize(s);
		List<String> obj = map.get(s2);
		if (obj == null) {
			obj = new ArrayList<String>();
			map.put(s2, obj);
		}
		obj.add(s1);
	}

	
	public String[] remove(Object key) {
		List<String> value = map.remove(normalize((String) key));
		return value == null ? null : value.toArray(new String[value.size()]);
	}

	public void removeRegexp(String key) {
		for (Iterator<Entry<String, List<String>>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry<String, List<String>> e = iter.next();
			if (e.getKey().matches(key)) {
				iter.remove();
			}
		}
	}

	public void clear() {
		map.clear();
	}

	public Set<String> keySet() {
		return map.keySet();
	}

	public boolean equals(Object obj) {
		return map.equals(obj);
	}

	public int hashCode() {
		return map.hashCode();
	}

	//
	// public String removeAndReturnFirst(String key) {
	// List<String> r=this.remove(key);
	// if(r==null || r.size()==0)return null;
	// return r.get(0);
	// }

	public String toValueString() {
		TreeMap<String, String> m = new TreeMap<String, String>();
		for (String s : map.keySet()) {
			m.put(s, map.get(s).get(0));
		}
		return StringUtils.join(m.values(), ",");
	}

	@Override
	public String toString() {
		return map.toString();
	}

	public void putAll(Map<? extends String, ? extends String[]> m) {
		for (Entry<? extends String, ? extends String[]> e : m.entrySet()) {
			map.put(e.getKey(), Arrays.<String> asList(e.getValue()));
		}
	}

	public String[] put(String key, String[] value) {
		List<String> ovalue = map.put(key, Arrays.asList(value));
		if (ovalue == null)
			return null;
		return ovalue.toArray(new String[ovalue.size()]);
	}

	public Collection<String[]> values() {
		List<String[]> result = new ArrayList<String[]>(map.size());
		for (List<String> v : map.values()) {
			result.add(v.toArray(new String[v.size()]));
		}
		return result;
	}

	public Set<java.util.Map.Entry<String, String[]>> entrySet() {
		Set<java.util.Map.Entry<String, String[]>> result = new HashSet<Map.Entry<String, String[]>>();
		for (Entry<String, List<String>> v : map.entrySet()) {
			result.add(new ListTowArrayEntry(v));
		}
		return result;
	}

	static class ListTowArrayEntry implements Map.Entry<String, String[]> {
		Entry<String, List<String>> e;

		public ListTowArrayEntry(java.util.Map.Entry<String, List<String>> v) {
			this.e = v;
		}

		public String getKey() {
			return e.getKey();
		}

		public String[] getValue() {
			List<String> v = e.getValue();
			return v == null ? null : v.toArray(new String[v.size()]);
		}

		public String[] setValue(String[] value) {
			List<String> ov = e.setValue(Arrays.asList(value));
			return ov == null ? null : ov.toArray(new String[ov.size()]);

		}

	}
}
