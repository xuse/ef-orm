package jef.tools;

import org.junit.Test;

public class RegexpUtilsTest {
	@Test
	public void testRegexp(){
		String a="1234567890abcdefGG";
		System.out.println(a.matches("[a-fA-F0-9]{16,}"));
		
		
	}

}
