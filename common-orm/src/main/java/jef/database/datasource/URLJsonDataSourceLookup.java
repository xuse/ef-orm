package jef.database.datasource;

import java.util.Collection;

import javax.sql.DataSource;

/**
 * 使用URL来访问json资源得到数据源配置的实现
 * @author jiyi
 *
 */
public class URLJsonDataSourceLookup implements DataSourceLookup {
	URLJsonDataSourceInfoLookup inner=new URLJsonDataSourceInfoLookup();
	public DataSource getDataSource(String dataSourceName) {
		DataSourceInfo info=inner.getDataSourceInfo(dataSourceName);
		return DataSources.getAsDataSource(info);
	}

	/**
	 * 数据源配置Json中的唯一标识字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，dataSourceKeyFieldName就是 'id'
	 * @param dataSourceKeyFieldName default valuie is 'url'
	 */
	public void setDataSourceKeyFieldName(String dataSourceKeyFieldName) {
		inner.setDataSourceKeyFieldName(dataSourceKeyFieldName);
	}

	/**
	 * 数据源配置Json中的JDBC URL字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，urlFieldName就是 'url'
	 * @param urlFieldName default valuie is 'url'
	 */
	public void setUrlFieldName(String urlFieldName) {
		inner.setUrlFieldName(urlFieldName);
	}

	/**
	 * 数据源配置Json中的用户名字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，userFieldName就是 'user'
	 * @param userFieldName default valuie is 'user'
	 */
	public void setUserFieldName(String userFieldName) {
		inner.setUserFieldName(userFieldName);
	}

	/**
	 * 数据源配置Json中的用口令字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，passwordFieldName就是 'password'
	 * @param passwordFieldName default valuie is 'password'
	 */
	public void setPasswordFieldName(String passwordFieldName) {
		inner.setPasswordFieldName(passwordFieldName);
	}
	/**
	 * 数据源配置Json中的用驱动类字段，举例<pre>
	 * [{id:"ds1", url:"jdbc:mysql://localhost:3306/test", user: "root", password:"123456",
	 *    driverClassName:"org.gjt.mm.mysql.Driver"}]
	 * </pre>
	 * 在上面这段json中，driverFieldName就是 'driverClassName'
	 * @param driverFieldName default valuie is 'driverClassName'
	 */
	public void setDriverFieldName(String driverFieldName) {
		inner.setDriverFieldName(driverFieldName);
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
		return inner.getDefaultKey();
	}

	public void setDefaultKey(String defaultKey) {
		inner.setDefaultKey(defaultKey);
	}
	
	public Collection<String> getAvailableKeys() {
		return inner.getAvailableKeys();
	}
	
	public void setIgnoreCase(boolean ignoreCase) {
		inner.setIgnoreCase(ignoreCase);
	}
	
	public boolean getIgnoreCase(){
		return inner.ignoreCase;
	}
	
	public String getLocation() {
		return inner.getLocation();
	}

	public void setLocation(String localtion) {
		inner.setLocation(localtion);
	}

}
