package jef.database.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.inject.Provider;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.springframework.util.StringUtils;

import com.github.geequery.support.spring.MultiDataSourceProvider;

import jef.common.Callback;
import jef.common.CopyOnWriteMap;
import jef.common.log.LogUtil;
import jef.database.ORMConfig;
import jef.database.innerpool.IConnection;
import jef.database.innerpool.JDBCRoutingConnection;
import jef.tools.Assert;

/**
 * 支持多DataSource的数据源，EF要实现多数据源配置和XA，需要使用此类来作为初始化的datasource。
 * 其实路由不是在DataSource层面做的。
 * 
 * @Date 2012-9-6
 */
public class DefaultRoutingDataSource extends AbstractDataSource implements RoutingDataSource {
	// 查找器1
	protected DataSourceLookup dataSourceLookup;
	// 缓存已经查找到结果
	protected Map<String, DataSource> resolvedDataSources = new CopyOnWriteMap<String, DataSource>();
	// 缺省数据源
	protected volatile String defaultKey;
	// 第一次成功的获取
	protected Entry<String, DataSource> firstReturnDataSource;
	// 初始化回调
	protected Callback<String, SQLException> callback;

	public String getDefaultKey() {
		return defaultKey;
	}

	public void setDefaultKey(String defaultKey) {
		this.defaultKey = defaultKey;
	}

	/**
	 * 空构造
	 */
	public DefaultRoutingDataSource() {
	}

	/**
	 * 构造
	 * 
	 * @param lookup
	 */
	public DefaultRoutingDataSource(DataSourceLookup lookup) {
		setDataSourceLookup(lookup);
	}

	public Connection getConnection() throws SQLException {
		return new JDBCRoutingConnection(this, ORMConfig.getInstance().isJpaContinueCommitIfError());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

	public DataSource getDataSource(String lookupKey) throws NoSuchElementException {
		DataSource dataSource = resolvedDataSources.get(lookupKey);
		if (dataSource == null && lookupKey == null) {
			throw new IllegalArgumentException("Can not lookup by empty Key, please assign a default datasource.");// 不允许这样使用，这样做会造成上层无法得到default的key,从而会将null ,"" ,"DEFAULT"这种表示误认为是三个数据源，其实是同一个。
		}
		if (dataSource == null) {
			dataSource = lookup(lookupKey);
		}
		if (dataSource == null) {
			throw new NoSuchElementException("Cannot determine target DataSource for lookup key [" + lookupKey + "]");
		}
		// 记录第一次成功获取的datasource，留作备用
		if (firstReturnDataSource == null) {
			firstReturnDataSource = new jef.common.Entry<String, DataSource>(lookupKey, dataSource);
			if (defaultKey == null) {
				defaultKey = lookupKey;
			}
		}
		return dataSource;
	}

	// 去找寻数据源配置
	private synchronized DataSource lookup(String lookupKey) {
		Assert.notNull(lookupKey);// 不允许ket为空的查找
		DataSource ds = null;
		if (dataSourceLookup != null) {
			ds = dataSourceLookup.getDataSource(lookupKey);
			if (ds != null)
				ds = checkDatasource(ds);
		}
		if (ds != null) {
			resolvedDataSources.put(lookupKey, ds);
			invokeCallback(lookupKey, ds);
		}
		return ds;
	}

	private void invokeCallback(String lookupKey, DataSource ds) {
		if (callback != null) {
			try {
				callback.call(lookupKey);
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}
	}

	/**
	 * 供子类覆盖，用于挑选lookup返回的datasource是否合理。（检查实例，池，XA等特性）
	 * 
	 * 有三种行为可供使用 1、返回再行包装后的Datasource 2、返回Null，本次lookup作废，会尝试去用DataSourceInfo查找。
	 * 3、直接抛出异常，提示用户配置错误等.
	 * 
	 * @param ds
	 * @return
	 */
	protected DataSource checkDatasource(DataSource ds) {
		return ds;
	}

	/**
	 * 供子类覆盖用，用于将返回的数据库配置信息包装为DataSource
	 * 
	 * @param dsi
	 * @return
	 */
	protected DataSource createDataSource(DataSourceInfo dsi) {
		return DataSources.getAsDataSource(dsi);
	}

	public boolean isSingleDatasource() {
		Set<String> s = new HashSet<String>();
		if (dataSourceLookup != null) {
			s.addAll(dataSourceLookup.getAvailableKeys());
		}
		return s.size() < 2;
	}

	/**
	 * 获得所有已解析的DataSource名称
	 * 
	 * @return
	 */
	public Set<String> getDataSourceNames() {
		Set<String> set = new HashSet<String>(resolvedDataSources.keySet());
		if (dataSourceLookup != null)
			set.addAll(dataSourceLookup.getAvailableKeys());
		return set;
	}

	@Override
	protected Class<? extends DataSource> getWrappedClass() {
		return null;
	}

	public void setDataSourceLookup(DataSourceLookup lookup) {
		this.dataSourceLookup = lookup;
		this.defaultKey = dataSourceLookup.getDefaultKey();
		if (StringUtils.isEmpty(defaultKey)) {
			if (!dataSourceLookup.getAvailableKeys().isEmpty()) {
				this.defaultKey = dataSourceLookup.getAvailableKeys().iterator().next();
				LogUtil.info("The default datasource was set to [{}].", this.defaultKey);
			}else {
				LogUtil.warn("There is NO default datasource set.");
			}
		}
	}

	public void setCallback(Callback<String, SQLException> callback) {
		this.callback = callback;
	}

	final class P implements MultiDataSourceProvider, Provider<IConnection> {
		@Override
		public IConnection get() {
			return new JDBCRoutingConnection(DefaultRoutingDataSource.this, ORMConfig.getInstance().isJpaContinueCommitIfError());
		}

		@Override
		public RoutingDataSource getRoutingDataSource() {
			return DefaultRoutingDataSource.this;
		}
	}

	public Provider<IConnection> toProvider() {
		return new P();
	}
}
