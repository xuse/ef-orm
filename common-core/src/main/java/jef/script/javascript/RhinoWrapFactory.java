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

import static sun.security.util.SecurityConstants.GET_CLASSLOADER_PERMISSION;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * This wrap factory is used for security reasons. JSR 223 script
 * engine interface and JavaScript engine classes are run as bootstrap
 * classes. For example, java.lang.Class.forName method (when called without
 * class loader) uses caller's class loader. This may be exploited by script
 * authors to access classes otherwise not accessible. For example,
 * classes in sun.* namespace are normally not accessible to untrusted
 * code and hence should not be accessible to JavaScript run from
 * untrusted code.
 *
 * @version 1.0
 * @author A. Sundararajan
 * @since 1.6
 */
@SuppressWarnings("rawtypes")
final class RhinoWrapFactory extends WrapFactory {
    private RhinoWrapFactory() {}
    private static RhinoWrapFactory theInstance;
    
    static synchronized WrapFactory getInstance() {
        if (theInstance == null) {
            theInstance = new RhinoWrapFactory();
        }
        return theInstance;
    }
   
    // We use instance of this class to wrap security sensitive
    // Java object. Please refer below.
    private static class RhinoJavaObject extends NativeJavaObject {
		private static final long serialVersionUID = 1L;
		RhinoJavaObject(Scriptable scope, Object obj, Class type) {
            // we pass 'null' to object. NativeJavaObject uses
            // passed 'type' to reflect fields and methods when
            // object is null.
            super(scope, null, type);

            // Now, we set actual object. 'javaObject' is protected
            // field of NativeJavaObject.
            javaObject = obj;
        }
    }

	public Scriptable wrapAsJavaObject(Context cx, Scriptable scope,Object javaObject, Class staticType) {
        SecurityManager sm = System.getSecurityManager();
        ClassShutter classShutter = RhinoClassShutter.getInstance();
        //System.out.println("=============DEBUG==============\n" + cx+ "\n" +scope+"\n"+javaObject+(javaObject==null?"":" L"+javaObject.getClass().getName())+"\n"+staticType);
        if (javaObject instanceof ClassLoader) {
            // Check with Security Manager whether we can expose a ClassLoader...
            if (sm != null) {
                sm.checkPermission(GET_CLASSLOADER_PERMISSION);
            }
            // if we fall through here, check permission succeeded.
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        } else if(javaObject instanceof Var){
            return new VarWrapper(cx, scope, (Var)javaObject, staticType);
        } else if(javaObject.getClass().getName().equals("net.sf.json.JSONObject")){
        	return new VarWrapper(cx, scope, (Map)javaObject, staticType);
        } else {
            String name = null;
            if (javaObject instanceof Class) {
                name = ((Class)javaObject).getName();
            } else if (javaObject instanceof Member) {
                Member member = (Member) javaObject;
                // Check member access. Don't allow reflective access to
                // non-public members. Note that we can't call checkMemberAccess
                // because that expects exact stack depth!
                if (sm != null && !Modifier.isPublic(member.getModifiers())) {
                    return null;
                }
                name = member.getDeclaringClass().getName();
            }
            // Now, make sure that no ClassShutter prevented Class or Member
            // of it is accessed reflectively. Note that ClassShutter may 
            // prevent access to a class, even though SecurityManager permit.
            if (name != null) {
                if (!classShutter.visibleToScripts(name)) {
                    return null;
                } else {
                    return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
                }
            }
        }

        // we have got some non-reflective object.
        Class dynamicType = javaObject.getClass();
        String name = dynamicType.getName();
        if (!classShutter.visibleToScripts(name)) {
            // Object of some sensitive class (such as sun.net.www.*
            // objects returned from public method of java.net.URL class.
            // We expose this object as though it is an object of some
            // super class that is safe for access.

            Class type = null;

            // Whenever a Java Object is wrapped, we are passed with a
            // staticType which is the type found from environment. For
            // example, method return type known from signature. The dynamic
            // type would be the actual Class of the actual returned object.
            // If the staticType is an interface, we just use that type.
            if (staticType != null && staticType.isInterface()) {
                type = staticType;
            } else {
                // dynamicType is always a class type and never an interface.
                // find an accessible super class of the dynamic type.
                while (dynamicType != null) {
                    dynamicType = dynamicType.getSuperclass();
                    name = dynamicType.getName();
                    if (classShutter.visibleToScripts(name)) {
                         type = dynamicType; break;
                    }
                }
                // atleast java.lang.Object has to be accessible. So, when
                // we reach here, type variable should not be null.
                assert type != null: 
                       "even java.lang.Object is not accessible?";
            }
            // create custom wrapper with the 'safe' type.
            return new RhinoJavaObject(scope, javaObject, type);
        } else {
            return super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }
    }
}
