package jef.tools;

import java.util.Date;

public final class Primitives {
	private Primitives() {
	}

	/**
	 * 返回八个原生类型的默认数值(的装箱类型)
	 * 
	 * @param javaClass
	 *            数据类型
	 * @return 返回该种技术类型的默认数值
	 * @throws IllegalArgumentException
	 *             如果传入的javaClass不是八种基础类型之一抛出。
	 */
	public static Object defaultValueOfPrimitive(Class<?> javaClass) {
		if (javaClass.isPrimitive()) {
			switch (javaClass.getName().hashCode()) {
			case 104431:
				return 0;
			case 109413500:
				return Short.valueOf((short) 0);
			case 3327612:
				return 0L;
			case -1325958191:
				return 0d;
			case 64711720:
				return Boolean.FALSE;
			case 97526364:
				return 0f;
			case 3052374:
				return (char) 0;
			case 3039496:
				return Byte.valueOf((byte) 0);
			}
		}
		throw new IllegalArgumentException(javaClass + " is not Primitive Type.");
	}

	/**
	 * 包装类转换为原生类
	 * 
	 * @param wrapperClass
	 * @return 如果不能转换，则返回输入的类
	 */
	public static Class<?> toPrimitiveClass(Class<?> wrapperClass) {
		switch (wrapperClass.getName()) {
		case "java.lang.Integer":
			return Integer.TYPE;
		case "java.lang.Byte":
			return Byte.TYPE;
		case "java.lang.Short":
			return Short.TYPE;
		case "java.lang.Long":
			return Long.TYPE;
		case "java.lang.Float":
			return Float.TYPE;
		case "java.lang.Double":
			return Double.TYPE;
		case "java.lang.Character":
			return Character.TYPE;
		case "java.lang.Boolean":
			return Boolean.TYPE;
		default:
			return wrapperClass;
		}
	}

	/**
	 * 将8原生类型的类转换为对应的包装的类型。
	 * 
	 * @param primitiveClass
	 * @return 包装类型
	 */
	public static Class<?> toWrapperClass(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			//不用担心hash值算法变化。因为java7支持String switch以后，编译出的类中，String的hash值就是固定的。因此新版JDK为了向下兼容，必须确保hash值相同。
			switch (primitiveClass.getName().hashCode()) {
			case 104431:
				return Integer.class;
			case 109413500:
				return Short.class;
			case 3327612:
				return Long.class;
			case -1325958191:
				return Double.class;
			case 64711720:
				return Boolean.class;
			case 97526364:
				return Float.class;
			case 3052374:
				return Character.class;
			case 3039496:
				return Byte.class;
			}
		}
		return primitiveClass;
	}

	/**
	 * 得到原生对象和String的缺省值。
	 * 
	 * @param cls
	 *            类型
	 * 
	 * @return 指定类型数据的缺省值。如果传入类型是primitive和String之外的类型返回null。
	 */
	public static Object defaultValueForBasicType(Class<?> cls) {
		if (cls == String.class) {
			return "";
		} else if (cls.isPrimitive()) {
			return defaultValueOfPrimitive(cls);
		}
		return null;
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static int unbox(Integer value, int defaultValue) {
		return value == null ? defaultValue : value.intValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static short unbox(Short value, short defaultValue) {
		return value == null ? defaultValue : value.shortValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static long unbox(Long value, long defaultValue) {
		return value == null ? defaultValue : value.longValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static double unbox(Double value, double defaultValue) {
		return value == null ? defaultValue : value.doubleValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static float unbox(Float value, float defaultValue) {
		return value == null ? defaultValue : value.floatValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static char unbox(Character value, char defaultValue) {
		return value == null ? defaultValue : value.charValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static byte unbox(Byte value, byte defaultValue) {
		return value == null ? defaultValue : value.byteValue();
	}

	/**
	 * 安全拆箱
	 * 
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static boolean unbox(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value.booleanValue();
	}
	
	/**
	 * 安全拆箱（扩展）
	 * @param value
	 * @param defaultValue
	 * @return
	 */
	public static long unbox(Date value, long defaultValue) {
		return value == null ? defaultValue : value.getTime();
	}
}
