package jef.database.datasource;

import java.util.Map;
import java.util.Properties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import jef.database.DataObject;
import jef.database.annotation.EasyEntity;


@SuppressWarnings("serial")
@Entity
@EasyEntity(checkEnhanced=false)
@Table(name="DATASOURCE_CONFIG")
public class DataSourceInfoImpl extends DataObject implements DataSourceInfo {
	@Column(name="JDBC_URL")
	private String url;
	@Column(name="DB_USER")
	private String user;
	@Column(name="DB_PASSWORD")
	private String password;
	@Column(name="DRIVER_CLASS")
	private String driverClass;
	@Column(name="ENABLE")
	private boolean enable=true;
	@Column(name="DATABASE_NAME")
	private String dbKey;
	
	private final Properties properties=new Properties();
	
	public DataSourceInfoImpl(){
	}
	public DataSourceInfoImpl(String url) {
		this.url=url;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getDriverClass() {
		return driverClass;
	}
	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}
	public Properties getProperties() {
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties.clear();
		this.properties.putAll(properties);
	}
	public void putProperty(String key, Object value) {
		properties.put(key, value);
	}
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	public String getDbKey() {
		return dbKey;
	}
	public void setDbKey(String dbKey) {
		this.dbKey = dbKey;
	}

	public enum Field implements jef.database.Field{
		url,user,password,driverClass,enable,dbKey
	}
}
