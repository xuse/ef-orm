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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jef.common.log.LogUtil;
import jef.tools.ArrayUtils;
import jef.tools.Assert;
import jef.tools.IOUtils;
import jef.tools.StringUtils;
import jef.tools.io.ReaderInputStream;
import jef.tools.reflect.BeanUtils;
import jef.tools.reflect.MethodEx;
import jef.tools.support.JefBase64;

public class EncrypterUtil {
	// static{
	// Security.addProvider(new
	// org.bouncycastle.jce.provider.BouncyCastleProvider());//添加PKCS7Padding支持
	// }

	/**
	 * 算法分成这些大类
	 */
	public enum AlgorithmType {
		KeyGenerator, // 密钥生成算法(KeyGenerator)
		KeyFactory, // 密钥生成算法(KeyFactory)
		KeyAgreement, // 公开密钥协商算法
		Cipher, // 加密/解密算法
		KeyStore, // 密钥编码/解码算法
		MessageDigest, // 消息摘要算法
		Signature, // 数字签名算法
		AlgorithmParameterGenerator, AlgorithmParameters, CertificateFactory, CertPathBuilder, CertPathValidator, Mac, Policy, SaslClientFactory, SaslServerFactory, SecretKeyFactory, SecureRandom, SSLContext, TerminalFactory, TransformService, TrustManagerFactory, XMLSignatureFactory
	}

	/**
	 * 安全算法元数据查询，查询类安全服务所支持的全部算法服务
	 * 
	 * @param type
	 * @return
	 */
	public static Service[] getSupportedAlgorithm(AlgorithmType type) {
		List<Service> list = new ArrayList<Service>();
		for (Provider p : Security.getProviders()) {
			for (Service s : p.getServices()) {
				if (type == null || s.getType().equals(type.name())) {
					list.add(s);
				}
			}
		}
		return list.toArray(new Service[0]);
	}

	/**
	 * 安全算法元数据查询，得到目前已支持的算法名称(含别名)
	 * 
	 * @param type
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String[] getSupportedAlgorithmName(AlgorithmType type)
			throws Exception {
		Service[] algoms = getSupportedAlgorithm(type);
		MethodEx m = BeanUtils.getCompatibleMethod(Service.class, "getAliases");
		List<String> names = new ArrayList<String>();
		for (Service s : algoms) {
			names.add(s.getAlgorithm());

			List<String> alias = ((List<String>) m.invoke(s));
			names.addAll(alias);
		}
		return names.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
	}

	/**
	 * 安全算法元数据查询，根据指定的算法，获得所有使用该算法的服务
	 * 
	 * @param name
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static Service[] getAlgorithmService(String name) throws Exception {
		List<Service> list = new ArrayList<Service>();
		MethodEx m = BeanUtils.getCompatibleMethod(Service.class, "getAliases");
		for (Provider p : Security.getProviders()) {
			for (Service s : p.getServices()) {
				boolean flag = false;
				if (s.getAlgorithm().equalsIgnoreCase(name)) {
					flag = true;
				} else {
					List<String> aliases = ((List<String>) m.invoke(s));
					for (String alias : aliases) {
						if (alias.equalsIgnoreCase(name)) {
							flag = true;
							break;
						}
					}
				}
				if (flag) {
					list.add(s);
				}
			}
		}
		return list.toArray(new Service[0]);
	}

	/**
	 * 将密码转换为DESKey
	 * 
	 * @param password
	 * @return
	 */
	public static final SecretKey toDESKey(String password) {
		byte[] bb = password.getBytes();
		Assert.isTrue(bb.length > 7, "the secretKey for DES must be 8 bytes at least.");
		try {
			KeySpec keySpec = new DESKeySpec(bb);
			SecretKey key = SecretKeyFactory.getInstance("DES").generateSecret(
					keySpec);
			return key;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 将密码转换为3DESKey
	 * 
	 * @param password
	 * @return
	 */
	public static final SecretKey toDESedeKey(String password) {
		byte[] bb = password.getBytes();
		Assert.isTrue(bb.length > 23, "the secretKey for 3DES must be 24 bytes at least.");
		try {
			KeySpec keySpec = new DESedeKeySpec(bb);
			SecretKey key = SecretKeyFactory.getInstance("DESede")
					.generateSecret(keySpec);
			return key;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 根据输入的数据，给出对应的密钥对象(RAW KEY)
	 * 
	 * @param value
	 * @param algom
	 * @return
	 */
	public static final SecretKey toSecretKey(byte[] value, String algorithm) {
		if (algorithm.indexOf("/") > -1) {
			// 某些时候指定的算法不能直接作为key的算法。
			// 比如Chiper算法指定为DES/ECB/NOPADDING时，key的算法必须是DES，否则会报错。
			return new RawSecretKeySpec(value, algorithm,
					StringUtils.substringBefore(algorithm, "/"));
		} else {
			return new SecretKeySpec(value, algorithm);
		}
	}

	// TODO 自动填充空格，防止key太短的问题
	static class RawSecretKeySpec extends SecretKeySpec {
		String chiperAlgomrithm;

		public RawSecretKeySpec(byte[] abyte0, String s) {
			super(abyte0, s);
		}

		public RawSecretKeySpec(byte[] abyte0, int i, int j, String s) {
			super(abyte0, i, j, s);
		}

		public RawSecretKeySpec(byte[] abyte0, String algorithm, String keyAlgom) {
			super(abyte0, keyAlgom);
			this.chiperAlgomrithm = algorithm;
		}

		private static final long serialVersionUID = 3865527433731129466L;
	}

	/**
	 * 生成指定算法的KEY
	 * 
	 * @param value
	 * @param algom
	 *            对称加密算法
	 * @return
	 */
	public static final SecretKey generateKey(String algom,int keylength) {
		try {
			KeyGenerator keygen = KeyGenerator.getInstance(algom);
			keygen.init(keylength);
			SecretKey deskey = keygen.generateKey();
			return deskey;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 生成指定算法的KEY对
	 * 
	 * @param algom
	 *            非对称加密算法 DSA RSA等
	 * @return
	 */
	public static final KeyPair generateKeyPair(String algom) {
		try {
			java.security.KeyPairGenerator keygen = java.security.KeyPairGenerator
					.getInstance(algom);
			SecureRandom secrand = new SecureRandom();
			secrand.setSeed("\n".getBytes()); // 初始化随机产生器
			// 密钥长度：其范围必须在 512 到 1024 之间，且必须为 64 的倍数
			keygen.initialize(1024, secrand); // 初始化密钥生成器
			// keygen.initialize(512);
			KeyPair keys = keygen.generateKeyPair(); // 生成密钥组
			return keys;
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 得到基于DSA算法的数字签名
	 * 
	 * @param in
	 * @return
	 * @throws InvalidKeyException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws SignatureException
	 */
	public static byte[] getDSASign(InputStream in, PrivateKey key) {
		try {
			java.security.Signature signet = java.security.Signature
					.getInstance("DSA");
			signet.initSign(key);
			byte[] b = new byte[1024];
			int len;
			while ((len = in.read(b)) != -1) {
				signet.update(b, 0, len);
			}
			byte[] signed = signet.sign();
			return signed;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 得到基于DSA算法的数字签名
	 * 
	 * @param in
	 * @return
	 */
	public static byte[] getDSASign(Reader in, PrivateKey key) {
		return getDSASign(new ReaderInputStream(in), key);
	}

	/**
	 * 得到基于DSA算法的数字签名
	 * 
	 * @param in
	 * @return
	 */
	public static byte[] getDSASign(String in, PrivateKey key) {
		return getDSASign(new StringReader(in), key);
	}

	/**
	 * 校验数据和签名是否一致
	 * 
	 * @param in
	 * @param key
	 * @param sign
	 * @return
	 */
	public static boolean verifyDSASign(InputStream in, byte[] sign,
			PublicKey key) {
		Signature signetcheck;
		try {
			signetcheck = java.security.Signature.getInstance("DSA");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		try {
			signetcheck.initVerify(key);
			byte[] b = new byte[1024];
			int len;
			while ((len = in.read(b)) != -1) {
				signetcheck.update(b, 0, len);
			}
			return signetcheck.verify(sign);
		} catch (InvalidKeyException e) {
			throw new RuntimeException(e);
		} catch (SignatureException e) {
			return false;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 校验数据和签名是否一致
	 * 
	 * @param in
	 * @param key
	 * @param sign
	 * @return
	 */
	public static boolean verifyDSASign(Reader in, byte[] sign, PublicKey key) {
		return verifyDSASign(new ReaderInputStream(in), sign, key);
	}

	/**
	 * 校验数据和签名是否一致
	 * 
	 * @param in
	 * @param key
	 * @param sign
	 * @return
	 */
	public static boolean verifyDSASign(String in, byte[] sign, PublicKey key) {
		return verifyDSASign(new StringReader(in), sign, key);
	}

	/**
	 * 将KEY保存为文件
	 * 
	 * @param key
	 * @param file
	 *            指定要保持的文件
	 * @return File,实际保存的文件，不会覆盖已有的文件，会自动改名。
	 */
	public static File saveKey(SecretKey key, File file) {
		try {
			File f = IOUtils.escapeExistFile(file);
			IOUtils.saveAsFile(f, false, key.getEncoded());
			return f;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 载入x509的密钥
	 * 
	 * @param f
	 * @param algom
	 *            算法，可用 getSupportedAlgorithmName (AlgorithmType.KeyFactory)查询
	 * @param isPublic
	 *            true生成公钥对象，false生成私钥对象
	 * @return
	 */
	public static Key loadX509Key(File f, String algom, boolean isPublic) {
		try {
			byte[] keyData = IOUtils.toByteArray(f);
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
			KeyFactory keyFactory = KeyFactory.getInstance(algom);
			Key result = (isPublic) ? keyFactory.generatePublic(keySpec)
					: keyFactory.generatePrivate(keySpec);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 载入PKCS8的密钥
	 * 
	 * @param f
	 * @param algom
	 *            算法
	 * @param isPublic
	 *            true生成公钥对象，false生成私钥对象
	 * @return
	 */
	public static Key loadPKCS8Key(File f, String algom, boolean isPublic) {
		try {
			byte[] keyData = IOUtils.toByteArray(f);
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
			KeyFactory keyFactory = KeyFactory.getInstance(algom);
			Key result = (isPublic) ? keyFactory.generatePublic(keySpec)
					: keyFactory.generatePrivate(keySpec);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 使用指定的KEY来解密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法.对称非对称均可，非对称要注意使用公钥来解密
	 * @return AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
	 *         iterationCount);
	 */
	public static byte[] decrypt(InputStream in, Key key,
			AlgorithmParameterSpec spec) {
		Assert.notNull(key, "SecretKey Key must not null");
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			Cipher c1 = Cipher
					.getInstance((key instanceof RawSecretKeySpec) ? ((RawSecretKeySpec) key).chiperAlgomrithm
							: key.getAlgorithm());
			c1.init(Cipher.DECRYPT_MODE, key, spec);
			byte[] b = new byte[1024];
			int len;
			while ((len = in.read(b)) != -1) {
				out.write(c1.update(b, 0, len));
			}
			out.write(c1.doFinal());
			return out.toByteArray();
		} catch (GeneralSecurityException e) {
			LogUtil.exception(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 使用指定的KEY来解密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法.对称非对称均可，非对称要注意使用公钥来解密
	 * @return AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
	 *         iterationCount);
	 */
	public static byte[] decrypt(InputStream in, SecretKey key) {
		return decrypt(in, key, null);
	}

	/**
	 * 使用指定的KEY来解密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法.对称非对称均可，非对称要注意使用公钥来解密
	 * @return AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
	 *         iterationCount);
	 */
	public static byte[] decrypt(byte[] in, SecretKey key) {
		return decrypt(new ByteArrayInputStream(in), key);
	}
	
	public static byte[] decrypt(byte[] in, PrivateKey key) {
		return decrypt(new ByteArrayInputStream(in), key,null);
	}
	

	/**
	 * 使用指定的KEY来解密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法.对称非对称均可，非对称要注意使用公钥来解密
	 * @return AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
	 *         iterationCount);
	 */
	public static byte[] decrypt(Reader in, SecretKey key) {
		return decrypt(new ReaderInputStream(in), key);
	}

	/**
	 * 使用指定的KEY来解密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法.对称非对称均可，非对称要注意使用公钥来解密
	 * @return AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
	 *         iterationCount);
	 */
	public static byte[] decrypt(String in, SecretKey key) {
		return decrypt(new StringReader(in), key);
	}

	/**
	 * 获得基于密码的加密器实例
	 * 
	 * @return
	 */
	public static PasswordEncryptor getDefaultPBE() {
		return new PasswordEncryptor(new byte[] { (byte) 0xA9, (byte) 0x9B,
				(byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35,
				(byte) 0xE3, (byte) 0x03 }, 19);
	}

	/**
	 * 获得基于密码的加密器实例
	 * 
	 * @param salt
	 *            盐
	 * @param iterationCount
	 *            迭代次数
	 * @return
	 */
	public static PasswordEncryptor getPBE(byte[] salt, int iterationCount) {
		return new PasswordEncryptor(salt, iterationCount);
	}

	/**
	 * 使用指定的KEY来加密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法,对称或非对称均可，非对称要注意使用私钥加密 支持的算法名称如下：DES DESede(TripleDES)
	 *            DESedeWrap PBEWithMD5AndDES(OID.1.2.840.113549.1.5.3 或
	 *            1.2.840.113549.1.5.3) PBEWithMD5AndTripleDES
	 *            PBEWithSHA1AndRC2_40(OID.1.2.840.113549.1.12.1.6 或
	 *            1.2.840.113549.1.12.1.6)
	 *            PBEWithSHA1AndDESede(OID.1.2.840.113549
	 *            .1.12.1.3,1.2.840.113549.1.12.1.3) Blowfish AES(Rijndael)
	 *            AESWrap RC2 ARCFOUR(RC4) RSA RSA/ECB/PKCS1Padding RSA 共计15种
	 * @return
	 */
	public static byte[] encrypt(InputStream in, Key key,
			AlgorithmParameterSpec spec,boolean padding) {
		Assert.notNull(key, "SecretKey Key must not null");
		String alg=(key instanceof RawSecretKeySpec) ? ((RawSecretKeySpec) key).chiperAlgomrithm
				: key.getAlgorithm();
		if(padding && alg.indexOf('/')==-1){
			alg=alg+"/ECB/PKCS1Padding";
		}
		try {
			Cipher c1 = Cipher.getInstance(alg);
			c1.init(Cipher.ENCRYPT_MODE, key, spec);
			ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
			byte[] b = new byte[1024];
			int len;
			while ((len = in.read(b)) != -1) {
				out.write(c1.update(b, 0, len));
			}
			out.write(c1.doFinal());
			return out.toByteArray();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final IvParameterSpec DEFAULT_IvParameterSpec = new IvParameterSpec(
			"12345678".getBytes());

	/**
	 * 使用指定的KEY来加密
	 * 
	 * @param schemaIndex
	 * @param key
	 *            密钥，包含算法,对称或非对称均可，非对称要注意使用私钥加密
	 * @return
	 */
	public static byte[] encrypt(InputStream in, SecretKey key) {
		return encrypt(in, key,null,false);
	}

	/**
	 * 使用指定的KEY来加密
	 * 
	 * @param data
	 * @param key
	 *            密钥，包含算法,对称或非对称均可，非对称要注意使用私钥加密
	 * @return
	 */
	public static byte[] encrypt(byte[] data, SecretKey key) {
		return encrypt(new ByteArrayInputStream(data), key);
	}

	public static byte[] encrypt(byte[] data, PublicKey key) {
		return encrypt(new ByteArrayInputStream(data), key,null,true);
	}
	
	/**
	 * 使用指定的KEY来加密
	 * 
	 * @param data
	 * @param key
	 *            密钥，包含算法,对称或非对称均可，非对称要注意使用私钥加密
	 * @return
	 */
	public static byte[] encrypt(Reader data, SecretKey key,AlgorithmParameterSpec... param) {
		if(param!=null && param.length>0){
			return encrypt(new ReaderInputStream(data), key,param[0],false);
		}else{
			return encrypt(new ReaderInputStream(data), key,null,false);	
		}
	}

	/**
	 * 使用指定的KEY来加密
	 * 
	 * @param data
	 * @param key
	 *            密钥，包含算法,对称或非对称均可，非对称要注意使用私钥加密
	 * @return
	 */
	public static byte[] encrypt(String data, SecretKey key,AlgorithmParameterSpec... param) {
		return encrypt(new StringReader(data), key,param);
	}

	public interface Transport{
		void send(byte[] encoded) throws IOException;
	}

	/**
	 * 将byte[]包装成InputStream对象
	 * 
	 * @param data
	 * @return
	 */
	public static InputStream wrap(byte[] data) {
		return new ByteArrayInputStream(data);
	}

	/**
	 * 将String包装成InputStream对象
	 * 
	 * @param data
	 * @param code
	 * @return
	 */
	public static InputStream wrap(String data, String code) {
		try {
			return new ByteArrayInputStream(data.getBytes(code));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 字节数据编码为可见字符(base64)
	 * 
	 * @param in
	 * @return
	 */
	public static String base64Encode(byte[] in) {
		return JefBase64.encode(in);
	}

	/**
	 * 字节数据编码为可见字符(base64)
	 * 
	 * @param in
	 * @return
	 */
	public static String base64Encode(InputStream in) {
		try {
			return JefBase64.encode(IOUtils.toByteArray(in));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 可见字符（base64）解码为字节组（base64）
	 * 
	 * @param in
	 * @return
	 */
	public static byte[] base64Decode(CharSequence in) {
		return JefBase64.decodeFast(in);
	}


	/**
	 * 字节组编码为可见字符（base64）
	 * 
	 * @param data
	 * @return
	 */
	public static byte[] base64Decode(byte[] data) {
		return JefBase64.decodeFast(data,0,data.length);
	}

	private EncrypterUtil() {
	};
	
	static {
		Provider p = (Provider) BeanUtils
				.newInstance("com.sun.crypto.provider.SunJCE");
		if (p == null) {
			LogUtil.show("Current JDK is not sun JDK compatible...");
		} else {
			Security.addProvider(p);
		}
		if(p==null){
			p = (Provider) BeanUtils
					.newInstance("com.ibm.crypto.provider.IBMJCE");
			if (p == null) {
				LogUtil.show("Current JDK is not IBM JDK compatible...");
			} else {
				Security.addProvider(p);
			}	
		}
	}

	/**
	 * 3des密码加密解密程序的DES实现。(允许使用8位密码)
	 * 3DES:是在DES的基础上采用三重DES,即用两个56位的密钥K1,K2,发送方用K1加密
	 * ,K2解密,再使用K1加密.接收方使用K1解密,K2加密,再使用K1解密, 3DES实现： 主要有CBC,ECB实现，java默认是ECB
	 * 对于待加密解密的数据的填充方式：NoPadding、PKCS5Padding、SSL3Padding，默认填充方式为，PKCS5Padding
	 * java中要求key的size必须为24；对于CBC模式下的向量iv的size两者均要求必须为8,
	 * 所以在处理8字节的key的时候，直接使用DES三次， 加密时候为（加密－－解密－－加密），解密时候为：（解密－－加密－－解密）
	 */
	public static class DESede {
		public static byte[] encrypt(byte[] msg, byte[] pass) throws Exception {
			byte[] input = msg;

			byte[] keyBytes = pass;

			SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
			Cipher cipher = Cipher.getInstance("DES/ECB/NOPADDING"); // TripleDES/ECB/NoPadding

			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
			int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
			ctLength += cipher.doFinal(cipherText, ctLength);

			return cipherText;

		}

		public static byte[] decrypt(byte[] s, byte[] k) throws Exception {

			byte[] input = s;

			byte[] keyBytes = k;
			SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
			Cipher cipher = Cipher.getInstance("DES/ECB/NOPADDING");

			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
			int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
			ctLength += cipher.doFinal(cipherText, ctLength);

			return cipherText;

		}

	}

	public static String decryptString(byte[] data, SecretKey key,
			String charset) {
		try {
			if (charset == null) {
				return new String(decrypt(data, key));
			} else {
				return new String(decrypt(data, key), charset);
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}
	
	/**
	 * 知道这是在干什么用的就用，不知道的就别用了。
	 * @return
	 */
	public static boolean removeCryptographyRestrictions() {
		if (!isRestrictedCryptography()) {
			return false;
		}
		try {
			/*
			 * Do the following, but with reflection to bypass access checks:
			 * 
			 * JceSecurity.isRestricted = false;
			 * JceSecurity.defaultPolicy.perms.clear();
			 * JceSecurity.defaultPolicy.add(CryptoAllPermission.INSTANCE);
			 */
			final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
			final Class<?> cryptoPermissions = Class.forName("javax.crypto.CryptoPermissions");
			final Class<?> cryptoAllPermission = Class.forName("javax.crypto.CryptoAllPermission");

			final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
			isRestrictedField.setAccessible(true);
			final Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(isRestrictedField, isRestrictedField.getModifiers() & ~Modifier.FINAL);
			isRestrictedField.set(null, false);

			final Field defaultPolicyField = jceSecurity.getDeclaredField("defaultPolicy");
			defaultPolicyField.setAccessible(true);
			final PermissionCollection defaultPolicy = (PermissionCollection) defaultPolicyField.get(null);

			final Field perms = cryptoPermissions.getDeclaredField("perms");
			perms.setAccessible(true);
			((Map<?, ?>) perms.get(defaultPolicy)).clear();

			final Field instance = cryptoAllPermission.getDeclaredField("INSTANCE");
			instance.setAccessible(true);
			defaultPolicy.add((Permission) instance.get(null));
			return true;
		} catch (final Exception e) {
			LogUtil.error("Failed to remove cryptography restrictions", e);
			return false;
		}
	}

	
	private static boolean isRestrictedCryptography() {
		return "Java(TM) SE Runtime Environment".equals(System.getProperty("java.runtime.name"));
	}
}
