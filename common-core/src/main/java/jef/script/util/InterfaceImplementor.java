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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

import javax.script.Invocable;
import javax.script.ScriptException;

/*
 * java.lang.reflect.Proxy based interface implementor. This is meant
 * to be used to implement Invocable.getInterface.
 *
 * @version 1.0
 * @author Mike Grogan
 * @since 1.6
 */
public class InterfaceImplementor {
    
    private Invocable engine;
    
    /** Creates a new instance of Invocable */
    public InterfaceImplementor(Invocable engine) {
        this.engine = engine;
    }
    
    private final class InterfaceImplementorInvocationHandler 
                        implements InvocationHandler {
        private Object thiz;
        private AccessControlContext accCtxt;

        public InterfaceImplementorInvocationHandler(Object thiz,
            AccessControlContext accCtxt) {
            this.thiz = thiz;
            this.accCtxt = accCtxt;
        }

        public Object invoke(Object proxy , Method method, Object[] args)
        throws java.lang.Throwable {
            // give chance to convert input args
            args = convertArguments(method, args);
            Object result;
            final Method m = method;
            final Object[] a = args;
            result = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    if (thiz == null) {
                        return engine.invokeFunction(m.getName(), a);
                    } else {
                        return engine.invokeMethod(thiz, m.getName(), a);
                    }
                }
            }, accCtxt);
            // give chance to convert the method result
            return convertResult(method, result);
        }
    }
    
    public <T> T getInterface(Object thiz, Class<T> iface)
    throws ScriptException {
        if (iface == null || !iface.isInterface()) {
            throw new IllegalArgumentException("interface Class expected");
        }
        AccessControlContext accCtxt = AccessController.getContext();
        return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(),
            new Class[]{iface},
            new InterfaceImplementorInvocationHandler(thiz, accCtxt)));
    }

    // called to convert method result after invoke
    protected Object convertResult(Method method, Object res) 
                                   throws ScriptException {
        // default is identity conversion
        return res;
    }

    // called to convert method arguments before invoke
    protected Object[] convertArguments(Method method, Object[] args)
                                      throws ScriptException {
        // default is identity conversion
        return args;
    }
}
