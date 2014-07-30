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
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

/*
 * Abstract super class for Bindings implementations. Handles
 * global and local scopes.
 *
 * @author Mike Grogan
 * @version 1.0
 * @since 1.6
 */
public abstract class BindingsImpl extends BindingsBase {
    
    //get method delegates to global if key is not defined in
    //base class or local scope
    protected Bindings global = null;
    
    //get delegates to local scope
    protected Bindings local = null;
    
    public void setGlobal(Bindings n) {
        global = n;
    }
    
    public void setLocal(Bindings n) {
        local = n;
    }
    
    public  Set<Map.Entry<String, Object>> entrySet() {
        return new BindingsEntrySet(this);
    }
    
    public Object get(Object key) {
        checkKey(key);
        
        Object ret  = null;
        if ((local != null) && (null != (ret = local.get(key)))) {
            return ret;
        }
        
        ret = getImpl((String)key);
        
        if (ret != null) {
            return ret;
        } else if (global != null) {
            return global.get(key);
        } else {
            return null;
        }
    }
    
    public Object remove(Object key) {
        checkKey(key);
        Object ret = get(key);
        if (ret != null) {
            removeImpl((String)key);
        }
        return ret;
    }
}
