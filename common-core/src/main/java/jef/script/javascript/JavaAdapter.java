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

import javax.script.Invocable;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

/**
 * This class implements Rhino-like JavaAdapter to help implement a Java
 * interface in JavaScript. We support this using Invocable.getInterface. Using
 * this JavaAdapter, script author could write:
 * 
 * var r = new java.lang.Runnable() { run: function() { script... } };
 * 
 * r.run(); new java.lang.Thread(r).start();
 * 
 * Note that Rhino's JavaAdapter support allows extending a Java class and/or
 * implementing one or more interfaces. This JavaAdapter implementation does not
 * support these.
 * 
 * @version 1.0
 * @author A. Sundararajan
 * @since 1.6
 */
final class JavaAdapter extends ScriptableObject implements Function {
	private static final long serialVersionUID = 1L;

	private JavaAdapter(Invocable engine) {
		this.engine = engine;
	}

	static void init(Context cx, Scriptable scope, boolean sealed) throws RhinoException {
		RhinoTopLevel topLevel = (RhinoTopLevel) scope;
		Invocable engine = topLevel.getScriptEngine();
		JavaAdapter obj = new JavaAdapter(engine);
		obj.setParentScope(scope);
		obj.setPrototype(getFunctionPrototype(scope));
		/*
		 * Note that we can't use defineProperty. A property of this name is
		 * already defined in Context.initStandardObjects. We simply overwrite
		 * the property value!
		 */
		ScriptableObject.putProperty(topLevel, "JavaAdapter", obj);
	}

	public String getClassName() {
		return "JavaAdapter";
	}

	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) throws RhinoException {
		return construct(cx, scope, args);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) throws RhinoException {
		if (args.length == 2) {
			Class clazz = null;
			Object obj1 = args[0];
			if (obj1 instanceof Wrapper) {
				Object o = ((Wrapper) obj1).unwrap();
				if (o instanceof Class && ((Class) o).isInterface()) {
					clazz = (Class) o;
				}
			} else if (obj1 instanceof Class) {
				if (((Class) obj1).isInterface()) {
					clazz = (Class) obj1;
				}
			}
			if (clazz == null) {
				throw Context.reportRuntimeError("JavaAdapter: first arg should be interface Class");
			}

			Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
			return Context.toObject(engine.getInterface(args[1], clazz), topLevel);
		} else {
			throw Context.reportRuntimeError("JavaAdapter requires two arguments");
		}
	}

	private Invocable engine;
}
