package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

public class DbcpDataSourceWrapper extends AbstractDataSource implements DataSourceWrapper  {
	private org.apache.commons.dbcp.BasicDataSource datasource;
	public DbcpDataSourceWrapper(){
		datasource=new BasicDataSource();
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

	public Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return datasource.getConnection();
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

	public boolean isConnectionPool() {
		return true;
	}

	public void setWrappedDataSource(DataSource ds) {
		this.datasource=(BasicDataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return BasicDataSource.class;
	}

	public Properties getProperties() {
		return new Properties();
	}

	public void putProperty(String key, Object value) {
		datasource.addConnectionProperty(key, String.valueOf(value));
	}

}
