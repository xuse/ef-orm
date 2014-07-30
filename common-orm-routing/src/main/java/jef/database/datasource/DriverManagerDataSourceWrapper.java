package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * 这是为Spring的AbstractDriverBasedDataSource(含DriverManagerDatasource)所写的包装类，可以将其包装为DataSourceInfo
 * @author jiyi
 *
 */
public class DriverManagerDataSourceWrapper extends AbstractDataSource implements DataSource,DataSourceWrapper{
	private AbstractDriverBasedDataSource datasource;

	public DriverManagerDataSourceWrapper(){
		datasource=new DriverManagerDataSource();
	}
	
	public DriverManagerDataSourceWrapper(AbstractDriverBasedDataSource datasource){
		this.datasource=datasource;
	}
	
	public String getUrl() {
		assertDataSource();
		return datasource.getUrl();
	}

	public String getUser() {
		assertDataSource();
		return datasource.getUsername();
	}

	public String getPassword() {
		assertDataSource();
		return datasource.getPassword();
	}

	public Connection getConnection() throws SQLException {
		assertDataSource();
		return datasource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return datasource.getConnection(username, password);
	}

	private void assertDataSource() {
		if(datasource==null){
			throw new NullPointerException("You have not inject the spring DriverManagerDataSourece into this object.");
		}
	}

	public String getDriverClass() {
		return null;
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
	}

	public boolean isConnectionPool() {
		return false;
	}

	public void setWrappedDataSource(DataSource ds) {
		this.datasource=(AbstractDriverBasedDataSource)ds;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return AbstractDriverBasedDataSource.class;
	}

	public Properties getProperties() {
		return datasource.getConnectionProperties();
	}

	public void putProperty(String key, Object value) {
		if(datasource.getConnectionProperties()==null){
			datasource.setConnectionProperties(new Properties());
		}
		datasource.getConnectionProperties().put(key, value);
	}
}
