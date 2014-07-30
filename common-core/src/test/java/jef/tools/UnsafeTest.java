package jef.tools;

import jef.tools.reflect.UnsafeUtils;

import org.junit.Test;

public class UnsafeTest {

	@Test
	public void testUnsafe() {
		doTest(new int[][]{}.getClass());

		//
	}

	private void doTest(Class c) {
		System.out.println(c==int[][].class);
		
	}
}
