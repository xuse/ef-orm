package jef.database.datasource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MapDataSourceInfoLookup implements DataSourceInfoLookup{
	private Map<String,DataSourceInfo> datasources;
	private PasswordDecryptor passwordDecryptor=PasswordDecryptor.DUMMY;
	private String defaultKey;
	public void setDatasources(Map<String, DataSourceInfo> datasources) {
		this.datasources = datasources;
	}

	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		if(datasources==null)return null;
		DataSourceInfo dsi=datasources.get(dataSourceName);
		if(dsi!=null){
			return decrypt(dsi);
		}else{
			return null;
		}
	}
	
	public void add(String key,DataSourceInfo dsi){
		if(this.datasources==null){
			datasources=new HashMap<String,DataSourceInfo>();
			datasources.put(key, dsi);
		}
	}
	
	private DataSourceInfo decrypt(DataSourceInfo dsi) {
		String newpass=passwordDecryptor.decrypt(dsi.getPassword());
		if(!StringUtils.equals(dsi.getPassword(), newpass)){
			dsi.setPassword(newpass);
		}
		return dsi;
	}

	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		this.passwordDecryptor=passwordDecryptor;
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
}
