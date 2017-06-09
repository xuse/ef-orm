package jef.database.datasource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

/**
 * 默认的DataSourceInfoLookup实现之一。
 * 所有的数据源信息都被存放在Map中。
 * @author jiyi
 *
 */
public class MapDataSourceLookup implements DataSourceLookup{
	private Map<String,DataSource> datasources;
	private String defaultKey;
	
	public Map<String, DataSource> getDatasources() {
		return datasources;
	}
	public MapDataSourceLookup(){
	}
	public MapDataSourceLookup(Map<String, DataSource> datasources){
		this.datasources=datasources;
	}
	
	public void setDatasources(Map<String, DataSource> datasources) {
		this.datasources = datasources;
	}

	public DataSource getDataSource(String dataSourceName){
		if(datasources==null)return null;
		return datasources.get(dataSourceName);
	}

	public String getDefaultKey() {
		if(datasources!=null && datasources.size()==1){
			return datasources.keySet().iterator().next();
		}
		return defaultKey;
	}

	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}

	public Collection<String> getAvailableKeys() {
		return Collections.unmodifiableCollection(datasources.keySet());
	}
	
	public void put(String key,DataSource ds){
	    if(datasources==null){
	        datasources=new HashMap<String,DataSource>();
	    }
	    datasources.put(key, ds);
	}
}
