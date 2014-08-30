package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import jef.common.log.LogUtil;

import org.logicalcobwebs.proxool.ProxoolDataSource;

public class ProxoolDataSourceWrapper extends AbstractDataSource implements DataSourceWrapper{
	private ProxoolDataSource datasource;

	public ProxoolDataSourceWrapper(){
		datasource=new ProxoolDataSource();
	}
	
	public String getUrl() {
		return datasource.getDriverUrl();
	}

	public String getUser() {
		return datasource.getUser();
	}

	public String getPassword() {
		return datasource.getPassword();
	}

	public String getDriverClass() {
		return datasource.getDriver();
	}

	public void setUrl(String url) {
		datasource.setDriverUrl(url);
	}

	public void setUser(String user) {
		datasource.setUser(user);
	}

	public void setPassword(String password) {
		datasource.setPassword(password);
	}

	public void setDriverClass(String driverClassName) {
		datasource.setDriver(driverClassName);
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
		this.datasource=(ProxoolDataSource) ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return ProxoolDataSource.class;
	}

	public Properties getProperties() {
		return new Properties();
	}

	public void putProperty(String key, Object value) {
		LogUtil.warn("Can not set the properties for ProxoolDataSource." );
	}

}
