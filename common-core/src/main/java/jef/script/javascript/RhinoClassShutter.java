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

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ClassShutter;

/**
 * This class prevents script access to certain sensitive classes.
 * Note that this class checks over and above SecurityManager. i.e., although
 * a SecurityManager would pass, class shutter may still prevent access.
 *
 * @version 1.0
 * @author A. Sundararajan
 * @since 1.6
 */
final class RhinoClassShutter implements ClassShutter {
    private static Map<String,Boolean> protectedClasses;
    private static RhinoClassShutter theInstance;
    
    private RhinoClassShutter() {
    }
    
    static synchronized ClassShutter getInstance() {
        if (theInstance == null) {
            theInstance = new RhinoClassShutter();
            protectedClasses = new HashMap<String,Boolean>();
            
            // For now, we just have AccessController. Allowing scripts
            // to this class will allow it to execute doPrivileged in
            // bootstrap context. We can add more classes for other reasons.
            protectedClasses.put("java.security.AccessController", Boolean.TRUE);
        }
        return theInstance;
    }
    
    public boolean visibleToScripts(String fullClassName) {
        // first do the security check.
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            int i = fullClassName.lastIndexOf(".");
            if (i != -1) {
                try {
                    sm.checkPackageAccess(fullClassName.substring(0, i));
                } catch (SecurityException se) {
                    return false;
                }
            }
        }
        // now, check is it a protected class.
        return protectedClasses.get(fullClassName) == null;
    }
}
