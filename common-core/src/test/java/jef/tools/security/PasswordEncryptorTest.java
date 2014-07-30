package jef.tools.security;

import static junit.framework.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import org.junit.Ignore;
import org.junit.Test;

public class PasswordEncryptorTest {

	@Test
	@Ignore
	public void test123(){
		SecretKey key=EncrypterUtil.toSecretKey("12345678".getBytes(), "DES/CBC/PKCS5PADDING");
		byte[] data=EncrypterUtil.encrypt("中国人民从此站起来了!", key);
		String decode=EncrypterUtil.decryptString(data, key,null);
		assertEquals("中国人民从此站起来了!",decode);
	}

	public void decryptTest(InputStream in, SecretKey key,AlgorithmParameterSpec spec){
		try{
			Cipher c1 = Cipher.getInstance(key.getAlgorithm());
			ByteArrayOutputStream out=new ByteArrayOutputStream(1024);
			c1.init(Cipher.DECRYPT_MODE,key,spec);
			byte[] b = new byte[1024];
			int len;
			while ((len = in.read(b)) != -1) {
				out.write(c1.update(b, 0, len));
			}
			out.write(c1.doFinal());
			System.out.println(new String(out.toByteArray())); 
		}catch(GeneralSecurityException e){
			throw new RuntimeException (e);
		} catch (IOException e) {
			throw new RuntimeException (e);
		}
	}
}
