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

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jef.common.log.LogUtil;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 *  9个例子掌握脚本引擎的用法
 */
public class ScriptEngineTest {
	
	private static ScriptEngineManager sem = new ScriptEngineManager();
	private static ScriptEngine engine;

	@BeforeClass
	public static void setup() {
//		sem.registerEngineName("JavaScript", new RhinoScriptEngineFactory());
		try{
			engine =JavaScriptUtil.newEngine();
		} catch (Exception e) {
			LogUtil.exception(e);
		}
	}

	/**
	 * ==1.默认的引擎特性
	 */
	@Test
	public void test1_engineFeatures() {
		for (ScriptEngineFactory f : sem.getEngineFactories()) {
			LogUtil.show(f.getEngineName());
			LogUtil.show(f.getLanguageName());
			LogUtil.show(f.getEngineVersion());
			LogUtil.show(f.getLanguageVersion());
		//	LogUtil.show(f.getParameter("THREADING"));
			// 返回线程安全特性：
			// MULTITHREADED安全 null不安全
		}
	}

	@Test
	public void test2() throws ScriptException {
		// ==2.eval的计算返回值/多次执行上下文相同
		System.out.println("======== Demo 2 =========");
		System.out.println(engine.eval("n=1728"));
		System.out.println(engine.eval("n+1"));
		System.out.println(engine.get("n"));
	}

	@Test
	public void test3() throws ScriptException {
		// ==3 传入和传出变量
		System.out.println("======== Demo 3 =========");
		HashMap<String, String> map = new HashMap<String, String>();
		engine.put("test", map);
		engine.put("output", System.out);
		System.out.println(engine.eval("test.put('sss','bbb');output.println([1,2,3,4,5,6,7,8,9,60,88])"));
		LogUtil.show(map);
	}

	@Test
	public void test4() throws ScriptException, NoSuchMethodException {
		// ==4 定义函数和方法
		System.out.println("======== Demo 4 =========");
		String script = "function max(first,second) { return (first > second) ?true:false;}";
		// 执行脚本
		engine.eval(script);
		Invocable inv = (Invocable) engine;
		Object obj = inv.invokeFunction("max", "0", "1");
		System.out.println((Boolean) obj == false);
		// inv.invokeMethod(thiz, name, args);
		// ====================================
		// System.out.println("======== Demo EX =========");
		// Object obj1=engine.eval("function test(){return false;}");
		// System.out.println(obj1.getClass().getName());
		// System.out.println(obj1);
	}

	@Test
	public void test5() throws ScriptException {
		// ==5 自定义变量作用范围
		System.out.println("======== Demo 5 =========");
		Bindings scope = engine.createBindings();
		scope.put("test", Arrays.asList("here", "is", "a", "dog"));
		System.out.println(engine.eval("str=test.get(0)+test.get(1)+test.get(2)+test.get(3)", scope));
	}

	@Test
	public void test6() throws ScriptException {
		// ==6 输出定向 //其实不定向默认就是system.out
		System.out.println("======== Demo 6 =========");
		engine.getContext().setWriter(new PrintWriter(System.out));
		engine.eval("print('dddd');");
		engine.eval("println('abc');");
	}

	@Test
	public void test7() throws ScriptException {
		// ==7 将脚本包装成接口
		System.out.println("======== Demo 7 =========");
		engine.eval("function max(first,second) {println('新的方法');return (first > second) ?true:false;}");
		Invocable inv = (Invocable) engine;
		TmpInterface service = (TmpInterface) inv.getInterface(TmpInterface.class);
		System.out.println(service.max(3, 2));
	}

	@Test
	public void test8() throws ScriptException {
		// ==8 编译
		System.out.println("======== Demo 8 =========");
		if (engine instanceof Compilable) {
			CompiledScript cp = ((Compilable) engine).compile("print(test.get('sss'))");
			System.out.println("Compling Success!!");
			cp.eval();
		}
	}
	@Test
	public void test9() throws ScriptException {
		// ==9. 每次 get到的是不同的引擎
		System.out.println("======== Demo 9 =========");
		ScriptEngine engine2 = JavaScriptUtil.newEngine();
		System.out.println(engine2 == engine);
		System.out.println(engine2.get("test"));
		engine2.eval("print('ssssssssssss')");
		engine2.eval("var str=new Packages.jef.tools.Person();println(str)");
	}

	interface TmpInterface {
		boolean max(Object a, Object b);
	}
}
