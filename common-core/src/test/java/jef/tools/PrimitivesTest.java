package jef.tools;

import static org.junit.Assert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class PrimitivesTest {

	@Test
	public void testTypes() {
		assertThat(Primitives.toWrapperClass(int.class), CoreMatchers.<Class<?>>equalTo(Integer.class));
		assertThat(Primitives.toWrapperClass(short.class), CoreMatchers.<Class<?>>equalTo(Short.class));
		assertThat(Primitives.toWrapperClass(long.class), CoreMatchers.<Class<?>>equalTo(Long.class));
		assertThat(Primitives.toWrapperClass(double.class), CoreMatchers.<Class<?>>equalTo(Double.class));
		assertThat(Primitives.toWrapperClass(float.class), CoreMatchers.<Class<?>>equalTo(Float.class));
		assertThat(Primitives.toWrapperClass(char.class), CoreMatchers.<Class<?>>equalTo(Character.class));
		assertThat(Primitives.toWrapperClass(boolean.class), CoreMatchers.<Class<?>>equalTo(Boolean.class));
		assertThat(Primitives.toWrapperClass(byte.class), CoreMatchers.<Class<?>>equalTo(Byte.class));
		
		
		assertThat(toWrapperClass0(int.class), CoreMatchers.<Class<?>>equalTo(Integer.class));
		assertThat(toWrapperClass0(short.class), CoreMatchers.<Class<?>>equalTo(Short.class));
		assertThat(toWrapperClass0(long.class), CoreMatchers.<Class<?>>equalTo(Long.class));
		assertThat(toWrapperClass0(double.class), CoreMatchers.<Class<?>>equalTo(Double.class));
		assertThat(toWrapperClass0(float.class), CoreMatchers.<Class<?>>equalTo(Float.class));
		assertThat(toWrapperClass0(char.class), CoreMatchers.<Class<?>>equalTo(Character.class));
		assertThat(toWrapperClass0(boolean.class), CoreMatchers.<Class<?>>equalTo(Boolean.class));
		assertThat(toWrapperClass0(byte.class), CoreMatchers.<Class<?>>equalTo(Byte.class));
		
		
		assertThat(Primitives.toPrimitiveClass(Integer.class), CoreMatchers.<Class<?>>equalTo(int.class));
		assertThat(Primitives.toPrimitiveClass(Short.class), CoreMatchers.<Class<?>>equalTo(short.class));
		assertThat(Primitives.toPrimitiveClass(Long.class), CoreMatchers.<Class<?>>equalTo(long.class));
		assertThat(Primitives.toPrimitiveClass(Double.class), CoreMatchers.<Class<?>>equalTo(double.class));
		assertThat(Primitives.toPrimitiveClass(Float.class), CoreMatchers.<Class<?>>equalTo(float.class));
		assertThat(Primitives.toPrimitiveClass(Character.class), CoreMatchers.<Class<?>>equalTo(char.class));
		assertThat(Primitives.toPrimitiveClass(Boolean.class), CoreMatchers.<Class<?>>equalTo(boolean.class));
		assertThat(Primitives.toPrimitiveClass(Byte.class), CoreMatchers.<Class<?>>equalTo(byte.class));
	}
 
	public static Class<?> toWrapperClass4(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			String s = primitiveClass.getName();
			if ("int".equals(s)) {
				return Integer.class;
			} else if ("short".equals(s)) {
				return Short.class;
			} else if ("long".equals(s)) {
				return Long.class;
			} else if ("boolean".equals(s)) {
				return Boolean.class;
			} else if ("float".equals(s)) {
				return Float.class;
			} else if ("double".equals(s)) {
				return Double.class;
			} else if ("char".equals(s)) {
				return Character.class;
			} else if ("byte".equals(s)) {
				return Byte.class;
			}
		}
		return primitiveClass;
	}

	// 千万次
	// 524,094,925ns
	// 513,577,520ns
	// 532,154,938ns
	@Test
	public void testCallTypes4() {
		toWrapperClass4(int.class);
		toWrapperClass4(short.class);
		toWrapperClass4(long.class);
		toWrapperClass4(double.class);
		toWrapperClass4(float.class);
		toWrapperClass4(char.class);
		toWrapperClass4(boolean.class);
		toWrapperClass4(byte.class);
		long s1 = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			toWrapperClass4(int.class);
			toWrapperClass4(short.class);
			toWrapperClass4(long.class);
			toWrapperClass4(double.class);
			toWrapperClass4(float.class);
			toWrapperClass4(char.class);
			toWrapperClass4(boolean.class);
			toWrapperClass4(byte.class);
		}
		long s2 = System.nanoTime();
		System.out.println((s2 - s1) + "ns");
	}

	public static Class<?> toWrapperClass3(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			String s = primitiveClass.getName();
			switch (s) {
			case "int":
				return Integer.class;
			case "short":
				return Short.class;
			case "long":
				return Long.class;
			case "boolean":
				return Boolean.class;
			case "float":
				return Float.class;
			case "double":
				return Double.class;
			case "char":
				return Character.class;
			case "byte":
				return Byte.class;
			}
		}
		return primitiveClass;
	}

	// 千万次
	// 522,559,007ns
	// 521,130,300ns
	// 520,663,415ns
	@Test
	public void testCallTypes3() {
		toWrapperClass3(int.class);
		toWrapperClass3(short.class);
		toWrapperClass3(long.class);
		toWrapperClass3(double.class);
		toWrapperClass3(float.class);
		toWrapperClass3(char.class);
		toWrapperClass3(boolean.class);
		toWrapperClass3(byte.class);
		long s1 = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			toWrapperClass3(int.class);
			toWrapperClass3(short.class);
			toWrapperClass3(long.class);
			toWrapperClass3(double.class);
			toWrapperClass3(float.class);
			toWrapperClass3(char.class);
			toWrapperClass3(boolean.class);
			toWrapperClass3(byte.class);
		}
		long s2 = System.nanoTime();
		System.out.println((s2 - s1) + "ns");
	}

	public static Class<?> toWrapperClass2(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			String s = primitiveClass.getName();
			switch (s.charAt(1) + s.charAt(2)) {
			case 226:
				return Integer.class;
			case 215:
				return Short.class;
			case 221:
				return Long.class;
			case 222:
				return Boolean.class;
			case 219:
				return Float.class;
			case 228:
				return Double.class;
			case 201:
				return Character.class;
			case 237:
				return Byte.class;
			}
		}
		return primitiveClass;
	}

	// 千万次
	// 148,698,889ns
	// 148,904,088ns
	// 148,243,148ns
	@Test
	public void testCallTypes2() {
		toWrapperClass2(int.class);
		toWrapperClass2(short.class);
		toWrapperClass2(long.class);
		toWrapperClass2(double.class);
		toWrapperClass2(float.class);
		toWrapperClass2(char.class);
		toWrapperClass2(boolean.class);
		toWrapperClass2(byte.class);
		long s1 = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			toWrapperClass2(int.class);
			toWrapperClass2(short.class);
			toWrapperClass2(long.class);
			toWrapperClass2(double.class);
			toWrapperClass2(float.class);
			toWrapperClass2(char.class);
			toWrapperClass2(boolean.class);
			toWrapperClass2(byte.class);
		}
		long s2 = System.nanoTime();
		System.out.println((s2 - s1) + "ns");
	}

	// 千万次
	// 124,229,880ns
	// 124,247,940ns
	// 123,613,515ns
	@Test
	public void testCallTypes() {
		toWrapperClass(int.class);
		toWrapperClass(short.class);
		toWrapperClass(long.class);
		toWrapperClass(double.class);
		toWrapperClass(float.class);
		toWrapperClass(char.class);
		toWrapperClass(boolean.class);
		toWrapperClass(byte.class);
		long s1 = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			toWrapperClass(int.class);
			toWrapperClass(short.class);
			toWrapperClass(long.class);
			toWrapperClass(double.class);
			toWrapperClass(float.class);
			toWrapperClass(char.class);
			toWrapperClass(boolean.class);
			toWrapperClass(byte.class);
		}
		long s2 = System.nanoTime();
		System.out.println((s2 - s1) + "ns");
	}
	
	//113,722,745
	@Test
	public void testCallTypes0() {
		toWrapperClass0(int.class);
		toWrapperClass0(short.class);
		toWrapperClass0(long.class);
		toWrapperClass0(double.class);
		toWrapperClass0(float.class);
		toWrapperClass0(char.class);
		toWrapperClass0(boolean.class);
		toWrapperClass0(byte.class);
		long s1 = System.nanoTime();
		for (int i = 0; i < 10000000; i++) {
			toWrapperClass0(int.class);
			toWrapperClass0(short.class);
			toWrapperClass0(long.class);
			toWrapperClass0(double.class);
			toWrapperClass0(float.class);
			toWrapperClass0(char.class);
			toWrapperClass0(boolean.class);
			toWrapperClass0(byte.class);
		}
		long s2 = System.nanoTime();
		System.out.println((s2 - s1) + "ns");
	}

	public static Class<?> toWrapperClass(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
			String s = primitiveClass.getName();
			switch (s.charAt(0) + (s.charAt(1)/2)) {
			case 160:
				return Integer.class;
			case 167:
				return Short.class;
			case 163:
				return Long.class;
			case 153:
				return Boolean.class;
			case 156:
				return Float.class;
			case 155:
				return Double.class;
			case 151:
				return Character.class;
			case 158:
				return Byte.class;
			}
		}
		return primitiveClass;
	}
	
	public static Class<?> toWrapperClass0(Class<?> primitiveClass) {
		if (primitiveClass.isPrimitive()) {
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

}
