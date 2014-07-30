package jef.database.datasource;

import jef.database.DbUtils;

/**
 * 数据库密码解密的回调接口。用户可以自行实现数据库密码解密的设置
 * @author jiyi
 *
 */
public interface PasswordDecryptor  {
	static final PasswordDecryptor DUMMY=new PasswordDecryptor(){
		public String decrypt(String raw) {
			return raw;
		}
	};
	static final PasswordDecryptor DEFAULT=new PasswordDecryptor(){
		public String decrypt(String raw) {
			return DbUtils.decrypt(raw);
		}
	};
	
	/**
	 * 将输入的密文解密后返回。
	 * 对于非加密的场景，返回的明文就是密文
	 * @param raw 密文
	 * @return    明文
	 */
	String decrypt(String raw);
}


