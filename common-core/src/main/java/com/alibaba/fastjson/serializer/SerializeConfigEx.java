package com.alibaba.fastjson.serializer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.sql.Clob;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import jef.json.JSONCustom;
import jef.tools.Assert;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONAware;
import com.alibaba.fastjson.JSONStreamAware;
import com.alibaba.fastjson.annotation.JSONType;
import com.alibaba.fastjson.util.ServiceLoader;
import com.alibaba.fastjson.util.TypeUtils;

/**
 * 对FastJSON的SerializeConfig稍作改变，以支持JSON/XML互转等一些功能。
 * 
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
	 * 
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

		if (writer == null) {
			final ClassLoader classLoader = JSON.class.getClassLoader();
			if (classLoader != Thread.currentThread().getContextClassLoader()) {
				try {
					for (Object o : ServiceLoader.load(AutowiredObjectSerializer.class, classLoader)) {

						if (!(o instanceof AutowiredObjectSerializer)) {
							continue;
						}

						AutowiredObjectSerializer autowired = (AutowiredObjectSerializer) o;
						for (Type forType : autowired.getAutowiredFor()) {
							put(forType, autowired);
						}
					}
				} catch (ClassCastException ex) {
					// skip
				}

				writer = get(clazz);
			}
		}

		if (writer == null) {
			if (Map.class.isAssignableFrom(clazz)) {
                put(clazz, MapSerializer.instance);
            } else if (List.class.isAssignableFrom(clazz)) {
                put(clazz, ListSerializer.instance);
            } else if (Collection.class.isAssignableFrom(clazz)) {
                put(clazz, CollectionCodec.instance);
            } else if (Date.class.isAssignableFrom(clazz)) {
                put(clazz, DateCodec.instance);
            } else if (JSONAware.class.isAssignableFrom(clazz)) {
                put(clazz, JSONAwareSerializer.instance);
            } else if (JSONSerializable.class.isAssignableFrom(clazz)) {
                put(clazz, JSONSerializableSerializer.instance);
            } else if (JSONStreamAware.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
            } else if (clazz.isEnum() || (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum())) {
                JSONType jsonType = clazz.getAnnotation(JSONType.class);
                if (jsonType != null && jsonType.serializeEnumAsJavaBean()) {
                    put(clazz, createJavaBeanSerializer0(clazz));
                } else {
                    put(clazz, EnumSerializer.instance);
                }
            } else if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                ObjectSerializer compObjectSerializer = getObjectWriter(componentType);
                put(clazz, new ArraySerializer(componentType, compObjectSerializer));
            } else if (Throwable.class.isAssignableFrom(clazz)) {
                SerializeBeanInfo beanInfo = TypeUtils.buildBeanInfo(clazz, null, propertyNamingStrategy);
                beanInfo.features |= SerializerFeature.WriteClassName.mask;
                put(clazz, new JavaBeanSerializer(beanInfo));
            } else if (TimeZone.class.isAssignableFrom(clazz) || Map.Entry.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
            } else if (Appendable.class.isAssignableFrom(clazz)) {
                put(clazz, AppendableSerializer.instance);
            } else if (Charset.class.isAssignableFrom(clazz)) {
                put(clazz, ToStringSerializer.instance);
            } else if (Enumeration.class.isAssignableFrom(clazz)) {
                put(clazz, EnumerationSerializer.instance);
            } else if (Calendar.class.isAssignableFrom(clazz) //
                    || XMLGregorianCalendar.class.isAssignableFrom(clazz)) {
                put(clazz, CalendarCodec.instance);
            } else if (Clob.class.isAssignableFrom(clazz)) {
                put(clazz, ClobSeriliazer.instance);
            } else if (TypeUtils.isPath(clazz)) {
                put(clazz, ToStringSerializer.instance);
            } else if (Iterator.class.isAssignableFrom(clazz)) {
                put(clazz, MiscCodec.instance);
			} else {
				boolean isCglibProxy = false;
				boolean isJavassistProxy = false;
				for (Class<?> item : clazz.getInterfaces()) {
					if (item.getName().equals("net.sf.cglib.proxy.Factory") || item.getName().equals("org.springframework.cglib.proxy.Factory")) {
						isCglibProxy = true;
						break;
					} else if (item.getName().equals("javassist.util.proxy.ProxyObject")) {
						isJavassistProxy = true;
						break;
					}
				}

				if (isCglibProxy || isJavassistProxy) {
					Class<?> superClazz = clazz.getSuperclass();

					ObjectSerializer superWriter = getObjectWriter(superClazz);
					put(clazz, superWriter);
					return superWriter;
				}

				if (Proxy.isProxyClass(clazz)) {
					put(clazz, createJavaBeanSerializer0(clazz));
				} else {
					put(clazz, createJavaBeanSerializer0(clazz));
				}
			}
			writer = get(clazz);
		}
		return writer;
	}

	private ObjectSerializer createJavaBeanSerializer0(Class<?> clazz) {
		SerializeBeanInfo beanInfo = TypeUtils.buildBeanInfo(clazz, null, propertyNamingStrategy);
		if (beanInfo.fields.length == 0 && Iterable.class.isAssignableFrom(clazz)) {
			return MiscCodec.instance;
		}
		return createJavaBeanSerializer(beanInfo);
	}

	@Override
	public ObjectSerializer createJavaBeanSerializer(SerializeBeanInfo clazz) {
		JSONCustom annotation = clazz.beanType.getAnnotation(JSONCustom.class);
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
