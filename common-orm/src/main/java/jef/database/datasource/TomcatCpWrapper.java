package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;


public class TomcatCpWrapper  extends AbstractDataSource implements DataSourceWrapper{
	org.apache.tomcat.jdbc.pool.DataSource datasource;

	public TomcatCpWrapper() {
		datasource = new org.apache.tomcat.jdbc.pool.DataSource();
	}

	public String getUrl() {
		return datasource.getUrl();
	}

	public String getUser() {
		return datasource.getUsername();
	}

	public String getPassword() {
		return datasource.getPassword();
	}

	public String getDriverClass() {
		return datasource.getDriverClassName();
	}

	public void setUrl(String url) {
		datasource.setUrl(url);
	}

	public void setUser(String user) {
		datasource.setUsername(user);
	}

	public void setPassword(String password) {
		datasource.setPassword(password);
	}

	public void setDriverClass(String driverClassName) {
		datasource.setDriverClassName(driverClassName);
	}

	public Properties getProperties() {
		return new ReflectionProperties(PoolConfiguration.class, datasource.getPoolProperties());
	}

	public void putProperty(String key, Object value) {
		new ReflectionProperties(PoolConfiguration.class, datasource.getPoolProperties()).put(key, value);
	}

	public Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return datasource.getConnection(username, password);
	}

	public boolean isConnectionPool() {
		return true;
	}

	public void setWrappedDataSource(DataSource ds) {
		datasource=(org.apache.tomcat.jdbc.pool.DataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return org.apache.tomcat.jdbc.pool.DataSource.class;
	}
}
