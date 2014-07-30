package jef.database.datasource;

import java.util.Collection;
import java.util.Collections;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jef.common.log.LogUtil;

import org.apache.commons.lang.StringUtils;

/**
 * 通过JNDI查找Datasource
 * 
 * 这个类也同时实现了DataSourceLookup和DataSourceInfoLookup接口
 * @author jiyi
 *
 */
public class JndiDatasourceLookup implements DataSourceLookup,DataSourceInfoLookup{
	private InitialContext ctx;
	private PasswordDecryptor passwordDecryptor;
	private String defaultKey;
	
	public DataSource getDataSource(String dataSourceName) {
		if(ctx==null){
			init();
		}
		try {
			DataSource ds = (DataSource) ctx.lookup(dataSourceName);
			DataSourceWrapper dw=DataSources.wrapFor(ds);
			if(dw==null){
				return ds;//包装失败
			}else{
				return (DataSource) decrypt(dw);//包装成功，支持解密特性
			}
		} catch (NamingException e) {
			LogUtil.exception("Can not lookup datasource from JNDI:" + dataSourceName, e);
		}
		return null;
	}
	

	public DataSourceInfo getDataSourceInfo(String dataSourceName) {
		if(ctx==null){
			init();
		}
		try {
			Object ds = ctx.lookup(dataSourceName);
			if(ds instanceof DataSourceInfo){
				return decrypt((DataSourceInfo) ds);
			}
			DataSourceWrapper dw=DataSources.wrapFor((DataSource)ds);
			if(dw==null){
				return null;//包装失败
			}else{
				return decrypt(dw);//包装成功，支持解密特性
			}
		} catch (NamingException e) {
			LogUtil.exception("Can not lookup datasource from JNDI:" + dataSourceName, e);
		}
		return null;
	}


	private DataSourceInfo decrypt(DataSourceInfo dsi) {
		String newpass=passwordDecryptor.decrypt(dsi.getPassword());
		if(!StringUtils.equals(dsi.getPassword(), newpass)){
			dsi.setPassword(newpass);
		}
		return dsi;
	}
	
	private synchronized void init() {
		if(ctx!=null)return;
		try {
			ctx= new InitialContext();
		} catch (NamingException e) {
			throw new IllegalStateException("JNDI init error.",e);
		}
	}

	public void setPasswordDecryptor(PasswordDecryptor passwordDecryptor) {
		passwordDecryptor=this.passwordDecryptor;
	}


	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}


	public String getDefaultKey() {
		return defaultKey;
	}
	@SuppressWarnings("unchecked")
	public Collection<String> getAvailableKeys() {
		return Collections.EMPTY_LIST;
	}
}
