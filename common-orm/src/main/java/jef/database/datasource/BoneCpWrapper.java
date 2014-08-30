package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

public class BoneCpWrapper extends AbstractDataSource implements DataSourceWrapper{
	BoneCPDataSource datasource;

	public String getUrl() {
		return datasource.getJdbcUrl();
	}

	public String getUser() {
		return datasource.getUsername();
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
		datasource.setUsername(user);
	}

	public void setPassword(String password) {
		datasource.setPassword(password);		
	}

	public void setDriverClass(String driverClassName) {
		datasource.setDriverClass(driverClassName);
	}
	
	//TODO 和TomcatPool一样，是基于各个属性的直接getter和setter的。因此无法直接获得Properties对象
	public Properties getProperties() {
		return new ReflectionProperties(BoneCPConfig.class, datasource);
	}

	public void putProperty(String key, Object value) {
		new ReflectionProperties(BoneCPConfig.class, datasource).put(key, value);
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
		datasource=(BoneCPDataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return BoneCPDataSource.class;
	}
	
	
}
