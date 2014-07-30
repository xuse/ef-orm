package jef.tools.support;

import org.junit.Assert;
import org.junit.Test;

public class TestsJefBase64 {
	@Test
	public void testBase64(){
		String s = "嘛嘛(⊙_⊙)？发1234567890";
		Assert.assertArrayEquals(s.getBytes(), JefBase64.decodeFast(JefBase64.encode(s.getBytes()))) ;
	}
}
