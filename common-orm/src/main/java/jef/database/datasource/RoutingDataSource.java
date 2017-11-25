package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import jef.common.Callback;
import jef.common.CopyOnWriteMap;
import jef.common.log.LogUtil;
import jef.tools.Assert;

/**
 * 支持路由的数据源，JEF要实现多数据源配置和XA，需要使用此类来作为初始化的datasource
 * 
 * 
 * 
 * 
 * @Date 2012-9-6
 */
public class RoutingDataSource extends AbstractDataSource implements IRoutingDataSource{
	//查找器1
	protected DataSourceLookup dataSourceLookup;
	//缓存已经查找到结果
	protected Map<String, DataSource> resolvedDataSources=new CopyOnWriteMap<String, DataSource>();
	//记录使用过的第一个数据源作为缺省的
	protected Entry<String,DataSource> resolvedDefaultDataSource;
	//第一次成功的获取
	protected Entry<String,DataSource> firstReturnDataSource;
	
	//记录当前datasource的状态，用于确定本次操作应该返回哪个数据源的连接
	private final ThreadLocal<String> keys=new ThreadLocal<String>();
	//初始化回调
	protected Callback<String,SQLException> callback;
	/**
	 * 空构造
	 */
	public RoutingDataSource(){
	}
	/**
	 * 构造
	 * @param lookup
	 */
	public RoutingDataSource(DataSourceLookup lookup){
		this.dataSourceLookup=lookup;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalStateException
	 *             if determineTargetDataSource() is null
	 * @see #determineTargetDataSource()
	 */
	public Connection getConnection() throws SQLException {
		return determineTargetDataSource().getConnection();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalStateException
	 *             if determineTargetDataSource() is null
	 * @see #determineTargetDataSource()
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		return determineTargetDataSource().getConnection(username, password);
	}

	/**
	 * Retrieve the current target DataSource. Determines the
	 * {@link #determineCurrentLookupKey() current lookup key}, performs a
	 * lookup in the {@link #setTargetDataSources targetDataSources} map, falls
	 * back to the specified {@link #setDefaultTargetDataSource default target
	 * DataSource} if necessary.
	 * 
	 * @see #determineCurrentLookupKey()
	 * @throws IllegalStateException
	 *             If cannot determine target dataSource for the given lookupKey
	 */
	protected DataSource determineTargetDataSource() {
		//Assert.notNull(resolvedDataSources, "DataSource router not initialized");
		String lookupKey = determineCurrentLookupKey();
		if(lookupKey==null){
			Entry<String,DataSource> result=getDefaultDatasource();
			if(result==null)
				throw new IllegalArgumentException("Can not determine default datasource. avaliable datasoruces are:" + getDataSourceNames());
			return result.getValue(); 
		}else{
			return getDataSource(lookupKey);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see jef.database.datasource.IRoutingDataSource#getDataSource(java.lang.String)
	 */
	public DataSource getDataSource(String lookupKey) throws NoSuchElementException{
		DataSource dataSource = resolvedDataSources.get(lookupKey);
		if (dataSource == null && lookupKey == null) {
			throw new IllegalArgumentException("Can not lookup by empty Key");//不允许这样使用，这样做会造成上层无法得到default的key,从而会将null ,"" ,"DEFAULT"这种表示误认为是三个数据源，其实是同一个。
		}
		if (dataSource == null) {
			dataSource=lookup(lookupKey);
		}
		if (dataSource == null) {
			throw new NoSuchElementException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");	
		}
		//记录第一次成功获取的datasource，留作备用
		if(firstReturnDataSource==null)firstReturnDataSource=new jef.common.Entry<String,DataSource>(lookupKey,dataSource);
		return dataSource;
	}

	public Entry<String,DataSource> getDefaultDatasource() {
		if(resolvedDefaultDataSource!=null){ //如果缺省数据源已经计算出来，那么直接返回。
			return resolvedDefaultDataSource;
		}
		DataSource ds=null;
		if(dataSourceLookup!=null){
			String defaultKey=dataSourceLookup.getDefaultKey();		//计算缺省数据源
			if(defaultKey!=null){
				LogUtil.info("Lookup key is null, using the default datasource:"+defaultKey);
				ds=resolvedDataSources.get(defaultKey);
				if(ds==null){
					ds=lookup(defaultKey);
				}
				if(ds==null){
					throw new NullPointerException("The default datasource '"+defaultKey+"' is not exist, please check you configuration!");
				}
				resolvedDefaultDataSource =new jef.common.Entry<String,DataSource>(defaultKey,ds); //记录缺省数据源
				return resolvedDefaultDataSource;
			}
		}
		//无法计算和找到缺省数据源，将之前首次使用的数据源当作缺省数据源返回（可能为null）
		return firstReturnDataSource;
	}
	
	

	//去找寻数据源配置
	private synchronized DataSource lookup(String lookupKey) {
		Assert.notNull(lookupKey);//不允许ket为空的查找
		DataSource ds=null;
		if(dataSourceLookup!=null){
			ds=dataSourceLookup.getDataSource(lookupKey);
			if(ds!=null)ds=checkDatasource(ds);
		}
		if(ds!=null){
			resolvedDataSources.put(lookupKey, ds);
			invokeCallback(lookupKey,ds);
		}
		return ds;
	}

	private void invokeCallback(String lookupKey, DataSource ds) {
		if(callback!=null){
			try{
				callback.call(lookupKey);
			}catch(SQLException e){
				throw new PersistenceException(e);
			}
		}
	}
	/**
	 * 供子类覆盖，用于挑选lookup返回的datasource是否合理。（检查实例，池，XA等特性）
	 * 
	 * 有三种行为可供使用
	 * 1、返回再行包装后的Datasource
	 * 2、返回Null，本次lookup作废，会尝试去用DataSourceInfo查找。
	 * 3、直接抛出异常，提示用户配置错误等.
	 * @param ds
	 * @return
	 */
	protected DataSource checkDatasource(DataSource ds) {
		return ds;
	}

	/**
	 * 供子类覆盖用，用于将返回的数据库配置信息包装为DataSource
	 * @param dsi
	 * @return
	 */
	protected DataSource createDataSource(DataSourceInfo dsi) {
		return DataSources.getAsDataSource(dsi);
	}
	/**
	 * 设置查找的数据源键
	 * @param key
	 */
	protected void setLookupKey(String key) {
		keys.set(key);
	}
	/**
	 * 获取数据源的键
	 * @return
	 */
	protected String determineCurrentLookupKey() {
		return keys.get();
	}
	
	public boolean isSingleDatasource() {
		Set<String> s=new HashSet<String>();
		if(dataSourceLookup!=null){
			s.addAll(dataSourceLookup.getAvailableKeys());
		}
		return s.size()<2;
	}

	/**
	 * 获得所有已解析的DataSource名称
	 * @return
	 */
	public Set<String> getDataSourceNames() {
		Set<String> set=new HashSet<String>(resolvedDataSources.keySet());
		if(dataSourceLookup!=null)
			set.addAll(dataSourceLookup.getAvailableKeys());
		return set;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}

	public void setDataSourceLookup(DataSourceLookup dataSourceLookup) {
		this.dataSourceLookup = dataSourceLookup;
	}

	public void setCallback(Callback<String, SQLException> callback) {
		this.callback = callback;
	}
}

