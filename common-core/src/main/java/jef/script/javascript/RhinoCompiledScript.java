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
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * Represents compiled JavaScript code.
 *
 * @author Mike Grogan
 * @version 1.0
 * @since 1.6
 */
final class RhinoCompiledScript extends CompiledScript {
    
    private RhinoScriptEngine engine;
    private Script script;
    
    
    RhinoCompiledScript(RhinoScriptEngine engine, Script script) {
        this.engine = engine;
        this.script = script;
    }
    
    public Object eval(ScriptContext context) throws ScriptException {
        
        Object result = null;
        Context cx = RhinoScriptEngine.enterContext();
        try {
            
            Scriptable scope = engine.getRuntimeScope(context);
            Object ret = script.exec(cx, scope);
            result = engine.unwrapReturnValue(ret);
            
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        
        return result;
    }
    
    public ScriptEngine getEngine() {
        return engine;
    }
    
}
