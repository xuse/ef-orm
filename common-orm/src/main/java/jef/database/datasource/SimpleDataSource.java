package jef.database.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;

import jef.common.log.LogUtil;
import jef.database.dialect.AbstractDialect;
import jef.database.dialect.DatabaseDialect;
import jef.tools.StringUtils;

/**
 * EF-ORM中简单的Datasource实现。
 * 只有SimpleDataSource才支持带properties参数的getConnection()方法。从而获得Oracle的列注释
 * 
 * @author Administrator
 * 
 */
public final class SimpleDataSource extends AbstractDriverBasedDataSource implements DataSourceWrapper {
	private String dbKey;
	private String racId;
	private String driverClass;

	
//	private String url;
//
//	private String username;
//
//	private String password;
//
//	private String catalog;
//
//	private String schema;
//
//	private Properties connectionProperties;
	
	@Override
	public String toString() {
		return StringUtils.concat(getUrl(), ":", getUsername());
	}

	public SimpleDataSource() {
	};

	public SimpleDataSource(String url,String user,String password) {
		setUrl(url);
		setUsername(user);
		setPassword(password);
	}
	
	
	public SimpleDataSource(AbstractDriverBasedDataSource ds) {
		setUrl(ds.getUrl());
		setUsername(ds.getUsername());
		setPassword(ds.getPassword());
		setSchema(ds.getSchema());
		setCatalog(ds.getCatalog());
		setConnectionProperties(ds.getConnectionProperties());
//		setLogWriter(ds.getLogWriter());
//		setLoginTimeout(ds.getLoginTimeout());
	}
	
	
	public SimpleDataSource(DataSourceInfo info) {
		setUrl(info.getUrl());
		setUsername(info.getUser());
		setPassword(info.getPassword());
		this.driverClass=info.getDriverClass();
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

	public Connection getConnection(String username, String password) throws SQLException {
		initDriver();
		return super.getConnectionFromDriver(username, password);
	}

	public Connection getConnectionFromDriver(Properties props) throws SQLException {
		initDriver();
		if (getUser() != null && !props.containsKey("user"))
			props.put("user", getUser());
		if (getPassword()!= null && !props.containsKey("password"))
			props.put("password", getPassword());
		Connection conn= DriverManager.getConnection(getUrl(), props);
		if (getCatalog() != null) {
			conn.setCatalog(getCatalog());
		}
		if (getSchema() != null) {
			conn.setSchema(getSchema());
		}
		return conn;
	}

	private void initDriver() {
		if(driverClass==null || driverClass.length()==0){
			String url=getUrl();
			if (url.startsWith("jdbc:")) {
				int m=url.indexOf(':',5);
				String dbName = url.substring(5, m).trim();
				DatabaseDialect profile = AbstractDialect.getDialect(dbName);
				if (profile == null) {
					throw new IllegalArgumentException("the db type " + dbName + " not supported!");
				}
				driverClass=profile.getDriverClass(url);
			}
		}
		//注册驱动
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			LogUtil.exception(e);
		}
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

	public void setWrappedDataSource(DataSource ds) {//do nothing...
	}

	public void putProperty(String key, Object value) {
		Properties props=getConnectionProperties();
		if(props==null) {
			props=new Properties();
			setConnectionProperties(props);
		}
		props.put(key, value);
	}

	public Properties getProperties() {
		return getConnectionProperties();
	}

	@Override
	public String getUser() {
		return getUsername();
	}
}
