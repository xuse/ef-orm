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
package jef.script.util;

import java.util.AbstractMap;
import java.util.Map;

import javax.script.Bindings;

/**
 * Abstract super class for Bindings implementations
 *
 * @version 1.0
 * @author Mike Grogan
 * @since 1.6
 */
public abstract class BindingsBase extends AbstractMap<String, Object>
        implements Bindings {
    
    //AbstractMap methods
    public Object get(Object name) {
        checkKey(name);
        return getImpl((String)name);
    }
    
    public Object remove(Object key) {
        checkKey(key);
        return removeImpl((String)key);
    }
    
    public Object put(String key, Object value) {
        checkKey(key);
        return putImpl(key, value);
    }
    
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        for (Map.Entry<? extends String, ? extends Object> entry : toMerge.entrySet()) {
            String key = entry.getKey();
            checkKey(key);
            putImpl(entry.getKey(), entry.getValue());
        }
    }
    
    //BindingsBase methods
    public abstract Object putImpl(String name, Object value);
    public abstract Object getImpl(String name);
    public abstract Object removeImpl(String name);
    public abstract String[] getNames();
    
    protected void checkKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (!(key instanceof String)) {
            throw new ClassCastException("key should be String");
        }
        if (key.equals("")) {
            throw new IllegalArgumentException("key can not be empty");
        }
    }
}
