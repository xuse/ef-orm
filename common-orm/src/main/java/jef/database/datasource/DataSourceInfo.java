package jef.database.datasource;

import java.util.Properties;


/**
 * 描述一个数据源的配置信息
 * @author jiyi
 *
 */
public interface DataSourceInfo {
	
	/**
	 * 返回数据库连接的JDBC url
	 * @return
	 */
	public String getUrl();

	/**
	 * 返回数据库连接的用户名
	 * @return
	 */
	public String getUser();

	/**
	 * 返回数据库连接的密码
	 * @return
	 */
	public String getPassword();
	
	/**
	 * 返回数据库连接的驱动类的全名
	 * @return
	 */
	public String getDriverClass();
	
	/**
	 * 设置数据源连接JDBC url
	 * @param url
	 */
	void setUrl(String url);

	/**
	 * 设置数据源连接的用户名
	 * @param user
	 */
	void setUser(String user);

	/**
	 * 设置数据源连接的密码
	 * @param password
	 */
	void setPassword(String password);
	
	/**
	 * 设置数据源连接的驱动类名称
	 * @param driverClassName
	 */
	void setDriverClass(String driverClassName);
	
	/**
	 * 得到连接属性配置
	 * @return
	 */
	Properties getProperties();
	
	/**
	 * 在连接属性配置中添加
	 * @param key
	 * @param value
	 */
	void putProperty(String key,Object value);
}
