package jef.tools;

import jef.tools.reflect.ConvertUtils;

import org.junit.Test;

public class TypeConvertorTest extends org.junit.Assert {
	final Integer I = 1;
	final Long L = 1L;
	final Short S = 1;
	final Double D = 1D;
	final Float F = 1f;
	final String S1 = "1";
	final String S_NULL = null;

	@Test
	public void testConvert() {
		check(ConvertUtils.toProperType(I, Object.class), Integer.class, I);
		check(ConvertUtils.toProperType(I, Integer.class), Integer.class, I);
		check(ConvertUtils.toProperType(I, Integer.TYPE), Integer.class, I);
		
		check(ConvertUtils.toProperType(L, Object.class), Long.class, L);
		check(ConvertUtils.toProperType(L, Integer.class), Integer.class, I);
		check(ConvertUtils.toProperType(L, Integer.TYPE), Integer.class, I);
		
		check(ConvertUtils.toProperType(S, Object.class), Short.class, S);
		check(ConvertUtils.toProperType(S, Integer.class), Integer.class, I);
		check(ConvertUtils.toProperType(S, Integer.TYPE), Integer.class, I);

		check(ConvertUtils.toProperType(D, Object.class), Double.class, D);
		check(ConvertUtils.toProperType(D, Integer.class), Integer.class, I);
		check(ConvertUtils.toProperType(D, Integer.TYPE), Integer.class, I);

		check(ConvertUtils.toProperType(F, Object.class), Float.class, F);
		check(ConvertUtils.toProperType(F, Integer.class), Integer.class, I);
		check(ConvertUtils.toProperType(F, Integer.TYPE), Integer.class, I);
		
	}

	private void check(Object properType, Class<?> class1, Object i2) {
		assertEquals(properType.getClass(), class1);
		assertEquals(properType, i2);
	}

}
