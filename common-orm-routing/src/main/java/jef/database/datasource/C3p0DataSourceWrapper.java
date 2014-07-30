package jef.database.datasource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import jef.common.log.LogUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3p0DataSourceWrapper extends AbstractDataSource implements DataSourceWrapper{
	private ComboPooledDataSource datasource;
	
	public C3p0DataSourceWrapper() {
		datasource=new ComboPooledDataSource();
	}

	public String getUrl() {
		return datasource.getJdbcUrl();
	}

	public String getUser() {
		return datasource.getUser();
	}

	public String getPassword() {
		return datasource.getPassword();
	}

	public String getDriverClass() {
		return datasource.getDriverClass();
	}

	public void setUrl(String url) {
		datasource.setJdbcUrl(url);
	}

	public void setUser(String user) {
		datasource.setUser(user);
	}

	public void setPassword(String password) {
		datasource.setPassword(password);
	}

	public void setDriverClass(String driverClassName) {
		try {
			datasource.setDriverClass(driverClassName);
		} catch (PropertyVetoException e) {
			LogUtil.exception(e);
		}
	}

	public Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return datasource.getConnection(username,password);
	}

	public boolean isConnectionPool() {
		return true;
	}

	public void setWrappedDataSource(DataSource ds) {
		this.datasource=(ComboPooledDataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return ComboPooledDataSource.class;
	}

	public Properties getProperties() {
		return datasource.getProperties();
	}

	public void putProperty(String key, Object value) {
		datasource.getProperties().put(key, value);
	}

}
