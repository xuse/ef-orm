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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;
import org.mozilla.javascript.Wrapper;

/**
 * This class serves as top level scope for Rhino. This class adds
 * 3 top level functions (bindings, scope, sync) and two constructors 
 * (JSAdapter, JavaAdapter).
 *
 * @version 1.0
 * @author A. Sundararajan
 * @since 1.6
 */
public final class RhinoTopLevel extends ImporterTopLevel {
	private static final long serialVersionUID = 1L;
	// variables defined always to help Java access from JavaScript
    private static final String builtinVariables =
                "var com = Packages.com;                   \n" +
                "var edu = Packages.edu;                   \n" +
                "var javax = Packages.javax;               \n" +
                "var net = Packages.net;                   \n" +
                "var org = Packages.org;                   \n";

    RhinoTopLevel(Context cx, RhinoScriptEngine engine) {
        super(cx);
        this.engine = engine;
        
        
        // initialize JSAdapter lazily. Reduces footprint & startup time.
        new LazilyLoadedCtor(this, "JSAdapter",
                "com.sun.script.javascript.JSAdapter",
                false);
        
        /*
         * initialize JavaAdapter. We can't lazy initialize this because
         * lazy initializer attempts to define a new property. But, JavaAdapter
         * is an exisiting property that we overwrite.
         */
        JavaAdapter.init(cx, this, false);
        
        // add top level functions
        String names[] = { "bindings", "scope", "sync"  };
        defineFunctionProperties(names, RhinoTopLevel.class,
                ScriptableObject.DONTENUM);

        // define built-in variables
        cx.evaluateString(this, builtinVariables, "<builtin>", 1, null);
    }

    /**
     * The bindings function takes a JavaScript scope object 
     * of type ExternalScriptable and returns the underlying Bindings
     * instance.
     *
     *    var page = scope(pageBindings);
     *    with (page) {
     *       // code that uses page scope 
     *    } 
     *    var b = bindings(page);
     *    // operate on bindings here.
     */
    public static Object bindings(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof Wrapper) {
                arg = ((Wrapper)arg).unwrap();
            }
            if (arg instanceof ExternalScriptable) {
                ScriptContext ctx = ((ExternalScriptable)arg).getContext();
                Bindings bind = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
                return Context.javaToJS(bind, 
                           ScriptableObject.getTopLevelScope(thisObj));
            }
        }
        return Context.getUndefinedValue();
    }
   
    /** 
     * The scope function creates a new JavaScript scope object 
     * with given Bindings object as backing store. This can be used
     * to create a script scope based on arbitrary Bindings instance.
     * For example, in webapp scenario, a 'page' level Bindings instance
     * may be wrapped as a scope and code can be run in JavaScripe 'with'
     * statement:
     *
     *    var page = scope(pageBindings);
     *    with (page) {
     *       // code that uses page scope 
     *    } 
     */
    public static Object scope(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof Wrapper) {
                arg = ((Wrapper)arg).unwrap();
            }
            if (arg instanceof Bindings) {
                ScriptContext ctx = new SimpleScriptContext();
                ctx.setBindings((Bindings)arg, ScriptContext.ENGINE_SCOPE);
                Scriptable res = new ExternalScriptable(ctx);
                res.setPrototype(ScriptableObject.getObjectPrototype(thisObj));
                res.setParentScope(ScriptableObject.getTopLevelScope(thisObj));
                return res;
            }
        }
        return Context.getUndefinedValue();
    }
 
    /**
     * The sync function creates a synchronized function (in the sense
     * of a Java synchronized method) from an existing function. The
     * new function synchronizes on the <code>this</code> object of
     * its invocation.
     * js> var o = { f : sync(function(x) {
     *       print("entry");
     *       Packages.java.lang.Thread.sleep(x*1000);
     *       print("exit");
     *     })};
     * js> thread(function() {o.f(5);});
     * entry
     * js> thread(function() {o.f(5);});
     * js>
     * exit
     * entry
     * exit
     */
    public static Object sync(Context cx, Scriptable thisObj, Object[] args,
            Function funObj) {
        if (args.length == 1 && args[0] instanceof Function) {
            return new Synchronizer((Function)args[0]);
        } else {
            throw Context.reportRuntimeError("wrong argument(s) for sync");
        }
    }
    
    RhinoScriptEngine getScriptEngine() {
        return engine;
    }
   
    private RhinoScriptEngine engine;
}
