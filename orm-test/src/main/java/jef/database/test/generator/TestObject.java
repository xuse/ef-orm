package jef.database.test.generator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jef.tools.Primitives;
import jef.tools.reflect.ConvertUtils;

public class TestObject {
	static boolean printStacktrace=false;
	static Map<Class,Object> defaultValueMap=new HashMap<Class,Object>();
	
	
	public static void invokeStatic(Class c) {
		for (Method m : c.getDeclaredMethods()) {
			if (!Modifier.isStatic(m.getModifiers())) {
				continue;
			}
			if (!Modifier.isPublic(m.getModifiers())) {
				continue;
			}
			doInvoke(m, null);
		}
	}

	public static void invokeObject(Object o) {
		Class clz=o.getClass();
		for (Method m : clz.getMethods()) {
			if(m.getDeclaringClass()!=clz){
				continue;
			}
			doInvoke(m, o);
		}
	}

	private static void doInvoke(Method m, Object o) {
		Class<?>[] clz = m.getParameterTypes();
		Object[] params = new Object[clz.length];
		for (int i = 0; i < clz.length; i++) {
			params[i] = getParam(clz[i]);
		}
		try {
			m.invoke(o, params);
//			System.out.println("==method Invoked: "+m.toGenericString());
		} catch (Exception e) {
			if(printStacktrace){
				e.printStackTrace();
			}else{
				String message = e.getMessage();
				if (message == null) {
					message = e.getClass().getSimpleName();
				} else {
					message = e.getClass().getSimpleName() + ":" + message;
				}
				System.err.println("Error invoke "+ m.toString()+" "+message);
			}
		}
	}

	private static Object getParam(Class<?> class1) {
		Object obj=defaultValueMap.get(class1);
		if(obj!=null)return obj;
		return Primitives.defaultValueForBasicType(class1);
	}
	
	static{
		defaultValueMap.put(Date.class, new Date());
		defaultValueMap.put(int.class, 1);
	}
}
