package jef.tools.security;

import jef.tools.Assert;
import jef.tools.security.cplus.TripleDES;
import junit.framework.TestCase;

public class TripleDEStest extends TestCase{
	static byte[] key = "781296-5e32-89122".getBytes();
	
	public void testTripleDes(){
		String source = "测试数据ff";
		TripleDES t = new TripleDES();
		String pass = t.cipher2(key, source);
		System.out.println(pass);
		System.out.println(source.length());
		System.out.println(pass.length());

		String result = t.decipher2(key, pass);
		System.out.println(result);
		Assert.equals(result, source);
	}
	
	
}
