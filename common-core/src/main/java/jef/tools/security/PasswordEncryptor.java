/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.tools.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

/**
 * 基于密码的加密解密器。(Password based encryption,简称PBE)
 * @author Administrator
 */
public class PasswordEncryptor{
	/**
	 * 标准SUN JCE支持的PBE算法有四种
	 * PBEWithMD5AndDES / PBEWithMD5AndTripleDES/PBEWithSHA1AndDESede/ PBEWithSHA1AndRC2_40
	 */
	public enum Alogorithm{
		PBEWithMD5AndDES,PBEWithMD5AndTripleDES,
		PBEWithSHA1AndDESede,PBEWithSHA1AndRC2_40
	}
	
	/**
	 * 默认采用PBEWithMD5AndDES算法
	 */
	private Alogorithm pbeAlogorithm=Alogorithm.PBEWithMD5AndDES;
	private byte[] pbe_salt;
	private int pbe_iterationCount;
	PasswordEncryptor(byte[] pbe_salt,int pbe_iterationCount){
		this.pbe_iterationCount=pbe_iterationCount;
		this.pbe_salt=pbe_salt;
	}
	
	/**
	 * 获得算法
	 * @return
	 */
	public PasswordEncryptor.Alogorithm getPbeAlogorithm() {
		return pbeAlogorithm;
	}

	/**
	 * 设置算法
	 * @param pbeAlogorithm
	 */
	public void setPbeAlogorithm(PasswordEncryptor.Alogorithm pbeAlogorithm) {
		this.pbeAlogorithm = pbeAlogorithm;
	}


	/**
	 * 基于密码的加密
	 * @param in
	 * @param password
	 * @return
	 */
	public byte[] encrypt(InputStream in,String password){
		try {
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), pbe_salt, pbe_iterationCount);
			SecretKey key = SecretKeyFactory.getInstance(pbeAlogorithm.name()).generateSecret(keySpec);
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(pbe_salt, pbe_iterationCount);
			return EncrypterUtil.encrypt(in,key,paramSpec,false);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	public byte[] encrypt(byte[] in,String password){
		return encrypt(new ByteArrayInputStream(in), password);
	}
	
	public byte[] decrypt(byte[] in,String password){
		return decrypt(new ByteArrayInputStream(in), password);
	}
	/**
	 * 基于密码的解密，实际算法为PBEWithMD5AndDES
	 * @param in
	 * @param password
	 * @return
	 */
	public byte[] decrypt(InputStream in,String password){
		try{
			KeySpec keySpec = new PBEKeySpec(password.toCharArray(), pbe_salt, pbe_iterationCount);
			SecretKey key = SecretKeyFactory.getInstance(pbeAlogorithm.name()).generateSecret(keySpec);
			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(pbe_salt, pbe_iterationCount);
			return EncrypterUtil.decrypt(in,key,paramSpec);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}	
	
}
