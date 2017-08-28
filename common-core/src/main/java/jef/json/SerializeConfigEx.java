package jef.json;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jef.tools.Assert;

import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;

/**
 * 对FastJSON的SerializeConfig稍作改变，以支持JSON/XML互转等一些功能。
 * @author jiyi
 *
 */
public class SerializeConfigEx extends SerializeConfig {
	/**
	 * 接口的序列化器
	 */
	private Map<Class<?>, ObjectSerializer> typeCodecs;

	/**
	 * 对传入类型和子类都生效的序列化器
	 * @param class1
	 * @param nodeSer
	 */
	public void putHierarchy(Class<?> class1, ObjectSerializer nodeSer) {
		if (typeCodecs == null) {
			typeCodecs = new HashMap<Class<?>, ObjectSerializer>();
		}
		typeCodecs.put(class1, nodeSer);
	}

	@Override
	public ObjectSerializer getObjectWriter(Class<?> clazz) {
		ObjectSerializer writer = get(clazz);
		if(writer==null){
			JSONCustom annotation = clazz.getAnnotation(JSONCustom.class);
			if (annotation != null && annotation.serializer() != Void.class) {
				ObjectSerializer os= createCustomSerializer(annotation.serializer());
				putInternal(clazz,os);
				return os;
			}
		}
		if (writer == null) {
			if (typeCodecs != null) {
				for (Map.Entry<Class<?>, ObjectSerializer> e : typeCodecs.entrySet()) {
					if (e.getKey().isAssignableFrom(clazz)) {
						writer = e.getValue();
						put(clazz, writer);
					}
				}
			}
		}
		return super.getObjectWriter(clazz);
	}

	private ObjectSerializer createCustomSerializer(Class<?> serializer) {
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
