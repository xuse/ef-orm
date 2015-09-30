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
package jef.jre5support.script;

import java.util.HashMap;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import jef.common.log.LogUtil;
import jef.script.javascript.RhinoScriptEngineFactory;
import jef.tools.ArrayUtils;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.XMLUtils;
import jef.tools.ZipUtils;

import org.apache.commons.lang.ObjectUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Decompiler;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.UintMap;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.UniqueTag;
import org.mozilla.javascript.Wrapper;

public class JavaScriptUtil {
	private static RhinoScriptEngineFactory fac = new RhinoScriptEngineFactory();

	public synchronized static ScriptEngine newEngine() {
		return fac.getScriptEngine();
	}

	private static final String printSource = "function echo(str) {                \n"
			+ "    if (typeof(str) == 'undefined') {         \n"
			+ "        str = 'undefined';                    \n"
			+ "    } else if (str == null) {                 \n"
			+ "        str = 'null';                         \n"
			+ "    }                                         \n"
			+ "    LogUtil.show(str)              \n"
			+ "}";

	public static void initEngine(ScriptEngine e, Bindings... b) {
		importClass(e, LogUtil.class, b);
		importClass(e, StringUtils.class, b);
		importClass(e, ArrayUtils.class, b);
		importClass(e, XMLUtils.class, b);
		importClass(e, IOUtils.class, b);
		importClass(e, ZipUtils.class, b);
		
		try {
			if (b.length > 0) {
				e.eval(printSource, b[0]);
			} else {
				e.eval(printSource);
			}
		} catch (ScriptException ex) {
			LogUtil.exception(ex);
		}
	}

	public static void importPackage(ScriptEngine e, Package pkg, Bindings... b) {
		try {
			if (b.length == 0) {
				e.eval("importPackage(Packages." + pkg.getName() + ")");
			} else {
				e.eval("importPackage(Packages." + pkg.getName() + ")", b[0]);
			}
		} catch (ScriptException e1) {
			throw new RuntimeException(e1);
		}
	}

	public static void importClass(ScriptEngine e, Class<?> pkg, Bindings... b) {
		try {
			if (b.length == 0) {
				e.eval("importClass(Packages." + pkg.getName() + ")");
			} else {
				e.eval("importClass(Packages." + pkg.getName() + ")", b[0]);
			}
		} catch (ScriptException e1) {
			throw new RuntimeException(e1);
		}
	}

	/**
	 * 将Native对象转换为String数组
	 * 
	 * @Title: toStringArray
	 * @Description: TODO(这里用一句话描述这个方法的作用)
	 * @param 参数
	 * @return String[] 返回类型
	 * @throws
	 */
	public static String[] toStringArray(NativeArray nv) {
		String[] result = new String[(int) nv.getLength()];
		for (int i = 0; i < result.length; i++) {
			result[i] = StringUtils.toString(nv.get(i, null));
		}
		return result;
	}

	/**
	 * 将NativeArray返回结果转换为int[]对象
	 * 
	 * @Title: toIntArray
	 * @Description: TODO(这里用一句话描述这个方法的作用)
	 * @param 参数
	 * @return int[] 返回类型
	 * @throws
	 */
	public static int[] toIntArray(NativeArray nv) {
		int[] result = new int[(int) nv.getLength()];
		for (int i = 0; i < result.length; i++) {
			Object obj = nv.get(i, null);
			if (obj instanceof Number) {
				result[i] = ((Number) obj).intValue();
			} else {
				result[i] = StringUtils.toInt(StringUtils.toString(obj), 0);
			}
		}
		return result;
	}

	/**
	 * 将Rhino中的JavaScript对象转换成相应的Java对象
	 * <p>
	 * 如果对象类型是wraped java object，那么 unwrap ； 如果对象类型是 scriptable object, 那么转换为相应的
	 * java object。
	 * </p>
	 * 
	 * @param jsObj
	 *            要转换为Java对象的返回值
	 * @param context
	 *            上下文
	 * @return return 转换后的Java对象，当产生异常的时候返回null
	 */
	public static Object jsToJava(Object jsObj) {
		return jsToJava(jsObj, Context.enter());
	}

	private static Object jsToJava(Object jsObj, Context context) {
		try {
			if (jsObj == null || jsObj == Undefined.instance
					|| jsObj == ScriptableObject.NOT_FOUND)
				return null;
			if (jsObj instanceof Wrapper)
				return ((Wrapper) jsObj).unwrap();
			// Rhino 内的 JavaScript 对象一般都继承 ScriptableObject
			if (jsObj instanceof ScriptableObject) {
				final ScriptableObject scriptObj = (ScriptableObject) jsObj;
				return scriptableToJava(scriptObj, context);
			} else {
				// 为Java Object，直接返回
				return jsObj;
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 对Scriptable对象转换成相应的Java对象
	 * <p>
	 * 如果对象类型是数组，按下标数组和关联数组两种情况分别转换为Array和Map； 否则转换为对应的Java对象，或者是一个包含此对象所有属性的Map
	 * </p>
	 * 
	 * @param scriptObj
	 *            需要转换的Scriptable对象
	 * @param context
	 *            上下文
	 * @return return 转换后的Java对象
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" })
	private static Object scriptableToJava(ScriptableObject scriptObj,
			Context context) throws IllegalAccessException,
			NoSuchFieldException {
		// Array & Arguments
		if (ScriptRuntime.isArrayObject(scriptObj)) {
			final Object[] arrayElements = (Object[]) context
					.getElements(scriptObj);
			// If scriptObj is a associative arry, arrayElements.length will
			// always 0
			// So if scriptObj is empty or index array, return true, else return
			// false
			if (scriptObj.getIds().length == 0
					|| arrayElements.length == scriptObj.getIds().length) {
				for (int i = 0; i < arrayElements.length; i++) {
					arrayElements[i] = jsToJava(arrayElements[i], context);
				}
				return arrayElements;
			} else {
				final Object[] ids = scriptObj.getIds();

				final HashMap map = new HashMap();
				for (int i = 0; i < ids.length; i++) {
					final String key = ids[i].toString();
					Object value = scriptObj.get(key, scriptObj);
					// if value is UniqueTag, means index is numeric,
					// should get its value by index
					if (value.getClass().equals(UniqueTag.class)) {
						value = scriptObj.get(Integer.parseInt(key), scriptObj);
					}
					map.put(ids[i], jsToJava(value, context));
				}
				return map;
			}
		} else {
			final String jsClassName = scriptObj.getClassName();
			// If jsClassName is 'Object', means scriptObj could't directly
			// convert to a normal java object, in this case we
			// return a map contains all properties in this scriptable object.
			if ("Object".equals(jsClassName)) {
				final Object[] ids = scriptObj.getIds();
				final HashMap map = new HashMap();
				for (int i = 0; i < ids.length; i++) {
					final String key = ids[i].toString();
					final Object value = scriptObj.get(key, scriptObj);
					map.put(key, jsToJava(value, context));
				}
				return map;
			}
			// If jsClassName is 'Funtion' & instanceof NativeFunction,
			// means scriptObj is a function defined in script,
			// in this case we return a String present source of this function
			else if ("Function".equals(jsClassName)
					&& scriptObj instanceof NativeFunction) {
				final NativeFunction func = (NativeFunction) scriptObj;
				return Decompiler.decompile(func.getEncodedSource(),
						Decompiler.TO_SOURCE_FLAG, new UintMap());
			}
			// Else, we can covert it to a desired java object by
			// Context.jsToJava()
			else {
				final Class clazz = (Class) ScriptRuntime.class
						.getDeclaredField(jsClassName + "Class").get(scriptObj);
				return Context.jsToJava(scriptObj, clazz);
			}
		}
	}
}
