package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidAbstractDataSource;
import com.alibaba.druid.pool.DruidDataSource;

public class DruidDataSourceWrapper extends AbstractDataSource implements DataSourceWrapper{
	private DruidAbstractDataSource datasource;

	public DruidDataSourceWrapper(){
		datasource=new DruidDataSource();
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
		return datasource.getConnection(username, password);
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
		this.datasource=(DruidAbstractDataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return DruidAbstractDataSource.class;
	}

	public Properties getProperties() {
		return datasource.getConnectProperties();
	}

	public void putProperty(String key, Object value) {
		Properties pp=datasource.getConnectProperties();
		pp.put(key, value);
		datasource.setConnectProperties(pp);
	}
}
