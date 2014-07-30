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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

import jef.common.wrapper.Holder;
import jef.tools.security.EncrypterUtil.Transport;

/**
 * 公开密钥生成体制实现
 * 基于Diffie-Hellman算法
 * 公开密钥密码体制的奠基人Diffie和Hellman所提出的 "指数密钥一致协议"(Exponential Key Agreement Protocol),
 * 该协议不要求别的安全性 先决条件,允许两名用户在公开媒体上交换信息以生成"一致"的,可以共享的密钥。
 * 在这种协商中，被交换的是双方的公钥，基于非对称加密的数学依据，是很难根据公钥推算出双方的私钥的。
 * 从而也就几乎不可能获得协商后的密钥。
 * @author Jiyi
 */
public class KeyConsult {
	private KeyPairGenerator kpairGen;
	private KeyFactory keyFac;
	{
		try {
			kpairGen = KeyPairGenerator.getInstance("DH");
			keyFac = KeyFactory.getInstance("DH");
			keyAgree = KeyAgreement.getInstance("DH");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private PrivateKey key;
	private PublicKey pubKey;

	private KeyAgreement keyAgree;
	private PublicKey phaseToDo = null;

	/**
	 * 发送公钥给对方
	 * 
	 * @param t
	 */
	public void send(Transport t) {
		byte[] data=pubKey.getEncoded();
//		System.out.println("sending:"+ StringUtils.byte2hex(data));
		try {
			t.send(data);
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	/**
	 * 创建一个协商会话
	 */
	public KeyConsult() {
		kpairGen.initialize(512);
		KeyPair aliceKpair = kpairGen.generateKeyPair();
		this.key = aliceKpair.getPrivate();
		try {
			keyAgree.init(key);// 用本地私钥初始化
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
		this.pubKey = aliceKpair.getPublic();
	}

	/**
	 * 当接收到对方的协商后，创建一个协商会话
	 */
	public KeyConsult(byte[] alicePubKeyEnc) {
//		System.out.println("create from received.");
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);
		try {
			PublicKey received = keyFac.generatePublic(x509KeySpec);
			/*
			 * B必须用相同的参数初始化的他的DH KEY对,所以要从A发给他的公开密钥,中读出参数,再用这个参数初始化他的 DH key对
			 */
			DHParameterSpec dhParamSpec = ((DHPublicKey) received).getParams();
			kpairGen.initialize(dhParamSpec);
			KeyPair bobKpair = kpairGen.generateKeyPair();

			this.key = bobKpair.getPrivate();
			keyAgree.init(key);

			this.pubKey = bobKpair.getPublic();
			this.phaseToDo = received;
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 当每一次收到对方的公钥后，就加密一次，最后一次如果要生成key,需要调用generate=true
	 * 
	 * @param recKey
	 * @param generate
	 */
	private void doPhase(boolean generate) {
		if (phaseToDo == null)
			return;
		try {
			keyAgree.doPhase(phaseToDo, generate);
			phaseToDo = null;
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 要生成之前必须先调用doPhase(xx,true)
	 * 
	 * @return
	 */
	public SecretKey generateKey(String typeNeed) {
		if (phaseToDo != null)
			this.doPhase(true);
		try {
			SecretKey bobDesKey = keyAgree.generateSecret(typeNeed);
			return bobDesKey;
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (IllegalStateException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void receive(byte[] data) {
		receive(data,0,data.length);
		
	}
	
	public void receive(byte[] data,int offset,int len) {
		try {
			X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(data);
			PublicKey received = keyFac.generatePublic(x509KeySpec);
			if (phaseToDo != null)
				this.doPhase(false);
			this.phaseToDo = received;
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	public static void test(){
		final Holder<byte[]> h=new Holder<byte[]>();
		Transport t=new Transport(){
			
			public void send(byte[] encoded) {
				h.set(encoded);
			}
		};
		//A发起
		KeyConsult a=new KeyConsult();
		a.send(t);

		//B C接收
		KeyConsult b=new KeyConsult(h.get());
		b.send(t);
		
		a.receive(h.get());
				
		SecretKey key1=a.generateKey("DES");
		SecretKey key2=b.generateKey("DES");
		
		System.out.println(key1);
		System.out.println(key2);
		System.out.println(key1.equals(key2));
	}
}
