package jef.database.datasource;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import jef.common.log.LogUtil;
import jef.common.wrapper.PropertiesMap;
import jef.tools.IOUtils;
import jef.tools.StringUtils;

/**
 * DatasourceLookup implementaion of properties
 * 会在classpath下的指定资源文件，以及System.properties中查找数据源配置
 * @author jiyi
 *
 */
public class PropertiesDataSourceInfoLookup implements DataSourceInfoLookup{
	private String keyOfPrefix="";
	private String keyOfUrl=".url";
	private String keyOfUser=".user";
	private String keyOfPassword=".password";
	private String keyOfDriver=".driver";
	private String localtion;
	private boolean needLogonInfo=true;
	private PasswordDecryptor passwordDecryptor=PasswordDecryptor.DUMMY;
	private String defaultKey;
	
	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		DataSourceInfo dsi=tryGet(new PropertiesMap(System.getProperties()),dataSourceName);
		if(dsi!=null)return dsi;
		if(StringUtils.isNotEmpty(localtion!=null)){
			try {
				Enumeration<URL> resources=this.getClass().getClassLoader().getResources(localtion);
				for(;resources.hasMoreElements();){
					Map<String,String> properties=IOUtils.loadProperties(resources.nextElement());
					dsi=tryGet(properties,dataSourceName);
					if(dsi!=null)
						break;
				}
			} catch (IOException e) {
				LogUtil.exception(e);
			}
		}
		return dsi;
	}

	private DataSourceInfo tryGet(Map<String,String> p,String dataSourceName) {
		String url=p.get(keyOfPrefix+dataSourceName+keyOfUrl);
		if(url!=null && url.length()>0){
			DataSourceInfoImpl dsi=new DataSourceInfoImpl(url);
			dsi.setUser(p.get(keyOfPrefix+dataSourceName+keyOfUser));
			String password=p.get(keyOfPrefix+dataSourceName+keyOfPassword);
			if(StringUtils.isNotEmpty(password)){
				password=passwordDecryptor.decrypt(password);
			}
			dsi.setPassword(password);
			dsi.setDriverClass(p.get(keyOfPrefix+dataSourceName+keyOfDriver));
			if(needLogonInfo && StringUtils.isEmpty(dsi.getUser())){
				dsi=null;
			}
			return dsi;
		}
		return null;
	}

	/**
	 * 数据源配置在Properties文件中的前缀，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfPrefix就是 'jdbc.'
	 * @param keyOfPrefix default value is empty
	 */
	public void setKeyOfPrefix(String keyOfPrefix) {
		this.keyOfPrefix = keyOfPrefix;
	}

	/**
	 * 数据源配置在Properties文件中的URL的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfUrl就是 '.url'
	 * @param keyOfPrefix default valuie is '.url'
	 */
	public void setKeyOfUrl(String keyOfUrl) {
		this.keyOfUrl = keyOfUrl;
	}
	/**
	 * 数据源配置在Properties文件中的username的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfUser就是 '.user'
	 * @param keyOfUser default valuie is '.user'
	 */
	public void setKeyOfUser(String keyOfUser) {
		this.keyOfUser = keyOfUser;
	}
	/**
	 * 数据源配置在Properties文件中的username的名称，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * </pre>
	 * 在上例中KeyOfPassword就是 '.password'
	 * @param keyOfPassword default valuie is '.password'
	 */
	public void setKeyOfPassword(String keyOfPassword) {
		this.keyOfPassword = keyOfPassword;
	}
	/**
	 * 数据源配置在Properties文件中的driverClass，举例<pre>
	 * jdbc.ds1.url=jdbc:mysql://localhost:3306/test
	 * jdbc.ds1.user=root
	 * jdbc.ds1.password=123456
	 * jdbc.ds1.driver=org.gjt.mm.mysql.Driver
	 * </pre>
	 * 在上例中KeyOfDriver就是 '.driver'
	 * @param keyOfDriver default valuie is '.driver'
	 */
	public void setKeyOfDriver(String keyOfDriver) {
		this.keyOfDriver = keyOfDriver;
	}
	
	/**
	 * 设置properties文件路径。不设置的话只会使用System.getProperty();
	 * 路径不能以/开头，jef会查找所有classpath下的该名称的资源文件来查找数据库配置
	 * @param localtion
	 */
	public void setLocaltion(String localtion) {
		this.localtion = localtion;
	}

	/**
	 * 如果没有配置user或者password的数据源就忽略
	 * @param needLogonInfo
	 */
	public void setNeedLogonInfo(boolean needLogonInfo) {
		this.needLogonInfo = needLogonInfo;
	}

	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		this.passwordDecryptor=passwordDecryptor;
	}

	public String getDefaultKey() {
		return defaultKey;
	}

	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}
	
	@SuppressWarnings("unchecked")
	public Collection<String> getAvailableKeys() {
		return Collections.EMPTY_LIST;
	}
}
