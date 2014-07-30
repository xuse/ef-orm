package org.easyframe.fastjson.util;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jef.tools.reflect.FieldAccessor;

import org.easyframe.fastjson.JSONException;
import org.easyframe.fastjson.annotation.JSONCreator;
import org.easyframe.fastjson.annotation.JSONField;
import org.easyframe.fastjson.annotation.JSONType;

public class DeserializeBeanInfo {

	private final Class<?> clazz;
	private Constructor<?> defaultConstructor;
	private Constructor<?> creatorConstructor;
	private Method factoryMethod;

	private final List<FieldInfo> fieldList = new ArrayList<FieldInfo>();
	private final List<FieldInfo> sortedFieldList = new ArrayList<FieldInfo>();

	public DeserializeBeanInfo(Class<?> clazz) {
		super();
		this.clazz = clazz;
	}

	public Constructor<?> getDefaultConstructor() {
		return defaultConstructor;
	}

	public void setDefaultConstructor(Constructor<?> defaultConstructor) {
		this.defaultConstructor = defaultConstructor;
	}

	public Constructor<?> getCreatorConstructor() {
		return creatorConstructor;
	}

	public void setCreatorConstructor(Constructor<?> createConstructor) {
		this.creatorConstructor = createConstructor;
	}

	public Method getFactoryMethod() {
		return factoryMethod;
	}

	public void setFactoryMethod(Method factoryMethod) {
		this.factoryMethod = factoryMethod;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public List<FieldInfo> getFieldList() {
		return fieldList;
	}

	public List<FieldInfo> getSortedFieldList() {
		return sortedFieldList;
	}

	public boolean add(FieldInfo field) {
		if (index.add(field.getName())) {
			fieldList.add(field);
			sortedFieldList.add(field);
			return true;
		}
		return false;
	}

	private Set<String> index = new HashSet<String>();

	private DeserializeBeanInfo finish() {
		Collections.sort(sortedFieldList);
		index.clear();
		index = null;
		return this;
	}

	public static DeserializeBeanInfo computeSetters(Class<?> clazz, Type type) {
		DeserializeBeanInfo beanInfo = new DeserializeBeanInfo(clazz);
		// ////////////处理构造器、工厂方法等
		JSONType jsonType = clazz.getAnnotation(JSONType.class);
		Constructor<?> defaultConstructor = getDefaultConstructor(clazz);
		if (defaultConstructor != null) {
			defaultConstructor.setAccessible(true);
			beanInfo.setDefaultConstructor(defaultConstructor);
		} else if (defaultConstructor == null && !(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))) {
			Constructor<?> creatorConstructor = getCreatorConstructor(clazz);
			if (creatorConstructor != null) {
				creatorConstructor.setAccessible(true);
				beanInfo.setCreatorConstructor(creatorConstructor);

				for (int i = 0; i < creatorConstructor.getParameterTypes().length; ++i) {
					Annotation[] paramAnnotations = creatorConstructor.getParameterAnnotations()[i];
					JSONField fieldAnnotation = null;
					for (Annotation paramAnnotation : paramAnnotations) {
						if (paramAnnotation instanceof JSONField) {
							fieldAnnotation = (JSONField) paramAnnotation;
							break;
						}
					}
					if (fieldAnnotation == null) {
						throw new JSONException("illegal json creator");
					}

					Class<?> fieldClass = creatorConstructor.getParameterTypes()[i];
					Type fieldType = creatorConstructor.getGenericParameterTypes()[i];
					Field field = getField(clazz, fieldAnnotation.name());
					FieldInfo fieldInfo = new FieldInfo(fieldAnnotation.name(), clazz, fieldClass, fieldType, field);
					beanInfo.add(fieldInfo);
				}
				return beanInfo.finish();
			}

			Method factoryMethod = getFactoryMethod(clazz);
			if (factoryMethod != null) {
				factoryMethod.setAccessible(true);
				beanInfo.setFactoryMethod(factoryMethod);

				for (int i = 0; i < factoryMethod.getParameterTypes().length; ++i) {
					Annotation[] paramAnnotations = factoryMethod.getParameterAnnotations()[i];
					JSONField fieldAnnotation = null;
					for (Annotation paramAnnotation : paramAnnotations) {
						if (paramAnnotation instanceof JSONField) {
							fieldAnnotation = (JSONField) paramAnnotation;
							break;
						}
					}
					if (fieldAnnotation == null) {
						throw new JSONException("illegal json creator");
					}

					Class<?> fieldClass = factoryMethod.getParameterTypes()[i];
					Type fieldType = factoryMethod.getGenericParameterTypes()[i];
					Field field = getField(clazz, fieldAnnotation.name());
					FieldInfo fieldInfo = new FieldInfo(fieldAnnotation.name(), clazz, fieldClass, fieldType, field);
					beanInfo.add(fieldInfo);
				}
				return beanInfo.finish();
			}

			throw new JSONException("default constructor not found. " + clazz);
		}
		boolean fieldAccess=jsonType!=null && jsonType.fieldAccess();
		if (!fieldAccess) {
			// ////////////////处理属性
			for (Method method : clazz.getMethods()) {
				String methodName = method.getName();
				if (methodName.length() < 4) {
					continue;
				}
				// 静态方法肯定也不是。参数只能有一个。
				if (Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1) {
					continue;
				}

				// support builder set (返回自身的set方法也被认可)
				if (!(method.getReturnType().equals(Void.TYPE) || method.getReturnType().equals(clazz))) {
					continue;
				}

				// Annotation查找
				JSONField annotation = method.getAnnotation(JSONField.class);
				if (annotation == null) {
					annotation = TypeUtils.getSupperMethodAnnotation(clazz, method);
				}

				if (annotation != null) {
					if (!annotation.deserialize()) { // 无需反序列化的字段直接跳过
						continue;
					}
					if (annotation.name().length() != 0) {// 通过JSONField定义了属性名称
						String propertyName = annotation.name();
						beanInfo.add(new FieldInfo(propertyName, method, null, clazz, type));
						method.setAccessible(true);
						continue;
					}
				}
				// Set方法的特征。注意@JSONField优先于这一特征判断，因此@JSONField可以添加在非setXxxx名称的方法上
				if (!methodName.startsWith("set")) {
					continue;
				}

				char c3 = methodName.charAt(3);

				String propertyName;
				if (Character.isUpperCase(c3)) {
					if (TypeUtils.compatibleWithJavaBean) {
						propertyName = Introspector.decapitalize(methodName.substring(3));
					} else {
						propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
					}
				} else if (c3 == '_') {
					propertyName = methodName.substring(4);
					// } else if (c3 == 'f') {
					// propertyName = methodName.substring(3);//这是为了满足什么条件？？？
				} else {
					continue;// 如果set后的字母为小写，将被忽略。
				}

				Field field = getField(clazz, propertyName);
				if (field == null && method.getParameterTypes()[0] == boolean.class) {
					String isFieldName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
					field = getField(clazz, isFieldName);
				}

				if (field != null) {
					JSONField fieldAnnotation = field.getAnnotation(JSONField.class);

					if (fieldAnnotation != null && fieldAnnotation.name().length() != 0) {
						propertyName = fieldAnnotation.name();

						beanInfo.add(new FieldInfo(propertyName, method, field, clazz, type));
						continue;
					}
				}

				beanInfo.add(new FieldInfo(propertyName, method, null, clazz, type));
				method.setAccessible(true);
			}
		}
		if(fieldAccess){//按Field存取
			Class<?> clz=clazz;
			while(clz!=Object.class){
				for (Field field : clz.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					String propertyName = field.getName();
					if (beanInfo.index.contains(propertyName)) {
						continue;
					}

					JSONField fieldAnnotation = field.getAnnotation(JSONField.class);
					if (fieldAnnotation != null && fieldAnnotation.name().length() != 0) {
						propertyName = fieldAnnotation.name();
					}
					FieldInfo fi=new FieldInfo(propertyName, null, field, clazz, type);
					fi.accessor=FieldAccessor.generateAccessor(fi.getField());
					beanInfo.add(fi);
				}
				clz=clz.getSuperclass();
			}
		}else{
			// ///处理公有字段
			for (Field field : clazz.getFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}

				String propertyName = field.getName();
				if (beanInfo.index.contains(propertyName)) {
					continue;
				}

				JSONField fieldAnnotation = field.getAnnotation(JSONField.class);
				if (fieldAnnotation != null && fieldAnnotation.name().length() != 0) {
					propertyName = fieldAnnotation.name();
				}
				beanInfo.add(new FieldInfo(propertyName, null, field, clazz, type));
			}

		}

		for (Method method : clazz.getMethods()) {
			String methodName = method.getName();
			if (methodName.length() < 4) {
				continue;
			}

			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			if (methodName.startsWith("get") && Character.isUpperCase(methodName.charAt(3))) {
				if (method.getParameterTypes().length != 0) {
					continue;
				}

				if (Collection.class.isAssignableFrom(method.getReturnType()) //
						|| Map.class.isAssignableFrom(method.getReturnType()) //
						|| AtomicBoolean.class == method.getReturnType() //
						|| AtomicInteger.class == method.getReturnType() //
						|| AtomicLong.class == method.getReturnType() //
				) {
					String propertyName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);

					if (beanInfo.index.contains(propertyName)) {
						continue;
					}

					beanInfo.add(new FieldInfo(propertyName, method, null, clazz, type));
					method.setAccessible(true);
				}
			}
		}

		return beanInfo.finish();
	}

	public static Field getField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (Exception e) {
			return null;
		}
	}

	public static Constructor<?> getDefaultConstructor(Class<?> clazz) {
		if (Modifier.isAbstract(clazz.getModifiers())) {
			return null;
		}

		Constructor<?> defaultConstructor = null;
		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			if (constructor.getParameterTypes().length == 0) {
				defaultConstructor = constructor;
				break;
			}
		}

		if (defaultConstructor == null) {
			if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
				for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
					if (constructor.getParameterTypes().length == 1 && constructor.getParameterTypes()[0].equals(clazz.getDeclaringClass())) {
						defaultConstructor = constructor;
						break;
					}
				}
			}
		}

		return defaultConstructor;
	}

	public static Constructor<?> getCreatorConstructor(Class<?> clazz) {
		Constructor<?> creatorConstructor = null;

		for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			JSONCreator annotation = constructor.getAnnotation(JSONCreator.class);
			if (annotation != null) {
				if (creatorConstructor != null) {
					throw new JSONException("multi-json creator");
				}

				creatorConstructor = constructor;
				break;
			}
		}
		return creatorConstructor;
	}

	public static Method getFactoryMethod(Class<?> clazz) {
		Method factoryMethod = null;

		for (Method method : clazz.getDeclaredMethods()) {
			if (!Modifier.isStatic(method.getModifiers())) {
				continue;
			}

			if (!clazz.isAssignableFrom(method.getReturnType())) {
				continue;
			}

			JSONCreator annotation = method.getAnnotation(JSONCreator.class);
			if (annotation != null) {
				if (factoryMethod != null) {
					throw new JSONException("multi-json creator");
				}

				factoryMethod = method;
				break;
			}
		}
		return factoryMethod;
	}

}
