package jef.database.datasource;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import jef.tools.Assert;
import jef.tools.reflect.FieldAccessor;

/**
 * 最狠的设计。通过反射强行获取或者设置目标DataSource中的字段的实现。
 * 总之不太建议用这个类
 * @author jiyi
 *
 */
public class RefectingHackDataSourceWrapper extends AbstractDataSource implements DataSourceWrapper{
	private DataSource datasource;
	private FieldAccessor url;
	private FieldAccessor user;
	private FieldAccessor password;
	private FieldAccessor driverClass;
	private FieldAccessor prop;

	public RefectingHackDataSourceWrapper(){
		datasource=new SimpleDataSource();
		initAccessor();
	}
	public RefectingHackDataSourceWrapper(DataSource ds) {
		this.datasource=ds;
		initAccessor();
	}
	private void initAccessor() {
		Class<?> clz=datasource.getClass();
		url=null;
		user=null;
		password=null;
		driverClass=null;
		while(clz!=Object.class){
			//properties 
			if(url==null)
				url=getField(clz,"url","jdbcUrl","driverUrl","connectionUrl");
			if(user==null)
				user=getField(clz,"user","username","userName");
			if(password==null)
				password=getField(clz,"password");
			if(driverClass==null)
				driverClass=getField(clz,"driverClassName","driverClass","driverName");
			if(prop==null)
				prop=getField(clz,"properties","prop");
			clz=clz.getSuperclass();
		}
		Assert.notNull(url);
		Assert.notNull(user);
		Assert.notNull(password);
	}
	
	private FieldAccessor getField(Class<?> clz, String... names) {
		for(String name:names){
			try {
				Field field=clz.getDeclaredField(name);
				if(field.getType()==String.class){
					return 	FieldAccessor.generateAccessor(field);
				}
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
			}
		}
		return null;
	}
	public String getUrl() {
		if(url!=null)
			return (String) url.get(datasource);
		return null;
	}

	public String getUser() {
		if(user!=null)
			return (String) user.get(datasource);
		return null;
	}

	public String getPassword() {
		if(password!=null)
			return (String) password.get(datasource);
		return null;
	}

	public String getDriverClass() {
		if(driverClass!=null)
			return (String) driverClass.get(datasource);
		return null;
	}

	public void setUrl(String url) {
		if(this.url!=null)
			this.url.set(datasource,url);
	}

	public void setUser(String user) {
		if(this.user!=null)
			this.user.set(datasource,user);
	}

	public void setPassword(String password) {
		if(this.password!=null)
			this.password.set(datasource,password);
	}

	public void setDriverClass(String driverClassName) {
		if(this.driverClass!=null)
			this.driverClass.set(datasource,driverClassName);
	}

	public Connection getConnection() throws SQLException {
		return datasource.getConnection();
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return datasource.getConnection(username, password);
	}

	public boolean isConnectionPool() {
		return datasource.getClass().getName().toLowerCase().indexOf("pool")>0;
	}

	public void setWrappedDataSource(DataSource ds) {
		this.datasource=ds;
		initAccessor();
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}
	public Properties getProperties() {
		if(prop!=null){
			Properties map=(Properties) this.prop.get(datasource);
			return map;
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void putProperty(String key, Object value) {
		if(prop!=null){
			Map map=(Map) this.prop.get(datasource);
			map.put(key, value);
		}
	}
}
