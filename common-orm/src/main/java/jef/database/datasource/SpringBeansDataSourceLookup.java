package jef.database.datasource;

import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import jef.tools.StringUtils;

/**
 * 在Spring的配置中找寻Bean的DataSoruceLookup
 * @author jiyi
 *
 */
public class SpringBeansDataSourceLookup extends AbstractSpringBeanLookup<DataSource> implements DataSourceLookup{
	public DataSource getDataSource(String dataSourceName) {
		if(isIgnorCase())dataSourceName=StringUtils.lowerCase(dataSourceName);//忽略大小写
		if(cache!=null){
			DataSource datasource=cache.get(dataSourceName);
			if(datasource!=null)return datasource;
		}
		cache=getCache();
		DataSource datasource=cache.get(dataSourceName);
		return datasource;
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
