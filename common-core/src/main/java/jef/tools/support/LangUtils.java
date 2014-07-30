package jef.tools.support;

/**
 * Java 基本语言和语法工具
 * 
 * @author Administrator
 * 
 */
public class LangUtils {
	public static final int NULL_IS_MAXIMUM = 0;
	public static final int NULL_IS_MINIMUM = 1;

	/**
	 * 获取两个对象中较小的
	 * 
	 * @param v1
	 * @param v2
	 * @param nullSupport
	 *            ，取常量NULL_IS_MAXIMUM 或者 NULL_IS_MINIMUM
	 * @return
	 */
	public static <T extends Comparable<T>> T min(T v1, T v2, int nullSupport) {
		if (v1 == v2)
			return v1;
		if (v1 == null) {
			return nullSupport == NULL_IS_MINIMUM ? v1 : v2;
		}
		if (v2 == null) {
			return nullSupport == NULL_IS_MINIMUM ? v2 : v1;
		}
		int v = v1.compareTo(v2);
		return (v > 0) ? v2 : v1;
	}

	/**
	 * 获取两个对象中较大的
	 * 
	 * @param v1
	 * @param v2
	 * @param nullSupport
	 *            ，取常量NULL_IS_MAXIMUM 或者 NULL_IS_MINIMUM
	 * @return
	 */
	public static <T extends Comparable<T>> T max(T v1, T v2, int nullSupport) {
		if (v1 == v2)
			return v1;
		if (v1 == null) {
			return nullSupport == NULL_IS_MAXIMUM ? v1 : v2;
		}
		if (v2 == null) {
			return nullSupport == NULL_IS_MAXIMUM ? v2 : v1;
		}
		int v = v1.compareTo(v2);
		return (v < 0) ? v2 : v1;
	}

	// public static void main(String[] args) {
	// System.out.println(max(32,null));
	// }

	/**
	 * 返回八个原生类型的默认数值(的装箱类型)
	 * 
	 * @param javaClass
	 * @return
	 */
	public static Object defaultValueOfPrimitive(Class<?> javaClass) {
		if (javaClass == Integer.TYPE) {
			return 0;
		} else if (javaClass == Short.TYPE) {
			return (short) 0;
		} else if (javaClass == Long.TYPE) {
			return 0L;
		} else if (javaClass == Float.TYPE) {
			return 0f;
		} else if (javaClass == Double.TYPE) {
			return (double) 0;
		} else if (javaClass == Byte.TYPE) {
			return (byte) 0;
		} else if (javaClass == Character.TYPE) {
			return (char) 0;
		} else if (javaClass == Boolean.TYPE) {
			return Boolean.FALSE;
		} else {
			throw new IllegalArgumentException(javaClass + " is not Primitive Type.");
		}
	}
}
