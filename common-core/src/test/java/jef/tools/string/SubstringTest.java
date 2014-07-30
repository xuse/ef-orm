package jef.tools.string;

import static junit.framework.Assert.assertEquals;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

import org.junit.Test;

public class SubstringTest {
	@Test
	public void test1(){
		String s1="abc12345678";
		String s2="12345678abc";
		Substring s1a=new Substring(s1,3);
		Substring s2a=new Substring(s2,0,8);
		assertEquals(s1a.length(),8);
		System.out.println(s2a+" "+s2a.length());
		assertEquals(s1a.compareTo(s2a),0);
	}
	
	
	@Test
	public void test2(){
		String s1="abc12345678";
		String s2="12345678abc";
		Substring s1a=new Substring(s1,3);
//		Substring s2a=new Substring(s2,0,8);
		
		System.out.println(s1a.subSequence(3, 5));
	}
	
	

	@Test
	public void test3(){
		String s1="abc12345678";
		String s2="12345678abc";
		
		System.out.println(IOUtils.removeExt(s1));
		System.out.println(IOUtils.removeExt("asd.asdsd.cfd.txt"));
		System.out.println(IOUtils.removeExt(".asd"));
		
	}
}
