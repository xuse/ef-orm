package jef.json;

import java.lang.reflect.Method;

import jef.tools.Assert;

import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

public class SerializeConfigEx extends SerializeConfig {

	public void putHierarchy(Class<?> class1, ObjectSerializer nodeSer) {

	}

	@Override
	public ObjectSerializer createJavaBeanSerializer(Class<?> clazz) {
		JSONCustom annotation=clazz.getAnnotation(JSONCustom.class);
		if (annotation != null && annotation.serializer() != Void.class) {
			return getCustomSerializer(annotation.serializer());
		}
		return super.createJavaBeanSerializer(clazz);
	}

	private ObjectSerializer getCustomSerializer(Class<?> serializer) {
		Method m;
		try {
			m = serializer.getDeclaredMethod("getSerializer"); // try to get
																// singleton
																// instance.
			m.setAccessible(true);
		} catch (NoSuchMethodException e) {
			try {
				return (ObjectSerializer) serializer.newInstance();// call empty
																	// constructor
			} catch (Exception e1) {
				throw new IllegalStateException(e1);
			}
		}
		try {
			Object o = m.invoke(null);
			Assert.notNull(o);
			return (ObjectSerializer) o;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
