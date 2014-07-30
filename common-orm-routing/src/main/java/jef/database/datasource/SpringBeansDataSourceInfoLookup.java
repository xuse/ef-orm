package jef.database.datasource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jef.tools.Assert;

import org.apache.commons.lang.StringUtils;


/**
 * Spring的DatasourceInfo查找实现
 * @author jiyi
 *
 */
public class SpringBeansDataSourceInfoLookup extends AbstractSpringBeanLookup<DataSourceInfo> implements DataSourceInfoLookup{
	Map<String,DataSource> dsCache;
	private PasswordDecryptor passwordDecryptor=PasswordDecryptor.DUMMY;

	
	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		if(isIgnorCase())dataSourceName=StringUtils.lowerCase(dataSourceName);//忽略大小写
		DataSourceInfo dsi;
		if(cache!=null){
			dsi=cache.get(dataSourceName);
			if(dsi!=null)return decrypt(dsi);
		}
		cache=getCache();
		dsi=cache.get(dataSourceName);
		if(dsi!=null)return decrypt(dsi);
		
		//DSI查找没找到的话，尝试找那些可以被重新封装为DataSourceInfo的DataSource类型
		if(dsCache!=null){
			DataSource ds=dsCache.get(dataSourceName);
			dsi=DataSources.wrapFor(ds);
			if(dsi!=null)return decrypt(dsi);
		}
		dsCache=getDataSoruceCache();
		DataSource ds=dsCache.get(dataSourceName);
		dsi=DataSources.wrapFor(ds);
		return decrypt(dsi);
	}

	private DataSourceInfo decrypt(DataSourceInfo dsi) {
		String newpass=passwordDecryptor.decrypt(dsi.getPassword());
		if(!StringUtils.equals(dsi.getPassword(), newpass)){
			dsi.setPassword(newpass);
		}
		return dsi;
	}

	private Map<String, DataSource> getDataSoruceCache() {
		Assert.notNull(context);
		Map<String, DataSource> ds = context.getBeansOfType(DataSource.class);// 这是一个非常复杂的操作，因此将结果缓存起来
		if (!isIgnorCase())
			return ds;
		Map<String, DataSource> result = new HashMap<String, DataSource>();
		log.debug("getting type:DataSource from spring context, found {} beans.",ds.size());
		for (Map.Entry<String, DataSource> entry : ds.entrySet()) {
			result.put(StringUtils.lowerCase(entry.getKey()), entry.getValue());
		}
		return result;
	}

	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		this.passwordDecryptor=passwordDecryptor;
	}

	public String getDefaultKey() {
		if(cache==null){
			cache=getCache();
		}
		if(cache.size()==1){
			return cache.keySet().iterator().next();
		}
		return defaultBeanName;
	}
	
	public Collection<String> getAvailableKeys() {
		if (cache == null) {
			cache=getCache();
		}
		return Collections.unmodifiableCollection(cache.keySet());
	}
}
