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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import jef.common.log.LogUtil;
import jef.script.util.InterfaceImplementor;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;


/**
 * Implementation of <code>ScriptEngine</code> using the Mozilla Rhino
 * interpreter.
 *
 * @author Mike Grogan
 * @author A. Sundararajan
 * @version 1.0
 * @since JEF 1.1
 */
@SuppressWarnings({"rawtypes" })
public final class RhinoScriptEngine extends AbstractScriptEngine
        implements  Invocable, Compilable {
    
    private static final boolean DEBUG = false;

    /* Scope where standard JavaScript objects and our
     * extensions to it are stored. Note that these are not
     * user defined engine level global variables. These are
     * variables have to be there on all compliant ECMAScript
     * scopes. We put these standard objects in this top level.
     */
    private RhinoTopLevel topLevel;

    /* map used to store indexed properties in engine scope
     * refer to comment on 'indexedProps' in ExternalScriptable.java.
     */
    private Map indexedProps;

    private ScriptEngineFactory factory;
    private InterfaceImplementor implementor;
    
    static {
        ContextFactory.initGlobal(new ContextFactory() {
            protected Context makeContext() {
                Context cx = super.makeContext();
                cx.setClassShutter(RhinoClassShutter.getInstance());
                cx.setWrapFactory(RhinoWrapFactory.getInstance());
                return cx;
            }

            public boolean hasFeature(Context cx, int feature) {
                // we do not support E4X (ECMAScript for XML)!
            	//Jiyi Modify here to support e4X
                if (feature == Context.FEATURE_E4X) {
                    return true;
                } else {
                    return super.hasFeature(cx, feature);
                }
            }
        });
    }

    
    /**
     * Creates a new instance of RhinoScriptEngine
     */
    public RhinoScriptEngine() {
       
        Context cx = enterContext();
        try { 
            topLevel = new RhinoTopLevel(cx, this);
        } finally {
        	Context.exit();
        }
      
        indexedProps = new HashMap();
 
        //construct object used to implement getInterface
        implementor = new InterfaceImplementor(this) {
				protected Object convertResult(Method method, Object res)
                                            throws ScriptException {
                    Class desiredType = method.getReturnType();
                    if (desiredType == Void.TYPE) {
                        return null;
                    } else {
                        return Context.jsToJava(res, desiredType);
                    }
                }
            };
    }
    
    public Object eval(Reader reader, ScriptContext ctxt)  throws ScriptException {
        Object ret;
        Context cx = enterContext();
        try {
            Scriptable scope = getRuntimeScope(ctxt);
            String filename = (String) get(ScriptEngine.FILENAME);
            filename = filename == null ? "<Unknown source>" : filename;
            
            ret = cx.evaluateReader(scope, reader, filename , 1,  null);
        } catch (RhinoException re) {
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new ScriptException(re.toString(), re.sourceName(), line);
        } catch (IOException ee) {
            throw new ScriptException(ee);
        } finally {
        	Context.exit();
        }
        
        return unwrapReturnValue(ret);
    }
    
    public Object eval(String script, ScriptContext ctxt) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("null script");
        }
        Object ret;
        Context cx = enterContext();
        int level=cx.getOptimizationLevel();
        try {
        	if(script.length()>71680){
        		cx.setOptimizationLevel(-1);
        	}
            Scriptable scope = getRuntimeScope(ctxt);
            String filename = (String) get(ScriptEngine.FILENAME);
            filename = filename == null ? "<Unknown source>" : filename;
            ret = cx.evaluateString(scope, script, filename , 1,  null);
            cx.setOptimizationLevel(level);
        } catch (RhinoException re) {
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new ScriptException(re.toString(), re.sourceName(), line);
        } finally {
        	Context.exit();
        }
        
        return unwrapReturnValue(ret);
    }
    
    public ScriptEngineFactory getFactory() {
        if (factory != null) {
            return factory;
        } else {
            return new RhinoScriptEngineFactory();
        }
    }
    
    public Bindings createBindings() {
        return new SimpleBindings();
    }
    
    //Invocable methods
    public Object invokeFunction(String name, Object... args)
    throws ScriptException, NoSuchMethodException {
        return invoke(null, name, args);
    }
    
    public Object invokeMethod(Object thiz, String name, Object... args)
    throws ScriptException, NoSuchMethodException {
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }
        return invoke(thiz, name, args);
    }

    private Object invoke(Object thiz, String name, Object... args)
    throws ScriptException, NoSuchMethodException {
        Context cx = enterContext();
        try {
            if (name == null) {
                throw new NullPointerException("method name is null");
            }

            if (thiz != null && !(thiz instanceof Scriptable)) {
                thiz = Context.toObject(thiz, topLevel);
            }
            
            Scriptable engineScope = getRuntimeScope(context);
            Scriptable localScope = (thiz != null)? (Scriptable) thiz :
                                                    engineScope;
            Object obj = ScriptableObject.getProperty(localScope, name);
            if (! (obj instanceof Function)) {
                throw new NoSuchMethodException("no such method: " + name);
            }

            Function func = (Function) obj;
            Scriptable scope = func.getParentScope();
            if (scope == null) {
                scope = engineScope;
            }
            Object result = func.call(cx, scope, localScope, 
                                      wrapArguments(args));
            return unwrapReturnValue(result);
        } catch (RhinoException re) {
            int line = (line = re.lineNumber()) == 0 ? -1 : line;
            throw new ScriptException(re.toString(), re.sourceName(), line);
        } finally {
        	Context.exit();
        }
    }
   
    public <T> T getInterface(Class<T> clasz) {
        try {
            return implementor.getInterface(null, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }
    
    public <T> T getInterface(Object thiz, Class<T> clasz) {
        if (thiz == null) {
            throw new IllegalArgumentException("script object can not be null");
        }

        try {
            return implementor.getInterface(thiz, clasz);
        } catch (ScriptException e) {
            return null;
        }
    }

    private static final String printSource = 
            "function print(str, newline) {                \n" +
            "    if (typeof(str) == 'undefined') {         \n" +
            "        str = 'undefined';                    \n" +
            "    } else if (str == null) {                 \n" +
            "        str = 'null';                         \n" +
            "    }                                         \n" +
            "    var out = context.getWriter();            \n" +
            "    out.print(String(str));                   \n" +
            "    if (newline) out.print('\\n');            \n" +
            "    out.flush();                              \n" +
            "}\n" +
            "function println(str) {                       \n" +
            "    print(str, true);                         \n" +
            "}";
    
    Scriptable getRuntimeScope(ScriptContext ctxt) {
        if (ctxt == null) {
            throw new NullPointerException("null script context");
        }

        // we create a scope for the given ScriptContext
        Scriptable newScope = new ExternalScriptable(ctxt, indexedProps);

        // Set the prototype of newScope to be 'topLevel' so that
        // JavaScript standard objects are visible from the scope.
        newScope.setPrototype(topLevel);

        // define "context" variable in the new scope
        newScope.put("context", newScope, ctxt);
       
        // define "print", "println" functions in the new scope
        Context cx = enterContext();
        try {
            cx.evaluateString(newScope, printSource, "print", 1, null);
        } finally {
        	Context.exit();
        } 
        return newScope;
    }
    
    
    //Compilable methods
    public CompiledScript compile(String script) throws ScriptException {
        return compile(new StringReader(script));
    }
    
    @SuppressWarnings("deprecation")
	public CompiledScript compile(java.io.Reader script) throws ScriptException {
        CompiledScript ret = null;
        Context cx = enterContext();
        
        try {
            String fileName = (String) get(ScriptEngine.FILENAME);
            if (fileName == null) {
                fileName = "<Unknown Source>";
            }
            
            Scriptable scope = getRuntimeScope(context);
            Script scr = cx.compileReader(scope, script, fileName, 1, null);
            ret = new RhinoCompiledScript(this, scr);
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
        	Context.exit();
        }
        return ret;
    }
    
    
    //package-private helpers

    static Context enterContext() {
        // call this always so that initializer of this class runs
        // and initializes custom wrap factory and class shutter.
        return Context.enter();
    }

    void setEngineFactory(ScriptEngineFactory fac) {
        factory = fac;
    }

    Object[] wrapArguments(Object[] args) {
        if (args == null) {
            return Context.emptyArgs;
        }
        Object[] res = new Object[args.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = Context.javaToJS(args[i], topLevel);
        }
        return res;
    }
    
    Object unwrapReturnValue(Object result) {
        if (result instanceof Wrapper) {
            result = ( (Wrapper) result).unwrap();
        }
        
        return result instanceof Undefined ? null : result;
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("No file specified");
            return;
        }
        
        InputStreamReader r = new InputStreamReader(new FileInputStream(args[0]));
        ScriptEngine engine = new RhinoScriptEngine();
        
        engine.put("x", "y");
        engine.put(ScriptEngine.FILENAME, args[0]);
        engine.eval(r);
        System.out.println(engine.get("x"));
    }
}
