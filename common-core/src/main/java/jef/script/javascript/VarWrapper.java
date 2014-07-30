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

import java.util.Collection;
import java.util.Map;

import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.MapWrapper;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class VarWrapper implements Scriptable {
	MapWrapper wrapper;
	protected Context context;
	private Scriptable scope;
	protected Class<?> staticType;
	private Scriptable prototype;
	 
	@SuppressWarnings({ "rawtypes" })
	public VarWrapper(Context cx, Scriptable scope, Map javaObject, Class<?> staticType) {
		this.context=cx;
		this.scope=scope;
		this.wrapper=new MapWrapper(javaObject);
		this.staticType=staticType;
	}

	public String getClassName() {
		return wrapper.getClassName();
	}

	public Object get(String name, Scriptable start) {
		if (isEmpty(name)) {
			if (wrapper.isProperty(name)) {
				return wrapper.getPropertyValue(name);
			} else {
				return NOT_FOUND;
			}
		}else{
			Object o=wrapper.getPropertyValue(name);
			return o;
		}
	}

	public Object get(int index, Scriptable start) {
		return get(String.valueOf(index),start);
	}

	public boolean has(String name, Scriptable start) {
		return wrapper.isProperty(name);
	}

	public boolean has(int index, Scriptable start) {
		return wrapper.isProperty(String.valueOf(index));
	}

	public void put(String name, Scriptable start, Object value) {
		wrapper.setPropertyValue(name, value);
	}

	public void put(int index, Scriptable start, Object value) {
		wrapper.setPropertyValue(String.valueOf(index), value);
	}

	@SuppressWarnings("rawtypes")
	public void delete(String name) {
		((Map)wrapper.getWrapped()).remove(name);
	}

	public void delete(int index) {
		delete(String.valueOf(index));
	}

	public Scriptable getPrototype() {
		return prototype;
	}

	public void setPrototype(Scriptable prototype) {
		this.prototype=prototype;
	}

	public Scriptable getParentScope() {
		return this.scope;
	}

	public void setParentScope(Scriptable parent) {
		this.scope=parent;
	}

	public Object[] getIds() {
		Collection<String> c=wrapper.getRwPropertyNames();
		return c.toArray(new String[c.size()]);
	}

	public Object getDefaultValue(Class<?> hint) {
		return BeanUtils.defaultValueForBasicType(hint);
	}

	public boolean hasInstance(Scriptable instance) {
		Scriptable proto = instance.getPrototype();
		while (proto != null) {
			if (proto.equals(this)) return true;
			proto = proto.getPrototype();
		}
		return false;
	}

	private boolean isEmpty(String name) {
		return name.equals("");
	}
}
