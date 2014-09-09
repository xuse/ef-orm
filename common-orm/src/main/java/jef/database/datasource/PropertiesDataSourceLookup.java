package jef.database.datasource;

import java.util.Collection;

import javax.sql.DataSource;

/**
 * 通过System.properties或者本地的properties文件来提供DataSource
 * @author jiyi
 *
 */
public class PropertiesDataSourceLookup implements DataSourceLookup{
	private PropertiesDataSourceInfoLookup inner=new PropertiesDataSourceInfoLookup();
	private String defaultKey;
	
	public DataSource getDataSource(String dataSourceName) {
		DataSourceInfo dsi=inner.getDataSourceInfo(dataSourceName);
		return DataSources.getAsDataSource(dsi);
	}

	/**
	 * 数据源配置在Properties文件中的前缀，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfPrefix就是 'jdbc.'
	 * @param keyOfPrefix default value is empty
	 */
	public void setKeyOfPrefix(String keyOfPrefix) {
		inner.setKeyOfPrefix(keyOfPrefix);
	}

	/**
	 * 数据源配置在Properties文件中的URL的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfUrl就是 '.url'
	 * @param keyOfPrefix default valuie is '.url'
	 */
	public void setKeyOfUrl(String keyOfUrl) {
		inner.setKeyOfUrl(keyOfUrl);
	}
	/**
	 * 数据源配置在Properties文件中的username的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfUser就是 '.user'
	 * @param keyOfUser default valuie is '.user'
	 */
	public void setKeyOfUser(String keyOfUser) {
		inner.setKeyOfUser(keyOfUser);
	}
	/**
	 * 数据源配置在Properties文件中的username的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfPassword就是 '.password'
	 * @param keyOfPassword default valuie is '.password'
	 */
	public void setKeyOfPassword(String keyOfPassword) {
		inner.setKeyOfPassword(keyOfPassword);
	}
	/**
	 * 数据源配置在Properties文件中的driverClass，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * jdbc.ds1.driver=org.gjt.mm.mysql.Driver
	 * </pre>
	 * 在上例中KeyOfDriver就是 '.driver'
	 * @param keyOfDriver default valuie is '.driver'
	 */
	public void setKeyOfDriver(String keyOfDriver) {
		inner.setKeyOfDriver(keyOfDriver);
	}
	
	/**
	 * 设置properties文件路径。不设置的话只会使用System.getProperty();
	 * 路径不能以/开头，jef会查找所有classpath下的该名称的资源文件来查找数据库配置
	 * @param localtion
	 */
	public void setLocation(String localtion) {
		inner.setLocation(localtion);
	}

	/**
	 * 如果没有配置user或者password的数据源就忽略
	 * @param needLogonInfo
	 */
	public void setNeedLogonInfo(boolean needLogonInfo) {
		inner.setNeedLogonInfo(needLogonInfo);
	}
	
	/**
	 * 设置数据库密码解密回调类。很多时候，我们配置的数据库密码都是加密后的，这种场合下我们可以实现PasswordDecryptor接口，
	 * 并将其设置到DataSourceLookup中，每当发现新的数据源，就可以对其中的用户口令解密.
	 * @param passwordDecryptor
	 */
	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		inner.setPasswordDecryptor(passwordDecryptor);
	}

	public String getDefaultKey() {
		return defaultKey;
	}

	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}
	
	public Collection<String> getAvailableKeys() {
		return inner.getAvailableKeys();
	}
}
