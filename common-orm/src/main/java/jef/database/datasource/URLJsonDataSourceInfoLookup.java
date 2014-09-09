package jef.database.datasource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * 设置一个location，localtion可以是一个URL，如http://baidu.com/getDatasource.jsp.也可以是一个本地资源文件的名称
 * 
 * 通过classpath查找json格式的配置文件，来实现数据源的配置
 * @author jiyi
 *
 */
public class URLJsonDataSourceInfoLookup extends AbstractJsonDataSourceInfoLookup{
	private String location;
	private String defaultKey;
	@Override
	protected URL getResource() {
		if(location.indexOf("://")>-1){
			try {
				return new URL(location);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(location);
			}
		}
		return this.getClass().getResource(location);
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String localtion) {
		this.location = localtion;
	}

	public String getDefaultKey() {
		if(cache==null){
			cache=getCache();
		}
		if(cache.size()==1){
			return cache.keySet().iterator().next();
		}
		return defaultKey;
	}

	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}

	public Collection<String> getAvailableKeys() {
		if(cache==null){
			cache=getCache();
		}
		return cache.keySet();
	}
}
