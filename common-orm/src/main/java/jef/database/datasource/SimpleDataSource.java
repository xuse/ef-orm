package jef.database.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import jef.common.log.LogUtil;
import jef.database.dialect.DatabaseDialect;
import jef.database.dialect.DbmsProfile;
import jef.tools.StringUtils;

/**
 * 只有SimpleDataSource才支持带properties参数的getConnection()方法。从而获得Oracle的列注释
 * 
 * @author Administrator
 * 
 */
public final class SimpleDataSource extends AbstractDataSource implements DataSourceWrapper {
	private String url;
	private String username;
	private String password;
	private String dbKey;
	private String racId;
	private String driverClass;
	private final java.util.Properties prop = new java.util.Properties();

	@Override
	public String toString() {
		return StringUtils.concat(url, ":", username);
	}

	public SimpleDataSource() {
	};

	public SimpleDataSource(String url,String user,String password) {
		this.url=url;
		this.username=user;
		this.password=password;
	}
	public SimpleDataSource(DataSourceInfo info) {
		this.url=info.getUrl();
		this.driverClass=info.getDriverClass();
		this.username=info.getUser();
		this.password=info.getPassword();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String user) {
		this.username = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDbKey() {
		return dbKey;
	}

	public void setDbKey(String dbKey) {
		this.dbKey = dbKey;
	}

	public String getRacId() {
		return racId;
	}

	public void setRacId(String racId) {
		this.racId = racId;
	}

	public Connection getConnection() throws SQLException {
		return getConnection(username, password);
	}

	public Connection getConnection(String username, String password) throws SQLException {
		initDriver();
		java.util.Properties info=(Properties) this.prop.clone();
		if (username != null) {
			info.put("user", username);
		}
		if (password != null) {
			info.put("password", password);
		}
		try{
			return DriverManager.getConnection(url, info);
		}catch(SQLException e){
			LogUtil.error("JDBC URL ERROR:[{}]",this.url);
			throw e;
		}
	}

	public Connection getConnectionFromDriver(Properties props) throws SQLException {
		initDriver();
		if (username != null && !props.containsKey("user"))
			props.put("user", username);
		if (password != null && !props.containsKey("password"))
			props.put("password", password);
		return DriverManager.getConnection(url, props);
	}

	private void initDriver() {
		if(driverClass==null || driverClass.length()==0){
			if (url.startsWith("jdbc:")) {
				int m=url.indexOf(':',5);
				String dbName = url.substring(5, m).trim();
				DatabaseDialect profile = DbmsProfile.getProfile(dbName);
				if (profile == null) {
					throw new IllegalArgumentException("the db type " + dbName + " not supported!");
				}
				driverClass=profile.getDriverClass(this.url);
			}
		}
		//注册驱动
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			LogUtil.exception(e);
		}
	}

	public String getUser() {
		return username;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public void setUser(String user) {
		setUsername(user);
	}

	public boolean isConnectionPool() {
		return false;
	}

	@Override
	protected Class<? extends AbstractDataSource> getWrappedClass() {
		return null;
	}

	public void setWrappedDataSource(DataSource ds) {//do nothing...
	}

	public void putProperty(String key, Object value) {
		this.prop.put(key, value);
	}

	public Properties getProperties() {
		return prop;
	}
}
